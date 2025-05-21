package ee.oyatl.ime.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
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

    override fun changeState(state: KeyboardStateSet) {
        val icon = when(state.shift) {
            KeyboardState.Shift.Released -> R.drawable.keyic_shift
            KeyboardState.Shift.Pressed -> R.drawable.keyic_shift_pressed
            KeyboardState.Shift.Locked -> R.drawable.keyic_shift_locked
        }
        shiftKeys.forEach { it.icon.setImageResource(icon) }
    }

    protected open fun buildRow(context: Context, listener: Keyboard.Listener, codes: List<Int>, height: Int): KbdRowBinding {
        val inflater = LayoutInflater.from(context)
        val row = KbdRowBinding.inflate(inflater)
        codes.forEach { code ->
            val key = buildKey(context, listener, code, code.toChar().toString(), height)
            row.root.addView(key.root)
        }
        return row
    }

    protected open fun buildKey(context: Context, listener: Keyboard.Listener, code: Int, label: String, height: Int): KbdKeyBinding {
        val inflater = LayoutInflater.from(ContextThemeWrapper(context, R.style.Theme_FusionIME_Keyboard_Key))
        val key = KbdKeyBinding.inflate(inflater)
        key.label.text = label
        key.root.setOnClickListener { listener.onChar(code) }
        key.root.layoutParams = LayoutParams(0, height).apply {
            weight = 1.0f
        }
        return key
    }

    protected open fun buildSpacer(context: Context, listener: Keyboard.Listener, width: Float): View {
        val spacer = View(context)
        spacer.layoutParams = LayoutParams(
            0, LayoutParams.MATCH_PARENT
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
        key.root.layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT).apply {
            weight = width
        }
        return key.root
    }
}