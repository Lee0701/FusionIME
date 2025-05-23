package ee.oyatl.ime.fusion

import android.content.Context
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

interface IMEMode {

    suspend fun onLoad(context: Context)
    fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo)
    fun onFinish()
    fun createInputView(context: Context): View
    fun createCandidateView(context: Context): View
    fun getInputView(): View?

    fun onKeyDown(keyCode: Int, metaState: Int)
    fun onKeyUp(keyCode: Int, metaState: Int)

    interface Listener {
        fun onLanguageSwitch()
        fun onRequestHideSelf(flags: Int)
        fun onCandidateViewVisibilityChange(visible: Boolean)
    }
}