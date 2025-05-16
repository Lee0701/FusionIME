package ee.oyatl.ime.fusion

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import com.android.inputmethod.latin.Suggest
import com.android.inputmethod.latin.TextEntryState
import com.android.inputmethod.latin.WordComposer
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.candidate.ScrollingCandidateView
import ee.oyatl.ime.keyboard.CommonKeyboardListener
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.keyboardset.GridBottomKeyboardSet
import ee.oyatl.ime.keyboard.keyboardset.GridKeyboardSet
import ee.oyatl.ime.keyboard.keyboardset.KeyboardSet
import ee.oyatl.ime.keyboard.keyboardset.StackedKeyboardSet
import ee.oyatl.ime.keyboard.layout.LayoutZhuyin
import tw.cheyingwu.zhuyin.ZhuYinDictionary
import tw.cheyingwu.zhuyin.ZhuYinIMESettings

class ZhuyinIMEMode(
    context: Context,
    private val listener: IMEMode.Listener
): IMEMode, CandidateView.Listener, CommonKeyboardListener.Callback {

    private val handler: Handler = Handler(Looper.getMainLooper()) { msg ->
        when(msg.what) {
            MSG_UPDATE_SUGGESTIONS -> {
                updateSuggestions()
                true
            }
            else -> false
        }
    }

    private val keyboardListener = KeyboardListener()
    private val keyboardSet: KeyboardSet = StackedKeyboardSet(
        GridKeyboardSet(keyboardListener, LayoutZhuyin.ROWS_ZHUYIN.dropLast(1)),
        GridBottomKeyboardSet(keyboardListener, LayoutZhuyin.ROWS_ZHUYIN.last())
    )
    private lateinit var candidateView: CandidateView
    private lateinit var imeView: ViewGroup

    private val wordComposer: WordComposer = WordComposer()
    private var mSuggest: Suggest = Suggest(context, R.raw.dict_zhuyin)
    private var mUserDictionary: ZhuYinDictionary = ZhuYinDictionary(context)

    private var currentInputConnection: InputConnection? = null
    private var currentInputEditorInfo: EditorInfo? = null
    private var util: KeyEventUtil? = null

    private var bestCandidate: ZhuyinCandidate? = null

    init {
        mSuggest.correctionMode = Suggest.CORRECTION_BASIC
        mSuggest.setUserDictionary(mUserDictionary)
        ZhuYinIMESettings.setCandidateCnt(50)
    }

    override fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo) {
        this.currentInputConnection = inputConnection
        this.currentInputEditorInfo = editorInfo
        this.util = KeyEventUtil(inputConnection, editorInfo)
    }

    override fun onFinish(inputConnection: InputConnection, editorInfo: EditorInfo) {
        this.currentInputConnection = null
        this.currentInputEditorInfo = null
        this.util = null
    }

    private fun reset() {
        currentInputConnection?.finishComposingText()
        candidateView.submitList(emptyList())
        bestCandidate = null
        wordComposer.reset()
    }

    override fun initView(context: Context): View {
        keyboardSet.initView(context)
        imeView = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val inputView = keyboardSet.getView(keyboardListener.shiftState, false)
        candidateView = ScrollingCandidateView(context, null).apply {
            listener = this@ZhuyinIMEMode
        }
        imeView.addView(candidateView as View)
        imeView.addView(inputView)
        return imeView
    }

    override fun getView(): View {
        updateInputView()
        return imeView
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        val inputConnection = currentInputConnection ?: return
        if(candidate is ZhuyinCandidate) {
            val text = candidate.text.toString().replace(" ", "")
            inputConnection.commitText(text, 1)
            mUserDictionary.useWordDB(text)
            reset()
        }
    }

    override fun updateInputView() {
        keyboardSet.getView(keyboardListener.shiftState, false)
    }

    private fun renderResult() {
        val inputConnection = currentInputConnection ?: return
        postUpdateSuggestions()
        inputConnection.setComposingText(wordComposer.typedWord ?: "", 1)
    }

    private fun updateSuggestions() {
        val list = mSuggest.getSuggestions(imeView, wordComposer, false)
        val candidates = list.mapIndexed { i, s -> ZhuyinCandidate(i, s) }
        bestCandidate = candidates.getOrNull(0)
        candidateView.submitList(candidates)
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
        if(wordComposer.typedWord?.isNotEmpty() == true) reset()
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

    inner class KeyboardListener: CommonKeyboardListener(this) {
        override fun onChar(code: Int) {
            val key = code.toChar()
            val value = LayoutZhuyin.CODES_MAP[key]
            if(value != null) {
                wordComposer.add(code, intArrayOf(value))
                renderResult()
            }
            super.onChar(code)
        }

        override fun onSpecial(type: Keyboard.SpecialKey) {
            when(type) {
                Keyboard.SpecialKey.Space -> handleSpace()
                Keyboard.SpecialKey.Return -> handleReturn()
                Keyboard.SpecialKey.Delete -> handleBackspace()
                Keyboard.SpecialKey.Language -> listener.onLanguageSwitch()
                else -> {}
            }
            super.onSpecial(type)
        }
    }

    data class ZhuyinCandidate(
        val index: Int,
        override val text: CharSequence
    ): CandidateView.Candidate

    companion object {
        const val MSG_UPDATE_SUGGESTIONS = 0
    }
}