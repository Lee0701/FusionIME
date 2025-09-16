package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.StringRes
import com.google.common.base.Optional
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.candidate.VerticalScrollingCandidateView
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.mozc.InputConnectionRenderer
import ee.oyatl.ime.keyboard.DefaultBottomRowKeyboard
import ee.oyatl.ime.keyboard.DefaultGridKeyboard
import ee.oyatl.ime.keyboard.DefaultMobileKeyboard
import ee.oyatl.ime.keyboard.DefaultTabletBottomRowKeyboard
import ee.oyatl.ime.keyboard.DefaultTabletKeyboard
import ee.oyatl.ime.keyboard.GridKanaBottomRowKeyboard
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.KeyboardInflater
import ee.oyatl.ime.keyboard.ScreenTypeKeyboard
import ee.oyatl.ime.keyboard.ShiftStateKeyboard
import ee.oyatl.ime.keyboard.StackedKeyboard
import ee.oyatl.ime.keyboard.layout.KeyboardTemplates
import ee.oyatl.ime.keyboard.layout.LayoutKana
import ee.oyatl.ime.keyboard.layout.LayoutRomaji
import org.mozc.android.inputmethod.japanese.MozcUtil
import org.mozc.android.inputmethod.japanese.PrimaryKeyCodeConverter
import org.mozc.android.inputmethod.japanese.keyboard.Keyboard.KeyboardSpecification
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCandidateWindow.CandidateWord
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Input.TouchEvent
import org.mozc.android.inputmethod.japanese.protobuf.ProtoConfig.Config
import org.mozc.android.inputmethod.japanese.session.SessionExecutor
import org.mozc.android.inputmethod.japanese.session.SessionExecutor.EvaluationCallback
import org.mozc.android.inputmethod.japanese.session.SessionHandlerFactory
import java.util.Locale

abstract class MozcIMEMode(
    listener: IMEMode.Listener
): CommonIMEMode(listener) {

    private lateinit var resources: Resources
    protected abstract val keyboardSpecification: KeyboardSpecification

    private var primaryKeyCodeConverter: PrimaryKeyCodeConverter? = null
    private var sessionExecutor: SessionExecutor? = null
    private var inputConnectionRenderer: InputConnectionRenderer? = null

    private val renderResultCallback = EvaluationCallback { command, triggeringKeyEvent ->
        if(!command.isPresent) return@EvaluationCallback
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

    override suspend fun onLoad(context: Context) {
        primaryKeyCodeConverter = PrimaryKeyCodeConverter(context)
        sessionExecutor = SessionExecutor.getInstanceInitializedIfNecessary(SessionHandlerFactory(context), context)
        resources = context.resources
    }

    override fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo) {
        super.onStart(inputConnection, editorInfo)
        inputConnectionRenderer = InputConnectionRenderer(inputConnection, editorInfo)
        restartInput()
    }

    override fun onReset() {
        sessionExecutor?.resetContext()
        sessionExecutor?.deleteSession()
        super.onReset()
        restartInput()
    }

    private fun restartInput() {
        val sessionExecutor = this.sessionExecutor
        if(sessionExecutor != null) {
            sessionExecutor.switchInputFieldType(ProtoCommands.Context.InputFieldType.NORMAL)
            sessionExecutor.switchInputMode(
                Optional.absent(), ProtoCommands.CompositionMode.HIRAGANA, renderResultCallback)
            sessionExecutor.setImposedConfig(
                Config.newBuilder()
                    .setSessionKeymap(Config.SessionKeymap.MOBILE)
                    .clearSelectionShortcut()
                    .setUseEmojiConversion(false)
                    .setIncognitoMode(passwordField)
                    .build()
            )
            sessionExecutor.updateRequest(
                MozcUtil.getRequestBuilder(resources, keyboardSpecification, resources.configuration).build(),
                emptyList()
            )
            sessionExecutor.resetContext()
        }
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
            sessionExecutor?.submitCandidate(candidate.id, Optional.absent(), renderResultCallback)
        }
    }

    override fun onChar(code: Int) {
        val primaryKeyCodeConverter = primaryKeyCodeConverter ?: return
        val sessionExecutor = sessionExecutor ?: return
        val eventList = emptyList<TouchEvent>()
        val keyEvent = primaryKeyCodeConverter.getPrimaryCodeKeyEvent(code)
        val mozcKeyEvent = primaryKeyCodeConverter.createMozcKeyEvent(code, eventList).orNull()
        if(mozcKeyEvent != null) {
            sessionExecutor.sendKey(mozcKeyEvent, keyEvent, eventList, renderResultCallback)
        } else if(keyEvent != null) {
            sessionExecutor.sendKeyEvent(keyEvent, renderResultCallback)
        }
    }

    override fun onSpecial(type: Keyboard.SpecialKey) {
        when(type) {
            Keyboard.SpecialKey.Space -> onChar(' '.code)
            Keyboard.SpecialKey.Return -> onChar(primaryKeyCodeConverter?.keyCodeEnter ?: return)
            Keyboard.SpecialKey.Delete -> onChar(primaryKeyCodeConverter?.keyCodeBackspace ?: return)
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

    class RomajiQwerty(listener: IMEMode.Listener): MozcIMEMode(listener) {
        override val keyboardSpecification: KeyboardSpecification = KeyboardSpecification.QWERTY_KANA
        override val layoutTable: Map<Int, List<Int>> = LayoutRomaji.TABLE_QWERTY
        override fun createTextKeyboard(): Keyboard {
            val layers = KeyboardInflater.inflate(KeyboardTemplates.MOBILE_MINUS, layoutTable)
            return StackedKeyboard(
                ShiftStateKeyboard(
                    createDefaultKeyboard(layers[0]),
                    createDefaultKeyboard(layers[1])
                ),
                ShiftStateKeyboard(
                    createBottomRowKeyboard(shift = false, symbol = false),
                    createBottomRowKeyboard(shift = true, symbol = false)
                )
            )
        }

        override fun createDefaultKeyboard(layer: List<List<Int>>): Keyboard {
            return ScreenTypeKeyboard(
                mobile = DefaultMobileKeyboard(layer),
                tablet = DefaultTabletKeyboard(layer, extraKeys = listOf('、'.code, '。'.code))
            )
        }

        override fun createBottomRowKeyboard(shift: Boolean, symbol: Boolean): Keyboard {
            val tabletExtraKeys = if(!symbol) listOf('！'.code, '？'.code) else listOf('<'.code, '>'.code)
            return ScreenTypeKeyboard(
                mobile = DefaultBottomRowKeyboard(extraKeys = listOf('、'.code, '。'.code), isSymbols = symbol),
                tablet = DefaultTabletBottomRowKeyboard(extraKeys = tabletExtraKeys, isSymbols = symbol)
            )
        }
    }

    class KanaJIS(listener: IMEMode.Listener): MozcIMEMode(listener) {
        override val keyboardSpecification: KeyboardSpecification = KeyboardSpecification.QWERTY_KANA_JIS
        override val layoutTable: Map<Int, List<Int>> = LayoutKana.TABLE_JIS
        override fun createTextKeyboard(): Keyboard {
            val lower = KeyboardInflater.inflate(LayoutKana.ROWS_JIS_LOWER)
            val upper = KeyboardInflater.inflate(LayoutKana.ROWS_JIS_UPPER)
            return StackedKeyboard(
                ShiftStateKeyboard(
                    DefaultGridKeyboard(lower[0]),
                    DefaultGridKeyboard(upper[0])
                ),
                ShiftStateKeyboard(
                    createBottomRowKeyboard(shift = false, symbol = false),
                    createBottomRowKeyboard(shift = true, symbol = false)
                )
            )
        }

        override fun createBottomRowKeyboard(shift: Boolean, symbol: Boolean): Keyboard {
            if(symbol) return super.createBottomRowKeyboard(shift, symbol)
            val bottomRight = KeyboardInflater.inflate(listOf(LayoutKana.BOTTOM_RIGHT_JIS), layoutTable)
            val rightExtraRow =
                if(!shift) bottomRight[0][0]
                else bottomRight[1][0]
            return ScreenTypeKeyboard(
                mobile = GridKanaBottomRowKeyboard(listOf(), rightExtraRow),
                tablet = GridKanaBottomRowKeyboard(listOf(), rightExtraRow)
            )
        }
    }

    class KanaSyllables(listener: IMEMode.Listener): MozcIMEMode(listener) {
        override val keyboardSpecification: KeyboardSpecification = KeyboardSpecification.TWELVE_KEY_FLICK_KANA
        override fun createTextKeyboard(): Keyboard {
            val layers = KeyboardInflater.inflate(LayoutKana.ROWS_50ONZU)
            return StackedKeyboard(
                DefaultGridKeyboard(layers[0]),
                GridKanaBottomRowKeyboard(
                    KeyboardInflater.inflate(listOf(LayoutKana.BOTTOM_LEFT_50ONZU))[0][0],
                    KeyboardInflater.inflate(listOf(LayoutKana.BOTTOM_RIGHT_50ONZU))[0][0]
                )
            )
        }
    }

    data class Params(
        val layout: Layout = Layout.RomajiQwerty
    ): IMEMode.Params {
        override val type: String = TYPE
        override fun create(listener: IMEMode.Listener): IMEMode {
            return when(layout) {
                Layout.RomajiQwerty -> RomajiQwerty(listener)
                Layout.KanaJIS -> KanaJIS(listener)
                Layout.KanaSyllables -> KanaSyllables(listener)
            }
        }

        override fun getLabel(context: Context): String {
            val localeName = Locale.JAPANESE.displayName
            val layoutName = context.resources.getString(layout.nameKey)
            return "$localeName $layoutName"
        }

        override fun getShortLabel(context: Context): String {
            return when(layout) {
                Layout.RomajiQwerty -> "あQ"
                Layout.KanaJIS -> "JIS"
                Layout.KanaSyllables -> "あいう"
            }
        }

        companion object {
            fun parse(map: Map<String, String>): Params {
                val layout = Layout.valueOf(map["layout"] ?: Layout.RomajiQwerty.name)
                return Params(
                    layout = layout
                )
            }
        }
    }

    enum class Layout(
        @StringRes val nameKey: Int
    ) {
        RomajiQwerty(R.string.mozc_layout_romaji_qwerty),
        KanaJIS(R.string.mozc_layout_kana_jis),
        KanaSyllables(R.string.mozc_layout_kana_syllables)
    }

    companion object {
        const val TYPE: String = "mozc"
    }
}