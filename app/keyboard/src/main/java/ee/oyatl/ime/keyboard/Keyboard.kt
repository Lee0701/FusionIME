package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.View

interface Keyboard {
    val numRows: Int

    fun createView(context: Context, listener: Listener, height: Int): View
    fun changeState(state: KeyboardStateSet)

    interface Listener {
        fun onChar(code: Int)
        fun onSpecial(type: SpecialKey, pressed: Boolean)
    }

    enum class SpecialKey {
        Shift, Caps,
        Space, Return, Delete,
        Language, Symbols, Numbers
    }
}