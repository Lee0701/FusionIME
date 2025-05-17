package ee.oyatl.ime.keyboard

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding

class DefaultGridKeyboard(
    override val listener: Keyboard.Listener,
    private val rows: List<String>,
    private val shiftState: Keyboard.ShiftState
): DefaultKeyboard(listener) {
    override fun buildRows(context: Context): List<KbdRowBinding> {
        val height = context.resources.getDimensionPixelSize(R.dimen.key_height) *
                context.resources.getInteger(R.integer.standard_row_count) / rows.size
        val builtRows = rows.map { buildRow(context, it, height) }
        return builtRows
    }

    override fun buildRow(context: Context, chars: String, height: Int): KbdRowBinding {
        val inflater = LayoutInflater.from(context)
        val row = KbdRowBinding.inflate(inflater)
        chars.forEach { char ->
            if(char == '　') {
                val spacer = buildSpacer(context, 1.0f)
                row.root.addView(spacer)
            } else {
                val key = buildKey(context, char, height)
                row.root.addView(key.root)
            }
        }
        return row
    }
}