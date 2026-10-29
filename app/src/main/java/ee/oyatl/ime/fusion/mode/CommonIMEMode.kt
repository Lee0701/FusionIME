package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.util.TypedValue
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.preference.PreferenceManager
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.candidate.ScrollingCandidateView
import ee.oyatl.ime.fusion.AssetsPresetLoader
import ee.oyatl.ime.fusion.FlickAction
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.SymbolLayout
import ee.oyatl.ime.fusion.layout.preset.LatinLayoutPresets
import ee.oyatl.ime.fusion.layout.preset.SymbolLayoutPresets
import ee.oyatl.ime.fusion.util.KeyEventUtil
import ee.oyatl.ime.fusion.util.PreferenceUtil.getOrientationBoolean
import ee.oyatl.ime.fusion.util.PreferenceUtil.getOrientationInteger
import ee.oyatl.ime.keyboard.CanvasKeyboardView
import ee.oyatl.ime.keyboard.DefaultKeyboardView
import ee.oyatl.ime.fusion.keyboard.core.KeyInput
import ee.oyatl.ime.fusion.keyboard.core.KeyLabel
import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.keyboard.core.KeyboardParams
import ee.oyatl.ime.fusion.keyboard.core.KeyboardState
import ee.oyatl.ime.keyboard.KeyboardView
import ee.oyatl.ime.keyboard.SwitcherKeyboardView
import ee.oyatl.ime.fusion.keyboard.core.TouchMode
import ee.oyatl.ime.keyboard.listener.DeleteRepeater
import ee.oyatl.ime.keyboard.listener.FlickListener
import ee.oyatl.ime.keyboard.listener.InputOnKeyUp
import ee.oyatl.ime.keyboard.listener.KeyFeedbackManager
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.listener.LongPressListener
import ee.oyatl.ime.keyboard.listener.ParallelKeyboardListener
import ee.oyatl.ime.keyboard.listener.SerialKeyboardListener
import ee.oyatl.ime.keyboard.listener.ShiftStateManager
import ee.oyatl.ime.keyboard.popup.DefaultPopupManager
import ee.oyatl.ime.keyboard.popup.MoreKeysPopup
import ee.oyatl.ime.fusion.keyboard.core.FlickDirection
import ee.oyatl.ime.keyboard.KeyboardInflater
import ee.oyatl.ime.keyboard.touchhandler.FlickTouchHandler
import ee.oyatl.ime.keyboard.touchhandler.LongPressTouchHandler
import ee.oyatl.ime.keyboard.touchhandler.SeekTouchHandler
import ee.oyatl.ime.keyboard.touchhandler.TouchHandler
import ee.oyatl.ime.theme.KeyboardTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

abstract class CommonIMEMode(
    private val listener: IMEMode.Listener
): IMEMode, KeyboardListener, FlickListener, LongPressListener, CandidateView.Listener, ShiftStateManager.ShiftStateHolder {
    protected val keyCharacterMap: KeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

    override var keyboardTheme: KeyboardTheme? = null
    protected open val extra: Map<String, String> = mapOf()

    open var textLayoutPreset: KeyboardLayoutPreset = KeyboardLayoutPreset()
    open var symbolLayoutPreset: KeyboardLayoutPreset = KeyboardLayoutPreset()
    open var numberLayoutPreset: KeyboardLayoutPreset = KeyboardLayoutPreset()

    val currentLayoutPreset: KeyboardLayoutPreset get() = when(symbolState) {
        KeyboardState.Symbol.Text -> textLayoutPreset
        KeyboardState.Symbol.Symbol -> symbolLayoutPreset
        KeyboardState.Symbol.Number -> numberLayoutPreset
    }

    open val keyLabels: Map<Int, KeyLabel>
        get() = currentLayoutPreset.layoutTable.map.mapValues { (k, _) ->
            val input = KeyInput(k, shiftState, null)
            val text = currentLayoutPreset.layoutTable[input]?.toChar()?.toString()
            KeyLabel(text)
        }

    protected var keyboardView: KeyboardView? = null
    protected var candidateView: CandidateView? = null

    var symbolState: KeyboardState.Symbol = KeyboardState.Symbol.Text
    override var shiftState: KeyboardState.Shift = KeyboardState.Shift.Released
        set(value) {
            field = value
            updateInputView()
        }

    private var defaultFlickActions: Map<FlickDirection, FlickAction> = mapOf()

    protected var util: KeyEventUtil? = null
        private set
    protected var passwordField: Boolean = false
    protected open val currentInputConnection: InputConnection? get() = util?.currentInputConnection
    protected open val currentInputEditorInfo: EditorInfo? get() = util?.currentInputEditorInfo

    open fun onChar(codePoint: Int) {
        util?.sendKeyChar(codePoint.toChar())
    }

    open fun onSpecial(keyCode: Int) {
        when(keyCode) {
            KeyEvent.KEYCODE_LANGUAGE_SWITCH -> listener.onLanguageSwitch()
            KeyEvent.KEYCODE_SYM -> {
                symbolState =
                    if(symbolState != KeyboardState.Symbol.Symbol) KeyboardState.Symbol.Symbol
                    else KeyboardState.Symbol.Text
                shiftState = KeyboardState.Shift.Released
                keyboardView?.onReset()
            }
            KeyEvent.KEYCODE_NUM -> {
                symbolState =
                    if(symbolState != KeyboardState.Symbol.Number) KeyboardState.Symbol.Number
                    else KeyboardState.Symbol.Text
                shiftState = KeyboardState.Shift.Released
                keyboardView?.onReset()
            }
            else -> {
                util?.sendDownUpKeyEvents(keyCode)
            }
        }
    }

    override fun onLoad(context: Context) {
        val preference = PreferenceManager.getDefaultSharedPreferences(context)

        val loader = AssetsPresetLoader(context.assets, "keyboard/symbol")
        val symbolLayoutTypeName = preference.getString("symbol_layout", null)
        val symbolLayoutType = SymbolLayout.entries.find { it.name == symbolLayoutTypeName } ?: SymbolLayout.G
        this.symbolLayoutPreset = loader.inflate(loader.load(symbolLayoutType.presetName))
        this.numberLayoutPreset = loader.inflate(loader.load("number"))

        val flickActionUp = FlickAction.valueOf(preference.getString("default_flick_action_up", null) ?: FlickAction.Shifted.name)
        val flickActionDown = FlickAction.valueOf(preference.getString("default_flick_action_down", null) ?: FlickAction.Symbol.name)
        defaultFlickActions = mapOf(
            FlickDirection.Up to flickActionUp,
            FlickDirection.Down to flickActionDown
        )
    }

    override fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo) {
        util = KeyEventUtil(inputConnection, editorInfo)
        onReset()
        setPreferredKeyboard(editorInfo)
        shiftState = KeyboardState.Shift.Released
        updateInputView()
        CoroutineScope(Dispatchers.Main).launch { keyboardView?.onReset() }
    }

    override fun onFinish() {
        onReset()
        keyboardView?.onReset()
        util = null
    }

    override fun onReset() {
        currentInputConnection?.finishComposingText()
        submitCandidates(emptyList())
    }

    override fun createInputView(context: Context): View {
        val textKeyboardParams = createKeyboardParams(context, KeyboardState.Symbol.Text)
        val symbolKeyboardParams = createKeyboardParams(context, KeyboardState.Symbol.Symbol)
        val numberKeyboardParams = createKeyboardParams(context, KeyboardState.Symbol.Number)

        val textKeyboard = KeyboardInflater.inflate(textLayoutPreset, textKeyboardParams)
        val symbolKeyboard = KeyboardInflater.inflate(symbolLayoutPreset, symbolKeyboardParams)
        val numberKeyboard = KeyboardInflater.inflate(numberLayoutPreset, numberKeyboardParams)

        val textKeyboardView = DefaultKeyboardView(context, null).also {
            it.keyboardTheme = keyboardTheme
            it.keyboard = textKeyboard
            it.listener = createKeyboardListener(context, textKeyboardParams)
            it.touchHandler = createTouchHandler(it, textKeyboardParams, KeyboardState.Symbol.Text)
            it.popupManager = DefaultPopupManager()
        }
        val symbolKeyboardView = DefaultKeyboardView(context, null).also {
            it.keyboardTheme = keyboardTheme
            it.keyboard = symbolKeyboard
            it.listener = createKeyboardListener(context, symbolKeyboardParams)
            it.touchHandler = createTouchHandler(it, symbolKeyboardParams, KeyboardState.Symbol.Symbol)
            it.popupManager = DefaultPopupManager()
        }
        val numberKeyboardView = DefaultKeyboardView(context, null).also {
            it.keyboardTheme = keyboardTheme
            it.keyboard = numberKeyboard
            it.listener = createKeyboardListener(context, numberKeyboardParams)
            it.touchHandler = createTouchHandler(it, numberKeyboardParams, KeyboardState.Symbol.Number)
            it.popupManager = DefaultPopupManager()
        }

        updateInputView()
        val switcherKeyboardView = SwitcherKeyboardView(context, null)
        switcherKeyboardView.map = mapOf(
            KeyboardState.Symbol.Text to textKeyboardView,
            KeyboardState.Symbol.Symbol to symbolKeyboardView,
            KeyboardState.Symbol.Number to numberKeyboardView
        )
        this.keyboardView = switcherKeyboardView
        return switcherKeyboardView
    }

    override fun createCandidateView(context: Context): View {
        candidateView = ScrollingCandidateView(context, null).apply {
            keyboardTheme = this@CommonIMEMode.keyboardTheme
            listener = this@CommonIMEMode
        }
        return candidateView as View
    }

    override fun getInputView(): View? {
        updateInputView()
        return keyboardView?.view
    }

    protected fun updateInputView() {
        val keyboardView = keyboardView
        if(keyboardView is SwitcherKeyboardView) {
            // Update keyboard view states
            keyboardView.state = symbolState
        }
        if(keyboardView != null) {
            val shiftIcon = when(shiftState) {
                KeyboardState.Shift.Released -> ee.oyatl.ime.keyboard.R.drawable.keyic_shift
                KeyboardState.Shift.Pressed -> ee.oyatl.ime.keyboard.R.drawable.keyic_shift_pressed
                KeyboardState.Shift.Locked -> ee.oyatl.ime.keyboard.R.drawable.keyic_shift_locked
                KeyboardState.Shift.AutoShifted -> ee.oyatl.ime.keyboard.R.drawable.keyic_shift_pressed
            }
            val icons = mapOf(
                KeyEvent.KEYCODE_SHIFT_LEFT to KeyLabel(icon = shiftIcon),
                KeyEvent.KEYCODE_SHIFT_RIGHT to KeyLabel(icon = shiftIcon)
            )
            keyboardView.labels = icons + this.keyLabels
        }
    }

    open fun createKeyboardParams(
        context: Context,
        symbolState: KeyboardState.Symbol
    ): KeyboardParams {
        val preference = PreferenceManager.getDefaultSharedPreferences(context)

        val defaultScreenMode = context.resources.getString(R.string.screen_mode_default)
        val screenMode = KeyboardState.ScreenMode.valueOf(preference.getString("screen_mode", null) ?: defaultScreenMode)
        val rowHeightDIP = getOrientationInteger(context, "keyboard_height")
        val height = (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, rowHeightDIP, context.resources.displayMetrics) * 4).roundToInt()
        val split = getOrientationBoolean(context, "split_keyboard")
        val splitRatio = if(split) getOrientationInteger(context, "split_ratio") else 0f
        val splitWidthDIP = context.resources.configuration.screenWidthDp / 100f * splitRatio
        val splitWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, splitWidthDIP, context.resources.displayMetrics).roundToInt()
        val showPreviewPopup = preference.getBoolean("preview_popup", true)
        val sound = preference.getBoolean("sound_feedback", true)
        val haptic = preference.getBoolean("haptic_feedback", true)
        val duration = preference.getFloat("vibration_duration", 10f).toLong()
        val soundVolume = if(sound) 1f else 0f
        val vibrationDuration = if(haptic) duration else 0L
        val touchMode = TouchMode.entries.find { it.name == preference.getString("touch_mode", null) } ?: TouchMode.Seek
        val touchCorrection = preference.getBoolean("touch_correction", true)
        val defaultFlickSensitivity = context.resources.getInteger(R.integer.flick_sensitivity_default).toFloat()
        val flickSensitivity = preference.getFloat("flick_sensitivity", defaultFlickSensitivity).toInt()
        val params = KeyboardParams(
            screenMode = screenMode,
            height = height,
            splitWidth = splitWidth,
            soundFeedback = false,
            hapticFeedback = false,
            soundVolume = soundVolume,
            vibrationDuration = vibrationDuration,
            touchMode = touchMode,
            touchCorrection = touchCorrection,
            flickSensitivity = flickSensitivity,
            longPressDelay = 300,
            shiftLockDelay = 300,
            shiftAutoRelease = true,
            repeatDelay = 300,
            repeatInterval = 30,
            previewPopups = showPreviewPopup,
            extra = extra
        )

        return when(symbolState) {
            KeyboardState.Symbol.Text -> params
            KeyboardState.Symbol.Symbol -> params.copy(shiftAutoRelease = false, extra = mapOf())
            KeyboardState.Symbol.Number -> params.copy(shiftAutoRelease = false, splitWidth = 0, extra = mapOf())
        }
    }

    open fun createKeyboardListener(
        context: Context,
        params: KeyboardParams
    ): KeyboardListener {
        return SerialKeyboardListener(
            KeyFeedbackManager(context, params),
            DeleteRepeater(this, params),
            ParallelKeyboardListener(
                InputOnKeyUp(this, ignoreKeyCodes = ShiftStateManager.KEY_CODES),
                ShiftStateManager(this, params, this)
            )
        )
    }

    open fun createTouchHandler(
        keyboardView: KeyboardView,
        params: KeyboardParams,
        symbolState: KeyboardState.Symbol
    ): TouchHandler {
        val flick = params.touchMode == TouchMode.Flick
        val handler = if(flick) {
            FlickTouchHandler(keyboardView, params, diagonal = false, multiFlick = false)
        } else {
            SeekTouchHandler(keyboardView, params)
        }
        return LongPressTouchHandler(keyboardView, handler, params)
    }

    protected fun setPreferredKeyboard(editorInfo: EditorInfo) {
        when(editorInfo.inputType and EditorInfo.TYPE_MASK_CLASS) {
            EditorInfo.TYPE_CLASS_NUMBER -> {
                if(symbolState != KeyboardState.Symbol.Number) {
                    symbolState = KeyboardState.Symbol.Number
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
            }
        }
    }

    override fun onKeyDown(keyCode: Int, metaState: Int): Boolean {
        if(keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            shiftState = KeyboardState.Shift.Pressed
        } else if(keyCode == KeyEvent.KEYCODE_CAPS_LOCK) {
            shiftState =
                if(shiftState != KeyboardState.Shift.Locked) KeyboardState.Shift.Locked
                else KeyboardState.Shift.Released
        } else if(keyCode < 0) {
            onChar(-keyCode)
        } else if(keyCode > KeyEvent.getMaxKeyCode() || keyCharacterMap.isPrintingKey(keyCode)) {
            val altGrPressed = metaState and KeyEvent.META_ALT_RIGHT_ON != 0
            val table =
                if(altGrPressed) currentLayoutPreset.altGraphTable ?: currentLayoutPreset.layoutTable
                else currentLayoutPreset.layoutTable
            val input = KeyInput(keyCode, shiftState, null)
            val text = table[input] ?: return false
            onChar(text)
        } else {
            onSpecial(keyCode)
        }
        updateInputView()
        return true
    }

    override fun onKeyUp(keyCode: Int, metaState: Int): Boolean {
        if(keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            shiftState = KeyboardState.Shift.Released
        }
        updateInputView()
        return true
    }

    override fun translateKeyCode(keyCode: Int): Int {
        return currentLayoutPreset.hardKeyCodeMapper[keyCode]
    }

    override fun isAltGraphCapable(): Boolean {
        return currentLayoutPreset.altGraphTable != null
    }

    override fun onFlick(keyCode: Int, direction: FlickDirection): Boolean {
        val action = defaultFlickActions[direction]
        val shiftState =
            if(action == FlickAction.Shifted || action == FlickAction.ShiftedSymbol) KeyboardState.Shift.Pressed
            else shiftState
        val input = KeyInput(keyCode, shiftState, direction)
        val text = currentLayoutPreset.layoutTable[input]
        val symbol = symbolLayoutPreset.layoutTable[input]
        when(defaultFlickActions[direction]) {
            FlickAction.Default -> onChar(text ?: return false)
            FlickAction.Shifted -> onChar(text ?: return false)
            FlickAction.Symbol -> onChar(symbol ?: return false)
            FlickAction.ShiftedSymbol -> onChar(symbol ?: return false)
            else -> Unit
        }
        return true
    }

    override fun onKeyLongPress(keyCode: Int, metaState: Int): Boolean {
        val keyboardView = keyboardView ?: return false
        val key = keyboardView.findKeys(keyCode).firstOrNull() ?: return false
        val input = KeyInput(keyCode, shiftState, null)
        val baseChar = currentLayoutPreset.layoutTable[input] ?: return false
        val candidates = currentLayoutPreset.moreKeysTable.candidatesFor(baseChar)
        if(candidates.isEmpty()) return false
        keyboardView.popupManager.removePopup(key)
        keyboardView.popupManager.showPopup(key) {
            MoreKeysPopup(keyboardView, key, candidates) {
                this.onChar(it)
                listener.onLongPressStateChanged(false)
            }
        }
        listener.onLongPressStateChanged(true)
        return true
    }

    protected open fun submitCandidates(candidates: List<CandidateView.Candidate>) {
        CoroutineScope(Dispatchers.Main).launch {
            candidateView?.submitList(candidates)
            listener.onExpandedCandidates(candidates)
            val visible = candidates.isNotEmpty()
            listener.onCandidateViewVisibilityChange(visible)
        }
    }

    override fun updateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) = Unit

    protected fun requestHideSelf(flags: Int) {
        listener.onRequestHideSelf(flags)
    }
}
