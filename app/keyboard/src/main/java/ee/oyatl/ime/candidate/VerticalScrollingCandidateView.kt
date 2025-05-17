package ee.oyatl.ime.candidate

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import ee.oyatl.ime.keyboard.R

class VerticalScrollingCandidateView(
    context: Context,
    attrs: AttributeSet?,
    rowCount: Int
): RecyclerCandidateView(context, attrs) {

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 2)

    init {
        setBackgroundColor(resources.getColor(R.color.candidate_bg_light))
        layoutManager = FlexboxLayoutManager(
            context,
            FlexDirection.ROW,
            FlexWrap.WRAP
        )
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            context.resources.getDimensionPixelSize(R.dimen.candidate_view_height) * rowCount
        )
    }

    override fun submitList(list: List<CandidateView.Candidate>) {
        super.submitList(list)
        this.visibility = if(list.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(ev) || this.onTouchEvent(ev)
    }
}