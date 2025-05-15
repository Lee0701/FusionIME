package ee.oyatl.ime.fusion

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import com.google.common.base.Optional
import ee.oyatl.ime.fusion.mozc.InputConnectionRenderer
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.keyboardset.BottomRowKeyboardSet
import ee.oyatl.ime.keyboard.keyboardset.DefaultKeyboardSet
import ee.oyatl.ime.keyboard.keyboardset.KeyboardSet
import ee.oyatl.ime.keyboard.keyboardset.StackedKeyboardSet
import ee.oyatl.ime.keyboard.layout.LayoutQwerty
import org.mozc.android.inputmethod.japanese.PrimaryKeyCodeConverter
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Input.TouchEvent
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
    private lateinit var primaryKeyCodeConverter: PrimaryKeyCodeConverter
    private lateinit var sessionExecutor: SessionExecutor
    private var inputConnectionRenderer: InputConnectionRenderer? = null

    private val renderResultCallback = EvaluationCallback { command, triggeringKeyEvent ->
        if(!command.isPresent || !triggeringKeyEvent.isPresent) return@EvaluationCallback
        inputConnectionRenderer?.renderInputConnection(command.get(), triggeringKeyEvent.get())
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
        sessionExecutor.resetContext()
    }

    override fun onCreateInputView(): View {
        keyboardSet.initView(this)
        updateInputView()
        return keyboardSet.getView(keyboardListener.shiftState, false)
    }

    fun updateInputView() {
        setInputView(keyboardSet.getView(keyboardListener.shiftState, false))
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
}