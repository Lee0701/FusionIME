package ee.oyatl.ime.keyboard.keyboardset

import android.content.Context
import android.view.View
import ee.oyatl.ime.keyboard.GridBottomRowKeyboard
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.ShiftKeyboardSwitcher

class GridBottomKeyboardSet(
    private val listener: Keyboard.Listener,
    private val normalLayout: String,
    private val shiftedLayout: String = normalLayout,
    private val lockedLayout: String = shiftedLayout
): KeyboardSet {
    private lateinit var keyboardSwitcher: ShiftKeyboardSwitcher

    override fun initView(context: Context): View {
        run {
            val normal = GridBottomRowKeyboard(normalLayout, Keyboard.ShiftState.Unpressed).createView(context, listener)
            val shifted = GridBottomRowKeyboard(shiftedLayout, Keyboard.ShiftState.Pressed).createView(context, listener)
            val locked = GridBottomRowKeyboard(lockedLayout, Keyboard.ShiftState.Locked).createView(context, listener)
            keyboardSwitcher = ShiftKeyboardSwitcher(context, normal, shifted, locked)
        }
        return keyboardSwitcher.view
    }

    override fun getView(shiftState: Keyboard.ShiftState, candidates: Boolean): View {
        keyboardSwitcher.switch(shiftState)
        return keyboardSwitcher.view
    }
}