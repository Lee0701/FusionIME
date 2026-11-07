package ee.oyatl.ime.fusion.cjkv

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue
import android.text.Spanned
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.TextView
import androidx.core.graphics.ColorUtils

/**
 * A TextView that draws a faint underline under its tappable (clickable) spans,
 * highlights the one being pressed the way a candidate is, and lets a
 * [LongPressSpan] be long-pressed as well as tapped.
 *
 * The span's own underline would be as dark as the text and too heavy for a
 * panel where almost every value can be tapped, and TextPaint has no public way
 * to thin or lighten it, so the line is drawn here from the text layout.
 */
@SuppressLint("AppCompatCustomView")
class CjkvTappableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
): TextView(context, attrs) {
    private val underlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density
    }
    private val underlineOffset = resources.displayMetrics.density * 3
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    // The pressed highlight uses the colour candidates get from ?android:selectableItemBackground.
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TypedValue().let { value ->
            if(context.theme.resolveAttribute(android.R.attr.colorControlHighlight, value, true)) value.data
            else 0x1F000000
        }
    }
    private val highlightRadius = resources.displayMetrics.density * 4
    private val highlightPadding = resources.displayMetrics.density * 2
    private val highlightRect = RectF()

    init {
        // LinkMovementMethod would also select the pressed span in the accent colour.
        highlightColor = android.graphics.Color.TRANSPARENT
    }
    private var highlightedSpan: ClickableSpan? = null
    private val clearHighlight = Runnable {
        highlightedSpan = null
        invalidate()
    }

    /** A clickable span that also does something on a long press. */
    interface LongPressSpan {
        val canLongPress: Boolean
        fun onLongPress(widget: View)
    }

    private var pressedSpan: LongPressSpan? = null
    private var longPressed = false
    private var downX = 0f
    private var downY = 0f
    private val longPressRunnable = Runnable {
        val span = pressedSpan ?: return@Runnable
        longPressed = true
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        span.onLongPress(this)
    }

    /** Draws the pressed span's background behind its text, line by line. */
    private fun drawHighlight(canvas: Canvas) {
        val span = highlightedSpan ?: return
        val spanned = text as? Spanned ?: return
        val layout = layout ?: return
        val start = spanned.getSpanStart(span)
        val end = spanned.getSpanEnd(span)
        if(start < 0 || start >= end) return
        canvas.save()
        canvas.translate(totalPaddingLeft.toFloat(), totalPaddingTop.toFloat())
        for(line in layout.getLineForOffset(start)..layout.getLineForOffset(end)) {
            val lineEnd = layout.getLineEnd(line)
            val segmentStart = maxOf(start, layout.getLineStart(line))
            val segmentEnd = minOf(end, lineEnd)
            if(segmentStart >= segmentEnd) continue
            val x1 = layout.getPrimaryHorizontal(segmentStart)
            val x2 = if(segmentEnd >= lineEnd) layout.getLineRight(line) else layout.getPrimaryHorizontal(segmentEnd)
            highlightRect.set(
                minOf(x1, x2) - highlightPadding, layout.getLineTop(line).toFloat(),
                maxOf(x1, x2) + highlightPadding, layout.getLineBottom(line).toFloat(),
            )
            canvas.drawRoundRect(highlightRect, highlightRadius, highlightRadius, highlightPaint)
        }
        canvas.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                removeCallbacks(clearHighlight)
                highlightedSpan = clickableSpanAt(event.x, event.y)
                invalidate()
                longPressed = false
                downX = event.x
                downY = event.y
                pressedSpan = longPressSpanAt(event.x, event.y)
                if(pressedSpan != null) postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> {
                if(Math.abs(event.x - downX) > touchSlop || Math.abs(event.y - downY) > touchSlop) {
                    cancelLongPress()
                    clearHighlight.run()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Keep a quick tap visible for a moment, as a ripple would be.
                postDelayed(clearHighlight, HIGHLIGHT_LINGER)
                cancelLongPress()
                if(longPressed) {
                    // The long press already acted; do not also treat it as a tap.
                    longPressed = false
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun cancelLongPress() {
        super.cancelLongPress()
        removeCallbacks(longPressRunnable)
        pressedSpan = null
    }

    private fun longPressSpanAt(x: Float, y: Float): LongPressSpan? {
        return (clickableSpanAt(x, y) as? LongPressSpan)?.takeIf { it.canLongPress }
    }

    private fun clickableSpanAt(x: Float, y: Float): ClickableSpan? {
        val spanned = text as? Spanned ?: return null
        val layout = layout ?: return null
        val localX = x - totalPaddingLeft + scrollX
        val localY = (y - totalPaddingTop + scrollY).toInt()
        val line = layout.getLineForVertical(localY)
        if(localX < layout.getLineLeft(line) || localX > layout.getLineRight(line)) return null
        val offset = layout.getOffsetForHorizontal(line, localX)
        return spanned.getSpans(offset, offset, ClickableSpan::class.java)
            .firstOrNull { spanContains(spanned, it, offset, layout.getPrimaryHorizontal(offset), localX) }
    }

    /** getSpans at a boundary returns both neighbours; keep the one actually under the finger. */
    private fun spanContains(spanned: Spanned, span: Any, offset: Int, offsetX: Float, x: Float): Boolean {
        val start = spanned.getSpanStart(span)
        val end = spanned.getSpanEnd(span)
        return when(offset) {
            start -> x >= offsetX
            end -> x <= offsetX
            else -> true
        }
    }

    override fun onDraw(canvas: Canvas) {
        drawHighlight(canvas)
        super.onDraw(canvas)
        val spanned = text as? Spanned ?: return
        val layout = layout ?: return
        val spans = spanned.getSpans(0, spanned.length, ClickableSpan::class.java)
        if(spans.isEmpty()) return
        underlinePaint.color = ColorUtils.setAlphaComponent(currentTextColor, UNDERLINE_ALPHA)
        canvas.save()
        canvas.translate(totalPaddingLeft.toFloat(), totalPaddingTop.toFloat())
        spans.forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            if(start >= end) return@forEach
            for(line in layout.getLineForOffset(start)..layout.getLineForOffset(end)) {
                val lineStart = layout.getLineStart(line)
                val lineEnd = layout.getLineEnd(line)
                val segmentStart = maxOf(start, lineStart)
                val segmentEnd = minOf(end, lineEnd)
                if(segmentStart >= segmentEnd) continue
                val x1 = layout.getPrimaryHorizontal(segmentStart)
                val x2 = if(segmentEnd >= lineEnd) layout.getLineRight(line)
                    else layout.getPrimaryHorizontal(segmentEnd)
                val y = layout.getLineBaseline(line) + underlineOffset
                canvas.drawLine(minOf(x1, x2), y, maxOf(x1, x2), y, underlinePaint)
            }
        }
        canvas.restore()
    }

    companion object {
        private const val UNDERLINE_ALPHA = 0x33
        private const val HIGHLIGHT_LINGER = 120L
    }
}
