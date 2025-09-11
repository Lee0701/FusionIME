package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.KeyEvent
import android.widget.LinearLayout
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.listener.RepeatableKeyListener

class DefaultNumberKeyboard(
    private val rows: List<List<Int>> = ROWS
): DefaultKeyboard() {
    override val numRows: Int = rows.size

    override fun buildRows(context: Context, listener: KeyboardListener): List<KbdRowBinding> {
        val delete = buildSpecialKey(
            context,
            RepeatableKeyListener(listener),
            Keyboard.SpecialKey.Delete,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_backspace,
            1.0f
        )

        val space = buildSpecialKey(
            context,
            listener,
            Keyboard.SpecialKey.Space,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_space,
            1.0f
        )

        val enter = buildSpecialKey(
            context,
            listener,
            Keyboard.SpecialKey.Return,
            R.style.Theme_FusionIME_Keyboard_Key_Return,
            R.drawable.keyic_return,
            1.0f
        )

        val symbols = buildSpecialKey(
            context,
            listener,
            Keyboard.SpecialKey.Symbols,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            R.drawable.keyic_option,
            1.0f
        )

        val comma = buildKey(
            context,
            listener,
            KeyEvent.KEYCODE_COMMA,
            ","
        ).apply {
            (root.layoutParams as LinearLayout.LayoutParams).weight = 0.5f
        }

        val period = buildKey(
            context,
            listener,
            KeyEvent.KEYCODE_PERIOD,
            "."
        ).apply {
            (root.layoutParams as LinearLayout.LayoutParams).weight = 0.5f
        }

        val builtRows = rows.map { buildRow(context, listener, it) }
        builtRows[0].root.addView(buildSpacer(context, listener, 1.0f))
        builtRows[1].root.addView(delete)
        builtRows[2].root.addView(space)
        builtRows[3].root.addView(symbols, 0)
        builtRows[3].root.addView(comma.root)
        builtRows[3].root.addView(period.root)
        builtRows[3].root.addView(enter)

        return builtRows
    }

    companion object {
        private val ROWS = listOf(
            listOf('1'.code, '2'.code, '3'.code),
            listOf('4'.code, '5'.code, '6'.code),
            listOf('7'.code, '8'.code, '9'.code),
            listOf('0'.code),
        )
    }
}