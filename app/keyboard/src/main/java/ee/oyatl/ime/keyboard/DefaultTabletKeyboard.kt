package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.View
import ee.oyatl.ime.keyboard.databinding.KbdKeyBinding
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.listener.RepeatableKeyListener

class DefaultTabletKeyboard(
    private val rows: List<List<Int>>,
    private val extraKeys: List<Int> = listOf(','.code, '.'.code)
): DefaultKeyboard() {
    override val numRows: Int = rows.size
    private val shiftSize: Float = (11 - (rows.last().size + extraKeys.size)) / 2f

    override fun buildRows(context: Context, listener: KeyboardListener): List<KbdRowBinding> {
        val builtRows = rows.map { buildRow(context, listener, it) }

        builtRows.first().root.addView(buildSpecialKey(
            context,
            RepeatableKeyListener(listener),
            Keyboard.SpecialKey.Delete,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_backspace,
            1f
        ))

        val enterSize: Float
        if(rows[rows.size - 2].size == 9) {
            builtRows[rows.size - 2].root.addView(buildSpacer(context, listener, 0.5f), 0)
            enterSize = 1.5f
        } else {
            enterSize = 11f - rows[rows.size - 2].size
        }
        if(rows.size == 4 && rows[1].size < 11) {
            builtRows[1].root.addView(buildSpacer(context, listener, 11f - rows[1].size))
        }

        builtRows[rows.size - 2].root.addView(buildSpecialKey(
            context,
            listener,
            Keyboard.SpecialKey.Return,
            R.style.Theme_FusionIME_Keyboard_Key_Return,
            R.drawable.keyic_return,
            enterSize
        ))

        builtRows.last().root.addView(buildShiftKey(context, listener), 0)

        extraKeys.forEach { code ->
            builtRows.last().root.addView(buildKey(context, listener, code, code.toChar().toString()).root)
        }

        builtRows.last().root.addView(buildShiftKey(context, listener))

        return builtRows
    }

    private fun buildShiftKey(context: Context, listener: KeyboardListener): View {
        val key = buildSpecialKey(
            context,
            listener,
            Keyboard.SpecialKey.Shift,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_shift,
            shiftSize
        )
        shiftKeys += KbdKeyBinding.bind(key)
        return key
    }
}