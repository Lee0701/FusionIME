package ee.oyatl.ime.fusion

import android.content.Context
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

interface IMEMode {
    fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo)
    fun onFinish()
    fun createInputView(context: Context): View
    fun createCandidateView(context: Context): View
    fun getInputView(): View

    interface Listener {
        fun onLanguageSwitch()
        fun onCandidateViewVisibilityChange(visible: Boolean)
    }
}