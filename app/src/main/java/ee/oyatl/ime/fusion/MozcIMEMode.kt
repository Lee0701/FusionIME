package ee.oyatl.ime.fusion

import android.content.Context
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.google.common.base.Optional
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.candidate.VerticalScrollingCandidateView
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

class MozcIMEMode(
    context: Context,
    listener: IMEMode.Listener
): CommonIMEMode(listener) {

    override val keyboardSet: KeyboardSet = StackedKeyboardSet(
        DefaultKeyboardSet(keyboardListener, LayoutQwerty.ROWS_ROMAJI_LOWER, LayoutQwerty.ROWS_ROMAJI_UPPER),
        BottomRowKeyboardSet(keyboardListener)
    )

    private val primaryKeyCodeConverter: PrimaryKeyCodeConverter = PrimaryKeyCodeConverter(context)
    private val sessionExecutor: SessionExecutor =
        SessionExecutor.getInstanceInitializedIfNecessary(SessionHandlerFactory(context), context)
    private var inputConnectionRenderer: InputConnectionRenderer? = null

    private val renderResultCallback = EvaluationCallback { command, triggeringKeyEvent ->
        inputConnectionRenderer?.renderInputConnection(command.orNull(), triggeringKeyEvent.orNull())
        if(command.get().hasOutput() && command.get().output.hasAllCandidateWords()) {
            val candidates = command.get().output.allCandidateWords.candidatesList
                .map { candidate -> MozcCandidate(candidate) }
            submitCandidates(candidates)
        } else {
            submitCandidates(emptyList())
        }
    }

    override fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo) {
        super.onStart(inputConnection, editorInfo)
        inputConnectionRenderer = InputConnectionRenderer(inputConnection, editorInfo)
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

    override fun onFinish() {
        super.onFinish()
        sessionExecutor.resetContext()
    }

    override fun createCandidateView(context: Context): View {
        val candidateView = VerticalScrollingCandidateView(context, null, 2).apply {
            listener = this@MozcIMEMode
        }
        this.candidateView = candidateView
        return candidateView
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        if(candidate is MozcCandidate) {
            sessionExecutor.submitCandidate(candidate.id, Optional.absent(), renderResultCallback)
        }
    }

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