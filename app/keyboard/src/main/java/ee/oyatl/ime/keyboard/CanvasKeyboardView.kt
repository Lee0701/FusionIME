package ee.oyatl.ime.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import ee.oyatl.ime.keyboard.listener.EmptyListener
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.popup.EmptyPopupManager
import ee.oyatl.ime.keyboard.popup.PopupManager
import ee.oyatl.ime.keyboard.touchhandler.CompoundTouchHandler
import ee.oyatl.ime.keyboard.touchhandler.TouchHandler
import kotlin.math.max
import kotlin.math.roundToInt

class CanvasKeyboardView(
    context: Context,
    attrs: AttributeSet?
): View(context, attrs), KeyboardView {
    override val view: View get() = this

    override val rect: Rect = Rect()
    override val location: IntArray = IntArray(2)
    private val keySet: MutableSet<CachedKey> = mutableSetOf()

    var keyboard: Keyboard? = null
        set(value) {
            field = value
            if(value != null) setup(value)
        }
    override var listener: KeyboardListener = EmptyListener
    override var popupManager: PopupManager = EmptyPopupManager
    var touchHandler: TouchHandler = CompoundTouchHandler(this)

    private val keyboardBackground: Drawable
    private val specialKeyStyles: Map<SpecialKeyType?, KeyStyle>

    val bitmapPaint = Paint()
    val textPaint = Paint()

    init {
        val typedValue = TypedValue()
        val keyboardContext = ContextThemeWrapper(context, R.style.Theme_FusionIME_Keyboard)

        keyboardContext.theme.resolveAttribute(R.attr.backgroundColor, typedValue, true)
        keyboardBackground = ContextCompat.getColor(keyboardContext, typedValue.resourceId).toDrawable()

        val keyStyle = KeyStyle.fromTheme(context, R.style.Theme_FusionIME_Keyboard_Key)
        specialKeyStyles = SpecialKeyType.entries.mapNotNull { type ->
            val style = KeyStyle.fromTheme(context, type.themeRes)
            if (style == null) null
            else type to style
        }.toMap() + listOfNotNull(keyStyle).associateBy { null }

        viewTreeObserver.addOnGlobalLayoutListener {
            updateKeyLocations()
        }
    }

    private fun setup(keyboard: Keyboard) {
        keySet.clear()
        val keyboardWidth = context.resources.displayMetrics.widthPixels
        val rowHeight = keyboard.params.height / keyboard.rows.size
        var y = 0
        keyboard.rows.forEach { items ->
            val splitSpacerIndex = items.indexOfFirst { it is Keyboard.KeyItem.SplitSpacer }
            val splitKeyboard = splitSpacerIndex != -1 && keyboard.params.splitWidth != 0
            val rowWidth =
                if(!splitKeyboard) keyboardWidth
                else (keyboardWidth - keyboard.params.splitWidth) / 2
            var unitWidth = rowWidth / items.map { it.width }.sum()
            if(splitKeyboard)
                unitWidth = rowWidth / items.take(splitSpacerIndex).map { it.width }.sum()
            var x = 0
            items.forEach { item ->
                val width = (unitWidth * item.width).toInt()
                when(item) {
                    is Keyboard.KeyItem.Key -> {
                        val isSpecialKey = item is Keyboard.KeyItem.SpecialKey
                        val specialKeyType = if(isSpecialKey) SpecialKeyType.ofKeyCode(item.keyCode) else null
                        val iconRes = specialKeyType?.iconRes
                        val icon = if(iconRes != null) ContextCompat.getDrawable(context, iconRes) else null
                        val label = if(item.keyCode < 0) (-item.keyCode).toChar().toString() else ""
                        val style = specialKeyStyles[specialKeyType]
                        val key = CachedKey(item.keyCode, label, style, icon)
                        key.rect.set(x, y, x + width, y + rowHeight)
                        keySet += key
                    }
                    is Keyboard.KeyItem.SplitSpacer -> {
                        unitWidth = rowWidth / items.drop(splitSpacerIndex + 1).map { it.width }.sum()
                        x += keyboard.params.splitWidth
                    }
                    else -> Unit
                }
                x += width
            }
            y += rowHeight
        }
        invalidate()
    }

    private fun updateKeyLocations() {
        val rect = Rect()
        this.getLocationOnScreen(location)
        this.getGlobalVisibleRect(rect)
        keySet.forEach {
            it.location[0] = it.rect.left
            it.location[1] = it.rect.top + location[1]
        }
    }

    override fun findKey(
        x: Int,
        y: Int
    ): KeyboardView.Key? {
        return keySet.find { key ->
            key.rect.contains(x, y)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        getLocalVisibleRect(rect)

        if(!rect.isEmpty) {
            canvas.drawBitmap(keyboardBackground.toBitmap(rect.width(), rect.height()), 0f, 0f, bitmapPaint)
        }

        keySet.forEach { key ->
            val style = key.style ?: return@forEach
            val background = style.background
            val keyState = intArrayOf(if(key.pressed) android.R.attr.state_pressed else 0)
            val tint = style.backgroundColorStateList.getColorForState(keyState, style.backgroundColorStateList.defaultColor)
            DrawableCompat.setTint(background, tint)
            canvas.drawBitmap(
                background.toBitmap(key.rect.width(), key.rect.height()),
                key.rect.left.toFloat(), key.rect.top.toFloat(),
                bitmapPaint
            )
        }

        keySet.forEach { key ->
            val style = key.style ?: return@forEach
            val icon = key.icon
            val label = key.label
            if(icon != null) {
                DrawableCompat.setTint(icon, style.foregroundColor)
                val bitmap = icon.toBitmap()
                val x = key.rect.centerX() - bitmap.width / 2
                val y = key.rect.centerY() - bitmap.height / 2
                canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), bitmapPaint)
            }
            if(label.isNotEmpty()) {
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.color = style.foregroundColor
                textPaint.textSize = context.resources.getDimension(R.dimen.key_text_size)
                val x = key.rect.centerX().toFloat()
                val y = key.rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(label, x, y, textPaint)
            }
        }
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val keyboard = keyboard ?: return
        val width = context.resources.displayMetrics.widthPixels
        val height = keyboard.params.height
        setMeasuredDimension(width, height)
    }

    override fun onReset() {
        listener.onReset()
    }

    override fun setLabels(labels: Map<Int, String>) {
        keySet.forEach {
            val label = labels[it.keyCode]
            if(label != null) it.label = label
        }
        invalidate()
    }

    override fun setIcons(icons: Map<Int, Int>) {
        keySet.forEach {
            val id = icons[it.keyCode]
            if(id != null) it.icon = ContextCompat.getDrawable(context, id)
        }
        invalidate()
    }

    inner class CachedKey(
        override val keyCode: Int,
        override var label: String,
        val style: KeyStyle?,
        var icon: Drawable? = null
    ): KeyboardView.Key {
        override val rect: Rect = Rect()
        override val location: IntArray = IntArray(2)
        var pressed: Boolean = false

        override fun onPressed() {
            this.pressed = true
            invalidate()
        }

        override fun onReleased() {
            this.pressed = false
            invalidate()
        }
    }

    data class KeyStyle(
        val background: Drawable,
        val backgroundColorStateList: ColorStateList,
        val foregroundColor: Int
    ) {
        companion object {
            fun fromTheme(context: Context, themeRes: Int): KeyStyle? {
                val typedValue = TypedValue()
                val keyContext = ContextThemeWrapper(context, themeRes)
                val background = ContextCompat.getDrawable(keyContext, R.drawable.key_bg) ?: return null
                keyContext.theme.resolveAttribute(R.attr.backgroundColor, typedValue, true)
                val backgroundColorStateList = ContextCompat.getColorStateList(keyContext, typedValue.resourceId) ?: ColorStateList(arrayOf(), intArrayOf())
                keyContext.theme.resolveAttribute(R.attr.foregroundColor, typedValue, true)
                val foregroundColor = ContextCompat.getColor(keyContext, typedValue.resourceId)
                return KeyStyle(
                    background,
                    backgroundColorStateList,
                    foregroundColor
                )
            }
        }
    }
}