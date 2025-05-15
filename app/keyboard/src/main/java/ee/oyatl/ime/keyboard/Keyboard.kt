package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.View

interface Keyboard {
    val listener: Listener

    fun createView(context: Context): View

    interface Listener {
        fun onChar(code: Int)
        fun onSpecial(type: SpecialKey)
        fun onShift(pressed: Boolean)
    }

    enum class SpecialKey {
        Caps,
        Space, Return, Delete,
        Language, Symbols,
    }

    enum class ShiftState {
        Unpressed, Pressed, Locked
    }
}