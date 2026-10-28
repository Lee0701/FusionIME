package ee.oyatl.ime.keyboard.listener

import android.view.KeyEvent
import ee.oyatl.ime.fusion.keyboard.core.KeyboardParams
import ee.oyatl.ime.fusion.keyboard.core.KeyboardState

class ShiftStateManager(
    val listener: KeyboardListener,
    val params: KeyboardParams,
    val stateHolder: ShiftStateHolder
): KeyboardListener {
    private var shiftPressing: Boolean = false
    private var shiftTime: Long = 0
    private var inputWhileShifted: Boolean = false

    override fun onKeyDown(keyCode: Int, metaState: Int): Boolean {
        when(keyCode) {
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                onShiftPressed(keyCode)
                return true
            }
            else -> {
                return true
            }
        }
    }

    override fun onKeyUp(keyCode: Int, metaState: Int): Boolean {
        when(keyCode) {
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                onShiftReleased(keyCode)
                return true
            }
            else -> {
                autoReleaseShift()
                return true
            }
        }
    }

    override fun onReset() {
        stateHolder.shiftState = KeyboardState.Shift.Released
    }

    private fun onShiftPressed(code: Int) {
        shiftPressing = true
        if(params.shiftAutoRelease) {
            val diff = System.currentTimeMillis() - shiftTime
            if(diff < params.shiftLockDelay) {
                stateHolder.shiftState = KeyboardState.Shift.Locked
                return
            }
        }
        when(stateHolder.shiftState) {
            KeyboardState.Shift.Released -> {
                stateHolder.shiftState = KeyboardState.Shift.Pressed
            }
            KeyboardState.Shift.Pressed, KeyboardState.Shift.AutoShifted -> {
                stateHolder.shiftState = KeyboardState.Shift.Released
            }
            KeyboardState.Shift.Locked -> {
                stateHolder.shiftState = KeyboardState.Shift.Released
            }
        }
    }

    private fun onShiftReleased(code: Int) {
        shiftPressing = false
        when(stateHolder.shiftState) {
            KeyboardState.Shift.Released -> {
            }
            KeyboardState.Shift.Pressed, KeyboardState.Shift.AutoShifted -> {
                if(inputWhileShifted) stateHolder.shiftState = KeyboardState.Shift.Released
                else stateHolder.shiftState = KeyboardState.Shift.Pressed
            }
            KeyboardState.Shift.Locked -> {
            }
        }
        shiftTime = System.currentTimeMillis()
        inputWhileShifted = false
    }

    private fun autoReleaseShift() {
        if(!params.shiftAutoRelease) return
        if(stateHolder.shiftState == KeyboardState.Shift.Pressed) {
            if(!shiftPressing) {
                stateHolder.shiftState = KeyboardState.Shift.Released
            } else {
                inputWhileShifted = true
            }
        }
    }

    interface ShiftStateHolder {
        var shiftState: KeyboardState.Shift
    }

    companion object {
        val KEY_CODES = setOf(
            KeyEvent.KEYCODE_SHIFT_LEFT,
            KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_CAPS_LOCK
        )
    }
}