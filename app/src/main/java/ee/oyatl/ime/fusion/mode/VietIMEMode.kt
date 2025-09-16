package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.view.KeyEvent
import androidx.annotation.StringRes
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.korean.WordComposer
import ee.oyatl.ime.keyboard.DefaultMobileKeyboard
import ee.oyatl.ime.keyboard.DefaultTabletKeyboard
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.ScreenTypeKeyboard
import ee.oyatl.ime.viet.ChuQuocNguTableConverter
import ee.oyatl.ime.viet.HanNomConverter
import java.util.Locale

abstract class VietIMEMode(
    listener: IMEMode.Listener
): CommonIMEMode(listener) {

    class Qwerty(
        listener: IMEMode.Listener
    ): VietIMEMode(listener) {
        override val keyboardMode: String = "q"
    }

    class Telex(
        listener: IMEMode.Listener
    ): VietIMEMode(listener) {
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

    override fun createDefaultKeyboard(layer: List<List<Int>>): Keyboard {
        return ScreenTypeKeyboard(
            mobile = DefaultMobileKeyboard(layer),
            tablet = DefaultTabletKeyboard(layer, extraKeys = listOf('，'.code, '。'.code))
        )
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
        val candidates = hanNomConverter?.convert(wordComposer.word, keyboardMode) ?: return
        bestCandidate = candidates.firstOrNull() as? HanNomConverter.Candidate
        submitCandidates(candidates)
    }

    private fun renderInputView() {
        val composing = chuQuocNguTableConverter.convert(wordComposer.word, keyboardMode)
        currentInputConnection?.setComposingText(composing, 1)
        convert()
    }

    override fun onChar(code: Int) {
        wordComposer.commit(code.toChar().toString())
        renderInputView()
    }

    override fun onSpecial(type: Keyboard.SpecialKey) {
        when(type) {
            Keyboard.SpecialKey.Space -> {
                val bestCandidate = bestCandidate
                if(bestCandidate != null) {
                    onCandidateSelected(bestCandidate)
                } else {
                    onReset()
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_SPACE)
                }
            }
            Keyboard.SpecialKey.Return -> {
                val send = wordComposer.word.isEmpty()
                onReset()
                if(send) {
                    if(util?.sendDefaultEditorAction(true) != true)
                        util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                }
            }
            Keyboard.SpecialKey.Delete -> {
                if(wordComposer.word.isNotEmpty()) {
                    wordComposer.delete(1)
                    renderInputView()
                } else {
                    onReset()
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                }
            }
            else -> {}
        }
    }

    data class Params(
        val layout: Layout
    ): IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): IMEMode {
            return when(layout) {
                Layout.Qwerty -> Qwerty(listener)
                Layout.Telex -> Telex(listener)
            }
        }

        override fun getLabel(context: Context): String {
            val localeName = Locale("vi").displayName
            val layoutName = layout.name
            return "$localeName $layoutName"
        }

        override fun getShortLabel(context: Context): String {
            val layoutHead = layout.name.first()
            return "越$layoutHead"
        }

        companion object {
            fun parse(map: Map<String, String>): Params {
                val layout = Layout.valueOf(map["layout"] ?: Layout.Qwerty.name)
                return Params(
                    layout = layout
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