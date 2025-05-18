package ee.oyatl.ime.fusion

import android.view.KeyEvent
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.korean.Hangul3Set
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
    listener: IMEMode.Listener
): CommonIMEMode(listener) {

    private val hangulCombiner: HangulCombiner = HangulCombiner(Hangul3Set.COMBINATION_391, true)
    private val stateStack: MutableList<Combiner.State> = mutableListOf()
    private val currentState: HangulCombiner.State get() = stateStack.last() as HangulCombiner.State

    private val layers = KeyboardInflater.inflate(KeyboardTemplates.MOBILE_WITH_SLASH, Hangul3Set.TABLE_391)
    override val textKeyboard: Keyboard = StackedKeyboard(
        ShiftStateKeyboard(
            DefaultMobileKeyboard(layers[0]),
            DefaultMobileKeyboard(layers[1])
        ),
        DefaultBottomRowKeyboard(extraKeys = listOf('.'.code, 0x1001169))
    )

    override fun onReset() {
        super.onReset()
        resetStack()
    }

    private fun resetStack() {
        stateStack.clear()
        stateStack += HangulCombiner.State.Initial
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
    }

    override fun onChar(code: Int) {
        val result = hangulCombiner.combine(currentState, code)
        if(result.textToCommit.isNotEmpty()) resetStack()
        if(result.newState.combined.isNotEmpty()) stateStack += result.newState
        currentInputConnection?.commitText(result.textToCommit, 1)
        currentInputConnection?.setComposingText(currentState.combined, 1)
    }

    override fun onSpecial(type: Keyboard.SpecialKey) {
        when(type) {
            Keyboard.SpecialKey.Delete -> {
                if(currentState == HangulCombiner.State.Initial) {
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                } else {
                    stateStack.removeLastOrNull()
                    currentInputConnection?.setComposingText(currentState.combined, 1)
                }
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