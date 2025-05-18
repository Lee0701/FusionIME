package ee.oyatl.ime.fusion

import android.content.Context
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.candidate.ScrollingCandidateView
import ee.oyatl.ime.keyboard.CommonKeyboardListener
import ee.oyatl.ime.keyboard.Keyboard

abstract class CommonIMEMode(
    private val listener: IMEMode.Listener
): IMEMode, CandidateView.Listener, CommonKeyboardListener.Callback {
    private val keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

    abstract val softKeyboard: Keyboard

    protected val keyboardListener = KeyboardListener()
    protected var candidateView: CandidateView? = null
    protected var imeView: View? = null

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
        val imeView = softKeyboard.createView(context, keyboardListener)
        this.imeView = imeView
        return imeView
    }

    override fun createCandidateView(context: Context): View {
        candidateView = ScrollingCandidateView(context, null).apply {
            listener = this@CommonIMEMode
        }
        return candidateView as View
    }

    override fun getInputView(): View {
        updateInputView()
        return imeView!!
    }

    override fun updateInputView() {
        softKeyboard.changeState(keyboardListener.shiftState)
    }

    override fun onKeyDown(keyCode: Int, metaState: Int) {
        val specialKey = getSpecialKeyType(keyCode)
        if(specialKey != null) keyboardListener.onSpecial(specialKey, true)
        else keyboardListener.onChar(keyCharacterMap.get(keyCode, metaState))
    }

    override fun onKeyUp(keyCode: Int, metaState: Int) {
        val specialKey = getSpecialKeyType(keyCode)
        if(specialKey != null) keyboardListener.onSpecial(specialKey, false)
    }

    private fun getSpecialKeyType(keyCode: Int): Keyboard.SpecialKey? {
        return when(keyCode) {
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> Keyboard.SpecialKey.Shift
            KeyEvent.KEYCODE_CAPS_LOCK -> Keyboard.SpecialKey.Caps
            KeyEvent.KEYCODE_SPACE -> Keyboard.SpecialKey.Space
            KeyEvent.KEYCODE_ENTER -> Keyboard.SpecialKey.Return
            KeyEvent.KEYCODE_DEL -> Keyboard.SpecialKey.Delete
            KeyEvent.KEYCODE_LANGUAGE_SWITCH -> Keyboard.SpecialKey.Language
            KeyEvent.KEYCODE_SYM -> Keyboard.SpecialKey.Symbols
            else -> null
        }
    }

    protected fun submitCandidates(candidates: List<CandidateView.Candidate>) {
        candidateView?.submitList(candidates)
        val visible = candidates.isNotEmpty()
        val candidateView = candidateView as? View
        candidateView?.visibility = if(visible) View.VISIBLE else View.GONE
        if(visible) candidateView?.bringToFront()
        listener.onCandidateViewVisibilityChange(visible)
    }

    inner class KeyboardListener: CommonKeyboardListener(this) {
        override fun onChar(code: Int) {
            this@CommonIMEMode.onChar(code)
            super.onChar(code)
        }

        override fun onSpecial(type: Keyboard.SpecialKey, pressed: Boolean) {
            if(pressed) when(type) {
                Keyboard.SpecialKey.Language -> listener.onLanguageSwitch()
                else -> this@CommonIMEMode.onSpecial(type)
            }
            super.onSpecial(type, pressed)
        }
    }

}