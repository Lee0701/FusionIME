package ee.oyatl.ime.fusion

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import com.google.common.base.Optional
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.candidate.CandidateViewWrapper
import ee.oyatl.ime.fusion.mozc.InputConnectionRenderer
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.keyboardset.BottomRowKeyboardSet
import ee.oyatl.ime.keyboard.keyboardset.DefaultKeyboardSet
import ee.oyatl.ime.keyboard.keyboardset.KeyboardSet
import ee.oyatl.ime.keyboard.keyboardset.StackedKeyboardSet
import ee.oyatl.ime.keyboard.layout.LayoutQwerty
import org.mozc.android.inputmethod.japanese.PrimaryKeyCodeConverter
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCandidates.CandidateWord
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Input.TouchEvent
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Request
import org.mozc.android.inputmethod.japanese.protobuf.ProtoConfig.Config
import org.mozc.android.inputmethod.japanese.session.SessionExecutor
import org.mozc.android.inputmethod.japanese.session.SessionExecutor.EvaluationCallback
import org.mozc.android.inputmethod.japanese.session.SessionHandlerFactory

class FusionIMEService: InputMethodService() {
    private val keyboardListener = KeyboardListener()
    private val keyboardSet: KeyboardSet = StackedKeyboardSet(
        DefaultKeyboardSet(keyboardListener, LayoutQwerty.ROWS_ROMAJI_LOWER, LayoutQwerty.ROWS_ROMAJI_UPPER),
        BottomRowKeyboardSet(keyboardListener)
    )
    private lateinit var candidateView: CandidateViewWrapper
    private lateinit var imeView: ViewGroup

    private lateinit var primaryKeyCodeConverter: PrimaryKeyCodeConverter
    private lateinit var sessionExecutor: SessionExecutor
    private var inputConnectionRenderer: InputConnectionRenderer? = null

    private val renderResultCallback = EvaluationCallback { command, triggeringKeyEvent ->
        inputConnectionRenderer?.renderInputConnection(command.orNull(), triggeringKeyEvent.orNull())
        if(command.get().hasOutput() && command.get().output.hasAllCandidateWords()) {
            val candidates = command.get().output.allCandidateWords.candidatesList
                .map { candidate -> MozcCandidate(candidate) }
            candidateView.submitList(candidates)
        } else {
            candidateView.submitList(emptyList())
        }
    }

    override fun onCreate() {
        super.onCreate()
        primaryKeyCodeConverter = PrimaryKeyCodeConverter(this)
        sessionExecutor = SessionExecutor.getInstanceInitializedIfNecessary(SessionHandlerFactory(this), this)
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        inputConnectionRenderer = InputConnectionRenderer(currentInputConnection, currentInputEditorInfo)
        sessionExecutor.switchInputFieldType(ProtoCommands.Context.InputFieldType.NORMAL)
        sessionExecutor.switchInputMode(
            Optional.absent(), ProtoCommands.CompositionMode.HIRAGANA, renderResultCallback)
        sessionExecutor.setImposedConfig(
            Config.newBuilder()
                .setSessionKeymap(Config.SessionKeymap.MOBILE)
                .clearSelectionShortcut()
                .setUseEmojiConversion(false)
                .build()
        )
        sessionExecutor.updateRequest(
            Request.newBuilder()
                .setZeroQuerySuggestion(true)
                .setMixedConversion(true)
                .setUpdateInputModeFromSurroundingText(false)
                .setAutoPartialSuggestion(true)
                .setCrossingEdgeBehavior(Request.CrossingEdgeBehavior.DO_NOTHING)
                .build(),
            emptyList()
        )
        sessionExecutor.resetContext()
    }

    override fun onCreateInputView(): View {
        keyboardSet.initView(this)
        imeView = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val inputView = keyboardSet.getView(keyboardListener.shiftState, false)
        val resId = ee.oyatl.ime.keyboard.R.layout.candidate_view
        candidateView = CandidateViewWrapper(
            candidateView = layoutInflater.inflate(resId, null, false) as CandidateView,
            onItemClick = { this.onCandiadteClick(it) }
        )
        imeView.addView(candidateView.view)
        imeView.addView(inputView)
        return imeView
    }

    fun updateInputView() {
        keyboardSet.getView(keyboardListener.shiftState, false)
        setInputView(imeView)
    }

    private fun onCandiadteClick(candidate: CandidateView.Candidate) {
        if(candidate is MozcCandidate) {
            sessionExecutor.submitCandidate(candidate.id, Optional.absent(), renderResultCallback)
        }
    }

    private inner class KeyboardListener(
        private val autoReleaseShift: Boolean = true
    ): Keyboard.Listener {
        var shiftState: Keyboard.ShiftState = Keyboard.ShiftState.Unpressed
            private set
        private var shiftPressing: Boolean = false
        private var shiftTime: Long = 0
        private var inputWhileShifted: Boolean = false

        override fun onChar(code: Int) {
            val eventList = emptyList<TouchEvent>()
            val keyEvent = primaryKeyCodeConverter.getPrimaryCodeKeyEvent(code)
            val mozcKeyEvent = primaryKeyCodeConverter.createMozcKeyEvent(code, eventList).orNull()
            sessionExecutor.sendKey(mozcKeyEvent, keyEvent, eventList, renderResultCallback)
        }

        override fun onSpecial(type: Keyboard.SpecialKey) {
            when(type) {
                Keyboard.SpecialKey.Space -> onChar(' '.code)
                Keyboard.SpecialKey.Return -> onChar(primaryKeyCodeConverter.keyCodeEnter)
                Keyboard.SpecialKey.Delete -> onChar(primaryKeyCodeConverter.keyCodeBackspace)
                else -> {}
            }
        }

        override fun onShift(pressed: Boolean) {
            shiftPressing = pressed
            val oldShiftState = shiftState
            if(pressed) onShiftPressed()
            else onShiftReleased()
            if(shiftState != oldShiftState) updateInputView()
        }

        private fun onShiftPressed() {
            when(shiftState) {
                Keyboard.ShiftState.Unpressed -> {
                    shiftState = Keyboard.ShiftState.Pressed
                }
                Keyboard.ShiftState.Pressed -> {
                    if(autoReleaseShift) {
                        val diff = System.currentTimeMillis() - shiftTime
                        if(diff < 300) shiftState = Keyboard.ShiftState.Locked
                        else shiftState = Keyboard.ShiftState.Unpressed
                    } else {
                        shiftState = Keyboard.ShiftState.Unpressed
                    }
                }
                Keyboard.ShiftState.Locked -> {
                    shiftState = Keyboard.ShiftState.Unpressed
                }
            }
        }

        private fun onShiftReleased() {
            when(shiftState) {
                Keyboard.ShiftState.Unpressed -> {
                }
                Keyboard.ShiftState.Pressed -> {
                    if(inputWhileShifted) shiftState = Keyboard.ShiftState.Unpressed
                    else shiftState = Keyboard.ShiftState.Pressed
                }
                Keyboard.ShiftState.Locked -> {
                }
            }
            shiftTime = System.currentTimeMillis()
            inputWhileShifted = false
        }
    }

    data class MozcCandidate(
        val id: Int,
        override val text: CharSequence
    ): CandidateView.Candidate {
        constructor(mozcCandidate: CandidateWord): this(
            id = mozcCandidate.id,
            text = mozcCandidate.value
        )
    }
}