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
package com.android.inputmethod.accessibility

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.SystemClock
import android.provider.Settings.Secure
import android.text.TextUtils
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.EditorInfo
import com.android.inputmethod.compat.SettingsSecureCompatUtils
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.SuggestedWords
import com.android.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import com.android.inputmethod.latin.utils.InputTypeUtils

class AccessibilityUtils private constructor() {
    private var mContext: Context? = null
    private var mAccessibilityManager: AccessibilityManager? = null
    private var mAudioManager: AudioManager? = null

    /** The most recent auto-correction.  */
    private var mAutoCorrectionWord: String? = null

    /** The most recent typed word for auto-correction.  */
    private var mTypedWord: String? = null

    private fun initInternal(context: Context) {
        mContext = context
        mAccessibilityManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
    }

    /**
     * Returns `true` if accessibility is enabled. Currently, this means
     * that the kill switch is off and system accessibility is turned on.
     *
     * @return `true` if accessibility is enabled.
     */
    fun isAccessibilityEnabled(): Boolean {
        return ENABLE_ACCESSIBILITY && mAccessibilityManager!!.isEnabled()
    }

    /**
     * Returns `true` if touch exploration is enabled. Currently, this
     * means that the kill switch is off, the device supports touch exploration,
     * and system accessibility is turned on.
     *
     * @return `true` if touch exploration is enabled.
     */
    fun isTouchExplorationEnabled(): Boolean {
        return isAccessibilityEnabled() && mAccessibilityManager!!.isTouchExplorationEnabled()
    }

    /**
     * Returns whether the device should obscure typed password characters.
     * Typically this means speaking "dot" in place of non-control characters.
     *
     * @return `true` if the device should obscure password characters.
     */
    @Suppress("deprecation")
    fun shouldObscureInput(editorInfo: EditorInfo?): Boolean {
        if (editorInfo == null) return false

        // The user can optionally force speaking passwords.
        if (SettingsSecureCompatUtils.ACCESSIBILITY_SPEAK_PASSWORD != null) {
            val speakPassword: Boolean = Secure.getInt(
                mContext!!.getContentResolver(),
                SettingsSecureCompatUtils.ACCESSIBILITY_SPEAK_PASSWORD, 0
            ) != 0
            if (speakPassword) return false
        }

        // Always speak if the user is listening through headphones.
        if (mAudioManager!!.isWiredHeadsetOn() || mAudioManager!!.isBluetoothA2dpOn()) {
            return false
        }

        // Don't speak if the IME is connected to a password field.
        return InputTypeUtils.isPasswordInputType(editorInfo.inputType)
    }

    /**
     * Sets the current auto-correction word and typed word. These may be used
     * to provide the user with a spoken description of what auto-correction
     * will occur when a key is typed.
     *
     * @param suggestedWords the list of suggested auto-correction words
     */
    fun setAutoCorrection(suggestedWords: SuggestedWords) {
        if (suggestedWords.mWillAutoCorrect) {
            mAutoCorrectionWord =
                suggestedWords.getWord(SuggestedWords.INDEX_OF_AUTO_CORRECTION)
            val typedWordInfo: SuggestedWordInfo? = suggestedWords.typedWordInfo
            if (null == typedWordInfo) {
                mTypedWord = null
            } else {
                mTypedWord = typedWordInfo.word
            }
        } else {
            mAutoCorrectionWord = null
            mTypedWord = null
        }
    }

    /**
     * Obtains a description for an auto-correction key, taking into account the
     * currently typed word and auto-correction.
     *
     * @param keyCodeDescription spoken description of the key that will insert
     * an auto-correction
     * @param shouldObscure whether the key should be obscured
     * @return a description including a description of the auto-correction, if
     * needed
     */
    fun getAutoCorrectionDescription(
        keyCodeDescription: String?, shouldObscure: Boolean
    ): String? {
        if (!TextUtils.isEmpty(mAutoCorrectionWord)) {
            if (!TextUtils.equals(mAutoCorrectionWord, mTypedWord)) {
                if (shouldObscure) {
                    // This should never happen, but just in case...
                    return mContext!!.getString(
                        R.string.spoken_auto_correct_obscured,
                        keyCodeDescription
                    )
                }
                return mContext!!.getString(
                    R.string.spoken_auto_correct, keyCodeDescription,
                    mTypedWord, mAutoCorrectionWord
                )
            }
        }

        return keyCodeDescription
    }

    /**
     * Sends the specified text to the [AccessibilityManager] to be
     * spoken.
     *
     * @param view The source view.
     * @param text The text to speak.
     */
    fun announceForAccessibility(view: View, text: CharSequence?) {
        if (!mAccessibilityManager!!.isEnabled()) {
            Log.e(TAG, "Attempted to speak when accessibility was disabled!")
            return
        }

        // The following is a hack to avoid using the heavy-weight TextToSpeech
        // class. Instead, we're just forcing a fake AccessibilityEvent into
        // the screen reader to make it speak.
        val event: AccessibilityEvent = AccessibilityEvent.obtain()

        event.setPackageName(PACKAGE)
        event.setClassName(CLASS)
        event.setEventTime(SystemClock.uptimeMillis())
        event.setEnabled(true)
        event.getText().add(text)

        // Platforms starting at SDK version 16 (Build.VERSION_CODES.JELLY_BEAN) should use
        // announce events.
        if (Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            event.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT)
        } else {
            event.setEventType(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }

        val viewParent: ViewParent? = view.getParent()
        if ((viewParent == null) || viewParent !is ViewGroup) {
            Log.e(TAG, "Failed to obtain ViewParent in announceForAccessibility")
            return
        }

        viewParent.requestSendAccessibilityEvent(view, event)
    }

    /**
     * Handles speaking the "connect a headset to hear passwords" notification
     * when connecting to a password field.
     *
     * @param view The source view.
     * @param editorInfo The input connection's editor info attribute.
     * @param restarting Whether the connection is being restarted.
     */
    fun onStartInputViewInternal(
        view: View, editorInfo: EditorInfo?,
        restarting: Boolean
    ) {
        if (shouldObscureInput(editorInfo)) {
            val text: CharSequence = mContext!!.getText(R.string.spoken_use_headphones)
            announceForAccessibility(view, text)
        }
    }

    /**
     * Sends the specified [AccessibilityEvent] if accessibility is
     * enabled. No operation if accessibility is disabled.
     *
     * @param event The event to send.
     */
    fun requestSendAccessibilityEvent(event: AccessibilityEvent?) {
        if (mAccessibilityManager!!.isEnabled()) {
            mAccessibilityManager!!.sendAccessibilityEvent(event)
        }
    }

    companion object {
        private val TAG: String = AccessibilityUtils::class.java.getSimpleName()
        private val CLASS: String = AccessibilityUtils::class.java.getName()
        private val PACKAGE: String = AccessibilityUtils::class.java.getPackage().getName()

        val instance: AccessibilityUtils = AccessibilityUtils()

        /*
     * Setting this constant to {@code false} will disable all keyboard
     * accessibility code, regardless of whether Accessibility is turned on in
     * the system settings. It should ONLY be used in the event of an emergency.
     */
        private const val ENABLE_ACCESSIBILITY: Boolean = true

        fun init(context: Context) {
            if (!ENABLE_ACCESSIBILITY) return

            // These only need to be initialized if the kill switch is off.
            instance.initInternal(context)
        }

        /**
         * Returns {@true} if the provided event is a touch exploration (e.g. hover)
         * event. This is used to determine whether the event should be processed by
         * the touch exploration code within the keyboard.
         *
         * @param event The event to check.
         * @return {@true} is the event is a touch exploration event
         */
        fun isTouchExplorationEvent(event: MotionEvent): Boolean {
            val action: Int = event.getAction()
            return action == MotionEvent.ACTION_HOVER_ENTER || action == MotionEvent.ACTION_HOVER_EXIT || action == MotionEvent.ACTION_HOVER_MOVE
        }
    }
}
