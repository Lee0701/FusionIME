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
import ee.oyatl.ime.keyboard.SoftKeyCodeMapper
import ee.oyatl.ime.keyboard.KeyboardConfiguration
import ee.oyatl.ime.keyboard.KeyboardTemplate
import ee.oyatl.ime.keyboard.layout.Hangul2Set
import ee.oyatl.ime.keyboard.layout.Hangul3Set
import ee.oyatl.ime.keyboard.layout.HangulOld
import ee.oyatl.ime.keyboard.LayoutTable
import ee.oyatl.ime.keyboard.layout.ExtKeyCode
import ee.oyatl.ime.keyboard.layout.MobileKeyboard
import ee.oyatl.ime.keyboard.layout.MobileKeyboardRows
import ee.oyatl.ime.keyboard.layout.LayoutExt
import ee.oyatl.ime.keyboard.layout.LayoutQwerty
import ee.oyatl.ime.keyboard.layout.TabletKeyboard
import ee.oyatl.ime.keyboard.layout.TabletKeyboardRows
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
        wordComposer.moveCursor(wordComposer.composingText.length)
        currentState = HangulCombiner.State.Initial
        inputConnection.commitText(candidate.text, 1)
        renderInputView()
    }

    private fun convert() {
        executor.execute {
            val candidates = hanjaConverter.convert(wordComposer.textBeforeCursor)
            handler.post { submitCandidates(candidates) }
        }
    }

    private fun postConvert() {
        handler.removeMessages(MSG_CONVERT)
        handler.sendMessageDelayed(handler.obtainMessage(MSG_CONVERT), 100)
    }

    private fun renderInputView() {
        currentInputConnection?.setComposingText(wordComposer.getSpannableSurfaceString(), 1)
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
                } else if(wordComposer.composingText.isNotEmpty()) {
                    wordComposer.delete(1)
                } else {
                    currentInputConnection?.deleteSurroundingText(1, 0)
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
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                currentState = HangulCombiner.State.Initial
                wordComposer.commit()
                if(wordComposer.composingText.isNotEmpty()) {
                    wordComposer.moveCursorRelative(-1)
                    renderInputView()
                } else {
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT)
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                currentState = HangulCombiner.State.Initial
                wordComposer.commit()
                if(wordComposer.composingText.isNotEmpty()) {
                    wordComposer.moveCursorRelative(1)
                    renderInputView()
                } else {
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT)
                }
            }
            else -> super.onSpecial(keyCode)
        }
    }

    class Hangul2SetKS(
        correctOrders: Boolean,
        converterType: ConverterType,
        listener: IMEMode.Listener
    ): KoreanIMEMode(listener) {
        override val hangulCombiner: HangulCombiner = HangulCombiner(Hangul2Set.COMB_KS, correctOrders)
        override val hanjaConverter: HanjaConverter = converterType.create()
        override val textLayoutTable: LayoutTable = LayoutTable.from(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + Hangul2Set.TABLE_KS)
    }

    /*
     * Common part for 390 and 391
     */
    abstract class Hangul3Set390391(listener: IMEMode.Listener): KoreanIMEMode(listener) {
        open val softKeyCodeMapper: SoftKeyCodeMapper get() = SoftKeyCodeMapper()
        override val textKeyboardTemplate: KeyboardTemplate = KeyboardTemplate.ByScreenMode(
            mobile = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    MobileKeyboard.numbers(),
                    MobileKeyboard.alphabetic(semicolon = true, shiftDeleteWidth = 1f),
                    MobileKeyboard.bottom(ExtKeyCode.KEYCODE_PERIOD_COMMA, KeyEvent.KEYCODE_SLASH)
                ),
                contentRows = MobileKeyboardRows.NUMBERS + MobileKeyboardRows.SEMICOLON_QUOTE,
                softKeyCodeMapper = softKeyCodeMapper
            ),
            tablet = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    TabletKeyboard.numbers(delete = true),
                    TabletKeyboard.alphabetic(semicolon = true, delete = false),
                    TabletKeyboard.bottom()
                ),
                contentRows = TabletKeyboardRows.NUMBERS + TabletKeyboardRows.SEMICOLON_QUOTE_SLASH,
                softKeyCodeMapper = softKeyCodeMapper
            )
        )
    }

    class Hangul3Set390(
        correctOrders: Boolean,
        converterType: ConverterType,
        listener: IMEMode.Listener
    ): Hangul3Set390391(listener) {
        override val hangulCombiner: HangulCombiner = HangulCombiner(Hangul3Set.COMBINATION_390, correctOrders)
        override val hanjaConverter: HanjaConverter = converterType.create()
        override val textLayoutTable: LayoutTable = LayoutTable.from(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + Hangul3Set.TABLE_390)
        override val softKeyCodeMapper: SoftKeyCodeMapper get() = SoftKeyCodeMapper(mapOf(
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
        override val textLayoutTable: LayoutTable = LayoutTable.from(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + Hangul3Set.TABLE_391)
    }

    class HangulOld2Set(
        correctOrders: Boolean,
        converterType: ConverterType,
        listener: IMEMode.Listener
    ): KoreanIMEMode(listener) {
        override val hangulCombiner: HangulCombiner = HangulCombiner(HangulOld.COMB_FULL, correctOrders)
        override val hanjaConverter: HanjaConverter = converterType.create()
        override val textLayoutTable: LayoutTable = LayoutTable.from(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + HangulOld.TABLE_OLD_2SET)
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

        override fun getShortLabel(context: Context, params: List<IMEMode.Params>): String {
            val koreanParams = params.filterIsInstance<Params>().filterNot { it == this }
            // If this is the only Korean mode
            if(koreanParams.isEmpty()) {
                return when(layout) {
                    // For modern Hangul layouts
                    Layout.Set2KS, Layout.Set3390, Layout.Set3391 -> "한"
                    // For old Hangul layouts
                    Layout.Set2Old -> "ᄒᆞ"
                }
            }
            // If there are any other Korean modes
            return when(layout) {
                // For 2-set layouts
                Layout.Set2KS -> "한2"
                Layout.Set3390, Layout.Set3391 -> {
                    // Find if there are any other 3-set layouts
                    val korean3SetParams = koreanParams.filter { it.layout in setOf(Layout.Set3390, Layout.Set3391) }
                    // If this is the only mode with 3-set layout
                    if(korean3SetParams.isEmpty()) "한3"
                    // If not, use specific layout name
                    else when(layout) {
                        Layout.Set3390 -> "390"
                        Layout.Set3391 -> "391"
                    }
                }
                // For old Hangul layouts
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