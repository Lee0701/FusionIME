package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.annotation.StringRes
import com.android.inputmethod.zhuyin.WordComposer
import com.diycircuits.cangjie.TableLoader
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.keyboard.CustomRowKeyboard
import ee.oyatl.ime.keyboard.DefaultBottomRowKeyboard
import ee.oyatl.ime.keyboard.DefaultGridKeyboard
import ee.oyatl.ime.keyboard.DefaultMobileKeyboard
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.KeyboardInflater
import ee.oyatl.ime.keyboard.ShiftStateKeyboard
import ee.oyatl.ime.keyboard.StackedKeyboard
import ee.oyatl.ime.keyboard.layout.KeyboardTemplates
import ee.oyatl.ime.keyboard.layout.LayoutCangjie
import ee.oyatl.ime.keyboard.listener.OnKeyClickListener
import java.util.Locale

abstract class CangjieIMEMode(
    listener: IMEMode.Listener
): CommonIMEMode(listener) {
    abstract val inputMode: Int
    abstract val fullWidth: Boolean

    private val handler: Handler = Handler(Looper.getMainLooper()) { msg ->
        when(msg.what) {
            MSG_UPDATE_SUGGESTIONS -> {
                updateSuggestions()
                true
            }
            else -> false
        }
    }

    override val symbolKeyListener: OnKeyClickListener = DirectKeyListener()

    override val layoutTable: Map<Int, List<Int>> = LayoutCangjie.TABLE_QWERTY
    abstract val keyboardTemplate: List<String>
    abstract val keyMap: Map<Char, Char>

    private var table: TableLoader? = null
    private val wordComposer = WordComposer()

    private var bestCandidate: CangjieCandidate? = null

    override suspend fun onLoad(context: Context) {
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
        onReset()
    }

    private fun updateSuggestions() {
        val table = table ?: return
        table.setInputMethod(inputMode)
        val chars = (wordComposer.typedWord?.toString().orEmpty()
            .map { keyMap[it] ?: it }.toCharArray() +
                (0 until 5).map { 0.toChar() }).take(5)
        val (c0, c1, c2, c3, c4) = chars
        table.searchCangjie(c0, c1, c2, c3, c4)
        val candidates = (0 until table.totalMatch())
            .map { CangjieCandidate(table.getMatchChar(it).toString()) }
        submitCandidates(candidates)
        bestCandidate = candidates.firstOrNull()
    }

    private fun postUpdateSuggestions() {
        handler.removeMessages(MSG_UPDATE_SUGGESTIONS)
        handler.sendMessageDelayed(handler.obtainMessage(MSG_UPDATE_SUGGESTIONS), 100)
    }

    private fun renderInput() {
        currentInputConnection?.setComposingText(wordComposer.typedWord?.toString().orEmpty(), 1)
        postUpdateSuggestions()
    }

    override fun onChar(code: Int) {
        wordComposer.add(code, intArrayOf(code))
        table?.setInputMethod(TableLoader.CANGJIE)
        renderInput()
    }

    override fun onSpecial(type: Keyboard.SpecialKey) {
        when(type) {
            Keyboard.SpecialKey.Space -> {
                if(wordComposer.typedWord?.isNotEmpty() == true) {
                    val bestCandidate = bestCandidate
                    if(bestCandidate != null) onCandidateSelected(bestCandidate)
                } else {
                    if(fullWidth) util?.sendKeyChar(0x3000.toChar())
                    else util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_SPACE)
                }
                onReset()
            }
            Keyboard.SpecialKey.Return -> {
                if(wordComposer.typedWord?.isNotEmpty() == true) onReset()
                else {
                    if (util?.sendDefaultEditorAction(true) != true)
                        currentInputConnection?.commitText("\n", 1)
                    onReset()
                }
            }
            Keyboard.SpecialKey.Delete -> {
                if(wordComposer.typedWord?.isNotEmpty() == true) {
                    wordComposer.deleteLast()
                } else {
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                }
            }
            else -> {}
        }
        renderInput()
    }

    data class CangjieCandidate(
        override val text: CharSequence
    ): CandidateView.Candidate

    inner class DirectKeyListener: OnKeyClickListener {
        override fun onKeyClick(code: Int) {
            val special = Keyboard.SpecialKey.ofCode(code)
            if(special != null) handleSpecialKey(special)
            else {
                onReset()
                val fullWidthChar = when(code) {
                    in 0x21 .. 0x7e -> (code - 0x20 + 0xff00).toChar()
                    else -> code.toChar()
                }
                val halfWidthChar = code.toChar()
                util?.sendKeyChar(if(fullWidth) fullWidthChar else halfWidthChar)
            }
            updateInputView()
        }
    }

    class Cangjie(
        override val fullWidth: Boolean,
        listener: IMEMode.Listener
    ): CangjieIMEMode(listener) {
        override val inputMode: Int = TableLoader.CANGJIE
        override val keyboardTemplate: List<String> = KeyboardTemplates.MOBILE
        private val textKeyboardLayers = KeyboardInflater.inflate(keyboardTemplate, layoutTable)
        override val textKeyboard: Keyboard = StackedKeyboard(
            ShiftStateKeyboard(
                DefaultMobileKeyboard(textKeyboardLayers[0]),
                DefaultMobileKeyboard(textKeyboardLayers[1])
            ),
            DefaultBottomRowKeyboard(listOf('，'.code, '。'.code))
        )
        override val keyMap: Map<Char, Char> = LayoutCangjie.KEY_MAP_CANGJIE
    }

    class Quick(
        override val fullWidth: Boolean,
        listener: IMEMode.Listener
    ): CangjieIMEMode(listener) {
        override val inputMode: Int = TableLoader.QUICK
        override val keyboardTemplate: List<String> = KeyboardTemplates.MOBILE
        private val textKeyboardLayers = KeyboardInflater.inflate(keyboardTemplate, layoutTable)
        override val textKeyboard: Keyboard = StackedKeyboard(
            ShiftStateKeyboard(
                DefaultMobileKeyboard(textKeyboardLayers[0]),
                DefaultMobileKeyboard(textKeyboardLayers[1])
            ),
            DefaultBottomRowKeyboard(listOf('，'.code, '。'.code))
        )
        override val keyMap: Map<Char, Char> = LayoutCangjie.KEY_MAP_CANGJIE
    }

    class Dayi3(
        override val fullWidth: Boolean,
        listener: IMEMode.Listener
    ): CangjieIMEMode(listener) {
        override val inputMode: Int = TableLoader.DAYI3
        override val keyboardTemplate: List<String> = KeyboardTemplates.MOBILE
        override val textKeyboard: Keyboard = StackedKeyboard(
            DefaultGridKeyboard(LayoutCangjie.ROWS_DAYI3.map { row -> row.map { it.code } }),
            CustomRowKeyboard(listOf(
                CustomRowKeyboard.KeyType.Symbols(width = 1.5f),
                CustomRowKeyboard.KeyType.Extra('，'.code),
                CustomRowKeyboard.KeyType.Language(width = 1f),
                CustomRowKeyboard.KeyType.Space(width = 3f),
                CustomRowKeyboard.KeyType.Extra('。'.code),
                CustomRowKeyboard.KeyType.Return(width = 1.5f),
                CustomRowKeyboard.KeyType.Delete(width = 1f)
            ))
        )
        override val keyMap: Map<Char, Char> = LayoutCangjie.KEY_MAP_DAYI3
    }

    data class Params(
        val layout: Layout,
        val fullWidth: Boolean
    ): IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): IMEMode {
            return when(layout) {
                Layout.Cangjie -> Cangjie(fullWidth, listener)
                Layout.Quick -> Quick(fullWidth, listener)
                Layout.Dayi3 -> Dayi3(fullWidth, listener)
            }
        }

        override fun getLabel(context: Context): String {
            val localeName = Locale.TRADITIONAL_CHINESE.displayName
            val layoutName = context.resources.getString(layout.nameKey)
            return "$localeName $layoutName"
        }

        override fun getShortLabel(context: Context): String {
            return when(layout) {
                Layout.Cangjie -> "倉頡"
                Layout.Quick -> "速成"
                Layout.Dayi3 -> "大易"
            }
        }

        companion object {
            fun parse(map: Map<String, String>): Params {
                val layout = Layout.valueOf(map["layout"] ?: Layout.Cangjie.name)
                val fullWidth = map["full_width"].toBoolean()
                return Params(
                    layout = layout,
                    fullWidth = fullWidth
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
        const val MSG_UPDATE_SUGGESTIONS = 0
    }
}