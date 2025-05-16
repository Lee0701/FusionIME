package ee.oyatl.ime.fusion

import android.content.Context
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

class IMEModeSwitcher(
    val modes: List<IMEMode>,
    private val callback: Callback
) {
    var currentModeIndex: Int = 0
        private set
    private val currentMode: IMEMode get() = modes[currentModeIndex]

    fun initView(context: Context): View {
        modes.forEach { it.initView(context) }
        return currentMode.getView()
    }

    fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo) {
        currentMode.onStart(inputConnection, editorInfo)
    }

    fun onFinish(inputConnection: InputConnection, editorInfo: EditorInfo) {
        currentMode.onFinish(inputConnection, editorInfo)
    }

    fun switchMode(index: Int, inputConnection: InputConnection, editorInfo: EditorInfo) {
        currentMode.onFinish(inputConnection, editorInfo)
        currentModeIndex = index
        currentMode.onStart(inputConnection, editorInfo)
        callback.onInputViewChanged(currentMode.getView())
    }

    interface Callback {
        fun onInputViewChanged(inputView: View)
    }
}