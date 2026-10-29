package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.inputmethodservice.InputMethodService
import android.view.KeyCharacterMap
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
import ee.oyatl.ime.fusion.layout.preset.LatinLayoutPresets
import ee.oyatl.ime.keyboard.KeyboardLayoutPreset
import ee.oyatl.ime.keyboard.KeyboardState
import java.util.Locale

class LatinIMEMode(
    private val listener: IMEMode.Listener,
    val locale: Locale,
    override var textLayoutPreset: KeyboardLayoutPreset,
    val autoCaps: Boolean,
    val autoPeriod: Boolean
): CommonIMEMode(listener), ILatinIME {
    override var context: Context? = null

    override val handler: LatinIME.UIHandler = LatinIME.UIHandler(this)
    private var dictionaryFacilitator: DictionaryFacilitator? = null
    override var settings: Settings? = null
    private var settingsValues: SettingsValues? = null
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
    override val currentAutoCapsState: Int get() {
        val inputLogic = inputLogic ?: return 0
        val settingsValues = settingsValues ?: return 0
        return inputLogic.getCurrentAutoCapsState(settingsValues)
    }
    override val currentRecapitalizeState: Int get() {
        val inputLogic = inputLogic ?: return 0
        return inputLogic.currentRecapitalizeState
    }

    // KeyboardSwitcher.loadKeyboard() is never called, so its script id stays SCRIPT_UNKNOWN,
    // which treats every code point as a word character. Derive the script from the locale instead.
    override val currentKeyboardScriptId: Int by lazy {
        ScriptUtils.getScriptFromSpellCheckerLocale(locale)
    }

    override suspend fun onLoad(context: Context) {
        super.onLoad(context)
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
        updateAutoCapsState()
    }

    override fun createCandidateView(context: Context): View {
        candidateView = TripleCandidateView(context, null).apply {
            keyboardTheme = this@LatinIMEMode.keyboardTheme
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
        if(codePoint == 0) return
        if(codePoint and KeyCharacterMap.COMBINING_ACCENT != 0) {
            val event = Event.createDeadEvent(codePoint and KeyCharacterMap.COMBINING_ACCENT_MASK, 0, null)
            onEvent(event)
            return
        }
        onCodeInput(codePoint, 0, 0, false)
        updateAutoCapsState()
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
        updateAutoCapsState()
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
            inputLogic?.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, settingsValues) == true) {

            keyboardSwitcher?.requestUpdatingShiftState(currentAutoCapsState, currentRecapitalizeState)
        }
    }

    private fun updateAutoCapsState() {
        if(symbolState != KeyboardState.Symbol.Text) return
        if(shiftState == KeyboardState.Shift.Locked) return
        if(currentAutoCapsState != Constants.TextUtils.CAP_MODE_OFF) {
            shiftState = KeyboardState.Shift.AutoShifted
        } else {
            shiftState = KeyboardState.Shift.Released
        }
        updateInputView()
    }

    fun loadSettings() {
        val context = context ?: return
        val pref = SharedPreferenceImpl(mapOf(
            Settings.PREF_AUTO_CAP to autoCaps.toString(),
            Settings.PREF_KEY_USE_DOUBLE_SPACE_PERIOD to autoPeriod.toString()
        ))
        val inputAttributes = InputAttributes(currentInputEditorInfo, false, context.packageName)
        settingsValues = SettingsValues(context, pref, context.resources, inputAttributes)
        settings?.loadSettings(context, locale, inputAttributes)
        val currentSettingsValues = settingsValues ?: return
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
        if (handler.hasPendingWaitForDictionaryLoad()) {
            handler.cancelWaitForDictionaryLoad()
            handler.postResumeSuggestions(false /* shouldDelay */)
        }
    }

    override fun resetDictionaryFacilitatorIfNecessary() {
        val dictionaryFacilitator = dictionaryFacilitator ?: return
        val subtypeSwitcherLocale = locale
        if (dictionaryFacilitator.isForLocale(subtypeSwitcherLocale)
            && dictionaryFacilitator.isForAccount(settingsValues?.mAccount)) {

            return
        }
        resetDictionaryFacilitator(subtypeSwitcherLocale)
    }

    private fun resetDictionaryFacilitator(locale: Locale?) {
        val dictionaryFacilitator = this.dictionaryFacilitator ?: return
        val settingsValues = settingsValues ?: return
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
        val settingsValues = settingsValues ?: return
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
        inputLogic?.startInput(richImm?.combiningRulesExtraValueOfCurrentSubtype, settingsValues)
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
                settingsValues, event,
                keyboardSwitcher.getKeyboardShiftMode(),
                currentKeyboardScriptId, handler
            )
        updateStateAfterInputTransaction(completeInputTransaction)
        keyboardSwitcher.onEvent(event, currentAutoCapsState, currentRecapitalizeState)
    }

    override fun onTextInput(rawText: String?) {
        // TODO: have the keyboard pass the correct key code when we need it.
        val event = Event.createSoftwareTextEvent(rawText, Constants.CODE_OUTPUT_TEXT)
        val completeInputTransaction = inputLogic?.onTextInput(
            settingsValues, event,
            keyboardSwitcher?.getKeyboardShiftMode() ?: WordComposer.CAPS_MODE_OFF,
            handler
        )
        if(completeInputTransaction != null) updateStateAfterInputTransaction(completeInputTransaction)
        keyboardSwitcher?.onEvent(event, currentAutoCapsState, currentRecapitalizeState)
    }

    override fun onStartBatchInput() {
        inputLogic?.onStartBatchInput(settingsValues, keyboardSwitcher, handler)
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
            settingsValues, keyboard,
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
            settingsValues, suggestionInfo,
            keyboardSwitcher?.getKeyboardShiftMode() ?: WordComposer.CAPS_MODE_OFF,
            currentKeyboardScriptId,
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
        val settingsValues: SettingsValues = settingsValues ?: return
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

    class SharedPreferenceImpl(
        val map: Map<String, String>
    ): SharedPreferences {
        override fun contains(key: String?): Boolean {
            return map.containsKey(key ?: return false)
        }

        override fun edit(): SharedPreferences.Editor? {
            return null
        }

        override fun getAll(): Map<String, String> {
            return map
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return map[key]?.toBoolean() ?: defValue
        }

        override fun getFloat(key: String?, defValue: Float): Float {
            return map[key]?.toFloatOrNull() ?: defValue
        }

        override fun getInt(key: String?, defValue: Int): Int {
            return map[key]?.toIntOrNull() ?: defValue
        }

        override fun getLong(key: String?, defValue: Long): Long {
            return map[key]?.toLongOrNull() ?: defValue
        }

        override fun getString(key: String?, defValue: String?): String? {
            return map[key] ?: defValue
        }

        override fun getStringSet(
            key: String?,
            defValues: Set<String?>?
        ): Set<String?>? {
            return null
        }

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    }

    data class LatinCandidate(
        val index: Int,
        val suggestedWordInfo: SuggestedWords.SuggestedWordInfo
    ): CandidateView.Candidate {
        override val text: CharSequence = suggestedWordInfo.word
    }

    data class Params(
        val locale: Locale = Locale.ENGLISH,
        val layout: Layout = Layout.Qwerty,
        val numberRow: Boolean = false,
        val cursorKeys: Boolean = false,
        val autoCaps: Boolean = false,
        val autoPeriod: Boolean = false
    ): IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): LatinIMEMode {
            val textLayoutPreset = when(layout) {
                Layout.Qwerty -> when(locale.language) {
                    "es" -> LatinLayoutPresets.spanishQwerty(numberRow, cursorKeys)
                    else -> LatinLayoutPresets.qwerty(false, numberRow, cursorKeys)
                }
                Layout.Azerty -> LatinLayoutPresets.azerty(numberRow, cursorKeys)
                Layout.Qwertz -> LatinLayoutPresets.qwertz(numberRow, cursorKeys)
                Layout.Deutsch -> LatinLayoutPresets.germanQwertz(numberRow, cursorKeys)
                Layout.Dvorak -> LatinLayoutPresets.dvorak(numberRow, cursorKeys)
                Layout.Colemak -> LatinLayoutPresets.colemak(numberRow, cursorKeys)
            }
            return LatinIMEMode(listener, locale, textLayoutPreset, autoCaps, autoPeriod)
        }

        override fun getLabel(context: Context): String {
            val localeName = locale.displayName
            val layoutName = context.resources.getString(layout.nameKey)
            return "$localeName $layoutName"
        }

        override fun getShortLabels(context: Context): List<String> {
            val localeLabel = locale.language.replaceFirstChar { it.uppercase() }
            val layoutHead = layout.name.first().toString()
            val layoutTail = layout.name.last().toString()
            val labels = listOf(localeLabel, "$localeLabel$layoutHead")
            if(layoutHead == "Q") return labels + listOf("$localeLabel$layoutHead$layoutTail")
            return labels
        }

        companion object {
            fun parse(map: Map<String, String>): Params {
                val localeName = (map["locale"] ?: Locale.ENGLISH.language).split('_')
                val builder = Locale.Builder()
                builder.setLanguage(localeName[0])
                if(localeName.size == 2) builder.setRegion(localeName[1])
                val locale = builder.build()
                val defaultLayout = if(locale.language == "fr") Layout.Azerty else Layout.Qwerty
                val layout = Layout.entries.find { it.name == map["layout"] } ?: defaultLayout
                val numberRow = map["number_row"]?.toBoolean() ?: false
                val cursorKeys = map["cursor_keys"]?.toBoolean() ?: false
                val autoCaps = map["auto_caps"]?.toBoolean() ?: false
                val autoPeriod = map["auto_period"]?.toBoolean() ?: false
                return Params(
                    locale = locale,
                    layout = layout,
                    numberRow = numberRow,
                    cursorKeys = cursorKeys,
                    autoCaps = autoCaps,
                    autoPeriod = autoPeriod
                )
            }
        }
    }

    enum class Layout(
        @StringRes val nameKey: Int
    ) {
        Qwerty(R.string.latin_layout_qwerty),
        Azerty(R.string.latin_layout_azerty),
        Qwertz(R.string.latin_layout_qwertz),
        Deutsch(R.string.latin_layout_deutsch),
        Dvorak(R.string.latin_layout_dvorak),
        Colemak(R.string.latin_layout_colemak)
    }

    companion object {
        const val TYPE: String = "latin"
    }
}
