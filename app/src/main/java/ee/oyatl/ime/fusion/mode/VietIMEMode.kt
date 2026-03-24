package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.view.KeyEvent
import androidx.annotation.StringRes
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.korean.WordComposer
import ee.oyatl.ime.keyboard.KeyboardConfiguration
import ee.oyatl.ime.keyboard.KeyboardTemplate
import ee.oyatl.ime.keyboard.layout.MobileKeyboard
import ee.oyatl.ime.keyboard.layout.MobileKeyboardRows
import ee.oyatl.ime.keyboard.layout.TabletKeyboard
import ee.oyatl.ime.keyboard.layout.TabletKeyboardRows
import ee.oyatl.ime.viet.ChuQuocNguTableConverter
import ee.oyatl.ime.viet.HanNomConverter
import java.util.Locale

abstract class VietIMEMode(
    listener: IMEMode.Listener,
    numberRow: Boolean,
): CommonIMEMode(listener) {

    override val textKeyboardTemplate: KeyboardTemplate = KeyboardTemplate.ByScreenMode(
        mobile = KeyboardTemplate.Basic(
            configuration = KeyboardConfiguration(
                if(numberRow) MobileKeyboard.numbers() else KeyboardConfiguration(),
                MobileKeyboard.alphabetic(),
                MobileKeyboard.bottom()
            ),
            contentRows = (if(numberRow) MobileKeyboardRows.NUMBERS else listOf()) + MobileKeyboardRows.DEFAULT
        ),
        tablet = KeyboardTemplate.Basic(
            configuration = KeyboardConfiguration(
                if(numberRow) TabletKeyboard.numbers(delete = true) else KeyboardConfiguration(),
                TabletKeyboard.alphabetic(delete = !numberRow),
                TabletKeyboard.bottom()
            ),
            contentRows = (if(numberRow) TabletKeyboardRows.NUMBERS else listOf()) + TabletKeyboardRows.DEFAULT
        )
    )

    class Qwerty(
        listener: IMEMode.Listener,
        numberRow: Boolean
    ): VietIMEMode(listener, numberRow) {
        override val keyboardMode: String = "q"
    }

    class Telex(
        listener: IMEMode.Listener,
        numberRow: Boolean
    ): VietIMEMode(listener, numberRow) {
        override val keyboardMode: String = "t"
    }

    abstract val keyboardMode: String

    private val wordComposer: WordComposer = WordComposer()
    private var hanNomConverter: HanNomConverter? = null
    private val chuQuocNguTableConverter: ChuQuocNguTableConverter = ChuQuocNguTableConverter()

    private var bestCandidate: HanNomConverter.Candidate? = null

    override suspend fun onLoad(context: Context) {
        hanNomConverter = HanNomConverter(context)
    }

    override fun onReset() {
        super.onReset()
        wordComposer.reset()
        bestCandidate = null
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        val inputConnection = currentInputConnection ?: return
        if(candidate is HanNomConverter.Candidate) {
            wordComposer.consume(candidate.key.length)
            inputConnection.commitText(candidate.text, 1)
            renderInputView()
        }
    }

    private fun convert() {
        val candidates = hanNomConverter?.convert(wordComposer.composingText, keyboardMode) ?: return
        bestCandidate = candidates.firstOrNull() as? HanNomConverter.Candidate
        submitCandidates(candidates)
    }

    private fun renderInputView() {
        val composing = chuQuocNguTableConverter.convert(wordComposer.composingText, keyboardMode)
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
            else -> super.onSpecial(keyCode)
        }
    }

    data class Params(
        val layout: Layout,
        val numberRow: Boolean
    ): IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): IMEMode {
            return when(layout) {
                Layout.Qwerty -> Qwerty(listener, numberRow)
                Layout.Telex -> Telex(listener, numberRow)
            }
        }

        override fun getLabel(context: Context): String {
            val localeName = Locale("vi").displayName
            val layoutName = layout.name
            return "$localeName $layoutName"
        }

        override fun getShortLabel(context: Context, params: List<IMEMode.Params>): String {
            val vietParams = params.filterIsInstance<Params>().filterNot { it == this }
            // If this is the only Vietnamese mode
            if(vietParams.isEmpty()) return "越"
            // If not, use specific layout name
            val layoutHead = layout.name.first()
            return "越$layoutHead"
        }

        companion object {
            fun parse(map: Map<String, String>): Params {
                val layout = Layout.valueOf(map["layout"] ?: Layout.Qwerty.name)
                val numberRow = map["number_row"]?.toBoolean() ?: false
                return Params(
                    layout = layout,
                    numberRow = numberRow
                )
            }
        }
    }

    enum class Layout(
        @StringRes val nameKey: Int
    ) {
        Qwerty(R.string.viet_layout_qwerty),
        Telex(R.string.viet_layout_telex)
    }

    companion object {
        const val TYPE: String = "viet"
    }
}