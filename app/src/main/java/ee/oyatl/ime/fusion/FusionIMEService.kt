package ee.oyatl.ime.fusion

import android.content.SharedPreferences
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets.Type
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.PreferenceManager
import com.android.inputmethod.latin.RichInputMethodManager
import com.android.inputmethod.latin.settings.Settings
import ee.oyatl.ime.fusion.mode.IMEMode
import ee.oyatl.ime.fusion.mode.IMEModeSwitcher
import ee.oyatl.ime.fusion.mode.LatinIMEMode
import ee.oyatl.ime.fusion.mode.PinyinIMEMode
import ee.oyatl.ime.fusion.settings.InputModeSettingsFragment
import ee.oyatl.ime.keyboard.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class FusionIMEService: InputMethodService(), IMEMode.Listener, IMEModeSwitcher.Callback, SharedPreferences.OnSharedPreferenceChangeListener {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var preference: SharedPreferences
    private lateinit var imeModeSwitcher: IMEModeSwitcher
    private lateinit var imeView: LinearLayout

    override fun onCreate() {
        super.onCreate()
        preference = PreferenceManager.getDefaultSharedPreferences(this)
        onInit()
        onLoad()
        preference.registerOnSharedPreferenceChangeListener(this)

        // LatinIMEMode
        Settings.init(this)
        RichInputMethodManager.init(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        onUnload()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        onUnload()
        onInit()
        onLoad()
        if(currentInputConnection != null && currentInputEditorInfo != null)
            imeModeSwitcher.onStart(currentInputConnection, currentInputEditorInfo)
        onResetViews()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onResetViews()
    }

    fun onInit() {
        val jsonArray = preference.getString(InputModeSettingsFragment.PREF_KEY, null) ?: "[]"
        val list = JSONArray(jsonArray).let { array -> (0 until array.length()).map { array.getString(it) } }
        val entries = mutableListOf<IMEModeSwitcher.Entry>()
        val params = list.mapNotNull { item ->
            IMEMode.Params.parse(item)
        }.toMutableList()
        if(params.isEmpty()) params += LatinIMEMode.Params()
        params.forEach { params ->
            entries += IMEModeSwitcher.Entry(params.getShortLabel(this), params.create(this))
        }
        imeModeSwitcher = IMEModeSwitcher(this, entries, this)
    }

    fun onLoad() {
        coroutineScope.launch {
            imeModeSwitcher.entries.forEach { it.imeMode.onLoad(this@FusionIMEService) }
        }
    }

    fun onUnload() {
        imeModeSwitcher.entries.forEach { entry ->
            if(entry.imeMode is PinyinIMEMode) entry.imeMode.stopPinyinDecoderService(this)
        }
    }

    fun onResetViews() {
        imeModeSwitcher.resetInputViews()
        imeModeSwitcher.resetCandidateViews()
        setInputView(onCreateInputView())
    }

    override fun onCreateInputView(): View {
        imeView = LinearLayout(this)
        imeView.orientation = LinearLayout.VERTICAL
        imeView.addView(imeModeSwitcher.createCandidateView())
        imeView.addView(imeModeSwitcher.createInputView())
        imeView.fitsSystemWindows = true

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) updateNavigationBar()
        onSwitchInputMode(0)
        return imeView
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
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

    override fun onFinishInput() {
        imeModeSwitcher.onFinish()
        super.onFinishInput()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if(event.isSystem || event.isCtrlPressed || event.isAltPressed || event.isMetaPressed)
            return super.onKeyDown(keyCode, event)
        imeModeSwitcher.currentMode.onKeyDown(keyCode, event.metaState)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if(event.isSystem || event.isCtrlPressed || event.isAltPressed || event.isMetaPressed)
            return super.onKeyUp(keyCode, event)
        imeModeSwitcher.currentMode.onKeyUp(keyCode, event.metaState)
        return true
    }

    override fun onLanguageSwitch() {
        val newIndex = (imeModeSwitcher.currentModeIndex + 1) % imeModeSwitcher.size
        onSwitchInputMode(newIndex)
    }

    override fun onSwitchInputMode(index: Int) {
        imeModeSwitcher.switchMode(index)
    }

    override fun onCandidateViewVisibilityChange(visible: Boolean) {
        if(visible) imeModeSwitcher.showCandidates()
        else imeModeSwitcher.showTabBar()
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

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun updateNavigationBar() {
        val typedValue = TypedValue()
        val theme = ContextThemeWrapper(this, R.style.Theme_FusionIME_Keyboard).theme
        theme.resolveAttribute(R.attr.backgroundColor, typedValue, true)
        window.window?.decorView?.setOnApplyWindowInsetsListener { view, insets ->
            val statusBarInsets = insets.getInsets(Type.statusBars())
            view.setBackgroundColor(typedValue.data)
            view.setPadding(0, statusBarInsets.top, 0, 0)
            insets
        }
    }
}