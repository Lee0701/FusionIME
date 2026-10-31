package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.view.KeyEvent
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.korean.WordComposer
import ee.oyatl.ime.fusion.layout.preset.JyutpingLayoutPresets
import ee.oyatl.ime.fusion.layout.preset.LatinLayoutPresets
import ee.oyatl.ime.keyboard.KeyboardLayoutPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jyutping.jyutping.BinaryDictionaries
import org.jyutping.jyutping.models.Researcher
import org.jyutping.jyutping.models.Segmenter
import org.jyutping.jyutping.models.VirtualInputKey
import org.jyutping.jyutping.models.previewMark
import org.jyutping.jyutping.models.schemeLength
import org.jyutping.jyutping.presets.PresetCharacter
import java.util.Locale

class JyutpingIMEMode(
    override var textLayoutPreset: KeyboardLayoutPreset,
    listener: IMEMode.Listener
): CommonIMEMode(listener) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var convertJob: Job? = null

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
                coroutineScope.launch {
                    if(wordComposer.composingText.isNotEmpty()) {
                        postUpdateSuggestions()
                        convertJob?.join()
                        val bestCandidate = bestCandidate
                        if(bestCandidate != null) onCandidateSelected(bestCandidate)
                    } else {
                        util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_SPACE)
                    }
                    onReset()
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
        postUpdateSuggestions()
    }

    private fun postUpdateSuggestions() {
        convertJob?.cancel()
        convertJob = coroutineScope.launch {
            val textBeforeCursor = wordComposer.textBeforeCursor
            val textAfterCursor = wordComposer.textAfterCursor
            val keys = textBeforeCursor.mapNotNull { VirtualInputKey.matchVirtualInputKey(it) }
            val segmentation = Segmenter.segment(keys)
            val suggestions = Researcher.suggest(keys, segmentation)
            val candidates = suggestions.map { JyutpingCandidate(it.text, it.romanization, it.input) }.distinctBy { it.text }
            bestCandidate = candidates.firstOrNull()
            submitCandidates(candidates)
            val mark = run {
                val isPeculiar = keys.any { key -> key.text.any { it.isUpperCase() } || !key.isSyllableLetter }
                if(isPeculiar) return@run keys.previewMarkNormalized()
                val firstCandidate = suggestions.firstOrNull()
                if(firstCandidate?.inputCount == keys.size) return@run firstCandidate.mark
                val bestScheme = segmentation.firstOrNull()
                val leadingLength = bestScheme?.schemeLength ?: 0
                val leadingMark = bestScheme?.previewMark ?: ""
                when(leadingLength) {
                    0 -> textBeforeCursor
                    textBeforeCursor.length -> leadingMark
                    else -> leadingMark + " " + textBeforeCursor.drop(leadingLength)
                }
            }
            withContext(Dispatchers.Main) {
                val space = textBeforeCursor.isNotEmpty() && textAfterCursor.isNotEmpty()
                currentInputConnection?.setComposingText(WordComposer.buildSpannableSurfaceString(
                    mark,
                    (if(space) " " else "") + textAfterCursor
                ), 1)
            }
        }
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

    fun List<VirtualInputKey>.previewMarkNormalized(): String = buildString {
        val inputLength = this@previewMarkNormalized.size
        var index = 0
        while (index < inputLength) {
            val key = this@previewMarkNormalized[index]
            val isPaired = (index + 1 < inputLength) && (key == this@previewMarkNormalized[index + 1])
            if (isPaired) {
                val replacement = when (key) {
                    VirtualInputKey.letterV -> VirtualInputKey.number4
                    VirtualInputKey.letterX -> VirtualInputKey.number5
                    VirtualInputKey.letterQ -> VirtualInputKey.number6
                    else -> null
                }
                if (replacement != null) {
                    append(replacement.character)
                    index += 2
                    if (index < inputLength) {
                        append(PresetCharacter.SPACE)
                    }
                    continue
                }
            }
            val matched = when (key) {
                VirtualInputKey.letterV -> VirtualInputKey.number1
                VirtualInputKey.letterX -> VirtualInputKey.number2
                VirtualInputKey.letterQ -> VirtualInputKey.number3
                else -> if (key.isLetter) null else key
            }
            index += 1
            if (matched != null) {
                append(matched.character)
                if (index < inputLength) {
                    append(PresetCharacter.SPACE)
                }
            } else {
                val keyText = if (key.text.any { it.isUpperCase() }) key.text.uppercase() else key.text
                append(keyText)
            }
        }
    }

    class Params(
        val numberRow: Boolean = false,
        val cursorKeys: Boolean
    ): IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): IMEMode {
            val textLayoutPreset = JyutpingLayoutPresets.jyutping(LatinLayoutPresets.qwerty(false, numberRow, cursorKeys))
            return JyutpingIMEMode(textLayoutPreset, listener)
        }

        override fun getLabel(context: Context): String {
            val localeName = Locale.Builder().setLanguage("zh").setRegion("HK").build().displayName
            val layoutName = context.getString(R.string.jyutping_layout_jyutping)
            return "$localeName $layoutName"
        }

        override fun getShortLabels(context: Context): List<String> {
            return listOf("粵拼")
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