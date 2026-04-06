package ee.oyatl.ime.keyboard.listener

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import ee.oyatl.ime.keyboard.FlickKeyCode
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.KeyboardParams
import ee.oyatl.ime.keyboard.KeyboardState

class ShiftStateManager(
    val listener: KeyboardListener,
    val params: KeyboardParams
): KeyboardListener {
    private val handler = Handler(Looper.getMainLooper())

    var shiftState: KeyboardState.Shift = KeyboardState.Shift.Released
        set(value) {
            field = value
            when(value) {
                KeyboardState.Shift.Released -> {
                    listener.onKeyUp(KeyEvent.KEYCODE_CAPS_LOCK, metaState)
                    listener.onKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT, metaState)
                }
                KeyboardState.Shift.Pressed -> {
                    listener.onKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT, metaState)
                }
                KeyboardState.Shift.Locked -> {
                    listener.onKeyDown(KeyEvent.KEYCODE_CAPS_LOCK, metaState)
                }
            }
        }
    private var shiftPressing: Boolean = false
    private var shiftTime: Long = 0
    private var inputWhileShifted: Boolean = false

    private val metaState: Int get() = when(shiftState) {
        KeyboardState.Shift.Released -> 0
        KeyboardState.Shift.Pressed -> KeyEvent.META_SHIFT_ON
        KeyboardState.Shift.Locked -> KeyEvent.META_CAPS_LOCK_ON
    }

    override fun onKeyDown(keyCode: Int, metaState: Int) {
        when(keyCode and FlickKeyCode.MASK_KEYCODE) {
            KeyEvent.KEYCODE_DEL -> onDeletePressed(keyCode)
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> onShiftPressed(keyCode)
        }
    }

    override fun onKeyUp(keyCode: Int, metaState: Int) {
        when(keyCode and FlickKeyCode.MASK_KEYCODE) {
            KeyEvent.KEYCODE_DEL -> onDeleteReleased(keyCode)
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> onShiftReleased(keyCode)
            else -> {
                listener.onKeyDown(keyCode, metaState)
                listener.onKeyUp(keyCode, metaState)
                autoReleaseShift()
            }
        }
    }

    override fun onReset() {
        shiftState = KeyboardState.Shift.Released
    }

    private fun repeat(code: Int) {
        listener.onKeyDown(code, metaState)
        listener.onKeyUp(code, metaState)
        handler.postDelayed({ repeat(code) }, params.repeatInterval.toLong())
    }

    private fun onDeletePressed(code: Int) {
        listener.onKeyDown(code, metaState)
        handler.postDelayed({ repeat(code) }, params.repeatDelay.toLong())
    }

    private fun onDeleteReleased(code: Int) {
        listener.onKeyUp(code, metaState)
        handler.removeCallbacksAndMessages(null)
    }

    private fun onShiftPressed(code: Int) {
        shiftPressing = true
        when(shiftState) {
            KeyboardState.Shift.Released -> {
                shiftState = KeyboardState.Shift.Pressed
            }
            KeyboardState.Shift.Pressed -> {
                if(params.shiftAutoRelease) {
                    val diff = System.currentTimeMillis() - shiftTime
                    if(diff < params.shiftLockDelay) shiftState = KeyboardState.Shift.Locked
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
        if(!params.shiftAutoRelease) return
        if(shiftState == KeyboardState.Shift.Pressed) {
            if(!shiftPressing) {
                shiftState = KeyboardState.Shift.Released
            } else {
                inputWhileShifted = true
            }
        }
    }

}