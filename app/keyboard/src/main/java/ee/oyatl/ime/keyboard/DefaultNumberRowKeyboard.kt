package ee.oyatl.ime.keyboard

import android.content.Context
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding

class DefaultNumberRowKeyboard(
    listener: Keyboard.Listener,
    private val row: String
): DefaultKeyboard(listener) {
    override fun buildRows(context: Context): List<KbdRowBinding> {
        val height = context.resources.getDimensionPixelSize(R.dimen.key_height)
        val row = buildRow(context, row, height)
        return listOf(row)
    }
}