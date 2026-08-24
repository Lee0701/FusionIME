package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.view.KeyEvent
import androidx.annotation.StringRes
import com.diycircuits.cangjie.TableLoader
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.keyboard.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.korean.WordComposer
import ee.oyatl.ime.keyboard.KeyboardState
import ee.oyatl.ime.fusion.layout.LayoutCangjie
import ee.oyatl.ime.fusion.layout.preset.CangjieLayoutPresets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.collections.plus

abstract class CangjieIMEMode(
    listener: IMEMode.Listener
): CommonIMEMode(listener) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var convertJob: Job? = null

    abstract val inputMode: Int
    abstract val fullWidth: Boolean

    abstract val keyMap: Map<Char, Char>

    private var table: TableLoader? = null
    private val wordComposer = WordComposer()

    private var bestCandidate: CangjieCandidate? = null

    override suspend fun onLoad(context: Context) {
        super.onLoad(context)
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
        numberRow: Boolean,
        cursorKeys: Boolean,
        listener: IMEMode.Listener
    ): CangjieIMEMode(listener) {
        override val keyMap: Map<Char, Char> = LayoutCangjie.KEY_MAP_CANGJIE
        override var textLayoutPreset: KeyboardLayoutPreset = CangjieLayoutPresets.cangjie(numberRow, cursorKeys)
    }

    class Dayi3(
        override val fullWidth: Boolean,
        cursorKeys: Boolean,
        listener: IMEMode.Listener
    ): CangjieIMEMode(listener) {
        override val inputMode: Int = TableLoader.DAYI3
        override var textLayoutPreset: KeyboardLayoutPreset = CangjieLayoutPresets.dayi3(cursorKeys)
        override val keyMap: Map<Char, Char> = LayoutCangjie.KEY_MAP_DAYI3
    }

    data class Params(
        val layout: Layout,
        val fullWidth: Boolean,
        val numberRow: Boolean,
        val cursorKeys: Boolean
    ): IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): IMEMode {
            return when(layout) {
                Layout.Cangjie -> CangjieQuick(fullWidth, TableLoader.CANGJIE, numberRow, cursorKeys, listener)
                Layout.Quick -> CangjieQuick(fullWidth, TableLoader.QUICK, numberRow, cursorKeys, listener)
                Layout.Dayi3 -> Dayi3(fullWidth, cursorKeys, listener)
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
                val numberRow = map["number_row"]?.toBoolean() ?: false
                val cursorKeys = map["cursor_keys"]?.toBoolean() ?: false
                return Params(
                    layout = layout,
                    fullWidth = fullWidth,
                    numberRow = numberRow,
                    cursorKeys = cursorKeys
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
    }
}