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
import ee.oyatl.ime.keyboard.DefaultKeyboardInflater
import ee.oyatl.ime.keyboard.KeyCodeMapper
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.KeyboardConfiguration
import ee.oyatl.ime.keyboard.KeyboardListener
import ee.oyatl.ime.keyboard.KeyboardParams
import ee.oyatl.ime.keyboard.KeyboardState
import ee.oyatl.ime.keyboard.KeyboardViewManager
import ee.oyatl.ime.keyboard.LayoutTable
import ee.oyatl.ime.keyboard.layout.KeyboardConfigurations
import ee.oyatl.ime.keyboard.layout.KeyboardTemplates
import ee.oyatl.ime.keyboard.layout.LayoutExt
import ee.oyatl.ime.keyboard.layout.LayoutQwerty
import kotlin.collections.plus
import kotlin.math.roundToInt

abstract class CommonIMEMode(
    private val listener: IMEMode.Listener
): IMEMode, KeyboardListener, CandidateView.Listener {
    protected val keyCharacterMap: KeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

    open val keyCodeMapper: KeyCodeMapper = KeyCodeMapper(mapOf())
    open val keyboardConfiguration: KeyboardConfiguration =
        KeyboardConfigurations.mobileAlpha() + KeyboardConfigurations.mobileBottom()
    open val keyboardTemplate: List<String> = KeyboardTemplates.MOBILE
    open val layoutTable: LayoutTable = LayoutTable.from(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY)

    protected var keyboard: Keyboard? = null
    protected var keyboardView: KeyboardViewManager? = null
    protected var candidateView: CandidateView? = null

    var symbolState: KeyboardState.Symbol = KeyboardState.Symbol.Text
    var shiftState: KeyboardState.Shift = KeyboardState.Shift.Released
    val metaState: Int get() {
        val shift = when(shiftState) {
            KeyboardState.Shift.Released -> 0
            KeyboardState.Shift.Pressed -> KeyEvent.META_SHIFT_ON
            KeyboardState.Shift.Locked -> KeyEvent.META_CAPS_LOCK_ON
        }
        return shift
    }

    protected var util: KeyEventUtil? = null
        private set
    protected var passwordField: Boolean = false
    protected val currentInputConnection: InputConnection? get() = util?.currentInputConnection
    protected val currentInputEditorInfo: EditorInfo? get() = util?.currentInputEditorInfo

    abstract fun onChar(codePoint: Int)
    abstract fun onSpecial(keyCode: Int)

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
        val sound = preference.getBoolean("sound_feedback", true)
        val haptic = preference.getBoolean("haptic_feedback", true)
        val duration = preference.getFloat("vibration_duration", 10f).toLong()
        val soundVolume = if(sound) 1f else 0f
        val vibrationDuration = if(haptic) duration else 0L
        val params = KeyboardParams(
            height = height,
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
        val defaultScreenMode = context.resources.getString(R.string.screen_mode_default)
        val screenMode = KeyboardState.ScreenMode.valueOf(preference.getString("screen_mode", null) ?: defaultScreenMode)

        val contentRows = keyboardTemplate.map { row -> row.map { KeyCodeMapper.keyCharToKeyCode(it) } }

        val keyboardInflater = DefaultKeyboardInflater(params, keyCodeMapper)
        val keyboard = keyboardInflater.inflate(keyboardConfiguration, contentRows)

        val keyboardView = keyboard.createView(context, this)
        updateInputView()
        this.keyboard = keyboard
        this.keyboardView = keyboardView
        return keyboardView.view
    }

    override fun createCandidateView(context: Context): View {
        candidateView = ScrollingCandidateView(context, null).apply {
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
        if(keyboardView != null) {
            val labels = layoutTable.map.mapValues { (_, v) -> v.forShiftState(shiftState).toChar().toString() }
            keyboardView.setLabels(labels)
            val shiftIcon = when(shiftState) {
                KeyboardState.Shift.Released -> ee.oyatl.ime.keyboard.R.drawable.keyic_shift
                KeyboardState.Shift.Pressed -> ee.oyatl.ime.keyboard.R.drawable.keyic_shift_pressed
                KeyboardState.Shift.Locked -> ee.oyatl.ime.keyboard.R.drawable.keyic_shift_locked
            }
            val icons = mapOf<Int, Int>(
                KeyEvent.KEYCODE_SHIFT_LEFT to shiftIcon,
                KeyEvent.KEYCODE_SHIFT_RIGHT to shiftIcon
            )
            keyboardView.setIcons(icons)
        }
    }

    protected fun setPreferredKeyboard(editorInfo: EditorInfo) {
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
        if(keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            shiftState = KeyboardState.Shift.Pressed
        } else if(keyCode == KeyEvent.KEYCODE_CAPS_LOCK) {
            shiftState = KeyboardState.Shift.Locked
        } else if(keyCode > KeyEvent.getMaxKeyCode() || keyCharacterMap.isPrintingKey(keyCode)) {
            onChar(
                layoutTable[keyCode]?.forShiftState(shiftState)
                    ?: keyCharacterMap.get(keyCode, metaState)
            )
        } else {
            handleSpecialKey(keyCode)
        }
        updateInputView()
    }

    override fun onKeyUp(keyCode: Int, metaState: Int) {
        if(keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            shiftState = KeyboardState.Shift.Released
        }
        updateInputView()
    }

    protected fun handleSpecialKey(keyCode: Int) {
        when(keyCode) {
            KeyEvent.KEYCODE_LANGUAGE_SWITCH -> listener.onLanguageSwitch()
            KeyEvent.KEYCODE_SYM -> {
                symbolState =
                    if(symbolState != KeyboardState.Symbol.Symbol) KeyboardState.Symbol.Symbol
                    else KeyboardState.Symbol.Text
                shiftState = KeyboardState.Shift.Released
            }
            KeyEvent.KEYCODE_NUM -> {
                symbolState =
                    if(symbolState != KeyboardState.Symbol.Number) KeyboardState.Symbol.Number
                    else KeyboardState.Symbol.Text
                shiftState = KeyboardState.Shift.Released
            }
            else -> onSpecial(keyCode)
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
}