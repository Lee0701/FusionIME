package ee.oyatl.ime.keyboard

import android.content.Context
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding
import ee.oyatl.ime.keyboard.listener.KeyboardListener

class DefaultTabletBottomRowKeyboard(
    private val extraKeys: List<Int> = listOf('!'.code, '?'.code),
    private val isSymbols: Boolean = false
): DefaultKeyboard() {
    override val numRows: Int = 1

    override fun buildRows(context: Context, listener: KeyboardListener): List<KbdRowBinding> {
        val row = buildRow(context, listener, listOf())
        row.root.addView(buildSpecialKey(
            context,
            listener,
            Keyboard.SpecialKey.Symbols,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_option,
            1.5f
        ))
        val left = extraKeys[0]
        row.root.addView(buildKey(context, listener, left, left.toChar().toString()).root)
        if(isSymbols) {
            row.root.addView(buildSpecialKey(
                context,
                listener,
                Keyboard.SpecialKey.Numbers,
                R.style.Theme_FusionIME_Keyboard_Key_Modifier,
                R.drawable.keyic_numbers,
                1.0f
            ))
        } else {
            row.root.addView(buildSpecialKey(
                context,
                listener,
                Keyboard.SpecialKey.Language,
                R.style.Theme_FusionIME_Keyboard_Key_Modifier,
                R.drawable.keyic_language,
                1.0f
            ))
        }
        row.root.addView(buildSpecialKey(
            context,
            listener,
            Keyboard.SpecialKey.Space,
            R.style.Theme_FusionIME_Keyboard_Key,
            R.drawable.keyic_space,
            5.0f
        ))
        val right = extraKeys[1]
        row.root.addView(buildKey(context, listener, right, right.toChar().toString()).root)
        row.root.addView(buildSpecialKey(
            context,
            listener,
            Keyboard.SpecialKey.Symbols,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_option,
            1.5f
        ))
        return listOf(row)
    }
}