package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.annotation.StringRes
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.Feature
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.hangul.HangulCombiner
import ee.oyatl.ime.fusion.korean.BigramHanjaConverter
import ee.oyatl.ime.fusion.korean.HanjaConverter
import ee.oyatl.ime.fusion.korean.JeongUnHanjaConverter
import ee.oyatl.ime.fusion.korean.UnigramHanjaConverter
import ee.oyatl.ime.fusion.korean.WordComposer
import ee.oyatl.ime.fusion.korean.layout.Hangul2Set
import ee.oyatl.ime.fusion.korean.layout.Hangul3Set
import ee.oyatl.ime.fusion.korean.layout.HangulOld
import ee.oyatl.ime.keyboard.DefaultBottomRowKeyboard
import ee.oyatl.ime.keyboard.DefaultMobileKeyboard
import ee.oyatl.ime.keyboard.DefaultTabletBottomRowKeyboard
import ee.oyatl.ime.keyboard.DefaultTabletKeyboard
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.KeyboardInflater
import ee.oyatl.ime.keyboard.ScreenTypeKeyboard
import ee.oyatl.ime.keyboard.ShiftStateKeyboard
import ee.oyatl.ime.keyboard.StackedKeyboard
import ee.oyatl.ime.keyboard.layout.KeyboardTemplates
import ee.oyatl.ime.keyboard.layout.LayoutSymbol
import java.util.Locale
import java.util.concurrent.Executors

abstract class KoreanIMEMode(
    listener: IMEMode.Listener
): CommonIMEMode(listener) {
    private val executor = Executors.newSingleThreadExecutor()

    private val handler: Handler = Handler(Looper.getMainLooper()) { msg ->
        when(msg.what) {
            MSG_CONVERT -> {
                convert()
                true
            }
            else -> false
        }
    }

    protected abstract val hangulCombiner: HangulCombiner
    private var currentState = HangulCombiner.State.Initial

    private val wordComposer: WordComposer = WordComposer()
    protected var hanjaConverter: HanjaConverter? = null

    override suspend fun onLoad(context: Context) {
        hanjaConverter =
            if(Feature.BigramHanjaConverter.availableInCurrentVersion) BigramHanjaConverter(context)
            else UnigramHanjaConverter(context)
    }

    override fun onReset() {
        super.onReset()
        wordComposer.reset()
        currentState = HangulCombiner.State.Initial
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        val inputConnection = currentInputConnection ?: return
        val length =
            if(candidate is CandidateView.VarLengthCandidate) candidate.inputLength
            else candidate.text.length
        wordComposer.consume(length)
        currentState = HangulCombiner.State.Initial
        inputConnection.commitText(candidate.text, 1)
        renderInputView()
    }

    private fun convert() {
        executor.execute {
            val candidates = hanjaConverter?.convert(wordComposer.word)
            if(candidates != null) handler.post { submitCandidates(candidates) }
        }
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
        if(result.textToCommit.isNotEmpty()) currentState = HangulCombiner.State.Initial
        if(result.newState.combined.isNotEmpty()) currentState = result.newState as HangulCombiner.State
        result.textToCommit.forEach { text -> wordComposer.commit(text.toString()) }
        wordComposer.compose(currentState.combined.toString())
        renderInputView()
    }

    override fun onSpecial(type: Keyboard.SpecialKey) {
        when(type) {
            Keyboard.SpecialKey.Delete -> {
                if(currentState != HangulCombiner.State.Initial) {
                    currentState = currentState.previous as HangulCombiner.State
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

    class Hangul2SetKS(listener: IMEMode.Listener): KoreanIMEMode(listener) {
        override val hangulCombiner: HangulCombiner = HangulCombiner(Hangul2Set.COMB_KS, true)
        override val layoutTable: Map<Int, List<Int>> = Hangul2Set.TABLE_KS
    }

    /*
     * Common part for 390 and 391
     */
    abstract class Hangul3Set390391(listener: IMEMode.Listener): KoreanIMEMode(listener) {
        override fun createSymbolKeyboard(): Keyboard {
            return StackedKeyboard(
                ShiftStateKeyboard(
                    super.createDefaultKeyboard(KeyboardInflater.inflate(LayoutSymbol.ROWS_LOWER, mapOf())[0]),
                    super.createDefaultKeyboard(KeyboardInflater.inflate(LayoutSymbol.ROWS_UPPER, mapOf())[0])
                ),
                ShiftStateKeyboard(
                    createSymbolBottomRowKeyboard(shift = false),
                    createSymbolBottomRowKeyboard(shift = true)
                )
            )
        }

        private fun createSymbolBottomRowKeyboard(shift: Boolean): Keyboard {
            return super.createBottomRowKeyboard(shift, true)
        }

        override fun createDefaultKeyboard(layer: List<List<Int>>): Keyboard {
            return ScreenTypeKeyboard(
                mobile = DefaultMobileKeyboard(layer),
                tablet = DefaultTabletKeyboard(layer, listOf())
            )
        }

        override fun createBottomRowKeyboard(shift: Boolean, symbol: Boolean): Keyboard {
            if(symbol) return super.createBottomRowKeyboard(shift, true)
            val extraKeys =
                if(!shift) listOf('.'.code, layoutTable[KeyEvent.KEYCODE_SLASH]!![0])
                else listOf('.'.code, layoutTable[KeyEvent.KEYCODE_SLASH]!![1])
            return ScreenTypeKeyboard(
                mobile = DefaultBottomRowKeyboard(extraKeys = extraKeys, isSymbols = false),
                tablet = DefaultTabletBottomRowKeyboard(extraKeys = extraKeys, isSymbols = false)
            )
        }
    }

    class Hangul3Set390(listener: IMEMode.Listener): Hangul3Set390391(listener) {
        override val hangulCombiner: HangulCombiner = HangulCombiner(Hangul3Set.COMBINATION_390, true)
        override val layoutTable: Map<Int, List<Int>> = Hangul3Set.TABLE_390
        override fun createTextKeyboard(): Keyboard {
            val layers = KeyboardInflater.inflate(KeyboardTemplates.MOBILE_WITH_QUOTE, layoutTable)
            return StackedKeyboard(
                ShiftStateKeyboard(
                    createDefaultKeyboard(layers[0]),
                    createDefaultKeyboard(modifyShiftedLayout(layers[1]))
                ),
                ShiftStateKeyboard(
                    createBottomRowKeyboard(shift = false, symbol = false),
                    createBottomRowKeyboard(shift = true, symbol = false)
                )
            )
        }

        /*
         * Modify shifted bottom row for number entry.
         */
        private fun modifyShiftedLayout(shifted: List<List<Int>>): List<List<Int>> {
            val bottom = shifted[3].toMutableList()
            bottom.remove('!'.code)
            bottom.remove('"'.code)
            bottom += '2'.code
            bottom += '3'.code
            return listOf(
                shifted[0],
                shifted[1],
                shifted[2],
                bottom
            )
        }
    }

    class Hangul3Set391(listener: IMEMode.Listener): Hangul3Set390391(listener) {
        override val hangulCombiner: HangulCombiner = HangulCombiner(Hangul3Set.COMBINATION_391, true)
        override val layoutTable: Map<Int, List<Int>> = Hangul3Set.TABLE_391
        override fun createTextKeyboard(): Keyboard {
            val layers = KeyboardInflater.inflate(KeyboardTemplates.MOBILE_WITH_QUOTE, layoutTable)
            return StackedKeyboard(
                ShiftStateKeyboard(
                    createDefaultKeyboard(layers[0]),
                    createDefaultKeyboard(layers[1])
                ),
                ShiftStateKeyboard(
                    createBottomRowKeyboard(shift = false, symbol = false),
                    createBottomRowKeyboard(shift = true, symbol = false)
                )
            )
        }
    }

    class HangulOld2Set(listener: IMEMode.Listener): KoreanIMEMode(listener) {
        override val hangulCombiner: HangulCombiner = HangulCombiner(HangulOld.COMB_FULL, false)
        override val layoutTable: Map<Int, List<Int>> = HangulOld.TABLE_OLD_2SET
        override suspend fun onLoad(context: Context) {
            hanjaConverter = JeongUnHanjaConverter(context)
        }
    }

    data class Params(
        val layout: Layout
    ): IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): IMEMode {
            return when(layout) {
                Layout.Set2KS -> Hangul2SetKS(listener)
                Layout.Set3390 -> Hangul3Set390(listener)
                Layout.Set3391 -> Hangul3Set391(listener)
                Layout.Set2Old -> HangulOld2Set(listener)
            }
        }

        override fun getLabel(context: Context): String {
            val localeName = Locale.KOREAN.displayName
            val layoutName = context.resources.getString(layout.nameKey)
            return "$localeName $layoutName"
        }

        override fun getShortLabel(context: Context): String {
            return when(layout) {
                Layout.Set2KS -> "한2"
                Layout.Set3390, Layout.Set3391 -> "한3"
                Layout.Set2Old -> "ᄒᆞ"
                else -> "한"
            }
        }

        companion object {
            fun parse(map: Map<String, String>): Params {
                val layout = Layout.valueOf(map["layout"] ?: Layout.Set2KS.name)
                return Params(
                    layout = layout
                )
            }
        }
    }

    enum class Layout(
        @StringRes val nameKey: Int
    ) {
        Set2KS(R.string.korean_layout_hangul_2set_ks),
        Set3390(R.string.korean_layout_hangul_3set_390),
        Set3391(R.string.korean_layout_hangul_3set_391),
        Set2Old(R.string.korean_layout_old_hangul_2set_ks)
    }

    companion object {
        const val MSG_CONVERT = 0
        const val TYPE: String = "korean"
    }
}