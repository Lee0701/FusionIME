package ee.oyatl.ime.fusion

import android.content.Context
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.candidate.ScrollingCandidateView
import ee.oyatl.ime.keyboard.CommonKeyboardListener
import ee.oyatl.ime.keyboard.DefaultBottomRowKeyboard
import ee.oyatl.ime.keyboard.DefaultMobileKeyboard
import ee.oyatl.ime.keyboard.DefaultNumberKeyboard
import ee.oyatl.ime.keyboard.DefaultSymbolsBottomRowKeyboard
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.KeyboardInflater
import ee.oyatl.ime.keyboard.KeyboardState
import ee.oyatl.ime.keyboard.ShiftStateKeyboard
import ee.oyatl.ime.keyboard.StackedKeyboard
import ee.oyatl.ime.keyboard.layout.KeyboardTemplates
import ee.oyatl.ime.keyboard.layout.LayoutQwerty
import ee.oyatl.ime.keyboard.layout.LayoutSymbol

abstract class CommonIMEMode(
    private val listener: IMEMode.Listener
): IMEMode, CandidateView.Listener, CommonKeyboardListener.Callback {
    private val keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

    open val layoutTable: Map<Int, List<Int>> = LayoutQwerty.TABLE_QWERTY
    private val textKeyboardLayers = KeyboardInflater.inflate(KeyboardTemplates.MOBILE, LayoutQwerty.TABLE_QWERTY)
    open val textKeyboard: Keyboard = StackedKeyboard(
        ShiftStateKeyboard(
            DefaultMobileKeyboard(textKeyboardLayers[0]),
            DefaultMobileKeyboard(textKeyboardLayers[1])
        ),
        DefaultBottomRowKeyboard()
    )

    open val symbolKeyboard: Keyboard = StackedKeyboard(
        ShiftStateKeyboard(
            DefaultMobileKeyboard(KeyboardInflater.inflate(LayoutSymbol.ROWS_LOWER, mapOf())[0]),
            DefaultMobileKeyboard(KeyboardInflater.inflate(LayoutSymbol.ROWS_UPPER, mapOf())[0])
        ),
        ShiftStateKeyboard(
            DefaultSymbolsBottomRowKeyboard(),
            DefaultSymbolsBottomRowKeyboard(listOf('<'.code, '>'.code))
        )
    )

    open val numpadKeyboard: Keyboard = DefaultNumberKeyboard()

    private lateinit var switcherView: FrameLayout
    private lateinit var textKeyboardView: View
    private lateinit var symbolKeyboardView: View
    private lateinit var numpadKeyboardView: View

    private val keyboardListener = KeyboardListener()
    private val directKeyboardListener = DirectKeyboardListener()
    protected var candidateView: CandidateView? = null

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
        switcherView = FrameLayout(context)
        val height = context.resources.getDimensionPixelSize(ee.oyatl.ime.keyboard.R.dimen.keyboard_height)
        textKeyboardView = textKeyboard.createView(context, keyboardListener, height / textKeyboard.numRows)
        symbolKeyboardView = symbolKeyboard.createView(context, keyboardListener, height / symbolKeyboard.numRows)
        numpadKeyboardView = numpadKeyboard.createView(context, directKeyboardListener, height / numpadKeyboard.numRows)
        switcherView.addView(textKeyboardView)
        switcherView.addView(symbolKeyboardView)
        switcherView.addView(numpadKeyboardView)
        return switcherView
    }

    override fun createCandidateView(context: Context): View {
        candidateView = ScrollingCandidateView(context, null).apply {
            listener = this@CommonIMEMode
        }
        return candidateView as View
    }

    override fun getInputView(): View {
        updateInputView()
        return switcherView
    }

    override fun updateInputView() {
        textKeyboard.changeState(keyboardListener.state)
        symbolKeyboard.changeState(keyboardListener.state)
        when (keyboardListener.state.symbol) {
            KeyboardState.Symbol.Text -> textKeyboardView.bringToFront()
            KeyboardState.Symbol.Symbol -> symbolKeyboardView.bringToFront()
            KeyboardState.Symbol.Number -> numpadKeyboardView.bringToFront()
        }
    }

    override fun onKeyDown(keyCode: Int, metaState: Int) {
        val shiftOn = (metaState and KeyEvent.META_SHIFT_MASK) != 0
        val layer = if(shiftOn) 1 else 0
        val specialKey = getSpecialKeyType(keyCode)
        if(specialKey != null) onSpecial(specialKey)
        else onChar(
            layoutTable[keyCode]?.getOrNull(layer)
                ?: layoutTable[keyCode]?.getOrNull(0)
                ?: keyCharacterMap.get(keyCode, metaState)
        )
    }

    override fun onKeyUp(keyCode: Int, metaState: Int) {
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

    protected fun requestHideSelf(flags: Int) {
        listener.onRequestHideSelf(flags)
    }

    inner class KeyboardListener: CommonKeyboardListener(this) {
        override fun onChar(code: Int) {
            this@CommonIMEMode.onChar(code)
            super.onChar(code)
        }

        private fun onSymbolsKey() {
            state = state.copy(symbol =
                if(state.symbol == KeyboardState.Symbol.Text) KeyboardState.Symbol.Symbol
                else KeyboardState.Symbol.Text
            )
            updateInputView()
        }

        private fun onNumbersKey() {
            state = state.copy(symbol =
                if(state.symbol == KeyboardState.Symbol.Symbol) KeyboardState.Symbol.Number
                else KeyboardState.Symbol.Symbol
            )
            updateInputView()
        }

        fun returnToText() {
            state = state.copy(symbol = KeyboardState.Symbol.Text)
            updateInputView()
        }

        override fun onSpecial(type: Keyboard.SpecialKey, pressed: Boolean) {
            if(!pressed) when(type) {
                Keyboard.SpecialKey.Language -> listener.onLanguageSwitch()
                Keyboard.SpecialKey.Symbols -> onSymbolsKey()
                Keyboard.SpecialKey.Numbers -> onNumbersKey()
                else -> this@CommonIMEMode.onSpecial(type)
            }
            super.onSpecial(type, pressed)
        }
    }

    inner class DirectKeyboardListener: CommonKeyboardListener(this) {
        override fun onChar(code: Int) {
            onReset()
            currentInputConnection?.commitText(code.toChar().toString(), 1)
            super.onChar(code)
        }

        override fun onSpecial(type: Keyboard.SpecialKey, pressed: Boolean) {
            if(!pressed) when(type) {
                Keyboard.SpecialKey.Language -> listener.onLanguageSwitch()
                Keyboard.SpecialKey.Symbols -> keyboardListener.returnToText()
                Keyboard.SpecialKey.Numbers -> keyboardListener.returnToText()
                else -> this@CommonIMEMode.onSpecial(type)
            }
            super.onSpecial(type, pressed)
        }
    }
}