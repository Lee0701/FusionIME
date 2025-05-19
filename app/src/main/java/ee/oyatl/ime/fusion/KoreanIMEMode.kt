package ee.oyatl.ime.fusion

import android.content.Context
import android.view.KeyEvent
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.korean.HanjaConverter
import ee.oyatl.ime.fusion.korean.WordComposer
import ee.oyatl.ime.fusion.korean.layout.Hangul3Set
import ee.oyatl.ime.hardhanja.hangul.Combiner
import ee.oyatl.ime.hardhanja.hangul.HangulCombiner
import ee.oyatl.ime.keyboard.DefaultBottomRowKeyboard
import ee.oyatl.ime.keyboard.DefaultMobileKeyboard
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.KeyboardInflater
import ee.oyatl.ime.keyboard.ShiftStateKeyboard
import ee.oyatl.ime.keyboard.StackedKeyboard
import ee.oyatl.ime.keyboard.layout.KeyboardTemplates

class KoreanIMEMode(
    context: Context,
    listener: IMEMode.Listener
): CommonIMEMode(listener) {
    private val hangulCombiner: HangulCombiner = HangulCombiner(Hangul3Set.COMBINATION_391, true)
    private val stateStack: MutableList<Combiner.State> = mutableListOf()
    private val currentState: HangulCombiner.State get() = stateStack.last() as HangulCombiner.State

    override val layoutTable: Map<Int, List<Int>> = Hangul3Set.TABLE_391
    private val layers = KeyboardInflater.inflate(KeyboardTemplates.MOBILE_WITH_QUOTE, Hangul3Set.TABLE_391)
    override val textKeyboard: Keyboard = StackedKeyboard(
        ShiftStateKeyboard(
            DefaultMobileKeyboard(layers[0]),
            DefaultMobileKeyboard(layers[1])
        ),
        DefaultBottomRowKeyboard(extraKeys = listOf('.'.code, 0x1001169))
    )

    private val wordComposer: WordComposer = WordComposer()
    private val hanjaConverter: HanjaConverter = HanjaConverter(context)

    override fun onReset() {
        super.onReset()
        wordComposer.reset()
        resetStack()
    }

    private fun resetStack() {
        stateStack.clear()
        stateStack += HangulCombiner.State.Initial
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        val inputConnection = currentInputConnection ?: return
        wordComposer.consume(candidate.text.length)
        resetStack()
        inputConnection.commitText(candidate.text, 1)
        renderResult()
    }

    private fun renderResult() {
        currentInputConnection?.setComposingText(wordComposer.word, 1)
        val candidates = hanjaConverter.convert(wordComposer.word)
        submitCandidates(candidates)
    }

    override fun onChar(code: Int) {
        val result = hangulCombiner.combine(currentState, code)
        if(result.textToCommit.isNotEmpty()) resetStack()
        if(result.newState.combined.isNotEmpty()) stateStack += result.newState
        wordComposer.commit(result.textToCommit.toString())
        wordComposer.compose(currentState.combined.toString())
        renderResult()
    }

    override fun onSpecial(type: Keyboard.SpecialKey) {
        when(type) {
            Keyboard.SpecialKey.Delete -> {
                if(currentState != HangulCombiner.State.Initial) {
                    stateStack.removeLastOrNull()
                    wordComposer.compose(currentState.combined.toString())
                } else if(!wordComposer.delete(1)) {
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                }
                renderResult()
            }
            Keyboard.SpecialKey.Space -> {
                onReset()
                util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_SPACE)
            }
            Keyboard.SpecialKey.Return -> {
                onReset()
                util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            }
            else -> {}
        }
    }
}