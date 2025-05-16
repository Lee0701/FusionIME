package ee.oyatl.ime.fusion

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo

class FusionIMEService: InputMethodService(), IMEMode.Listener, IMEModeSwitcher.Callback {

    private lateinit var imeModeSwitcher: IMEModeSwitcher

    override fun onCreate() {
        super.onCreate()
        val imeModes = mutableListOf<IMEMode>()
        imeModes += MozcIMEMode(this, this)
        imeModes += PinyinIMEMode(this, this)
        imeModes += ZhuyinIMEMode(this, this)
        imeModes += CangjieIMEMode(this, this)
        imeModeSwitcher = IMEModeSwitcher(imeModes, this)
    }

    override fun onCreateInputView(): View {
        return imeModeSwitcher.initView(this)
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
        imeModeSwitcher.switchMode(newIndex, currentInputConnection, currentInputEditorInfo)
    }

    override fun onInputViewChanged(inputView: View) {
        setInputView(inputView)
    }
}