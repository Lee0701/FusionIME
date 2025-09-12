package ee.oyatl.ime.keyboard.switcher

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import ee.oyatl.ime.keyboard.KeyboardState

class ShiftKeyboardSwitcher(
    context: Context,
    private val normalView: View,
    private val shiftedView: View,
    private val lockedView: View
): KeyboardSwitcher<KeyboardState.Shift> {
    private val switcherView: FrameLayout = FrameLayout(context)

    init {
        if(normalView.parent == null) switcherView.addView(normalView)
        if(shiftedView.parent == null) switcherView.addView(shiftedView)
        if(lockedView.parent == null) switcherView.addView(lockedView)
    }

    override val view: View
        get() = switcherView

    override fun switch(state: KeyboardState.Shift) {
        when(state) {
            KeyboardState.Shift.Released -> {
                normalView.bringToFront()
            }
            KeyboardState.Shift.Pressed -> {
                shiftedView.bringToFront()
            }
            KeyboardState.Shift.Locked -> {
                lockedView.bringToFront()
            }
        }
    }
}