package ee.oyatl.ime.fusion

import android.content.Context
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout

class IMEModeSwitcher(
    val modes: List<IMEMode>
) {
    var currentModeIndex: Int = 0
        private set
    private val currentMode: IMEMode get() = modes[currentModeIndex]

    private lateinit var inputView: FrameLayout
    private lateinit var candidateView: FrameLayout

    fun createInputView(context: Context): View {
        inputView = FrameLayout(context)
        modes.forEach { inputView.addView(it.createInputView(context)) }
        return inputView
    }

    fun createCandidateView(context: Context): View {
        candidateView = FrameLayout(context)
        modes.forEach { candidateView.addView(it.createCandidateView(context)) }
        return candidateView
    }

    fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo) {
        currentMode.onStart(inputConnection, editorInfo)
    }

    fun onFinish(inputConnection: InputConnection, editorInfo: EditorInfo) {
        currentMode.onFinish()
    }

    fun switchMode(index: Int, inputConnection: InputConnection, editorInfo: EditorInfo) {
        currentMode.onFinish()
        currentModeIndex = index
        currentMode.onStart(inputConnection, editorInfo)
        currentMode.getInputView().bringToFront()
    }
}