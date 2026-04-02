package ee.oyatl.ime.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import ee.oyatl.ime.keyboard.databinding.KbdKeyBinding
import ee.oyatl.ime.keyboard.databinding.KbdKeyboardBinding
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding
import ee.oyatl.ime.keyboard.popup.Popup
import ee.oyatl.ime.keyboard.popup.PreviewPopup
import kotlin.math.max
import kotlin.math.roundToInt

class DefaultKeyboardView(
    context: Context,
    attrs: AttributeSet?
): KeyboardView(context, attrs) {
    private val rect: Rect = Rect()
    private val location = IntArray(2)
    private val keySet: MutableSet<KeyContainer> = mutableSetOf()
    private val pointers: MutableMap<Int, Pointer> = mutableMapOf()

    var keyboard: Keyboard? = null
    var listener: KeyboardListener? = null

    init {
        viewTreeObserver.addOnGlobalLayoutListener {
            cacheKeys()
        }
    }

    fun setup(keyboard: Keyboard, listener: KeyboardListener) {
        val inflater = LayoutInflater.from(ContextThemeWrapper(context, R.style.Theme_FusionIME_Keyboard))
        val binding = KbdKeyboardBinding.inflate(inflater)

        this.keyboard = keyboard
        this.listener = DefaultKeyboardListener(context, listener, keyboard.params)

        keySet.clear()
        pointers.clear()

        val keyHeight = keyboard.params.height / keyboard.rows.size
        keyboard.rows.forEach { keys ->
            val row = KbdRowBinding.inflate(inflater)
            var subRow = KbdRowBinding.inflate(inflater)
            keys.forEach { item ->
                when(item) {
                    is Keyboard.KeyItem.SplitSpacer -> {
                        if(keyboard.params.splitWidth == 0) return@forEach
                        subRow.root.layoutParams = createLayoutParams(1f, keyHeight)
                        row.root.addView(subRow.root)
                        val view = View(context)
                        view.isClickable = true
                        view.layoutParams = LinearLayout.LayoutParams(
                            item.absoluteWidth,
                            keyHeight
                        )
                        row.root.addView(view)
                        subRow = KbdRowBinding.inflate(inflater)
                    }
                    is Keyboard.KeyItem.Spacer -> {
                        val view = View(context)
                        view.isClickable = true
                        view.layoutParams = createLayoutParams(item.width, keyHeight)
                        subRow.root.addView(view)
                    }
                    is Keyboard.KeyItem.SpecialKey -> {
                        val type = SpecialKeyType.ofKeyCode(item.keyCode) ?: SpecialKeyType.Default
                        val themedInflater = LayoutInflater.from(ContextThemeWrapper(context, type.themeRes))
                        val key = KbdKeyBinding.inflate(themedInflater)
                        if(type.iconRes != null) key.icon.setImageResource(type.iconRes)
                        keySet += KeyContainer(item.keyCode, key)
                        key.root.layoutParams = createLayoutParams(item.width, keyHeight)
                        subRow.root.addView(key.root)
                    }
                    is Keyboard.KeyItem.Key -> {
                        val themedInflater = LayoutInflater.from(ContextThemeWrapper(context, R.style.Theme_FusionIME_Keyboard_Key))
                        val key = KbdKeyBinding.inflate(themedInflater)
                        keySet += KeyContainer(item.keyCode, key)
                        if(item.keyCode < 0) key.label.text = (-item.keyCode).toChar().toString()
                        key.root.layoutParams = createLayoutParams(item.width, keyHeight)
                        subRow.root.addView(key.root)
                    }
                }
            }
            subRow.root.layoutParams = createLayoutParams(1f, keyHeight)
            row.root.addView(subRow.root)
            binding.root.addView(row.root)
        }

        this.removeAllViews()
        this.addView(binding.root)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return super.onTouchEvent(event)
        this.getGlobalVisibleRect(rect)
        val pointerId = event.getPointerId(event.actionIndex)
        val rawX =
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) event.getRawX(event.actionIndex)
            else event.getX(event.actionIndex) + location[0]
        val rawY =
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) event.getRawY(event.actionIndex)
            else event.getY(event.actionIndex) + location[1]
        val x = rawX.roundToInt() - location[0]
        // Clamp event from outside (maybe intercepted from candidate view?)
        val y = max(0, rawY.roundToInt() - location[1])

        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val key = findKey(x, y) ?: return true
                val popup =
                    if(keyboard?.params?.previewPopups == true && key.binding.label.text.isNotEmpty())
                        PreviewPopup(context)
                    else null
                if(popup != null) {
                    popup.label = key.binding.label.text.toString()
                    popup.size = key.rect.width() to key.rect.height() * 2
                    val y = rect.top + key.location[1] - location[1] - key.rect.height()
                    popup.show(this, key.rect.left, y)
                }
                pointers += pointerId to Pointer(x, y, key, popup)
                key.binding.root.isPressed = true
                listener?.onKeyDown(key.keyCode, 0)
            }
            MotionEvent.ACTION_MOVE -> {
                val pointer = pointers[pointerId]
                val key = pointer?.key ?: findKey(x, y) ?: return true
                if(!key.rect.contains(x, y)) key.binding.root.isPressed = false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pointer = pointers[pointerId]
                val key = pointer?.key ?: findKey(x, y) ?: return true
                key.binding.root.isPressed = false
                listener?.onKeyUp(key.keyCode, 0)
                pointer?.popup?.hide()
                pointers -= pointerId
            }
        }
        return true
    }

    private fun cacheKeys() {
        val rect = Rect()
        this.getLocationOnScreen(location)
        this.getGlobalVisibleRect(rect)
        keySet.forEach {
            it.binding.root.getGlobalVisibleRect(it.rect)
            it.binding.root.getLocationOnScreen(it.location)
            it.rect.offset(0, -rect.top)
        }
    }

    private fun findKey(x: Int, y: Int): KeyContainer? {
        return keySet.find { key ->
            key.rect.contains(x, y)
        }
    }

    override fun setLabels(labels: Map<Int, String>) {
        keySet.forEach {
            val label = labels[it.keyCode]
            if(label != null) it.binding.label.text = label
        }
    }

    override fun setIcons(icons: Map<Int, Int>) {
        keySet.forEach {
            val icon = icons[it.keyCode]
            if(icon != null) it.binding.icon.setImageResource(icon)
        }
    }

    override fun onReset() {
        val listener = this.listener
        if(listener is DefaultKeyboardListener) listener.shiftState = KeyboardState.Shift.Released
        pointers.values.forEach {
            it.key.binding.root.isPressed = false
            it.popup?.hide()
        }
        pointers.clear()
    }

    data class Pointer(
        val x: Int,
        val y: Int,
        val key: KeyContainer,
        val popup: Popup?
    )

    data class KeyContainer(
        val keyCode: Int,
        val binding: KbdKeyBinding
    ) {
        val rect: Rect = Rect()
        val location: IntArray = IntArray(2)
    }

    private fun createLayoutParams(width: Float, height: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            0,
            height
        ).apply {
            weight = width
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
        ),

        Left(
            keyCode = KeyEvent.KEYCODE_DPAD_LEFT,
            themeRes = R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            iconRes = R.drawable.keyic_left
        ),

        Right(
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            themeRes = R.style.Theme_FusionIME_Keyboard_Key_Modifier,
            iconRes = R.drawable.keyic_right
        );

        companion object {
            val keyCodeMap = SpecialKeyType.entries.associateBy { it.keyCode }
            fun ofKeyCode(keyCode: Int): SpecialKeyType? {
                return keyCodeMap[keyCode]
            }
        }
    }
}