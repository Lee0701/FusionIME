package ee.oyatl.ime.fusion

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
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

class CangjieIMEMode(
    context: Context,
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

    override val keyboardSet: KeyboardSet = StackedKeyboardSet(
        DefaultKeyboardSet(keyboardListener, LayoutCangjie.ROWS_LOWER, LayoutCangjie.ROWS_UPPER),
        BottomRowKeyboardSet(keyboardListener)
    )

    private val table: TableLoader = TableLoader()
    private val wordComposer: WordComposer = WordComposer()

    private var bestCandidate: CangjieCandidate? = null

    init {
        table.setPath(context.filesDir.absolutePath.encodeToByteArray())
        table.initialize()
    }

    override fun onReset() {
        super.onReset()
        wordComposer.reset()
        bestCandidate = null
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        val inputConnection = currentInputConnection ?: return
        inputConnection.commitText(candidate.text, 1)
        inputConnection.setComposingText("", 1)
        onReset()
    }

    private fun updateSuggestions() {
        val chars = (wordComposer.typedWord?.toString().orEmpty()
            .map { LayoutCangjie.KEY_MAP[it] ?: it }.toCharArray() +
                (0 until 5).map { 0.toChar() }).take(5)
        val (c0, c1, c2, c3, c4) = chars
        table.searchCangjie(c0, c1, c2, c3, c4)
        val candidates = (0 until table.totalMatch())
            .map { CangjieCandidate(table.getMatchChar(it).toString()) }
        submitCandidates(candidates)
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

    override fun onChar(code: Int) {
        wordComposer.add(code, intArrayOf(code))
        table.setInputMethod(TableLoader.CANGJIE)
        renderInput()
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
                onReset()
            }
            Keyboard.SpecialKey.Return -> {
                if(wordComposer.typedWord?.isNotEmpty() == true) onReset()
                else {
                    if (util?.sendDefaultEditorAction(true) != true)
                        currentInputConnection?.commitText("\n", 1)
                    onReset()
                }
            }
            Keyboard.SpecialKey.Delete -> {
                if(wordComposer.typedWord?.isNotEmpty() == true) {
                    wordComposer.deleteLast()
                } else {
                    util?.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                }
            }
            else -> {}
        }
        renderInput()
    }

    data class CangjieCandidate(
        override val text: CharSequence
    ): CandidateView.Candidate

    companion object {
        const val MSG_UPDATE_SUGGESTIONS = 0
    }
}