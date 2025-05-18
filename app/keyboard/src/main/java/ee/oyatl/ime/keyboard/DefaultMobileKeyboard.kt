package ee.oyatl.ime.keyboard

import android.content.Context
import ee.oyatl.ime.keyboard.databinding.KbdKeyBinding
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding

class DefaultMobileKeyboard(
    private val rows: List<String>
): DefaultKeyboard() {
    override fun buildRows(context: Context, listener: Keyboard.Listener): List<KbdRowBinding> {
        val height = context.resources.getDimensionPixelSize(R.dimen.key_height) *
                context.resources.getInteger(R.integer.standard_row_count) / rows.size
        val row1 = buildRow(context, listener, rows[0], height)
        val row2 = buildRow(context, listener, rows[1], height)
        val row3 = buildRow(context, listener, rows[2], height)

        if(rows[1].length != 10) {
            val space = (10 - rows[1].length) / 2f
            row2.root.addView(buildSpacer(context, listener, space), 0)
            row2.root.addView(buildSpacer(context, listener, space))
        }

        val shiftKey = KbdKeyBinding.bind(buildSpecialKey(
            context,
            listener,
            Keyboard.SpecialKey.Shift,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_shift,
            1.5f
        ))
        shiftKeys += shiftKey
        row3.root.addView(shiftKey.root, 0)

        row3.root.addView(buildSpecialKey(
            context,
            RepeatableKeyListener(listener),
            Keyboard.SpecialKey.Delete,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_backspace,
            1.5f
        ))

        return listOf(row1, row2, row3)
    }
}