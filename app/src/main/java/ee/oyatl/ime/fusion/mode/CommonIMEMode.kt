package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.content.res.Configuration
import android.util.TypedValue
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.preference.PreferenceManager
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.candidate.ScrollingCandidateView
import ee.oyatl.ime.fusion.KeyEventUtil
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.keyboard.DefaultBottomRowKeyboard
import ee.oyatl.ime.keyboard.DefaultMobileKeyboard
import ee.oyatl.ime.keyboard.DefaultNumberKeyboard
import ee.oyatl.ime.keyboard.DefaultTabletBottomRowKeyboard
import ee.oyatl.ime.keyboard.DefaultTabletKeyboard
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.KeyboardInflater
import ee.oyatl.ime.keyboard.KeyboardState
import ee.oyatl.ime.keyboard.KeyboardViewParams
import ee.oyatl.ime.keyboard.ScreenModeKeyboard
import ee.oyatl.ime.keyboard.ShiftStateKeyboard
import ee.oyatl.ime.keyboard.StackedKeyboard
import ee.oyatl.ime.keyboard.SymbolStateKeyboard
import ee.oyatl.ime.keyboard.layout.KeyboardTemplates
import ee.oyatl.ime.keyboard.layout.LayoutQwerty
import ee.oyatl.ime.keyboard.layout.LayoutSymbol
import ee.oyatl.ime.keyboard.listener.AutoShiftLockListener
import ee.oyatl.ime.keyboard.listener.ClickKeyOnReleaseListener
import ee.oyatl.ime.keyboard.listener.FeedbackListener
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.listener.OnKeyClickListener
import ee.oyatl.ime.keyboard.listener.RepeatableKeyListener
import ee.oyatl.ime.keyboard.listener.SymbolStateKeyboardListener
import kotlin.math.roundToInt

abstract class CommonIMEMode(
    private val listener: IMEMode.Listener
): IMEMode, CandidateView.Listener, AutoShiftLockListener.StateContainer {
    private val keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

    open val layoutTable: Map<Int, List<Int>> = LayoutQwerty.TABLE_QWERTY

    protected var keyboard: Keyboard? = null
    protected var keyboardView: View? = null
    protected var candidateView: CandidateView? = null

    override var symbolState: KeyboardState.Symbol = KeyboardState.Symbol.Text
    override var shiftState: KeyboardState.Shift = KeyboardState.Shift.Released

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

        val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val rowHeightKey = if(landscape) "keyboard_height_landscape" else "keyboard_height_portrait"
        val rowHeightDefaultKey = if(landscape) R.integer.keyboard_height_landscape_default else R.integer.keyboard_height_portrait_default
        val rowHeightDefault = context.resources.getInteger(rowHeightDefaultKey).toFloat()
        val rowHeightDIP = preference.getFloat(rowHeightKey, rowHeightDefault)
        val height = (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, rowHeightDIP, context.resources.displayMetrics) * 4).roundToInt()
        val showPreviewPopup = preference.getBoolean("preview_popup", true)
        val params = KeyboardViewParams(
            keyHeight = height / 4,
            showPreviewPopup = showPreviewPopup
        )
        val defaultScreenMode = context.resources.getString(R.string.screen_mode_default)
        val screenMode = KeyboardState.ScreenMode.valueOf(preference.getString("screen_mode", null) ?: defaultScreenMode)

        val textKeyboardListener = createKeyboardListener(context, KeyListener())
        val symbolKeyboardListener = createKeyboardListener(context, KeyListener(), false)
        val directKeyboardListener = createKeyboardListener(context, DirectKeyListener())
        val keyboardListener = SymbolStateKeyboardListener(textKeyboardListener, symbolKeyboardListener, directKeyboardListener)

        val textKeyboard = createTextKeyboard()
        val symbolKeyboard = createSymbolKeyboard()
        val numpadKeyboard = createNumberKeyboard()
        val keyboard: Keyboard = SymbolStateKeyboard(textKeyboard, symbolKeyboard, numpadKeyboard)
        keyboard.setState(screenMode) // Set screen type before creating view.

        val keyboardView = keyboard.createView(context, keyboardListener, params)
        updateInputView()
        this.keyboard = keyboard
        this.keyboardView = keyboardView
        return keyboardView
    }

    override fun createCandidateView(context: Context): View {
        candidateView = ScrollingCandidateView(context, null).apply {
            listener = this@CommonIMEMode
        }
        return candidateView as View
    }

    override fun getInputView(): View? {
        updateInputView()
        return keyboardView
    }

    private fun updateInputView() {
        keyboard?.setState(symbolState)
        keyboard?.setState(shiftState)
    }

    open fun createTextKeyboard(): Keyboard {
        val layers = KeyboardInflater.inflate(KeyboardTemplates.MOBILE, layoutTable)
        return StackedKeyboard(
            ShiftStateKeyboard(
                createDefaultKeyboard(layers[0]),
                createDefaultKeyboard(layers[1])
            ),
            ShiftStateKeyboard(
                createBottomRowKeyboard(shift = false, symbol = false),
                createBottomRowKeyboard(shift = true, symbol = false)
            )
        )
    }

    open fun createSymbolKeyboard(): Keyboard {
        return StackedKeyboard(
            ShiftStateKeyboard(
                createDefaultKeyboard(KeyboardInflater.inflate(LayoutSymbol.ROWS_LOWER, mapOf())[0]),
                createDefaultKeyboard(KeyboardInflater.inflate(LayoutSymbol.ROWS_UPPER, mapOf())[0])
            ),
            ShiftStateKeyboard(
                createBottomRowKeyboard(shift = false, symbol = true),
                createBottomRowKeyboard(shift = true, symbol = true)
            )
        )
    }

    open fun createNumberKeyboard(): Keyboard {
        return DefaultNumberKeyboard()
    }

    open fun createDefaultKeyboard(layer: List<List<Int>>): Keyboard {
        return ScreenModeKeyboard(
            mobile = DefaultMobileKeyboard(layer),
            tablet = DefaultTabletKeyboard(layer)
        )
    }

    open fun createBottomRowKeyboard(shift: Boolean, symbol: Boolean): Keyboard {
        val extraKeys = if(!shift) listOf(','.code, '.'.code) else listOf('<'.code, '>'.code)
        val tabletExtraKeys = if(!symbol) listOf('!'.code, '?'.code) else listOf('<'.code, '>'.code)
        return ScreenModeKeyboard(
            mobile = DefaultBottomRowKeyboard(extraKeys = extraKeys, isSymbols = symbol),
            tablet = DefaultTabletBottomRowKeyboard(extraKeys = tabletExtraKeys, isSymbols = symbol)
        )
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
                updateInputView()
            }
            Keyboard.SpecialKey.Numbers -> {
                symbolState =
                    if(symbolState != KeyboardState.Symbol.Number) KeyboardState.Symbol.Number
                    else KeyboardState.Symbol.Text
                shiftState = KeyboardState.Shift.Released
                updateInputView()
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
        val duration = pref.getFloat("vibration_duration", 10f).toLong()
        val soundVolume = if(sound) 1f else 0f
        val vibrationDuration = if(haptic) duration else 0L
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