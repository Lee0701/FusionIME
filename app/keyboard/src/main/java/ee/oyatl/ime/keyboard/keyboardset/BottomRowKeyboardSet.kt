package ee.oyatl.ime.keyboard.keyboardset

import android.content.Context
import android.view.View
import ee.oyatl.ime.keyboard.DefaultBottomRowKeyboard
import ee.oyatl.ime.keyboard.Keyboard

class BottomRowKeyboardSet(
    private val listener: Keyboard.Listener
): KeyboardSet {
    private lateinit var view: View

    override fun initView(context: Context): View {
        view = DefaultBottomRowKeyboard(listener).createView(context)
        return view
    }

    override fun getView(shiftState: Keyboard.ShiftState, candidates: Boolean): View {
        return view
    }
}