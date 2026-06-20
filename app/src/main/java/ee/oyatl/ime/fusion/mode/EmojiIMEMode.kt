package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.util.Consumer
import androidx.emoji2.emojipicker.EmojiViewItem
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.candidate.ScrollingCandidateView
import ee.oyatl.ime.fusion.DimensionUtil
import ee.oyatl.ime.fusion.KeyEventUtil
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.databinding.EmojiImeModeBinding

class EmojiIMEMode(
    private val listener: IMEMode.Listener
): IMEMode, Consumer<EmojiViewItem>, CandidateView.Listener {

    private var inputView: View? = null
    private var candidateView: CandidateView? = null
    private var util: KeyEventUtil? = null
    private val currentInputConnection: InputConnection? get() = util?.currentInputConnection

    override suspend fun onLoad(context: Context) {
    }

    override fun onStart(
        inputConnection: InputConnection,
        editorInfo: EditorInfo
    ) {
        util = KeyEventUtil(inputConnection, editorInfo)
        currentInputConnection?.finishComposingText()
    }

    override fun onFinish() {
        currentInputConnection?.finishComposingText()
    }

    override fun createInputView(context: Context): View {
        val height = DimensionUtil.getKeyboardHeight(context)
        val binding = EmojiImeModeBinding.inflate(LayoutInflater.from(context))
        binding.emojiPickerView.setOnEmojiPickedListener(this)
        binding.buttonDelete.setOnClickListener {
            onDelete()
        }
        binding.root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            height
        )
        this.inputView = binding.root
        return binding.root
    }

    override fun createCandidateView(context: Context): View {
        candidateView = ScrollingCandidateView(context, null).apply {
            listener = this@EmojiIMEMode
        }
        return candidateView as View
    }

    override fun getInputView(): View? {
        return inputView
    }

    override fun accept(value: EmojiViewItem) {
        currentInputConnection?.finishComposingText()
        currentInputConnection?.setComposingText(value.emoji, 1)
        val candidates = value.variants.map { Candidate(it) }
        submitCandidates(candidates)
    }

    private fun submitCandidates(candidates: List<Candidate>) {
        candidateView?.submitList(candidates)
        listener.onCandidateViewVisibilityChange(candidates.isNotEmpty())
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        currentInputConnection?.setComposingText(candidate.text, 1)
        currentInputConnection?.finishComposingText()
        submitCandidates(emptyList())
    }

    private fun onDelete() {
        currentInputConnection?.finishComposingText()
        util?.deleteSurroundingText(1, 0)
    }

    override fun onKeyDown(keyCode: Int, metaState: Int) = Unit

    override fun onKeyUp(keyCode: Int, metaState: Int) = Unit

    override fun updateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) = Unit

    data class Candidate(
        override val text: CharSequence
    ): CandidateView.Candidate

    class Params: IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): IMEMode {
            return EmojiIMEMode(listener)
        }

        override fun getLabel(context: Context): String {
            return context.getString(R.string.input_mode_emoji)
        }

        override fun getShortLabel(
            context: Context,
            params: List<IMEMode.Params>
        ): String {
            return "\uD83D\uDE00"
        }

        companion object {
            fun parse(map: Map<String, String>): Params {
                return Params()
            }
        }
    }

    companion object {
        const val TYPE: String = "emoji"
    }
}