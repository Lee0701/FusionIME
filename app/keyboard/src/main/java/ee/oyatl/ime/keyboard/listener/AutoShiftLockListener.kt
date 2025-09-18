package ee.oyatl.ime.keyboard.listener

import android.view.KeyEvent
import ee.oyatl.ime.keyboard.KeyboardState

class AutoShiftLockListener(
    private val listener: KeyboardListener,
    private val stateContainer: StateContainer,
    private val lockDelay: Int = 300,
    private val autoReleaseOnInput: Boolean = true
): KeyboardListener {
    private val shiftCodes: Set<Int> = setOf(
        KeyEvent.KEYCODE_SHIFT_LEFT,
        KeyEvent.KEYCODE_SHIFT_RIGHT
    )

    private var shiftState: KeyboardState.Shift
        get() = stateContainer.shiftState
        set(value) { stateContainer.shiftState = value }

    private var shiftPressing: Boolean = false
    private var shiftTime: Long = 0
    private var inputWhileShifted: Boolean = false

    override fun onKeyDown(code: Int) {
        if(code in shiftCodes) onShiftPressed(code)
        else {
            listener.onKeyDown(code)
            autoReleaseShift()
        }
    }

    override fun onKeyUp(code: Int) {
        if(code in shiftCodes) onShiftReleased(code)
        else {
            listener.onKeyUp(code)
        }
    }

    private fun onShiftPressed(code: Int) {
        shiftPressing = true
        when(shiftState) {
            KeyboardState.Shift.Released -> {
                shiftState = KeyboardState.Shift.Pressed
            }
            KeyboardState.Shift.Pressed -> {
                if(autoReleaseOnInput) {
                    val diff = System.currentTimeMillis() - shiftTime
                    if(diff < lockDelay) shiftState = KeyboardState.Shift.Locked
                    else shiftState = KeyboardState.Shift.Released
                } else {
                    shiftState = KeyboardState.Shift.Released
                }
            }
            KeyboardState.Shift.Locked -> {
                shiftState = KeyboardState.Shift.Released
            }
        }
    }

    private fun onShiftReleased(code: Int) {
        shiftPressing = false
        when(shiftState) {
            KeyboardState.Shift.Released -> {
            }
            KeyboardState.Shift.Pressed -> {
                if(inputWhileShifted) shiftState = KeyboardState.Shift.Released
                else shiftState = KeyboardState.Shift.Pressed
            }
            KeyboardState.Shift.Locked -> {
            }
        }
        shiftTime = System.currentTimeMillis()
        inputWhileShifted = false
    }

    private fun autoReleaseShift() {
        if(!autoReleaseOnInput) return
        if(shiftState == KeyboardState.Shift.Pressed) {
            if(!shiftPressing) {
                shiftState = KeyboardState.Shift.Released
            } else {
                inputWhileShifted = true
            }
        }
    }

    interface StateContainer {
        var shiftState: KeyboardState.Shift
        var symbolState: KeyboardState.Symbol
    }
}