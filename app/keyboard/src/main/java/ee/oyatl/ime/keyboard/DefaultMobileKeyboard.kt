package ee.oyatl.ime.keyboard

import android.content.Context
import ee.oyatl.ime.keyboard.databinding.KbdKeyBinding
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding

class DefaultMobileKeyboard(
    private val rows: List<List<Int>>
): DefaultKeyboard() {
    override fun buildRows(context: Context, listener: Keyboard.Listener): List<KbdRowBinding> {
        val height = context.resources.getDimensionPixelSize(R.dimen.key_height) *
                context.resources.getInteger(R.integer.standard_row_count) / rows.size
        val builtRows = rows.map { buildRow(context, listener, it, height) }

        if(rows[rows.size - 2].size != 10) {
            val space = (10 - rows[1].size) / 2f
            builtRows[rows.size - 2].root.addView(buildSpacer(context, listener, space), 0)
            builtRows[rows.size - 2].root.addView(buildSpacer(context, listener, space))
        }

        val shiftDelWidth = if(rows.last().size == 7) 1.5f else 1.0f

        val shiftKey = KbdKeyBinding.bind(buildSpecialKey(
            context,
            listener,
            Keyboard.SpecialKey.Shift,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_shift,
            shiftDelWidth
        ))
        shiftKeys += shiftKey
        builtRows.last().root.addView(shiftKey.root, 0)

        builtRows.last().root.addView(buildSpecialKey(
            context,
            RepeatableKeyListener(listener),
            Keyboard.SpecialKey.Delete,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_backspace,
            shiftDelWidth
        ))

        return builtRows
    }
}