package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.LayoutInflater
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding

class DefaultGridKeyboard(
    private val rows: List<List<Int>>
): DefaultKeyboard() {
    override val numRows: Int = rows.size

    override fun buildRows(context: Context, listener: Keyboard.Listener, height: Int): List<KbdRowBinding> {
        val builtRows = rows.map { buildRow(context, listener, it, height) }
        return builtRows
    }

    override fun buildRow(context: Context, listener: Keyboard.Listener, codes: List<Int>, height: Int): KbdRowBinding {
        val inflater = LayoutInflater.from(context)
        val row = KbdRowBinding.inflate(inflater)
        codes.forEach { code ->
            if(code == '　'.code) {
                val spacer = buildSpacer(context, listener, 1.0f)
                row.root.addView(spacer)
            } else if(code == '\b'.code) {
                row.root.addView(buildSpecialKey(
                    context,
                    RepeatableKeyListener(listener),
                    Keyboard.SpecialKey.Delete,
                    R.style.Theme_FusionIME_Keyboard_Key_Modifier,
                    R.drawable.keyic_backspace,
                    1.0f
                ))
            } else {
                val key = buildKey(context, listener, code, code.toChar().toString(), height)
                row.root.addView(key.root)
            }
        }
        return row
    }
}