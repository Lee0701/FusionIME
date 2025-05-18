package ee.oyatl.ime.keyboard

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding

class DefaultGridKeyboard(
    private val rows: List<String>
): DefaultKeyboard() {
    override fun buildRows(context: Context, listener: Keyboard.Listener): List<KbdRowBinding> {
        val height = context.resources.getDimensionPixelSize(R.dimen.key_height) *
                context.resources.getInteger(R.integer.standard_row_count) / rows.size
        val builtRows = rows.map { buildRow(context, listener, it, height) }
        return builtRows
    }

    override fun buildRow(context: Context, listener: Keyboard.Listener, chars: String, height: Int): KbdRowBinding {
        val inflater = LayoutInflater.from(context)
        val row = KbdRowBinding.inflate(inflater)
        chars.forEach { char ->
            if(char == '　') {
                val spacer = buildSpacer(context, listener, 1.0f)
                row.root.addView(spacer)
            } else {
                val key = buildKey(context, listener, char, height)
                row.root.addView(key.root)
            }
        }
        return row
    }
}