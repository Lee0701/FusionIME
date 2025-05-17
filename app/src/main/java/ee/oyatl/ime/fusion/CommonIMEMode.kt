package ee.oyatl.ime.fusion

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.candidate.ScrollingCandidateView
import ee.oyatl.ime.candidate.VerticalScrollingCandidateView
import ee.oyatl.ime.keyboard.CommonKeyboardListener
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.keyboardset.KeyboardSet

abstract class CommonIMEMode(
    private val listener: IMEMode.Listener
): IMEMode, CandidateView.Listener, CommonKeyboardListener.Callback {

    abstract val keyboardSet: KeyboardSet

    protected val keyboardListener = KeyboardListener()
    protected lateinit var candidateView: CandidateView
    protected lateinit var imeView: ViewGroup

    protected var util: KeyEventUtil? = null
        private set
    protected val currentInputConnection: InputConnection? get() = util?.currentInputConnection
    protected val currentInputEditorInfo: EditorInfo? get() = util?.currentInputEditorInfo

    abstract fun onChar(code: Int)
    abstract fun onSpecial(type: Keyboard.SpecialKey)

    override fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo) {
        util = KeyEventUtil(inputConnection, editorInfo)
    }

    override fun onFinish() {
        onReset()
        util = null
    }

    open fun onReset() {
        currentInputConnection?.finishComposingText()
        submitCandidates(emptyList())
    }

    override fun createInputView(context: Context): View {
        keyboardSet.initView(context)
        imeView = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val inputView = keyboardSet.getView(keyboardListener.shiftState, false)
        candidateView = createCandidateView(context) as CandidateView
        imeView.addView(candidateView as View)
        imeView.addView(inputView)
        return imeView
    }

    override fun createCandidateView(context: Context): View {
        return ScrollingCandidateView(context, null).apply {
            listener = this@CommonIMEMode
        }
    }

    override fun getInputView(): View {
        updateInputView()
        return imeView
    }

    override fun updateInputView() {
        keyboardSet.getView(keyboardListener.shiftState, false)
    }

    protected fun submitCandidates(candidates: List<CandidateView.Candidate>) {
        listener.onCandidateViewVisibilityChange(candidates.isNotEmpty())
        candidateView.submitList(candidates)
    }

    inner class KeyboardListener: CommonKeyboardListener(this) {
        override fun onChar(code: Int) {
            this@CommonIMEMode.onChar(code)
            super.onChar(code)
        }

        override fun onSpecial(type: Keyboard.SpecialKey) {
            when(type) {
                Keyboard.SpecialKey.Language -> listener.onLanguageSwitch()
                else -> this@CommonIMEMode.onSpecial(type)
            }
            super.onSpecial(type)
        }
    }

}