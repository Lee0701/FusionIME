package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.content.res.Configuration
import android.util.TypedValue
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import androidx.preference.PreferenceManager
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.candidate.ScrollingCandidateView
import ee.oyatl.ime.fusion.KeyEventUtil
import ee.oyatl.ime.keyboard.DefaultBottomRowKeyboard
import ee.oyatl.ime.keyboard.DefaultMobileKeyboard
import ee.oyatl.ime.keyboard.DefaultNumberKeyboard
import ee.oyatl.ime.keyboard.DefaultSymbolsBottomRowKeyboard
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.KeyboardInflater
import ee.oyatl.ime.keyboard.KeyboardState
import ee.oyatl.ime.keyboard.KeyboardViewParams
import ee.oyatl.ime.keyboard.ShiftStateKeyboard
import ee.oyatl.ime.keyboard.StackedKeyboard
import ee.oyatl.ime.keyboard.layout.KeyboardTemplates
import ee.oyatl.ime.keyboard.layout.LayoutQwerty
import ee.oyatl.ime.keyboard.layout.LayoutSymbol
import ee.oyatl.ime.keyboard.listener.AutoShiftLockListener
import ee.oyatl.ime.keyboard.listener.ClickKeyOnReleaseListener
import ee.oyatl.ime.keyboard.listener.FeedbackListener
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.listener.OnKeyClickListener
import ee.oyatl.ime.keyboard.listener.RepeatableKeyListener
import kotlin.math.roundToInt

abstract class CommonIMEMode(
    private val listener: IMEMode.Listener
): IMEMode, CandidateView.Listener, AutoShiftLockListener.StateContainer {
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

    private var switcherView: FrameLayout? = null
    private var textKeyboardView: View? = null
    private var symbolKeyboardView: View? = null
    private var numpadKeyboardView: View? = null
    protected var candidateView: CandidateView? = null

    private lateinit var textKeyboardListener: KeyboardListener
    private lateinit var symbolKeyboardListener: KeyboardListener
    private lateinit var directKeyboardListener: KeyboardListener

    private var symbolState: KeyboardState.Symbol = KeyboardState.Symbol.Text
    override var shiftState: KeyboardState.Shift = KeyboardState.Shift.Released
        set(value) {
            field = value
            updateInputView()
        }

    protected var util: KeyEventUtil? = null
        private set
    protected var passwordField: Boolean = false
    protected val currentInputConnection: InputConnection? get() = util?.currentInputConnection
    protected val currentInputEditorInfo: EditorInfo? get() = util?.currentInputEditorInfo

    abstract fun onChar(code: Int)
    abstract fun onSpecial(type: Keyboard.SpecialKey)

    override suspend fun onLoad(context: Context) = Unit

    override fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo) {
        util = KeyEventUtil(inputConnection, editorInfo)
        onReset()
        setPreferredKeyboard(editorInfo)
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
        val preference = PreferenceManager.getDefaultSharedPreferences(context)
        textKeyboardListener = createKeyboardListener(context, KeyListener())
        symbolKeyboardListener = createKeyboardListener(context, KeyListener(), false)
        directKeyboardListener = createKeyboardListener(context, DirectKeyListener())
        val switcherView = FrameLayout(context)
        val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val rowHeightKey = if(landscape) "keyboard_height_landscape" else "keyboard_height_portrait"
        val rowHeightDefault = if(landscape) 45f else 55f
        val rowHeightDIP = preference.getFloat(rowHeightKey, rowHeightDefault)
        val height = (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, rowHeightDIP, context.resources.displayMetrics) * 4).roundToInt()
        val showPreviewPopup = preference.getBoolean("keyboard_preview_popup", true)
        val params = KeyboardViewParams(
            keyHeight = height / 4,
            showPreviewPopup = showPreviewPopup
        )
        textKeyboardView = textKeyboard.createView(context, textKeyboardListener, params.copy(keyHeight = height / textKeyboard.numRows))
        symbolKeyboardView = symbolKeyboard.createView(context, symbolKeyboardListener, params.copy(keyHeight = height / symbolKeyboard.numRows))
        numpadKeyboardView = numpadKeyboard.createView(context, directKeyboardListener, params.copy(keyHeight = height / numpadKeyboard.numRows))
        switcherView.addView(textKeyboardView)
        switcherView.addView(symbolKeyboardView)
        switcherView.addView(numpadKeyboardView)
        this.switcherView = switcherView
        updateInputView()
        return switcherView
    }

    override fun createCandidateView(context: Context): View {
        candidateView = ScrollingCandidateView(context, null).apply {
            listener = this@CommonIMEMode
        }
        return candidateView as View
    }

    override fun getInputView(): View? {
        updateInputView()
        return switcherView
    }

    private fun updateInputView() {
        when (symbolState) {
            KeyboardState.Symbol.Text -> {
                textKeyboardView?.bringToFront()
                textKeyboard.setShiftState(shiftState)
            }
            KeyboardState.Symbol.Symbol -> {
                symbolKeyboardView?.bringToFront()
                symbolKeyboard.setShiftState(shiftState)
            }
            KeyboardState.Symbol.Number -> {
                numpadKeyboardView?.bringToFront()
                numpadKeyboard.setShiftState(shiftState)
            }
        }
    }

    private fun setPreferredKeyboard(editorInfo: EditorInfo) {
        when(editorInfo.inputType and EditorInfo.TYPE_MASK_CLASS) {
            EditorInfo.TYPE_CLASS_NUMBER -> {
                if(symbolState != KeyboardState.Symbol.Number) {
                    symbolState = KeyboardState.Symbol.Number
                    updateInputView()
                }
            }
            EditorInfo.TYPE_CLASS_TEXT -> {
                when(editorInfo.inputType and EditorInfo.TYPE_MASK_VARIATION) {
                    EditorInfo.TYPE_TEXT_VARIATION_PASSWORD,
                    EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                    EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD -> {
                        if(!passwordField) {
                            passwordField = true
                            onReset()
                        }
                    }
                    else -> {
                        if(passwordField) {
                            passwordField = false
                            onReset()
                        }
                    }
                }
                symbolState = KeyboardState.Symbol.Text
                updateInputView()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, metaState: Int) {
        val shiftOn = (metaState and KeyEvent.META_SHIFT_MASK) != 0
        val layer = if(shiftOn) 1 else 0
        val specialKey = getSpecialKeyType(keyCode)
        if(specialKey != null) handleSpecialKey(specialKey)
        else onChar(
            layoutTable[keyCode]?.getOrNull(layer)
                ?: layoutTable[keyCode]?.getOrNull(0)
                ?: keyCharacterMap.get(keyCode, metaState)
        )
    }

    override fun onKeyUp(keyCode: Int, metaState: Int) {
    }

    private fun handleSpecialKey(type: Keyboard.SpecialKey) {
        when(type) {
            Keyboard.SpecialKey.Language -> listener.onLanguageSwitch()
            Keyboard.SpecialKey.Symbols -> {
                symbolState =
                    if(symbolState != KeyboardState.Symbol.Symbol) KeyboardState.Symbol.Symbol
                    else KeyboardState.Symbol.Text
                shiftState = KeyboardState.Shift.Released
            }
            Keyboard.SpecialKey.Numbers -> {
                symbolState =
                    if(symbolState != KeyboardState.Symbol.Number) KeyboardState.Symbol.Number
                    else KeyboardState.Symbol.Text
                shiftState = KeyboardState.Shift.Released
            }
            else -> onSpecial(type)
        }
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

    private fun createKeyboardListener(
        context: Context,
        listener: OnKeyClickListener,
        autoReleaseOnInput: Boolean = true
    ): KeyboardListener {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val sound = pref.getBoolean("sound_feedback", true)
        val haptic = pref.getBoolean("haptic_feedback", true)
        val soundVolume = if(sound) 1f else 0f
        val vibrationDuration = if(haptic) 10L else 0L
        return FeedbackListener.Repeatable(
            context,
            RepeatableKeyListener.RepeatToKeyDownUp(
                AutoShiftLockListener(
                    ClickKeyOnReleaseListener(listener),
                    stateContainer = this,
                    autoReleaseOnInput = autoReleaseOnInput
                )
            ),
            soundVolume = soundVolume,
            vibrationDuration = vibrationDuration,
            repeatVibrationDuration = vibrationDuration / 2
        )
    }

    inner class KeyListener: OnKeyClickListener {
        override fun onKeyClick(code: Int) {
            val special = Keyboard.SpecialKey.ofCode(code)
            if(special != null) handleSpecialKey(special)
            else onChar(code)
            updateInputView()
        }
    }

    inner class DirectKeyListener: OnKeyClickListener {
        override fun onKeyClick(code: Int) {
            val special = Keyboard.SpecialKey.ofCode(code)
            if(special != null) handleSpecialKey(special)
            else {
                onReset()
                util?.sendKeyChar(code.toChar())
            }
            updateInputView()
        }
    }
}