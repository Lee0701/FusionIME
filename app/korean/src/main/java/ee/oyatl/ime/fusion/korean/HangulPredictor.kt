package ee.oyatl.ime.fusion.korean

import android.content.Context
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.keyboard.internal.KeyboardBuilder
import com.android.inputmethod.keyboard.internal.KeyboardParams
import com.android.inputmethod.latin.DictionaryFacilitator
import com.android.inputmethod.latin.DictionaryFacilitatorProvider
import com.android.inputmethod.latin.NgramContext
import com.android.inputmethod.latin.Suggest
import com.android.inputmethod.latin.SuggestedWords
import com.android.inputmethod.latin.WordComposer
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.settings.SettingsValuesForSuggestion
import ee.oyatl.ime.candidate.CandidateView
import java.util.Locale

class HangulPredictor: HanjaConverter, DictionaryFacilitator.DictionaryInitializationListener {
    private val dictionaryFacilitator: DictionaryFacilitator = DictionaryFacilitatorProvider.getDictionaryFacilitator(false)

    private val params = KeyboardParams().apply {
        mOccupiedWidth = 1
        mOccupiedHeight = 1
    }
    private lateinit var dummyKeyboard: Keyboard

    override fun load(context: Context) {
        val locale = Locale.KOREAN
        dictionaryFacilitator.resetDictionaries(
            context, locale,
            false, false,
            false,
            null,
            "",
            this
        )
        dummyKeyboard = KeyboardBuilder(context, params).build()
    }

    override fun convert(text: String): List<CandidateView.Candidate> {
        val composer = WordComposer()
        val context = NgramContext.BEGINNING_OF_SENTENCE
        val keyboard = dummyKeyboard
        val settings = SettingsValuesForSuggestion(true)
        val inputStyle = SuggestedWords.INPUT_STYLE_TYPING

        val codePoints = text.map { it.code }.toIntArray()
        val coordinates = text.flatMap { listOf(Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE) }.toIntArray()
        composer.setComposingWord(codePoints, coordinates)
        val result = dictionaryFacilitator.getSuggestionResults(
            composer.composedDataSnapshot,
            context,
            keyboard,
            settings,
            Suggest.SESSION_ID_TYPING,
            inputStyle
        )
        return result
            .map {
                val text = it.mWord.replace(" ", "")
                val score = it.mScore
                Candidate(text, score)
            }
            .sortedByDescending { it.score }
            .distinctBy { it.text }
    }

    override fun onUpdateMainDictionaryAvailability(isMainDictionaryAvailable: Boolean) = Unit

    data class Candidate(
        override val text: String,
        val score: Int
    ): CandidateView.Candidate
}