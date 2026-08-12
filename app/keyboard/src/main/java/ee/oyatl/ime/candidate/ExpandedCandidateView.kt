package ee.oyatl.ime.candidate

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager

class ExpandedCandidateView(
    context: Context,
    attrs: AttributeSet?
): RecyclerCandidateView(context, attrs) {
    init {
        setBackgroundColor(backgroundColor)
        layoutManager = FlexboxLayoutManager(
            context,
            FlexDirection.ROW,
            FlexWrap.WRAP
        )
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(ev) || this.onTouchEvent(ev)
    }
}