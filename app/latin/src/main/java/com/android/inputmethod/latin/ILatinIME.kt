package com.android.inputmethod.latin

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodSubtype
import com.android.inputmethod.event.Event
import com.android.inputmethod.keyboard.KeyboardActionListener
import com.android.inputmethod.keyboard.KeyboardSwitcher
import com.android.inputmethod.latin.DictionaryFacilitator.DictionaryInitializationListener
import com.android.inputmethod.latin.LatinIME.UIHandler
import com.android.inputmethod.latin.Suggest.OnGetSuggestedWordsCallback
import com.android.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import com.android.inputmethod.latin.common.InputPointers
import com.android.inputmethod.latin.inputlogic.InputLogic
import com.android.inputmethod.latin.permissions.PermissionsManager.PermissionsResultCallback
import com.android.inputmethod.latin.settings.Settings
import com.android.inputmethod.latin.suggestions.SuggestionStripView
import com.android.inputmethod.latin.suggestions.SuggestionStripViewAccessor
import javax.annotation.Nonnull

interface ILatinIME : KeyboardActionListener, SuggestionStripView.Listener,
    SuggestionStripViewAccessor, DictionaryInitializationListener, PermissionsResultCallback {

    val context: Context?

    val handler: UIHandler?

    val inputLogic: InputLogic?

    val settings: Settings?

    val keyboardSwitcher: KeyboardSwitcher?

    val isInputViewShown: Boolean

    val currentInputEditorInfo: EditorInfo?

    val resources: Resources?

    val currentInputConnection: InputConnection?

    // Note that this method is called from a non-UI thread.
    override fun onUpdateMainDictionaryAvailability(isMainDictionaryAvailable: Boolean)

    fun resetDictionaryFacilitatorIfNecessary()

    /**
     * Reset suggest by loading the main dictionary of the current locale.
     */
    /* package private */
    fun resetSuggestMainDict()

    fun setInputView(view: View?)

    fun onStartInputInternal(editorInfo: EditorInfo?, restarting: Boolean)

    fun onStartInputViewInternal(editorInfo: EditorInfo?, restarting: Boolean)

    fun onFinishInputInternal()

    fun onFinishInputViewInternal(finishingInput: Boolean)

    fun deallocateMemory()

    fun hideWindow()

    fun startShowingInputView(needsToLoadKeyboard: Boolean)

    fun stopShowingInputView()

    fun onEvaluateInputViewShown(): Boolean

    fun updateFullscreenMode()

    val currentAutoCapsState: Int

    val currentRecapitalizeState: Int

    /**
     * @param codePoints code points to get coordinates for.
     * @return x,y coordinates for this keyboard, as a flattened array.
     */
    fun getCoordinatesForCurrentKeyboard(codePoints: IntArray?): IntArray?

    // Callback for the {@link SuggestionStripView}, to call when the important notice strip is
    // pressed.
    override fun showImportantNoticeContents()

    override fun onRequestPermissionsResult(allGranted: Boolean)

    fun displaySettingsDialog()

    override fun onCustomRequest(requestCode: Int): Boolean

    fun switchLanguage(subtype: InputMethodSubtype?)

    // TODO: Revise the language switch key behavior to make it much smarter and more reasonable.
    fun switchToNextSubtype()

    // Implementation of {@link KeyboardActionListener}.
    override fun onCodeInput(
        codePoint: Int, x: Int, y: Int,
        isKeyRepeat: Boolean
    )

    // This method is for testability of LatinIME, but also in the future it should
    // completely replace #onCodeInput.
    fun onEvent(@Nonnull event: Event)

    // Called from PointerTracker through the KeyboardActionListener interface
    override fun onTextInput(rawText: String?)

    override fun onStartBatchInput()

    override fun onUpdateBatchInput(batchPointers: InputPointers?)

    override fun onEndBatchInput(batchPointers: InputPointers?)

    override fun onCancelBatchInput()

    /**
     * To be called after the InputLogic has gotten a chance to act on the suggested words by the
     * IME for the full gesture, possibly updating the TextView to reflect the first suggestion.
     * 
     * 
     * This method must be run on the UI Thread.
     * @param suggestedWords suggested words by the IME for the full gesture.
     */
    fun onTailBatchInputResultShown(suggestedWords: SuggestedWords?)

    // This method must run on the UI Thread.
    fun showGesturePreviewAndSuggestionStrip(
        @Nonnull suggestedWords: SuggestedWords,
        dismissGestureFloatingPreviewText: Boolean
    )

    // Called from PointerTracker through the KeyboardActionListener interface
    override fun onFinishSlidingInput()

    // Called from PointerTracker through the KeyboardActionListener interface
    override fun onCancelInput()

    fun hasSuggestionStripView(): Boolean

    // TODO[IL]: Move this out of LatinIME.
    fun getSuggestedWords(
        inputStyle: Int, sequenceNumber: Int,
        callback: OnGetSuggestedWordsCallback?
    )

    override fun showSuggestionStrip(suggestedWords: SuggestedWords?)

    // Called from {@link SuggestionStripView} through the {@link SuggestionStripView#Listener}
    // interface
    override fun pickSuggestionManually(suggestionInfo: SuggestedWordInfo?)

    // This will show either an empty suggestion strip (if prediction is enabled) or
    // punctuation suggestions (if it's disabled).
    override fun setNeutralSuggestionStrip()

    // Callback of the {@link KeyboardActionListener}. This is called when a key is depressed;
    // release matching call is {@link #onReleaseKey(int,boolean)} below.
    override fun onPressKey(
        primaryCode: Int, repeatCount: Int,
        isSinglePointer: Boolean
    )

    // Callback of the {@link KeyboardActionListener}. This is called when a key is released;
    // press matching call is {@link #onPressKey(int,int,boolean)} above.
    override fun onReleaseKey(primaryCode: Int, withSliding: Boolean)

    fun launchSettings(extraEntryValue: String?)

    fun dumpDictionaryForDebug(dictName: String?)

    fun debugDumpStateAndCrashWithException(context: String?)

    fun shouldShowLanguageSwitchKey(): Boolean

    fun enableHardwareAcceleration(): Boolean
}
