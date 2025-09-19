package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import com.android.inputmethod.zhuyin.Suggest
import com.android.inputmethod.zhuyin.TextEntryState
import com.android.inputmethod.zhuyin.WordComposer
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.keyboard.KeyboardConfiguration
import ee.oyatl.ime.keyboard.layout.LayoutZhuyin
import ee.oyatl.ime.keyboard.LayoutTable
import ee.oyatl.ime.keyboard.layout.KeyboardConfigurations
import ee.oyatl.ime.keyboard.layout.KeyboardTemplates
import ee.oyatl.ime.keyboard.layout.LayoutExt
import ee.oyatl.ime.keyboard.layout.LayoutQwerty
import tw.cheyingwu.zhuyin.ZhuYinDictionary
import tw.cheyingwu.zhuyin.ZhuYinIMESettings
import java.util.Locale

class ZhuyinIMEMode(
    listener: IMEMode.Listener
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

    override val keyboardConfiguration: KeyboardConfiguration = KeyboardConfiguration(
        KeyboardConfigurations.mobileNumbers(),
        KeyboardConfigurations.mobileAlpha(semicolon = true, shiftDeleteWidth = 1f, shift = false),
        KeyboardConfigurations.mobileBottom(KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_SLASH)
    )
    override val keyboardTemplate: List<String> =
        KeyboardTemplates.MOBILE_NUMBERS + KeyboardTemplates.MOBILE_HALF_GRID
    override val layoutTable: LayoutTable = LayoutTable.from(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + LayoutExt.TABLE_CHINESE + LayoutZhuyin.TABLE)

    private val wordComposer = WordComposer()
    private var mSuggest: Suggest? = null
    private var mUserDictionary: ZhuYinDictionary? = null

    private var bestCandidate: ZhuyinCandidate? = null

    override suspend fun onLoad(context: Context) {
        val suggest = Suggest(context, R.raw.dict_zhuyin)
        mUserDictionary = ZhuYinDictionary(context)

        suggest.correctionMode = Suggest.CORRECTION_BASIC
        suggest.setUserDictionary(mUserDictionary)
        ZhuYinIMESettings.setCandidateCnt(50)

        this.mSuggest = suggest
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
            inputConnection.commitText(text, 1)
            mUserDictionary?.useWordDB(text)
            onReset()
        }
    }

    private fun renderResult() {
        val inputConnection = currentInputConnection ?: return
        postUpdateSuggestions()
        inputConnection.setComposingText(wordComposer.typedWord ?: "", 1)
    }

    private fun updateSuggestions() {
        val list = mSuggest?.getSuggestions(getInputView(), wordComposer, false) ?: return
        val candidates = list.mapIndexed { i, s -> ZhuyinCandidate(i, s) }
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
            //Log.i(TAG, "MSG_UPDATE_SUGGESTIONS");
            updateSuggestions()
        }
        val bestCandidate = bestCandidate
        if(bestCandidate != null) onCandidateSelected(bestCandidate)
    }

    private fun handleSpace() {
        if(bestCandidate != null) pickDefaultSuggestion()
        else currentInputConnection?.commitText(" ", 1)
    }

    private fun handleReturn() {
        if(wordComposer.typedWord?.isNotEmpty() == true) onReset()
        else {
            if (util?.sendDefaultEditorAction(true) != true)
                currentInputConnection?.commitText("\n", 1)
        }
    }

    private fun handleBackspace() {
        val ic = currentInputConnection ?: return
        var deleteChar = false
        if (wordComposer.typedWord?.isNotEmpty() == true) {
            val length: Int = wordComposer.typedWord.length
            if (length > 0) {
                wordComposer.deleteLast()
                postUpdateSuggestions()
            } else {
                ic.deleteSurroundingText(1, 0)
            }
        } else {
            deleteChar = true
        }
        TextEntryState.backspace()
        if (deleteChar) {
            util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        }
        renderResult()
    }

    override fun onChar(codePoint: Int) {
        val key = codePoint.toChar()
        val value = LayoutZhuyin.CODES_MAP[key]
        if(value != null) {
            wordComposer.add(codePoint, intArrayOf(value))
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
            else -> {}
        }
    }

    data class ZhuyinCandidate(
        val index: Int,
        override val text: CharSequence
    ): CandidateView.Candidate

    class Params: IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): IMEMode {
            return ZhuyinIMEMode(listener)
        }

        override fun getLabel(context: Context): String {
            val localeName = Locale.TRADITIONAL_CHINESE.displayName
            val layoutName = context.resources.getString(R.string.zhuyin_layout_zhuyin)
            return "$localeName $layoutName"
        }

        override fun getShortLabel(context: Context): String {
            return "注音"
        }

        companion object {
            fun parse(map: Map<String, String>): Params {
                return Params()
            }
        }
    }

    companion object {
        const val TYPE: String = "zhuyin"
        const val MSG_UPDATE_SUGGESTIONS = 0
    }
}