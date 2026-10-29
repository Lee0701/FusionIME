package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.view.KeyEvent
import androidx.annotation.StringRes
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.hangul.HangulCombiner
import ee.oyatl.ime.fusion.hangul.JamoCombinationTable
import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPresetLoader
import ee.oyatl.ime.fusion.korean.BigramHanjaConverter
import ee.oyatl.ime.fusion.korean.HangulPredictor
import ee.oyatl.ime.fusion.korean.HanjaConverter
import ee.oyatl.ime.fusion.korean.JeongUnHanjaConverter
import ee.oyatl.ime.fusion.korean.UnigramHanjaConverter
import ee.oyatl.ime.fusion.korean.WordComposer
import ee.oyatl.ime.fusion.keyboard.core.SoftKeyCodeMapper
import ee.oyatl.ime.fusion.layout.Hangul2Set
import ee.oyatl.ime.fusion.layout.Hangul3Set
import ee.oyatl.ime.fusion.layout.HangulOld
import ee.oyatl.ime.fusion.keyboard.core.LayoutTable
import ee.oyatl.ime.fusion.layout.preset.KoreanLayoutPresets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.text.toBoolean

abstract class KoreanIMEMode(
    converterType: ConverterType,
    override val extra: Map<String, String>,
    listener: IMEMode.Listener
): CommonIMEMode(listener) {
    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val coroutineScope = CoroutineScope(dispatcher)
    private var convertJob: Job? = null

    private var hangulCombiner: HangulCombiner = HangulCombiner(JamoCombinationTable(), false)
    private var currentState = HangulCombiner.State.Initial

    private val wordComposer: WordComposer = WordComposer()
    private val hanjaConverter: HanjaConverter = converterType.create()

    override suspend fun onLoad(context: Context) {
        super.onLoad(context)
        if(this is Hangul2SetKS) {
            val loader = object: KeyboardLayoutPresetLoader() {
                override fun open(name: String): InputStream {
                    return context.assets.open("keyboard/${name}${EXT_YAML}")
                }
            }
            textLayoutPreset = loader.inflate(loader.load("def_hangul_2set_ks"))
        }
        val jamoCombinationTable = (textLayoutPreset.extra as? KeyboardLayoutPreset.Extra.Hangul)
            ?.jamoCombinationTable ?: JamoCombinationTable()
        val correctOrders = (extra["correct_orders"] ?: "false").toBoolean()
        hangulCombiner = HangulCombiner(jamoCombinationTable, correctOrders)
        hanjaConverter.load(context)
    }

    override fun onReset() {
        super.onReset()
        wordComposer.reset()
        currentState = HangulCombiner.State.Initial
        hanjaConverter.reset()
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

    private fun postConvert() {
        convertJob?.cancel()
        convertJob = coroutineScope.launch {
            val candidates = hanjaConverter.convert(wordComposer.textBeforeCursor)
            submitCandidates(candidates)
        }
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
                    util?.deleteSurroundingText(1, 0)
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

    abstract class Hangul2SetKSCompatible(
        converterType: ConverterType,
        extra: Map<String, String>,
        listener: IMEMode.Listener
    ): KoreanIMEMode(converterType, extra, listener) {
        abstract val layoutTable: LayoutTable
        abstract val jamoCombinationTable: JamoCombinationTable
        override var textLayoutPreset: KeyboardLayoutPreset =
            KoreanLayoutPresets.ksCompatible(
                layoutTable,
                jamoCombinationTable
            )
    }

    class Hangul2SetKS(
        converterType: ConverterType,
        extra: Map<String, String>,
        listener: IMEMode.Listener
    ): KoreanIMEMode(converterType, extra, listener)

    /*
     * Common part for 390 and 391
     */
    abstract class Hangul3Set390391(
        converterType: ConverterType,
        extra: Map<String, String>,
        listener: IMEMode.Listener
    ): KoreanIMEMode(converterType, extra + mapOf("number_row" to "true"), listener) {
        abstract val layoutTable: LayoutTable
        abstract val jamoCombinationTable: JamoCombinationTable
        abstract val softKeyCodeMapper: SoftKeyCodeMapper
        override var textLayoutPreset: KeyboardLayoutPreset =
            KoreanLayoutPresets.threeSet390391(
                layoutTable,
                softKeyCodeMapper,
                jamoCombinationTable
            )
    }

    class Hangul3Set390(
        converterType: ConverterType,
        extra: Map<String, String>,
        listener: IMEMode.Listener
    ): Hangul3Set390391(converterType, extra, listener) {
        override val layoutTable: LayoutTable get() = LayoutTable.fromShiftStates(Hangul3Set.TABLE_390)
        override val jamoCombinationTable: JamoCombinationTable = JamoCombinationTable.from(Hangul3Set.COMBINATION_390_391)
        override val softKeyCodeMapper: SoftKeyCodeMapper get() = SoftKeyCodeMapper.ByScreenMode(
            mobile = SoftKeyCodeMapper.Basic(Hangul3Set.KEYCODE_MAP_390_MOBILE),
            tablet = SoftKeyCodeMapper.Empty
        )
    }

    class Hangul3Set391(
        converterType: ConverterType,
        extra: Map<String, String>,
        listener: IMEMode.Listener
    ): Hangul3Set390391(converterType, extra, listener) {
        override val layoutTable: LayoutTable get() = LayoutTable.fromShiftStates(Hangul3Set.TABLE_391)
        override val jamoCombinationTable: JamoCombinationTable = JamoCombinationTable.from(Hangul3Set.COMBINATION_390_391)
        override val softKeyCodeMapper: SoftKeyCodeMapper get() = SoftKeyCodeMapper.Empty
    }

    class Hangul3Set391Strict(
        converterType: ConverterType,
        extra: Map<String, String>,
        listener: IMEMode.Listener
    ): Hangul3Set390391(converterType, extra, listener) {
        override val layoutTable: LayoutTable get() = LayoutTable.fromShiftStates(Hangul3Set.TABLE_391_STRICT)
        override val jamoCombinationTable: JamoCombinationTable = JamoCombinationTable.from(Hangul3Set.COMBINATION_391_STRICT)
        override val softKeyCodeMapper: SoftKeyCodeMapper get() = SoftKeyCodeMapper.Empty
    }

    class HangulOld2Set(
        converterType: ConverterType,
        extra: Map<String, String>,
        listener: IMEMode.Listener
    ): Hangul2SetKSCompatible(converterType, extra, listener) {
        override val layoutTable: LayoutTable get() = LayoutTable.fromShiftStates(HangulOld.TABLE_OLD_2SET)
        override val jamoCombinationTable: JamoCombinationTable = JamoCombinationTable.from(HangulOld.COMBINATION_FULL)
    }

    class HangulOld3Set393(
        converterType: ConverterType,
        extra: Map<String, String>,
        listener: IMEMode.Listener
    ): KoreanIMEMode(converterType, extra + mapOf("number_row" to "true"), listener) {
        override var textLayoutPreset: KeyboardLayoutPreset = KoreanLayoutPresets.threeSet393()
    }

    data class Params(
        val layout: Layout,
        val converterType: ConverterType,
        val extra: Map<String, String>
    ): IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): IMEMode {
            return when(layout) {
                Layout.Set2KS -> Hangul2SetKS(converterType, extra, listener)
                Layout.Set3390 -> Hangul3Set390(converterType, extra, listener)
                Layout.Set3391 -> Hangul3Set391(converterType, extra, listener)
                Layout.Set3391Strict -> Hangul3Set391Strict(converterType, extra, listener)
                Layout.Set2Old -> HangulOld2Set(converterType, extra, listener)
                Layout.Set3Old393 -> HangulOld3Set393(converterType, extra, listener)
            }
        }

        override fun getLabel(context: Context): String {
            val localeName = Locale.KOREAN.displayName
            val layoutName = context.resources.getString(layout.nameKey)
            return "$localeName $layoutName"
        }

        override fun getShortLabels(context: Context): List<String> {
            return when(layout) {
                Layout.Set2KS -> listOf("한", "한2")
                Layout.Set3390 -> listOf("한", "한3", "390")
                Layout.Set3391 -> listOf("한", "한3", "391")
                Layout.Set3391Strict -> listOf("한", "한3", "391", "391S")
                Layout.Set2Old -> listOf("ᄒᆞ", "ᄒᆞ2")
                Layout.Set3Old393 -> listOf("ᄒᆞ", "ᄒᆞ3", "393")
            }
        }

        companion object {
            fun parse(map: Map<String, String>): Params {
                val layout = Layout.entries.find { it.name == map["layout"] } ?: Layout.Set2KS
                val converterType = ConverterType.valueOf(map["converter"] ?: ConverterType.Word.name)
                return Params(
                    layout = layout,
                    converterType = converterType,
                    extra = map
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
        Set3391Strict(R.string.korean_layout_hangul_3set_391_strict),
        Set2Old(R.string.korean_layout_old_hangul_2set_ks),
        Set3Old393(R.string.korean_layout_old_hangul_3set_393)
    }

    enum class ConverterType {
        Word, Phrase, JeongUn, HangulOnly;

        fun create(): HanjaConverter {
            return when(this) {
                Word -> UnigramHanjaConverter()
                Phrase -> BigramHanjaConverter()
                JeongUn -> JeongUnHanjaConverter()
                HangulOnly -> HangulPredictor()
            }
        }
    }

    companion object {
        const val TYPE: String = "korean"
    }
}