package ee.oyatl.ime.keyboard

abstract class CommonKeyboardListener(
    private val callback: Callback,
    private val autoReleaseShift: Boolean = true
): Keyboard.Listener {
    var shiftState: Keyboard.ShiftState = Keyboard.ShiftState.Unpressed
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
            val oldShiftState = shiftState
            if(pressed) onShiftPressed()
            else onShiftReleased()
            if(shiftState != oldShiftState) callback.updateInputView()
        }
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

    protected fun autoReleaseShift() {
        if(!autoReleaseShift) return
        if(shiftState == Keyboard.ShiftState.Pressed) {
            if(!shiftPressing) {
                shiftState = Keyboard.ShiftState.Unpressed
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