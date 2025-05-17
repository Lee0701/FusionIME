package ee.oyatl.ime.keyboard

import android.content.Context
import android.os.Handler
import android.os.Looper
import ee.oyatl.ime.keyboard.R
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding

class GridKanaBottomRowKeyboard(
    override val listener: Keyboard.Listener,
    private val leftExtraRow: String,
    private val rightExtraRow: String
): DefaultKeyboard(listener) {
    override fun buildRows(context: Context): List<KbdRowBinding> {
        val height = context.resources.getDimensionPixelSize(R.dimen.key_height)
        val row = buildRow(context, "", height)

        leftExtraRow.forEach { c ->
            val key = buildKey(context, c, height)
            row.root.addView(key.root)
        }

        row.root.addView(buildSpecialKey(
            context,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_numbers,
            1.5f) { pressed -> if(pressed) listener.onSpecial(Keyboard.SpecialKey.Symbols) }
        )

        row.root.addView(buildSpecialKey(
            context,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_language,
            1.0f) { pressed -> if(pressed) listener.onSpecial(Keyboard.SpecialKey.Language) })

        row.root.addView(buildSpecialKey(
            context,
            R.style.Theme_FusionIME_Keyboard_Key,
            R.drawable.keyic_space,
            2.0f
        ) { pressed -> if(pressed) listener.onSpecial(Keyboard.SpecialKey.Space) })

        rightExtraRow.forEach { c ->
            val key = buildKey(context, c, height)
            row.root.addView(key.root)
        }

        row.root.addView(buildSpecialKey(
            context,
            R.style.Theme_FusionIME_Keyboard_Key_Return,
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
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
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