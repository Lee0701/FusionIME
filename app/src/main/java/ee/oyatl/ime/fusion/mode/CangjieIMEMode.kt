package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.view.KeyEvent
import androidx.annotation.StringRes
import com.diycircuits.cangjie.TableLoader
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.AssetsPresetLoader
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.keyboard.core.KeyboardState
import ee.oyatl.ime.fusion.korean.WordComposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

abstract class CangjieIMEMode(
    listener: IMEMode.Listener,
    val presetName: String,
    override val extra: Map<String, String>
): CommonIMEMode(listener) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var convertJob: Job? = null

    abstract val inputMode: Int
    abstract val fullWidth: Boolean

    abstract val keyMap: Map<Char, Char>

    private var table: TableLoader? = null
    private val wordComposer = WordComposer()

    private var bestCandidate: CangjieCandidate? = null

    override fun onLoad(context: Context) {
        super.onLoad(context)
        val loader = AssetsPresetLoader(context.assets, "keyboard/cangjie")
        textLayoutPreset = loader.inflate(loader.load(presetName))
        val table = TableLoader()
        table.setPath(context.filesDir.absolutePath.encodeToByteArray())
        table.initialize()
        this.table = table
    }

    override fun onReset() {
        super.onReset()
        wordComposer.reset()
        bestCandidate = null
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        val inputConnection = currentInputConnection ?: return
        inputConnection.commitText(candidate.text, 1)
        inputConnection.setComposingText("", 1)
        if(candidate is CangjieCandidate) {
            wordComposer.consume(candidate.key.length)
        } else {
            wordComposer.consume(candidate.text.length)
        }
        wordComposer.moveCursor(wordComposer.composingText.length)
        renderInput()
    }

    private fun postUpdateSuggestions() {
        convertJob?.cancel()
        convertJob = coroutineScope.launch {
            val table = table ?: return@launch
            table.setInputMethod(inputMode)
            val key = wordComposer.textBeforeCursor
            val zeros = (0 until 5).map { 0.toChar() }
            val chars = (key.map { keyMap[it] ?: it }.toCharArray() + zeros).take(5)
            val (c0, c1, c2, c3, c4) = chars
            table.searchCangjie(c0, c1, c2, c3, c4)
            val candidates = (0 until table.totalMatch())
                .map { CangjieCandidate(table.getMatchChar(it).toString(), key) }
            submitCandidates(candidates)
            bestCandidate = candidates.firstOrNull()
        }
    }

    private fun renderInput() {
        currentInputConnection?.setComposingText(wordComposer.getSpannableSurfaceString(), 1)
        postUpdateSuggestions()
    }

    override fun onChar(codePoint: Int) {
        val char = getFullOrHalfWidthChar(codePoint)
        wordComposer.commit(char.toChar().toString())
        table?.setInputMethod(TableLoader.CANGJIE)
        renderInput()
    }

    override fun onSpecial(keyCode: Int) {
        when(keyCode) {
            KeyEvent.KEYCODE_SPACE -> {
                if(wordComposer.composingText.isNotEmpty()) {
                    coroutineScope.launch {
                        postUpdateSuggestions()
                        convertJob?.join()
                        val bestCandidate = bestCandidate
                        if(bestCandidate != null) onCandidateSelected(bestCandidate)
                        else onReset()
                    }
                } else {
                    onReset()
                    if(fullWidth) util?.sendKeyChar(0x3000.toChar())
                    else util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_SPACE)
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
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if(wordComposer.composingText.isNotEmpty()) {
                    wordComposer.moveCursorRelative(-1)
                    renderInput()
                } else {
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT)
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if(wordComposer.composingText.isNotEmpty()) {
                    wordComposer.moveCursorRelative(1)
                    renderInput()
                } else {
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT)
                }
            }
            else -> super.onSpecial(keyCode)
        }
        renderInput()
    }

    private fun getFullOrHalfWidthChar(codePoint: Int): Int {
        return if(fullWidth && symbolState == KeyboardState.Symbol.Symbol) getFullWidthChar(codePoint)
        else codePoint
    }

    private fun getFullWidthChar(codePoint: Int): Int {
        return if(codePoint in 0x21 .. 0x5f)
            codePoint - 0x21 + 0xff01
        else codePoint
    }

    data class CangjieCandidate(
        override val text: CharSequence,
        val key: CharSequence
    ): CandidateView.Candidate

    class CangjieQuick(
        override val fullWidth: Boolean,
        override val inputMode: Int,
        extra: Map<String, String>,
        listener: IMEMode.Listener
    ): CangjieIMEMode(listener, "cangjie", extra) {
        override val keyMap: Map<Char, Char> = KEY_MAP_CANGJIE
    }

    class Dayi3(
        override val fullWidth: Boolean,
        extra: Map<String, String>,
        listener: IMEMode.Listener
    ): CangjieIMEMode(listener, "dayi3", extra) {
        override val inputMode: Int = TableLoader.DAYI3
        override val keyMap: Map<Char, Char> = KEY_MAP_DAYI3

        override fun onLoad(context: Context) {
            super.onLoad(context)
            val symbolLoader = AssetsPresetLoader(context.assets, "keyboard/symbol")
            val chineseSymbols = symbolLoader.inflate(symbolLoader.load("symbol_chinese"))
            symbolLayoutPreset = super.symbolLayoutPreset.copy(
                layoutTable = super.symbolLayoutPreset.layoutTable + chineseSymbols.layoutTable
            )
        }
    }

    data class Params(
        val layout: Layout,
        val fullWidth: Boolean,
        val extra: Map<String, String>
    ): IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): IMEMode {
            return when(layout) {
                Layout.Cangjie -> CangjieQuick(fullWidth, TableLoader.CANGJIE, extra, listener)
                Layout.Quick -> CangjieQuick(fullWidth, TableLoader.QUICK, extra, listener)
                Layout.Dayi3 -> Dayi3(fullWidth, extra, listener)
            }
        }

        override fun getLabel(context: Context): String {
            val localeName = Locale.TRADITIONAL_CHINESE.displayName
            val layoutName = context.resources.getString(layout.nameKey)
            return "$localeName $layoutName"
        }

        override fun getShortLabels(context: Context): List<String> {
            return when(layout) {
                Layout.Cangjie -> listOf("倉頡")
                Layout.Quick -> listOf("速成")
                Layout.Dayi3 -> listOf("大易")
            }
        }

        companion object {
            fun parse(map: Map<String, String>): Params {
                val layout = Layout.entries.find { it.name == map["layout"] } ?: Layout.Cangjie
                val fullWidth = map["full_width"].toBoolean()
                return Params(
                    layout = layout,
                    fullWidth = fullWidth,
                    extra = map
                )
            }
        }
    }

    enum class Layout(
        @StringRes val nameKey: Int
    ) {
        Cangjie(R.string.cangjie_layout_cangjie),
        Quick(R.string.cangjie_layout_quick),
        Dayi3(R.string.cangjie_layout_dayi3)
    }

    companion object {
        const val TYPE: String = "cangjie"

        val KEY_MAP_CANGJIE = mapOf(
            '日' to 'a',
            '月' to 'b',
            '金' to 'c',
            '木' to 'd',
            '水' to 'e',
            '火' to 'f',
            '土' to 'g',
            '竹' to 'h',
            '戈' to 'i',
            '十' to 'j',
            '大' to 'k',
            '中' to 'l',
            '一' to 'm',
            '弓' to 'n',
            '人' to 'o',
            '心' to 'p',
            '手' to 'q',
            '口' to 'r',
            '尸' to 's',
            '廿' to 't',
            '山' to 'u',
            '女' to 'v',
            '田' to 'w',
            '難' to 'x',
            '卜' to 'y',
            '重' to 'z'
        )

        val KEY_MAP_DAYI3 = mapOf(
            '巷' to '`',
            '言' to '1',
            '牛' to '2',
            '目' to '3',
            '四' to '4',
            '王' to '5',
            '門' to '6',
            '田' to '7',
            '米' to '8',
            '足' to '9',
            '金' to '0',
            '郷' to '-',
            '石' to 'Q',
            '山' to 'W',
            '一' to 'E',
            '工' to 'R',
            '糸' to 'T',
            '火' to 'Y',
            '艸' to 'U',
            '木' to 'I',
            '口' to 'O',
            '耳' to 'P',
            '路' to '[',
            '街' to ']',
            '鎮' to '\\',
            '人' to 'A',
            '革' to 'S',
            '日' to 'D',
            '土' to 'F',
            '手' to 'G',
            '鳥' to 'H',
            '月' to 'J',
            '立' to 'K',
            '女' to 'L',
            '虫' to ';',
            '號' to '\'',
            '心' to 'Z',
            '水' to 'X',
            '鹿' to 'C',
            '禾' to 'V',
            '馬' to 'B',
            '魚' to 'N',
            '雨' to 'M',
            '力' to ',',
            '舟' to '.',
            '竹' to '/'
        )
    }
}