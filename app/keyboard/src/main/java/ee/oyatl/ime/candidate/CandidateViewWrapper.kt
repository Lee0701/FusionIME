package ee.oyatl.ime.candidate

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import ee.oyatl.ime.keyboard.R

class CandidateViewWrapper(
    private val candidateView: CandidateView,
    onItemClick: (CandidateView.Candidate) -> Unit
) {
    val view: View = LinearLayout(candidateView.context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            context.resources.getDimensionPixelSize(R.dimen.candidate_view_height),
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        setBackgroundColor(context.resources.getColor(R.color.keyboard_bg_light))
        addView(candidateView)
    }
    private val adapter: CandidateView.Adapter = CandidateView.Adapter(onItemClick)
    init {
        candidateView.adapter = adapter
    }

    fun submitList(list: List<CandidateView.Candidate>){
        adapter.submitList(list)
        view.visibility = if(list.isNotEmpty()) View.VISIBLE else View.GONE
    }
}