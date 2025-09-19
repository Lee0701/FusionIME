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
import ee.oyatl.ime.keyboard.KeyboardConfiguration
import ee.oyatl.ime.keyboard.LayoutTable
import ee.oyatl.ime.keyboard.layout.ExtKeyCode
import ee.oyatl.ime.keyboard.layout.KeyboardConfigurations
import ee.oyatl.ime.keyboard.layout.KeyboardTemplates
import ee.oyatl.ime.keyboard.layout.LayoutCangjie
import ee.oyatl.ime.keyboard.layout.LayoutExt
import ee.oyatl.ime.keyboard.layout.LayoutQwerty
import java.util.Locale
import kotlin.collections.plus

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

    override val layoutTable: LayoutTable = LayoutTable.from(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + LayoutExt.TABLE_CHINESE + LayoutCangjie.TABLE_QWERTY)
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

    override fun onChar(codePoint: Int) {
        wordComposer.add(codePoint, intArrayOf(codePoint))
        table?.setInputMethod(TableLoader.CANGJIE)
        renderInput()
    }

    override fun onSpecial(keyCode: Int) {
        when(keyCode) {
            KeyEvent.KEYCODE_SPACE -> {
                if(wordComposer.typedWord?.isNotEmpty() == true) {
                    val bestCandidate = bestCandidate
                    if(bestCandidate != null) onCandidateSelected(bestCandidate)
                } else {
                    if(fullWidth) util?.sendKeyChar(0x3000.toChar())
                    else util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_SPACE)
                }
                onReset()
            }
            KeyEvent.KEYCODE_ENTER -> {
                if(wordComposer.typedWord?.isNotEmpty() == true) onReset()
                else {
                    if (util?.sendDefaultEditorAction(true) != true)
                        currentInputConnection?.commitText("\n", 1)
                    onReset()
                }
            }
            KeyEvent.KEYCODE_DEL -> {
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

    class Cangjie(
        override val fullWidth: Boolean,
        listener: IMEMode.Listener
    ): CangjieIMEMode(listener) {
        override val inputMode: Int = TableLoader.CANGJIE
        override val keyboardTemplate: List<String> = KeyboardTemplates.MOBILE
        override val keyMap: Map<Char, Char> = LayoutCangjie.KEY_MAP_CANGJIE
    }

    class Quick(
        override val fullWidth: Boolean,
        listener: IMEMode.Listener
    ): CangjieIMEMode(listener) {
        override val inputMode: Int = TableLoader.QUICK
        override val keyboardTemplate: List<String> = KeyboardTemplates.MOBILE
        override val keyMap: Map<Char, Char> = LayoutCangjie.KEY_MAP_CANGJIE
    }

    class Dayi3(
        override val fullWidth: Boolean,
        listener: IMEMode.Listener
    ): CangjieIMEMode(listener) {
        override val inputMode: Int = TableLoader.DAYI3
        override val keyboardConfiguration: KeyboardConfiguration = KeyboardConfiguration(
            KeyboardConfigurations.mobileNumbers(),
            KeyboardConfigurations.mobileAlpha(semicolon = true, shiftDeleteWidth = 1f, shift = false),
            KeyboardConfigurations.mobileBottom(ExtKeyCode.KEYCODE_PERIOD_COMMA, KeyEvent.KEYCODE_SLASH)
        )
        override val keyboardTemplate: List<String> =
            KeyboardTemplates.MOBILE_NUMBERS + KeyboardTemplates.MOBILE_HALF_GRID
        override val layoutTable: LayoutTable = LayoutTable.from(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + LayoutExt.TABLE_CHINESE + LayoutCangjie.TABLE_DAYI3)
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