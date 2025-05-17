package ee.oyatl.ime.keyboard.keyboardset

import android.content.Context
import android.view.View
import ee.oyatl.ime.keyboard.GridKanaBottomRowKeyboard
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.ShiftKeyboardSwitcher

class GridKanaBottomKeyboardSet(
    private val listener: Keyboard.Listener,
    private val leftLayout: String,
    private val rightLayout: String
): KeyboardSet {
    private lateinit var keyboardSwitcher: ShiftKeyboardSwitcher

    override fun initView(context: Context): View {
        run {
            val normal = GridKanaBottomRowKeyboard(listener, leftLayout, rightLayout).createView(context)
            keyboardSwitcher = ShiftKeyboardSwitcher(context, normal, normal, normal)
        }
        return keyboardSwitcher.view
    }

    override fun getView(shiftState: Keyboard.ShiftState, candidates: Boolean): View {
        keyboardSwitcher.switch(shiftState)
        return keyboardSwitcher.view
    }
}