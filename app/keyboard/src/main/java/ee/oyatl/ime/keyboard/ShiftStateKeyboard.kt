package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.View
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.switcher.ShiftKeyboardSwitcher

class ShiftStateKeyboard(
    private val normal: Keyboard,
    private val shifted: Keyboard = normal,
    private val locked: Keyboard = shifted,
): Keyboard {
    private var keyboardSwitcher: ShiftKeyboardSwitcher? = null

    override val numRows: Int = listOf(normal, shifted, locked).maxOf { it.numRows }

    override fun createView(context: Context, listener: KeyboardListener, params: KeyboardViewParams): View {
        val keyboardSwitcher = ShiftKeyboardSwitcher(
            context,
            normal.createView(context, listener, params),
            shifted.createView(context, listener, params),
            locked.createView(context, listener, params)
        )
        normal.setState(KeyboardState.Shift.Released)
        shifted.setState(KeyboardState.Shift.Pressed)
        locked.setState(KeyboardState.Shift.Locked)
        this.keyboardSwitcher = keyboardSwitcher
        return keyboardSwitcher.view
    }

    override fun setState(state: KeyboardState) {
        if(state is KeyboardState.Shift) keyboardSwitcher?.switch(state)
        listOf(normal, shifted, locked).forEach { it.setState(state) }
    }
}