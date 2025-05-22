package ee.oyatl.ime.fusion

import android.content.Context
import android.view.KeyEvent
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.korean.WordComposer
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.viet.HanNomConverter

abstract class VietIMEMode(
    context: Context,
    listener: IMEMode.Listener
): CommonIMEMode(listener) {

    class Qwerty(
        context: Context,
        listener: IMEMode.Listener
    ): VietIMEMode(context, listener) {
        override val keyboardMode: String = "q"
    }

    class Telex(
        context: Context,
        listener: IMEMode.Listener
    ): VietIMEMode(context, listener) {
        override val keyboardMode: String = "t"
    }

    abstract val keyboardMode: String

    private val wordComposer: WordComposer = WordComposer()
    private val converter: HanNomConverter = HanNomConverter(context)

    private var bestCandidate: HanNomConverter.Candidate? = null

    override fun onReset() {
        super.onReset()
        wordComposer.reset()
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        val inputConnection = currentInputConnection ?: return
        if(candidate is HanNomConverter.Candidate) {
            wordComposer.consume(candidate.key.length)
            inputConnection.commitText(candidate.text, 1)
            renderInputView()
        }
    }

    private fun convert() {
        val candidates = converter.convert(wordComposer.word, keyboardMode)
        bestCandidate = candidates.firstOrNull() as? HanNomConverter.Candidate
        submitCandidates(candidates)
    }

    private fun renderInputView() {
        currentInputConnection?.setComposingText(wordComposer.word, 1)
        convert()
    }

    override fun onChar(code: Int) {
        wordComposer.commit(code.toChar().toString())
        renderInputView()
    }

    override fun onSpecial(type: Keyboard.SpecialKey) {
        when(type) {
            Keyboard.SpecialKey.Space -> {
                val bestCandidate = bestCandidate
                if(bestCandidate != null) {
                    onCandidateSelected(bestCandidate)
                } else {
                    onReset()
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_SPACE)
                }
            }
            Keyboard.SpecialKey.Return -> {
                onReset()
                if(util?.sendDefaultEditorAction(true) != true)
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            }
            Keyboard.SpecialKey.Delete -> {
                if(wordComposer.word.isNotEmpty()) {
                    wordComposer.delete(1)
                    renderInputView()
                } else {
                    onReset()
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                }
            }
            else -> {}
        }
    }
}