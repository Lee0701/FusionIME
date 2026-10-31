package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.view.KeyEvent
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.layout.preset.StrokeLayoutPresets
import ee.oyatl.ime.fusion.stroke.Stroke
import ee.oyatl.ime.fusion.stroke.StrokeDictionaries
import ee.oyatl.ime.fusion.stroke.StrokeDictionary
import ee.oyatl.ime.keyboard.KeyboardLayoutPreset
import ee.oyatl.ime.keyboard.KeyboardState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Five-stroke input (筆畫輸入法): characters are entered by their strokes in writing
 * order, each classified as horizontal, vertical, left-falling, dot or turning.
 * Created by [CangjieIMEMode.Params] for [CangjieIMEMode.Layout.Stroke].
 */
class StrokeIMEMode(
    listener: IMEMode.Listener,
    private val chineseTraditional: Boolean
): CommonIMEMode(listener) {
    override var textLayoutPreset: KeyboardLayoutPreset = StrokeLayoutPresets.stroke12Key()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var searchJob: Job? = null
    private var dictionary: StrokeDictionary? = null

    // Stroke digits '1' to '5' and Stroke.WILDCARD entered for the current character.
    private var strokes: String = ""

    override suspend fun onLoad(context: Context) {
        super.onLoad(context)
        dictionary = withContext(Dispatchers.IO) { StrokeDictionaries.load(context) }
    }

    override fun onReset() {
        // Drop the strokes; finishComposingText() in super would commit them as text.
        clearStrokes()
        currentInputConnection?.setComposingText("", 1)
        super.onReset()
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        commit(candidate.text)
    }

    override fun onChar(codePoint: Int) {
        val stroke = if(symbolState == KeyboardState.Symbol.Text) Stroke.ofLabel(codePoint) else null
        if(stroke != null) {
            strokes += stroke
            updateComposition()
        } else {
            // Anything else ends the character being entered.
            commitBestCandidate()
            super.onChar(codePoint)
        }
    }

    override fun onSpecial(keyCode: Int) {
        if(strokes.isEmpty()) {
            when(keyCode) {
                KeyEvent.KEYCODE_ENTER -> {
                    if(util?.sendDefaultEditorAction(true) != true)
                        currentInputConnection?.commitText("\n", 1)
                }
                else -> super.onSpecial(keyCode)
            }
            return
        }
        when(keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                strokes = strokes.dropLast(1)
                if(strokes.isEmpty()) onReset()
                else updateComposition()
            }
            KeyEvent.KEYCODE_SPACE -> commitBestCandidate()
            KeyEvent.KEYCODE_ENTER -> onReset()
            // There is no cursor inside a stroke sequence.
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> Unit
            else -> super.onSpecial(keyCode)
        }
    }

    private fun updateComposition() {
        currentInputConnection?.setComposingText(strokes.map(Stroke::label).joinToString(""), 1)
        val dictionary = dictionary ?: return
        val strokes = strokes
        searchJob?.cancel()
        searchJob = coroutineScope.launch {
            val candidates = dictionary.search(strokes, chineseTraditional).map(::StrokeCandidate)
            withContext(Dispatchers.Main) {
                // Typing may have moved on while searching.
                if(this@StrokeIMEMode.strokes == strokes) submitCandidates(candidates)
            }
        }
    }

    private fun commitBestCandidate() {
        if(strokes.isEmpty()) return
        val best = dictionary?.search(strokes, chineseTraditional, limit = 1)?.firstOrNull()
        if(best != null) commit(String(Character.toChars(best)))
        else onReset()
    }

    private fun commit(text: CharSequence) {
        clearStrokes()
        currentInputConnection?.commitText(text, 1)
        submitCandidates(emptyList())
    }

    private fun clearStrokes() {
        searchJob?.cancel()
        strokes = ""
    }

    data class StrokeCandidate(
        override val text: CharSequence
    ): CandidateView.Candidate {
        constructor(codePoint: Int): this(String(Character.toChars(codePoint)))
    }
}
