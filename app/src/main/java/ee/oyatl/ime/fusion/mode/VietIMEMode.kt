package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.view.KeyEvent
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.korean.WordComposer
import ee.oyatl.ime.fusion.layout.preset.LatinLayoutPresets
import ee.oyatl.ime.keyboard.KeyboardLayoutPreset
import ee.oyatl.ime.viet.ChuQuocNguTableConverter
import ee.oyatl.ime.viet.VietnameseConverter
import java.util.Locale

class VietIMEMode(
    listener: IMEMode.Listener,
    numberRow: Boolean,
    cursorKeys: Boolean,
    val layout: Layout
): CommonIMEMode(listener) {
    private val wordComposer: WordComposer = WordComposer()
    private var converter: VietnameseConverter? = null
    private val chuQuocNguTableConverter: ChuQuocNguTableConverter = ChuQuocNguTableConverter()

    override var textLayoutPreset: KeyboardLayoutPreset = LatinLayoutPresets.qwerty(false, numberRow, cursorKeys)

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
            wordComposer.consume(candidate.src.keys.take(candidate.keyLength).sumOf { it.length })
            inputConnection.commitText(candidate.text, 1)
            if(layout.insertSpaces && wordComposer.composingText.isNotEmpty()) {
                inputConnection.commitText(" ", 1)
            }
            renderInputView()
        }
    }

    private fun convert() {
        val result = chuQuocNguTableConverter.convert(wordComposer.composingText, layout.keyboardMode)
        val candidates = converter?.convert(result) ?: return
        bestCandidate = candidates.firstOrNull() as? VietnameseConverter.Candidate
        submitCandidates(candidates)
    }

    private fun renderInputView() {
        val result = chuQuocNguTableConverter.convert(wordComposer.composingText, layout.keyboardMode)
        val composing = result.values.joinToString("")
        currentInputConnection?.setComposingText(composing, 1)
        convert()
    }

    override fun onChar(codePoint: Int) {
        wordComposer.commit(codePoint.toChar().toString())
        renderInputView()
    }

    override fun onSpecial(keyCode: Int) {
        when(keyCode) {
            KeyEvent.KEYCODE_SPACE -> {
                val bestCandidate = bestCandidate
                if(bestCandidate != null) {
                    onCandidateSelected(bestCandidate)
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
            val localeName = Locale("vi").displayName
            val layoutName = context.getString(layout.nameKey)
            return "$localeName $layoutName"
        }

        override fun getShortLabel(context: Context, params: List<IMEMode.Params>): String {
            val vietParams = params.filterIsInstance<Params>().filterNot { it == this }
            val localeHead = layout.labelHead
            // If this is the only Vietnamese mode
            if(vietParams.isEmpty()) return localeHead
            // If not, use specific layout name
            val layoutHead = layout.name.first()
            return "$localeHead$layoutHead"
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
        val keyboardMode: String,
        @RawRes val dictResId: Int,
        val insertSpaces: Boolean
    ) {
        QwertyNom(
            R.string.viet_layout_qwerty_nom,
            "越",
            "q",
            ee.oyatl.ime.viet.R.raw.nom_qwerty,
            false
        ),
        QwertyQuocNgu(
            R.string.viet_layout_qwerty_quoc_ngu,
            "Việt",
            "q",
            ee.oyatl.ime.viet.R.raw.quoc_ngu_qwerty,
            true
        ),
        TelexNom(
            R.string.viet_layout_telex_nom,
            "越",
            "t",
            ee.oyatl.ime.viet.R.raw.nom_quoc_ngu,
            false
        ),
        TelexQuocNgu(
            R.string.viet_layout_telex_quoc_ngu,
            "Việt",
            "t",
            0,
            true
        )
    }

    companion object {
        const val TYPE: String = "viet"
    }
}