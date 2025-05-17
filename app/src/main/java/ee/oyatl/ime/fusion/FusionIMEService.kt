package ee.oyatl.ime.fusion

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout

class FusionIMEService: InputMethodService(), IMEMode.Listener, IMEModeSwitcher.Callback {

    private lateinit var imeModeSwitcher: IMEModeSwitcher

    private lateinit var imeView: LinearLayout

    override fun onCreate() {
        super.onCreate()
        val entries = mutableListOf<IMEModeSwitcher.Entry>()
        entries += IMEModeSwitcher.Entry("あQ", MozcIMEMode.RomajiQwerty(this, this))
        entries += IMEModeSwitcher.Entry("あC", MozcIMEMode.RomajiColemak(this, this))
        entries += IMEModeSwitcher.Entry("あいう", MozcIMEMode.Kana50OnZu(this, this))
        entries += IMEModeSwitcher.Entry("拼音", PinyinIMEMode(this, this))
        entries += IMEModeSwitcher.Entry("注音", ZhuyinIMEMode(this, this))
        entries += IMEModeSwitcher.Entry("倉頡", CangjieIMEMode(this, this))
        imeModeSwitcher = IMEModeSwitcher(entries, this)
    }

    override fun onCreateInputView(): View {
        imeView = LinearLayout(this)
        imeView.orientation = LinearLayout.VERTICAL
        val candidateView = imeModeSwitcher.createCandidateView(this) as ViewGroup
        candidateView.addView(imeModeSwitcher.initTabBarView(this))
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
        imeModeSwitcher.onFinish()
        super.onFinishInput()
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
}