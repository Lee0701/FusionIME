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
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import ee.oyatl.ime.keyboard.listener.EmptyListener
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.popup.EmptyPopupManager
import ee.oyatl.ime.keyboard.popup.PopupManager
import ee.oyatl.ime.keyboard.touchhandler.EmptyTouchHandler
import ee.oyatl.ime.keyboard.touchhandler.TouchHandler
import ee.oyatl.ime.theme.KeyTheme
import ee.oyatl.ime.theme.KeyboardTheme
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

    override var labels: Map<Int, KeyLabel> = mapOf()
        set(value) {
            field = value
            keySet.forEach {
                val label = value[it.keyCode] ?: return@forEach
                when(label) {
                    is KeyLabel.Default -> {
                        if(label.text != null) it.label = label.text
                        if(label.icon != null) it.icon = AppCompatResources.getDrawable(context, label.icon)
                    }
                    is KeyLabel.Flick -> {
                        if(label.text != null) it.label = label.text
                        if(label.showAsHint) {
                            if(label.up != null) it.labelHintTop = label.up
                            if(label.down != null) it.labelHintBottom = label.down
                            if(label.left != null) it.labelHintLeft = label.left
                            if(label.right != null) it.labelHintRight = label.right
                        }
                    }
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

    val bitmapPaint = Paint()
    val textPaint = Paint()

    init {
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
                        val icon = if(item.iconRes != 0) ContextCompat.getDrawable(context, item.iconRes) else null
                        val label = if(item.keyCode < 0) (-item.keyCode).toChar().toString() else ""
                        val background = AppCompatResources.getDrawable(context, item.bkgRes)
                        val keyTheme = keyboardTheme?.keyThemes[item.keyType]
                        val key = CachedKey(item.keyCode, label, background, keyTheme, icon)
                        key.rect.set(x, y, x + width, y + rowHeight)
                        keySet += key
                    }
                    is Keyboard.KeyItem.SplitSpacer -> {
                        if(splitKeyboard) {
                            unitWidth = rowWidth / items.drop(splitSpacerIndex + 1).map { it.width }.sum()
                            x += keyboard.params.splitWidth
                        }
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

    override fun findKeys(keyCode: Int): List<KeyboardView.Key> {
        return keySet.filter { it.keyCode == keyCode }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val keyboardTheme = keyboardTheme ?: return
        getLocalVisibleRect(rect)

        if(!rect.isEmpty) {
            val bitmap = context
                .getColor(keyboardTheme.backgroundColor)
                .toDrawable()
                .toBitmap(rect.width(), rect.height())
            canvas.drawBitmap(bitmap, 0f, 0f, bitmapPaint)
        }

        keySet.forEach { key ->
            val keyTheme = key.keyTheme ?: return@forEach
            val background = key.background ?: return@forEach
            val keyState = intArrayOf(if(key.pressed) android.R.attr.state_pressed else 0)
            val tintColorStateList = context.getColorStateList(keyTheme.backgroundColor)
            val tint = tintColorStateList.getColorForState(keyState, tintColorStateList.defaultColor)
            DrawableCompat.setTint(background, tint)
            canvas.drawBitmap(
                background.toBitmap(key.rect.width(), key.rect.height()),
                key.rect.left.toFloat(), key.rect.top.toFloat(),
                bitmapPaint
            )
        }

        keySet.forEach { key ->
            val keyTheme = key.keyTheme ?: return@forEach
            val icon = key.icon
            val label = key.label
            // Draw hint labels
            val labelHints = listOf(
                key.labelHintTop,
                key.labelHintBottom,
                key.labelHintLeft,
                key.labelHintRight
            )
            val hintSize = context.resources.getDimension(R.dimen.key_hint_text_size)
            val positions = listOf(
                key.rect.centerX().toFloat() to key.rect.top + hintSize*3/2,
                key.rect.centerX().toFloat() to key.rect.bottom - hintSize*2/3,
                key.rect.left + hintSize to key.rect.centerY().toFloat() + hintSize/2,
                key.rect.right - hintSize to key.rect.centerY().toFloat() + hintSize/2
            )
            labelHints.zip(positions).forEach { (label, position) ->
                label ?: return@forEach
                val (x, y) = position
                textPaint.color = context.getColor(keyTheme.hintColor)
                textPaint.textSize = hintSize
                canvas.drawText(label, x, y, textPaint)
            }
            // Draw icon
            if(icon != null) {
                DrawableCompat.setTint(icon, context.getColor(keyTheme.foregroundColor))
                val bitmap = icon.toBitmap()
                val x = key.rect.centerX() - bitmap.width / 2
                val y = key.rect.centerY() - bitmap.height / 2
                canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), bitmapPaint)
            }
            // Draw main label
            if(label.isNotEmpty()) {
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.color = context.getColor(keyTheme.foregroundColor)
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
        invalidate()
    }

    inner class CachedKey(
        override val keyCode: Int,
        override var label: String,
        val background: Drawable? = null,
        val keyTheme: KeyTheme? = null,
        var icon: Drawable? = null,
        var labelHintTop: String? = null,
        var labelHintBottom: String? = null,
        var labelHintLeft: String? = null,
        var labelHintRight: String? = null
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
}