package ee.oyatl.ime.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.annotation.DrawableRes
import ee.oyatl.ime.keyboard.databinding.KbdKeyBinding
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.listener.RepeatableKeyListener
import androidx.core.graphics.drawable.toDrawable

class DefaultTabletKeyboard(
    private val rows: List<List<Int>>,
    private val extraKeys: List<Int> = listOf(','.code, '.'.code)
): DefaultKeyboard() {
    override val numRows: Int = rows.size
    private val enterKeys: MutableList<KbdKeyBinding> = mutableListOf()

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
        @DrawableRes val enterBkg: Int
        if(rows[rows.size - 2].size == 9) {
            builtRows[rows.size - 2].root.addView(buildSpacer(context, listener, 0.5f), 0)
            enterSize = 1.5f
        } else if(rows[rows.size - 2].size == 10) {
            builtRows[rows.size - 2].root.addView(buildSpacer(context, listener, 0.5f), 0)
            enterSize = 0.5f
        } else {
            enterSize = 11f - rows[rows.size - 2].size
        }

        if(rows.size == 4 && rows[1].size < 11) {
            builtRows[1].root.addView(buildEnterKey(context, listener, 11f - rows[1].size, R.drawable.key_bg_top_right))
            enterBkg = R.drawable.key_bg_bottom
        } else {
            enterBkg = R.drawable.key_bg
        }

        builtRows[rows.size - 2].root.addView(buildEnterKey(context, listener, enterSize, enterBkg))

        builtRows.last().root.addView(buildShiftKey(context, listener), 0)

        extraKeys.forEach { code ->
            builtRows.last().root.addView(buildKey(context, listener, code, code.toChar().toString()).root)
        }

        builtRows.last().root.addView(buildShiftKey(context, listener))

        return builtRows
    }

    private fun buildShiftKey(context: Context, listener: KeyboardListener): View {
        val shiftSize: Float = (11 - (rows.last().size + extraKeys.size)) / 2f
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

    private fun buildEnterKey(context: Context, listener: KeyboardListener, width: Float, @DrawableRes bkg: Int): View {
        val enterListener = object: KeyboardListener {
            override fun onKeyDown(code: Int) {
                enterKeys.forEach { it.root.isPressed = true }
                listener.onKeyDown(code)
            }
            override fun onKeyUp(code: Int) {
                enterKeys.forEach { it.root.isPressed = false }
                listener.onKeyUp(code)
            }
        }
        val view = buildSpecialKey(
            context,
            enterListener,
            Keyboard.SpecialKey.Return,
            R.style.Theme_FusionIME_Keyboard_Key_Return,
            R.drawable.keyic_return,
            width
        )
        val binding = KbdKeyBinding.bind(view)
        binding.bkg.setImageResource(bkg)
        if(bkg == R.drawable.key_bg_bottom) binding.icon.setImageDrawable(
            Color.argb(0, 0, 0, 0).toDrawable())
        enterKeys += binding
        return binding.root
    }
}