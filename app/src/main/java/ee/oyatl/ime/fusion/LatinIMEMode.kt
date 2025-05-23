package ee.oyatl.ime.fusion

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
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
import ee.oyatl.ime.keyboard.Keyboard.SpecialKey
import java.util.Locale

class LatinIMEMode(
    context: Context,
    listener: IMEMode.Listener
): CommonIMEMode(listener), DictionaryFacilitator.DictionaryInitializationListener, OnGetSuggestedWordsCallback {

    private var dictionaryFacilitator: DictionaryFacilitator? = null
    private var suggest: Suggest? = null

    private val wordComposer: WordComposer = WordComposer()
    private var lastComposedWord: LastComposedWord = LastComposedWord.NOT_A_COMPOSED_WORD

    private val keyboardParams: KeyboardParams = KeyboardParams().apply {
        mOccupiedWidth = 1
        mOccupiedHeight = 1
    }
    private val dummyKeyboard: Keyboard = KeyboardBuilder(context, keyboardParams).build()

    override suspend fun onLoad(context: Context) {
        val dictionaryFacilitator = DictionaryFacilitatorProvider.getDictionaryFacilitator(false)
        dictionaryFacilitator.resetDictionaries(
            context,
            Locale.ENGLISH,
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
            ic.commitText(lastComposedWord.mCommittedWord, 1)
            ic.commitText(" ", 1)
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
        ic.setComposingText(wordComposer.typedWord, 1)
    }

    override fun onChar(code: Int) {
        val event = Event.createEventForCodePointFromUnknownSource(code)
        val processedEvent = wordComposer.processEvent(event)
        wordComposer.applyProcessedEvent(processedEvent)
        renderInputView()
        updateSuggestions()
    }

    override fun onSpecial(type: SpecialKey) {
        when(type) {
            SpecialKey.Delete -> {
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
            SpecialKey.Space -> {
                onReset()
                util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_SPACE)
            }
            SpecialKey.Return -> {
                onReset()
                if (util?.sendDefaultEditorAction(true) != true)
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            }
            else -> {}
        }
        renderInputView()
    }

    override fun onUpdateMainDictionaryAvailability(isMainDictionaryAvailable: Boolean) {
    }

    data class LatinCandidate(
        val index: Int,
        override val text: CharSequence
    ): CandidateView.Candidate
}