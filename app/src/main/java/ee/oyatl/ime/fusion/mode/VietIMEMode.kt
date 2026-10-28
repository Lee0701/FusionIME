package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.view.KeyEvent
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.korean.WordComposer
import ee.oyatl.ime.fusion.layout.preset.VietLayoutPresets
import ee.oyatl.ime.keyboard.KeyboardLayoutPreset
import ee.oyatl.ime.viet.VietnameseConverter
import net.sourceforge.vietpad.inputmethod.InputMethod
import net.sourceforge.vietpad.inputmethod.KeyboardMode
import net.sourceforge.vietpad.inputmethod.VietKeyInput
import java.util.Locale

class VietIMEMode(
    listener: IMEMode.Listener,
    numberRow: Boolean,
    cursorKeys: Boolean,
    val layout: Layout
): CommonIMEMode(listener) {
    private val wordComposer: WordComposer = WordComposer()
    private var converter: VietnameseConverter? = null
    private val inputMethod: InputMethod = layout.keyboardMode.createInputMethod()

    private val forcedNumberRow = layout.keyboardMode == KeyboardMode.VNI
    override var textLayoutPreset: KeyboardLayoutPreset = VietLayoutPresets.qwerty(forcedNumberRow || numberRow, cursorKeys, layout.nomMode)

    private var bestCandidate: VietnameseConverter.Candidate? = null

    override suspend fun onLoad(context: Context) {
        super.onLoad(context)
        if(layout.dictResId != 0) {
            converter = VietnameseConverter(context, layout.dictResId)
        }
    }

    override fun onReset() {
        super.onReset()
        wordComposer.reset()
        bestCandidate = null
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        val inputConnection = currentInputConnection ?: return
        if(candidate is VietnameseConverter.Candidate) {
            wordComposer.consume(candidate.src.length)
            wordComposer.moveCursor(wordComposer.composingText.length)
            inputConnection.commitText(candidate.text, 1)
            if(layout.insertSpaces && wordComposer.composingText.isNotEmpty()) {
                inputConnection.commitText(" ", 1)
            }
            renderInputView()
        }
    }

    private fun convert() {
        val text = wordComposer.textBeforeCursor
        val candidates = converter?.convert(text) ?: return
        bestCandidate = candidates.firstOrNull() as? VietnameseConverter.Candidate
        submitCandidates(candidates)
    }

    private fun renderInputView() {
        val composing = wordComposer.getSpannableSurfaceString()
        currentInputConnection?.setComposingText(composing, 1)
        convert()
    }

    override fun onChar(codePoint: Int) {
        wordComposer.commit(codePoint.toChar().toString())
        val target = wordComposer.textBeforeCursor.split(' ').lastOrNull()
        if(target != null && target.length >= 2) {
            val keyChar = codePoint.toChar()
            val curChar = target.last()
            val curWord = target.dropLast(1)
            val vietnameseWord = keyTyped(keyChar, curChar, curWord)
            val newComposingText = wordComposer.textBeforeCursor.dropLast(target.length) + vietnameseWord
            wordComposer.replace(wordComposer.textBeforeCursor.length, newComposingText)
        }
        renderInputView()
    }

    override fun onSpecial(keyCode: Int) {
        when(keyCode) {
            KeyEvent.KEYCODE_SPACE -> {
                if(wordComposer.textBeforeCursor.lastOrNull() == ' ') {
                    val bestCandidate = bestCandidate
                    if(bestCandidate != null) {
                        onCandidateSelected(bestCandidate)
                    } else {
                        onReset()
                    }
                } else if(wordComposer.textBeforeCursor.isNotEmpty()) {
                    wordComposer.commit(" ")
                    renderInputView()
                } else {
                    onReset()
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_SPACE)
                }
            }
            KeyEvent.KEYCODE_ENTER -> {
                val send = wordComposer.composingText.isEmpty()
                onReset()
                if(send) {
                    if(util?.sendDefaultEditorAction(true) != true)
                        util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                }
            }
            KeyEvent.KEYCODE_DEL -> {
                if(wordComposer.composingText.isNotEmpty()) {
                    wordComposer.delete(1)
                    renderInputView()
                } else {
                    onReset()
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                }
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if(wordComposer.composingText.isNotEmpty()) {
                    wordComposer.moveCursorRelative(-1)
                    renderInputView()
                } else {
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT)
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
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

    private fun keyTyped(keyChar: Char, curChar: Char, curWord: String): String {
        val accent = inputMethod.getAccentMark(keyChar, curChar, curWord)
        if(accent.isDigit()) {
            val vietWord = VietKeyInput.toVietWord(curWord, accent)
            if(vietWord != curWord) return vietWord
        }
        return curWord + keyChar
    }

    data class Params(
        val layout: Layout,
        val numberRow: Boolean,
        val cursorKeys: Boolean
    ): IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): IMEMode {
            return VietIMEMode(listener, numberRow, cursorKeys, layout)
        }

        override fun getLabel(context: Context): String {
            val localeName = Locale.Builder().setLanguage("vi").build().displayName
            val layoutName = context.getString(layout.nameKey)
            return "$localeName $layoutName"
        }

        override fun getShortLabels(context: Context): List<String> {
            val localeHead = layout.labelHead
            val layoutHead = layout.name.first()
            return listOf(localeHead, "$localeHead$layoutHead")
        }

        companion object {
            fun parse(map: Map<String, String>): Params {
                val layout = Layout.entries.find { it.name == map["layout"] } ?: Layout.QwertyNom
                val numberRow = map["number_row"]?.toBoolean() ?: false
                val cursorKeys = map["cursor_keys"]?.toBoolean() ?: false
                return Params(
                    layout = layout,
                    numberRow = numberRow,
                    cursorKeys = cursorKeys
                )
            }
        }
    }

    enum class Layout(
        @StringRes val nameKey: Int,
        val labelHead: String,
        val keyboardMode: KeyboardMode,
        val nomMode: Boolean,
        @RawRes val dictResId: Int,
        val insertSpaces: Boolean,
    ) {
        QwertyNom(
            R.string.viet_layout_qwerty_nom,
            "越",
            KeyboardMode.Qwerty,
            true,
            ee.oyatl.ime.viet.R.raw.nom_qwerty,
            false
        ),
        QwertyQuocNgu(
            R.string.viet_layout_qwerty_quoc_ngu,
            "Việt",
            KeyboardMode.Qwerty,
            false,
            ee.oyatl.ime.viet.R.raw.quoc_ngu_qwerty,
            true
        ),
        TelexNom(
            R.string.viet_layout_telex_nom,
            "越",
            KeyboardMode.Telex,
            true,
            ee.oyatl.ime.viet.R.raw.nom_quoc_ngu,
            false
        ),
        TelexQuocNgu(
            R.string.viet_layout_telex_quoc_ngu,
            "Việt",
            KeyboardMode.Telex,
            false,
            0,
            true
        ),
        VNINom(
            R.string.viet_layout_telex_nom,
            "越",
            KeyboardMode.VNI,
            true,
            ee.oyatl.ime.viet.R.raw.nom_quoc_ngu,
            false
        ),
        VNIQuocNgu(
            R.string.viet_layout_telex_quoc_ngu,
            "Việt",
            KeyboardMode.VNI,
            false,
            0,
            true
        )
    }

    companion object {
        const val TYPE: String = "viet"
    }
}