package ee.oyatl.ime.keyboard

import android.content.Context
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding

class DefaultSymbolsBottomRowKeyboard(
    private val extraKeys: List<Int> = listOf(','.code, '.'.code)
): DefaultKeyboard() {
    override fun buildRows(context: Context, listener: Keyboard.Listener): List<KbdRowBinding> {
        val height = context.resources.getDimensionPixelSize(R.dimen.key_height)
        val row = buildRow(context, listener, listOf(), height)
        row.root.addView(buildSpecialKey(
            context,
            listener,
            Keyboard.SpecialKey.Symbols,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_option,
            1.5f
        ))
        val left = extraKeys[0]
        row.root.addView(buildKey(context, listener, left, left.toChar().toString(), height).root)
        row.root.addView(buildSpecialKey(
            context,
            listener,
            Keyboard.SpecialKey.Numbers,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_numbers,
            1.0f
        ))
        row.root.addView(buildSpecialKey(
            context,
            listener,
            Keyboard.SpecialKey.Space,
            R.style.Theme_FusionIME_Keyboard_Key,
            R.drawable.keyic_space,
            4.0f
        ))
        val right = extraKeys[1]
        row.root.addView(buildKey(context, listener, right, right.toChar().toString(), height).root)
        row.root.addView(buildSpecialKey(
            context,
            listener,
            Keyboard.SpecialKey.Return,
            R.style.Theme_FusionIME_Keyboard_Key_Return,
            R.drawable.keyic_return,
            1.5f
        ))
        return listOf(row)
    }
}