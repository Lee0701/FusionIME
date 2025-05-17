package ee.oyatl.ime.keyboard

import android.content.Context
import android.os.Handler
import android.os.Looper
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding

class DefaultMobileKeyboard(
    override val listener: Keyboard.Listener,
    private val rows: List<String>,
    private val shiftState: Keyboard.ShiftState
): DefaultKeyboard(listener) {
    override fun buildRows(context: Context): List<KbdRowBinding> {
        val height = context.resources.getDimensionPixelSize(R.dimen.key_height) *
                context.resources.getInteger(R.integer.standard_row_count) / rows.size
        val row1 = buildRow(context, rows[0], height)
        val row2 = buildRow(context, rows[1], height)
        val row3 = buildRow(context, rows[2], height)

        if(rows[1].length != 10) {
            val space = (10 - rows[1].length) / 2f
            row2.root.addView(buildSpacer(context, space), 0)
            row2.root.addView(buildSpacer(context, space))
        }

        val icon = when(shiftState) {
            Keyboard.ShiftState.Unpressed -> R.drawable.keyic_shift
            Keyboard.ShiftState.Pressed -> R.drawable.keyic_shift_pressed
            Keyboard.ShiftState.Locked -> R.drawable.keyic_shift_locked
        }

        row3.root.addView(buildSpecialKey(
            context,
            R.color.key_bg_static_light_mod,
            icon,
            1.5f
        ) { pressed -> listener.onShift(pressed) }, 0)

        val handler = Handler(Looper.getMainLooper())
        fun repeat() {
            listener.onSpecial(Keyboard.SpecialKey.Delete)
            handler.postDelayed({ repeat() }, 50)
        }
        row3.root.addView(buildSpecialKey(
            context,
            R.color.key_bg_static_light_mod,
            R.drawable.keyic_backspace,
            1.5f
        ) { pressed ->
            if(pressed) {
                listener.onSpecial(Keyboard.SpecialKey.Delete)
                handler.postDelayed({ repeat() }, 500)
            } else {
                handler.removeCallbacksAndMessages(null)
            }
            Unit
        })

        return listOf(row1, row2, row3)
    }
}