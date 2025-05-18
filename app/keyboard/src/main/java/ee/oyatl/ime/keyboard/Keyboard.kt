package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.View

interface Keyboard {
    fun createView(context: Context, listener: Listener): View
    fun changeState(shiftState: ShiftState)

    interface Listener {
        fun onChar(code: Int)
        fun onSpecial(type: SpecialKey, pressed: Boolean)
    }

    enum class SpecialKey {
        Shift, Caps,
        Space, Return, Delete,
        Language, Symbols,
    }

    enum class ShiftState {
        Unpressed, Pressed, Locked
    }
}