package ee.oyatl.ime.fusion

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout

class FusionIMEService: InputMethodService(), IMEMode.Listener, ModeSwitcherTabBar.Callback {

    private lateinit var imeModeSwitcher: IMEModeSwitcher
    private lateinit var modeSwitcherTabBar: ModeSwitcherTabBar

    private lateinit var imeView: LinearLayout

    override fun onCreate() {
        super.onCreate()
        val imeModes = mutableListOf<IMEMode>()
        val labels = mutableListOf<String>()
        imeModes += MozcIMEMode(this, this)
        labels += "日"
        imeModes += PinyinIMEMode(this, this)
        labels += "拼"
        imeModes += ZhuyinIMEMode(this, this)
        labels += "注"
        imeModes += CangjieIMEMode(this, this)
        labels += "倉"
        imeModeSwitcher = IMEModeSwitcher(imeModes)
        modeSwitcherTabBar = ModeSwitcherTabBar(labels, this)
    }

    override fun onCreateInputView(): View {
        imeView = LinearLayout(this)
        imeView.orientation = LinearLayout.VERTICAL
        val candidateView = imeModeSwitcher.createCandidateView(this) as ViewGroup
        candidateView.addView(modeSwitcherTabBar.initView(this))
        imeView.addView(candidateView)
        imeView.addView(imeModeSwitcher.createInputView(this))
        onSwitchInputMode(0)
        return imeView
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        imeModeSwitcher.onStart(currentInputConnection, currentInputEditorInfo)
    }

    override fun onFinishInput() {
        imeModeSwitcher.onFinish(currentInputConnection, currentInputEditorInfo)
        super.onFinishInput()
    }

    override fun onLanguageSwitch() {
        val newIndex = (imeModeSwitcher.currentModeIndex + 1) % imeModeSwitcher.modes.size
        onSwitchInputMode(newIndex)
    }

    override fun onSwitchInputMode(index: Int) {
        imeModeSwitcher.switchMode(index, currentInputConnection, currentInputEditorInfo)
        modeSwitcherTabBar.activate(index)
    }

    override fun onCandidateViewVisibilityChange(visible: Boolean) {
        modeSwitcherTabBar.isShown = !visible
    }
}