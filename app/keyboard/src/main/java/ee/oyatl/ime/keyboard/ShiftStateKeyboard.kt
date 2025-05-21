package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.View

class ShiftStateKeyboard(
    private val normal: Keyboard,
    private val shifted: Keyboard = normal,
    private val locked: Keyboard = shifted,
): Keyboard {
    private lateinit var keyboardSwitcher: ShiftKeyboardSwitcher

    override val numRows: Int = listOf(normal, shifted, locked).maxOf { it.numRows }

    override fun createView(context: Context, listener: Keyboard.Listener, height: Int): View {
        val normal = normal.createView(context, listener, height)
        val shifted = shifted.createView(context, listener, height)
        val locked = locked.createView(context, listener, height)
        keyboardSwitcher = ShiftKeyboardSwitcher(context, normal, shifted, locked)
        return keyboardSwitcher.view
    }

    override fun changeState(state: KeyboardStateSet) {
        keyboardSwitcher.switch(state.shift)
        normal.changeState(state)
        shifted.changeState(state)
        locked.changeState(state)
    }
}