/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.inputmethod.latin

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.CharacterStyle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import com.android.inputmethod.compat.InputConnectionCompatUtils
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.StringUtils
import com.android.inputmethod.latin.common.UnicodeSurrogate
import com.android.inputmethod.latin.inputlogic.PrivateCommandPerformer
import com.android.inputmethod.latin.settings.SpacingAndPunctuations
import com.android.inputmethod.latin.utils.CapsModeUtils
import com.android.inputmethod.latin.utils.DebugLogUtils
import com.android.inputmethod.latin.utils.NgramContextUtils
import com.android.inputmethod.latin.utils.ScriptUtils
import com.android.inputmethod.latin.utils.SpannableStringUtils
import com.android.inputmethod.latin.utils.StatsUtils
import com.android.inputmethod.latin.utils.TextRange
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

/**
 * Enrichment class for InputConnection to simplify interaction and add functionality.
 *
 * This class serves as a wrapper to be able to simply add hooks to any calls to the underlying
 * InputConnection. It also keeps track of a number of things to avoid having to call upon IPC
 * all the time to find out what text is in the buffer, when we need it to determine caps mode
 * for example.
 */
class RichInputConnection(parent: InputMethodService) : PrivateCommandPerformer {
    /**
     * This variable contains an expected value for the selection start position. This is where the
     * cursor or selection start may end up after all the keyboard-triggered updates have passed. We
     * keep this to compare it to the actual selection start to guess whether the move was caused by
     * a keyboard command or not.
     * It's not really the selection start position: the selection start may not be there yet, and
     * in some cases, it may never arrive there.
     */
    var expectedSelectionStart: Int = INVALID_CURSOR_POSITION // in chars, not code points
        private set

    /**
     * The expected selection end.  Only differs from mExpectedSelStart if a non-empty selection is
     * expected.  The same caveats as mExpectedSelStart apply.
     */
    var expectedSelectionEnd: Int = INVALID_CURSOR_POSITION // in chars, not code points
        private set

    /**
     * This contains the committed text immediately preceding the cursor and the composing
     * text, if any. It is refreshed when the cursor moves by calling upon the TextView.
     */
    private val mCommittedTextBeforeComposingText: StringBuilder = StringBuilder()

    /**
     * This contains the currently composing text, as LatinIME thinks the TextView is seeing it.
     */
    private val mComposingText: StringBuilder = StringBuilder()

    /**
     * This variable is a temporary object used in [.commitText]
     * to avoid object creation.
     */
    private val mTempObjectForCommitText: SpannableStringBuilder = SpannableStringBuilder()

    private val mParent: InputMethodService
    private var mIC: InputConnection?
    private var mNestLevel: Int

    /**
     * The timestamp of the last slow InputConnection operation
     */
    private var mLastSlowInputConnectionTime: Long = -SLOW_INPUTCONNECTION_PERSIST_MS

    init {
        mParent = parent
        mIC = null
        mNestLevel = 0
    }

    val isConnected: Boolean
        get() {
            return mIC != null
        }

    /**
     * Returns whether or not the underlying InputConnection is slow. When true, we want to avoid
     * calling InputConnection methods that trigger an IPC round-trip (e.g., getTextAfterCursor).
     */
    fun hasSlowInputConnection(): Boolean {
        return ((SystemClock.uptimeMillis() - mLastSlowInputConnectionTime)
                <= SLOW_INPUTCONNECTION_PERSIST_MS)
    }

    fun onStartInput() {
        mLastSlowInputConnectionTime = -SLOW_INPUTCONNECTION_PERSIST_MS
    }

    private fun checkConsistencyForDebug() {
        val r: ExtractedTextRequest = ExtractedTextRequest()
        r.hintMaxChars = 0
        r.hintMaxLines = 0
        r.token = 1
        r.flags = 0
        val et: ExtractedText? = mIC!!.getExtractedText(r, 0)
        val beforeCursor: CharSequence? = getTextBeforeCursor(
            Constants.EDITOR_CONTENTS_CACHE_SIZE,
            0
        )
        val internal: StringBuilder = StringBuilder(mCommittedTextBeforeComposingText)
            .append(mComposingText)
        if (null == et || null == beforeCursor) return
        val actualLength: Int =
            min(beforeCursor.length.toDouble(), internal.length.toDouble()).toInt()
        if (internal.length > actualLength) {
            internal.delete(0, internal.length - actualLength)
        }
        val reference: String = if ((beforeCursor.length <= actualLength))
            beforeCursor.toString()
        else
            beforeCursor.subSequence(
                beforeCursor.length - actualLength,
                beforeCursor.length
            ).toString()
        if (et.selectionStart != expectedSelectionStart
            || !(reference == internal.toString())
        ) {
            val context: String = ("Expected selection start = " + expectedSelectionStart
                    + "\nActual selection start = " + et.selectionStart
                    + "\nExpected text = " + internal.length + " " + internal
                    + "\nActual text = " + reference.length + " " + reference)
            (mParent as LatinIME).debugDumpStateAndCrashWithException(context)
        } else {
            Log.e(TAG, DebugLogUtils.getStackTrace(2))
            Log.e(TAG, "Exp <> Actual : " + expectedSelectionStart + " <> " + et.selectionStart)
        }
    }

    fun beginBatchEdit() {
        if (++mNestLevel == 1) {
            mIC = mParent.getCurrentInputConnection()
            if (isConnected) {
                mIC!!.beginBatchEdit()
            }
        } else {
            if (DBG) {
                throw RuntimeException("Nest level too deep")
            }
            Log.e(TAG, "Nest level too deep : " + mNestLevel)
        }
        if (DEBUG_BATCH_NESTING) checkBatchEdit()
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug()
    }

    fun endBatchEdit() {
        if (mNestLevel <= 0) Log.e(TAG, "Batch edit not in progress!") // TODO: exception instead

        if (--mNestLevel == 0 && isConnected) {
            mIC!!.endBatchEdit()
        }
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug()
    }

    /**
     * Reset the cached text and retrieve it again from the editor.
     *
     * This should be called when the cursor moved. It's possible that we can't connect to
     * the application when doing this; notably, this happens sometimes during rotation, probably
     * because of a race condition in the framework. In this case, we just can't retrieve the
     * data, so we empty the cache and note that we don't know the new cursor position, and we
     * return false so that the caller knows about this and can retry later.
     *
     * @param newSelStart the new position of the selection start, as received from the system.
     * @param newSelEnd the new position of the selection end, as received from the system.
     * @param shouldFinishComposition whether we should finish the composition in progress.
     * @return true if we were able to connect to the editor successfully, false otherwise. When
     * this method returns false, the caches could not be correctly refreshed so they were only
     * reset: the caller should try again later to return to normal operation.
     */
    fun resetCachesUponCursorMoveAndReturnSuccess(
        newSelStart: Int,
        newSelEnd: Int, shouldFinishComposition: Boolean
    ): Boolean {
        expectedSelectionStart = newSelStart
        expectedSelectionEnd = newSelEnd
        mComposingText.setLength(0)
        val didReloadTextSuccessfully: Boolean = reloadTextCache()
        if (!didReloadTextSuccessfully) {
            Log.d(TAG, "Will try to retrieve text later.")
            return false
        }
        if (isConnected && shouldFinishComposition) {
            mIC!!.finishComposingText()
        }
        return true
    }

    /**
     * Reload the cached text from the InputConnection.
     *
     * @return true if successful
     */
    private fun reloadTextCache(): Boolean {
        mCommittedTextBeforeComposingText.setLength(0)
        mIC = mParent.getCurrentInputConnection()
        // Call upon the inputconnection directly since our own method is using the cache, and
        // we want to refresh it.
        val textBeforeCursor: CharSequence? = getTextBeforeCursorAndDetectLaggyConnection(
            OPERATION_RELOAD_TEXT_CACHE,
            SLOW_INPUT_CONNECTION_ON_FULL_RELOAD_MS,
            Constants.EDITOR_CONTENTS_CACHE_SIZE,
            0 /* flags */
        )
        if (null == textBeforeCursor) {
            // For some reason the app thinks we are not connected to it. This looks like a
            // framework bug... Fall back to ground state and return false.
            expectedSelectionStart = INVALID_CURSOR_POSITION
            expectedSelectionEnd = INVALID_CURSOR_POSITION
            Log.e(TAG, "Unable to connect to the editor to retrieve text.")
            return false
        }
        mCommittedTextBeforeComposingText.append(textBeforeCursor)
        return true
    }

    private fun checkBatchEdit() {
        if (mNestLevel != 1) {
            // TODO: exception instead
            Log.e(TAG, "Batch edit level incorrect : " + mNestLevel)
            Log.e(TAG, DebugLogUtils.getStackTrace(4))
        }
    }

    fun finishComposingText() {
        if (DEBUG_BATCH_NESTING) checkBatchEdit()
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug()
        // TODO: this is not correct! The cursor is not necessarily after the composing text.
        // In the practice right now this is only called when input ends so it will be reset so
        // it works, but it's wrong and should be fixed.
        mCommittedTextBeforeComposingText.append(mComposingText)
        mComposingText.setLength(0)
        if (isConnected) {
            mIC!!.finishComposingText()
        }
    }

    /**
     * Calls [InputConnection.commitText].
     *
     * @param text The text to commit. This may include styles.
     * @param newCursorPosition The new cursor position around the text.
     */
    fun commitText(text: CharSequence, newCursorPosition: Int) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit()
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug()
        mCommittedTextBeforeComposingText.append(text)
        // TODO: the following is exceedingly error-prone. Right now when the cursor is in the
        // middle of the composing word mComposingText only holds the part of the composing text
        // that is before the cursor, so this actually works, but it's terribly confusing. Fix this.
        expectedSelectionStart += text.length - mComposingText.length
        expectedSelectionEnd = expectedSelectionStart
        mComposingText.setLength(0)
        if (isConnected) {
            mTempObjectForCommitText.clear()
            mTempObjectForCommitText.append(text)
            val spans: Array<CharacterStyle> = mTempObjectForCommitText.getSpans(
                0, text.length, CharacterStyle::class.java
            )
            for (span: CharacterStyle? in spans) {
                val spanStart: Int = mTempObjectForCommitText.getSpanStart(span)
                val spanEnd: Int = mTempObjectForCommitText.getSpanEnd(span)
                val spanFlags: Int = mTempObjectForCommitText.getSpanFlags(span)
                // We have to adjust the end of the span to include an additional character.
                // This is to avoid splitting a unicode surrogate pair.
                // See com.android.inputmethod.latin.common.Constants.UnicodeSurrogate
                // See https://b.corp.google.com/issues/19255233
                if (0 < spanEnd && spanEnd < mTempObjectForCommitText.length) {
                    val spanEndChar: Char = mTempObjectForCommitText.get(spanEnd - 1)
                    val nextChar: Char = mTempObjectForCommitText.get(spanEnd)
                    if (UnicodeSurrogate.isLowSurrogate(spanEndChar)
                        && UnicodeSurrogate.isHighSurrogate(nextChar)
                    ) {
                        mTempObjectForCommitText.setSpan(span, spanStart, spanEnd + 1, spanFlags)
                    }
                }
            }
            mIC!!.commitText(mTempObjectForCommitText, newCursorPosition)
        }
    }

    fun getSelectedText(flags: Int): CharSequence? {
        return if (isConnected) mIC!!.getSelectedText(flags) else null
    }

    fun canDeleteCharacters(): Boolean {
        return expectedSelectionStart > 0
    }

    /**
     * Gets the caps modes we should be in after this specific string.
     *
     * This returns a bit set of TextUtils#CAP_MODE_*, masked by the inputType argument.
     * This method also supports faking an additional space after the string passed in argument,
     * to support cases where a space will be added automatically, like in phantom space
     * state for example.
     * Note that for English, we are using American typography rules (which are not specific to
     * American English, it's just the most common set of rules for English).
     *
     * @param inputType a mask of the caps modes to test for.
     * @param spacingAndPunctuations the values of the settings to use for locale and separators.
     * @param hasSpaceBefore if we should consider there should be a space after the string.
     * @return the caps modes that should be on as a set of bits
     */
    fun getCursorCapsMode(
        inputType: Int,
        spacingAndPunctuations: SpacingAndPunctuations, hasSpaceBefore: Boolean
    ): Int {
        mIC = mParent.getCurrentInputConnection()
        if (!isConnected) {
            return Constants.TextUtils.CAP_MODE_OFF
        }
        if (!TextUtils.isEmpty(mComposingText)) {
            if (hasSpaceBefore) {
                // If we have some composing text and a space before, then we should have
                // MODE_CHARACTERS and MODE_WORDS on.
                return (TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS) and inputType
            }
            // We have some composing text - we should be in MODE_CHARACTERS only.
            return TextUtils.CAP_MODE_CHARACTERS and inputType
        }
        // TODO: this will generally work, but there may be cases where the buffer contains SOME
        // information but not enough to determine the caps mode accurately. This may happen after
        // heavy pressing of delete, for example DEFAULT_TEXT_CACHE_SIZE - 5 times or so.
        // getCapsMode should be updated to be able to return a "not enough info" result so that
        // we can get more context only when needed.
        if (TextUtils.isEmpty(mCommittedTextBeforeComposingText) && 0 != expectedSelectionStart) {
            if (!reloadTextCache()) {
                Log.w(
                    TAG, "Unable to connect to the editor. "
                            + "Setting caps mode without knowing text."
                )
            }
        }
        // This never calls InputConnection#getCapsMode - in fact, it's a static method that
        // never blocks or initiates IPC.
        // TODO: don't call #toString() here. Instead, all accesses to
        // mCommittedTextBeforeComposingText should be done on the main thread.
        return CapsModeUtils.getCapsMode(
            mCommittedTextBeforeComposingText.toString(), inputType,
            spacingAndPunctuations, hasSpaceBefore
        )
    }

    val codePointBeforeCursor: Int
        get() {
            val length: Int = mCommittedTextBeforeComposingText.length
            if (length < 1) return Constants.NOT_A_CODE
            return Character.codePointBefore(mCommittedTextBeforeComposingText, length)
        }

    fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? {
        val cachedLength: Int =
            mCommittedTextBeforeComposingText.length + mComposingText.length
        // If we have enough characters to satisfy the request, or if we have all characters in
        // the text field, then we can return the cached version right away.
        // However, if we don't have an expected cursor position, then we should always
        // go fetch the cache again (as it happens, INVALID_CURSOR_POSITION < 0, so we need to
        // test for this explicitly)
        if (INVALID_CURSOR_POSITION != expectedSelectionStart
            && (cachedLength >= n || cachedLength >= expectedSelectionStart)
        ) {
            val s: StringBuilder = StringBuilder(mCommittedTextBeforeComposingText)
            // We call #toString() here to create a temporary object.
            // In some situations, this method is called on a worker thread, and it's possible
            // the main thread touches the contents of mComposingText while this worker thread
            // is suspended, because mComposingText is a StringBuilder. This may lead to crashes,
            // so we call #toString() on it. That will result in the return value being strictly
            // speaking wrong, but since this is used for basing bigram probability off, and
            // it's only going to matter for one getSuggestions call, it's fine in the practice.
            s.append(mComposingText.toString())
            if (s.length > n) {
                s.delete(0, s.length - n)
            }
            return s
        }
        return getTextBeforeCursorAndDetectLaggyConnection(
            OPERATION_GET_TEXT_BEFORE_CURSOR,
            SLOW_INPUT_CONNECTION_ON_PARTIAL_RELOAD_MS,
            n, flags
        )
    }

    private fun getTextBeforeCursorAndDetectLaggyConnection(
        operation: Int, timeout: Long, n: Int, flags: Int
    ): CharSequence? {
        mIC = mParent.getCurrentInputConnection()
        if (!isConnected) {
            return null
        }
        val startTime: Long = SystemClock.uptimeMillis()
        val result: CharSequence? = mIC!!.getTextBeforeCursor(n, flags)
        detectLaggyConnection(operation, timeout, startTime)
        return result
    }

    fun getTextAfterCursor(n: Int, flags: Int): CharSequence? {
        return getTextAfterCursorAndDetectLaggyConnection(
            OPERATION_GET_TEXT_AFTER_CURSOR,
            SLOW_INPUT_CONNECTION_ON_PARTIAL_RELOAD_MS,
            n, flags
        )
    }

    private fun getTextAfterCursorAndDetectLaggyConnection(
        operation: Int, timeout: Long, n: Int, flags: Int
    ): CharSequence? {
        mIC = mParent.getCurrentInputConnection()
        if (!isConnected) {
            return null
        }
        val startTime: Long = SystemClock.uptimeMillis()
        val result: CharSequence? = mIC!!.getTextAfterCursor(n, flags)
        detectLaggyConnection(operation, timeout, startTime)
        return result
    }

    private fun detectLaggyConnection(operation: Int, timeout: Long, startTime: Long) {
        val duration: Long = SystemClock.uptimeMillis() - startTime
        if (duration >= timeout) {
            val operationName: String = OPERATION_NAMES.get(operation)
            Log.w(TAG, "Slow InputConnection: " + operationName + " took " + duration + " ms.")
            StatsUtils.onInputConnectionLaggy(operation, duration)
            mLastSlowInputConnectionTime = SystemClock.uptimeMillis()
        }
    }

    fun deleteTextBeforeCursor(beforeLength: Int) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit()
        // TODO: the following is incorrect if the cursor is not immediately after the composition.
        // Right now we never come here in this case because we reset the composing state before we
        // come here in this case, but we need to fix this.
        val remainingChars: Int = mComposingText.length - beforeLength
        if (remainingChars >= 0) {
            mComposingText.setLength(remainingChars)
        } else {
            mComposingText.setLength(0)
            // Never cut under 0
            val len: Int = max(
                (mCommittedTextBeforeComposingText.length
                        + remainingChars).toDouble(), 0.0
            ).toInt()
            mCommittedTextBeforeComposingText.setLength(len)
        }
        if (expectedSelectionStart > beforeLength) {
            expectedSelectionStart -= beforeLength
            expectedSelectionEnd -= beforeLength
        } else {
            // There are fewer characters before the cursor in the buffer than we are being asked to
            // delete. Only delete what is there, and update the end with the amount deleted.
            expectedSelectionEnd -= expectedSelectionStart
            expectedSelectionStart = 0
        }
        if (isConnected) {
            mIC!!.deleteSurroundingText(beforeLength, 0)
        }
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug()
    }

    fun performEditorAction(actionId: Int) {
        mIC = mParent.getCurrentInputConnection()
        if (isConnected) {
            mIC!!.performEditorAction(actionId)
        }
    }

    fun sendKeyEvent(keyEvent: KeyEvent) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit()
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug()
            // This method is only called for enter or backspace when speaking to old applications
            // (target SDK <= 15 (Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)), or for digits.
            // When talking to new applications we never use this method because it's inherently
            // racy and has unpredictable results, but for backward compatibility we continue
            // sending the key events for only Enter and Backspace because some applications
            // mistakenly catch them to do some stuff.
            when (keyEvent.getKeyCode()) {
                KeyEvent.KEYCODE_ENTER -> {
                    mCommittedTextBeforeComposingText.append("\n")
                    expectedSelectionStart += 1
                    expectedSelectionEnd = expectedSelectionStart
                }

                KeyEvent.KEYCODE_DEL -> {
                    if (0 == mComposingText.length) {
                        if (mCommittedTextBeforeComposingText.length > 0) {
                            mCommittedTextBeforeComposingText.delete(
                                mCommittedTextBeforeComposingText.length - 1,
                                mCommittedTextBeforeComposingText.length
                            )
                        }
                    } else {
                        mComposingText.delete(mComposingText.length - 1, mComposingText.length)
                    }
                    if (expectedSelectionStart > 0 && expectedSelectionStart == expectedSelectionEnd) {
                        // TODO: Handle surrogate pairs.
                        expectedSelectionStart -= 1
                    }
                    expectedSelectionEnd = expectedSelectionStart
                }

                KeyEvent.KEYCODE_UNKNOWN -> if (null != keyEvent.getCharacters()) {
                    mCommittedTextBeforeComposingText.append(keyEvent.getCharacters())
                    expectedSelectionStart += keyEvent.getCharacters().length
                    expectedSelectionEnd = expectedSelectionStart
                }

                else -> {
                    val text: String =
                        StringUtils.newSingleCodePointString(keyEvent.getUnicodeChar())
                    mCommittedTextBeforeComposingText.append(text)
                    expectedSelectionStart += text.length
                    expectedSelectionEnd = expectedSelectionStart
                }
            }
        }
        if (isConnected) {
            mIC!!.sendKeyEvent(keyEvent)
        }
    }

    fun setComposingRegion(start: Int, end: Int) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit()
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug()
        val textBeforeCursor: CharSequence? =
            getTextBeforeCursor(Constants.EDITOR_CONTENTS_CACHE_SIZE + (end - start), 0)
        mCommittedTextBeforeComposingText.setLength(0)
        if (!TextUtils.isEmpty(textBeforeCursor)) {
            // The cursor is not necessarily at the end of the composing text, but we have its
            // position in mExpectedSelStart and mExpectedSelEnd. In this case we want the start
            // of the text, so we should use mExpectedSelStart. In other words, the composing
            // text starts (mExpectedSelStart - start) characters before the end of textBeforeCursor
            val indexOfStartOfComposingText: Int = max(
                (textBeforeCursor!!.length - (expectedSelectionStart - start)).toDouble(),
                0.0
            ).toInt()
            mComposingText.append(
                textBeforeCursor.subSequence(
                    indexOfStartOfComposingText,
                    textBeforeCursor.length
                )
            )
            mCommittedTextBeforeComposingText.append(
                textBeforeCursor.subSequence(0, indexOfStartOfComposingText)
            )
        }
        if (isConnected) {
            mIC!!.setComposingRegion(start, end)
        }
    }

    fun setComposingText(text: CharSequence, newCursorPosition: Int) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit()
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug()
        expectedSelectionStart += text.length - mComposingText.length
        expectedSelectionEnd = expectedSelectionStart
        mComposingText.setLength(0)
        mComposingText.append(text)
        // TODO: support values of newCursorPosition != 1. At this time, this is never called with
        // newCursorPosition != 1.
        if (isConnected) {
            mIC!!.setComposingText(text, newCursorPosition)
        }
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug()
    }

    /**
     * Set the selection of the text editor.
     *
     * Calls through to [InputConnection.setSelection].
     *
     * @param start the character index where the selection should start.
     * @param end the character index where the selection should end.
     * @return Returns true on success, false on failure: either the input connection is no longer
     * valid when setting the selection or when retrieving the text cache at that point, or
     * invalid arguments were passed.
     */
    fun setSelection(start: Int, end: Int): Boolean {
        if (DEBUG_BATCH_NESTING) checkBatchEdit()
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug()
        if (start < 0 || end < 0) {
            return false
        }
        expectedSelectionStart = start
        expectedSelectionEnd = end
        if (isConnected) {
            val isIcValid: Boolean = mIC!!.setSelection(start, end)
            if (!isIcValid) {
                return false
            }
        }
        return reloadTextCache()
    }

    fun commitCorrection(correctionInfo: CorrectionInfo?) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit()
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug()
        // This has no effect on the text field and does not change its content. It only makes
        // TextView flash the text for a second based on indices contained in the argument.
        if (isConnected) {
            mIC!!.commitCorrection(correctionInfo)
        }
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug()
    }

    fun commitCompletion(completionInfo: CompletionInfo) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit()
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug()
        var text: CharSequence? = completionInfo.getText()
        // text should never be null, but just in case, it's better to insert nothing than to crash
        if (null == text) text = ""
        mCommittedTextBeforeComposingText.append(text)
        expectedSelectionStart += text.length - mComposingText.length
        expectedSelectionEnd = expectedSelectionStart
        mComposingText.setLength(0)
        if (isConnected) {
            mIC!!.commitCompletion(completionInfo)
        }
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug()
    }

    @Suppress("unused")
    fun getNgramContextFromNthPreviousWord(
        spacingAndPunctuations: SpacingAndPunctuations, n: Int
    ): NgramContext? {
        mIC = mParent.getCurrentInputConnection()
        if (!isConnected) {
            return NgramContext.EMPTY_PREV_WORDS_INFO
        }
        val prev: CharSequence? = getTextBeforeCursor(NUM_CHARS_TO_GET_BEFORE_CURSOR, 0)
        if (DEBUG_PREVIOUS_TEXT && null != prev) {
            val checkLength: Int = NUM_CHARS_TO_GET_BEFORE_CURSOR - 1
            val reference: String = if (prev.length <= checkLength)
                prev.toString()
            else
                prev.subSequence(prev.length - checkLength, prev.length).toString()
            // TODO: right now the following works because mComposingText holds the part of the
            // composing text that is before the cursor, but this is very confusing. We should
            // fix it.
            val internal: StringBuilder = StringBuilder()
                .append(mCommittedTextBeforeComposingText).append(mComposingText)
            if (internal.length > checkLength) {
                internal.delete(0, internal.length - checkLength)
                if (!(reference == internal.toString())) {
                    val context: String =
                        "Expected text = " + internal + "\nActual text = " + reference
                    (mParent as LatinIME).debugDumpStateAndCrashWithException(context)
                }
            }
        }
        return NgramContextUtils.getNgramContextFromNthPreviousWord(
            prev, spacingAndPunctuations, n
        )
    }

    /**
     * Returns the text surrounding the cursor.
     *
     * @param spacingAndPunctuations the rules for spacing and punctuation
     * @param scriptId the script we consider to be writing words, as one of ScriptUtils.SCRIPT_*
     * @return a range containing the text surrounding the cursor
     */
    fun getWordRangeAtCursor(
        spacingAndPunctuations: SpacingAndPunctuations,
        scriptId: Int
    ): TextRange? {
        mIC = mParent.getCurrentInputConnection()
        if (!isConnected) {
            return null
        }
        val before: CharSequence? = getTextBeforeCursorAndDetectLaggyConnection(
            OPERATION_GET_WORD_RANGE_AT_CURSOR,
            SLOW_INPUT_CONNECTION_ON_PARTIAL_RELOAD_MS,
            NUM_CHARS_TO_GET_BEFORE_CURSOR,
            InputConnection.GET_TEXT_WITH_STYLES
        )
        val after: CharSequence? = getTextAfterCursorAndDetectLaggyConnection(
            OPERATION_GET_WORD_RANGE_AT_CURSOR,
            SLOW_INPUT_CONNECTION_ON_PARTIAL_RELOAD_MS,
            NUM_CHARS_TO_GET_AFTER_CURSOR,
            InputConnection.GET_TEXT_WITH_STYLES
        )
        if (before == null || after == null) {
            return null
        }

        // Going backward, find the first breaking point (separator)
        var startIndexInBefore: Int = before.length
        while (startIndexInBefore > 0) {
            val codePoint: Int = Character.codePointBefore(before, startIndexInBefore)
            if (!isPartOfCompositionForScript(codePoint, spacingAndPunctuations, scriptId)) {
                break
            }
            --startIndexInBefore
            if (Character.isSupplementaryCodePoint(codePoint)) {
                --startIndexInBefore
            }
        }

        // Find last word separator after the cursor
        var endIndexInAfter: Int = -1
        while (++endIndexInAfter < after.length) {
            val codePoint: Int = Character.codePointAt(after, endIndexInAfter)
            if (!isPartOfCompositionForScript(codePoint, spacingAndPunctuations, scriptId)) {
                break
            }
            if (Character.isSupplementaryCodePoint(codePoint)) {
                ++endIndexInAfter
            }
        }

        val hasUrlSpans: Boolean =
            SpannableStringUtils.hasUrlSpans(before, startIndexInBefore, before.length)
                    || SpannableStringUtils.hasUrlSpans(after, 0, endIndexInAfter)
        // We don't use TextUtils#concat because it copies all spans without respect to their
        // nature. If the text includes a PARAGRAPH span and it has been split, then
        // TextUtils#concat will crash when it tries to concat both sides of it.
        return TextRange(
            SpannableStringUtils.concatWithNonParagraphSuggestionSpansOnly(before, after),
            startIndexInBefore, before.length + endIndexInAfter, before.length,
            hasUrlSpans
        )
    }

    fun isCursorTouchingWord(
        spacingAndPunctuations: SpacingAndPunctuations,
        checkTextAfter: Boolean
    ): Boolean {
        if (checkTextAfter && isCursorFollowedByWordCharacter(spacingAndPunctuations)) {
            // If what's after the cursor is a word character, then we're touching a word.
            return true
        }
        val textBeforeCursor: String = mCommittedTextBeforeComposingText.toString()
        var indexOfCodePointInJavaChars: Int = textBeforeCursor.length
        var consideredCodePoint: Int = if (0 == indexOfCodePointInJavaChars)
            Constants.NOT_A_CODE
        else
            textBeforeCursor.codePointBefore(indexOfCodePointInJavaChars)
        // Search for the first non word-connector char
        if (spacingAndPunctuations.isWordConnector(consideredCodePoint)) {
            indexOfCodePointInJavaChars -= Character.charCount(consideredCodePoint)
            consideredCodePoint = if (0 == indexOfCodePointInJavaChars)
                Constants.NOT_A_CODE
            else
                textBeforeCursor.codePointBefore(indexOfCodePointInJavaChars)
        }
        return !(Constants.NOT_A_CODE == consideredCodePoint || spacingAndPunctuations.isWordSeparator(
            consideredCodePoint
        )
                || spacingAndPunctuations.isWordConnector(consideredCodePoint))
    }

    fun isCursorFollowedByWordCharacter(
        spacingAndPunctuations: SpacingAndPunctuations
    ): Boolean {
        val after: CharSequence? = getTextAfterCursor(1, 0)
        if (TextUtils.isEmpty(after)) {
            return false
        }
        val codePointAfterCursor: Int = Character.codePointAt(after, 0)
        if (spacingAndPunctuations.isWordSeparator(codePointAfterCursor)
            || spacingAndPunctuations.isWordConnector(codePointAfterCursor)
        ) {
            return false
        }
        return true
    }

    fun removeTrailingSpace() {
        if (DEBUG_BATCH_NESTING) checkBatchEdit()
        val codePointBeforeCursor: Int = codePointBeforeCursor
        if (Constants.CODE_SPACE == codePointBeforeCursor) {
            deleteTextBeforeCursor(1)
        }
    }

    fun sameAsTextBeforeCursor(text: CharSequence): Boolean {
        val beforeText: CharSequence? = getTextBeforeCursor(text.length, 0)
        return TextUtils.equals(text, beforeText)
    }

    fun revertDoubleSpacePeriod(spacingAndPunctuations: SpacingAndPunctuations): Boolean {
        if (DEBUG_BATCH_NESTING) checkBatchEdit()
        // Here we test whether we indeed have a period and a space before us. This should not
        // be needed, but it's there just in case something went wrong.
        val textBeforeCursor: CharSequence? = getTextBeforeCursor(2, 0)
        if (!TextUtils.equals(
                spacingAndPunctuations.mSentenceSeparatorAndSpace,
                textBeforeCursor
            )
        ) {
            // Theoretically we should not be coming here if there isn't ". " before the
            // cursor, but the application may be changing the text while we are typing, so
            // anything goes. We should not crash.
            Log.d(
                TAG, ("Tried to revert double-space combo but we didn't find \""
                        + spacingAndPunctuations.mSentenceSeparatorAndSpace
                        + "\" just before the cursor.")
            )
            return false
        }
        // Double-space results in ". ". A backspace to cancel this should result in a single
        // space in the text field, so we replace ". " with a single space.
        deleteTextBeforeCursor(2)
        val singleSpace: String = " "
        commitText(singleSpace, 1)
        return true
    }

    fun revertSwapPunctuation(): Boolean {
        if (DEBUG_BATCH_NESTING) checkBatchEdit()
        // Here we test whether we indeed have a space and something else before us. This should not
        // be needed, but it's there just in case something went wrong.
        val textBeforeCursor: CharSequence? = getTextBeforeCursor(2, 0)
        // NOTE: This does not work with surrogate pairs. Hopefully when the keyboard is able to
        // enter surrogate pairs this code will have been removed.
        if (TextUtils.isEmpty(textBeforeCursor)
            || (Constants.CODE_SPACE != textBeforeCursor!!.get(1).code)
        ) {
            // We may only come here if the application is changing the text while we are typing.
            // This is quite a broken case, but not logically impossible, so we shouldn't crash,
            // but some debugging log may be in order.
            Log.d(
                TAG, "Tried to revert a swap of punctuation but we didn't "
                        + "find a space just before the cursor."
            )
            return false
        }
        deleteTextBeforeCursor(2)
        val text: String = " " + textBeforeCursor!!.subSequence(0, 1)
        commitText(text, 1)
        return true
    }

    /**
     * Heuristic to determine if this is an expected update of the cursor.
     *
     * Sometimes updates to the cursor position are late because of their asynchronous nature.
     * This method tries to determine if this update is one, based on the values of the cursor
     * position in the update, and the currently expected position of the cursor according to
     * LatinIME's internal accounting. If this is not a belated expected update, then it should
     * mean that the user moved the cursor explicitly.
     * This is quite robust, but of course it's not perfect. In particular, it will fail in the
     * case we get an update A, the user types in N characters so as to move the cursor to A+N but
     * we don't get those, and then the user places the cursor between A and A+N, and we get only
     * this update and not the ones in-between. This is almost impossible to achieve even trying
     * very very hard.
     *
     * @param oldSelStart The value of the old selection in the update.
     * @param newSelStart The value of the new selection in the update.
     * @param oldSelEnd The value of the old selection end in the update.
     * @param newSelEnd The value of the new selection end in the update.
     * @return whether this is a belated expected update or not.
     */
    fun isBelatedExpectedUpdate(
        oldSelStart: Int, newSelStart: Int,
        oldSelEnd: Int, newSelEnd: Int
    ): Boolean {
        // This update is "belated" if we are expecting it. That is, mExpectedSelStart and
        // mExpectedSelEnd match the new values that the TextView is updating TO.
        if (expectedSelectionStart == newSelStart && expectedSelectionEnd == newSelEnd) return true
        // This update is not belated if mExpectedSelStart and mExpectedSelEnd match the old
        // values, and one of newSelStart or newSelEnd is updated to a different value. In this
        // case, it is likely that something other than the IME has moved the selection endpoint
        // to the new value.
        if (expectedSelectionStart == oldSelStart && expectedSelectionEnd == oldSelEnd && (oldSelStart != newSelStart || oldSelEnd != newSelEnd)) return false
        // If neither of the above two cases hold, then the system may be having trouble keeping up
        // with updates. If 1) the selection is a cursor, 2) newSelStart is between oldSelStart
        // and mExpectedSelStart, and 3) newSelEnd is between oldSelEnd and mExpectedSelEnd, then
        // assume a belated update.
        return (newSelStart == newSelEnd)
                && (newSelStart - oldSelStart) * (expectedSelectionStart - newSelStart) >= 0 && (newSelEnd - oldSelEnd) * (expectedSelectionEnd - newSelEnd) >= 0
    }

    /**
     * Looks at the text just before the cursor to find out if it looks like a URL.
     *
     * The weakest point here is, if we don't have enough text bufferized, we may fail to realize
     * we are in URL situation, but other places in this class have the same limitation and it
     * does not matter too much in the practice.
     */
    fun textBeforeCursorLooksLikeURL(): Boolean {
        return StringUtils.lastPartLooksLikeURL(mCommittedTextBeforeComposingText)
    }

    val isInsideDoubleQuoteOrAfterDigit: Boolean
        /**
         * Looks at the text just before the cursor to find out if we are inside a double quote.
         *
         * As with #textBeforeCursorLooksLikeURL, this is dependent on how much text we have cached.
         * However this won't be a concrete problem in most situations, as the cache is almost always
         * long enough for this use.
         */
        get() {
            return StringUtils.isInsideDoubleQuoteOrAfterDigit(
                mCommittedTextBeforeComposingText
            )
        }

    /**
     * Try to get the text from the editor to expose lies the framework may have been
     * telling us. Concretely, when the device rotates and when the keyboard reopens in the same
     * text field after having been closed with the back key, the frameworks tells us about where
     * the cursor used to be initially in the editor at the time it first received the focus; this
     * may be completely different from the place it is upon rotation. Since we don't have any
     * means to get the real value, try at least to ask the text view for some characters and
     * detect the most damaging cases: when the cursor position is declared to be much smaller
     * than it really is.
     */
    fun tryFixLyingCursorPosition() {
        mIC = mParent.getCurrentInputConnection()
        val textBeforeCursor: CharSequence? = getTextBeforeCursor(
            Constants.EDITOR_CONTENTS_CACHE_SIZE, 0
        )
        val selectedText: CharSequence? =
            if (isConnected) mIC!!.getSelectedText(0 /* flags */) else null
        if (null == textBeforeCursor ||
            (!TextUtils.isEmpty(selectedText) && expectedSelectionEnd == expectedSelectionStart)
        ) {
            // If textBeforeCursor is null, we have no idea what kind of text field we have or if
            // thinking about the "cursor position" actually makes any sense. In this case we
            // remember a meaningless cursor position. Contrast this with an empty string, which is
            // valid and should mean the cursor is at the start of the text.
            // Also, if we expect we don't have a selection but we DO have non-empty selected text,
            // then the framework lied to us about the cursor position. In this case, we should just
            // revert to the most basic behavior possible for the next action (backspace in
            // particular comes to mind), so we remember a meaningless cursor position which should
            // result in degraded behavior from the next input.
            // Interestingly, in either case, chances are any action the user takes next will result
            // in a call to onUpdateSelection, which should set things right.
            expectedSelectionEnd = Constants.NOT_A_CURSOR_POSITION
            expectedSelectionStart = expectedSelectionEnd
        } else {
            val textLength: Int = textBeforeCursor.length
            if (textLength < Constants.EDITOR_CONTENTS_CACHE_SIZE
                && (textLength > expectedSelectionStart
                        || expectedSelectionStart < Constants.EDITOR_CONTENTS_CACHE_SIZE)
            ) {
                // It should not be possible to have only one of those variables be
                // NOT_A_CURSOR_POSITION, so if they are equal, either the selection is zero-sized
                // (simple cursor, no selection) or there is no cursor/we don't know its pos
                val wasEqual: Boolean = expectedSelectionStart == expectedSelectionEnd
                expectedSelectionStart = textLength
                // We can't figure out the value of mLastSelectionEnd :(
                // But at least if it's smaller than mLastSelectionStart something is wrong,
                // and if they used to be equal we also don't want to make it look like there is a
                // selection.
                if (wasEqual || expectedSelectionStart > expectedSelectionEnd) {
                    expectedSelectionEnd = expectedSelectionStart
                }
            }
        }
    }

    override fun performPrivateCommand(action: String?, data: Bundle?): Boolean {
        mIC = mParent.getCurrentInputConnection()
        if (!isConnected) {
            return false
        }
        return mIC!!.performPrivateCommand(action, data)
    }

    /**
     * @return whether there is a selection currently active.
     */
    fun hasSelection(): Boolean {
        return expectedSelectionEnd != expectedSelectionStart
    }

    val isCursorPositionKnown: Boolean
        get() {
            return INVALID_CURSOR_POSITION != expectedSelectionStart
        }

    /**
     * Work around a bug that was present before Jelly Bean upon rotation.
     *
     * Before Jelly Bean, there is a bug where setComposingRegion and other committing
     * functions on the input connection get ignored until the cursor moves. This method works
     * around the bug by wiggling the cursor first, which reactivates the connection and has
     * the subsequent methods work, then restoring it to its original position.
     *
     * On platforms on which this method is not present, this is a no-op.
     */
    fun maybeMoveTheCursorAroundAndRestoreToWorkaroundABug() {
        if (Build.VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN) {
            if (expectedSelectionStart > 0) {
                mIC!!.setSelection(expectedSelectionStart - 1, expectedSelectionStart - 1)
            } else {
                mIC!!.setSelection(expectedSelectionStart + 1, expectedSelectionStart + 1)
            }
            mIC!!.setSelection(expectedSelectionStart, expectedSelectionEnd)
        }
    }

    /**
     * Requests the editor to call back [InputMethodManager.updateCursorAnchorInfo].
     * @param enableMonitor `true` to request the editor to call back the method whenever the
     * cursor/anchor position is changed.
     * @param requestImmediateCallback `true` to request the editor to call back the method
     * as soon as possible to notify the current cursor/anchor position to the input method.
     * @return `true` if the request is accepted. Returns `false` otherwise, which
     * includes "not implemented" or "rejected" or "temporarily unavailable" or whatever which
     * prevents the application from fulfilling the request. (TODO: Improve the API when it turns
     * out that we actually need more detailed error codes)
     */
    fun requestCursorUpdates(
        enableMonitor: Boolean,
        requestImmediateCallback: Boolean
    ): Boolean {
        mIC = mParent.getCurrentInputConnection()
        if (!isConnected) {
            return false
        }
        return InputConnectionCompatUtils.requestCursorUpdates(
            mIC, enableMonitor, requestImmediateCallback
        )
    }

    companion object {
        private const val TAG: String = "RichInputConnection"
        private const val DBG: Boolean = false
        private const val DEBUG_PREVIOUS_TEXT: Boolean = false
        private const val DEBUG_BATCH_NESTING: Boolean = false
        private const val NUM_CHARS_TO_GET_BEFORE_CURSOR: Int = 40
        private const val NUM_CHARS_TO_GET_AFTER_CURSOR: Int = 40
        private val INVALID_CURSOR_POSITION: Int = -1

        /**
         * The amount of time a [.reloadTextCache] call needs to take for the keyboard to enter
         * the [.hasSlowInputConnection] state.
         */
        private const val SLOW_INPUT_CONNECTION_ON_FULL_RELOAD_MS: Long = 1000

        /**
         * The amount of time a [.getTextBeforeCursor] or [.getTextAfterCursor] call needs
         * to take for the keyboard to enter the [.hasSlowInputConnection] state.
         */
        private const val SLOW_INPUT_CONNECTION_ON_PARTIAL_RELOAD_MS: Long = 200

        private const val OPERATION_GET_TEXT_BEFORE_CURSOR: Int = 0
        private const val OPERATION_GET_TEXT_AFTER_CURSOR: Int = 1
        private const val OPERATION_GET_WORD_RANGE_AT_CURSOR: Int = 2
        private const val OPERATION_RELOAD_TEXT_CACHE: Int = 3
        private val OPERATION_NAMES: Array<String> = arrayOf(
            "GET_TEXT_BEFORE_CURSOR",
            "GET_TEXT_AFTER_CURSOR",
            "GET_WORD_RANGE_AT_CURSOR",
            "RELOAD_TEXT_CACHE"
        )

        /**
         * The amount of time the keyboard will persist in the [.hasSlowInputConnection] state
         * after observing a slow InputConnection event.
         */
        private val SLOW_INPUTCONNECTION_PERSIST_MS: Long = TimeUnit.MINUTES.toMillis(10)

        private fun isPartOfCompositionForScript(
            codePoint: Int,
            spacingAndPunctuations: SpacingAndPunctuations, scriptId: Int
        ): Boolean {
            // We always consider word connectors part of compositions.
            return spacingAndPunctuations.isWordConnector(codePoint) // Otherwise, it's part of composition if it's part of script and not a separator.
                    || (!spacingAndPunctuations.isWordSeparator(codePoint)
                    && ScriptUtils.isLetterPartOfScript(codePoint, scriptId))
        }
    }
}
