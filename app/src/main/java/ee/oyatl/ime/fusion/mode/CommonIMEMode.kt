package ee.oyatl.ime.fusion.mode

import android.annotation.SuppressLint
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
import ee.oyatl.ime.fusion.Feature
import ee.oyatl.ime.fusion.FlickAction
import ee.oyatl.ime.fusion.KeyEventUtil
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.layout.LayoutQwerty
import ee.oyatl.ime.fusion.layout.LayoutSymbol
import ee.oyatl.ime.fusion.layout.MobileKeyboard
import ee.oyatl.ime.fusion.layout.MobileKeyboardRows
import ee.oyatl.ime.fusion.layout.NumberKeyboard
import ee.oyatl.ime.fusion.layout.TabletKeyboard
import ee.oyatl.ime.fusion.layout.TabletKeyboardRows
import ee.oyatl.ime.keyboard.DefaultKeyboardView
import ee.oyatl.ime.keyboard.FlickKeyCode
import ee.oyatl.ime.keyboard.KeyboardConfiguration
import ee.oyatl.ime.keyboard.KeyboardParams
import ee.oyatl.ime.keyboard.KeyboardState
import ee.oyatl.ime.keyboard.KeyboardTemplate
import ee.oyatl.ime.keyboard.KeyboardView
import ee.oyatl.ime.keyboard.LayoutTable
import ee.oyatl.ime.keyboard.SwitcherKeyboardView
import ee.oyatl.ime.keyboard.listener.CompoundKeyboardListener
import ee.oyatl.ime.keyboard.listener.KeyFeedbackManager
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.listener.ShiftStateManager
import ee.oyatl.ime.keyboard.popup.DefaultPopupManager
import ee.oyatl.ime.keyboard.touchhandler.FlickDirection
import ee.oyatl.ime.keyboard.touchhandler.FlickTouchHandler
import ee.oyatl.ime.keyboard.touchhandler.SeekTouchHandler
import ee.oyatl.ime.keyboard.touchhandler.TouchHandler
import kotlin.math.roundToInt

abstract class CommonIMEMode(
    private val listener: IMEMode.Listener
): IMEMode, KeyboardListener, CandidateView.Listener {
    protected val keyCharacterMap: KeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

    open val textLayoutTable: LayoutTable = LayoutTable.from(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY)
    open val symbolLayoutTable: LayoutTable = LayoutTable.from(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + LayoutSymbol.TABLE_G)
    open val numberLayoutTable: LayoutTable = LayoutTable(mapOf())

    open val textKeyboardTemplate: KeyboardTemplate = KeyboardTemplate.ByScreenMode(
        mobile = KeyboardTemplate.Basic(
            configuration = KeyboardConfiguration(
                MobileKeyboard.alphabetic(),
                MobileKeyboard.bottom()
            ),
            contentRows = MobileKeyboardRows.DEFAULT
        ),
        tablet = KeyboardTemplate.Basic(
            configuration = KeyboardConfiguration(
                TabletKeyboard.alphabetic(),
                TabletKeyboard.bottom()
            ),
            contentRows = TabletKeyboardRows.DEFAULT
        )
    )
    open val symbolKeyboardTemplate: KeyboardTemplate = KeyboardTemplate.ByScreenMode(
        mobile = KeyboardTemplate.Basic(
            configuration = KeyboardConfiguration(
                MobileKeyboard.alphabetic(semicolon = true),
                MobileKeyboard.bottom(languageKeyCode = KeyEvent.KEYCODE_NUM)
            ),
            contentRows = MobileKeyboardRows.SEMICOLON,
        ),
        tablet = KeyboardTemplate.Basic(
            configuration = KeyboardConfiguration(
                TabletKeyboard.alphabetic(semicolon = true),
                TabletKeyboard.bottom(languageKeyCode = KeyEvent.KEYCODE_NUM)
            ),
            contentRows = TabletKeyboardRows.SEMICOLON
        )
    )
    open val numberKeyboardTemplate: KeyboardTemplate = KeyboardTemplate.ByScreenMode(
        mobile = KeyboardTemplate.Basic(
            configuration = NumberKeyboard.mobile(),
            contentRows = emptyList(),
        ),
        tablet = KeyboardTemplate.Basic(
            configuration = NumberKeyboard.tablet(),
            contentRows = emptyList(),
        )
    )

    val currentLayoutTable: LayoutTable get() = when(symbolState) {
        KeyboardState.Symbol.Text -> textLayoutTable
        KeyboardState.Symbol.Symbol -> symbolLayoutTable
        KeyboardState.Symbol.Number -> numberLayoutTable
    }

    open val keyLabels: Map<Int, String>
        get() = currentLayoutTable.map.mapValues { (_, v) -> v.forShiftState(shiftState).toChar().toString() }

    protected var keyboardView: KeyboardView? = null
    protected var candidateView: CandidateView? = null

    var symbolState: KeyboardState.Symbol = KeyboardState.Symbol.Text
    var shiftState: KeyboardState.Shift = KeyboardState.Shift.Released

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
            else -> util?.sendDownUpKeyEvents(keyCode)
        }
    }

    override suspend fun onLoad(context: Context) {
        val preference = PreferenceManager.getDefaultSharedPreferences(context)

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
        keyboardView?.onReset()
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

    private fun getOrientationSuffix(context: Context): String {
        val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val suffix = if(landscape) "_landscape" else "_portrait"
        return suffix
    }

    private fun getOrientationInteger(context: Context, key: String): Float {
        val preference = PreferenceManager.getDefaultSharedPreferences(context)
        val suffix = getOrientationSuffix(context)
        @SuppressLint("DiscouragedApi")
        val defaultId = context.resources.getIdentifier("${key}${suffix}_default", "integer", context.packageName)
        val defaultValue = context.resources.getInteger(defaultId).toFloat()
        val value = preference.getFloat("${key}${suffix}", defaultValue)
        return value
    }

    private fun getOrientationBoolean(context: Context, key: String): Boolean {
        val preference = PreferenceManager.getDefaultSharedPreferences(context)
        val suffix = getOrientationSuffix(context)
        @SuppressLint("DiscouragedApi")
        val defaultId = context.resources.getIdentifier("${key}${suffix}_default", "bool", context.packageName)
        val defaultValue = context.resources.getBoolean(defaultId)
        val value = preference.getBoolean("${key}${suffix}", defaultValue)
        return value
    }

    override fun createInputView(context: Context): View {
        val preference = PreferenceManager.getDefaultSharedPreferences(context)

        val defaultScreenMode = context.resources.getString(R.string.screen_mode_default)
        val screenMode = KeyboardState.ScreenMode.valueOf(preference.getString("screen_mode", null) ?: defaultScreenMode)
        val rowHeightDIP = getOrientationInteger(context, "keyboard_height")
        val height = (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, rowHeightDIP, context.resources.displayMetrics) * 4).roundToInt()
        val split = Feature.SplitKeyboard.availableInCurrentVersion && getOrientationBoolean(context, "split_keyboard")
        val splitRatio = if(split) getOrientationInteger(context, "split_ratio") else 0f
        val splitWidthDIP = context.resources.configuration.screenWidthDp / 100f * splitRatio
        val splitWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, splitWidthDIP, context.resources.displayMetrics).roundToInt()
        val showPreviewPopup = preference.getBoolean("preview_popup", true)
        val sound = preference.getBoolean("sound_feedback", true)
        val haptic = preference.getBoolean("haptic_feedback", true)
        val duration = preference.getFloat("vibration_duration", 10f).toLong()
        val soundVolume = if(sound) 1f else 0f
        val vibrationDuration = if(haptic) duration else 0L
        val params = KeyboardParams(
            screenMode = screenMode,
            height = height,
            splitWidth = splitWidth,
            soundFeedback = false,
            hapticFeedback = false,
            soundVolume = soundVolume,
            vibrationDuration = vibrationDuration,
            previewPopups = showPreviewPopup,
            shiftLockDelay = 300,
            shiftAutoRelease = true,
            repeatDelay = 300,
            repeatInterval = 30,
        )

        val textKeyboardParams = params.copy()
        val symbolKeyboardParams = params.copy(shiftAutoRelease = false)
        val numberKeyboardParams = params.copy(shiftAutoRelease = false, splitWidth = 0)

        val textKeyboard = textKeyboardTemplate.inflate(textKeyboardParams)
        val symbolKeyboard = symbolKeyboardTemplate.inflate(symbolKeyboardParams)
        val numberKeyboard = numberKeyboardTemplate.inflate(numberKeyboardParams)

        val textKeyboardView = DefaultKeyboardView(context, null).also {
            it.keyboard = textKeyboard
            it.listener = createKeyboardListener(context, textKeyboardParams)
            it.touchHandler = createTouchHandler(it, context)
            if(params.previewPopups) it.popupManager = DefaultPopupManager(it, it)
        }
        val symbolKeyboardView = DefaultKeyboardView(context, null).also {
            it.keyboard = symbolKeyboard
            it.listener = createKeyboardListener(context, symbolKeyboardParams)
            it.touchHandler = createTouchHandler(it, context)
            if(params.previewPopups) it.popupManager = DefaultPopupManager(it, it)
        }
        val numberKeyboardView = DefaultKeyboardView(context, null).also {
            it.keyboard = numberKeyboard
            it.listener = createKeyboardListener(context, numberKeyboardParams)
            it.touchHandler = createTouchHandler(it, context)
            if(params.previewPopups) it.popupManager = DefaultPopupManager(it, it)
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
            listener = this@CommonIMEMode
        }
        return candidateView as View
    }

    override fun getInputView(): View? {
        updateInputView()
        return keyboardView
    }

    protected fun updateInputView() {
        val keyboardView = keyboardView
        if(keyboardView is SwitcherKeyboardView) {
            // Update keyboard view states
            keyboardView.state = symbolState
        }
        if(keyboardView != null) {
            keyboardView.labels = this.keyLabels
            val shiftIcon = when(shiftState) {
                KeyboardState.Shift.Released -> ee.oyatl.ime.keyboard.R.drawable.keyic_shift
                KeyboardState.Shift.Pressed -> ee.oyatl.ime.keyboard.R.drawable.keyic_shift_pressed
                KeyboardState.Shift.Locked -> ee.oyatl.ime.keyboard.R.drawable.keyic_shift_locked
            }
            val icons = mapOf(
                KeyEvent.KEYCODE_SHIFT_LEFT to shiftIcon,
                KeyEvent.KEYCODE_SHIFT_RIGHT to shiftIcon
            )
            keyboardView.icons = icons
        }
    }

    open fun createKeyboardListener(context: Context, params: KeyboardParams): KeyboardListener {
        return CompoundKeyboardListener(
            ShiftStateManager(this, params),
            KeyFeedbackManager(context, params)
        )
    }

    open fun createTouchHandler(keyboardView: TouchHandler.KeyboardViewInterface, context: Context): TouchHandler {
        val preference = PreferenceManager.getDefaultSharedPreferences(context)
        val touchMode = preference.getString("touch_mode", "seek")
        if(touchMode == "flick" && Feature.TouchMode.availableInCurrentVersion) {
            val defaultValue = context.resources.getInteger(R.integer.flick_sensitivity_default).toFloat()
            val flickSensitivity = preference.getFloat("flick_sensitivity", defaultValue).toInt()
            return FlickTouchHandler(keyboardView, flickSensitivity, diagonal = false, multiFlick = false)
        } else {
            return SeekTouchHandler(keyboardView)
        }
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

    override fun onKeyDown(keyCode: Int, metaState: Int) {
        if(keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            shiftState = KeyboardState.Shift.Pressed
        } else if(keyCode == KeyEvent.KEYCODE_CAPS_LOCK) {
            shiftState = KeyboardState.Shift.Locked
        } else if(keyCode < 0) {
            onChar(-keyCode)
        } else if(keyCode > KeyEvent.getMaxKeyCode() || keyCharacterMap.isPrintingKey(keyCode)) {
            val maskedKeyCode = keyCode and FlickKeyCode.MASK_KEYCODE
            val default = keyCharacterMap.get(maskedKeyCode, metaState)
            if(keyCode and FlickKeyCode.FLAG_FLICK != 0) {
                val direction = FlickDirection.valueOfKeycode(keyCode)
                when(defaultFlickActions[direction]) {
                    FlickAction.Default -> onChar(
                        currentLayoutTable[maskedKeyCode]?.forShiftState(shiftState) ?: default)
                    FlickAction.Shifted -> onChar(
                        currentLayoutTable[maskedKeyCode]?.forShiftState(KeyboardState.Shift.Pressed) ?: default)
                    FlickAction.Symbol -> onChar(
                        symbolLayoutTable[maskedKeyCode]?.forShiftState(shiftState) ?: default)
                    FlickAction.ShiftedSymbol -> onChar(
                        symbolLayoutTable[maskedKeyCode]?.forShiftState(KeyboardState.Shift.Pressed) ?: default)
                    else -> Unit
                }
            } else {
                onChar(
                    currentLayoutTable[keyCode]?.forShiftState(shiftState) ?: default)
            }
        } else {
            onSpecial(keyCode)
        }
        updateInputView()
    }

    override fun onKeyUp(keyCode: Int, metaState: Int) {
        if(keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            shiftState = KeyboardState.Shift.Released
        }
        updateInputView()
    }

    protected fun submitCandidates(candidates: List<CandidateView.Candidate>) {
        candidateView?.submitList(candidates)
        val visible = candidates.isNotEmpty()
        listener.onCandidateViewVisibilityChange(visible)
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