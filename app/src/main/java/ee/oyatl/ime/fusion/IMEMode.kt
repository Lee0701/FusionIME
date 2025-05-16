package ee.oyatl.ime.fusion

import android.content.Context
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

interface IMEMode {
    fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo)
    fun onFinish(inputConnection: InputConnection, editorInfo: EditorInfo)
    fun initView(context: Context): View
    fun getView(): View

    interface Listener {
        fun onLanguageSwitch()
    }
}