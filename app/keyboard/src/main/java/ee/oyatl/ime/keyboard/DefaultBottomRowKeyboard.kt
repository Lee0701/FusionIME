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
            R.color.key_bg_static_light_mod,
            R.drawable.keyic_numbers,
            1.5f) { pressed -> if(pressed) listener.onSpecial(Keyboard.SpecialKey.Symbols) }
        )
        row.root.addView(buildKey(context, ',', height).root)
        row.root.addView(buildSpecialKey(
            context,
            R.color.key_bg_static_light_mod,
            R.drawable.keyic_language,
            1.0f) { pressed -> if(pressed) listener.onSpecial(Keyboard.SpecialKey.Language) })
        row.root.addView(buildSpecialKey(
            context,
            R.color.key_bg_static_light,
            R.drawable.keyic_space,
            4.0f
        ) { pressed -> if(pressed) listener.onSpecial(Keyboard.SpecialKey.Space) })
        row.root.addView(buildKey(context, '.', height).root)
        row.root.addView(buildSpecialKey(
            context,
            R.color.key_bg_static_light_return,
            R.drawable.keyic_return,
            1.5f
        ) { pressed -> if(pressed) listener.onSpecial(Keyboard.SpecialKey.Return) })
        return listOf(row)
    }
}