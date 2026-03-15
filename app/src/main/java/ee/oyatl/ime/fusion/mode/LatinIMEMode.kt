package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.content.res.Resources
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodSubtype
import androidx.annotation.StringRes
import com.android.inputmethod.event.Event
import com.android.inputmethod.event.InputTransaction
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.keyboard.KeyboardSwitcher
import com.android.inputmethod.keyboard.internal.KeyboardBuilder
import com.android.inputmethod.keyboard.internal.KeyboardParams
import com.android.inputmethod.latin.AudioAndHapticFeedbackManager
import com.android.inputmethod.latin.DictionaryFacilitator
import com.android.inputmethod.latin.DictionaryFacilitatorProvider
import com.android.inputmethod.latin.ILatinIME
import com.android.inputmethod.latin.InputAttributes
import com.android.inputmethod.latin.LatinIME
import com.android.inputmethod.latin.RichInputMethodManager
import com.android.inputmethod.latin.Suggest.OnGetSuggestedWordsCallback
import com.android.inputmethod.latin.SuggestedWords
import com.android.inputmethod.latin.WordComposer
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.CoordinateUtils
import com.android.inputmethod.latin.common.InputPointers
import com.android.inputmethod.latin.inputlogic.InputLogic
import com.android.inputmethod.latin.settings.Settings
import com.android.inputmethod.latin.settings.SettingsValues
import com.android.inputmethod.latin.utils.ScriptUtils
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.candidate.TripleCandidateView
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.keyboard.KeyboardConfiguration
import ee.oyatl.ime.keyboard.KeyboardTemplate
import ee.oyatl.ime.keyboard.LayoutTable
import ee.oyatl.ime.keyboard.layout.LayoutLatin
import ee.oyatl.ime.keyboard.layout.MobileKeyboard
import ee.oyatl.ime.keyboard.layout.MobileKeyboardRows
import ee.oyatl.ime.keyboard.layout.TabletKeyboard
import ee.oyatl.ime.keyboard.layout.TabletKeyboardRows
import java.util.Locale

abstract class LatinIMEMode(
    private val listener: IMEMode.Listener
): CommonIMEMode(listener), ILatinIME {
    abstract val locale: Locale
    override var context: Context? = null

    override val handler: LatinIME.UIHandler = LatinIME.UIHandler(this)
    private var dictionaryFacilitator: DictionaryFacilitator? = null
    override var settings: Settings? = null
    private var richImm: RichInputMethodManager? = null
    override var keyboardSwitcher: KeyboardSwitcher? = null
    override var inputLogic: InputLogic? = null

    private val keyboardParams: KeyboardParams = KeyboardParams().apply {
        mOccupiedWidth = 1
        mOccupiedHeight = 1
    }
    private lateinit var dummyKeyboard: Keyboard

    override val currentInputConnection: InputConnection? get() = super.currentInputConnection
    override val currentInputEditorInfo: EditorInfo? get() = super.currentInputEditorInfo

    override val isInputViewShown: Boolean = true
    override val resources: Resources? = context?.resources
    override val currentAutoCapsState: Int = 0
    override val currentRecapitalizeState: Int = 0

    override suspend fun onLoad(context: Context) {
        this.context = context
        dummyKeyboard = KeyboardBuilder(context, keyboardParams).build()
        dictionaryFacilitator = DictionaryFacilitatorProvider.getDictionaryFacilitator(false)
        settings = Settings.getInstance()
        richImm = RichInputMethodManager.getInstance()
        KeyboardSwitcher.init(this)
        keyboardSwitcher = KeyboardSwitcher.getInstance()
        inputLogic = InputLogic(this, this, dictionaryFacilitator)
    }

    override fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo) {
        super.onStart(inputConnection, editorInfo)
        onStartInputViewInternal(editorInfo, false)
        onStartInputInternal(editorInfo, false)
    }

    override fun onReset() {
        super.onReset()
    }

    override fun createCandidateView(context: Context): View {
        candidateView = TripleCandidateView(context, null).apply {
            listener = this@LatinIMEMode
        }
        return candidateView as View
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        if(candidate is LatinCandidate) {
            pickSuggestionManually(candidate.suggestedWordInfo)
        }
    }

    override fun onChar(codePoint: Int) {
        onCodeInput(codePoint, 0, 0, false)
    }

    override fun onSpecial(keyCode: Int) {
        when(keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                onCodeInput(Constants.CODE_DELETE, 0, 0, false)
            }
            KeyEvent.KEYCODE_SPACE -> {
                onCodeInput(Constants.CODE_SPACE, 0, 0, false)
            }
            KeyEvent.KEYCODE_ENTER -> {
                onCodeInput(Constants.CODE_ENTER, 0, 0, false)
            }
            else -> super.onSpecial(keyCode)
        }
    }

    override fun updateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.updateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if(isInputViewShown &&
            inputLogic?.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, settings?.current) == true) {

            keyboardSwitcher?.requestUpdatingShiftState(currentAutoCapsState, currentRecapitalizeState)
        }
    }

    fun loadSettings() {
        val editorInfo = currentInputEditorInfo ?: return
        val inputAttributes = InputAttributes(
            editorInfo, false, context?.packageName
        )
        settings?.loadSettings(context, locale, inputAttributes)
        val currentSettingsValues = settings?.current ?: return
        AudioAndHapticFeedbackManager.getInstance().onSettingsChanged(currentSettingsValues)


        // This method is called on startup and language switch, before the new layout has
        // been displayed. Opening dictionaries never affects responsivity as dictionaries are
        // asynchronously loaded.
        if (!handler.hasPendingReopenDictionaries()) {
            resetDictionaryFacilitator(locale)
        }
//        refreshPersonalizationDictionarySession(currentSettingsValues)
        resetDictionaryFacilitatorIfNecessary()
    }

    override fun onUpdateMainDictionaryAvailability(isMainDictionaryAvailable: Boolean) {
        val handler = handler ?: return
        if (handler.hasPendingWaitForDictionaryLoad()) {
            handler.cancelWaitForDictionaryLoad()
            handler.postResumeSuggestions(false /* shouldDelay */)
        }
    }

    override fun resetDictionaryFacilitatorIfNecessary() {
        val dictionaryFacilitator = dictionaryFacilitator ?: return
        val subtypeSwitcherLocale = locale
        if (dictionaryFacilitator.isForLocale(subtypeSwitcherLocale)
            && dictionaryFacilitator.isForAccount(settings?.current?.mAccount)) {

            return
        }
        resetDictionaryFacilitator(subtypeSwitcherLocale)
    }

    private fun resetDictionaryFacilitator(locale: Locale?) {
        val dictionaryFacilitator = this.dictionaryFacilitator ?: return
        val settingsValues = settings?.current ?: return
        val inputLogic = inputLogic ?: return
        dictionaryFacilitator.resetDictionaries(
            context,  /* context */locale,
            settingsValues.mUseContactsDict, settingsValues.mUsePersonalizedDicts,
            false,  /* forceReloadMainDictionary */
            settingsValues.mAccount, "",  /* dictNamePrefix */
            this /* DictionaryInitializationListener */
        )
        if (settingsValues.mAutoCorrectionEnabledPerUserSettings) {
            inputLogic.mSuggest.setAutoCorrectionThreshold(
                settingsValues.mAutoCorrectionThreshold
            )
        }
        inputLogic.mSuggest.setPlausibilityThreshold(settingsValues.mPlausibilityThreshold)
    }

    override fun resetSuggestMainDict() {
        val dictionaryFacilitator = dictionaryFacilitator ?: return
        val settingsValues = settings?.current ?: return
        dictionaryFacilitator.resetDictionaries(
            context,
            dictionaryFacilitator.locale,
            settingsValues.mUseContactsDict,
            settingsValues.mUsePersonalizedDicts,
            true,
            settingsValues.mAccount,
            "",
            this
        )
    }

    override fun setInputView(view: View?) = Unit

    override fun onStartInputInternal(
        editorInfo: EditorInfo?,
        restarting: Boolean
    ) {
    }

    override fun onStartInputViewInternal(
        editorInfo: EditorInfo?,
        restarting: Boolean
    ) {
        loadSettings()
        dictionaryFacilitator?.onStartInput()
        inputLogic?.startInput(richImm?.combiningRulesExtraValueOfCurrentSubtype, settings?.current)
    }

    override fun onFinishInputInternal() {
    }

    override fun onFinishInputViewInternal(finishingInput: Boolean) {
    }

    override fun deallocateMemory() {
        keyboardSwitcher?.deallocateMemory()
    }

    override fun hideWindow() {
        requestHideSelf(0)
    }

    override fun startShowingInputView(needsToLoadKeyboard: Boolean) = Unit

    override fun stopShowingInputView() = Unit

    override fun onEvaluateInputViewShown(): Boolean = true

    override fun updateFullscreenMode() = Unit

    override fun getCoordinatesForCurrentKeyboard(codePoints: IntArray?): IntArray {
        return CoordinateUtils.newCoordinateArray(
            codePoints?.size ?: 0,
            Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE
        )
    }

    override fun showImportantNoticeContents() = Unit

    override fun onRequestPermissionsResult(allGranted: Boolean) {
    }

    override fun displaySettingsDialog() {
    }

    override fun onCustomRequest(requestCode: Int): Boolean {
        return false
    }

    override fun switchLanguage(subtype: InputMethodSubtype?) = Unit

    override fun switchToNextSubtype() {
        listener.onLanguageSwitch()
    }

    override fun onCodeInput(
        codePoint: Int,
        x: Int,
        y: Int,
        isKeyRepeat: Boolean
    ) {
//        val keyboardSwitcher = keyboardSwitcher ?: return
        // TODO: this processing does not belong inside LatinIME, the caller should be doing this.
//        val mainKeyboardView: MainKeyboardView = keyboardSwitcher.getMainKeyboardView()


        // x and y include some padding, but everything down the line (especially native
        // code) needs the coordinates in the keyboard frame.
        // TODO: We should reconsider which coordinate system should be used to represent
        // keyboard event. Also we should pull this up -- LatinIME has no business doing
        // this transformation, it should be done already before calling onEvent.
//        val keyX = mainKeyboardView.getKeyX(x)
//        val keyY = mainKeyboardView.getKeyY(y)
        val event = LatinIME.createSoftwareKeypressEvent(
            codePoint, x, y, isKeyRepeat
        )
        onEvent(event)
    }

    override fun onEvent(event: Event) {
        val richImm = richImm ?: return
        val inputLogic = inputLogic ?: return
        val keyboardSwitcher = keyboardSwitcher ?: return
        if (Constants.CODE_SHORTCUT == event.mKeyCode) {
            val context = context
            if(context is InputMethodService) richImm.switchToShortcutIme(context)
        }
        val completeInputTransaction: InputTransaction =
            inputLogic.onCodeInput(
                settings?.current, event,
                keyboardSwitcher.getKeyboardShiftMode(),
                keyboardSwitcher.getCurrentKeyboardScriptId(), handler
            )
        updateStateAfterInputTransaction(completeInputTransaction)
        keyboardSwitcher.onEvent(event, currentAutoCapsState, currentRecapitalizeState)
    }

    override fun onTextInput(rawText: String?) {
        // TODO: have the keyboard pass the correct key code when we need it.
        val event = Event.createSoftwareTextEvent(rawText, Constants.CODE_OUTPUT_TEXT)
        val completeInputTransaction = inputLogic?.onTextInput(
            settings?.current, event,
            keyboardSwitcher?.getKeyboardShiftMode() ?: WordComposer.CAPS_MODE_OFF,
            handler
        )
        if(completeInputTransaction != null) updateStateAfterInputTransaction(completeInputTransaction)
        keyboardSwitcher?.onEvent(event, currentAutoCapsState, currentRecapitalizeState)
    }

    override fun onStartBatchInput() {
        inputLogic?.onStartBatchInput(settings?.current, keyboardSwitcher, handler)
//        mGestureConsumer.onGestureStarted(
//            mRichImm.getCurrentSubtypeLocale(),
//            mKeyboardSwitcher.getKeyboard()
//        )
    }

    override fun onUpdateBatchInput(batchPointers: InputPointers?) {
        inputLogic?.onUpdateBatchInput(batchPointers)
    }

    override fun onEndBatchInput(batchPointers: InputPointers?) {
        inputLogic?.onEndBatchInput(batchPointers)
    }

    override fun onCancelBatchInput() {
        inputLogic?.onCancelBatchInput(handler)
//        mGestureConsumer.onGestureCompleted(batchPointers)
    }

    override fun onTailBatchInputResultShown(suggestedWords: SuggestedWords?) {
    }

    override fun showGesturePreviewAndSuggestionStrip(
        suggestedWords: SuggestedWords,
        dismissGestureFloatingPreviewText: Boolean
    ) {
    }

    override fun onFinishSlidingInput() {
    }

    override fun onCancelInput() {
    }

    override fun hasSuggestionStripView(): Boolean = true

    override fun getSuggestedWords(
        inputStyle: Int,
        sequenceNumber: Int,
        callback: OnGetSuggestedWordsCallback?
    ) {
        val keyboard = dummyKeyboard
        inputLogic?.getSuggestedWords(
            settings?.current, keyboard,
            keyboardSwitcher?.getKeyboardShiftMode() ?: WordComposer.CAPS_MODE_OFF,
            inputStyle, sequenceNumber, callback
        )
    }

    override fun showSuggestionStrip(suggestedWords: SuggestedWords?) {
        if (suggestedWords == null || suggestedWords.isEmpty) {
            setNeutralSuggestionStrip()
        } else {
            setSuggestedWords(suggestedWords)
        }
    }

    fun setSuggestedWords(suggestedWords: SuggestedWords) {
        val wordList = (0 until suggestedWords.size()).map { suggestedWords.getInfo(it) }
        val candidates = wordList.mapIndexed { i, s -> LatinCandidate(i, s) }
        submitCandidates(candidates)
    }

    override fun setNeutralSuggestionStrip() {
        submitCandidates(listOf())
    }

    override fun pickSuggestionManually(suggestionInfo: SuggestedWords.SuggestedWordInfo?) {
        val completeInputTransaction: InputTransaction = inputLogic?.onPickSuggestionManually(
            settings?.current, suggestionInfo,
            keyboardSwitcher?.getKeyboardShiftMode() ?: WordComposer.CAPS_MODE_OFF,
            keyboardSwitcher?.getCurrentKeyboardScriptId() ?: ScriptUtils.SCRIPT_UNKNOWN,
            handler
        ) ?: return
        updateStateAfterInputTransaction(completeInputTransaction)
    }

    /**
     * After an input transaction has been executed, some state must be updated. This includes
     * the shift state of the keyboard and suggestions. This method looks at the finished
     * inputTransaction to find out what is necessary and updates the state accordingly.
     * @param inputTransaction The transaction that has been executed.
     */
    private fun updateStateAfterInputTransaction(inputTransaction: InputTransaction) {
        when (inputTransaction.requiredShiftUpdate) {
            InputTransaction.SHIFT_UPDATE_LATER -> handler.postUpdateShiftState()
            InputTransaction.SHIFT_UPDATE_NOW -> {
                keyboardSwitcher?.requestUpdatingShiftState(
                    currentAutoCapsState,
                    currentRecapitalizeState
                )
            }

            else -> {}
        }
        if (inputTransaction.requiresUpdateSuggestions()) {
            val inputStyle: Int
            if (inputTransaction.mEvent.isSuggestionStripPress) {
                // Suggestion strip press: no input.
                inputStyle = SuggestedWords.INPUT_STYLE_NONE
            } else if (inputTransaction.mEvent.isGesture) {
                inputStyle = SuggestedWords.INPUT_STYLE_TAIL_BATCH
            } else {
                inputStyle = SuggestedWords.INPUT_STYLE_TYPING
            }
            handler.postUpdateSuggestionStrip(inputStyle)
        }
        if (inputTransaction.didAffectContents()) {
//            subtypeState.setCurrentSubtypeHasBeenUsed()
        }
    }

    override fun onPressKey(
        primaryCode: Int,
        repeatCount: Int,
        isSinglePointer: Boolean
    ) {
        keyboardSwitcher?.onPressKey(
            primaryCode, isSinglePointer, currentAutoCapsState,
            currentRecapitalizeState
        )
    }

    override fun onReleaseKey(primaryCode: Int, withSliding: Boolean) {
        keyboardSwitcher?.onReleaseKey(
            primaryCode, withSliding, currentAutoCapsState,
            currentRecapitalizeState
        )
    }

    override fun launchSettings(extraEntryValue: String?) {
    }

    override fun dumpDictionaryForDebug(dictName: String?) {
        val dictionaryFacilitator = this.dictionaryFacilitator ?: return
        if (!dictionaryFacilitator.isActive()) {
            resetDictionaryFacilitatorIfNecessary()
        }
        dictionaryFacilitator.dumpDictionaryForDebug(dictName)
    }

    override fun debugDumpStateAndCrashWithException(context: String?) {
        val settingsValues: SettingsValues = settings?.current ?: return
        val s = StringBuilder(settingsValues.toString())
        s.append("\nAttributes : ").append(settingsValues.mInputAttributes)
            .append("\nContext : ").append(context)
        throw RuntimeException(s.toString())
    }

    override fun shouldShowLanguageSwitchKey(): Boolean = true

    override fun enableHardwareAcceleration(): Boolean {
        val context = this.context
        return context is InputMethodService && context.enableHardwareAcceleration()
    }

    data class LatinCandidate(
        val index: Int,
        val suggestedWordInfo: SuggestedWords.SuggestedWordInfo
    ): CandidateView.Candidate {
        override val text: CharSequence = suggestedWordInfo.word
    }

    class Qwerty(
        override val locale: Locale,
        numberRow: Boolean,
        listener: IMEMode.Listener
    ): LatinIMEMode(listener) {
        override val textKeyboardTemplate: KeyboardTemplate = KeyboardTemplate.ByScreenMode(
            mobile = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    MobileKeyboard.alphabetic(numberRow = numberRow),
                    MobileKeyboard.bottom()
                ),
                contentRows = (if(numberRow) MobileKeyboardRows.NUMBERS else listOf()) + MobileKeyboardRows.DEFAULT
            ),
            tablet = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    TabletKeyboard.alphabetic(),
                    TabletKeyboard.bottom()
                ),
                contentRows = TabletKeyboardRows.DEFAULT
            )
        )
    }

    class Dvorak(
        override val locale: Locale,
        numberRow: Boolean,
        listener: IMEMode.Listener
    ): LatinIMEMode(listener) {
        override val textLayoutTable: LayoutTable = super.textLayoutTable.mapKeyCodes(LayoutLatin.KEYCODE_MAP_DVORAK)
        override val textKeyboardTemplate: KeyboardTemplate = KeyboardTemplate.ByScreenMode(
            mobile = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    MobileKeyboard.alphabetic(semicolon = true, numberRow = numberRow),
                    MobileKeyboard.bottom(KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_SLASH)
                ),
                contentRows = (if(numberRow) MobileKeyboardRows.NUMBERS else listOf()) + MobileKeyboardRows.DVORAK
            ),
            tablet = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    TabletKeyboard.alphabetic(semicolon = true),
                    TabletKeyboard.bottom()
                ),
                contentRows = TabletKeyboardRows.DVORAK
            )
        )
    }

    class Colemak(
        override val locale: Locale,
        numberRow: Boolean,
        listener: IMEMode.Listener
    ): LatinIMEMode(listener) {
        override val textLayoutTable: LayoutTable = super.textLayoutTable.mapKeyCodes(LayoutLatin.KEYCODE_MAP_COLEMAK)
        override val textKeyboardTemplate: KeyboardTemplate = KeyboardTemplate.ByScreenMode(
            mobile = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    MobileKeyboard.alphabetic(semicolon = true, numberRow = numberRow),
                    MobileKeyboard.bottom()
                ),
                contentRows = (if(numberRow) MobileKeyboardRows.NUMBERS else listOf()) + MobileKeyboardRows.SEMICOLON
            ),
            tablet = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    TabletKeyboard.alphabetic(semicolon = true),
                    TabletKeyboard.bottom()
                ),
                contentRows = TabletKeyboardRows.SEMICOLON
            )
        )
    }

    data class Params(
        val locale: Locale = Locale.ENGLISH,
        val layout: Layout = Layout.Qwerty,
        val numberRow: Boolean = true
    ): IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): LatinIMEMode {
            return when(layout) {
                Layout.Qwerty -> Qwerty(locale, numberRow, listener)
                Layout.Dvorak -> Dvorak(locale, numberRow, listener)
                Layout.Colemak -> Colemak(locale, numberRow, listener)
            }
        }

        override fun getLabel(context: Context): String {
            val localeName = locale.displayName
            val layoutName = context.resources.getString(layout.nameKey)
            return "$localeName $layoutName"
        }

        override fun getShortLabel(context: Context): String {
            return locale.language.replaceFirstChar { it.uppercase() }
        }

        companion object {
            fun parse(map: Map<String, String>): Params {
                val localeName = (map["locale"] ?: Locale.ENGLISH.language).split('_')
                val locale =
                    if(localeName.size == 2) Locale(localeName[0], localeName[1])
                    else Locale(localeName[0])
                val layout = Layout.valueOf(map["layout"] ?: Layout.Qwerty.name)
                val numberRow = map["number_row"]?.toBoolean() ?: false
                return Params(
                    locale = locale,
                    layout = layout,
                    numberRow = numberRow
                )
            }
        }
    }

    enum class Layout(
        @StringRes val nameKey: Int
    ) {
        Qwerty(R.string.latin_layout_qwerty),
        Dvorak(R.string.latin_layout_dvorak),
        Colemak(R.string.latin_layout_colemak)
    }

    companion object {
        const val TYPE: String = "latin"
    }
}