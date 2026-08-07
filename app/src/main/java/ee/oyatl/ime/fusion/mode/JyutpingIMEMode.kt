package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.korean.WordComposer
import org.jyutping.jyutping.BinaryDictionaries
import org.jyutping.jyutping.models.Researcher
import org.jyutping.jyutping.models.Segmenter
import org.jyutping.jyutping.models.VirtualInputKey

class JyutpingIMEMode(
    listener: IMEMode.Listener
): CommonIMEMode(listener) {
    private val wordComposer = WordComposer()
    private var bestCandidate: CandidateView.Candidate? = null

    private val handler: Handler = Handler(Looper.getMainLooper()) { msg ->
        when(msg.what) {
            MSG_UPDATE_SUGGESTIONS -> {
                updateSuggestions()
                true
            }
            else -> false
        }
    }

    override suspend fun onLoad(context: Context) {
        super.onLoad(context)
        Segmenter.prepare()
        BinaryDictionaries.loadDictionaries(context)
    }

    override fun onReset() {
        super.onReset()
        bestCandidate = null
    }

    override fun onChar(codePoint: Int) {
        wordComposer.commit(codePoint.toChar().toString())
        renderInput()
    }

    override fun onSpecial(keyCode: Int) {
        when(keyCode) {
            KeyEvent.KEYCODE_SPACE -> {
                if(wordComposer.composingText.isNotEmpty()) {
                    updateSuggestions()
                    val bestCandidate = bestCandidate
                    if(bestCandidate != null) onCandidateSelected(bestCandidate)
                } else {
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_SPACE)
                }
            }
            KeyEvent.KEYCODE_ENTER -> {
                if(wordComposer.composingText.isNotEmpty()) onReset()
                else {
                    if (util?.sendDefaultEditorAction(true) != true)
                        currentInputConnection?.commitText("\n", 1)
                    onReset()
                }
            }
            KeyEvent.KEYCODE_DEL -> {
                if(wordComposer.composingText.isNotEmpty()) {
                    wordComposer.delete(1)
                } else {
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                }
            }
            else -> super.onSpecial(keyCode)
        }
        renderInput()
    }

    private fun renderInput() {
        currentInputConnection?.setComposingText(wordComposer.getSpannableSurfaceString(), 1)
        postUpdateSuggestions()
    }

    private fun postUpdateSuggestions() {
        handler.removeMessages(MSG_UPDATE_SUGGESTIONS)
        handler.sendMessageDelayed(handler.obtainMessage(MSG_UPDATE_SUGGESTIONS), 100)
    }

    private fun updateSuggestions() {
        val keys = wordComposer.textBeforeCursor.mapNotNull { VirtualInputKey.matchVirtualInputKey(it) }
        val suggestions = Researcher.suggest(keys, Segmenter.segment(keys))
        val candidates = suggestions.map { JyutpingCandidate(it.text, it.romanization, it.input) }
        bestCandidate = candidates.firstOrNull()
        submitCandidates(candidates)
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        currentInputConnection?.commitText(candidate.text, 1)
        if(candidate is JyutpingCandidate) {
            wordComposer.consume(candidate.input.length)
        } else {
            wordComposer.consume(candidate.text.length)
        }
        renderInput()
    }

    class Params: IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): IMEMode {
            return JyutpingIMEMode(listener)
        }

        override fun getLabel(context: Context): String {
            return "粵拼"
        }

        override fun getShortLabel(
            context: Context,
            params: List<IMEMode.Params>
        ): String {
            return "粵拼"
        }
    }

    data class JyutpingCandidate(
        override val text: CharSequence,
        override val extra: CharSequence,
        val input: CharSequence
    ): CandidateView.Candidate, CandidateView.ExtraCandidate

    companion object {
        const val TYPE: String = "jyutping"
        const val MSG_UPDATE_SUGGESTIONS = 0
    }
}