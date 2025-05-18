package ee.oyatl.ime.keyboard

abstract class CommonKeyboardListener(
    private val callback: Callback,
    private val autoReleaseShift: Boolean = true
): Keyboard.Listener {
    var state: KeyboardStateSet = KeyboardStateSet()
        private set
    private var shiftPressing: Boolean = false
    private var shiftTime: Long = 0
    private var inputWhileShifted: Boolean = false

    override fun onChar(code: Int) {
        autoReleaseShift()
    }

    override fun onSpecial(type: Keyboard.SpecialKey, pressed: Boolean) {
        if(type == Keyboard.SpecialKey.Shift) {
            shiftPressing = pressed
            val oldShiftState = state
            if(pressed) onShiftPressed()
            else onShiftReleased()
            if(state != oldShiftState) callback.updateInputView()
        }
    }

    private fun onShiftPressed() {
        when(state.shift) {
            KeyboardState.Shift.Released -> {
                state = state.copy(shift = KeyboardState.Shift.Pressed)
            }
            KeyboardState.Shift.Pressed -> {
                if(autoReleaseShift) {
                    val diff = System.currentTimeMillis() - shiftTime
                    if(diff < 300) state = state.copy(shift = KeyboardState.Shift.Locked)
                    else state = state.copy(shift = KeyboardState.Shift.Released)
                } else {
                    state = state.copy(shift = KeyboardState.Shift.Released)
                }
            }
            KeyboardState.Shift.Locked -> {
                state = state.copy(shift = KeyboardState.Shift.Released)
            }
        }
    }

    private fun onShiftReleased() {
        when(state.shift) {
            KeyboardState.Shift.Released -> {
            }
            KeyboardState.Shift.Pressed -> {
                if(inputWhileShifted) state = state.copy(shift = KeyboardState.Shift.Released)
                else state = state.copy(shift = KeyboardState.Shift.Pressed)
            }
            KeyboardState.Shift.Locked -> {
            }
        }
        shiftTime = System.currentTimeMillis()
        inputWhileShifted = false
    }

    protected fun autoReleaseShift() {
        if(!autoReleaseShift) return
        if(state.shift == KeyboardState.Shift.Pressed) {
            if(!shiftPressing) {
                state = state.copy(shift = KeyboardState.Shift.Released)
                callback.updateInputView()
            } else {
                inputWhileShifted = true
            }
        }
    }

    interface Callback {
        fun updateInputView()
    }
}