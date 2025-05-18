package ee.oyatl.ime.keyboard.keyboardset

import android.content.Context
import android.view.View
import ee.oyatl.ime.keyboard.DefaultGridKeyboard
import ee.oyatl.ime.keyboard.DefaultMobileKeyboard
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.ShiftKeyboardSwitcher

class GridKeyboardSet(
    private val listener: Keyboard.Listener,
    private val normalLayout: List<String>,
    private val shiftedLayout: List<String> = normalLayout,
    private val lockedLayout: List<String> = shiftedLayout
): KeyboardSet {
    private lateinit var keyboardSwitcher: ShiftKeyboardSwitcher

    override fun initView(context: Context): View {
        run {
            val normal = DefaultGridKeyboard(normalLayout).createView(context, listener)
            val shifted = DefaultGridKeyboard(shiftedLayout).createView(context, listener)
            val locked = DefaultGridKeyboard(lockedLayout).createView(context, listener)
            keyboardSwitcher = ShiftKeyboardSwitcher(context, normal, shifted, locked)
        }
        return keyboardSwitcher.view
    }

    override fun getView(shiftState: Keyboard.ShiftState, candidates: Boolean): View {
        keyboardSwitcher.switch(shiftState)
        return keyboardSwitcher.view
    }
}