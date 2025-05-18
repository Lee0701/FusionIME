package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.View

class ShiftStateKeyboard(
    private val normal: Keyboard,
    private val shifted: Keyboard = normal,
    private val locked: Keyboard = shifted,
): Keyboard {
    private lateinit var keyboardSwitcher: ShiftKeyboardSwitcher

    override fun createView(context: Context, listener: Keyboard.Listener): View {
        val normal = normal.createView(context, listener)
        val shifted = shifted.createView(context, listener)
        val locked = locked.createView(context, listener)
        keyboardSwitcher = ShiftKeyboardSwitcher(context, normal, shifted, locked)
        return keyboardSwitcher.view
    }

    override fun changeState(shiftState: Keyboard.ShiftState) {
        keyboardSwitcher.switch(shiftState)
        normal.changeState(shiftState)
        shifted.changeState(shiftState)
        locked.changeState(shiftState)
    }
}