package ee.oyatl.ime.fusion

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class TouchBlockingFrameLayout(
    context: Context,
    attrs: AttributeSet
): FrameLayout(context, attrs) {
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return true
    }
}