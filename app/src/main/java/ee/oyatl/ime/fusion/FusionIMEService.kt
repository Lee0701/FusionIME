package ee.oyatl.ime.fusion

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets.Type
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import com.android.inputmethod.latin.RichInputMethodManager
import com.android.inputmethod.latin.settings.Settings
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.mode.IMEMode
import ee.oyatl.ime.fusion.mode.IMEModeSwitcher
import ee.oyatl.ime.fusion.mode.LatinIMEMode
import ee.oyatl.ime.fusion.mode.PinyinIMEMode
import ee.oyatl.ime.fusion.preference.KeyStrokePreference
import ee.oyatl.ime.fusion.settings.InputModeSettingsFragment
import ee.oyatl.ime.fusion.util.PreferenceUtil
import ee.oyatl.ime.keyboard.PaleViewFilter
import ee.oyatl.ime.theme.KeyboardTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class FusionIMEService: InputMethodService(), IMEMode.Listener, IMEModeSwitcher.Callback, SharedPreferences.OnSharedPreferenceChangeListener {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var preference: SharedPreferences
    private lateinit var keyboardTheme: KeyboardTheme
    private lateinit var imeModeSwitcher: IMEModeSwitcher
    private lateinit var clipboardManager: ClipboardManager
    private var imeView: View? = null
    private val imeViewFilter = PaleViewFilter()
    private var hardwareLanguageKeyStroke: KeyStrokePreference.KeyStroke = KeyStrokePreference.KeyStroke()
    private val pressedKeys: MutableMap<Int, Int> = mutableMapOf()
    private var inputViewActive = false
    private var clipboardListenerRegistered = false
    private val clipboardHandler = Handler(Looper.getMainLooper())
    private val primaryClipChangedListener = ClipboardManager.OnPrimaryClipChangedListener {
        clipboardHandler.post(::updateClipboardCandidate)
    }

    override fun onCreate() {
        super.onCreate()
        preference = PreferenceManager.getDefaultSharedPreferences(this)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        onInit()
        onLoad()
        preference.registerOnSharedPreferenceChangeListener(this)

        updateNavigationBar()

        // LatinIMEMode
        Settings.init(this)
        RichInputMethodManager.init(this)
    }

    override fun onDestroy() {
        inputViewActive = false
        updateClipboardListenerRegistration()
        clipboardHandler.removeCallbacksAndMessages(null)
        imeViewFilter.clear()
        onUnload()
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        onUnload()
        onInit()
        onLoad()
        if(currentInputConnection != null && currentInputEditorInfo != null)
            imeModeSwitcher.onStart(currentInputConnection, currentInputEditorInfo)
        onResetViews()
        updateClipboardListenerRegistration()
        updateClipboardCandidate()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onResetViews()
        updateNavigationBar()
    }

    fun onInit() {
        val jsonArray = preference.getString(InputModeSettingsFragment.PREF_KEY, null) ?: "[]"
        val list = JSONArray(jsonArray).let { array -> (0 until array.length()).map { array.getString(it) } }
        val entries = mutableListOf<IMEModeSwitcher.Entry>()
        val params = list.mapNotNull { item ->
            IMEMode.Params.parse(item)
        }.toMutableList()
        if(params.isEmpty()) params += LatinIMEMode.Params()
        params.forEach { p ->
            val labels = p.getShortLabels(this)
            val others = params.filterNot { it == p }.flatMap { it.getShortLabels(this) }.toSet()
            val shortLabel = labels.firstOrNull { it !in others } ?: labels.last()
            entries += IMEModeSwitcher.Entry(shortLabel, p.create(this))
        }
        keyboardTheme = PreferenceUtil.getKeyboardTheme(this)
        imeModeSwitcher = IMEModeSwitcher(this, entries, keyboardTheme, this)
    }

    fun onLoad() {
        hardwareLanguageKeyStroke = KeyStrokePreference.KeyStroke
            .parse(preference.getString("hardware_language_key", null) ?: "")
            ?: hardwareLanguageKeyStroke
        coroutineScope.launch {
            imeModeSwitcher.entries.forEach { it.imeMode.onLoad(this@FusionIMEService) }
        }
    }

    fun onUnload() {
        coroutineScope.launch {
            imeModeSwitcher.entries.forEach { it.imeMode.onUnload(this@FusionIMEService) }
        }
    }

    fun onResetViews() {
        imeViewFilter.clear()
        keyboardTheme = PreferenceUtil.getKeyboardTheme(this)
        imeModeSwitcher.keyboardTheme = keyboardTheme
        imeModeSwitcher.resetInputViews()
        imeModeSwitcher.resetCandidateViews()
        setInputView(onCreateInputView())
    }

    override fun onCreateInputView(): View {
        val imeView = imeModeSwitcher.createInputView()
        onSwitchInputMode(0)
        this.imeView = imeView
        return imeView
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        imeModeSwitcher.setClipboardCandidate(null)
        if(attribute != null) {
            val cls = attribute.inputType and EditorInfo.TYPE_MASK_CLASS
            val variation = attribute.inputType and EditorInfo.TYPE_MASK_VARIATION
            val passwordVariations = setOf(
                EditorInfo.TYPE_TEXT_VARIATION_PASSWORD,
                EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            )
            if(cls == EditorInfo.TYPE_CLASS_TEXT && variation in passwordVariations) {
                val englishModeIndex = imeModeSwitcher.entries.indexOfFirst { it.imeMode is LatinIMEMode }
                if(englishModeIndex != -1) imeModeSwitcher.switchMode(englishModeIndex)
            }
        }
        imeModeSwitcher.onStart(currentInputConnection, currentInputEditorInfo)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        inputViewActive = true
        updateClipboardListenerRegistration()
        updateClipboardCandidate()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        inputViewActive = false
        updateClipboardListenerRegistration()
        imeModeSwitcher.setClipboardCandidate(null)
        super.onFinishInputView(finishingInput)
    }

    override fun onFinishInput() {
        inputViewActive = false
        updateClipboardListenerRegistration()
        imeModeSwitcher.setClipboardCandidate(null)
        imeModeSwitcher.onFinish()
        super.onFinishInput()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if(hardwareLanguageKeyStroke.matches(event)) {
            onLanguageSwitch()
            return true
        }
        if(event.isSystem) return super.onKeyDown(keyCode, event)

        val logicalKey = imeModeSwitcher.currentMode.translateKeyCode(event.keyCode)
        val translatedEvent = cloneEvent(event, logicalKey)
        pressedKeys += event.keyCode to logicalKey

        val isAltGraphPressed = (event.metaState and KeyEvent.META_ALT_RIGHT_ON != 0) &&
                imeModeSwitcher.currentMode.isAltGraphCapable()

        if(event.isSystem || event.isCtrlPressed || (event.isAltPressed && !isAltGraphPressed) || event.isMetaPressed) {
            currentInputConnection.sendKeyEvent(translatedEvent)
            return true
        }

        val result = imeModeSwitcher.currentMode.onKeyDown(logicalKey, translatedEvent.metaState)
        if(!result) {
            currentInputConnection.sendKeyEvent(translatedEvent)
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if(hardwareLanguageKeyStroke.matches(event)) return true
        if(event.isSystem) return super.onKeyUp(keyCode, event)

        val logicalKey = pressedKeys.remove(event.keyCode)
            ?: imeModeSwitcher.currentMode.translateKeyCode(event.keyCode)
        val translatedEvent = cloneEvent(event, logicalKey)

        val isAltGraphPressed = (event.metaState and KeyEvent.META_ALT_RIGHT_ON != 0) &&
                imeModeSwitcher.currentMode.isAltGraphCapable()

        if(event.isSystem || event.isCtrlPressed || (event.isAltPressed && !isAltGraphPressed) || event.isMetaPressed) {
            currentInputConnection.sendKeyEvent(translatedEvent)
            return true
        }

        val result = imeModeSwitcher.currentMode.onKeyUp(logicalKey, translatedEvent.metaState)
        if(!result) {
            currentInputConnection.sendKeyEvent(translatedEvent)
        }
        return true
    }

    private fun cloneEvent(event: KeyEvent, keyCode: Int): KeyEvent {
        return KeyEvent(
            event.downTime,
            event.eventTime,
            event.action,
            keyCode,
            event.repeatCount,
            event.metaState,
            event.deviceId,
            event.scanCode,
            event.flags,
            event.source
        )
    }

    override fun onLanguageSwitch() {
        val newIndex = (imeModeSwitcher.currentModeIndex + 1) % imeModeSwitcher.size
        onSwitchInputMode(newIndex)
    }

    override fun onSwitchInputMode(index: Int) {
        imeModeSwitcher.switchMode(index)
    }

    override fun onSwitchInputMethod(id: String, subtype: InputMethodSubtype) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) switchInputMethod(id, subtype)
        else {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val token = window?.window?.attributes?.token
            @Suppress("DEPRECATION")
            if(token != null) imm.setInputMethodAndSubtype(token, id, subtype)
        }
    }

    override fun onClipboardCandidateSelected(text: String) {
        val editorInfo = currentInputEditorInfo ?: return
        if(!clipboardCandidateEnabled() || isSensitiveInputType(editorInfo.inputType)) return
        currentInputConnection?.commitText(text, 1)
    }

    override fun onClipboardCandidateClearRequested(): Boolean {
        val editorInfo = currentInputEditorInfo ?: return false
        if(!clipboardCandidateEnabled() || isSensitiveInputType(editorInfo.inputType)) return false
        val cleared = try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboardManager.clearPrimaryClip()
            } else {
                clipboardManager.setPrimaryClip(ClipData.newPlainText(null, ""))
            }
            true
        } catch(_: SecurityException) {
            false
        }
        if(cleared) imeModeSwitcher.setClipboardCandidate(null)
        return cleared
    }

    override fun onCandidateViewVisibilityChange(visible: Boolean) {
        if(visible) imeModeSwitcher.showCandidates()
        else imeModeSwitcher.showTabBar()
    }

    override fun onExpandedCandidates(candidates: List<CandidateView.Candidate>) {
        imeModeSwitcher.expandedCandidateView?.submitList(candidates)
        if(candidates.isEmpty()) imeModeSwitcher.collapseCandidateView()
    }

    override fun onLongPressStateChanged(active: Boolean) {
        if(active) {
            imeView?.let(imeViewFilter::apply)
        } else {
            imeViewFilter.clear()
        }
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        imeModeSwitcher.currentMode.updateSelection(
            oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd
        )
    }

    override fun onRequestHideSelf(flags: Int) {
        this.requestHideSelf(flags)
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        super.onEvaluateFullscreenMode()
        return false
    }

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return true
    }

    private fun updateClipboardListenerRegistration() {
        val shouldRegister = inputViewActive && clipboardCandidateEnabled()
        if(shouldRegister && !clipboardListenerRegistered) {
            clipboardManager.addPrimaryClipChangedListener(primaryClipChangedListener)
            clipboardListenerRegistered = true
        } else if(!shouldRegister && clipboardListenerRegistered) {
            clipboardManager.removePrimaryClipChangedListener(primaryClipChangedListener)
            clipboardListenerRegistered = false
        }
    }

    private fun updateClipboardCandidate() {
        val editorInfo = currentInputEditorInfo
        val text = if(inputViewActive && clipboardCandidateEnabled() && editorInfo != null) {
            readClipboardCandidateText(clipboardManager, editorInfo.inputType)
        } else null
        imeModeSwitcher.setClipboardCandidate(text)
    }

    private fun clipboardCandidateEnabled(): Boolean {
        return preference.getBoolean(PREF_CLIPBOARD_CANDIDATE_ENABLED, true)
    }

    private fun updateNavigationBar() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val window = window.window ?: return
        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.enableEdgeToEdge(window)
        val darkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        // Update colors
        window.decorView.setBackgroundColor(getColor(keyboardTheme.backgroundColor))
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = !darkMode
        // Get system bar insets after keyboard is displayed
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, windowInsets ->
            val insets = windowInsets.getInsets(Type.systemBars() and Type.ime().inv())
            // Apply bottom padding to root input view
            // Not applying to the decor view since different behaviours between devices
            val imeView = imeView
            // Workaround for some devices that applies bottom padding also for navbar buttons
            imeView?.setPadding(0, 0, 0, insets.bottom)
            view.setPadding(0, 0, 0, if(imeView != null) 0 else insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    companion object {
        private const val PREF_CLIPBOARD_CANDIDATE_ENABLED = "clipboard_candidate_enabled"
    }
}
