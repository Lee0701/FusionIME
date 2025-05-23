package ee.oyatl.ime.fusion

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.korean.HanjaConverter
import ee.oyatl.ime.fusion.korean.WordComposer
import ee.oyatl.ime.fusion.korean.layout.Hangul2Set
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

abstract class KoreanIMEMode(
    listener: IMEMode.Listener
): CommonIMEMode(listener) {

    private val handler: Handler = Handler(Looper.getMainLooper()) { msg ->
        when(msg.what) {
            MSG_CONVERT -> {
                convert()
                true
            }
            else -> false
        }
    }

    class Hangul2SetKS(listener: IMEMode.Listener): KoreanIMEMode(listener) {
        override val hangulCombiner: HangulCombiner = HangulCombiner(Hangul2Set.COMB_KS, true)
        override val layoutTable: Map<Int, List<Int>> = Hangul2Set.TABLE_KS
        private val layers = KeyboardInflater.inflate(KeyboardTemplates.MOBILE, layoutTable)
        override val textKeyboard: Keyboard = StackedKeyboard(
            ShiftStateKeyboard(
                DefaultMobileKeyboard(layers[0]),
                DefaultMobileKeyboard(layers[1])
            ),
            DefaultBottomRowKeyboard()
        )
    }

    class Hangul3Set391(listener: IMEMode.Listener): KoreanIMEMode(listener) {
        override val hangulCombiner: HangulCombiner = HangulCombiner(Hangul3Set.COMBINATION_391, true)
        override val layoutTable: Map<Int, List<Int>> = Hangul3Set.TABLE_391
        private val layers = KeyboardInflater.inflate(KeyboardTemplates.MOBILE_WITH_QUOTE, layoutTable)
        override val textKeyboard: Keyboard = StackedKeyboard(
            ShiftStateKeyboard(
                DefaultMobileKeyboard(layers[0]),
                DefaultMobileKeyboard(layers[1])
            ),
            DefaultBottomRowKeyboard(extraKeys = listOf('.'.code, 0x1001169))
        )
    }

    protected abstract val hangulCombiner: HangulCombiner
    private val stateStack: MutableList<Combiner.State> = mutableListOf(HangulCombiner.State.Initial)
    private val currentState: HangulCombiner.State get() = stateStack.last() as HangulCombiner.State

    private val wordComposer: WordComposer = WordComposer()
    private var hanjaConverter: HanjaConverter? = null

    override suspend fun onLoad(context: Context) {
        hanjaConverter = HanjaConverter(context)
    }

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
        renderInputView()
    }

    private fun convert() {
        val candidates = hanjaConverter?.convert(wordComposer.word)
        if(candidates != null) submitCandidates(candidates)
    }

    private fun postConvert() {
        handler.removeMessages(MSG_CONVERT)
        handler.sendMessageDelayed(handler.obtainMessage(MSG_CONVERT), 100)
    }

    private fun renderInputView() {
        currentInputConnection?.setComposingText(wordComposer.word, 1)
        postConvert()
    }

    override fun onChar(code: Int) {
        val result = hangulCombiner.combine(currentState, code)
        if(result.textToCommit.isNotEmpty()) resetStack()
        if(result.newState.combined.isNotEmpty()) stateStack += result.newState
        wordComposer.commit(result.textToCommit.toString())
        wordComposer.compose(currentState.combined.toString())
        renderInputView()
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
                renderInputView()
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

    companion object {
        const val MSG_CONVERT = 0
    }
}