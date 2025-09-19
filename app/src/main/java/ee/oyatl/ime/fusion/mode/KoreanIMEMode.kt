package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.annotation.StringRes
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.hangul.HangulCombiner
import ee.oyatl.ime.fusion.korean.BigramHanjaConverter
import ee.oyatl.ime.fusion.korean.HanjaConverter
import ee.oyatl.ime.fusion.korean.JeongUnHanjaConverter
import ee.oyatl.ime.fusion.korean.UnigramHanjaConverter
import ee.oyatl.ime.fusion.korean.WordComposer
import ee.oyatl.ime.keyboard.KeyCodeMapper
import ee.oyatl.ime.keyboard.KeyboardConfiguration
import ee.oyatl.ime.keyboard.layout.Hangul2Set
import ee.oyatl.ime.keyboard.layout.Hangul3Set
import ee.oyatl.ime.keyboard.layout.HangulOld
import ee.oyatl.ime.keyboard.LayoutTable
import ee.oyatl.ime.keyboard.layout.ExtKeyCode
import ee.oyatl.ime.keyboard.layout.KeyboardConfigurations
import ee.oyatl.ime.keyboard.layout.KeyboardTemplates
import ee.oyatl.ime.keyboard.layout.LayoutExt
import ee.oyatl.ime.keyboard.layout.LayoutQwerty
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
    protected abstract val hanjaConverter: HanjaConverter

    override suspend fun onLoad(context: Context) {
        super.onLoad(context)
        hanjaConverter.load(context)
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
            val candidates = hanjaConverter.convert(wordComposer.word)
            handler.post { submitCandidates(candidates) }
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

    override fun onChar(codePoint: Int) {
        val result = hangulCombiner.combine(currentState, codePoint)
        if(result.textToCommit.isNotEmpty()) currentState = HangulCombiner.State.Initial
        if(result.newState.combined.isNotEmpty()) currentState = result.newState as HangulCombiner.State
        result.textToCommit.forEach { text -> wordComposer.commit(text.toString()) }
        wordComposer.compose(currentState.combined.toString())
        renderInputView()
    }

    override fun onSpecial(keyCode: Int) {
        when(keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                if(currentState != HangulCombiner.State.Initial) {
                    currentState = currentState.previous as HangulCombiner.State
                    wordComposer.compose(currentState.combined.toString())
                } else if(!wordComposer.delete(1)) {
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                }
                renderInputView()
            }
            KeyEvent.KEYCODE_SPACE -> {
                onReset()
                util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_SPACE)
            }
            KeyEvent.KEYCODE_ENTER -> {
                onReset()
                util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            }
            else -> {}
        }
    }

    class Hangul2SetKS(
        correctOrders: Boolean,
        converterType: ConverterType,
        listener: IMEMode.Listener
    ): KoreanIMEMode(listener) {
        override val hangulCombiner: HangulCombiner = HangulCombiner(Hangul2Set.COMB_KS, correctOrders)
        override val hanjaConverter: HanjaConverter = converterType.create()
        override val layoutTable: LayoutTable = LayoutTable.from(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + Hangul2Set.TABLE_KS)
    }

    /*
     * Common part for 390 and 391
     */
    abstract class Hangul3Set390391(listener: IMEMode.Listener): KoreanIMEMode(listener) {
        override val keyboardConfiguration: KeyboardConfiguration = KeyboardConfiguration(
            KeyboardConfigurations.mobileNumbers(),
            KeyboardConfigurations.mobileAlpha(semicolon = true, shiftDeleteWidth = 1f),
            KeyboardConfigurations.mobileBottom(ExtKeyCode.KEYCODE_PERIOD_COMMA, KeyEvent.KEYCODE_SLASH)
        )
        override val keyboardTemplate: List<String> =
            KeyboardTemplates.MOBILE_NUMBERS + KeyboardTemplates.MOBILE_SEMICOLON_QUOTE
    }

    class Hangul3Set390(
        correctOrders: Boolean,
        converterType: ConverterType,
        listener: IMEMode.Listener
    ): Hangul3Set390391(listener) {
        override val hangulCombiner: HangulCombiner = HangulCombiner(Hangul3Set.COMBINATION_390, correctOrders)
        override val hanjaConverter: HanjaConverter = converterType.create()
        override val layoutTable: LayoutTable = LayoutTable.from(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + Hangul3Set.TABLE_390)
        override val keyCodeMapper: KeyCodeMapper = KeyCodeMapper(mapOf(
            KeyEvent.KEYCODE_B to ExtKeyCode.KEYCODE_390_0,
            KeyEvent.KEYCODE_N to ExtKeyCode.KEYCODE_390_1,
            KeyEvent.KEYCODE_M to ExtKeyCode.KEYCODE_390_2,
            KeyEvent.KEYCODE_APOSTROPHE to ExtKeyCode.KEYCODE_390_3,
        ))
    }

    class Hangul3Set391(
        correctOrders: Boolean,
        converterType: ConverterType,
        listener: IMEMode.Listener
    ): Hangul3Set390391(listener) {
        override val hangulCombiner: HangulCombiner = HangulCombiner(Hangul3Set.COMBINATION_391, correctOrders)
        override val hanjaConverter: HanjaConverter = converterType.create()
        override val layoutTable: LayoutTable = LayoutTable.from(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + Hangul3Set.TABLE_391)
    }

    class HangulOld2Set(
        correctOrders: Boolean,
        converterType: ConverterType,
        listener: IMEMode.Listener
    ): KoreanIMEMode(listener) {
        override val hangulCombiner: HangulCombiner = HangulCombiner(HangulOld.COMB_FULL, correctOrders)
        override val hanjaConverter: HanjaConverter = converterType.create()
        override val layoutTable: LayoutTable = LayoutTable.from(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + HangulOld.TABLE_OLD_2SET)
    }

    data class Params(
        val layout: Layout,
        val correctOrders: Boolean,
        val converterType: ConverterType
    ): IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): IMEMode {
            return when(layout) {
                Layout.Set2KS -> Hangul2SetKS(correctOrders, converterType, listener)
                Layout.Set3390 -> Hangul3Set390(correctOrders, converterType, listener)
                Layout.Set3391 -> Hangul3Set391(correctOrders, converterType, listener)
                Layout.Set2Old -> HangulOld2Set(correctOrders, converterType, listener)
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
            }
        }

        companion object {
            fun parse(map: Map<String, String>): Params {
                val layout = Layout.valueOf(map["layout"] ?: Layout.Set2KS.name)
                val converterType = ConverterType.valueOf(map["converter"] ?: ConverterType.Word.name)
                val correctOrders = (map["correct_orders"] ?: "false").toBoolean()
                return Params(
                    layout = layout,
                    converterType = converterType,
                    correctOrders = correctOrders
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

    enum class ConverterType {
        Word, Phrase, JeongUn;

        fun create(): HanjaConverter {
            return when(this) {
                Word -> UnigramHanjaConverter()
                Phrase -> BigramHanjaConverter()
                JeongUn -> JeongUnHanjaConverter()
            }
        }
    }

    companion object {
        const val MSG_CONVERT = 0
        const val TYPE: String = "korean"
    }
}