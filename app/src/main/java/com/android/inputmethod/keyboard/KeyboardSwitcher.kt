/*
 * Copyright (C) 2008 The Android Open Source Project
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
package com.android.inputmethod.keyboard

import android.content.Context
import android.content.res.Resources
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import com.android.inputmethod.compat.InputMethodServiceCompatUtils
import com.android.inputmethod.event.Event
import com.android.inputmethod.keyboard.KeyboardLayoutSet.KeyboardLayoutSetException
import com.android.inputmethod.keyboard.emoji.EmojiPalettesView
import com.android.inputmethod.keyboard.internal.KeyboardState
import com.android.inputmethod.keyboard.internal.KeyboardState.SwitchActions
import com.android.inputmethod.keyboard.internal.KeyboardTextsSet
import com.android.inputmethod.latin.InputView
import com.android.inputmethod.latin.LatinIME
import ee.oyatl.ime.fusion.R
import com.android.inputmethod.latin.RichInputMethodManager
import com.android.inputmethod.latin.WordComposer
import com.android.inputmethod.latin.define.ProductionFlags
import com.android.inputmethod.latin.settings.Settings
import com.android.inputmethod.latin.settings.SettingsValues
import com.android.inputmethod.latin.utils.CapsModeUtils
import com.android.inputmethod.latin.utils.LanguageOnSpacebarUtils
import com.android.inputmethod.latin.utils.RecapitalizeStatus
import com.android.inputmethod.latin.utils.ResourceUtils
import com.android.inputmethod.latin.utils.ScriptUtils

class KeyboardSwitcher private constructor() : SwitchActions {
    private var mCurrentInputView: InputView? = null
    private var mMainKeyboardFrame: View? = null
    private var mKeyboardView: MainKeyboardView? = null
    private var mEmojiPalettesView: EmojiPalettesView? = null
    private var mLatinIME: LatinIME? = null
    private var mRichImm: RichInputMethodManager? = null
    private var mIsHardwareAcceleratedDrawingEnabled: Boolean = false

    private var mState: KeyboardState? = null

    private var mKeyboardLayoutSet: KeyboardLayoutSet? = null

    // TODO: The following {@link KeyboardTextsSet} should be in {@link KeyboardLayoutSet}.
    private val mKeyboardTextsSet: KeyboardTextsSet = KeyboardTextsSet()

    private var mKeyboardTheme: KeyboardTheme? = null
    private var mThemeContext: Context? = null

    private fun initInternal(latinIme: LatinIME) {
        mLatinIME = latinIme
        mRichImm = RichInputMethodManager
        mState = KeyboardState(this)
        mIsHardwareAcceleratedDrawingEnabled =
            InputMethodServiceCompatUtils.enableHardwareAcceleration(mLatinIME)
    }

    fun updateKeyboardTheme(displayContext: Context) {
        val themeUpdated: Boolean = updateKeyboardThemeAndContextThemeWrapper(
            displayContext, KeyboardTheme.getKeyboardTheme(displayContext /* context */)!!
        )
        if (themeUpdated && mKeyboardView != null) {
            mLatinIME!!.setInputView(
                onCreateInputView(displayContext, mIsHardwareAcceleratedDrawingEnabled)!!
            )
        }
    }

    private fun updateKeyboardThemeAndContextThemeWrapper(
        context: Context,
        keyboardTheme: KeyboardTheme
    ): Boolean {
        if (mThemeContext == null || keyboardTheme != mKeyboardTheme || mThemeContext!!.getResources() != context.getResources()) {
            mKeyboardTheme = keyboardTheme
            mThemeContext = ContextThemeWrapper(context, keyboardTheme.mStyleId)
            KeyboardLayoutSet.onKeyboardThemeChanged()
            return true
        }
        return false
    }

    fun loadKeyboard(
        editorInfo: EditorInfo?, settingsValues: SettingsValues,
        currentAutoCapsState: Int, currentRecapitalizeState: Int
    ) {
        val builder: KeyboardLayoutSet.Builder = KeyboardLayoutSet.Builder(
            mThemeContext!!, editorInfo
        )
        val res: Resources = mThemeContext!!.getResources()
        val keyboardWidth: Int = ResourceUtils.getDefaultKeyboardWidth(
            mThemeContext!!
        )
        val keyboardHeight: Int = ResourceUtils.getKeyboardHeight(res, settingsValues)
        builder.setKeyboardGeometry(keyboardWidth, keyboardHeight)
        builder.setSubtype(mRichImm!!.currentSubtype!!)
        builder.setVoiceInputKeyEnabled(settingsValues.mShowsVoiceInputKey)
        builder.setLanguageSwitchKeyEnabled(mLatinIME!!.shouldShowLanguageSwitchKey())
        builder.setSplitLayoutEnabledByUser(
            ProductionFlags.IS_SPLIT_KEYBOARD_SUPPORTED
                    && settingsValues.mIsSplitKeyboardEnabled
        )
        mKeyboardLayoutSet = builder.build()
        try {
            mState!!.onLoadKeyboard(currentAutoCapsState, currentRecapitalizeState)
            mKeyboardTextsSet.setLocale(mRichImm!!.currentSubtypeLocale!!, mThemeContext!!)
        } catch (e: KeyboardLayoutSetException) {
            Log.w(TAG, "loading keyboard failed: " + e.mKeyboardId, e.cause)
        }
    }

    fun saveKeyboardState() {
        if (getKeyboard() != null || isShowingEmojiPalettes()) {
            mState!!.onSaveKeyboardState()
        }
    }

    fun onHideWindow() {
        if (mKeyboardView != null) {
            mKeyboardView!!.onHideWindow()
        }
    }

    private fun setKeyboard(
        keyboardId: Int,
        toggleState: KeyboardSwitchState
    ) {
        // Make {@link MainKeyboardView} visible and hide {@link EmojiPalettesView}.
        val currentSettingsValues: SettingsValues? = Settings.instance.current
        setMainKeyboardFrame(currentSettingsValues!!, toggleState)
        // TODO: pass this object to setKeyboard instead of getting the current values.
        val keyboardView: MainKeyboardView? = mKeyboardView
        val oldKeyboard: Keyboard? = keyboardView?.keyboard
        val newKeyboard: Keyboard = mKeyboardLayoutSet!!.getKeyboard(keyboardId)
        keyboardView!!.keyboard = newKeyboard
        mCurrentInputView!!.setKeyboardTopPadding(newKeyboard.mTopPadding)
        keyboardView.setKeyPreviewPopupEnabled(
            currentSettingsValues.mKeyPreviewPopupOn,
            currentSettingsValues.mKeyPreviewPopupDismissDelay
        )
        keyboardView.setKeyPreviewAnimationParams(
            currentSettingsValues.mHasCustomKeyPreviewAnimationParams,
            currentSettingsValues.mKeyPreviewShowUpStartXScale,
            currentSettingsValues.mKeyPreviewShowUpStartYScale,
            currentSettingsValues.mKeyPreviewShowUpDuration,
            currentSettingsValues.mKeyPreviewDismissEndXScale,
            currentSettingsValues.mKeyPreviewDismissEndYScale,
            currentSettingsValues.mKeyPreviewDismissDuration
        )
        keyboardView.updateShortcutKey(mRichImm!!.isShortcutImeReady)
        val subtypeChanged: Boolean = (oldKeyboard == null)
                || newKeyboard.mId.mSubtype != oldKeyboard.mId.mSubtype
        val languageOnSpacebarFormatType: Int =
            LanguageOnSpacebarUtils.getLanguageOnSpacebarFormatType(
                newKeyboard.mId.mSubtype!!
            )
        val hasMultipleEnabledIMEsOrSubtypes: Boolean = mRichImm
            ?.hasMultipleEnabledIMEsOrSubtypes(true /* shouldIncludeAuxiliarySubtypes */) == true
        keyboardView.startDisplayLanguageOnSpacebar(
            subtypeChanged, languageOnSpacebarFormatType,
            hasMultipleEnabledIMEsOrSubtypes
        )
    }

    fun getKeyboard(): Keyboard? {
        if (mKeyboardView != null) {
            return mKeyboardView?.keyboard
        }
        return null
    }

    // TODO: Remove this method. Come up with a more comprehensive way to reset the keyboard layout
    // when a keyboard layout set doesn't get reloaded in LatinIME.onStartInputViewInternal().
    fun resetKeyboardStateToAlphabet(
        currentAutoCapsState: Int,
        currentRecapitalizeState: Int
    ) {
        mState!!.onResetKeyboardStateToAlphabet(currentAutoCapsState, currentRecapitalizeState)
    }

    fun onPressKey(
        code: Int, isSinglePointer: Boolean,
        currentAutoCapsState: Int, currentRecapitalizeState: Int
    ) {
        mState!!.onPressKey(code, isSinglePointer, currentAutoCapsState, currentRecapitalizeState)
    }

    fun onReleaseKey(
        code: Int, withSliding: Boolean,
        currentAutoCapsState: Int, currentRecapitalizeState: Int
    ) {
        mState!!.onReleaseKey(code, withSliding, currentAutoCapsState, currentRecapitalizeState)
    }

    fun onFinishSlidingInput(
        currentAutoCapsState: Int,
        currentRecapitalizeState: Int
    ) {
        mState!!.onFinishSlidingInput(currentAutoCapsState, currentRecapitalizeState)
    }

    // Implements {@link KeyboardState.SwitchActions}.
    override fun setAlphabetKeyboard() {
        if (SwitchActions.DEBUG_ACTION) {
            Log.d(TAG, "setAlphabetKeyboard")
        }
        setKeyboard(KeyboardId.ELEMENT_ALPHABET, KeyboardSwitchState.OTHER)
    }

    // Implements {@link KeyboardState.SwitchActions}.
    override fun setAlphabetManualShiftedKeyboard() {
        if (SwitchActions.DEBUG_ACTION) {
            Log.d(TAG, "setAlphabetManualShiftedKeyboard")
        }
        setKeyboard(KeyboardId.ELEMENT_ALPHABET_MANUAL_SHIFTED, KeyboardSwitchState.OTHER)
    }

    // Implements {@link KeyboardState.SwitchActions}.
    override fun setAlphabetAutomaticShiftedKeyboard() {
        if (SwitchActions.DEBUG_ACTION) {
            Log.d(TAG, "setAlphabetAutomaticShiftedKeyboard")
        }
        setKeyboard(
            KeyboardId.ELEMENT_ALPHABET_AUTOMATIC_SHIFTED,
            KeyboardSwitchState.OTHER
        )
    }

    // Implements {@link KeyboardState.SwitchActions}.
    override fun setAlphabetShiftLockedKeyboard() {
        if (SwitchActions.DEBUG_ACTION) {
            Log.d(TAG, "setAlphabetShiftLockedKeyboard")
        }
        setKeyboard(KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCKED, KeyboardSwitchState.OTHER)
    }

    // Implements {@link KeyboardState.SwitchActions}.
    override fun setAlphabetShiftLockShiftedKeyboard() {
        if (SwitchActions.DEBUG_ACTION) {
            Log.d(TAG, "setAlphabetShiftLockShiftedKeyboard")
        }
        setKeyboard(
            KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCK_SHIFTED,
            KeyboardSwitchState.OTHER
        )
    }

    // Implements {@link KeyboardState.SwitchActions}.
    override fun setSymbolsKeyboard() {
        if (SwitchActions.DEBUG_ACTION) {
            Log.d(TAG, "setSymbolsKeyboard")
        }
        setKeyboard(KeyboardId.ELEMENT_SYMBOLS, KeyboardSwitchState.OTHER)
    }

    // Implements {@link KeyboardState.SwitchActions}.
    override fun setSymbolsShiftedKeyboard() {
        if (SwitchActions.DEBUG_ACTION) {
            Log.d(TAG, "setSymbolsShiftedKeyboard")
        }
        setKeyboard(
            KeyboardId.ELEMENT_SYMBOLS_SHIFTED,
            KeyboardSwitchState.SYMBOLS_SHIFTED
        )
    }

    fun isImeSuppressedByHardwareKeyboard(
        settingsValues: SettingsValues,
        toggleState: KeyboardSwitchState
    ): Boolean {
        return settingsValues.mHasHardwareKeyboard && toggleState == KeyboardSwitchState.HIDDEN
    }

    private fun setMainKeyboardFrame(
        settingsValues: SettingsValues,
        toggleState: KeyboardSwitchState
    ) {
        val visibility: Int = if (isImeSuppressedByHardwareKeyboard(settingsValues, toggleState))
            View.GONE
        else
            View.VISIBLE
        mKeyboardView!!.setVisibility(visibility)
        // The visibility of {@link #mKeyboardView} must be aligned with {@link #MainKeyboardFrame}.
        // @see #getVisibleKeyboardView() and
        // @see LatinIME#onComputeInset(android.inputmethodservice.InputMethodService.Insets)
        mMainKeyboardFrame!!.setVisibility(visibility)
        mEmojiPalettesView!!.setVisibility(View.GONE)
        mEmojiPalettesView!!.stopEmojiPalettes()
    }

    // Implements {@link KeyboardState.SwitchActions}.
    override fun setEmojiKeyboard() {
        if (SwitchActions.DEBUG_ACTION) {
            Log.d(TAG, "setEmojiKeyboard")
        }
        val keyboard: Keyboard =
            mKeyboardLayoutSet!!.getKeyboard(KeyboardId.ELEMENT_ALPHABET)
        mMainKeyboardFrame!!.setVisibility(View.GONE)
        // The visibility of {@link #mKeyboardView} must be aligned with {@link #MainKeyboardFrame}.
        // @see #getVisibleKeyboardView() and
        // @see LatinIME#onComputeInset(android.inputmethodservice.InputMethodService.Insets)
        mKeyboardView!!.setVisibility(View.GONE)
        mEmojiPalettesView!!.startEmojiPalettes(
            mKeyboardTextsSet.getText(KeyboardTextsSet.SWITCH_TO_ALPHA_KEY_LABEL),
            mKeyboardView?.keyVisualAttribute, keyboard.mIconsSet
        )
        mEmojiPalettesView!!.setVisibility(View.VISIBLE)
    }

    enum class KeyboardSwitchState(keyboardId: Int) {
        HIDDEN(-1),
        SYMBOLS_SHIFTED(KeyboardId.ELEMENT_SYMBOLS_SHIFTED),
        EMOJI(KeyboardId.ELEMENT_EMOJI_RECENTS),
        OTHER(-1);

        val mKeyboardId: Int

        init {
            mKeyboardId = keyboardId
        }
    }

    fun getKeyboardSwitchState(): KeyboardSwitchState {
        val hidden: Boolean = !isShowingEmojiPalettes()
                && (mKeyboardLayoutSet == null || mKeyboardView == null || !mKeyboardView!!.isShown())
        var state: KeyboardSwitchState?
        if (hidden) {
            return KeyboardSwitchState.HIDDEN
        } else if (isShowingEmojiPalettes()) {
            return KeyboardSwitchState.EMOJI
        } else if (isShowingKeyboardId(KeyboardId.ELEMENT_SYMBOLS_SHIFTED)) {
            return KeyboardSwitchState.SYMBOLS_SHIFTED
        }
        return KeyboardSwitchState.OTHER
    }

    fun onToggleKeyboard(toggleState: KeyboardSwitchState) {
        val currentState: KeyboardSwitchState = getKeyboardSwitchState()
        Log.w(TAG, "onToggleKeyboard() : Current = " + currentState + " : Toggle = " + toggleState)
        if (currentState == toggleState) {
            mLatinIME!!.stopShowingInputView()
            mLatinIME!!.hideWindow()
            setAlphabetKeyboard()
        } else {
            mLatinIME!!.startShowingInputView(true)
            if (toggleState == KeyboardSwitchState.EMOJI) {
                setEmojiKeyboard()
            } else {
                mEmojiPalettesView!!.stopEmojiPalettes()
                mEmojiPalettesView!!.setVisibility(View.GONE)

                mMainKeyboardFrame!!.setVisibility(View.VISIBLE)
                mKeyboardView!!.setVisibility(View.VISIBLE)
                setKeyboard(toggleState.mKeyboardId, toggleState)
            }
        }
    }

    // Future method for requesting an updating to the shift state.
    override fun requestUpdatingShiftState(autoCapsFlags: Int, recapitalizeMode: Int) {
        if (SwitchActions.DEBUG_ACTION) {
            Log.d(
                TAG, ("requestUpdatingShiftState: "
                        + " autoCapsFlags=" + CapsModeUtils.flagsToString(autoCapsFlags)
                        + " recapitalizeMode=" + RecapitalizeStatus.modeToString(
                    recapitalizeMode
                ))
            )
        }
        mState!!.onUpdateShiftState(autoCapsFlags, recapitalizeMode)
    }

    // Implements {@link KeyboardState.SwitchActions}.
    override fun startDoubleTapShiftKeyTimer() {
        if (SwitchActions.DEBUG_TIMER_ACTION) {
            Log.d(TAG, "startDoubleTapShiftKeyTimer")
        }
        val keyboardView: MainKeyboardView? = getMainKeyboardView()
        if (keyboardView != null) {
            keyboardView.startDoubleTapShiftKeyTimer()
        }
    }

    // Implements {@link KeyboardState.SwitchActions}.
    override fun cancelDoubleTapShiftKeyTimer() {
        if (SwitchActions.DEBUG_TIMER_ACTION) {
            Log.d(TAG, "setAlphabetKeyboard")
        }
        val keyboardView: MainKeyboardView? = getMainKeyboardView()
        if (keyboardView != null) {
            keyboardView.cancelDoubleTapShiftKeyTimer()
        }
    }

    // Implements {@link KeyboardState.SwitchActions}.
    override fun isInDoubleTapShiftKeyTimeout(): Boolean {
        if (SwitchActions.DEBUG_TIMER_ACTION) {
            Log.d(TAG, "isInDoubleTapShiftKeyTimeout")
        }
        val keyboardView: MainKeyboardView? = getMainKeyboardView()
        return keyboardView != null && keyboardView.isInDoubleTapShiftKeyTimeout()
    }

    /**
     * Updates state machine to figure out when to automatically switch back to the previous mode.
     */
    fun onEvent(
        event: Event, currentAutoCapsState: Int,
        currentRecapitalizeState: Int
    ) {
        mState!!.onEvent(event, currentAutoCapsState, currentRecapitalizeState)
    }

    fun isShowingKeyboardId(vararg keyboardIds: Int): Boolean {
        if (mKeyboardView == null || !mKeyboardView!!.isShown()) {
            return false
        }
        val activeKeyboardId: Int = mKeyboardView?.keyboard?.mId?.mElementId!!
        for (keyboardId: Int in keyboardIds) {
            if (activeKeyboardId == keyboardId) {
                return true
            }
        }
        return false
    }

    fun isShowingEmojiPalettes(): Boolean {
        return mEmojiPalettesView != null && mEmojiPalettesView!!.isShown()
    }

    fun isShowingMoreKeysPanel(): Boolean {
        if (isShowingEmojiPalettes()) {
            return false
        }
        return mKeyboardView!!.isShowingMoreKeysPanel()
    }

    fun getVisibleKeyboardView(): View? {
        if (isShowingEmojiPalettes()) {
            return mEmojiPalettesView
        }
        return mKeyboardView
    }

    fun getMainKeyboardView(): MainKeyboardView? {
        return mKeyboardView
    }

    fun deallocateMemory() {
        if (mKeyboardView != null) {
            mKeyboardView!!.cancelAllOngoingEvents()
            mKeyboardView!!.deallocateMemory()
        }
        if (mEmojiPalettesView != null) {
            mEmojiPalettesView!!.stopEmojiPalettes()
        }
    }

    fun onCreateInputView(
        displayContext: Context,
        isHardwareAcceleratedDrawingEnabled: Boolean
    ): View? {
        if (mKeyboardView != null) {
            mKeyboardView!!.closing()
        }

        updateKeyboardThemeAndContextThemeWrapper(
            displayContext, KeyboardTheme.getKeyboardTheme(displayContext /* context */)!!
        )
        mCurrentInputView = LayoutInflater.from(mThemeContext).inflate(
            R.layout.input_view, null
        ) as InputView?
        mMainKeyboardFrame = mCurrentInputView!!.findViewById(R.id.main_keyboard_frame)
        mEmojiPalettesView = mCurrentInputView!!.findViewById<View>(
            R.id.emoji_palettes_view
        ) as EmojiPalettesView?

        mKeyboardView =
            mCurrentInputView!!.findViewById<View>(R.id.keyboard_view) as MainKeyboardView?
        mKeyboardView!!.setHardwareAcceleratedDrawingEnabled(isHardwareAcceleratedDrawingEnabled)
        mKeyboardView!!.setKeyboardActionListener(mLatinIME)
        mEmojiPalettesView!!.setHardwareAcceleratedDrawingEnabled(
            isHardwareAcceleratedDrawingEnabled
        )
        mEmojiPalettesView!!.setKeyboardActionListener(mLatinIME)
        return mCurrentInputView
    }

    fun getKeyboardShiftMode(): Int {
        val keyboard: Keyboard? = getKeyboard()
        if (keyboard == null) {
            return WordComposer.CAPS_MODE_OFF
        }
        when (keyboard.mId.mElementId) {
            KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCKED, KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCK_SHIFTED -> return WordComposer.CAPS_MODE_MANUAL_SHIFT_LOCKED
            KeyboardId.ELEMENT_ALPHABET_MANUAL_SHIFTED -> return WordComposer.CAPS_MODE_MANUAL_SHIFTED
            KeyboardId.ELEMENT_ALPHABET_AUTOMATIC_SHIFTED -> return WordComposer.CAPS_MODE_AUTO_SHIFTED
            else -> return WordComposer.CAPS_MODE_OFF
        }
    }

    fun getCurrentKeyboardScriptId(): Int {
        if (null == mKeyboardLayoutSet) {
            return ScriptUtils.SCRIPT_UNKNOWN
        }
        return mKeyboardLayoutSet!!.getScriptId()
    }

    companion object {
        private val TAG: String = KeyboardSwitcher::class.java.getSimpleName()

        private val sInstance: KeyboardSwitcher = KeyboardSwitcher()

        fun getInstance(): KeyboardSwitcher {
            return sInstance
        }

        fun init(latinIme: LatinIME) {
            sInstance.initInternal(latinIme)
        }
    }
}
