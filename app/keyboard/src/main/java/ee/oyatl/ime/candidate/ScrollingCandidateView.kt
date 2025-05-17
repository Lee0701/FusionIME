package ee.oyatl.ime.candidate

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import ee.oyatl.ime.keyboard.R

class ScrollingCandidateView(
    context: Context,
    attributeSet: AttributeSet?
): RecyclerCandidateView(context, attributeSet) {
    init {
        setBackgroundColor(backgroundColor)
        layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
    }
}