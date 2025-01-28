/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.inputmethod.keyboard.internal

import android.text.TextUtils
import android.util.Log
import com.android.inputmethod.event.Event
import com.android.inputmethod.keyboard.internal.KeyboardState.SwitchActions
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.utils.CapsModeUtils
import com.android.inputmethod.latin.utils.RecapitalizeStatus

/**
 * Keyboard state machine.
 *
 * This class contains all keyboard state transition logic.
 *
 * The input events are [.onLoadKeyboard], [.onSaveKeyboardState],
 * [.onPressKey], [.onReleaseKey],
 * [.onEvent], [.onFinishSlidingInput],
 * [.onUpdateShiftState], [.onResetKeyboardStateToAlphabet].
 *
 * The actions are [SwitchActions]'s methods.
 */
class KeyboardState(switchActions: SwitchActions) {
    interface SwitchActions {
        fun setAlphabetKeyboard()
        fun setAlphabetManualShiftedKeyboard()
        fun setAlphabetAutomaticShiftedKeyboard()
        fun setAlphabetShiftLockedKeyboard()
        fun setAlphabetShiftLockShiftedKeyboard()
        fun setEmojiKeyboard()
        fun setSymbolsKeyboard()
        fun setSymbolsShiftedKeyboard()

        /**
         * Request to call back [KeyboardState.onUpdateShiftState].
         */
        fun requestUpdatingShiftState(autoCapsFlags: Int, recapitalizeMode: Int)

        fun startDoubleTapShiftKeyTimer()
        fun isInDoubleTapShiftKeyTimeout(): Boolean
        fun cancelDoubleTapShiftKeyTimer()

        companion object {
            const val DEBUG_ACTION: Boolean = false

            const val DEBUG_TIMER_ACTION: Boolean = false
        }
    }

    private val mSwitchActions: SwitchActions

    private val mShiftKeyState: ShiftKeyState = ShiftKeyState("Shift")
    private val mSymbolKeyState: ModifierKeyState = ModifierKeyState("Symbol")

    private var mSwitchState: Int = SWITCH_STATE_ALPHA

    // TODO: Consolidate these two mode booleans into one integer to distinguish between alphabet,
    // symbols, and emoji mode.
    private var mIsAlphabetMode: Boolean = false
    private var mIsEmojiMode: Boolean = false
    private val mAlphabetShiftState: AlphabetShiftState = AlphabetShiftState()
    private var mIsSymbolShifted: Boolean = false
    private var mPrevMainKeyboardWasShiftLocked: Boolean = false
    private var mPrevSymbolsKeyboardWasShifted: Boolean = false
    private var mRecapitalizeMode: Int

    // For handling double tap.
    private var mIsInAlphabetUnshiftedFromShifted: Boolean = false
    private var mIsInDoubleTapShiftKey: Boolean = false

    private val mSavedKeyboardState: SavedKeyboardState = SavedKeyboardState()

    internal class SavedKeyboardState {
        var mIsValid: Boolean = false
        var mIsAlphabetMode: Boolean = false
        var mIsAlphabetShiftLocked: Boolean = false
        var mIsEmojiMode: Boolean = false
        var mShiftMode: Int = 0

        override fun toString(): String {
            if (!mIsValid) {
                return "INVALID"
            }
            if (mIsAlphabetMode) {
                return if (mIsAlphabetShiftLocked)
                    "ALPHABET_SHIFT_LOCKED"
                else
                    "ALPHABET_" + shiftModeToString(mShiftMode)
            }
            if (mIsEmojiMode) {
                return "EMOJI"
            }
            return "SYMBOLS_" + shiftModeToString(mShiftMode)
        }
    }

    fun onLoadKeyboard(autoCapsFlags: Int, recapitalizeMode: Int) {
        if (DEBUG_EVENT) {
            Log.d(TAG, "onLoadKeyboard: " + stateToString(autoCapsFlags, recapitalizeMode))
        }
        // Reset alphabet shift state.
        mAlphabetShiftState.setShiftLocked(false)
        mPrevMainKeyboardWasShiftLocked = false
        mPrevSymbolsKeyboardWasShifted = false
        mShiftKeyState.onRelease()
        mSymbolKeyState.onRelease()
        if (mSavedKeyboardState.mIsValid) {
            onRestoreKeyboardState(autoCapsFlags, recapitalizeMode)
            mSavedKeyboardState.mIsValid = false
        } else {
            // Reset keyboard to alphabet mode.
            setAlphabetKeyboard(autoCapsFlags, recapitalizeMode)
        }
    }

    init {
        mSwitchActions = switchActions
        mRecapitalizeMode = RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE
    }

    fun onSaveKeyboardState() {
        val state: SavedKeyboardState = mSavedKeyboardState
        state.mIsAlphabetMode = mIsAlphabetMode
        state.mIsEmojiMode = mIsEmojiMode
        if (mIsAlphabetMode) {
            state.mIsAlphabetShiftLocked = mAlphabetShiftState.isShiftLocked()
            state.mShiftMode = if (mAlphabetShiftState.isAutomaticShifted())
                AUTOMATIC_SHIFT
            else
                (if (mAlphabetShiftState.isShiftedOrShiftLocked()) MANUAL_SHIFT else UNSHIFT)
        } else {
            state.mIsAlphabetShiftLocked = mPrevMainKeyboardWasShiftLocked
            state.mShiftMode = if (mIsSymbolShifted) MANUAL_SHIFT else UNSHIFT
        }
        state.mIsValid = true
        if (DEBUG_EVENT) {
            Log.d(TAG, "onSaveKeyboardState: saved=" + state + " " + this)
        }
    }

    private fun onRestoreKeyboardState(autoCapsFlags: Int, recapitalizeMode: Int) {
        val state: SavedKeyboardState = mSavedKeyboardState
        if (DEBUG_EVENT) {
            Log.d(
                TAG, ("onRestoreKeyboardState: saved=" + state
                        + " " + stateToString(autoCapsFlags, recapitalizeMode))
            )
        }
        mPrevMainKeyboardWasShiftLocked = state.mIsAlphabetShiftLocked
        if (state.mIsAlphabetMode) {
            setAlphabetKeyboard(autoCapsFlags, recapitalizeMode)
            setShiftLocked(state.mIsAlphabetShiftLocked)
            if (!state.mIsAlphabetShiftLocked) {
                setShifted(state.mShiftMode)
            }
            return
        }
        if (state.mIsEmojiMode) {
            setEmojiKeyboard()
            return
        }
        // Symbol mode
        if (state.mShiftMode == MANUAL_SHIFT) {
            setSymbolsShiftedKeyboard()
        } else {
            setSymbolsKeyboard()
        }
    }

    private fun setShifted(shiftMode: Int) {
        if (DEBUG_INTERNAL_ACTION) {
            Log.d(TAG, "setShifted: shiftMode=" + shiftModeToString(shiftMode) + " " + this)
        }
        if (!mIsAlphabetMode) return
        val prevShiftMode: Int
        if (mAlphabetShiftState.isAutomaticShifted()) {
            prevShiftMode = AUTOMATIC_SHIFT
        } else if (mAlphabetShiftState.isManualShifted()) {
            prevShiftMode = MANUAL_SHIFT
        } else {
            prevShiftMode = UNSHIFT
        }
        when (shiftMode) {
            AUTOMATIC_SHIFT -> {
                mAlphabetShiftState.setAutomaticShifted()
                if (shiftMode != prevShiftMode) {
                    mSwitchActions.setAlphabetAutomaticShiftedKeyboard()
                }
            }

            MANUAL_SHIFT -> {
                mAlphabetShiftState.setShifted(true)
                if (shiftMode != prevShiftMode) {
                    mSwitchActions.setAlphabetManualShiftedKeyboard()
                }
            }

            UNSHIFT -> {
                mAlphabetShiftState.setShifted(false)
                if (shiftMode != prevShiftMode) {
                    mSwitchActions.setAlphabetKeyboard()
                }
            }

            SHIFT_LOCK_SHIFTED -> {
                mAlphabetShiftState.setShifted(true)
                mSwitchActions.setAlphabetShiftLockShiftedKeyboard()
            }
        }
    }

    private fun setShiftLocked(shiftLocked: Boolean) {
        if (DEBUG_INTERNAL_ACTION) {
            Log.d(TAG, "setShiftLocked: shiftLocked=" + shiftLocked + " " + this)
        }
        if (!mIsAlphabetMode) return
        if (shiftLocked && (!mAlphabetShiftState.isShiftLocked()
                    || mAlphabetShiftState.isShiftLockShifted())
        ) {
            mSwitchActions.setAlphabetShiftLockedKeyboard()
        }
        if (!shiftLocked && mAlphabetShiftState.isShiftLocked()) {
            mSwitchActions.setAlphabetKeyboard()
        }
        mAlphabetShiftState.setShiftLocked(shiftLocked)
    }

    private fun toggleAlphabetAndSymbols(autoCapsFlags: Int, recapitalizeMode: Int) {
        if (DEBUG_INTERNAL_ACTION) {
            Log.d(
                TAG, "toggleAlphabetAndSymbols: "
                        + stateToString(autoCapsFlags, recapitalizeMode)
            )
        }
        if (mIsAlphabetMode) {
            mPrevMainKeyboardWasShiftLocked = mAlphabetShiftState.isShiftLocked()
            if (mPrevSymbolsKeyboardWasShifted) {
                setSymbolsShiftedKeyboard()
            } else {
                setSymbolsKeyboard()
            }
            mPrevSymbolsKeyboardWasShifted = false
        } else {
            mPrevSymbolsKeyboardWasShifted = mIsSymbolShifted
            setAlphabetKeyboard(autoCapsFlags, recapitalizeMode)
            if (mPrevMainKeyboardWasShiftLocked) {
                setShiftLocked(true)
            }
            mPrevMainKeyboardWasShiftLocked = false
        }
    }

    // TODO: Remove this method. Come up with a more comprehensive way to reset the keyboard layout
    // when a keyboard layout set doesn't get reloaded in LatinIME.onStartInputViewInternal().
    private fun resetKeyboardStateToAlphabet(autoCapsFlags: Int, recapitalizeMode: Int) {
        if (DEBUG_INTERNAL_ACTION) {
            Log.d(
                TAG, "resetKeyboardStateToAlphabet: "
                        + stateToString(autoCapsFlags, recapitalizeMode)
            )
        }
        if (mIsAlphabetMode) return

        mPrevSymbolsKeyboardWasShifted = mIsSymbolShifted
        setAlphabetKeyboard(autoCapsFlags, recapitalizeMode)
        if (mPrevMainKeyboardWasShiftLocked) {
            setShiftLocked(true)
        }
        mPrevMainKeyboardWasShiftLocked = false
    }

    private fun toggleShiftInSymbols() {
        if (mIsSymbolShifted) {
            setSymbolsKeyboard()
        } else {
            setSymbolsShiftedKeyboard()
        }
    }

    private fun setAlphabetKeyboard(autoCapsFlags: Int, recapitalizeMode: Int) {
        if (DEBUG_INTERNAL_ACTION) {
            Log.d(TAG, "setAlphabetKeyboard: " + stateToString(autoCapsFlags, recapitalizeMode))
        }

        mSwitchActions.setAlphabetKeyboard()
        mIsAlphabetMode = true
        mIsEmojiMode = false
        mIsSymbolShifted = false
        mRecapitalizeMode = RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE
        mSwitchState = SWITCH_STATE_ALPHA
        mSwitchActions.requestUpdatingShiftState(autoCapsFlags, recapitalizeMode)
    }

    private fun setSymbolsKeyboard() {
        if (DEBUG_INTERNAL_ACTION) {
            Log.d(TAG, "setSymbolsKeyboard")
        }
        mSwitchActions.setSymbolsKeyboard()
        mIsAlphabetMode = false
        mIsSymbolShifted = false
        mRecapitalizeMode = RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE
        // Reset alphabet shift state.
        mAlphabetShiftState.setShiftLocked(false)
        mSwitchState = SWITCH_STATE_SYMBOL_BEGIN
    }

    private fun setSymbolsShiftedKeyboard() {
        if (DEBUG_INTERNAL_ACTION) {
            Log.d(TAG, "setSymbolsShiftedKeyboard")
        }
        mSwitchActions.setSymbolsShiftedKeyboard()
        mIsAlphabetMode = false
        mIsSymbolShifted = true
        mRecapitalizeMode = RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE
        // Reset alphabet shift state.
        mAlphabetShiftState.setShiftLocked(false)
        mSwitchState = SWITCH_STATE_SYMBOL_BEGIN
    }

    private fun setEmojiKeyboard() {
        if (DEBUG_INTERNAL_ACTION) {
            Log.d(TAG, "setEmojiKeyboard")
        }
        mIsAlphabetMode = false
        mIsEmojiMode = true
        mRecapitalizeMode = RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE
        // Remember caps lock mode and reset alphabet shift state.
        mPrevMainKeyboardWasShiftLocked = mAlphabetShiftState.isShiftLocked()
        mAlphabetShiftState.setShiftLocked(false)
        mSwitchActions.setEmojiKeyboard()
    }

    fun onPressKey(
        code: Int, isSinglePointer: Boolean, autoCapsFlags: Int,
        recapitalizeMode: Int
    ) {
        if (DEBUG_EVENT) {
            Log.d(
                TAG, ("onPressKey: code=" + Constants.printableCode(code)
                        + " single=" + isSinglePointer
                        + " " + stateToString(autoCapsFlags, recapitalizeMode))
            )
        }
        if (code != Constants.CODE_SHIFT) {
            // Because the double tap shift key timer is to detect two consecutive shift key press,
            // it should be canceled when a non-shift key is pressed.
            mSwitchActions.cancelDoubleTapShiftKeyTimer()
        }
        if (code == Constants.CODE_SHIFT) {
            onPressShift()
        } else if (code == Constants.CODE_CAPSLOCK) {
            // Nothing to do here. See {@link #onReleaseKey(int,boolean)}.
        } else if (code == Constants.CODE_SWITCH_ALPHA_SYMBOL) {
            onPressSymbol(autoCapsFlags, recapitalizeMode)
        } else {
            mShiftKeyState.onOtherKeyPressed()
            mSymbolKeyState.onOtherKeyPressed()
            // It is required to reset the auto caps state when all of the following conditions
            // are met:
            // 1) two or more fingers are in action
            // 2) in alphabet layout
            // 3) not in all characters caps mode
            // As for #3, please note that it's required to check even when the auto caps mode is
            // off because, for example, we may be in the #1 state within the manual temporary
            // shifted mode.
            if (!isSinglePointer && mIsAlphabetMode
                && autoCapsFlags != TextUtils.CAP_MODE_CHARACTERS
            ) {
                val needsToResetAutoCaps: Boolean = mAlphabetShiftState.isAutomaticShifted()
                        || (mAlphabetShiftState.isManualShifted() && mShiftKeyState.isReleasing())
                if (needsToResetAutoCaps) {
                    mSwitchActions.setAlphabetKeyboard()
                }
            }
        }
    }

    fun onReleaseKey(
        code: Int, withSliding: Boolean, autoCapsFlags: Int,
        recapitalizeMode: Int
    ) {
        if (DEBUG_EVENT) {
            Log.d(
                TAG, ("onReleaseKey: code=" + Constants.printableCode(code)
                        + " sliding=" + withSliding
                        + " " + stateToString(autoCapsFlags, recapitalizeMode))
            )
        }
        if (code == Constants.CODE_SHIFT) {
            onReleaseShift(withSliding, autoCapsFlags, recapitalizeMode)
        } else if (code == Constants.CODE_CAPSLOCK) {
            setShiftLocked(!mAlphabetShiftState.isShiftLocked())
        } else if (code == Constants.CODE_SWITCH_ALPHA_SYMBOL) {
            onReleaseSymbol(withSliding, autoCapsFlags, recapitalizeMode)
        }
    }

    private fun onPressSymbol(
        autoCapsFlags: Int,
        recapitalizeMode: Int
    ) {
        toggleAlphabetAndSymbols(autoCapsFlags, recapitalizeMode)
        mSymbolKeyState.onPress()
        mSwitchState = SWITCH_STATE_MOMENTARY_ALPHA_AND_SYMBOL
    }

    private fun onReleaseSymbol(
        withSliding: Boolean, autoCapsFlags: Int,
        recapitalizeMode: Int
    ) {
        if (mSymbolKeyState.isChording()) {
            // Switch back to the previous keyboard mode if the user chords the mode change key and
            // another key, then releases the mode change key.
            toggleAlphabetAndSymbols(autoCapsFlags, recapitalizeMode)
        } else if (!withSliding) {
            // If the mode change key is being released without sliding, we should forget the
            // previous symbols keyboard shift state and simply switch back to symbols layout
            // (never symbols shifted) next time the mode gets changed to symbols layout.
            mPrevSymbolsKeyboardWasShifted = false
        }
        mSymbolKeyState.onRelease()
    }

    fun onUpdateShiftState(autoCapsFlags: Int, recapitalizeMode: Int) {
        if (DEBUG_EVENT) {
            Log.d(TAG, "onUpdateShiftState: " + stateToString(autoCapsFlags, recapitalizeMode))
        }
        mRecapitalizeMode = recapitalizeMode
        updateAlphabetShiftState(autoCapsFlags, recapitalizeMode)
    }

    // TODO: Remove this method. Come up with a more comprehensive way to reset the keyboard layout
    // when a keyboard layout set doesn't get reloaded in LatinIME.onStartInputViewInternal().
    fun onResetKeyboardStateToAlphabet(
        autoCapsFlags: Int,
        recapitalizeMode: Int
    ) {
        if (DEBUG_EVENT) {
            Log.d(
                TAG, "onResetKeyboardStateToAlphabet: "
                        + stateToString(autoCapsFlags, recapitalizeMode)
            )
        }
        resetKeyboardStateToAlphabet(autoCapsFlags, recapitalizeMode)
    }

    private fun updateShiftStateForRecapitalize(recapitalizeMode: Int) {
        when (recapitalizeMode) {
            RecapitalizeStatus.CAPS_MODE_ALL_UPPER -> setShifted(
                SHIFT_LOCK_SHIFTED
            )

            RecapitalizeStatus.CAPS_MODE_FIRST_WORD_UPPER -> setShifted(AUTOMATIC_SHIFT)
            RecapitalizeStatus.CAPS_MODE_ALL_LOWER, RecapitalizeStatus.CAPS_MODE_ORIGINAL_MIXED_CASE -> setShifted(
                UNSHIFT
            )

            else -> setShifted(UNSHIFT)
        }
    }

    private fun updateAlphabetShiftState(autoCapsFlags: Int, recapitalizeMode: Int) {
        if (!mIsAlphabetMode) return
        if (RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE != recapitalizeMode) {
            // We are recapitalizing. Match the keyboard to the current recapitalize state.
            updateShiftStateForRecapitalize(recapitalizeMode)
            return
        }
        if (!mShiftKeyState.isReleasing()) {
            // Ignore update shift state event while the shift key is being pressed (including
            // chording).
            return
        }
        if (!mAlphabetShiftState.isShiftLocked() && !mShiftKeyState.isIgnoring()) {
            if (mShiftKeyState.isReleasing() && autoCapsFlags != Constants.TextUtils.CAP_MODE_OFF) {
                // Only when shift key is releasing, automatic temporary upper case will be set.
                setShifted(AUTOMATIC_SHIFT)
            } else {
                setShifted(if (mShiftKeyState.isChording()) MANUAL_SHIFT else UNSHIFT)
            }
        }
    }

    private fun onPressShift() {
        // If we are recapitalizing, we don't do any of the normal processing, including
        // importantly the double tap timer.
        if (RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE != mRecapitalizeMode) {
            return
        }
        if (mIsAlphabetMode) {
            mIsInDoubleTapShiftKey = mSwitchActions.isInDoubleTapShiftKeyTimeout()
            if (!mIsInDoubleTapShiftKey) {
                // This is first tap.
                mSwitchActions.startDoubleTapShiftKeyTimer()
            }
            if (mIsInDoubleTapShiftKey) {
                if (mAlphabetShiftState.isManualShifted() || mIsInAlphabetUnshiftedFromShifted) {
                    // Shift key has been double tapped while in manual shifted or automatic
                    // shifted state.
                    setShiftLocked(true)
                } else {
                    // Shift key has been double tapped while in normal state. This is the second
                    // tap to disable shift locked state, so just ignore this.
                }
            } else {
                if (mAlphabetShiftState.isShiftLocked()) {
                    // Shift key is pressed while shift locked state, we will treat this state as
                    // shift lock shifted state and mark as if shift key pressed while normal
                    // state.
                    setShifted(SHIFT_LOCK_SHIFTED)
                    mShiftKeyState.onPress()
                } else if (mAlphabetShiftState.isAutomaticShifted()) {
                    // Shift key is pressed while automatic shifted, we have to move to manual
                    // shifted.
                    setShifted(MANUAL_SHIFT)
                    mShiftKeyState.onPress()
                } else if (mAlphabetShiftState.isShiftedOrShiftLocked()) {
                    // In manual shifted state, we just record shift key has been pressing while
                    // shifted state.
                    mShiftKeyState.onPressOnShifted()
                } else {
                    // In base layout, chording or manual shifted mode is started.
                    setShifted(MANUAL_SHIFT)
                    mShiftKeyState.onPress()
                }
            }
        } else {
            // In symbol mode, just toggle symbol and symbol more keyboard.
            toggleShiftInSymbols()
            mSwitchState = SWITCH_STATE_MOMENTARY_SYMBOL_AND_MORE
            mShiftKeyState.onPress()
        }
    }

    private fun onReleaseShift(
        withSliding: Boolean, autoCapsFlags: Int,
        recapitalizeMode: Int
    ) {
        if (RecapitalizeStatus.NOT_A_RECAPITALIZE_MODE != mRecapitalizeMode) {
            // We are recapitalizing. We should match the keyboard state to the recapitalize
            // state in priority.
            updateShiftStateForRecapitalize(mRecapitalizeMode)
        } else if (mIsAlphabetMode) {
            val isShiftLocked: Boolean = mAlphabetShiftState.isShiftLocked()
            mIsInAlphabetUnshiftedFromShifted = false
            if (mIsInDoubleTapShiftKey) {
                // Double tap shift key has been handled in {@link #onPressShift}, so that just
                // ignore this release shift key here.
                mIsInDoubleTapShiftKey = false
            } else if (mShiftKeyState.isChording()) {
                if (mAlphabetShiftState.isShiftLockShifted()) {
                    // After chording input while shift locked state.
                    setShiftLocked(true)
                } else {
                    // After chording input while normal state.
                    setShifted(UNSHIFT)
                }
                // After chording input, automatic shift state may have been changed depending on
                // what characters were input.
                mShiftKeyState.onRelease()
                mSwitchActions.requestUpdatingShiftState(autoCapsFlags, recapitalizeMode)
                return
            } else if (mAlphabetShiftState.isShiftLockShifted() && withSliding) {
                // In shift locked state, shift has been pressed and slid out to other key.
                setShiftLocked(true)
            } else if (mAlphabetShiftState.isManualShifted() && withSliding) {
                // Shift has been pressed and slid out to other key.
                mSwitchState = SWITCH_STATE_MOMENTARY_ALPHA_SHIFT
            } else if (isShiftLocked && !mAlphabetShiftState.isShiftLockShifted()
                && (mShiftKeyState.isPressing() || mShiftKeyState.isPressingOnShifted())
                && !withSliding
            ) {
                // Shift has been long pressed, ignore this release.
            } else if (isShiftLocked && !mShiftKeyState.isIgnoring() && !withSliding) {
                // Shift has been pressed without chording while shift locked state.
                setShiftLocked(false)
            } else if (mAlphabetShiftState.isShiftedOrShiftLocked()
                && mShiftKeyState.isPressingOnShifted() && !withSliding
            ) {
                // Shift has been pressed without chording while shifted state.
                setShifted(UNSHIFT)
                mIsInAlphabetUnshiftedFromShifted = true
            } else if (mAlphabetShiftState.isManualShiftedFromAutomaticShifted()
                && mShiftKeyState.isPressing() && !withSliding
            ) {
                // Shift has been pressed without chording while manual shifted transited from
                // automatic shifted
                setShifted(UNSHIFT)
                mIsInAlphabetUnshiftedFromShifted = true
            }
        } else {
            // In symbol mode, switch back to the previous keyboard mode if the user chords the
            // shift key and another key, then releases the shift key.
            if (mShiftKeyState.isChording()) {
                toggleShiftInSymbols()
            }
        }
        mShiftKeyState.onRelease()
    }

    fun onFinishSlidingInput(autoCapsFlags: Int, recapitalizeMode: Int) {
        if (DEBUG_EVENT) {
            Log.d(TAG, "onFinishSlidingInput: " + stateToString(autoCapsFlags, recapitalizeMode))
        }
        // Switch back to the previous keyboard mode if the user cancels sliding input.
        when (mSwitchState) {
            SWITCH_STATE_MOMENTARY_ALPHA_AND_SYMBOL -> toggleAlphabetAndSymbols(
                autoCapsFlags,
                recapitalizeMode
            )

            SWITCH_STATE_MOMENTARY_SYMBOL_AND_MORE -> toggleShiftInSymbols()
            SWITCH_STATE_MOMENTARY_ALPHA_SHIFT -> setAlphabetKeyboard(
                autoCapsFlags,
                recapitalizeMode
            )
        }
    }

    fun onEvent(event: Event, autoCapsFlags: Int, recapitalizeMode: Int) {
        val code: Int = if (event.isFunctionalKeyEvent) event.mKeyCode else event.mCodePoint
        if (DEBUG_EVENT) {
            Log.d(
                TAG, ("onEvent: code=" + Constants.printableCode(code)
                        + " " + stateToString(autoCapsFlags, recapitalizeMode))
            )
        }

        when (mSwitchState) {
            SWITCH_STATE_MOMENTARY_ALPHA_AND_SYMBOL -> if (code == Constants.CODE_SWITCH_ALPHA_SYMBOL) {
                // Detected only the mode change key has been pressed, and then released.
                if (mIsAlphabetMode) {
                    mSwitchState = SWITCH_STATE_ALPHA
                } else {
                    mSwitchState = SWITCH_STATE_SYMBOL_BEGIN
                }
            }

            SWITCH_STATE_MOMENTARY_SYMBOL_AND_MORE -> if (code == Constants.CODE_SHIFT) {
                // Detected only the shift key has been pressed on symbol layout, and then
                // released.
                mSwitchState = SWITCH_STATE_SYMBOL_BEGIN
            }

            SWITCH_STATE_SYMBOL_BEGIN -> {
                if (mIsEmojiMode) {
                    // When in the Emoji keyboard, we don't want to switch back to the main layout even
                    // after the user hits an emoji letter followed by an enter or a space.
                } else {
                    if (!isSpaceOrEnter(code) && (Constants.isLetterCode(code)
                                || code == Constants.CODE_OUTPUT_TEXT)
                    ) {
                        mSwitchState = SWITCH_STATE_SYMBOL
                    }
                }
            }

            SWITCH_STATE_SYMBOL ->             // Switch back to alpha keyboard mode if user types one or more non-space/enter
                // characters followed by a space/enter.
                if (isSpaceOrEnter(code)) {
                    toggleAlphabetAndSymbols(autoCapsFlags, recapitalizeMode)
                    mPrevSymbolsKeyboardWasShifted = false
                }
        }

        // If the code is a letter, update keyboard shift state.
        if (Constants.isLetterCode(code)) {
            updateAlphabetShiftState(autoCapsFlags, recapitalizeMode)
        } else if (code == Constants.CODE_EMOJI) {
            setEmojiKeyboard()
        } else if (code == Constants.CODE_ALPHA_FROM_EMOJI) {
            setAlphabetKeyboard(autoCapsFlags, recapitalizeMode)
        }
    }

    override fun toString(): String {
        return ("[keyboard=" + (if (mIsAlphabetMode)
            mAlphabetShiftState.toString()
        else
            (if (mIsSymbolShifted) "SYMBOLS_SHIFTED" else "SYMBOLS"))
                + " shift=" + mShiftKeyState
                + " symbol=" + mSymbolKeyState
                + " switch=" + switchStateToString(mSwitchState) + "]")
    }

    private fun stateToString(autoCapsFlags: Int, recapitalizeMode: Int): String {
        return (this.toString() + " autoCapsFlags=" + CapsModeUtils.flagsToString(autoCapsFlags)
                + " recapitalizeMode=" + RecapitalizeStatus.modeToString(recapitalizeMode))
    }

    companion object {
        private val TAG: String = KeyboardState::class.java.getSimpleName()
        private const val DEBUG_EVENT: Boolean = false
        private const val DEBUG_INTERNAL_ACTION: Boolean = false

        // TODO: Merge {@link #mSwitchState}, {@link #mIsAlphabetMode}, {@link #mAlphabetShiftState},
        // {@link #mIsSymbolShifted}, {@link #mPrevMainKeyboardWasShiftLocked}, and
        // {@link #mPrevSymbolsKeyboardWasShifted} into single state variable.
        private const val SWITCH_STATE_ALPHA: Int = 0
        private const val SWITCH_STATE_SYMBOL_BEGIN: Int = 1
        private const val SWITCH_STATE_SYMBOL: Int = 2
        private const val SWITCH_STATE_MOMENTARY_ALPHA_AND_SYMBOL: Int = 3
        private const val SWITCH_STATE_MOMENTARY_SYMBOL_AND_MORE: Int = 4
        private const val SWITCH_STATE_MOMENTARY_ALPHA_SHIFT: Int = 5

        // Constants for {@link SavedKeyboardState#mShiftMode} and {@link #setShifted(int)}.
        private const val UNSHIFT: Int = 0
        private const val MANUAL_SHIFT: Int = 1
        private const val AUTOMATIC_SHIFT: Int = 2
        private const val SHIFT_LOCK_SHIFTED: Int = 3

        private fun isSpaceOrEnter(c: Int): Boolean {
            return c == Constants.CODE_SPACE || c == Constants.CODE_ENTER
        }

        fun shiftModeToString(shiftMode: Int): String? {
            when (shiftMode) {
                UNSHIFT -> return "UNSHIFT"
                MANUAL_SHIFT -> return "MANUAL"
                AUTOMATIC_SHIFT -> return "AUTOMATIC"
                else -> return null
            }
        }

        private fun switchStateToString(switchState: Int): String? {
            when (switchState) {
                SWITCH_STATE_ALPHA -> return "ALPHA"
                SWITCH_STATE_SYMBOL_BEGIN -> return "SYMBOL-BEGIN"
                SWITCH_STATE_SYMBOL -> return "SYMBOL"
                SWITCH_STATE_MOMENTARY_ALPHA_AND_SYMBOL -> return "MOMENTARY-ALPHA-SYMBOL"
                SWITCH_STATE_MOMENTARY_SYMBOL_AND_MORE -> return "MOMENTARY-SYMBOL-MORE"
                SWITCH_STATE_MOMENTARY_ALPHA_SHIFT -> return "MOMENTARY-ALPHA_SHIFT"
                else -> return null
            }
        }
    }
}
