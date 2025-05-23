package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.View
import ee.oyatl.ime.keyboard.listener.KeyboardListener

class ShiftStateKeyboard(
    private val normal: Keyboard,
    private val shifted: Keyboard = normal,
    private val locked: Keyboard = shifted,
): Keyboard {
    private lateinit var keyboardSwitcher: ShiftKeyboardSwitcher

    override val numRows: Int = listOf(normal, shifted, locked).maxOf { it.numRows }

    override fun createView(context: Context, listener: KeyboardListener, height: Int): View {
        keyboardSwitcher = ShiftKeyboardSwitcher(
            context,
            normal.createView(context, listener, height),
            shifted.createView(context, listener, height),
            locked.createView(context, listener, height)
        )
        normal.setShiftState(KeyboardState.Shift.Released)
        shifted.setShiftState(KeyboardState.Shift.Pressed)
        locked.setShiftState(KeyboardState.Shift.Locked)
        return keyboardSwitcher.view
    }

    override fun setShiftState(state: KeyboardState.Shift) {
        keyboardSwitcher.switch(state)
    }
}