/*
 * Copyright (C) 2009 The Android Open Source Project
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
package com.android.inputmethod.pinyin

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import com.android.inputmethod.pinyin.ComposingView.ComposingStatus
import ee.oyatl.ime.fusion.R
import java.util.Vector
import kotlin.math.abs

/**
 * Main class of the Pinyin input method.
 */
class PinyinIME : InputMethodService() {
    /**
     * Necessary environment configurations like screen size for this IME.
     */
    private var mEnvironment: Environment? = null

    /**
     * Used to switch input mode.
     */
    private var mInputModeSwitcher: InputModeSwitcher? = null

    /**
     * Soft keyboard container view to host real soft keyboard view.
     */
    private var mSkbContainer: SkbContainer? = null

    /**
     * The floating container which contains the composing view. If necessary,
     * some other view like candiates container can also be put here.
     */
    private var mFloatingContainer: LinearLayout? = null

    /**
     * View to show the composing string.
     */
    private var mComposingView: ComposingView? = null

    /**
     * Window to show the composing string.
     */
    private var mFloatingWindow: PopupWindow? = null

    /**
     * Used to show the floating window.
     */
    private val mFloatingWindowTimer = PopupTimer()

    /**
     * View to show candidates list.
     */
    private var mCandidatesContainer: CandidatesContainer? = null

    /**
     * Balloon used when user presses a candidate.
     */
    private var mCandidatesBalloon: BalloonHint? = null

    /**
     * Used to notify the input method when the user touch a candidate.
     */
    private var mChoiceNotifier: ChoiceNotifier? = null

    /**
     * Used to notify gestures from soft keyboard.
     */
    private var mGestureListenerSkb: OnGestureListener? = null

    /**
     * Used to notify gestures from candidates view.
     */
    private var mGestureListenerCandidates: OnGestureListener? = null

    /**
     * The on-screen movement gesture detector for soft keyboard.
     */
    private var mGestureDetectorSkb: GestureDetector? = null

    /**
     * The on-screen movement gesture detector for candidates view.
     */
    private var mGestureDetectorCandidates: GestureDetector? = null

    /**
     * Option dialog to choose settings and other IMEs.
     */
    private var mOptionsDialog: AlertDialog? = null

    /**
     * Connection used to bind the decoding service.
     */
    private var mPinyinDecoderServiceConnection: PinyinDecoderServiceConnection? = null

    /**
     * The current IME status.
     *
     * @see com.android.inputmethod.pinyin.PinyinIME.ImeState
     */
    private var mImeState = ImeState.STATE_IDLE

    /**
     * The decoding information, include spelling(Pinyin) string, decoding
     * result, etc.
     */
    private val mDecInfo = DecodingInfo()

    /**
     * For English input.
     */
    private var mImEn: EnglishInputProcessor? = null

    // receive ringer mode changes
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            SoundManager.getInstance(context)!!.updateRingerMode()
        }
    }

    override fun onCreate() {
        mEnvironment = Environment.instance
        if (mEnvironment!!.needDebug()) {
            Log.d(TAG, "onCreate.")
        }
        super.onCreate()

        startPinyinDecoderService()
        mImEn = EnglishInputProcessor()
        Settings.getInstance(
            PreferenceManager
                .getDefaultSharedPreferences(applicationContext)
        )

        mInputModeSwitcher = InputModeSwitcher(this)
        mChoiceNotifier = ChoiceNotifier(this)
        mGestureListenerSkb = OnGestureListener(false)
        mGestureListenerCandidates = OnGestureListener(true)
        mGestureDetectorSkb = GestureDetector(this, mGestureListenerSkb!!)
        mGestureDetectorCandidates = GestureDetector(
            this,
            mGestureListenerCandidates!!
        )

        mEnvironment!!.onConfigurationChanged(
            resources.configuration,
            this
        )
    }

    override fun onDestroy() {
        if (mEnvironment!!.needDebug()) {
            Log.d(TAG, "onDestroy.")
        }
        unbindService(mPinyinDecoderServiceConnection!!)
        Settings.releaseInstance()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val env: Environment = Environment.instance
        if (mEnvironment!!.needDebug()) {
            Log.d(TAG, "onConfigurationChanged")
            Log.d(TAG, "--last config: " + env.configuration.toString())
            Log.d(TAG, "---new config: $newConfig")
        }
        // We need to change the local environment first so that UI components
        // can get the environment instance to handle size issues. When
        // super.onConfigurationChanged() is called, onCreateCandidatesView()
        // and onCreateInputView() will be executed if necessary.
        env.onConfigurationChanged(newConfig, this)

        // Clear related UI of the previous configuration.
        if (null != mSkbContainer) {
            mSkbContainer!!.dismissPopups()
        }
        if (null != mCandidatesBalloon) {
            mCandidatesBalloon!!.dismiss()
        }
        super.onConfigurationChanged(newConfig)
        resetToIdleState(false)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (processKey(event, 0 != event.repeatCount)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (processKey(event, true)) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun processKey(event: KeyEvent, realAction: Boolean): Boolean {
        if (ImeState.STATE_BYPASS == mImeState) return false

        val keyCode = event.keyCode
        // SHIFT-SPACE is used to switch between Chinese and English
        // when HKB is on.
        if (KeyEvent.KEYCODE_SPACE == keyCode && event.isShiftPressed) {
            if (!realAction) return true

            updateIcon(mInputModeSwitcher!!.switchLanguageWithHkb())
            resetToIdleState(false)

            val allMetaState = (KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
                    or KeyEvent.META_ALT_RIGHT_ON or KeyEvent.META_SHIFT_ON
                    or KeyEvent.META_SHIFT_LEFT_ON
                    or KeyEvent.META_SHIFT_RIGHT_ON or KeyEvent.META_SYM_ON)
            currentInputConnection.clearMetaKeyStates(allMetaState)
            return true
        }

        // If HKB is on to input English, by-pass the key event so that
        // default key listener will handle it.
        if (mInputModeSwitcher!!.isEnglishWithHkb) {
            return false
        }

        if (processFunctionKeys(keyCode, realAction)) {
            return true
        }

        var keyChar = 0
        if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
            keyChar = keyCode - KeyEvent.KEYCODE_A + 'a'.code
        } else if (keyCode >= KeyEvent.KEYCODE_0
            && keyCode <= KeyEvent.KEYCODE_9
        ) {
            keyChar = keyCode - KeyEvent.KEYCODE_0 + '0'.code
        } else if (keyCode == KeyEvent.KEYCODE_COMMA) {
            keyChar = ','.code
        } else if (keyCode == KeyEvent.KEYCODE_PERIOD) {
            keyChar = '.'.code
        } else if (keyCode == KeyEvent.KEYCODE_SPACE) {
            keyChar = ' '.code
        } else if (keyCode == KeyEvent.KEYCODE_APOSTROPHE) {
            keyChar = '\''.code
        }

        if (mInputModeSwitcher!!.isEnglishWithSkb) {
            return mImEn!!.processKey(
                currentInputConnection, event,
                mInputModeSwitcher!!.isEnglishUpperCaseWithSkb, realAction
            )
        } else if (mInputModeSwitcher!!.isChineseText) {
            if (mImeState == ImeState.STATE_IDLE ||
                mImeState == ImeState.STATE_APP_COMPLETION
            ) {
                mImeState = ImeState.STATE_IDLE
                return processStateIdle(keyChar, keyCode, event, realAction)
            } else if (mImeState == ImeState.STATE_INPUT) {
                return processStateInput(keyChar, keyCode, event, realAction)
            } else if (mImeState == ImeState.STATE_PREDICT) {
                return processStatePredict(keyChar, keyCode, event, realAction)
            } else if (mImeState == ImeState.STATE_COMPOSING) {
                return processStateEditComposing(
                    keyChar, keyCode, event,
                    realAction
                )
            }
        } else {
            if (0 != keyChar && realAction) {
                commitResultText(keyChar.toChar().toString())
            }
        }

        return false
    }

    // keyCode can be from both hard key or soft key.
    private fun processFunctionKeys(keyCode: Int, realAction: Boolean): Boolean {
        // Back key is used to dismiss all popup UI in a soft keyboard.
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isInputViewShown) {
                if (mSkbContainer!!.handleBack(realAction)) return true
            }
        }

        // Chinese related input is handle separately.
        if (mInputModeSwitcher!!.isChineseText) {
            return false
        }

        if (null != mCandidatesContainer && mCandidatesContainer!!.isShown
            && !mDecInfo.isCandidatesListEmpty
        ) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                if (!realAction) return true

                chooseCandidate(-1)
                return true
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (!realAction) return true
                mCandidatesContainer!!.activeCurseBackward()
                return true
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (!realAction) return true
                mCandidatesContainer!!.activeCurseForward()
                return true
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                if (!realAction) return true
                mCandidatesContainer!!.pageBackward(false, true)
                return true
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (!realAction) return true
                mCandidatesContainer!!.pageForward(false, true)
                return true
            }

            if (keyCode == KeyEvent.KEYCODE_DEL &&
                ImeState.STATE_PREDICT == mImeState
            ) {
                if (!realAction) return true
                resetToIdleState(false)
                return true
            }
        } else {
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                if (!realAction) return true
                if (SIMULATE_KEY_DELETE) {
                    simulateKeyEventDownUp(keyCode)
                } else {
                    currentInputConnection.deleteSurroundingText(1, 0)
                }
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (!realAction) return true
                sendKeyChar('\n')
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_SPACE) {
                if (!realAction) return true
                sendKeyChar(' ')
                return true
            }
        }

        return false
    }

    private fun processStateIdle(
        keyChar: Int, keyCode: Int, event: KeyEvent,
        realAction: Boolean
    ): Boolean {
        // In this status, when user presses keys in [a..z], the status will
        // change to input state.
        if (keyChar >= 'a'.code && keyChar <= 'z'.code && !event.isAltPressed) {
            if (!realAction) return true
            mDecInfo.addSplChar(keyChar.toChar(), true)
            chooseAndUpdate(-1)
            return true
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            if (!realAction) return true
            if (SIMULATE_KEY_DELETE) {
                simulateKeyEventDownUp(keyCode)
            } else {
                currentInputConnection.deleteSurroundingText(1, 0)
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (!realAction) return true
            sendKeyChar('\n')
            return true
        } else if (keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT || keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            return true
        } else if (event.isAltPressed) {
            val fullwidth_char = KeyMapDream.getChineseLabel(keyCode)
            if (0 != fullwidth_char.code) {
                if (realAction) {
                    val result = fullwidth_char.toString()
                    commitResultText(result)
                }
                return true
            } else {
                if (keyCode >= KeyEvent.KEYCODE_A
                    && keyCode <= KeyEvent.KEYCODE_Z
                ) {
                    return true
                }
            }
        } else if (keyChar != 0 && keyChar != '\t'.code) {
            if (realAction) {
                if (keyChar == ','.code || keyChar == '.'.code) {
                    inputCommaPeriod("", keyChar, false, ImeState.STATE_IDLE)
                } else {
                    if (0 != keyChar) {
                        val result = keyChar.toChar().toString()
                        commitResultText(result)
                    }
                }
            }
            return true
        }
        return false
    }

    private fun processStateInput(
        keyChar: Int, keyCode: Int, event: KeyEvent,
        realAction: Boolean
    ): Boolean {
        // If ALT key is pressed, input alternative key. But if the
        // alternative key is quote key, it will be used for input a splitter
        // in Pinyin string.
        var keyChar = keyChar
        if (event.isAltPressed) {
            if ('\''.code != event.getUnicodeChar(event.metaState)) {
                if (realAction) {
                    val fullwidth_char = KeyMapDream.getChineseLabel(keyCode)
                    if (0 != fullwidth_char.code) {
                        commitResultText(
                            mDecInfo
                                .getCurrentFullSent(
                                    mCandidatesContainer!!.activeCandiatePos
                                ) + fullwidth_char.toString()
                        )
                        resetToIdleState(false)
                    }
                }
                return true
            } else {
                keyChar = '\''.code
            }
        }

        if (keyChar >= 'a'.code && keyChar <= 'z'.code || keyChar == '\''.code
            && !mDecInfo.charBeforeCursorIsSeparator() || keyCode == KeyEvent.KEYCODE_DEL
        ) {
            if (!realAction) return true
            return processSurfaceChange(keyChar, keyCode)
        } else if (keyChar == ','.code || keyChar == '.'.code) {
            if (!realAction) return true
            inputCommaPeriod(
                mDecInfo.getCurrentFullSent(
                    mCandidatesContainer!!.activeCandiatePos
                ), keyChar, true,
                ImeState.STATE_IDLE
            )
            return true
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (!realAction) return true

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                mCandidatesContainer!!.activeCurseBackward()
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                mCandidatesContainer!!.activeCurseForward()
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                // If it has been the first page, a up key will shift
                // the state to edit composing string.
                if (!mCandidatesContainer!!.pageBackward(false, true)) {
                    mCandidatesContainer!!.enableActiveHighlight(false)
                    changeToStateComposing(true)
                    updateComposingText(true)
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                mCandidatesContainer!!.pageForward(false, true)
            }
            return true
        } else if (keyCode >= KeyEvent.KEYCODE_1
            && keyCode <= KeyEvent.KEYCODE_9
        ) {
            if (!realAction) return true

            var activePos = keyCode - KeyEvent.KEYCODE_1
            val currentPage = mCandidatesContainer!!.currentPage
            if (activePos < mDecInfo.getCurrentPageSize(currentPage)) {
                activePos = (activePos
                        + mDecInfo.getCurrentPageStart(currentPage))
                if (activePos >= 0) {
                    chooseAndUpdate(activePos)
                }
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (!realAction) return true
            if (mInputModeSwitcher!!.isEnterNoramlState) {
                commitResultText(mDecInfo.originalSplStr.toString())
                resetToIdleState(false)
            } else {
                commitResultText(
                    mDecInfo
                        .getCurrentFullSent(
                            mCandidatesContainer!!.activeCandiatePos
                        )
                )
                sendKeyChar('\n')
                resetToIdleState(false)
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
            || keyCode == KeyEvent.KEYCODE_SPACE
        ) {
            if (!realAction) return true
            chooseCandidate(-1)
            return true
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!realAction) return true
            resetToIdleState(false)
            requestHideSelf(0)
            return true
        }
        return false
    }

    private fun processStatePredict(
        keyChar: Int, keyCode: Int,
        event: KeyEvent, realAction: Boolean
    ): Boolean {
        if (!realAction) return true

        // If ALT key is pressed, input alternative key.
        if (event.isAltPressed) {
            val fullwidth_char = KeyMapDream.getChineseLabel(keyCode)
            if (0 != fullwidth_char.code) {
                commitResultText(
                    mDecInfo.getCandidate(
                        mCandidatesContainer!!.activeCandiatePos
                    ) + fullwidth_char.toString()
                )
                resetToIdleState(false)
            }
            return true
        }

        // In this status, when user presses keys in [a..z], the status will
        // change to input state.
        if (keyChar >= 'a'.code && keyChar <= 'z'.code) {
            changeToStateInput(true)
            mDecInfo.addSplChar(keyChar.toChar(), true)
            chooseAndUpdate(-1)
        } else if (keyChar == ','.code || keyChar == '.'.code) {
            inputCommaPeriod("", keyChar, true, ImeState.STATE_IDLE)
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                mCandidatesContainer!!.activeCurseBackward()
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                mCandidatesContainer!!.activeCurseForward()
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                mCandidatesContainer!!.pageBackward(false, true)
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                mCandidatesContainer!!.pageForward(false, true)
            }
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            resetToIdleState(false)
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            resetToIdleState(false)
            requestHideSelf(0)
        } else if (keyCode >= KeyEvent.KEYCODE_1
            && keyCode <= KeyEvent.KEYCODE_9
        ) {
            var activePos = keyCode - KeyEvent.KEYCODE_1
            val currentPage = mCandidatesContainer!!.currentPage
            if (activePos < mDecInfo.getCurrentPageSize(currentPage)) {
                activePos = (activePos
                        + mDecInfo.getCurrentPageStart(currentPage))
                if (activePos >= 0) {
                    chooseAndUpdate(activePos)
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            sendKeyChar('\n')
            resetToIdleState(false)
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
            || keyCode == KeyEvent.KEYCODE_SPACE
        ) {
            chooseCandidate(-1)
        }

        return true
    }

    private fun processStateEditComposing(
        keyChar: Int, keyCode: Int,
        event: KeyEvent, realAction: Boolean
    ): Boolean {
        var keyChar = keyChar
        if (!realAction) return true

        val cmpsvStatus =
            mComposingView!!.composingStatus

        // If ALT key is pressed, input alternative key. But if the
        // alternative key is quote key, it will be used for input a splitter
        // in Pinyin string.
        if (event.isAltPressed) {
            if ('\''.code != event.getUnicodeChar(event.metaState)) {
                val fullwidth_char = KeyMapDream.getChineseLabel(keyCode)
                if (0 != fullwidth_char.code) {
                    val retStr = if (ComposingStatus.SHOW_STRING_LOWERCASE ==
                        cmpsvStatus
                    ) {
                        mDecInfo.originalSplStr.toString()
                    } else {
                        mDecInfo.composingStr
                    }
                    commitResultText(retStr + fullwidth_char.toString())
                    resetToIdleState(false)
                }
                return true
            } else {
                keyChar = '\''.code
            }
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (!mDecInfo.selectionFinished()) {
                changeToStateInput(true)
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
            || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
        ) {
            mComposingView!!.moveCursor(keyCode)
        } else if ((keyCode == KeyEvent.KEYCODE_ENTER && mInputModeSwitcher!!.isEnterNoramlState)
            || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_SPACE
        ) {
            if (ComposingStatus.SHOW_STRING_LOWERCASE == cmpsvStatus) {
                val str = mDecInfo.originalSplStr.toString()
                if (!tryInputRawUnicode(str)) {
                    commitResultText(str)
                }
            } else if (ComposingStatus.EDIT_PINYIN == cmpsvStatus) {
                val str = mDecInfo.composingStr
                if (!tryInputRawUnicode(str!!)) {
                    commitResultText(str)
                }
            } else {
                commitResultText(mDecInfo.composingStr)
            }
            resetToIdleState(false)
        } else if (keyCode == KeyEvent.KEYCODE_ENTER
            && !mInputModeSwitcher!!.isEnterNoramlState
        ) {
            val retStr = if (!mDecInfo.isCandidatesListEmpty) {
                mDecInfo.getCurrentFullSent(
                    mCandidatesContainer!!.activeCandiatePos
                )
            } else {
                mDecInfo.composingStr
            }
            commitResultText(retStr)
            sendKeyChar('\n')
            resetToIdleState(false)
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            resetToIdleState(false)
            requestHideSelf(0)
            return true
        } else {
            return processSurfaceChange(keyChar, keyCode)
        }
        return true
    }

    private fun tryInputRawUnicode(str: String): Boolean {
        if (str.length > 7) {
            if (str.substring(0, 7).compareTo("unicode") == 0) {
                try {
                    var digitStr = str.substring(7)
                    var startPos = 0
                    var radix = 10
                    if (digitStr.length > 2 && digitStr[0] == '0' && digitStr[1] == 'x') {
                        startPos = 2
                        radix = 16
                    }
                    digitStr = digitStr.substring(startPos)
                    val unicode = digitStr.toInt(radix)
                    if (unicode > 0) {
                        val low = (unicode and 0x0000ffff).toChar()
                        val high = ((unicode and -0x10000) shr 16).toChar()
                        commitResultText(low.toString())
                        if (0 != high.code) {
                            commitResultText(high.toString())
                        }
                    }
                    return true
                } catch (e: NumberFormatException) {
                    return false
                }
            } else if (str.substring(str.length - 7, str.length).compareTo(
                    "unicode"
                ) == 0
            ) {
                var resultStr = ""
                for (pos in 0 until str.length - 7) {
                    if (pos > 0) {
                        resultStr += " "
                    }

                    resultStr += "0x" + Integer.toHexString(str[pos].code)
                }
                commitResultText(resultStr.toString())
                return true
            }
        }
        return false
    }

    private fun processSurfaceChange(keyChar: Int, keyCode: Int): Boolean {
        if (mDecInfo.isSplStrFull && KeyEvent.KEYCODE_DEL != keyCode) {
            return true
        }

        if ((keyChar >= 'a'.code && keyChar <= 'z'.code)
            || (keyChar == '\''.code && !mDecInfo.charBeforeCursorIsSeparator())
            || (((keyChar >= '0'.code && keyChar <= '9'.code) || keyChar == ' '.code) && ImeState.STATE_COMPOSING == mImeState)
        ) {
            mDecInfo.addSplChar(keyChar.toChar(), false)
            chooseAndUpdate(-1)
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            mDecInfo.prepareDeleteBeforeCursor()
            chooseAndUpdate(-1)
        }
        return true
    }

    private fun changeToStateComposing(updateUi: Boolean) {
        mImeState = ImeState.STATE_COMPOSING
        if (!updateUi) return

        if (null != mSkbContainer && mSkbContainer!!.isShown) {
            mSkbContainer!!.toggleCandidateMode(true)
        }
    }

    private fun changeToStateInput(updateUi: Boolean) {
        mImeState = ImeState.STATE_INPUT
        if (!updateUi) return

        if (null != mSkbContainer && mSkbContainer!!.isShown) {
            mSkbContainer!!.toggleCandidateMode(true)
        }
        showCandidateWindow(true)
    }

    private fun simulateKeyEventDownUp(keyCode: Int) {
        val ic = currentInputConnection ?: return

        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun commitResultText(resultText: String?) {
        val ic = currentInputConnection
        ic?.commitText(resultText, 1)
        if (null != mComposingView) {
            mComposingView!!.visibility = View.INVISIBLE
            mComposingView!!.invalidate()
        }
    }

    private fun updateComposingText(visible: Boolean) {
        if (!visible) {
            mComposingView!!.visibility = View.INVISIBLE
        } else {
            mComposingView!!.setDecodingInfo(mDecInfo, mImeState)
            mComposingView!!.visibility = View.VISIBLE
        }
        mComposingView!!.invalidate()
    }

    private fun inputCommaPeriod(
        preEdit: String, keyChar: Int,
        dismissCandWindow: Boolean, nextState: ImeState
    ) {
        var preEdit: String? = preEdit
        preEdit += if (keyChar == ','.code) '\uff0c'
        else if (keyChar == '.'.code) '\u3002'
        else return
        commitResultText(preEdit)
        if (dismissCandWindow) resetCandidateWindow()
        mImeState = nextState
    }

    private fun resetToIdleState(resetInlineText: Boolean) {
        if (ImeState.STATE_IDLE == mImeState) return

        mImeState = ImeState.STATE_IDLE
        mDecInfo.reset()

        if (null != mComposingView) mComposingView!!.reset()
        if (resetInlineText) commitResultText("")
        resetCandidateWindow()
    }

    private fun chooseAndUpdate(candId: Int) {
        if (!mInputModeSwitcher!!.isChineseText) {
            val choice = mDecInfo.getCandidate(candId)
            if (null != choice) {
                commitResultText(choice)
            }
            resetToIdleState(false)
            return
        }

        if (ImeState.STATE_PREDICT != mImeState) {
            // Get result candidate list, if choice_id < 0, do a new decoding.
            // If choice_id >=0, select the candidate, and get the new candidate
            // list.
            mDecInfo.chooseDecodingCandidate(candId)
        } else {
            // Choose a prediction item.
            mDecInfo.choosePredictChoice(candId)
        }

        if (mDecInfo.composingStr!!.length > 0) {
            val resultStr = mDecInfo.composingStrActivePart

            // choiceId >= 0 means user finishes a choice selection.
            if (candId >= 0 && mDecInfo.canDoPrediction()) {
                commitResultText(resultStr)
                mImeState = ImeState.STATE_PREDICT
                if (null != mSkbContainer && mSkbContainer!!.isShown) {
                    mSkbContainer!!.toggleCandidateMode(false)
                }
                // Try to get the prediction list.
                if (Settings.prediction) {
                    val ic = currentInputConnection
                    if (null != ic) {
                        val cs = ic.getTextBeforeCursor(3, 0)
                        if (null != cs) {
                            mDecInfo.preparePredicts(cs)
                        }
                    }
                } else {
                    mDecInfo.resetCandidates()
                }

                if (mDecInfo.mCandidatesList.size > 0) {
                    showCandidateWindow(false)
                } else {
                    resetToIdleState(false)
                }
            } else {
                if (ImeState.STATE_IDLE == mImeState) {
                    if (mDecInfo.splStrDecodedLen == 0) {
                        changeToStateComposing(true)
                    } else {
                        changeToStateInput(true)
                    }
                } else {
                    if (mDecInfo.selectionFinished()) {
                        changeToStateComposing(true)
                    }
                }
                showCandidateWindow(true)
            }
        } else {
            resetToIdleState(false)
        }
    }

    // If activeCandNo is less than 0, get the current active candidate number
    // from candidate view, otherwise use activeCandNo.
    private fun chooseCandidate(activeCandNo: Int) {
        var activeCandNo = activeCandNo
        if (activeCandNo < 0) {
            activeCandNo = mCandidatesContainer!!.activeCandiatePos
        }
        if (activeCandNo >= 0) {
            chooseAndUpdate(activeCandNo)
        }
    }

    private fun startPinyinDecoderService(): Boolean {
        if (null == mDecInfo.mIPinyinDecoderService) {
            val serviceIntent = Intent()
            serviceIntent.setClass(this, PinyinDecoderService::class.java)

            if (null == mPinyinDecoderServiceConnection) {
                mPinyinDecoderServiceConnection = PinyinDecoderServiceConnection()
            }

            // Bind service
            return if (bindService(
                    serviceIntent, mPinyinDecoderServiceConnection!!,
                    BIND_AUTO_CREATE
                )
            ) {
                true
            } else {
                false
            }
        }
        return true
    }

    override fun onCreateCandidatesView(): View {
        if (mEnvironment!!.needDebug()) {
            Log.d(TAG, "onCreateCandidatesView.")
        }

        val inflater = layoutInflater
        // Inflate the floating container view
        mFloatingContainer = inflater.inflate(
            R.layout.floating_container, null
        ) as LinearLayout

        // The first child is the composing view.
        mComposingView = mFloatingContainer!!.getChildAt(0) as ComposingView

        mCandidatesContainer = inflater.inflate(
            R.layout.candidates_container, null
        ) as CandidatesContainer

        // Create balloon hint for candidates view.
        mCandidatesBalloon = BalloonHint(
            this, mCandidatesContainer,
            MeasureSpec.UNSPECIFIED
        )
        mCandidatesBalloon!!.setBalloonBackground(
            resources.getDrawable(
                R.drawable.candidate_balloon_bg
            )
        )
        mCandidatesContainer!!.initialize(
            mChoiceNotifier, mCandidatesBalloon,
            mGestureDetectorCandidates
        )

        // The floating window
        if (null != mFloatingWindow && mFloatingWindow!!.isShowing) {
            mFloatingWindowTimer.cancelShowing()
            mFloatingWindow!!.dismiss()
        }
        mFloatingWindow = PopupWindow(this)
        mFloatingWindow!!.isClippingEnabled = false
        mFloatingWindow!!.setBackgroundDrawable(null)
        mFloatingWindow!!.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        mFloatingWindow!!.contentView = mFloatingContainer

        setCandidatesViewShown(true)
        return mCandidatesContainer!!
    }

    fun responseSoftKeyEvent(sKey: SoftKey?) {
        if (null == sKey) return

        val ic = currentInputConnection ?: return

        val keyCode = sKey.keyCode
        // Process some general keys, including KEYCODE_DEL, KEYCODE_SPACE,
        // KEYCODE_ENTER and KEYCODE_DPAD_CENTER.
        if (sKey.isKeyCodeKey) {
            if (processFunctionKeys(keyCode, true)) return
        }

        if (sKey.isUserDefKey) {
            updateIcon(mInputModeSwitcher!!.switchModeForUserKey(keyCode))
            resetToIdleState(false)
            mSkbContainer!!.updateInputMode()
        } else {
            if (sKey.isKeyCodeKey) {
                val eDown = KeyEvent(
                    0, 0, KeyEvent.ACTION_DOWN,
                    keyCode, 0, 0, 0, 0, KeyEvent.FLAG_SOFT_KEYBOARD
                )
                val eUp = KeyEvent(
                    0, 0, KeyEvent.ACTION_UP, keyCode,
                    0, 0, 0, 0, KeyEvent.FLAG_SOFT_KEYBOARD
                )

                onKeyDown(keyCode, eDown)
                onKeyUp(keyCode, eUp)
            } else if (sKey.isUniStrKey) {
                var kUsed = false
                val keyLabel = sKey.keyLabel
                if (mInputModeSwitcher!!.isChineseTextWithSkb
                    && (ImeState.STATE_INPUT == mImeState || ImeState.STATE_COMPOSING == mImeState)
                ) {
                    if (mDecInfo.length() > 0 && keyLabel!!.length == 1 && keyLabel[0] == '\'') {
                        processSurfaceChange('\''.code, 0)
                        kUsed = true
                    }
                }
                if (!kUsed) {
                    if (ImeState.STATE_INPUT == mImeState) {
                        commitResultText(
                            mDecInfo
                                .getCurrentFullSent(
                                    mCandidatesContainer!!.activeCandiatePos
                                )
                        )
                    } else if (ImeState.STATE_COMPOSING == mImeState) {
                        commitResultText(mDecInfo.composingStr)
                    }
                    commitResultText(keyLabel)
                    resetToIdleState(false)
                }
            }

            // If the current soft keyboard is not sticky, IME needs to go
            // back to the previous soft keyboard automatically.
            if (!mSkbContainer!!.isCurrentSkbSticky) {
                updateIcon(mInputModeSwitcher!!.requestBackToPreviousSkb())
                resetToIdleState(false)
                mSkbContainer!!.updateInputMode()
            }
        }
    }

    private fun showCandidateWindow(showComposingView: Boolean) {
        if (mEnvironment!!.needDebug()) {
            Log.d(
                TAG, "Candidates window is shown. Parent = "
                        + mCandidatesContainer
            )
        }

        setCandidatesViewShown(true)

        if (null != mSkbContainer) mSkbContainer!!.requestLayout()

        if (null == mCandidatesContainer) {
            resetToIdleState(false)
            return
        }

        updateComposingText(showComposingView)
        mCandidatesContainer!!.showCandidates(
            mDecInfo,
            ImeState.STATE_COMPOSING != mImeState
        )
        mFloatingWindowTimer.postShowFloatingWindow()
    }

    private fun dismissCandidateWindow() {
        if (mEnvironment!!.needDebug()) {
            Log.d(TAG, "Candidates window is to be dismissed")
        }
        if (null == mCandidatesContainer) return
        try {
            mFloatingWindowTimer.cancelShowing()
            mFloatingWindow!!.dismiss()
        } catch (e: Exception) {
            Log.e(TAG, "Fail to show the PopupWindow.")
        }
        setCandidatesViewShown(false)

        if (null != mSkbContainer && mSkbContainer!!.isShown) {
            mSkbContainer!!.toggleCandidateMode(false)
        }
    }

    private fun resetCandidateWindow() {
        if (mEnvironment!!.needDebug()) {
            Log.d(TAG, "Candidates window is to be reset")
        }
        if (null == mCandidatesContainer) return
        try {
            mFloatingWindowTimer.cancelShowing()
            mFloatingWindow!!.dismiss()
        } catch (e: Exception) {
            Log.e(TAG, "Fail to show the PopupWindow.")
        }

        if (null != mSkbContainer && mSkbContainer!!.isShown) {
            mSkbContainer!!.toggleCandidateMode(false)
        }

        mDecInfo.resetCandidates()

        if (null != mCandidatesContainer && mCandidatesContainer!!.isShown) {
            showCandidateWindow(false)
        }
    }

    private fun updateIcon(iconId: Int) {
        if (iconId > 0) {
            showStatusIcon(iconId)
        } else {
            hideStatusIcon()
        }
    }

    override fun onCreateInputView(): View {
        if (mEnvironment!!.needDebug()) {
            Log.d(TAG, "onCreateInputView.")
        }
        val inflater = layoutInflater
        mSkbContainer = inflater.inflate(
            R.layout.skb_container,
            null
        ) as SkbContainer
        mSkbContainer!!.setService(this)
        mSkbContainer!!.setInputModeSwitcher(mInputModeSwitcher)
        mSkbContainer!!.setGestureDetector(mGestureDetectorSkb)
        return mSkbContainer!!
    }

    override fun onStartInput(editorInfo: EditorInfo, restarting: Boolean) {
        if (mEnvironment!!.needDebug()) {
            Log.d(
                TAG, ("onStartInput " + " ccontentType: "
                        + editorInfo.inputType.toString() + " Restarting:"
                        + restarting.toString())
            )
        }
        updateIcon(mInputModeSwitcher!!.requestInputWithHkb(editorInfo))
        resetToIdleState(false)
    }

    override fun onStartInputView(editorInfo: EditorInfo, restarting: Boolean) {
        if (mEnvironment!!.needDebug()) {
            Log.d(
                TAG, ("onStartInputView " + " contentType: "
                        + editorInfo.inputType.toString() + " Restarting:"
                        + restarting.toString())
            )
        }
        updateIcon(mInputModeSwitcher!!.requestInputWithSkb(editorInfo))
        resetToIdleState(false)
        mSkbContainer!!.updateInputMode()
        setCandidatesViewShown(false)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        if (mEnvironment!!.needDebug()) {
            Log.d(TAG, "onFinishInputView.")
        }
        resetToIdleState(false)
        super.onFinishInputView(finishingInput)
    }

    override fun onFinishInput() {
        if (mEnvironment!!.needDebug()) {
            Log.d(TAG, "onFinishInput.")
        }
        resetToIdleState(false)
        super.onFinishInput()
    }

    override fun onFinishCandidatesView(finishingInput: Boolean) {
        if (mEnvironment!!.needDebug()) {
            Log.d(TAG, "onFinishCandidateView.")
        }
        resetToIdleState(false)
        super.onFinishCandidatesView(finishingInput)
    }

    override fun onDisplayCompletions(completions: Array<CompletionInfo>?) {
        if (!isFullscreenMode) return
        if (null == completions || completions.size <= 0) return
        if (null == mSkbContainer || !mSkbContainer!!.isShown) return

        if (!mInputModeSwitcher!!.isChineseText || ImeState.STATE_IDLE == mImeState || ImeState.STATE_PREDICT == mImeState) {
            mImeState = ImeState.STATE_APP_COMPLETION
            mDecInfo.prepareAppCompletions(completions)
            showCandidateWindow(false)
        }
    }

    private fun onChoiceTouched(activeCandNo: Int) {
        if (mImeState == ImeState.STATE_COMPOSING) {
            changeToStateInput(true)
        } else if (mImeState == ImeState.STATE_INPUT
            || mImeState == ImeState.STATE_PREDICT
        ) {
            chooseCandidate(activeCandNo)
        } else if (mImeState == ImeState.STATE_APP_COMPLETION) {
            if (null != mDecInfo.mAppCompletions && activeCandNo >= 0 && activeCandNo < mDecInfo.mAppCompletions!!.size) {
                val ci = mDecInfo.mAppCompletions!![activeCandNo]
                if (null != ci) {
                    val ic = currentInputConnection
                    ic.commitCompletion(ci)
                }
            }
            resetToIdleState(false)
        }
    }

    override fun requestHideSelf(flags: Int) {
        if (mEnvironment!!.needDebug()) {
            Log.d(TAG, "DimissSoftInput.")
        }
        dismissCandidateWindow()
        if (null != mSkbContainer && mSkbContainer!!.isShown) {
            mSkbContainer!!.dismissPopups()
        }
        super.requestHideSelf(flags)
    }

    fun showOptionsMenu() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(true)
        builder.setIcon(R.drawable.app_icon)
        builder.setNegativeButton(android.R.string.cancel, null)
        val itemSettings: CharSequence = getString(R.string.ime_settings_activity_name)
        val itemInputMethod: CharSequence = getString(R.string.inputMethod)
        builder.setItems(
            arrayOf(itemSettings, itemInputMethod)
        ) { di, position ->
            di.dismiss()
            when (position) {
                0 -> launchSettings()
                1 -> (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showInputMethodPicker()
            }
        }
        builder.setTitle(getString(R.string.ime_name))
        mOptionsDialog = builder.create()
        val window = mOptionsDialog?.getWindow()
        val lp = window!!.attributes
        lp.token = mSkbContainer!!.windowToken
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
        window.attributes = lp
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        mOptionsDialog?.show()
    }

    private fun launchSettings() {
        val intent = Intent()
        intent.setClass(this@PinyinIME, SettingsActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private inner class PopupTimer : Handler(), Runnable {
        private val mParentLocation = IntArray(2)

        fun postShowFloatingWindow() {
            mFloatingContainer!!.measure(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            mFloatingWindow!!.width = mFloatingContainer!!.measuredWidth
            mFloatingWindow!!.height = mFloatingContainer!!.measuredHeight
            post(this)
        }

        fun cancelShowing() {
            if (mFloatingWindow!!.isShowing) {
                mFloatingWindow!!.dismiss()
            }
            removeCallbacks(this)
        }

        override fun run() {
            mCandidatesContainer!!.getLocationInWindow(mParentLocation)

            if (!mFloatingWindow!!.isShowing) {
                mFloatingWindow!!.showAtLocation(
                    mCandidatesContainer,
                    Gravity.LEFT or Gravity.TOP, mParentLocation[0],
                    mParentLocation[1] - mFloatingWindow!!.height
                )
            } else {
                mFloatingWindow?.update(
                    mParentLocation[0],
                    mParentLocation[1] - mFloatingWindow!!.height,
                    mFloatingWindow!!.width,
                    mFloatingWindow!!.height
                )
            }
        }
    }

    /**
     * Used to notify IME that the user selects a candidate or performs an
     * gesture.
     */
    inner class ChoiceNotifier internal constructor(var mIme: PinyinIME) : Handler(),
        CandidateViewListener {
        override fun onClickChoice(choiceId: Int) {
            if (choiceId >= 0) {
                mIme.onChoiceTouched(choiceId)
            }
        }

        override fun onToLeftGesture() {
            if (ImeState.STATE_COMPOSING == mImeState) {
                changeToStateInput(true)
            }
            mCandidatesContainer!!.pageForward(true, false)
        }

        override fun onToRightGesture() {
            if (ImeState.STATE_COMPOSING == mImeState) {
                changeToStateInput(true)
            }
            mCandidatesContainer!!.pageBackward(true, false)
        }

        override fun onToTopGesture() {
        }

        override fun onToBottomGesture() {
        }
    }

    inner class OnGestureListener(
        /** If it false, we will not response detected gestures.  */
        private val mReponseGestures: Boolean
    ) : SimpleOnGestureListener() {
        /** The minimum X velocity observed in the gesture.  */
        private var mMinVelocityX = Float.MAX_VALUE

        /** The minimum Y velocity observed in the gesture.  */
        private var mMinVelocityY = Float.MAX_VALUE

        /** The first down time for the series of touch events for an action.  */
        private var mTimeDown: Long = 0

        /** The last time when onScroll() is called.  */
        private var mTimeLastOnScroll: Long = 0

        /** This flag used to indicate that this gesture is not a gesture.  */
        private var mNotGesture = false

        /** This flag used to indicate that this gesture has been recognized.  */
        private var mGestureRecognized = false

        override fun onDown(e: MotionEvent): Boolean {
            mMinVelocityX = Int.MAX_VALUE.toFloat()
            mMinVelocityY = Int.MAX_VALUE.toFloat()
            mTimeDown = e.eventTime
            mTimeLastOnScroll = mTimeDown
            mNotGesture = false
            mGestureRecognized = false
            return false
        }

        override fun onScroll(
            e1: MotionEvent?, e2: MotionEvent,
            distanceX: Float, distanceY: Float
        ): Boolean {
            e1 ?: return false
            if (mNotGesture) return false
            if (mGestureRecognized) return true

            if (abs((e1.x - e2.x).toDouble()) < MIN_X_FOR_DRAG
                && abs((e1.y - e2.y).toDouble()) < MIN_Y_FOR_DRAG
            ) return false

            val timeNow = e2.eventTime
            var spanTotal = timeNow - mTimeDown
            var spanThis = timeNow - mTimeLastOnScroll
            if (0L == spanTotal) spanTotal = 1
            if (0L == spanThis) spanThis = 1

            val vXTotal = (e2.x - e1.x) / spanTotal
            val vYTotal = (e2.y - e1.y) / spanTotal

            // The distances are from the current point to the previous one.
            val vXThis = -distanceX / spanThis
            val vYThis = -distanceY / spanThis

            val kX = vXTotal * vXThis
            val kY = vYTotal * vYThis
            val k1 = kX + kY
            val k2 = (abs(kX.toDouble()) + abs(kY.toDouble())).toFloat()

            if (k1 / k2 < 0.8) {
                mNotGesture = true
                return false
            }
            val absVXTotal = abs(vXTotal.toDouble()).toFloat()
            val absVYTotal = abs(vYTotal.toDouble()).toFloat()
            if (absVXTotal < mMinVelocityX) {
                mMinVelocityX = absVXTotal
            }
            if (absVYTotal < mMinVelocityY) {
                mMinVelocityY = absVYTotal
            }

            if (mMinVelocityX < VELOCITY_THRESHOLD_X1
                && mMinVelocityY < VELOCITY_THRESHOLD_Y1
            ) {
                mNotGesture = true
                return false
            }

            if (vXTotal > VELOCITY_THRESHOLD_X2
                && absVYTotal < VELOCITY_THRESHOLD_Y2
            ) {
                if (mReponseGestures) onDirectionGesture(Gravity.RIGHT)
                mGestureRecognized = true
            } else if (vXTotal < -VELOCITY_THRESHOLD_X2
                && absVYTotal < VELOCITY_THRESHOLD_Y2
            ) {
                if (mReponseGestures) onDirectionGesture(Gravity.LEFT)
                mGestureRecognized = true
            } else if (vYTotal > VELOCITY_THRESHOLD_Y2
                && absVXTotal < VELOCITY_THRESHOLD_X2
            ) {
                if (mReponseGestures) onDirectionGesture(Gravity.BOTTOM)
                mGestureRecognized = true
            } else if (vYTotal < -VELOCITY_THRESHOLD_Y2
                && absVXTotal < VELOCITY_THRESHOLD_X2
            ) {
                if (mReponseGestures) onDirectionGesture(Gravity.TOP)
                mGestureRecognized = true
            }

            mTimeLastOnScroll = timeNow
            return mGestureRecognized
        }

        override fun onFling(
            me1: MotionEvent?, me2: MotionEvent,
            velocityX: Float, velocityY: Float
        ): Boolean {
            return mGestureRecognized
        }

        fun onDirectionGesture(gravity: Int) {
            if (Gravity.NO_GRAVITY == gravity) {
                return
            }

            if (Gravity.LEFT == gravity || Gravity.RIGHT == gravity) {
                if (mCandidatesContainer!!.isShown) {
                    if (Gravity.LEFT == gravity) {
                        mCandidatesContainer!!.pageForward(true, true)
                    } else {
                        mCandidatesContainer!!.pageBackward(true, true)
                    }
                    return
                }
            }
        }
    }

    /**
     * Connection used for binding to the Pinyin decoding service.
     */
    inner class PinyinDecoderServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mDecInfo.mIPinyinDecoderService =
                IPinyinDecoderService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
        }
    }

    enum class ImeState {
        STATE_BYPASS, STATE_IDLE, STATE_INPUT, STATE_COMPOSING, STATE_PREDICT,
        STATE_APP_COMPLETION
    }

    inner class DecodingInfo {
        /**
         * Spelling (Pinyin) string.
         */
        val originalSplStr: StringBuffer = StringBuffer()

        /**
         * Byte buffer used as the Pinyin string parameter for native function
         * call.
         */
        private var mPyBuf: ByteArray? = null

        /**
         * The length of surface string successfully decoded by engine.
         */
        var splStrDecodedLen: Int = 0
            private set

        /**
         * Composing string.
         */
        var composingStr: String? = null
            private set

        /**
         * Length of the active composing string.
         */
        var activeCmpsLen: Int = 0
            private set

        /**
         * Composing string for display, it is copied from mComposingStr, and
         * add spaces between spellings.
         */
        var composingStrForDisplay: String? = null
            private set

        /**
         * Length of the active composing string for display.
         */
        var activeCmpsDisplayLen: Int = 0
            private set

        /**
         * The first full sentence choice.
         */
        var fullSent: String? = null
            private set

        /**
         * Number of characters which have been fixed.
         */
        var fixedLen: Int = 0
            private set

        /**
         * If this flag is true, selection is finished.
         */
        private var mFinishSelection = false

        /**
         * The starting position for each spelling. The first one is the number
         * of the real starting position elements.
         */
        var splStart: IntArray? = null
            private set

        /**
         * Editing cursor in mSurface.
         */
        var cursorPos: Int = 0
            private set

        /**
         * Remote Pinyin-to-Hanzi decoding engine service.
         */
        var mIPinyinDecoderService: IPinyinDecoderService? = null

        /**
         * The complication information suggested by application.
         */
        var mAppCompletions: Array<CompletionInfo>? = null

        /**
         * The total number of choices for display. The list may only contains
         * the first part. If user tries to navigate to next page which is not
         * in the result list, we need to get these items.
         */
        var mTotalChoicesNum: Int = 0

        /**
         * Candidate list. The first one is the full-sentence candidate.
         */
        var mCandidatesList: MutableList<String> = Vector()

        /**
         * Element i stores the starting position of page i.
         */
        var mPageStart: Vector<Int> = Vector()

        /**
         * Element i stores the number of characters to page i.
         */
        var mCnToPage: Vector<Int?> = Vector()

        /**
         * The position to delete in Pinyin string. If it is less than 0, IME
         * will do an incremental search, otherwise IME will do a deletion
         * operation. if [.mIsPosInSpl] is true, IME will delete the whole
         * string for mPosDelSpl-th spelling, otherwise it will only delete
         * mPosDelSpl-th character in the Pinyin string.
         */
        var mPosDelSpl: Int = -1

        /**
         * If [.mPosDelSpl] is big than or equal to 0, this member is used
         * to indicate that whether the postion is counted in spelling id or
         * character.
         */
        var mIsPosInSpl: Boolean = false

        fun reset() {
            originalSplStr.delete(0, originalSplStr.length)
            splStrDecodedLen = 0
            cursorPos = 0
            fullSent = ""
            fixedLen = 0
            mFinishSelection = false
            composingStr = ""
            composingStrForDisplay = ""
            activeCmpsLen = 0
            activeCmpsDisplayLen = 0

            resetCandidates()
        }

        val isCandidatesListEmpty: Boolean
            get() = mCandidatesList.size == 0

        val isSplStrFull: Boolean
            get() {
                if (originalSplStr.length >= PY_STRING_MAX - 1) return true
                return false
            }

        fun addSplChar(ch: Char, reset: Boolean) {
            if (reset) {
                originalSplStr.delete(0, originalSplStr.length)
                splStrDecodedLen = 0
                cursorPos = 0
                mIPinyinDecoderService!!.imResetSearch()
            }
            originalSplStr.insert(cursorPos, ch)
            cursorPos++
        }

        // Prepare to delete before cursor. We may delete a spelling char if
        // the cursor is in the range of unfixed part, delete a whole spelling
        // if the cursor in inside the range of the fixed part.
        // This function only marks the position used to delete.
        fun prepareDeleteBeforeCursor() {
            if (cursorPos > 0) {
                var pos = 0
                while (pos < fixedLen) {
                    if (splStart!![pos + 2] >= cursorPos
                        && splStart!![pos + 1] < cursorPos
                    ) {
                        mPosDelSpl = pos
                        cursorPos = splStart!![pos + 1]
                        mIsPosInSpl = true
                        break
                    }
                    pos++
                }
                if (mPosDelSpl < 0) {
                    mPosDelSpl = cursorPos - 1
                    cursorPos--
                    mIsPosInSpl = false
                }
            }
        }

        fun length(): Int {
            return originalSplStr.length
        }

        fun charAt(index: Int): Char {
            return originalSplStr[index]
        }

        val composingStrActivePart: String
            get() {
                assert(activeCmpsLen <= composingStr!!.length)
                return composingStr!!.substring(0, activeCmpsLen)
            }

        fun getCurrentFullSent(activeCandPos: Int): String {
            try {
                var retStr = fullSent!!.substring(0, fixedLen)
                retStr += mCandidatesList[activeCandPos]
                return retStr
            } catch (e: Exception) {
                return ""
            }
        }

        fun resetCandidates() {
            mCandidatesList.clear()
            mTotalChoicesNum = 0

            mPageStart.clear()
            mPageStart.add(0)
            mCnToPage.clear()
            mCnToPage.add(0)
        }

        fun candidatesFromApp(): Boolean {
            return ImeState.STATE_APP_COMPLETION == mImeState
        }

        fun canDoPrediction(): Boolean {
            return composingStr!!.length == fixedLen
        }

        fun selectionFinished(): Boolean {
            return mFinishSelection
        }

        // After the user chooses a candidate, input method will do a
        // re-decoding and give the new candidate list.
        // If candidate id is less than 0, means user is inputting Pinyin,
        // not selecting any choice.
        fun chooseDecodingCandidate(candId: Int) {
            if (mImeState != ImeState.STATE_PREDICT) {
                resetCandidates()
                var totalChoicesNum = 0
                if (candId < 0) {
                    if (length() == 0) {
                        totalChoicesNum = 0
                    } else {
                        if (mPyBuf == null) mPyBuf = ByteArray(PY_STRING_MAX)
                        for (i in 0 until length()) mPyBuf!![i] = charAt(i).code.toByte()
                        mPyBuf!![length()] = 0

                        if (mPosDelSpl < 0) {
                            totalChoicesNum = mIPinyinDecoderService!!
                                .imSearch(mPyBuf, length())
                        } else {
                            var clear_fixed_this_step = true
                            if (ImeState.STATE_COMPOSING == mImeState) {
                                clear_fixed_this_step = false
                            }
                            totalChoicesNum = mIPinyinDecoderService!!
                                .imDelSearch(
                                    mPosDelSpl, mIsPosInSpl,
                                    clear_fixed_this_step
                                )
                            mPosDelSpl = -1
                        }
                    }
                } else {
                    totalChoicesNum = mIPinyinDecoderService!!
                        .imChoose(candId)
                }
                updateDecInfoForSearch(totalChoicesNum)
            }
        }

        private fun updateDecInfoForSearch(totalChoicesNum: Int) {
            mTotalChoicesNum = totalChoicesNum
            if (mTotalChoicesNum < 0) {
                mTotalChoicesNum = 0
                return
            }

            try {
                splStart = mIPinyinDecoderService!!.imGetSplStart()
                val pyStr = mIPinyinDecoderService!!.imGetPyStr(false)
                splStrDecodedLen = mIPinyinDecoderService!!.imGetPyStrLen(true)
                assert(splStrDecodedLen <= pyStr.length)

                fullSent = mIPinyinDecoderService!!.imGetChoice(0)
                fixedLen = mIPinyinDecoderService!!.imGetFixedLen()

                // Update the surface string to the one kept by engine.
                originalSplStr.replace(0, originalSplStr.length, pyStr)

                if (cursorPos > originalSplStr.length) cursorPos = originalSplStr.length
                composingStr = (fullSent!!.substring(0, fixedLen)
                        + originalSplStr.substring(splStart!![fixedLen + 1]))

                activeCmpsLen = composingStr!!.length
                if (splStrDecodedLen > 0) {
                    activeCmpsLen = (activeCmpsLen
                            - (originalSplStr.length - splStrDecodedLen))
                }

                // Prepare the display string.
                if (0 == splStrDecodedLen) {
                    composingStrForDisplay = composingStr
                    activeCmpsDisplayLen = composingStr!!.length
                } else {
                    composingStrForDisplay = fullSent!!.substring(0, fixedLen)
                    for (pos in fixedLen + 1 until splStart!!.size - 1) {
                        composingStrForDisplay += originalSplStr.substring(
                            splStart!![pos], splStart!![pos + 1]
                        )
                        if (splStart!![pos + 1] < splStrDecodedLen) {
                            composingStrForDisplay += " "
                        }
                    }
                    activeCmpsDisplayLen = composingStrForDisplay!!.length
                    if (splStrDecodedLen < originalSplStr.length) {
                        composingStrForDisplay += originalSplStr
                            .substring(splStrDecodedLen)
                    }
                }

                mFinishSelection = if (splStart!!.size == fixedLen + 2) {
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                mTotalChoicesNum = 0
                composingStr = ""
            }
            // Prepare page 0.
            if (!mFinishSelection) {
                preparePage(0)
            }
        }

        fun choosePredictChoice(choiceId: Int) {
            if (ImeState.STATE_PREDICT != mImeState || choiceId < 0 || choiceId >= mTotalChoicesNum) {
                return
            }

            val tmp = mCandidatesList[choiceId]

            resetCandidates()

            mCandidatesList.add(tmp)
            mTotalChoicesNum = 1

            originalSplStr.replace(0, originalSplStr.length, "")
            cursorPos = 0
            fullSent = tmp
            fixedLen = tmp.length
            composingStr = fullSent
            activeCmpsLen = fixedLen

            mFinishSelection = true
        }

        fun getCandidate(candId: Int): String? {
            // Only loaded items can be gotten, so we use mCandidatesList.size()
            // instead mTotalChoiceNum.
            if (candId < 0 || candId > mCandidatesList.size) {
                return null
            }
            return mCandidatesList[candId]
        }

        private val candiagtesForCache: Unit
            get() {
                val fetchStart = mCandidatesList.size
                var fetchSize = mTotalChoicesNum - fetchStart
                if (fetchSize > MAX_PAGE_SIZE_DISPLAY) {
                    fetchSize = MAX_PAGE_SIZE_DISPLAY
                }
                var newList: List<String>? = null
                if (ImeState.STATE_INPUT == mImeState || ImeState.STATE_IDLE == mImeState || ImeState.STATE_COMPOSING == mImeState) {
                    newList = mIPinyinDecoderService!!.imGetChoiceList(
                        fetchStart, fetchSize, fixedLen
                    )
                } else if (ImeState.STATE_PREDICT == mImeState) {
                    newList = mIPinyinDecoderService!!.imGetPredictList(
                        fetchStart, fetchSize
                    )
                } else if (ImeState.STATE_APP_COMPLETION == mImeState) {
                    newList = ArrayList()
                    if (null != mAppCompletions) {
                        for (pos in fetchStart until fetchSize) {
                            val ci = mAppCompletions!![pos]
                            if (null != ci) {
                                val s = ci.text
                                if (null != s) newList.add(s.toString())
                            }
                        }
                    }
                }
                mCandidatesList.addAll(newList.orEmpty())
            }

        fun pageReady(pageNo: Int): Boolean {
            // If the page number is less than 0, return false
            if (pageNo < 0) return false

            // Page pageNo's ending information is not ready.
            if (mPageStart.size <= pageNo + 1) {
                return false
            }

            return true
        }

        fun preparePage(pageNo: Int): Boolean {
            // If the page number is less than 0, return false
            if (pageNo < 0) return false

            // Make sure the starting information for page pageNo is ready.
            if (mPageStart.size <= pageNo) {
                return false
            }

            // Page pageNo's ending information is also ready.
            if (mPageStart.size > pageNo + 1) {
                return true
            }

            // If cached items is enough for page pageNo.
            if (mCandidatesList.size - mPageStart.elementAt(pageNo) >= MAX_PAGE_SIZE_DISPLAY) {
                return true
            }

            // Try to get more items from engine
            candiagtesForCache

            // Try to find if there are available new items to display.
            // If no new item, return false;
            if (mPageStart.elementAt(pageNo) >= mCandidatesList.size) {
                return false
            }

            // If there are new items, return true;
            return true
        }

        fun preparePredicts(history: CharSequence?) {
            if (null == history) return

            resetCandidates()

            if (Settings.prediction) {
                val preEdit = history.toString()
                val predictNum = 0
                if (null != preEdit) {
                    mTotalChoicesNum = mIPinyinDecoderService!!
                        .imGetPredictsNum(preEdit)
                }
            }

            preparePage(0)
            mFinishSelection = false
        }

        fun prepareAppCompletions(completions: Array<CompletionInfo>) {
            resetCandidates()
            mAppCompletions = completions
            mTotalChoicesNum = completions.size
            preparePage(0)
            mFinishSelection = false
            return
        }

        fun getCurrentPageSize(currentPage: Int): Int {
            if (mPageStart.size <= currentPage + 1) return 0
            return (mPageStart.elementAt(currentPage + 1)
                    - mPageStart.elementAt(currentPage))
        }

        fun getCurrentPageStart(currentPage: Int): Int {
            if (mPageStart.size < currentPage + 1) return mTotalChoicesNum
            return mPageStart.elementAt(currentPage)
        }

        fun pageForwardable(currentPage: Int): Boolean {
            if (mPageStart.size <= currentPage + 1) return false
            if (mPageStart.elementAt(currentPage + 1) >= mTotalChoicesNum) {
                return false
            }
            return true
        }

        fun pageBackwardable(currentPage: Int): Boolean {
            if (currentPage > 0) return true
            return false
        }

        fun charBeforeCursorIsSeparator(): Boolean {
            val len = originalSplStr.length
            if (cursorPos > len) return false
            if (cursorPos > 0 && originalSplStr[cursorPos - 1] == '\'') {
                return true
            }
            return false
        }

        val cursorPosInCmps: Int
            get() {
                var cursorPos = cursorPos
                val fixedLen = 0

                for (hzPos in 0 until this.fixedLen) {
                    if (this.cursorPos >= splStart!![hzPos + 2]) {
                        cursorPos -= splStart!![hzPos + 2] - splStart!![hzPos + 1]
                        cursorPos += 1
                    }
                }
                return cursorPos
            }

        val cursorPosInCmpsDisplay: Int
            get() {
                var cursorPos = cursorPosInCmps
                // +2 is because: one for mSplStart[0], which is used for other
                // purpose(The length of the segmentation string), and another
                // for the first spelling which does not need a space before it.
                for (pos in fixedLen + 2 until splStart!!.size - 1) {
                    if (this.cursorPos <= splStart!![pos]) {
                        break
                    } else {
                        cursorPos++
                    }
                }
                return cursorPos
            }

        fun moveCursorToEdge(left: Boolean) {
            if (left) cursorPos = 0
            else cursorPos = originalSplStr.length
        }

        // Move cursor. If offset is 0, this function can be used to adjust
        // the cursor into the bounds of the string.
        fun moveCursor(offset: Int) {
            var offset = offset
            if (offset > 1 || offset < -1) return

            if (offset != 0) {
                var hzPos = 0
                hzPos = 0
                while (hzPos <= fixedLen) {
                    if (cursorPos == splStart!![hzPos + 1]) {
                        if (offset < 0) {
                            if (hzPos > 0) {
                                offset = (splStart!![hzPos]
                                        - splStart!![hzPos + 1])
                            }
                        } else {
                            if (hzPos < fixedLen) {
                                offset = (splStart!![hzPos + 2]
                                        - splStart!![hzPos + 1])
                            }
                        }
                        break
                    }
                    hzPos++
                }
            }
            cursorPos += offset
            if (cursorPos < 0) {
                cursorPos = 0
            } else if (cursorPos > originalSplStr.length) {
                cursorPos = originalSplStr.length
            }
        }

        val splNum: Int
            get() = splStart!![0]
    }

    companion object {
        /**
         * TAG for debug.
         */
        const val TAG: String = "PinyinIME"

        /**
         * If is is true, IME will simulate key events for delete key, and send the
         * events back to the application.
         */
        private const val SIMULATE_KEY_DELETE = true

        /**
         * When user presses and drags, the minimum x-distance to make a
         * response to the drag event.
         */
        private const val MIN_X_FOR_DRAG = 60

        /**
         * When user presses and drags, the minimum y-distance to make a
         * response to the drag event.
         */
        private const val MIN_Y_FOR_DRAG = 40

        /**
         * Velocity threshold for a screen-move gesture. If the minimum
         * x-velocity is less than it, no gesture.
         */
        private const val VELOCITY_THRESHOLD_X1 = 0.3f

        /**
         * Velocity threshold for a screen-move gesture. If the maximum
         * x-velocity is less than it, no gesture.
         */
        private const val VELOCITY_THRESHOLD_X2 = 0.7f

        /**
         * Velocity threshold for a screen-move gesture. If the minimum
         * y-velocity is less than it, no gesture.
         */
        private const val VELOCITY_THRESHOLD_Y1 = 0.2f

        /**
         * Velocity threshold for a screen-move gesture. If the maximum
         * y-velocity is less than it, no gesture.
         */
        private const val VELOCITY_THRESHOLD_Y2 = 0.45f

        /**
         * Maximum length of the Pinyin string
         */
        private const val PY_STRING_MAX = 28

        /**
         * Maximum number of candidates to display in one page.
         */
        private const val MAX_PAGE_SIZE_DISPLAY = 10
    }
}
