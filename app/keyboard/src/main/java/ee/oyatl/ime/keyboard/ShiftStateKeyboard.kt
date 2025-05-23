package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.View
import ee.oyatl.ime.keyboard.listener.KeyboardListener

class ShiftStateKeyboard(
    private val normal: Keyboard,
    private val shifted: Keyboard = normal,
    private val locked: Keyboard = shifted,
): Keyboard {
    private var keyboardSwitcher: ShiftKeyboardSwitcher? = null

    override val numRows: Int = listOf(normal, shifted, locked).maxOf { it.numRows }

    override fun createView(context: Context, listener: KeyboardListener, height: Int): View {
        val keyboardSwitcher = ShiftKeyboardSwitcher(
            context,
            normal.createView(context, listener, height),
            shifted.createView(context, listener, height),
            locked.createView(context, listener, height)
        )
        this.keyboardSwitcher = keyboardSwitcher
        return keyboardSwitcher.view
    }

    override fun setShiftState(state: KeyboardState.Shift) {
        keyboardSwitcher?.switch(state)
        normal.setShiftState(state)
        shifted.setShiftState(state)
        locked.setShiftState(state)
    }
}