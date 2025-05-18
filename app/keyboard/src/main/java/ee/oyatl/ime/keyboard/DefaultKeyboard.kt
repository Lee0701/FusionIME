package ee.oyatl.ime.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import ee.oyatl.ime.keyboard.databinding.KbdKeyBinding
import ee.oyatl.ime.keyboard.databinding.KbdKeyboardBinding
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding

abstract class DefaultKeyboard: Keyboard {

    protected val shiftKeys: MutableList<KbdKeyBinding> = mutableListOf()

    abstract fun buildRows(context: Context, listener: Keyboard.Listener): List<KbdRowBinding>

    override fun createView(context: Context, listener: Keyboard.Listener): View {
        val inflater = LayoutInflater.from(ContextThemeWrapper(context, R.style.Theme_FusionIME_Keyboard))
        val keyboard = KbdKeyboardBinding.inflate(inflater)
        buildRows(context, listener).forEach { keyboard.root.addView(it.root) }
        return keyboard.root
    }

    override fun changeState(shiftState: Keyboard.ShiftState) {
        val icon = when(shiftState) {
            Keyboard.ShiftState.Unpressed -> R.drawable.keyic_shift
            Keyboard.ShiftState.Pressed -> R.drawable.keyic_shift_pressed
            Keyboard.ShiftState.Locked -> R.drawable.keyic_shift_locked
        }
        shiftKeys.forEach { it.icon.setImageResource(icon) }
    }

    protected open fun buildRow(context: Context, listener: Keyboard.Listener, chars: String, height: Int): KbdRowBinding {
        val inflater = LayoutInflater.from(context)
        val row = KbdRowBinding.inflate(inflater)
        chars.forEach { char ->
            val key = buildKey(context, listener, char, height)
            row.root.addView(key.root)
        }
        return row
    }

    protected open fun buildKey(context: Context, listener: Keyboard.Listener, char: Char, height: Int): KbdKeyBinding {
        val inflater = LayoutInflater.from(ContextThemeWrapper(context, R.style.Theme_FusionIME_Keyboard_Key))
        val key = KbdKeyBinding.inflate(inflater)
        key.label.text = char.toString()
        key.root.setOnClickListener { listener.onChar(char.code) }
        key.root.layoutParams = LinearLayout.LayoutParams(0, height).apply {
            weight = 1.0f
        }
        return key
    }

    protected open fun buildSpacer(context: Context, listener: Keyboard.Listener, width: Float): View {
        val spacer = View(context)
        spacer.layoutParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT
        ).apply {
            weight = width
        }
        spacer.isClickable = true
        return spacer
    }

    @SuppressLint("ClickableViewAccessibility")
    protected open fun buildSpecialKey(
        context: Context,
        listener: Keyboard.Listener,
        type: Keyboard.SpecialKey,
        @StyleRes theme: Int,
        @DrawableRes icon: Int,
        width: Float
    ): View {
        val inflater = LayoutInflater.from(ContextThemeWrapper(context, theme))
        val height = context.resources.getDimensionPixelSize(R.dimen.key_height)
        val key = KbdKeyBinding.inflate(inflater)
        key.icon.setImageResource(icon)
        key.root.setOnTouchListener { view, event ->
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    listener.onSpecial(type, true)
                    view.isPressed = true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    listener.onSpecial(type, false)
                    view.isPressed = false
                }
            }
            view.invalidate()
            true
        }
        key.root.layoutParams = LinearLayout.LayoutParams(0, height).apply {
            weight = width
        }
        return key.root
    }
}