package ee.oyatl.ime.fusion

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.google.common.base.Optional
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.candidate.VerticalScrollingCandidateView
import ee.oyatl.ime.fusion.mozc.InputConnectionRenderer
import ee.oyatl.ime.keyboard.DefaultBottomRowKeyboard
import ee.oyatl.ime.keyboard.DefaultGridKeyboard
import ee.oyatl.ime.keyboard.DefaultMobileKeyboard
import ee.oyatl.ime.keyboard.GridKanaBottomRowKeyboard
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.KeyboardInflater
import ee.oyatl.ime.keyboard.ShiftStateKeyboard
import ee.oyatl.ime.keyboard.StackedKeyboard
import ee.oyatl.ime.keyboard.layout.KeyboardTemplates
import ee.oyatl.ime.keyboard.layout.LayoutKana50OnZu
import ee.oyatl.ime.keyboard.layout.LayoutRomaji
import org.mozc.android.inputmethod.japanese.MozcUtil
import org.mozc.android.inputmethod.japanese.PrimaryKeyCodeConverter
import org.mozc.android.inputmethod.japanese.keyboard.Keyboard.KeyboardSpecification
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCandidates.CandidateWord
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Input.TouchEvent
import org.mozc.android.inputmethod.japanese.protobuf.ProtoConfig.Config
import org.mozc.android.inputmethod.japanese.session.SessionExecutor
import org.mozc.android.inputmethod.japanese.session.SessionExecutor.EvaluationCallback
import org.mozc.android.inputmethod.japanese.session.SessionHandlerFactory

abstract class MozcIMEMode(
    context: Context,
    listener: IMEMode.Listener
): CommonIMEMode(listener) {

    class RomajiQwerty(context: Context, listener: IMEMode.Listener): MozcIMEMode(context, listener) {
        override val keyboardSpecification: KeyboardSpecification = KeyboardSpecification.QWERTY_KANA
        override val layoutTable: Map<Int, List<Int>> = LayoutRomaji.TABLE_QWERTY
        private val layers = KeyboardInflater.inflate(KeyboardTemplates.MOBILE_MINUS, layoutTable)
        override val textKeyboard: Keyboard = StackedKeyboard(
            ShiftStateKeyboard(
                DefaultMobileKeyboard(layers[0]),
                DefaultMobileKeyboard(layers[1])
            ),
            DefaultBottomRowKeyboard()
        )
    }

    class Kana50OnZu(context: Context, listener: IMEMode.Listener): MozcIMEMode(context, listener) {
        override val keyboardSpecification: KeyboardSpecification = KeyboardSpecification.TWELVE_KEY_FLICK_KANA
        private val layers = KeyboardInflater.inflate(LayoutKana50OnZu.ROWS)
        override val textKeyboard: Keyboard = StackedKeyboard(
            DefaultGridKeyboard(layers[0]),
            GridKanaBottomRowKeyboard(
                KeyboardInflater.inflate(listOf(LayoutKana50OnZu.BOTTOM_LEFT))[0][0],
                KeyboardInflater.inflate(listOf(LayoutKana50OnZu.BOTTOM_RIGHT))[0][0]
            )
        )
    }

    private val resources: Resources = context.resources
    private val configuration: Configuration = resources.configuration
    protected abstract val keyboardSpecification: KeyboardSpecification

    private val primaryKeyCodeConverter: PrimaryKeyCodeConverter = PrimaryKeyCodeConverter(context)
    private val sessionExecutor: SessionExecutor =
        SessionExecutor.getInstanceInitializedIfNecessary(SessionHandlerFactory(context), context)
    private var inputConnectionRenderer: InputConnectionRenderer? = null

    private val renderResultCallback = EvaluationCallback { command, triggeringKeyEvent ->
        inputConnectionRenderer?.renderInputConnection(command.orNull(), triggeringKeyEvent.orNull())
        if(command.get().hasOutput()) {
            val output = command.get().output
            if(output.hasAllCandidateWords()) {
                val index =
                    if(output.allCandidateWords.hasFocusedIndex())
                        output.allCandidateWords.focusedIndex
                    else 0
                val candidates = output
                    .allCandidateWords.candidatesList
                    .mapIndexed { i, candidate -> MozcCandidate(candidate, index == i) }
                submitCandidates(candidates)
            } else {
                submitCandidates(emptyList())
            }
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
            MozcUtil.getRequestBuilder(resources, keyboardSpecification, configuration).build(),
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
        override val text: CharSequence,
        override val focused: Boolean
    ): CandidateView.FocusableCandidate {
        constructor(
            mozcCandidate: CandidateWord,
            focused: Boolean
        ): this(
            id = mozcCandidate.id,
            text = mozcCandidate.value,
            focused = focused
        )
    }
}