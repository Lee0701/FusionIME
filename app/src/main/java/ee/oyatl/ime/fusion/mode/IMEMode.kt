package ee.oyatl.ime.fusion.mode

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

    interface Params {
        val type: String
        fun create(listener: Listener): IMEMode
        fun getLabel(context: Context): String
        fun getShortLabel(context: Context): String

        companion object {
            fun parse(stringifedMap: String): Params? {
                val map = stringifedMap
                    .split(';').map { it.split('=') }
                    .associate { (key, value) -> key to value }
                return parse(map)
            }

            fun parse(map: Map<String, String>): Params? {
                return when(map["type"]) {
                    LatinIMEMode.TYPE -> LatinIMEMode.Params.parse(map)
                    KoreanIMEMode.TYPE -> KoreanIMEMode.Params.parse(map)
                    MozcIMEMode.TYPE -> MozcIMEMode.Params.parse(map)
                    PinyinIMEMode.TYPE -> PinyinIMEMode.Params.parse(map)
                    ZhuyinIMEMode.TYPE -> ZhuyinIMEMode.Params.parse(map)
                    CangjieIMEMode.TYPE -> CangjieIMEMode.Params.parse(map)
                    VietIMEMode.TYPE -> VietIMEMode.Params.parse(map)
                    else -> null
                }
            }
        }
    }
}