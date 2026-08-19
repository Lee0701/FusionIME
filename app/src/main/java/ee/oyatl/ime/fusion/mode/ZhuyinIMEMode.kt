package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import com.miyabi_hiroshi.app.libchewing_android_app_module.ConversionEngines
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.Feature
import ee.oyatl.ime.fusion.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.korean.WordComposer
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.layout.LayoutQwerty
import ee.oyatl.ime.fusion.layout.LayoutZhuyin
import ee.oyatl.ime.fusion.layout.MobileKeyboard
import ee.oyatl.ime.fusion.layout.MobileKeyboardRows
import ee.oyatl.ime.fusion.layout.TabletKeyboard
import ee.oyatl.ime.fusion.layout.TabletKeyboardRows
import ee.oyatl.ime.fusion.layout.preset.ZhuyinLayoutPresets
import ee.oyatl.ime.fusion.zhuyin.ChewingConverter
import ee.oyatl.ime.keyboard.KeyboardConfiguration
import ee.oyatl.ime.keyboard.KeyboardTemplate
import ee.oyatl.ime.keyboard.LayoutTable
import java.util.Locale

class ZhuyinIMEMode(
    listener: IMEMode.Listener,
    override var textLayoutPreset: KeyboardLayoutPreset,
    conversionEngine: ConversionEngines
): CommonIMEMode(listener) {
    private val handler: Handler = Handler(Looper.getMainLooper()) { msg ->
        when(msg.what) {
            MSG_UPDATE_SUGGESTIONS -> {
                updateSuggestions()
                true
            }
            else -> false
        }
    }

    private val wordComposer = WordComposer()
    private val converter: ChewingConverter = ChewingConverter(conversionEngine)

    private var bestCandidate: ZhuyinCandidate? = null

    override suspend fun onLoad(context: Context) {
        super.onLoad(context)
        symbolLayoutPreset = super.symbolLayoutPreset.copy(
            layoutTable = super.symbolLayoutPreset.layoutTable + LayoutTable.fromShiftStates(LayoutExt.TABLE_CHINESE)
        )
        converter.initialize(context)
    }

    override fun onReset() {
        super.onReset()
        bestCandidate = null
        wordComposer.reset()
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        val inputConnection = currentInputConnection ?: return
        if(candidate is ZhuyinCandidate) {
            val text = candidate.text.toString().replace(" ", "")
            wordComposer.consume(candidate.key.length)
            wordComposer.moveCursor(wordComposer.composingText.length)
            inputConnection.commitText(text, 1)
            renderResult()
        }
    }

    private fun renderResult() {
        val inputConnection = currentInputConnection ?: return
        postUpdateSuggestions()
        inputConnection.setComposingText(wordComposer.getSpannableSurfaceString(), 1)
    }

    private fun updateSuggestions() {
        val key = wordComposer.textBeforeCursor
        val codes = key.mapNotNull { LayoutZhuyin.CODES_MAP[it]?.code }.toMutableList()
        if(key.lastOrNull() !in LayoutZhuyin.TONE_MARKS) codes += ' '.code
        val candidates = converter.getSuggestions(codes).mapIndexed { i, s -> ZhuyinCandidate(i, s, key) }
        bestCandidate = candidates.getOrNull(0)
        submitCandidates(candidates)
    }

    private fun postUpdateSuggestions() {
        handler.removeMessages(MSG_UPDATE_SUGGESTIONS)
        handler.sendMessageDelayed(handler.obtainMessage(MSG_UPDATE_SUGGESTIONS), 100)
    }

    private fun pickDefaultSuggestion() {
        // Complete any pending candidate query first
        if (handler.hasMessages(MSG_UPDATE_SUGGESTIONS)) {
            handler.removeMessages(MSG_UPDATE_SUGGESTIONS)
            updateSuggestions()
        }
        val bestCandidate = bestCandidate
        if(bestCandidate != null) onCandidateSelected(bestCandidate)
    }

    private fun handleSpace() {
        if(wordComposer.composingText.isNotEmpty()) {
            if(wordComposer.textBeforeCursor.lastOrNull() !in LayoutZhuyin.TONE_MARKS)
                wordComposer.commit('ˉ'.toString())
            else if(bestCandidate != null)
                pickDefaultSuggestion()
            else
                onReset()
            renderResult()
        }
        else currentInputConnection?.commitText(" ", 1)
    }

    private fun handleReturn() {
        if(wordComposer.composingText.isNotEmpty()) {
            if(bestCandidate != null) pickDefaultSuggestion()
            else onReset()
        } else {
            if (util?.sendDefaultEditorAction(true) != true)
                currentInputConnection?.commitText("\n", 1)
        }
        renderResult()
    }

    private fun handleBackspace() {
        val ic = currentInputConnection ?: return
        var deleteChar = false
        if (wordComposer.composingText.isNotEmpty()) {
            val length: Int = wordComposer.composingText.length
            if (length > 0) {
                wordComposer.delete(1)
            } else {
                ic.deleteSurroundingText(1, 0)
            }
        } else {
            deleteChar = true
        }
        if (deleteChar) {
            util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        }
        renderResult()
    }

    private fun handleDpad(amount: Int) {
        if(wordComposer.composingText.isNotEmpty()) {
            wordComposer.moveCursorRelative(amount)
            renderResult()
        } else {
            val keyCode =
                if(amount < 0) KeyEvent.KEYCODE_DPAD_LEFT
                else if(amount > 0) KeyEvent.KEYCODE_DPAD_RIGHT
                else 0
            if(keyCode != 0) util?.sendDownUpKeyEvents(keyCode)
        }
    }

    override fun onChar(codePoint: Int) {
        val key = codePoint.toChar()
        if(key in LayoutZhuyin.CODES_MAP) {
            wordComposer.commit(key.toString())
            renderResult()
        } else {
            onReset()
            util?.sendKeyChar(codePoint.toChar())
        }
    }

    override fun onSpecial(keyCode: Int) {
        when(keyCode) {
            KeyEvent.KEYCODE_SPACE -> handleSpace()
            KeyEvent.KEYCODE_ENTER -> handleReturn()
            KeyEvent.KEYCODE_DEL -> handleBackspace()
            KeyEvent.KEYCODE_DPAD_LEFT -> handleDpad(-1)
            KeyEvent.KEYCODE_DPAD_RIGHT -> handleDpad(1)
            else -> super.onSpecial(keyCode)
        }
    }

    data class ZhuyinCandidate(
        val index: Int,
        override val text: CharSequence,
        val key: String
    ): CandidateView.Candidate

    class Params(
        val conversionEngine: ConversionEngines,
        val cursorKeys: Boolean
    ): IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): IMEMode {
            val textLayoutPreset = ZhuyinLayoutPresets.bopomofo(cursorKeys)
            return ZhuyinIMEMode(listener, textLayoutPreset, conversionEngine)
        }

        override fun getLabel(context: Context): String {
            val localeName = Locale.TRADITIONAL_CHINESE.displayName
            val layoutName = context.resources.getString(R.string.zhuyin_layout_zhuyin)
            return "$localeName $layoutName"
        }

        override fun getShortLabel(context: Context, params: List<IMEMode.Params>): String {
            return "注音"
        }

        companion object {
            fun parse(map: Map<String, String>): Params {
                val conversionEngine = ConversionEngines.entries.find { it.name == map["conversion_engine"] }
                    ?: ConversionEngines.FUZZY_CHEWING_CONVERSION_ENGINE
                val cursorKeys = map["cursor_keys"]?.toBoolean() ?: false
                return Params(
                    conversionEngine = conversionEngine,
                    cursorKeys = cursorKeys
                )
            }
        }
    }

    companion object {
        const val TYPE: String = "zhuyin"
        const val MSG_UPDATE_SUGGESTIONS = 0
    }
}