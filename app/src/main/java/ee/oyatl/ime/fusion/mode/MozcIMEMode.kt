package ee.oyatl.ime.fusion.mode

import android.content.Context
import android.content.res.Resources
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.google.common.base.Optional
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.candidate.VerticalScrollingCandidateView
import ee.oyatl.ime.fusion.Feature
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.layout.ExtKeyCode
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.layout.LayoutGodan
import ee.oyatl.ime.fusion.layout.LayoutKana
import ee.oyatl.ime.fusion.layout.LayoutRomaji
import ee.oyatl.ime.fusion.layout.MobileKeyboard
import ee.oyatl.ime.fusion.layout.MobileKeyboardRows
import ee.oyatl.ime.fusion.layout.TabletKeyboard
import ee.oyatl.ime.fusion.layout.TabletKeyboardRows
import ee.oyatl.ime.fusion.mozc.InputConnectionRenderer
import ee.oyatl.ime.fusion.util.KyujitaiConverter
import ee.oyatl.ime.keyboard.KeyLabel
import ee.oyatl.ime.keyboard.KeyboardConfiguration
import ee.oyatl.ime.keyboard.KeyboardState.Symbol
import ee.oyatl.ime.keyboard.KeyboardTemplate
import ee.oyatl.ime.keyboard.KeyboardView
import ee.oyatl.ime.keyboard.LayoutTable
import ee.oyatl.ime.keyboard.SoftKeyCodeMapper
import ee.oyatl.ime.keyboard.listener.FlickListener
import ee.oyatl.ime.keyboard.touchhandler.FlickDirection
import ee.oyatl.ime.keyboard.touchhandler.FlickTouchHandler
import ee.oyatl.ime.keyboard.touchhandler.TouchHandler
import org.mozc.android.inputmethod.japanese.MozcUtil
import org.mozc.android.inputmethod.japanese.PrimaryKeyCodeConverter
import org.mozc.android.inputmethod.japanese.keyboard.Keyboard.KeyboardSpecification
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Input.TouchEvent
import org.mozc.android.inputmethod.japanese.protobuf.ProtoConfig.Config
import org.mozc.android.inputmethod.japanese.session.SessionExecutor
import org.mozc.android.inputmethod.japanese.session.SessionExecutor.EvaluationCallback
import org.mozc.android.inputmethod.japanese.session.SessionHandlerFactory
import java.util.Locale

internal fun hiraganaToKatakana(text: String): String {
    return buildString(text.length) {
        for(character in text) {
            when(character) {
                in '\u3041'..'\u3096', in '\u309d'..'\u309e' ->
                    append((character.code + 0x60).toChar())
                else -> append(character)
            }
        }
    }
}

private fun transformMozcText(
    text: String,
    kyujitaiEnabled: Boolean,
    katakanaEnabled: Boolean
): String {
    val converted = KyujitaiConverter.encode(text, kyujitaiEnabled)
    return if(katakanaEnabled) hiraganaToKatakana(converted) else converted
}

private fun KeyLabel.toKatakana(): KeyLabel {
    return when(this) {
        is KeyLabel.Default -> copy(text = text?.let(::hiraganaToKatakana))
        is KeyLabel.Flick -> copy(
            text = text?.let(::hiraganaToKatakana),
            up = up?.let(::hiraganaToKatakana),
            down = down?.let(::hiraganaToKatakana),
            left = left?.let(::hiraganaToKatakana),
            right = right?.let(::hiraganaToKatakana)
        )
        else -> this
    }
}

abstract class MozcIMEMode(
    listener: IMEMode.Listener,
    val candidateViewHeight: Int,
    private val kyujitaiEnabled: Boolean = false,
    protected val katakanaEnabled: Boolean = false
): CommonIMEMode(listener) {

    private lateinit var resources: Resources
    protected abstract val keyboardSpecification: KeyboardSpecification

    private var primaryKeyCodeConverter: PrimaryKeyCodeConverter? = null
    private var sessionExecutor: SessionExecutor? = null
    private var inputConnectionRenderer: InputConnectionRenderer? = null
    private var inputConnection: InputConnection? = null

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
                    .mapIndexed { i, candidate ->
                        MozcCandidate(
                            id = candidate.id,
                            originalText = candidate.value,
                            text = transformMozcText(
                                candidate.value,
                                kyujitaiEnabled,
                                katakanaEnabled
                            ),
                            focused = index == i
                        )
                    }
                submitCandidates(candidates)
            } else {
                submitCandidates(emptyList())
            }
        } else {
            submitCandidates(emptyList())
        }
    }

    override suspend fun onLoad(context: Context) {
        super.onLoad(context)
        KyujitaiConverter.initialize(context)
        primaryKeyCodeConverter = PrimaryKeyCodeConverter(context)
        sessionExecutor = SessionExecutor.getInstanceInitializedIfNecessary(SessionHandlerFactory(context), context)
        resources = context.resources
    }

    override fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo) {
        super.onStart(inputConnection, editorInfo)
        this.inputConnection = inputConnection
        inputConnectionRenderer = InputConnectionRenderer(inputConnection, editorInfo) { text ->
            transformMozcText(text, kyujitaiEnabled, katakanaEnabled)
        }
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
            val compositionMode =
                if(katakanaEnabled) ProtoCommands.CompositionMode.FULL_KATAKANA
                else ProtoCommands.CompositionMode.HIRAGANA
            sessionExecutor.switchInputMode(
                Optional.absent(), compositionMode, renderResultCallback)
            sessionExecutor.setImposedConfig(
                Config.newBuilder()
                    .setSessionKeymap(Config.SessionKeymap.MOBILE)
                    .clearSelectionShortcut()
                    .setUseEmojiConversion(false)
                    .setIncognitoMode(passwordField)
                    .build()
            )
            val keyboardSpecification = when(symbolState) {
                Symbol.Text -> this.keyboardSpecification
                Symbol.Symbol -> KeyboardSpecification.QWERTY_KANA
                Symbol.Number -> KeyboardSpecification.NUMBER
            }
            sessionExecutor.updateRequest(
                MozcUtil.getRequestBuilder(resources, keyboardSpecification, resources.configuration).build(),
                emptyList()
            )
            sessionExecutor.resetContext()
        }
    }

    override fun createCandidateView(context: Context): View {
        val candidateView = VerticalScrollingCandidateView(context, null, candidateViewHeight).apply {
            listener = this@MozcIMEMode
        }
        this.candidateView = candidateView
        return candidateView
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        if(candidate !is MozcCandidate) return

        val convertedText = candidate.text.toString()
        if(convertedText == candidate.originalText) {
            sessionExecutor?.submitCandidate(candidate.id, Optional.absent(), renderResultCallback)
            return
        }

        inputConnection?.commitText(convertedText, 1)
        sessionExecutor?.resetContext()
        sessionExecutor?.deleteSession()
        submitCandidates(emptyList())
        restartInput()
    }

    override fun onChar(codePoint: Int) {
        if(symbolState == Symbol.Number) {
            onReset()
            return super.onChar(codePoint)
        }
        val primaryKeyCodeConverter = primaryKeyCodeConverter ?: return
        val sessionExecutor = sessionExecutor ?: return
        val eventList = emptyList<TouchEvent>()
        val keyEvent = primaryKeyCodeConverter.getPrimaryCodeKeyEvent(codePoint)
        val mozcKeyEvent = primaryKeyCodeConverter.createMozcKeyEvent(codePoint, eventList).orNull()
        if(mozcKeyEvent != null) {
            sessionExecutor.sendKey(mozcKeyEvent, keyEvent, eventList, renderResultCallback)
        } else if(keyEvent != null) {
            sessionExecutor.sendKeyEvent(keyEvent, renderResultCallback)
        }
    }

    override fun onSpecial(keyCode: Int) {
        when(keyCode) {
            KeyEvent.KEYCODE_SYM, KeyEvent.KEYCODE_NUM -> {
                super.onSpecial(keyCode)
                onReset()
                return
            }
        }
        if(symbolState == Symbol.Number) {
            onReset()
            return super.onSpecial(keyCode)
        }
        val converter = primaryKeyCodeConverter ?: return super.onSpecial(keyCode)
        when(keyCode) {
            KeyEvent.KEYCODE_SPACE -> onChar(' '.code)
            KeyEvent.KEYCODE_ENTER -> onChar(converter.keyCodeEnter)
            KeyEvent.KEYCODE_DEL -> onChar(converter.keyCodeBackspace)
            KeyEvent.KEYCODE_TAB -> onChar(converter.keyCodeUndo)
            KeyEvent.KEYCODE_DPAD_LEFT -> onChar(converter.keyCodeLeft)
            KeyEvent.KEYCODE_DPAD_RIGHT -> onChar(converter.keyCodeRight)
            KeyEvent.KEYCODE_DPAD_UP -> onChar(converter.keyCodeUp)
            KeyEvent.KEYCODE_DPAD_DOWN -> onChar(converter.keyCodeDown)
            else -> super.onSpecial(keyCode)
        }
    }

    data class MozcCandidate(
        val id: Int,
        val originalText: String,
        override val text: CharSequence,
        override val focused: Boolean
    ): CandidateView.FocusableCandidate

    class RomajiQwerty(
        listener: IMEMode.Listener,
        candidateViewHeight: Int,
        numberRow: Boolean,
        kyujitaiEnabled: Boolean,
        katakanaEnabled: Boolean
    ): MozcIMEMode(listener, candidateViewHeight, kyujitaiEnabled, katakanaEnabled) {
        private val numberRow = Feature.NumberRow.availableInCurrentVersion && numberRow

        override val keyboardSpecification: KeyboardSpecification = KeyboardSpecification.QWERTY_KANA
        override val textLayoutTable: LayoutTable = LayoutTable.fromShiftStates(LayoutExt.TABLE + LayoutRomaji.TABLE_QWERTY)
        override val textKeyboardTemplate: KeyboardTemplate = KeyboardTemplate.ByScreenMode(
            mobile = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    if(this.numberRow) MobileKeyboard.numbers() else KeyboardConfiguration(),
                    MobileKeyboard.alphabetic(semicolon = true),
                    MobileKeyboard.bottom(dpad = true)
                ),
                contentRows = (if(this.numberRow) MobileKeyboardRows.NUMBERS else listOf()) + MobileKeyboardRows.MINUS
            ),
            tablet = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    if(this.numberRow) TabletKeyboard.numbers() else KeyboardConfiguration(),
                    TabletKeyboard.alphabetic(semicolon = true),
                    TabletKeyboard.bottom()
                ),
                contentRows = (if(this.numberRow) TabletKeyboardRows.NUMBERS else listOf()) + TabletKeyboardRows.MINUS
            )
        )
    }

    class Kana12Key(
        listener: IMEMode.Listener,
        candidateViewHeight: Int,
        val flickMode: FlickMode,
        val flickHint: Boolean,
        kyujitaiEnabled: Boolean,
        katakanaEnabled: Boolean
    ): MozcIMEMode(
        listener,
        candidateViewHeight,
        kyujitaiEnabled,
        katakanaEnabled
    ), FlickListener {
        override val keyboardSpecification: KeyboardSpecification = flickMode.keyboardSpecification
        override val textLayoutTable: LayoutTable = LayoutTable.fromFlick4Dirs(LayoutKana.TABLE_12KEY.mapValues { (_, list) -> list.map { it.code } })
        override val textKeyboardTemplate: KeyboardTemplate = KeyboardTemplate.ByScreenMode(
            mobile = KeyboardTemplate.Basic(
                configuration = LayoutKana.mobileKeyboardConfiguration12Key(),
                contentRows = LayoutKana.ROWS_12KEY
            )
        )

        // Remove flick labels if toggle only mode
        // Show them as hints only when option is on
        private val keyLabels12Key: Map<Int, KeyLabel> = (
            if(flickMode == FlickMode.ToggleOnly) LayoutKana.LABELS_12KEY.mapValues { (_, v) ->
                KeyLabel.Default(v.text)
            }
            else LayoutKana.LABELS_12KEY.mapValues { (_, v) ->
                v.copy(showAsHint = flickHint)
            }
        ).mapValues { (_, label) ->
            if(katakanaEnabled) label.toKatakana() else label
        }

        override val keyLabels: Map<Int, KeyLabel>
            get() =
                if(symbolState == Symbol.Text) super.keyLabels + keyLabels12Key
                else super.keyLabels

        private val flicks: MutableMap<Int, FlickDirection> = mutableMapOf()

        override fun createTouchHandler(
            keyboardView: KeyboardView,
            context: Context,
            symbolState: Symbol
        ): TouchHandler {
            if(symbolState == Symbol.Text) {
                val preference = PreferenceManager.getDefaultSharedPreferences(context)
                val defaultValue = context.resources.getInteger(R.integer.flick_sensitivity_default).toFloat()
                val flickSensitivity = preference.getFloat("flick_sensitivity", defaultValue).toInt()
                return FlickTouchHandler(keyboardView, flickSensitivity, diagonal = false, multiFlick = false, sendOnUp = true)
            } else {
                return super.createTouchHandler(keyboardView, context, symbolState)
            }
        }

        override fun onKeyDown(keyCode: Int, metaState: Int): Boolean {
            if(keyCode <= 0 || !keyCharacterMap.isPrintingKey(keyCode)) {
                return super.onKeyDown(keyCode, metaState)
            }
            return false
        }

        override fun onKeyUp(keyCode: Int, metaState: Int): Boolean {
            if(keyCode <= 0 || !keyCharacterMap.isPrintingKey(keyCode)) {
                return super.onKeyUp(keyCode, metaState)
            }
            val direction = flicks[keyCode]
            when(val item = currentLayoutTable[keyCode]) {
                is LayoutTable.FlickItem -> onChar(item.forFlickDirection(direction))
                else -> onChar(item?.normal ?: keyCharacterMap.get(keyCode, metaState))
            }
            this.flicks -= keyCode
            return false
        }

        override fun onFlick(
            keyCode: Int,
            direction: FlickDirection
        ): Boolean {
            if(flickMode == FlickMode.ToggleOnly) return false
            this.flicks += keyCode to direction
            return false
        }
    }


    class Godan(
        listener: IMEMode.Listener,
        candidateViewHeight: Int,
        val flickHint: Boolean,
        kyujitaiEnabled: Boolean,
        katakanaEnabled: Boolean
    ): MozcIMEMode(listener, candidateViewHeight, kyujitaiEnabled, katakanaEnabled) {
        override val keyboardSpecification: KeyboardSpecification =
            KeyboardSpecification.GODAN_KANA

        override val textLayoutTable: LayoutTable =
            LayoutTable.fromFlick4Dirs(LayoutGodan.TABLE)

        override val textKeyboardTemplate: KeyboardTemplate =
            KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = LayoutGodan.mobileKeyboardConfiguration(),
                    contentRows = emptyList()
                )
            )

        // Show flick hints only when option is on
        private val keyLabelsGodan = LayoutGodan.LABELS.mapValues { (_, label) ->
            val labelWithHint = label.copy(showAsHint = flickHint)
            if(katakanaEnabled) labelWithHint.toKatakana() else labelWithHint
        }

        override val keyLabels: Map<Int, KeyLabel>
            get() =
                if(symbolState == Symbol.Text) super.keyLabels + keyLabelsGodan
                else super.keyLabels

        private val flicks: MutableMap<Int, FlickDirection> = mutableMapOf()

        override fun createTouchHandler(
            keyboardView: KeyboardView,
            context: Context,
            symbolState: Symbol
        ): TouchHandler {
            if(symbolState != Symbol.Text) {
                return super.createTouchHandler(keyboardView, context, symbolState)
            }

            val preference = PreferenceManager.getDefaultSharedPreferences(context)
            val defaultValue =
                context.resources.getInteger(R.integer.flick_sensitivity_default).toFloat()
            val flickSensitivity =
                preference.getFloat("flick_sensitivity", defaultValue).toInt()

            return FlickTouchHandler(
                keyboardView,
                flickSensitivity,
                diagonal = false,
                multiFlick = false,
                sendOnUp = true
            )
        }

        override fun onKeyDown(keyCode: Int, metaState: Int): Boolean {
            if(keyCode <= 0 || !keyCharacterMap.isPrintingKey(keyCode)) {
                return super.onKeyDown(keyCode, metaState)
            }
            return false
        }

        override fun onKeyUp(keyCode: Int, metaState: Int): Boolean {
            if(keyCode <= 0 || !keyCharacterMap.isPrintingKey(keyCode)) {
                return super.onKeyUp(keyCode, metaState)
            }
            val direction = flicks.remove(keyCode)
            when(val item = currentLayoutTable[keyCode]) {
                is LayoutTable.FlickItem -> onChar(item.forFlickDirection(direction))
                else -> onChar(item?.normal ?: keyCharacterMap.get(keyCode, metaState))
            }
            return false
        }

        override fun onFlick(keyCode: Int, direction: FlickDirection): Boolean {
            if(symbolState != Symbol.Text) {
                return super.onFlick(keyCode, direction)
            }
            flicks[keyCode] = direction
            return false
        }
    }

    class KanaJIS(
        listener: IMEMode.Listener,
        candidateViewHeight: Int,
        kyujitaiEnabled: Boolean,
        katakanaEnabled: Boolean
    ): MozcIMEMode(listener, candidateViewHeight, kyujitaiEnabled, katakanaEnabled) {
        override val keyboardSpecification: KeyboardSpecification = KeyboardSpecification.QWERTY_KANA_JIS
        override val textLayoutTable: LayoutTable = LayoutTable.fromShiftStates(LayoutExt.TABLE + LayoutKana.TABLE_JIS)
        override val textKeyboardTemplate: KeyboardTemplate = KeyboardTemplate.ByScreenMode(
            mobile = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    MobileKeyboard.numbers(),
                    MobileKeyboard.alphabetic(semicolon = true, shiftDeleteWidth = 1f),
                    MobileKeyboard.bottom(left = ExtKeyCode.KEYCODE_KANA_EQUALS, right = ExtKeyCode.KEYCODE_KANA_SLASH, dpad = true)
                ),
                contentRows = MobileKeyboardRows.JIS,
                softKeyCodeMapper = SoftKeyCodeMapper(mapOf(
                    KeyEvent.KEYCODE_MINUS to ExtKeyCode.KEYCODE_KANA_MINUS,
                    KeyEvent.KEYCODE_APOSTROPHE to ExtKeyCode.KEYCODE_KANA_APOSTROPHE,
                    KeyEvent.KEYCODE_LEFT_BRACKET to ExtKeyCode.KEYCODE_KANA_VOICED_MARK
                ))
            ),
            tablet = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    TabletKeyboard.numbers(delete = true, spacerOnDelete = false),
                    TabletKeyboard.alphabetic(semicolon = true, delete = false, spacerOnDelete = true),
                    TabletKeyboard.bottom()
                ),
                contentRows = TabletKeyboardRows.JIS
            )
        )
    }

    class KanaSyllables(
        listener: IMEMode.Listener,
        candidateViewHeight: Int,
        keys: String,
        keyLayout: LayoutKana.KeyLayout,
        kyujitaiEnabled: Boolean,
        katakanaEnabled: Boolean
    ): MozcIMEMode(listener, candidateViewHeight, kyujitaiEnabled, katakanaEnabled) {
        private val contentRows = LayoutKana.generateContentRows(keys, keyLayout)
        override val keyboardSpecification: KeyboardSpecification = KeyboardSpecification.TWELVE_KEY_FLICK_KANA
        override val textKeyboardTemplate: KeyboardTemplate = KeyboardTemplate.ByScreenMode(
            mobile = KeyboardTemplate.Basic(
                configuration = LayoutKana.mobileKeyboardConfigurationSyllables(contentRows),
                contentRows = emptyList()
            ),
            tablet = KeyboardTemplate.Basic(
                configuration = LayoutKana.tabletKeyboardConfigurationSyllables(contentRows),
                contentRows = emptyList()
            )
        )
    }

    data class Params(
        val layout: Layout = Layout.RomajiQwerty,
        val numberRow: Boolean = false,
        val candidateViewHeight: Int = 2,
        val flickMode: FlickMode = FlickMode.FlickToggle,
        val flickHint: Boolean = false,
        val syllablesKeyLayout: LayoutKana.KeyLayout,
        val kyujitaiEnabled: Boolean = false,
        val katakanaEnabled: Boolean = false
    ): IMEMode.Params {
        override val type: String = TYPE
        override fun create(listener: IMEMode.Listener): IMEMode {
            return when(layout) {
                Layout.RomajiQwerty -> RomajiQwerty(
                    listener, candidateViewHeight, numberRow,
                    kyujitaiEnabled, katakanaEnabled
                )
                Layout.Kana12Key -> Kana12Key(
                    listener, candidateViewHeight, flickMode, flickHint,
                    kyujitaiEnabled, katakanaEnabled
                )
                Layout.Godan -> Godan(
                    listener, candidateViewHeight, flickHint,
                    kyujitaiEnabled, katakanaEnabled
                )
                Layout.KanaJIS -> KanaJIS(
                    listener, candidateViewHeight,
                    kyujitaiEnabled, katakanaEnabled
                )
                Layout.KanaSyllables -> KanaSyllables(
                    listener, candidateViewHeight, LayoutKana.KEYS_AIUEO,
                    syllablesKeyLayout, kyujitaiEnabled, katakanaEnabled
                )
                Layout.KanaIroha -> KanaSyllables(
                    listener, candidateViewHeight, LayoutKana.KEYS_IROHA,
                    syllablesKeyLayout, kyujitaiEnabled, katakanaEnabled
                )
            }
        }

        override fun getLabel(context: Context): String {
            val localeName = Locale.JAPANESE.displayName
            val layoutName = context.resources.getString(layout.nameKey)
            return "$localeName $layoutName"
        }

        override fun getShortLabel(context: Context, params: List<IMEMode.Params>): String {
            val mozcParams = params.filterIsInstance<Params>().filterNot { it == this }
            // If this is the only Mozc mode
            if(mozcParams.isEmpty()) return if(katakanaEnabled) "ア" else "あ"
            // If not, use specific layout name
            val label = when(layout) {
                Layout.RomajiQwerty -> "あQ"
                Layout.Kana12Key -> "あK"
                Layout.Godan -> "あG"
                Layout.KanaJIS -> "JIS"
                Layout.KanaSyllables -> "あいう"
                Layout.KanaIroha -> "いろは"
            }
            return if(katakanaEnabled) hiraganaToKatakana(label) else label
        }

        companion object {
            fun parse(map: Map<String, String>): Params {
                val layout = Layout.entries.find { it.name == map["layout"] } ?: Layout.RomajiQwerty
                val numberRow = map["number_row"]?.toBoolean() ?: false
                val candidateViewHeight = map["candidate_view_height"]?.toFloatOrNull()?.toInt() ?: 2
                val flickMode = FlickMode.valueOf(map["flick_mode"] ?: FlickMode.FlickToggle.name)
                val flickHint = map["flick_hint"]?.toBoolean() ?: false
                val syllablesKeyLayout = LayoutKana.KeyLayout.entries.find { it.name == map["syllables_key_layout"] } ?: LayoutKana.KeyLayout.Horizontal
                val kyujitaiEnabled = map["kyujitai_enabled"]?.toBoolean() ?: false
                val katakanaEnabled = map["katakana_enabled"]?.toBoolean() ?: false
                return Params(
                    layout = layout,
                    candidateViewHeight = candidateViewHeight,
                    numberRow = numberRow,
                    flickMode = flickMode,
                    flickHint = flickHint,
                    syllablesKeyLayout = syllablesKeyLayout,
                    kyujitaiEnabled = kyujitaiEnabled,
                    katakanaEnabled = katakanaEnabled
                )
            }
        }
    }

    enum class Layout(
        @StringRes val nameKey: Int
    ) {
        RomajiQwerty(R.string.mozc_layout_romaji_qwerty),
        Kana12Key(R.string.mozc_layout_kana_12key),
        Godan(R.string.mozc_layout_godan),
        KanaJIS(R.string.mozc_layout_kana_jis),
        KanaSyllables(R.string.mozc_layout_kana_syllables),
        KanaIroha(R.string.mozc_layout_kana_iroha)
    }

    enum class FlickMode(
        val keyboardSpecification: KeyboardSpecification
    ) {
        FlickToggle(KeyboardSpecification.TWELVE_KEY_TOGGLE_FLICK_KANA),
        ToggleOnly(KeyboardSpecification.TWELVE_KEY_TOGGLE_KANA),
        FlickOnly(KeyboardSpecification.TWELVE_KEY_FLICK_KANA)
    }

    companion object {
        const val TYPE: String = "mozc"
    }
}
