package ee.oyatl.ime.keyboard

import android.content.Context
import ee.oyatl.ime.keyboard.R
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding

class DefaultBottomRowKeyboard(
    override val listener: Keyboard.Listener
): DefaultKeyboard(listener) {
    override fun buildRows(context: Context): List<KbdRowBinding> {
        val height = context.resources.getDimensionPixelSize(R.dimen.key_height)
        val row = buildRow(context, "", height)
        row.root.addView(buildSpecialKey(
            context,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_numbers,
            1.5f) { pressed -> listener.onSpecial(Keyboard.SpecialKey.Symbols, pressed) }
        )
        row.root.addView(buildKey(context, ',', height).root)
        row.root.addView(buildSpecialKey(
            context,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_language,
            1.0f) { pressed -> listener.onSpecial(Keyboard.SpecialKey.Language, pressed) })
        row.root.addView(buildSpecialKey(
            context,
            R.style.Theme_FusionIME_Keyboard_Key,
            R.drawable.keyic_space,
            4.0f
        ) { pressed -> listener.onSpecial(Keyboard.SpecialKey.Space, pressed) })
        row.root.addView(buildKey(context, '.', height).root)
        row.root.addView(buildSpecialKey(
            context,
            R.style.Theme_FusionIME_Keyboard_Key_Return,
            R.drawable.keyic_return,
            1.5f
        ) { pressed -> listener.onSpecial(Keyboard.SpecialKey.Return, pressed) })
        return listOf(row)
    }
}