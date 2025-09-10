package ee.oyatl.ime.fusion.korean

import ee.oyatl.ime.candidate.CandidateView

interface HanjaConverter {
    fun convert(text: String): List<CandidateView.Candidate>
}