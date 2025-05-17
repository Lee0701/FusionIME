package ee.oyatl.ime.keyboard

import android.content.Context
import android.os.Handler
import android.os.Looper
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
}