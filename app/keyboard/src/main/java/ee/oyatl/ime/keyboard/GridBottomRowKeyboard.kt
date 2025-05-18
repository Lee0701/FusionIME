package ee.oyatl.ime.keyboard

import android.content.Context
import android.os.Handler
import android.os.Looper
import ee.oyatl.ime.keyboard.databinding.KbdKeyBinding
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding

class GridBottomRowKeyboard(
    private val extraRow: List<Int>
): DefaultKeyboard() {
    override fun buildRows(context: Context, listener: Keyboard.Listener): List<KbdRowBinding> {
        val height = context.resources.getDimensionPixelSize(R.dimen.key_height)
        val row = buildRow(context, listener, listOf(), height)
        row.root.addView(buildSpecialKey(
            context,
            listener,
            Keyboard.SpecialKey.Symbols,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_numbers,
            1.25f
        ))

        val shiftKey = KbdKeyBinding.bind(buildSpecialKey(
            context,
            listener,
            Keyboard.SpecialKey.Shift,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_shift,
            1.25f
        ))
        shiftKeys += shiftKey
        row.root.addView(shiftKey.root)

        row.root.addView(buildSpecialKey(
            context,
            listener,
            Keyboard.SpecialKey.Language,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_language,
            1.0f))

        row.root.addView(buildSpecialKey(
            context,
            listener,
            Keyboard.SpecialKey.Space,
            R.style.Theme_FusionIME_Keyboard_Key,
            R.drawable.keyic_space,
            3.0f
        ))

        extraRow.forEach { c ->
            val key = buildKey(context, listener, c, c.toChar().toString(), height)
            row.root.addView(key.root)
        }

        row.root.addView(buildSpecialKey(
            context,
            listener,
            Keyboard.SpecialKey.Return,
            R.style.Theme_FusionIME_Keyboard_Key_Return,
            R.drawable.keyic_return,
            1.25f
        ))

        val handler = Handler(Looper.getMainLooper())
        fun repeat() {
            listener.onSpecial(Keyboard.SpecialKey.Delete, true)
            listener.onSpecial(Keyboard.SpecialKey.Delete, false)
            handler.postDelayed({ repeat() }, 50)
        }
        row.root.addView(buildSpecialKey(
            context,
            RepeatableKeyListener(listener),
            Keyboard.SpecialKey.Delete,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_backspace,
            1.25f
        ))

        return listOf(row)
    }
}