package ee.oyatl.ime.keyboard.listener

import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.KeyboardState

class AutoShiftLockListener(
    private val listener: KeyboardListener,
    private val stateContainer: StateContainer,
    private val lockDelay: Int = 300,
    private val autoReleaseOnInput: Boolean = true
): KeyboardListener {
    private var state: KeyboardState.Shift
        get() = stateContainer.shiftState
        set(value) { stateContainer.shiftState = value }

    private var shiftPressing: Boolean = false
    private var shiftTime: Long = 0
    private var inputWhileShifted: Boolean = false

    override fun onKeyDown(code: Int) {
        if(code == Keyboard.SpecialKey.Shift.code) onShiftPressed()
        else {
            listener.onKeyDown(code)
            autoReleaseShift()
        }
    }

    override fun onKeyUp(code: Int) {
        if(code == Keyboard.SpecialKey.Shift.code) onShiftReleased()
        else listener.onKeyUp(code)
    }

    private fun onShiftPressed() {
        listener.onKeyDown(Keyboard.SpecialKey.Shift.code)
        shiftPressing = true
        when(state) {
            KeyboardState.Shift.Released -> {
                state = KeyboardState.Shift.Pressed
            }
            KeyboardState.Shift.Pressed -> {
                if(autoReleaseOnInput) {
                    val diff = System.currentTimeMillis() - shiftTime
                    if(diff < lockDelay) state = KeyboardState.Shift.Locked
                    else state = KeyboardState.Shift.Released
                } else {
                    state = KeyboardState.Shift.Released
                }
            }
            KeyboardState.Shift.Locked -> {
                state = KeyboardState.Shift.Released
            }
        }
    }

    private fun onShiftReleased() {
        listener.onKeyUp(Keyboard.SpecialKey.Shift.code)
        shiftPressing = false
        when(state) {
            KeyboardState.Shift.Released -> {
            }
            KeyboardState.Shift.Pressed -> {
                if(inputWhileShifted) state = KeyboardState.Shift.Released
                else state = KeyboardState.Shift.Pressed
            }
            KeyboardState.Shift.Locked -> {
            }
        }
        shiftTime = System.currentTimeMillis()
        inputWhileShifted = false
    }

    private fun autoReleaseShift() {
        if(!autoReleaseOnInput) return
        if(state == KeyboardState.Shift.Pressed) {
            if(!shiftPressing) {
                state = KeyboardState.Shift.Released
            } else {
                inputWhileShifted = true
            }
        }
    }

    interface StateContainer {
        var shiftState: KeyboardState.Shift
    }
}