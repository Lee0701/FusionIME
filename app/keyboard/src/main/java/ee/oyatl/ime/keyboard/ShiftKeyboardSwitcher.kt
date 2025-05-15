package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.View
import android.widget.FrameLayout

class ShiftKeyboardSwitcher(
    context: Context,
    private val normalView: View,
    private val shiftedView: View,
    private val lockedView: View
): KeyboardSwitcher<Keyboard.ShiftState> {
    private val switcherView: FrameLayout = FrameLayout(context)

    init {
        if(normalView.parent == null) switcherView.addView(normalView)
        if(shiftedView.parent == null) switcherView.addView(shiftedView)
        if(lockedView.parent == null) switcherView.addView(lockedView)
    }

    override val view: View
        get() = switcherView

    override fun switch(state: Keyboard.ShiftState) {
        when(state) {
            Keyboard.ShiftState.Unpressed -> {
                normalView.bringToFront()
            }
            Keyboard.ShiftState.Pressed -> {
                shiftedView.bringToFront()
            }
            Keyboard.ShiftState.Locked -> {
                lockedView.bringToFront()
            }
        }
    }
}