package ee.oyatl.ime.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import ee.oyatl.ime.keyboard.databinding.KbdKeyBinding
import ee.oyatl.ime.keyboard.databinding.KbdKeyboardBinding
import ee.oyatl.ime.keyboard.databinding.KbdRowBinding
import ee.oyatl.ime.keyboard.listener.EmptyListener
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.popup.EmptyPopupManager
import ee.oyatl.ime.keyboard.popup.PopupManager
import ee.oyatl.ime.keyboard.touchhandler.EmptyTouchHandler
import ee.oyatl.ime.keyboard.touchhandler.TouchHandler
import ee.oyatl.ime.theme.KeyboardTheme
import kotlin.math.max
import kotlin.math.roundToInt

class DefaultKeyboardView(
    context: Context,
    attrs: AttributeSet?
): FrameLayout(context, attrs), KeyboardView {
    override val view: View = this

    override val rect: Rect = Rect()
    override val location: IntArray = IntArray(2)
    private val keySet: MutableSet<CachedKey> = mutableSetOf()

    override var labels: Map<Int, KeyLabel> = mapOf()
        set(value) {
            field = value
            keySet.forEach {
                val label = value[it.keyCode] ?: return@forEach
                if(label.text != null) it.binding.label.text = label.text
                if(label.icon != null) it.binding.icon.setBackgroundResource(label.icon)
                if(label.showFlickHint) {
                    if(label.up != null) it.binding.labelHintTop.text = label.up
                    if(label.down != null) it.binding.labelHintBottom.text = label.down
                    if(label.left != null) it.binding.labelHintLeft.text = label.left
                    if(label.right != null) it.binding.labelHintRight.text = label.right
                }
            }
        }

    override var keyboardTheme: KeyboardTheme? = null
    override var keyboard: Keyboard? = null
        set(value) {
            field = value
            if(value != null) setup(value)
        }
    override var listener: KeyboardListener = EmptyListener
    override var popupManager: PopupManager = EmptyPopupManager
    var touchHandler: TouchHandler = EmptyTouchHandler

    init {
        viewTreeObserver.addOnGlobalLayoutListener {
            updateKeyLocations()
        }
    }

    private fun setup(keyboard: Keyboard) {
        val inflater = LayoutInflater.from(ContextThemeWrapper(
            context, androidx.appcompat.R.style.Theme_AppCompat))
        val binding = KbdKeyboardBinding.inflate(inflater)
        val keyboardTheme = keyboardTheme
        if(keyboardTheme != null && keyboard.params.screenMode != KeyboardState.ScreenMode.MoreKeys) {
            binding.root.setBackgroundColor(context.getColor(keyboardTheme.backgroundColor))
        }

        keySet.clear()

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
                    is Keyboard.KeyItem.Key -> {
                        val key = KbdKeyBinding.inflate(inflater)
                        if(item.iconRes != 0) key.icon.setBackgroundResource(item.iconRes)
                        if(item.keyCode < 0) key.label.text = (-item.keyCode).toChar().toString()

                        val keyTheme = keyboardTheme?.keyThemes[item.keyType]
                        if(keyTheme != null) {
                            key.root.setBackgroundResource(item.bkgRes)
                            key.root.backgroundTintList = context.getColorStateList(keyTheme.backgroundColor)
                            key.icon.backgroundTintList = ColorStateList.valueOf(context.getColor(keyTheme.foregroundColor))
                            key.label.setTextColor(context.getColor(keyTheme.foregroundColor))
                            val hintColor = context.getColor(keyTheme.hintColor)
                            listOf(key.labelHintTop, key.labelHintLeft, key.labelHintRight, key.labelHintBottom).forEach { label ->
                                label.setTextColor(hintColor)
                            }
                        }

                        keySet += CachedKey(item.keyCode, key)
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

    override fun onReset() {
        touchHandler.onReset()
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
                touchHandler.onTouchDown(pointerId, x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                touchHandler.onTouchMove(pointerId, x, y)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                touchHandler.onTouchUp(pointerId, x, y)
            }
        }
        return true
    }

    private fun updateKeyLocations() {
        val rect = Rect()
        this.getLocationOnScreen(location)
        this.getGlobalVisibleRect(rect)
        keySet.forEach {
            it.binding.root.getGlobalVisibleRect(it.rect)
            it.binding.root.getLocationOnScreen(it.location)
            it.rect.offset(0, -rect.top)
        }
    }

    override fun findKey(x: Int, y: Int): CachedKey? {
        return keySet.find { key ->
            key.rect.contains(x, y)
        }
    }

    override fun findKeys(keyCode: Int): List<KeyboardView.Key> {
        return keySet.filter { it.keyCode == keyCode }
    }

    data class CachedKey(
        override val keyCode: Int,
        val binding: KbdKeyBinding
    ): KeyboardView.Key {
        override val label: String get() = binding.label.text.toString()
        override val rect: Rect = Rect()
        override val location: IntArray = IntArray(2)

        override fun onPressed() {
            binding.root.isPressed = true
        }

        override fun onReleased() {
            binding.root.isPressed = false
        }
    }

    private fun createLayoutParams(width: Float, height: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            0,
            height
        ).apply {
            weight = width
        }
    }
}