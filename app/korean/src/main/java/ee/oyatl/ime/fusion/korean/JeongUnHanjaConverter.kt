package ee.oyatl.ime.fusion.korean

import android.content.Context
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.dictionary.DiskDictionary

class JeongUnHanjaConverter(
    context: Context
): HanjaConverter {
    private val dict: DiskDictionary =
        DiskDictionary(context.resources.openRawResource(R.raw.jeongun))
    override fun convert(text: String): List<CandidateView.Candidate> {
        return (1 .. text.length).asSequence()
            .map { l -> dict.search(text.take(l)) }
            .flatten()
            .map { Candidate(it.result) }
            .toList()
    }
    data class Candidate(
        override val text: CharSequence
    ): CandidateView.Candidate
}