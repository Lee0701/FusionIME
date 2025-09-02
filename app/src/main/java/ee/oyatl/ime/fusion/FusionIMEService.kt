package ee.oyatl.ime.fusion

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets.Type
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.PreferenceManager
import ee.oyatl.ime.fusion.mode.CangjieIMEMode
import ee.oyatl.ime.fusion.mode.IMEMode
import ee.oyatl.ime.fusion.mode.IMEModeSwitcher
import ee.oyatl.ime.fusion.mode.KoreanIMEMode
import ee.oyatl.ime.fusion.mode.LatinIMEMode
import ee.oyatl.ime.fusion.mode.MozcIMEMode
import ee.oyatl.ime.fusion.mode.PinyinIMEMode
import ee.oyatl.ime.fusion.mode.VietIMEMode
import ee.oyatl.ime.fusion.mode.ZhuyinIMEMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FusionIMEService: InputMethodService(), IMEMode.Listener, IMEModeSwitcher.Callback {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private lateinit var imeModeSwitcher: IMEModeSwitcher
    private lateinit var imeView: LinearLayout

    override fun onCreate() {
        super.onCreate()
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val list = pref.getStringSet("input_modes", null).orEmpty()
        val entries = mutableListOf<IMEModeSwitcher.Entry>()
        if("qwerty" in list || list.isEmpty())
            entries += IMEModeSwitcher.Entry("ABC", LatinIMEMode(this, this))
        if("ko_391" in list)
            entries += IMEModeSwitcher.Entry("한3", KoreanIMEMode.Hangul3Set391(this))
        if("ko_ks" in list)
            entries += IMEModeSwitcher.Entry("한2", KoreanIMEMode.Hangul2SetKS(this))
        if("ja_qwerty" in list)
            entries += IMEModeSwitcher.Entry("あQ", MozcIMEMode.RomajiQwerty(this))
        if("ja_50onzu" in list)
            entries += IMEModeSwitcher.Entry("あいう", MozcIMEMode.Kana50OnZu(this))
        if("ja_jis" in list)
            entries += IMEModeSwitcher.Entry("JIS", MozcIMEMode.KanaJIS(this))
        if("zh_pinyin" in list)
            entries += IMEModeSwitcher.Entry("拼音", PinyinIMEMode(this))
        if("zh_zhuyin" in list)
            entries += IMEModeSwitcher.Entry("注音", ZhuyinIMEMode(this))
        if("zh_cangjie" in list)
            entries += IMEModeSwitcher.Entry("倉頡", CangjieIMEMode(this))
        if("vi_qwerty" in list)
            entries += IMEModeSwitcher.Entry("越Q", VietIMEMode.Qwerty(this))
        if("vi_telex" in list)
            entries += IMEModeSwitcher.Entry("越T", VietIMEMode.Telex(this))
        imeModeSwitcher = IMEModeSwitcher(this, entries, this)

        coroutineScope.launch {
            entries.forEach { it.imeMode.onLoad(this@FusionIMEService) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        imeModeSwitcher.entries.forEach { entry ->
            if(entry.imeMode is PinyinIMEMode) entry.imeMode.stopPinyinDecoderService(this)
        }
    }

    override fun onCreateInputView(): View {
        imeView = LinearLayout(this)
        imeView.orientation = LinearLayout.VERTICAL
        val candidateSwitchView = FrameLayout(this)
        candidateSwitchView.addView(imeModeSwitcher.initTabBarView(this))
        candidateSwitchView.addView(imeModeSwitcher.createCandidateView())
        imeView.addView(candidateSwitchView)
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
        if(event.keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || event.keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT)
            return super.onKeyDown(keyCode, event)
        imeModeSwitcher.currentMode.onKeyDown(keyCode, event.metaState)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if(event.isSystem || event.isCtrlPressed || event.isAltPressed || event.isMetaPressed)
            return super.onKeyUp(keyCode, event)
        if(event.keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || event.keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT)
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
        imeModeSwitcher.isShown = !visible
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
        val theme = ContextThemeWrapper(this, ee.oyatl.ime.keyboard.R.style.Theme_FusionIME_Keyboard).theme
        theme.resolveAttribute(ee.oyatl.ime.keyboard.R.attr.backgroundColor, typedValue, true)
        window.window?.decorView?.setOnApplyWindowInsetsListener { view, insets ->
            val statusBarInsets = insets.getInsets(Type.statusBars())
            view.setBackgroundColor(typedValue.data)
            view.setPadding(0, statusBarInsets.top, 0, 0)
            insets
        }
    }
}