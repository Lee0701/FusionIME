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
import com.android.inputmethod.latin.WordComposer
import com.diycircuits.cangjie.TableLoader
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.candidate.ScrollingCandidateView
import ee.oyatl.ime.keyboard.CommonKeyboardListener
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.keyboardset.BottomRowKeyboardSet
import ee.oyatl.ime.keyboard.keyboardset.DefaultKeyboardSet
import ee.oyatl.ime.keyboard.keyboardset.KeyboardSet
import ee.oyatl.ime.keyboard.keyboardset.StackedKeyboardSet
import ee.oyatl.ime.keyboard.layout.LayoutCangjie
import ee.oyatl.ime.keyboard.layout.LayoutQwerty

class CangjieIMEMode(
    context: Context,
    private val listener: IMEMode.Listener
): IMEMode, CandidateView.Listener, CommonKeyboardListener.Callback {

    private val handler: Handler = Handler(Looper.getMainLooper()) { msg ->
        when(msg.what) {
            ZhuyinIMEMode.MSG_UPDATE_SUGGESTIONS -> {
                updateSuggestions()
                true
            }
            else -> false
        }
    }

    private val keyboardListener = KeyboardListener()
    private val keyboardSet: KeyboardSet = StackedKeyboardSet(
        DefaultKeyboardSet(keyboardListener, LayoutCangjie.ROWS_LOWER, LayoutCangjie.ROWS_UPPER),
        BottomRowKeyboardSet(keyboardListener)
    )
    private lateinit var candidateView: CandidateView
    private lateinit var imeView: ViewGroup

    private val table: TableLoader = TableLoader()
    private val wordComposer: WordComposer = WordComposer()

    private var util: KeyEventUtil? = null
    private val currentInputConnection: InputConnection? get() = util?.currentInputConnection
    private val currentInputEditorInfo: EditorInfo? get() = util?.currentInputEditorInfo

    private var bestCandidate: CangjieCandidate? = null

    init {
        table.setPath(context.filesDir.absolutePath.encodeToByteArray())
        table.initialize()
    }

    override fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo) {
        util = KeyEventUtil(inputConnection, editorInfo)
    }

    override fun onFinish(inputConnection: InputConnection, editorInfo: EditorInfo) {
        reset()
        util = null
    }

    private fun reset() {
        currentInputConnection?.finishComposingText()
        candidateView.submitList(emptyList())
        wordComposer.reset()
        bestCandidate = null
    }

    override fun initView(context: Context): View {
        keyboardSet.initView(context)
        imeView = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val inputView = keyboardSet.getView(keyboardListener.shiftState, false)
        candidateView = ScrollingCandidateView(context, null).apply {
            listener = this@CangjieIMEMode
        }
        imeView.addView(candidateView as View)
        imeView.addView(inputView)
        return imeView
    }

    override fun getView(): View {
        updateInputView()
        return imeView
    }

    override fun updateInputView() {
        keyboardSet.getView(keyboardListener.shiftState, false)
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        val inputConnection = currentInputConnection ?: return
        inputConnection.commitText(candidate.text, 1)
        inputConnection.setComposingText("", 1)
        reset()
    }

    private fun updateSuggestions() {
        val chars = (wordComposer.typedWord?.toString().orEmpty()
            .map { LayoutCangjie.KEY_MAP[it] ?: it }.toCharArray() +
                (0 until 5).map { 0.toChar() }).take(5)
        val (c0, c1, c2, c3, c4) = chars
        table.searchCangjie(c0, c1, c2, c3, c4)
        val candidates = (0 until table.totalMatch())
            .map { CangjieCandidate(table.getMatchChar(it).toString()) }
        candidateView.submitList(candidates)
        bestCandidate = candidates.firstOrNull()
    }

    private fun postUpdateSuggestions() {
        handler.removeMessages(MSG_UPDATE_SUGGESTIONS)
        handler.sendMessageDelayed(handler.obtainMessage(MSG_UPDATE_SUGGESTIONS), 10)
    }

    private fun renderInput() {
        currentInputConnection?.setComposingText(wordComposer.typedWord?.toString().orEmpty(), 1)
        postUpdateSuggestions()
    }

    inner class KeyboardListener: CommonKeyboardListener(this) {
        override fun onChar(code: Int) {
            wordComposer.add(code, intArrayOf(code))
            table.setInputMethod(TableLoader.CANGJIE)
            renderInput()
            super.onChar(code)
        }

        override fun onSpecial(type: Keyboard.SpecialKey) {
            when(type) {
                Keyboard.SpecialKey.Space -> {
                    if(wordComposer.typedWord?.isNotEmpty() == true) {
                        val bestCandidate = bestCandidate
                        if(bestCandidate != null) onCandidateSelected(bestCandidate)
                    } else {
                        util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_SPACE)
                    }
                    reset()
                }
                Keyboard.SpecialKey.Return -> {
                    if(wordComposer.typedWord?.isNotEmpty() == true) reset()
                    else {
                        if (util?.sendDefaultEditorAction(true) != true)
                            currentInputConnection?.commitText("\n", 1)
                        reset()
                    }
                }
                Keyboard.SpecialKey.Delete -> {
                    if(wordComposer.typedWord?.isNotEmpty() == true) {
                        wordComposer.deleteLast()
                    } else {
                        util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                    }
                }
                Keyboard.SpecialKey.Language -> listener.onLanguageSwitch()
                else -> {}
            }
            super.onSpecial(type)
            renderInput()
        }
    }

    data class CangjieCandidate(
        override val text: CharSequence
    ): CandidateView.Candidate

    companion object {
        const val MSG_UPDATE_SUGGESTIONS = 0
    }
}