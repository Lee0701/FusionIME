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
import ee.oyatl.ime.keyboard.listener.KeyboardListener

abstract class DefaultKeyboard: Keyboard {

    protected val shiftKeys: MutableList<KbdKeyBinding> = mutableListOf()

    abstract fun buildRows(context: Context, listener: KeyboardListener, height: Int): List<KbdRowBinding>

    override fun createView(context: Context, listener: KeyboardListener, height: Int): View {
        val inflater = LayoutInflater.from(ContextThemeWrapper(context, R.style.Theme_FusionIME_Keyboard))
        val keyboard = KbdKeyboardBinding.inflate(inflater)
        buildRows(context, listener, height).forEach { keyboard.root.addView(it.root) }
        return keyboard.root
    }

    override fun setShiftState(state: KeyboardState.Shift) {
        val icon = when(state) {
            KeyboardState.Shift.Released -> R.drawable.keyic_shift
            KeyboardState.Shift.Pressed -> R.drawable.keyic_shift_pressed
            KeyboardState.Shift.Locked -> R.drawable.keyic_shift_locked
        }
        shiftKeys.forEach { it.icon.setImageResource(icon) }
    }

    protected open fun buildRow(context: Context, listener: KeyboardListener, codes: List<Int>, height: Int): KbdRowBinding {
        val inflater = LayoutInflater.from(context)
        val row = KbdRowBinding.inflate(inflater)
        codes.forEach { code ->
            val key = buildKey(context, listener, code, code.toChar().toString(), height)
            row.root.addView(key.root)
        }
        return row
    }

    @SuppressLint("ClickableViewAccessibility")
    protected open fun buildKey(context: Context, listener: KeyboardListener, code: Int, label: String, height: Int): KbdKeyBinding {
        val inflater = LayoutInflater.from(ContextThemeWrapper(context, R.style.Theme_FusionIME_Keyboard_Key))
        val key = KbdKeyBinding.inflate(inflater)
        key.label.text = label
        key.root.setOnTouchListener { view, event ->
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    listener.onKeyDown(code)
                    view.isPressed = true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    listener.onKeyUp(code)
                    view.isPressed = false
                }
            }
            true
        }
        key.root.layoutParams = LayoutParams(0, height).apply {
            weight = 1.0f
        }
        return key
    }

    protected open fun buildSpacer(context: Context, listener: KeyboardListener, width: Float): View {
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
        listener: KeyboardListener,
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
                    listener.onKeyDown(type.code)
                    view.isPressed = true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    listener.onKeyUp(type.code)
                    view.isPressed = false
                }
            }
            true
        }
        key.root.layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT).apply {
            weight = width
        }
        return key.root
    }
}