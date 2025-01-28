/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.inputmethod.accessibility

import android.content.Context
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import android.util.SparseIntArray
import android.view.MotionEvent
import com.android.inputmethod.accessibility.AccessibilityLongPressTimer.LongPressTimerCallback
import com.android.inputmethod.keyboard.Key
import com.android.inputmethod.keyboard.KeyDetector
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.keyboard.KeyboardId
import com.android.inputmethod.keyboard.MainKeyboardView
import com.android.inputmethod.keyboard.PointerTracker
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.utils.SubtypeLocaleUtils

/**
 * This class represents a delegate that can be registered in [MainKeyboardView] to enhance
 * accessibility support via composition rather via inheritance.
 */
class MainKeyboardAccessibilityDelegate
    (
    mainKeyboardView: MainKeyboardView,
    keyDetector: KeyDetector
) : KeyboardAccessibilityDelegate<MainKeyboardView>(mainKeyboardView, keyDetector),
    LongPressTimerCallback {
    /** The most recently set keyboard mode.  */
    private var mLastKeyboardMode: Int = KEYBOARD_IS_HIDDEN

    // The rectangle region to ignore hover events.
    private val mBoundsToIgnoreHoverEvent: Rect = Rect()


    /**
     * {@inheritDoc}
     */
    override fun setKeyboard(keyboard: Keyboard?) {
        if (keyboard == null) {
            return
        }
        val lastKeyboard: Keyboard? = getKeyboard()
        super.setKeyboard(keyboard)
        val lastKeyboardMode: Int = mLastKeyboardMode
        mLastKeyboardMode = keyboard.mId.mMode

        // Since this method is called even when accessibility is off, make sure
        // to check the state before announcing anything.
        if (!AccessibilityUtils.instance.isAccessibilityEnabled()) {
            return
        }
        // Announce the language name only when the language is changed.
        if (lastKeyboard == null || keyboard.mId.mSubtype != lastKeyboard.mId.mSubtype) {
            announceKeyboardLanguage(keyboard)
            return
        }
        // Announce the mode only when the mode is changed.
        if (keyboard.mId.mMode != lastKeyboardMode) {
            announceKeyboardMode(keyboard)
            return
        }
        // Announce the keyboard type only when the type is changed.
        if (keyboard.mId.mElementId != lastKeyboard.mId.mElementId) {
            announceKeyboardType(keyboard, lastKeyboard)
            return
        }
    }

    /**
     * Called when the keyboard is hidden and accessibility is enabled.
     */
    fun onHideWindow() {
        if (mLastKeyboardMode != KEYBOARD_IS_HIDDEN) {
            announceKeyboardHidden()
        }
        mLastKeyboardMode = KEYBOARD_IS_HIDDEN
    }

    /**
     * Announces which language of keyboard is being displayed.
     *
     * @param keyboard The new keyboard.
     */
    private fun announceKeyboardLanguage(keyboard: Keyboard) {
        val languageText: String = SubtypeLocaleUtils.getSubtypeDisplayNameInSystemLocale(
            keyboard.mId.mSubtype?.rawSubtype!!
        )
        sendWindowStateChanged(languageText)
    }

    /**
     * Announces which type of keyboard is being displayed.
     * If the keyboard type is unknown, no announcement is made.
     *
     * @param keyboard The new keyboard.
     */
    private fun announceKeyboardMode(keyboard: Keyboard) {
        val context: Context = mKeyboardView.context
        val modeTextResId: Int = KEYBOARD_MODE_RES_IDS.get(
            keyboard.mId.mMode
        )
        if (modeTextResId == 0) {
            return
        }
        val modeText: String = context.getString(modeTextResId)
        val text: String = context.getString(R.string.announce_keyboard_mode, modeText)
        sendWindowStateChanged(text)
    }

    /**
     * Announces which type of keyboard is being displayed.
     *
     * @param keyboard The new keyboard.
     * @param lastKeyboard The last keyboard.
     */
    private fun announceKeyboardType(keyboard: Keyboard, lastKeyboard: Keyboard) {
        val lastElementId: Int = lastKeyboard.mId.mElementId
        val resId: Int
        when (keyboard.mId.mElementId) {
            KeyboardId.ELEMENT_ALPHABET_AUTOMATIC_SHIFTED, KeyboardId.ELEMENT_ALPHABET -> {
                if (lastElementId == KeyboardId.ELEMENT_ALPHABET
                    || lastElementId == KeyboardId.ELEMENT_ALPHABET_AUTOMATIC_SHIFTED
                ) {
                    // Transition between alphabet mode and automatic shifted mode should be silently
                    // ignored because it can be determined by each key's talk back announce.
                    return
                }
                resId = R.string.spoken_description_mode_alpha
            }

            KeyboardId.ELEMENT_ALPHABET_MANUAL_SHIFTED -> {
                if (lastElementId == KeyboardId.ELEMENT_ALPHABET_AUTOMATIC_SHIFTED) {
                    // Resetting automatic shifted mode by pressing the shift key causes the transition
                    // from automatic shifted to manual shifted that should be silently ignored.
                    return
                }
                resId = R.string.spoken_description_shiftmode_on
            }

            KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCK_SHIFTED -> {
                if (lastElementId == KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCKED) {
                    // Resetting caps locked mode by pressing the shift key causes the transition
                    // from shift locked to shift lock shifted that should be silently ignored.
                    return
                }
                resId = R.string.spoken_description_shiftmode_locked
            }

            KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCKED -> resId =
                R.string.spoken_description_shiftmode_locked

            KeyboardId.ELEMENT_SYMBOLS -> resId = R.string.spoken_description_mode_symbol
            KeyboardId.ELEMENT_SYMBOLS_SHIFTED -> resId =
                R.string.spoken_description_mode_symbol_shift

            KeyboardId.ELEMENT_PHONE -> resId = R.string.spoken_description_mode_phone
            KeyboardId.ELEMENT_PHONE_SYMBOLS -> resId =
                R.string.spoken_description_mode_phone_shift

            else -> return
        }
        sendWindowStateChanged(resId)
    }

    /**
     * Announces that the keyboard has been hidden.
     */
    private fun announceKeyboardHidden() {
        sendWindowStateChanged(R.string.announce_keyboard_hidden)
    }

    override fun performClickOn(key: Key) {
        val x: Int = key.hitBox.centerX()
        val y: Int = key.hitBox.centerY()
        if (DEBUG_HOVER) {
            Log.d(
                TAG, ("performClickOn: key=" + key
                        + " inIgnoreBounds=" + mBoundsToIgnoreHoverEvent.contains(x, y))
            )
        }
        if (mBoundsToIgnoreHoverEvent.contains(x, y)) {
            // This hover exit event points to the key that should be ignored.
            // Clear the ignoring region to handle further hover events.
            mBoundsToIgnoreHoverEvent.setEmpty()
            return
        }
        super.performClickOn(key)
    }

    override fun onHoverEnterTo(key: Key) {
        val x: Int = key.hitBox.centerX()
        val y: Int = key.hitBox.centerY()
        if (DEBUG_HOVER) {
            Log.d(
                TAG, ("onHoverEnterTo: key=" + key
                        + " inIgnoreBounds=" + mBoundsToIgnoreHoverEvent.contains(x, y))
            )
        }
        if (mBoundsToIgnoreHoverEvent.contains(x, y)) {
            return
        }
        // This hover enter event points to the key that isn't in the ignoring region.
        // Further hover events should be handled.
        mBoundsToIgnoreHoverEvent.setEmpty()
        super.onHoverEnterTo(key)
    }

    override fun onHoverExitFrom(key: Key) {
        val x: Int = key.hitBox.centerX()
        val y: Int = key.hitBox.centerY()
        if (DEBUG_HOVER) {
            Log.d(
                TAG, ("onHoverExitFrom: key=" + key
                        + " inIgnoreBounds=" + mBoundsToIgnoreHoverEvent.contains(x, y))
            )
        }
        super.onHoverExitFrom(key)
    }

    override fun performLongClickOn(key: Key) {
        if (DEBUG_HOVER) {
            Log.d(TAG, "performLongClickOn: key=" + key)
        }
        val tracker: PointerTracker =
            PointerTracker.getPointerTracker(KeyboardAccessibilityDelegate.HOVER_EVENT_POINTER_ID)
        val eventTime: Long = SystemClock.uptimeMillis()
        val x: Int = key.hitBox.centerX()
        val y: Int = key.hitBox.centerY()
        val downEvent: MotionEvent = MotionEvent.obtain(
            eventTime,
            eventTime,
            MotionEvent.ACTION_DOWN,
            x.toFloat(),
            y.toFloat(),
            0 /* metaState */
        )
        // Inject a fake down event to {@link PointerTracker} to handle a long press correctly.
        tracker.processMotionEvent(downEvent, mKeyDetector)
        downEvent.recycle()
        // Invoke {@link PointerTracker#onLongPressed()} as if a long press timeout has passed.
        tracker.onLongPressed()
        // If {@link Key#hasNoPanelAutoMoreKeys()} is true (such as "0 +" key on the phone layout)
        // or a key invokes IME switcher dialog, we should just ignore the next
        // {@link #onRegisterHoverKey(Key,MotionEvent)}. It can be determined by whether
        // {@link PointerTracker} is in operation or not.
        if (tracker.isInOperation()) {
            // This long press shows a more keys keyboard and further hover events should be
            // handled.
            mBoundsToIgnoreHoverEvent.setEmpty()
            return
        }
        // This long press has handled at {@link MainKeyboardView#onLongPress(PointerTracker)}.
        // We should ignore further hover events on this key.
        mBoundsToIgnoreHoverEvent.set(key.hitBox)
        if (key.hasNoPanelAutoMoreKey()) {
            // This long press has registered a code point without showing a more keys keyboard.
            // We should talk back the code point if possible.
            val codePointOfNoPanelAutoMoreKey: Int = key.moreKeys.get(0).mCode
            val text: String? =
                KeyCodeDescriptionMapper.getInstance().getDescriptionForCodePoint(
                    mKeyboardView.context, codePointOfNoPanelAutoMoreKey
                )
            if (text != null) {
                sendWindowStateChanged(text)
            }
        }
    }

    companion object {
        private val TAG: String = MainKeyboardAccessibilityDelegate::class.java.getSimpleName()

        /** Map of keyboard modes to resource IDs.  */
        private val KEYBOARD_MODE_RES_IDS: SparseIntArray = SparseIntArray()

        init {
            KEYBOARD_MODE_RES_IDS.put(KeyboardId.MODE_DATE, R.string.keyboard_mode_date)
            KEYBOARD_MODE_RES_IDS.put(
                KeyboardId.MODE_DATETIME,
                R.string.keyboard_mode_date_time
            )
            KEYBOARD_MODE_RES_IDS.put(KeyboardId.MODE_EMAIL, R.string.keyboard_mode_email)
            KEYBOARD_MODE_RES_IDS.put(KeyboardId.MODE_IM, R.string.keyboard_mode_im)
            KEYBOARD_MODE_RES_IDS.put(
                KeyboardId.MODE_NUMBER,
                R.string.keyboard_mode_number
            )
            KEYBOARD_MODE_RES_IDS.put(KeyboardId.MODE_PHONE, R.string.keyboard_mode_phone)
            KEYBOARD_MODE_RES_IDS.put(KeyboardId.MODE_TEXT, R.string.keyboard_mode_text)
            KEYBOARD_MODE_RES_IDS.put(KeyboardId.MODE_TIME, R.string.keyboard_mode_time)
            KEYBOARD_MODE_RES_IDS.put(KeyboardId.MODE_URL, R.string.keyboard_mode_url)
        }

        private val KEYBOARD_IS_HIDDEN: Int = -1
    }
}
