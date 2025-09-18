package ee.oyatl.ime.keyboard.rewrite

import android.annotation.SuppressLint
import android.content.Context
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import ee.oyatl.ime.keyboard.R
import ee.oyatl.ime.keyboard.databinding.KbdKeyBinding
import ee.oyatl.ime.keyboard.databinding.KbdKeyboardBinding
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding
import ee.oyatl.ime.keyboard.listener.KeyboardListener

class DefaultKeyboard(
    val rows: List<List<Keyboard.KeyItem>>,
    val params: KeyboardParams
): Keyboard {
    @SuppressLint("ClickableViewAccessibility")
    override fun createView(
        context: Context,
        listener: KeyboardListener
    ): KeyboardViewManager {
        val keyboardListener = DefaultKeyboardView.Listener(context, listener, params)
        val inflater = LayoutInflater.from(ContextThemeWrapper(context, R.style.Theme_FusionIME_Keyboard))
        val keyboard = KbdKeyboardBinding.inflate(inflater)
        val keySet = mutableSetOf<DefaultKeyboardView.KeyContainer>()
        rows.forEach { keys ->
            val row = KbdRowBinding.inflate(inflater)
            keys.forEach { item ->
                val view: View = when(item) {
                    is Keyboard.KeyItem.Spacer -> {
                        val view = View(context)
                        view.isClickable = true
                        view
                    }
                    is Keyboard.KeyItem.SpecialKey -> {
                        val type = SpecialKeyType.ofKeyCode(item.keyCode) ?: SpecialKeyType.Default
                        val themedInflater = LayoutInflater.from(ContextThemeWrapper(context, type.themeRes))
                        val key = KbdKeyBinding.inflate(themedInflater)
                        if(type.iconRes != null) key.icon.setImageResource(type.iconRes)
                        keySet += DefaultKeyboardView.KeyContainer(item.keyCode, key)
                        key.root.setOnTouchListener(createOnTouchListener(item.keyCode, keyboardListener))
                        key.root
                    }
                    is Keyboard.KeyItem.Key -> {
                        val themedInflater = LayoutInflater.from(ContextThemeWrapper(context, R.style.Theme_FusionIME_Keyboard_Key))
                        val key = KbdKeyBinding.inflate(themedInflater)
                        keySet += DefaultKeyboardView.KeyContainer(item.keyCode, key)
                        key.root.setOnTouchListener(createOnTouchListener(item.keyCode, keyboardListener))
                        key.root
                    }
                }
                view.layoutParams = createLayoutParams(item.width)
                row.root.addView(view)
            }
            keyboard.root.addView(row.root)
        }
        return DefaultKeyboardView(keyboard, keySet)
    }

    private fun createLayoutParams(width: Float): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            0,
            params.height / rows.size
        ).apply {
            weight = width
        }
    }

    private fun createOnTouchListener(code: Int, listener: KeyboardListener): View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        return View.OnTouchListener { view, event ->
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
    }

    enum class SpecialKeyType(
        val keyCode: Int,
        val themeRes: Int,
        val iconRes: Int?
    ) {
        Default(
            -1,
            R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            iconRes = null
        ),

        LeftShift(
            keyCode = KeyEvent.KEYCODE_SHIFT_LEFT,
            themeRes = R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            iconRes = R.drawable.keyic_shift
        ),

        RightShift(
            keyCode = KeyEvent.KEYCODE_SHIFT_RIGHT,
            themeRes = R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            iconRes = R.drawable.keyic_shift
        ),

        Language(
            keyCode = KeyEvent.KEYCODE_LANGUAGE_SWITCH,
            themeRes = R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            iconRes = R.drawable.keyic_language
        ),
        Symbol(
            keyCode = KeyEvent.KEYCODE_SYM,
            themeRes = R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            iconRes = R.drawable.keyic_option
        ),

        Numbers(
            keyCode = KeyEvent.KEYCODE_NUM,
            themeRes = R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            iconRes = R.drawable.keyic_numbers
        ),

        Return(
            keyCode = KeyEvent.KEYCODE_ENTER,
            themeRes = R.style.Theme_FusionIME_Keyboard_Key_Return,
            iconRes = R.drawable.keyic_return
        ),

        Delete(
            keyCode = KeyEvent.KEYCODE_DEL,
            themeRes = R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            iconRes = R.drawable.keyic_delete
        ),

        Space(
            keyCode = KeyEvent.KEYCODE_SPACE,
            themeRes = R.style.Theme_FusionIME_Keyboard_Key,
            iconRes = R.drawable.keyic_space
        );

        companion object {
            val keyCodeMap = SpecialKeyType.entries.associateBy { it.keyCode }
            fun ofKeyCode(keyCode: Int): SpecialKeyType? {
                return keyCodeMap[keyCode]
            }
        }
    }
}