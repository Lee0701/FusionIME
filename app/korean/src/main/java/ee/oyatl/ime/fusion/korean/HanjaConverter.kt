package ee.oyatl.ime.fusion.korean

import android.content.Context
import ee.oyatl.ime.candidate.CandidateView

interface HanjaConverter {
    fun load(context: Context)
    suspend fun convert(text: String): List<CandidateView.Candidate>
}