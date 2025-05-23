package ee.oyatl.ime.fusion

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets.Type
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.view.ContextThemeWrapper

class FusionIMEService: InputMethodService(), IMEMode.Listener, IMEModeSwitcher.Callback {

    private lateinit var imeModeSwitcher: IMEModeSwitcher

    private lateinit var imeView: LinearLayout

    override fun onCreate() {
        super.onCreate()
        val entries = mutableListOf<IMEModeSwitcher.Entry>()
        entries += IMEModeSwitcher.Entry("ABC", LatinIMEMode(this, this))
        entries += IMEModeSwitcher.Entry("한3", KoreanIMEMode.Hangul3Set391(this, this))
        entries += IMEModeSwitcher.Entry("한2", KoreanIMEMode.Hangul2SetKS(this, this))
        entries += IMEModeSwitcher.Entry("あQ", MozcIMEMode.RomajiQwerty(this, this))
        entries += IMEModeSwitcher.Entry("あいう", MozcIMEMode.Kana50OnZu(this, this))
        entries += IMEModeSwitcher.Entry("JIS", MozcIMEMode.KanaJIS(this, this))
        entries += IMEModeSwitcher.Entry("拼音", PinyinIMEMode(this, this))
        entries += IMEModeSwitcher.Entry("注音", ZhuyinIMEMode(this, this))
        entries += IMEModeSwitcher.Entry("倉頡", CangjieIMEMode(this, this))
        entries += IMEModeSwitcher.Entry("越Q", VietIMEMode.Qwerty(this, this))
        entries += IMEModeSwitcher.Entry("越T", VietIMEMode.Telex(this, this))
        imeModeSwitcher = IMEModeSwitcher(entries, this)
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
        val candidateView = imeModeSwitcher.createCandidateView(this) as ViewGroup
        candidateView.addView(imeModeSwitcher.initTabBarView(this))
        imeView.addView(candidateView)
        imeView.addView(imeModeSwitcher.createInputView(this))
        imeView.fitsSystemWindows = true
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) updateNavigationBar()
        onSwitchInputMode(0)
        return imeView
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
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