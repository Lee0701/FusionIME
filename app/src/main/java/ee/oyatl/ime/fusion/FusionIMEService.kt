package ee.oyatl.ime.fusion

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo

class FusionIMEService: InputMethodService() {

    private lateinit var imeMode: IMEMode

    override fun onCreate() {
        super.onCreate()
        imeMode = MozcIMEMode(this)
    }

    override fun onCreateInputView(): View {
        return imeMode.initView(this)
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        imeMode.onStart(currentInputConnection, currentInputEditorInfo)
    }

    override fun onFinishInput() {
        super.onFinishInput()
        imeMode.onFinish(currentInputConnection, currentInputEditorInfo)
    }
}