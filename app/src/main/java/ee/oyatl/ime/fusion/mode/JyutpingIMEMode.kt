package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.view.KeyEvent
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.Feature
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.korean.WordComposer
import ee.oyatl.ime.fusion.layout.MobileKeyboard
import ee.oyatl.ime.fusion.layout.MobileKeyboardRows
import ee.oyatl.ime.fusion.layout.TabletKeyboard
import ee.oyatl.ime.fusion.layout.TabletKeyboardRows
import ee.oyatl.ime.keyboard.KeyboardConfiguration
import ee.oyatl.ime.keyboard.KeyboardTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jyutping.jyutping.BinaryDictionaries
import org.jyutping.jyutping.models.Researcher
import org.jyutping.jyutping.models.Segmenter
import org.jyutping.jyutping.models.VirtualInputKey
import java.util.Locale

class JyutpingIMEMode(
    numberRow: Boolean,
    cursorKeys: Boolean,
    listener: IMEMode.Listener
): CommonIMEMode(listener) {
    private val numberRow = Feature.NumberRow.availableInCurrentVersion && numberRow
    private val cursorKeys = Feature.CursorKeys.availableInCurrentVersion && cursorKeys

    override val textKeyboardTemplate: KeyboardTemplate = KeyboardTemplate.ByScreenMode(
        mobile = KeyboardTemplate.Basic(
            configuration = KeyboardConfiguration(
                if(this.numberRow) MobileKeyboard.numbers() else KeyboardConfiguration(),
                MobileKeyboard.alphabetic(),
                MobileKeyboard.bottom(dpad = this.cursorKeys)
            ),
            contentRows = (if(this.numberRow) MobileKeyboardRows.NUMBERS else listOf()) + MobileKeyboardRows.DEFAULT
        ),
        tablet = KeyboardTemplate.Basic(
            configuration = KeyboardConfiguration(
                if(this.numberRow) TabletKeyboard.numbers(delete = true) else KeyboardConfiguration(),
                TabletKeyboard.alphabetic(delete = !this.numberRow),
                TabletKeyboard.bottom()
            ),
            contentRows = (if(this.numberRow) TabletKeyboardRows.NUMBERS else listOf()) + TabletKeyboardRows.DEFAULT
        )
    )

    private val wordComposer = WordComposer()
    private var bestCandidate: CandidateView.Candidate? = null

    override suspend fun onLoad(context: Context) {
        super.onLoad(context)
        Segmenter.prepare()
        BinaryDictionaries.loadDictionaries(context)
    }

    override fun onReset() {
        super.onReset()
        wordComposer.reset()
        bestCandidate = null
    }

    override fun onChar(codePoint: Int) {
        wordComposer.commit(codePoint.toChar().toString())
        renderInput()
    }

    override fun onSpecial(keyCode: Int) {
        when(keyCode) {
            KeyEvent.KEYCODE_SPACE -> {
                if(wordComposer.composingText.isNotEmpty()) {
                    updateSuggestions()
                    val bestCandidate = bestCandidate
                    if(bestCandidate != null) onCandidateSelected(bestCandidate)
                } else {
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_SPACE)
                }
                onReset()
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
                    renderInput()
                } else {
                    onReset()
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
    }

    private fun renderInput() {
        currentInputConnection?.setComposingText(wordComposer.getSpannableSurfaceString(), 1)
        postUpdateSuggestions()
    }

    private fun postUpdateSuggestions() {
        CoroutineScope(Dispatchers.Default).launch {
            updateSuggestions()
        }
    }

    private fun updateSuggestions() {
        val keys = wordComposer.textBeforeCursor.mapNotNull { VirtualInputKey.matchVirtualInputKey(it) }
        val suggestions = Researcher.suggest(keys, Segmenter.segment(keys))
        val candidates = suggestions.map { JyutpingCandidate(it.text, it.romanization, it.input) }.distinctBy { it.text }
        bestCandidate = candidates.firstOrNull()
        submitCandidates(candidates)
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        currentInputConnection?.commitText(candidate.text, 1)
        if(candidate is JyutpingCandidate) {
            wordComposer.consume(candidate.input.length)
        } else {
            wordComposer.consume(candidate.text.length)
        }
        renderInput()
    }

    class Params(
        val numberRow: Boolean = false,
        val cursorKeys: Boolean
    ): IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): IMEMode {
            return JyutpingIMEMode(numberRow, cursorKeys, listener)
        }

        override fun getLabel(context: Context): String {
            val localeName = Locale("zh", "HK").displayName
            val layoutName = context.getString(R.string.jyutping_layout_jyutping)
            return "$localeName $layoutName"
        }

        override fun getShortLabel(
            context: Context,
            params: List<IMEMode.Params>
        ): String {
            return "粵拼"
        }

        companion object {
            fun parse(map: Map<String, String>): Params {
                val numberRow = map["number_row"]?.toBoolean() ?: false
                val cursorKeys = map["cursor_keys"]?.toBoolean() ?: false
                return Params(
                    numberRow = numberRow,
                    cursorKeys = cursorKeys
                )
            }
        }
    }

    data class JyutpingCandidate(
        override val text: CharSequence,
        override val extra: CharSequence,
        val input: CharSequence
    ): CandidateView.Candidate, CandidateView.ExtraCandidate

    companion object {
        const val TYPE: String = "jyutping"
    }
}