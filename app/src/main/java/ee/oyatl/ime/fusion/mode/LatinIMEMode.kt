package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.StringRes
import com.android.inputmethod.event.Event
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.keyboard.internal.KeyboardBuilder
import com.android.inputmethod.keyboard.internal.KeyboardParams
import com.android.inputmethod.latin.DictionaryFacilitator
import com.android.inputmethod.latin.DictionaryFacilitatorProvider
import com.android.inputmethod.latin.LastComposedWord
import com.android.inputmethod.latin.NgramContext
import com.android.inputmethod.latin.Suggest
import com.android.inputmethod.latin.Suggest.OnGetSuggestedWordsCallback
import com.android.inputmethod.latin.SuggestedWords
import com.android.inputmethod.latin.WordComposer
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.settings.SettingsValuesForSuggestion
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.candidate.TripleCandidateView
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.keyboard.KeyCodeMapper
import ee.oyatl.ime.keyboard.KeyboardConfiguration
import ee.oyatl.ime.keyboard.layout.KeyboardConfigurations
import ee.oyatl.ime.keyboard.layout.KeyboardMappings
import ee.oyatl.ime.keyboard.layout.KeyboardTemplates
import java.util.Locale

abstract class LatinIMEMode(
    listener: IMEMode.Listener
): CommonIMEMode(listener), DictionaryFacilitator.DictionaryInitializationListener, OnGetSuggestedWordsCallback {
    abstract val locale: Locale

    private var dictionaryFacilitator: DictionaryFacilitator? = null
    private var suggest: Suggest? = null

    private val wordComposer: WordComposer = WordComposer()
    private var lastComposedWord: LastComposedWord = LastComposedWord.NOT_A_COMPOSED_WORD
    private var ghostSpace = false

    private val keyboardParams: KeyboardParams = KeyboardParams().apply {
        mOccupiedWidth = 1
        mOccupiedHeight = 1
    }
    private lateinit var dummyKeyboard: Keyboard

    override suspend fun onLoad(context: Context) {
        dummyKeyboard = KeyboardBuilder(context, keyboardParams).build()
        val dictionaryFacilitator = DictionaryFacilitatorProvider.getDictionaryFacilitator(false)
        dictionaryFacilitator.resetDictionaries(
            context,
            locale,
            false,
            false,
            true,
            null,
            "",
            this
        )
        suggest = Suggest(dictionaryFacilitator)
        this.dictionaryFacilitator = dictionaryFacilitator
    }

    override fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo) {
        super.onStart(inputConnection, editorInfo)
        wordComposer.restartCombining(null)
    }

    override fun onReset() {
        super.onReset()
        wordComposer.reset()
        lastComposedWord = LastComposedWord.NOT_A_COMPOSED_WORD
        ghostSpace = false
    }

    override fun createCandidateView(context: Context): View {
        candidateView = TripleCandidateView(context, null).apply {
            listener = this@LatinIMEMode
        }
        return candidateView as View
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        lastComposedWord = wordComposer.commitWord(
            LastComposedWord.COMMIT_TYPE_MANUAL_PICK,
            candidate.text,
            LastComposedWord.NOT_A_SEPARATOR,
            NgramContext.EMPTY_PREV_WORDS_INFO
        )
        val ic = currentInputConnection
        if(ic != null) {
            if(ghostSpace) ic.commitText(" ", 1)
            ic.commitText(lastComposedWord.mCommittedWord, 1)
            ghostSpace = true
        }
        renderInputView()
        updateSuggestions()
    }

    override fun onGetSuggestedWords(suggestedWords: SuggestedWords?) {
        suggestedWords ?: return
        val wordList = (0 until suggestedWords.size()).map { suggestedWords.getWord(it) }
        val candidates = wordList.mapIndexed { i, s -> LatinCandidate(i, s) }
        submitCandidates(if(candidates.size > 1) candidates.drop(1) else candidates)
    }

    private fun updateSuggestions() {
        val ngramContext =
            if(lastComposedWord == LastComposedWord.NOT_A_COMPOSED_WORD) NgramContext.BEGINNING_OF_SENTENCE
            else NgramContext(NgramContext.WordInfo(lastComposedWord.mCommittedWord))
        val inputStyle =
            if(wordComposer.isComposingWord) SuggestedWords.INPUT_STYLE_TYPING
            else SuggestedWords.INPUT_STYLE_PREDICTION
        suggest?.getSuggestedWords(
            wordComposer,
            ngramContext,
            dummyKeyboard,
            SettingsValuesForSuggestion(true),
            true,
            inputStyle,
            SuggestedWords.NOT_A_SEQUENCE_NUMBER,
            this
        )
    }

    private fun renderInputView() {
        val ic = currentInputConnection ?: return
        if(passwordField) {
            ic.commitText(wordComposer.typedWord, 1)
            onReset()
        } else {
            ic.setComposingText(wordComposer.typedWord, 1)
        }
    }

    override fun onChar(codePoint: Int) {
        val event = Event.createEventForCodePointFromUnknownSource(codePoint)
        val processedEvent = wordComposer.processEvent(event)
        wordComposer.applyProcessedEvent(processedEvent)
        if(ghostSpace) currentInputConnection?.commitText(" ", 1)
        ghostSpace = false
        renderInputView()
        updateSuggestions()
    }

    override fun onSpecial(keyCode: Int) {
        when(keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                val event = Event.createSoftwareKeypressEvent(Event.NOT_A_CODE_POINT, Constants.CODE_DELETE, 0, 0, false)
                val processedEvent = wordComposer.processEvent(event)
                if(wordComposer.isComposingWord) {
                    wordComposer.applyProcessedEvent(processedEvent)
                } else {
                    onReset()
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                }
                updateSuggestions()
            }
            KeyEvent.KEYCODE_SPACE -> {
                onReset()
                util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_SPACE)
            }
            KeyEvent.KEYCODE_ENTER -> {
                onReset()
                if (util?.sendDefaultEditorAction(true) != true)
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            }
            else -> {}
        }
        ghostSpace = false
        renderInputView()
    }

    override fun onUpdateMainDictionaryAvailability(isMainDictionaryAvailable: Boolean) {
    }

    data class LatinCandidate(
        val index: Int,
        override val text: CharSequence
    ): CandidateView.Candidate

    class Qwerty(override val locale: Locale, listener: IMEMode.Listener): LatinIMEMode(listener)

    class Dvorak(override val locale: Locale, listener: IMEMode.Listener): LatinIMEMode(listener) {
        override val keyCodeMapper: KeyCodeMapper = KeyCodeMapper.from(
            KeyboardMappings.ANSI_QWERTY,
            KeyboardMappings.ANSI_QWERTY_DVORAK
        )
        override val keyboardConfiguration: KeyboardConfiguration = KeyboardConfigurations.MOBILE_DVORAK
        override val keyboardTemplate: List<String> = KeyboardTemplates.MOBILE_DVORAK
    }

    class Colemak(override val locale: Locale, listener: IMEMode.Listener): LatinIMEMode(listener) {
        override val keyCodeMapper: KeyCodeMapper = KeyCodeMapper.from(
            KeyboardMappings.ANSI_QWERTY,
            KeyboardMappings.ANSI_QWERTY_COLEMAK
        )
        override val keyboardConfiguration: KeyboardConfiguration = KeyboardConfigurations.MOBILE_EXT1
        override val keyboardTemplate: List<String> = KeyboardTemplates.MOBILE_SEMICOLON
    }

    data class Params(
        val locale: Locale = Locale.ENGLISH,
        val layout: Layout = Layout.Qwerty
    ): IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): LatinIMEMode {
            return when(layout) {
                Layout.Qwerty -> Qwerty(locale, listener)
                Layout.Dvorak -> Dvorak(locale, listener)
                Layout.Colemak -> Colemak(locale, listener)
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
                return Params(
                    locale = locale,
                    layout = layout
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