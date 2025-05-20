package ee.oyatl.ime.candidate

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import ee.oyatl.ime.keyboard.R

class TripleCandidateView(
    context: Context,
    attributeSet: AttributeSet?
): RecyclerCandidateView(context, attributeSet) {
    init {
        setBackgroundColor(backgroundColor)
        layoutManager = FlexboxLayoutManager(
            context,
            FlexDirection.ROW
        ).apply {
            justifyContent = JustifyContent.SPACE_AROUND
        }
        setHasFixedSize(true)
    }

    override fun submitList(list: List<CandidateView.Candidate>) {
        val firstThree = list.take(3)
        val result = listOfNotNull(
            firstThree.getOrNull(1),
            firstThree.getOrNull(0),
            firstThree.getOrNull(2)
        )
        super.submitList(result)
    }
}