package ee.oyatl.ime.fusion

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.HorizontalScrollView
import android.widget.TextView

class ClipboardMarqueeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.horizontalScrollViewStyle
) : HorizontalScrollView(context, attrs, defStyleAttr) {
    private var scrollRequested = false
    private var animator: ValueAnimator? = null

    private val contentTextView: TextView
        get() = getChildAt(0) as TextView

    var text: CharSequence
        get() = contentTextView.text
        set(value) {
            contentTextView.text = value
        }

    private val startRunnable = Runnable { startAnimation() }
    private val repeatRunnable = Runnable {
        if(!scrollRequested) return@Runnable
        scrollTo(0, 0)
        postDelayed(startRunnable, START_DELAY_MILLIS)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        contentTextView.setOnClickListener { performClick() }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun startScrolling() {
        if(scrollRequested) return
        scrollRequested = true
        scrollTo(0, 0)
        postDelayed(startRunnable, START_DELAY_MILLIS)
    }

    fun stopScrolling() {
        scrollRequested = false
        removeCallbacks(startRunnable)
        removeCallbacks(repeatRunnable)
        animator?.cancel()
        animator = null
        scrollTo(0, 0)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if(width != oldWidth && scrollRequested) restartScrolling()
    }

    override fun onDetachedFromWindow() {
        stopScrolling()
        super.onDetachedFromWindow()
    }

    private fun restartScrolling() {
        stopScrolling()
        startScrolling()
    }

    private fun startAnimation() {
        if(!scrollRequested || !isShown) return

        val availableWidth = width - paddingLeft - paddingRight
        val overflow = (contentTextView.width - availableWidth).coerceAtLeast(0)
        if(overflow <= 0) return

        animator = ValueAnimator.ofInt(0, overflow).apply {
            duration = ((overflow / (SCROLL_DP_PER_SECOND * resources.displayMetrics.density)) * 1000)
                .toLong()
                .coerceAtLeast(MINIMUM_SCROLL_MILLIS)
            interpolator = LinearInterpolator()
            addUpdateListener { scrollTo(it.animatedValue as Int, 0) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animator = null
                    if(scrollRequested) postDelayed(repeatRunnable, END_PAUSE_MILLIS)
                }
            })
            start()
        }
    }

    companion object {
        private const val START_DELAY_MILLIS = 1200L
        private const val END_PAUSE_MILLIS = 800L
        private const val MINIMUM_SCROLL_MILLIS = 1000L
        private const val SCROLL_DP_PER_SECOND = 36f
    }
}
