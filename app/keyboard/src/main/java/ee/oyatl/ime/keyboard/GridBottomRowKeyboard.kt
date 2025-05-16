package ee.oyatl.ime.keyboard

import android.content.Context
import android.os.Handler
import android.os.Looper
import ee.oyatl.ime.keyboard.R
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding

class GridBottomRowKeyboard(
    override val listener: Keyboard.Listener,
    private val extraRow: String,
    private val shiftState: Keyboard.ShiftState

): DefaultKeyboard(listener) {
    override fun buildRows(context: Context): List<KbdRowBinding> {
        val height = context.resources.getDimensionPixelSize(R.dimen.key_height)
        val row = buildRow(context, "", height)
        row.root.addView(buildSpecialKey(
            context,
            R.color.key_bg_static_light_mod,
            R.drawable.keyic_numbers,
            1.25f) { pressed -> if(pressed) listener.onSpecial(Keyboard.SpecialKey.Symbols) }
        )

        val icon = when(shiftState) {
            Keyboard.ShiftState.Unpressed -> R.drawable.keyic_shift
            Keyboard.ShiftState.Pressed -> R.drawable.keyic_shift_pressed
            Keyboard.ShiftState.Locked -> R.drawable.keyic_shift_locked
        }

        row.root.addView(buildSpecialKey(
            context,
            R.color.key_bg_static_light_mod,
            icon,
            1.25f
        ) { pressed -> listener.onShift(pressed) })

        row.root.addView(buildSpecialKey(
            context,
            R.color.key_bg_static_light_mod,
            R.drawable.keyic_language,
            1.0f) { pressed -> if(pressed) listener.onSpecial(Keyboard.SpecialKey.Language) })

        row.root.addView(buildSpecialKey(
            context,
            R.color.key_bg_static_light,
            R.drawable.keyic_space,
            3.0f
        ) { pressed -> if(pressed) listener.onSpecial(Keyboard.SpecialKey.Space) })

        extraRow.forEach { c ->
            val key = buildKey(context, c, height)
            row.root.addView(key.root)
        }

        row.root.addView(buildSpecialKey(
            context,
            R.color.key_bg_static_light_return,
            R.drawable.keyic_return,
            1.25f
        ) { pressed -> if(pressed) listener.onSpecial(Keyboard.SpecialKey.Return) })

        val handler = Handler(Looper.getMainLooper())
        fun repeat() {
            listener.onSpecial(Keyboard.SpecialKey.Delete)
            handler.postDelayed({ repeat() }, 50)
        }
        row.root.addView(buildSpecialKey(
            context,
            R.color.key_bg_static_light_mod,
            R.drawable.keyic_backspace,
            1.25f
        ) { pressed ->
            if(pressed) {
                listener.onSpecial(Keyboard.SpecialKey.Delete)
                handler.postDelayed({ repeat() }, 500)
            } else {
                handler.removeCallbacksAndMessages(null)
            }
            Unit
        })

        return listOf(row)
    }
}