package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.view.KeyEvent
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.keyboard.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.hangul.CheonjiinComposer
import ee.oyatl.ime.fusion.hangul.Combiner
import ee.oyatl.ime.fusion.hangul.HangulCombiner
import ee.oyatl.ime.fusion.korean.BigramHanjaConverter
import ee.oyatl.ime.fusion.korean.HanjaConverter
import ee.oyatl.ime.fusion.korean.JeongUnHanjaConverter
import ee.oyatl.ime.fusion.korean.UnigramHanjaConverter
import ee.oyatl.ime.fusion.korean.WordComposer
import ee.oyatl.ime.keyboard.SoftKeyCodeMapper
import ee.oyatl.ime.fusion.layout.Hangul2Set
import ee.oyatl.ime.fusion.layout.Hangul3Set
import ee.oyatl.ime.fusion.layout.HangulOld
import ee.oyatl.ime.fusion.layout.LayoutCheonjiin
import ee.oyatl.ime.keyboard.LayoutTable
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.layout.LayoutQwerty
import ee.oyatl.ime.fusion.layout.TabletKeyboard
import ee.oyatl.ime.fusion.layout.TabletKeyboardRows
import ee.oyatl.ime.keyboard.KeyLabel
import ee.oyatl.ime.keyboard.touchhandler.FlickTouchHandler
import ee.oyatl.ime.keyboard.touchhandler.TouchHandler
import ee.oyatl.ime.keyboard.KeyboardState
import ee.oyatl.ime.keyboard.KeyboardView
import ee.oyatl.ime.keyboard.touchhandler.FlickDirection
import ee.oyatl.ime.fusion.layout.preset.KoreanLayoutPresets
import ee.oyatl.ime.keyboard.KeyboardParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

abstract class KoreanIMEMode(
    converterType: ConverterType,
    listener: IMEMode.Listener
): CommonIMEMode(listener) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var convertJob: Job? = null

    protected abstract val hangulCombiner: Combiner
    private var currentState: Combiner.State = HangulCombiner.State.Initial
    protected val currentCombinerState: Combiner.State get() = currentState

    private val wordComposer: WordComposer = WordComposer()
    private val hanjaConverter: HanjaConverter = converterType.create()

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
        applyCombinerResult(hangulCombiner.combine(currentState, codePoint))
    }

    protected fun applyCombinerResult(result: Combiner.Result) {
        currentState = result.newState
        result.textToCommit.forEach { text -> wordComposer.commit(text.toString()) }
        wordComposer.compose(currentState.combined.toString())
        renderInputView()
    }

    override fun onSpecial(keyCode: Int) {
        when(keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                val previous = currentState.previous
                if(previous != null) {
                    currentState = previous
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
            KeyEvent.KEYCODE_NUM -> {
                onReset()
                super.onSpecial(keyCode)
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
        numberRow: Boolean,
        cursorKeys: Boolean,
        converterType: ConverterType,
        listener: IMEMode.Listener
    ): KoreanIMEMode(converterType, listener) {
        abstract val layoutTable: LayoutTable
        override var textLayoutPreset: KeyboardLayoutPreset =
            KoreanLayoutPresets.ksCompatible(
                layoutTable,
                numberRow,
                cursorKeys
            )
    }

    class Hangul2SetKS(
        correctOrders: Boolean,
        numberRow: Boolean,
        cursorKeys: Boolean,
        converterType: ConverterType,
        listener: IMEMode.Listener
    ): Hangul2SetKSCompatible(numberRow, cursorKeys, converterType, listener) {
        override val hangulCombiner: HangulCombiner = HangulCombiner(Hangul2Set.COMB_KS, correctOrders)
        override val layoutTable: LayoutTable get() = LayoutTable.fromShiftStates(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + Hangul2Set.TABLE_KS)
    }

    class Cheonjiin(
        correctOrders: Boolean,
        converterType: ConverterType,
        listener: IMEMode.Listener
    ): KoreanIMEMode(converterType, listener) {
        override val hangulCombiner: CheonjiinComposer = CheonjiinComposer(
            HangulCombiner(Hangul2Set.COMB_KS, correctOrders)
        )
        override var textLayoutPreset: KeyboardLayoutPreset = KoreanLayoutPresets.cheonjiin()
        override val keyLabels: Map<Int, KeyLabel>
            get() =
                if(symbolState == KeyboardState.Symbol.Text) {
                    super.keyLabels + LayoutCheonjiin.LABELS
                } else {
                    super.keyLabels
                }
        private val flickDirections: MutableMap<Int, FlickDirection> = mutableMapOf()
        override fun createTouchHandler(
            keyboardView: KeyboardView,
            params: KeyboardParams,
            symbolState: KeyboardState.Symbol
        ): TouchHandler {
            if(symbolState != KeyboardState.Symbol.Text) {
                return super.createTouchHandler(keyboardView, params, symbolState)
            }

            return FlickTouchHandler(
                keyboardView,
                params,
                diagonal = false,
                multiFlick = false,
                sendOnUp = true
            )
        }

        override fun onKeyDown(keyCode: Int, metaState: Int): Boolean {
            if(symbolState != KeyboardState.Symbol.Text) {
                super.onKeyDown(keyCode, metaState)
                return false
            }

            if(keyCode <= 0 || !keyCharacterMap.isPrintingKey(keyCode)) {
                super.onKeyDown(keyCode, metaState)
            }
            return false
        }

        override fun onKeyUp(keyCode: Int, metaState: Int): Boolean {
            if(symbolState != KeyboardState.Symbol.Text) {
                super.onKeyUp(keyCode, metaState)
                return false
            }
            if(keyCode <= 0 || !keyCharacterMap.isPrintingKey(keyCode)) {
                super.onKeyUp(keyCode, metaState)
                return false
            }
            val sourceInput = LayoutCheonjiin.TABLE[keyCode]?.firstOrNull()
            if(sourceInput == null) {
                super.onKeyUp(keyCode, metaState)
                return false
            }
            val direction = flickDirections.remove(keyCode)
            if(direction == null) {
                onChar(sourceInput)
                return false
            }
            val consonantIndex =
                LayoutCheonjiin.CONSONANT_FLICK_INDICES[keyCode]?.get(direction)
            val vowel =
                LayoutCheonjiin.VOWEL_FLICK_OUTPUTS[keyCode]?.get(direction)
            val result = when {
                consonantIndex != null -> hangulCombiner.selectConsonant(
                    currentCombinerState,
                    sourceInput,
                    consonantIndex
                )
                vowel != null -> hangulCombiner.selectCompletedVowel(
                    currentCombinerState,
                    sourceInput,
                    vowel
                )
                else -> hangulCombiner.cancelFlick(
                    currentCombinerState,
                    sourceInput
                )
            }
            applyCombinerResult(result)
            updateInputView()

            return false
        }

        override fun onFlick(keyCode: Int, direction: FlickDirection): Boolean {
            flickDirections[keyCode] = direction
            return false
        }

        override fun onSpecial(keyCode: Int) {
            if(
                keyCode == KeyEvent.KEYCODE_SPACE &&
                hangulCombiner.canConfirmConsonantCycle(currentCombinerState)
            ) {
                onChar(CheonjiinComposer.CONFIRM_INPUT)
            } else {
                super.onSpecial(keyCode)
            }
        }
    }

    /*
     * Common part for 390 and 391
     */
    abstract class Hangul3Set390391(
        cursorKeys: Boolean,
        converterType: ConverterType,
        listener: IMEMode.Listener
    ): KoreanIMEMode(converterType, listener) {
        abstract val softKeyCodeMapper: SoftKeyCodeMapper
        abstract val layoutTable: LayoutTable
        override var textLayoutPreset: KeyboardLayoutPreset =
            KoreanLayoutPresets.threeSet390391(
                layoutTable,
                softKeyCodeMapper,
                cursorKeys
            )
    }

    class Hangul3Set390(
        correctOrders: Boolean,
        cursorKeys: Boolean,
        converterType: ConverterType,
        listener: IMEMode.Listener
    ): Hangul3Set390391(cursorKeys, converterType, listener) {
        override val hangulCombiner: HangulCombiner = HangulCombiner(Hangul3Set.COMBINATION_390_391, correctOrders)
        override val layoutTable: LayoutTable get() = LayoutTable.fromShiftStates(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + Hangul3Set.TABLE_390)
        override val softKeyCodeMapper: SoftKeyCodeMapper get() = SoftKeyCodeMapper.ByScreenMode(
            mobile = SoftKeyCodeMapper.Basic(Hangul3Set.KEYCODE_MAP_390_MOBILE),
            tablet = SoftKeyCodeMapper.Empty
        )
    }

    class Hangul3Set391(
        correctOrders: Boolean,
        cursorKeys: Boolean,
        converterType: ConverterType,
        listener: IMEMode.Listener
    ): Hangul3Set390391(cursorKeys, converterType, listener) {
        override val hangulCombiner: HangulCombiner = HangulCombiner(Hangul3Set.COMBINATION_390_391, correctOrders)
        override val layoutTable: LayoutTable get() = LayoutTable.fromShiftStates(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + Hangul3Set.TABLE_391)
        override val softKeyCodeMapper: SoftKeyCodeMapper get() = SoftKeyCodeMapper.Empty
    }

    class Hangul3Set391Strict(
        correctOrders: Boolean,
        cursorKeys: Boolean,
        converterType: ConverterType,
        listener: IMEMode.Listener
    ): Hangul3Set390391(cursorKeys, converterType, listener) {
        override val hangulCombiner: HangulCombiner = HangulCombiner(Hangul3Set.COMBINATION_391_STRICT, correctOrders)
        override val layoutTable: LayoutTable get() = LayoutTable.fromShiftStates(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + Hangul3Set.TABLE_391_STRICT)
        override val softKeyCodeMapper: SoftKeyCodeMapper get() = SoftKeyCodeMapper.Empty
    }

    class HangulOld2Set(
        correctOrders: Boolean,
        numberRow: Boolean,
        cursorKeys: Boolean,
        converterType: ConverterType,
        listener: IMEMode.Listener
    ): Hangul2SetKSCompatible(numberRow, cursorKeys, converterType, listener) {
        override val hangulCombiner: HangulCombiner = HangulCombiner(HangulOld.COMB_FULL, correctOrders)
        override val layoutTable: LayoutTable get() = LayoutTable.fromShiftStates(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + HangulOld.TABLE_OLD_2SET)
    }

    class HangulOld3Set393(
        correctOrders: Boolean,
        cursorKeys: Boolean,
        converterType: ConverterType,
        listener: IMEMode.Listener
    ): KoreanIMEMode(converterType, listener) {
        override val hangulCombiner: HangulCombiner = HangulCombiner(HangulOld.COMB_FULL, correctOrders)
        override var textLayoutPreset: KeyboardLayoutPreset = KoreanLayoutPresets.threeSet393(cursorKeys)
    }

    data class Params(
        val layout: Layout,
        val correctOrders: Boolean,
        val converterType: ConverterType,
        val numberRow: Boolean,
        val cursorKeys: Boolean
    ): IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): IMEMode {
            return when(layout) {
                Layout.Set2KS -> Hangul2SetKS(correctOrders, numberRow, cursorKeys, converterType, listener)
                Layout.Cheonjiin -> Cheonjiin(correctOrders, converterType, listener)
                Layout.Set3390 -> Hangul3Set390(correctOrders, cursorKeys, converterType, listener)
                Layout.Set3391 -> Hangul3Set391(correctOrders, cursorKeys, converterType, listener)
                Layout.Set3391Strict -> Hangul3Set391Strict(correctOrders, cursorKeys, converterType, listener)
                Layout.Set2Old -> HangulOld2Set(correctOrders, numberRow, cursorKeys, converterType, listener)
                Layout.Set3Old393 -> HangulOld3Set393(correctOrders, cursorKeys, converterType, listener)
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
                    Layout.Set2KS, Layout.Cheonjiin, Layout.Set3390, Layout.Set3391, Layout.Set3391Strict -> "한"
                    // For old Hangul layouts
                    Layout.Set2Old, Layout.Set3Old393 -> "ᄒᆞ"
                }
            }
            // If there are any other Korean modes
            return when(layout) {
                // For 2-set layouts
                Layout.Set2KS -> "한2"
                Layout.Cheonjiin -> "천"
                Layout.Set3390, Layout.Set3391, Layout.Set3391Strict -> {
                    // Find if there are any other 3-set layouts
                    val korean3SetParams = koreanParams.filter { it.layout in setOf(Layout.Set3390, Layout.Set3391, Layout.Set3391Strict) }
                    // If this is the only mode with 3-set layout
                    if(korean3SetParams.isEmpty()) "한3"
                    // If not, use specific layout name
                    else when(layout) {
                        Layout.Set3390 -> "390"
                        Layout.Set3391 -> "391"
                        Layout.Set3391Strict -> "391"
                    }
                }
                // For old Hangul layouts
                Layout.Set2Old, Layout.Set3Old393 -> {
                    val oldParams = koreanParams.filter { it.layout in setOf(Layout.Set2Old, Layout.Set3Old393) }
                    if(oldParams.isEmpty()) "ᄒᆞ"
                    else when(layout) {
                        Layout.Set2Old -> "ᄒᆞ2"
                        Layout.Set3Old393 -> "ᄒᆞ3"
                    }
                }
            }
        }

        companion object {
            fun parse(map: Map<String, String>): Params {
                val layout = Layout.entries.find { it.name == map["layout"] } ?: Layout.Set2KS
                val converterType = ConverterType.valueOf(map["converter"] ?: ConverterType.Word.name)
                val correctOrders = (map["correct_orders"] ?: "false").toBoolean()
                val numberRow = map["number_row"]?.toBoolean() ?: false
                val cursorKeys = map["cursor_keys"]?.toBoolean() ?: false
                return Params(
                    layout = layout,
                    converterType = converterType,
                    correctOrders = correctOrders,
                    numberRow = numberRow,
                    cursorKeys = cursorKeys
                )
            }
        }
    }

    enum class Layout(
        @StringRes val nameKey: Int
    ) {
        Set2KS(R.string.korean_layout_hangul_2set_ks),
        Cheonjiin(R.string.korean_layout_cheonjiin),
        Set3390(R.string.korean_layout_hangul_3set_390),
        Set3391(R.string.korean_layout_hangul_3set_391),
        Set3391Strict(R.string.korean_layout_hangul_3set_391_strict),
        Set2Old(R.string.korean_layout_old_hangul_2set_ks),
        Set3Old393(R.string.korean_layout_old_hangul_3set_393)
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
        const val TYPE: String = "korean"
    }
}
