package ee.oyatl.ime.fusion.korean

import android.content.Context
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.newdict.DiskHanjaDictionary
import ee.oyatl.ime.newdict.DiskTrieDictionary

class UnigramHanjaConverter(
    context: Context
): HanjaConverter {
    private val indexDict: DiskTrieDictionary =
        DiskTrieDictionary(context.resources.openRawResource(R.raw.hanja_index))
    private val vocabDict: DiskHanjaDictionary =
        DiskHanjaDictionary(context.resources.openRawResource(R.raw.hanja_content))

    override fun convert(text: String): List<CandidateView.Candidate> {
        val hanjaResult = (1 .. text.length).map { l ->
            indexDict.get(text.take(l))
                .map { vocabDict.get(it) }
                .filter { it.hanja.length == l }
                .map { Candidate(it.hanja, it.frequency.toFloat(), it.extra) }
        }.flatten()
        return hanjaResult
            .sortedByDescending { it.score }
            .distinctBy { it.text }
            .sortedByDescending { it.text.length }
    }

    data class Candidate(
        override val text: CharSequence,
        val score: Float,
        override val extra: CharSequence = ""
    ): CandidateView.Candidate, CandidateView.ExtraCandidate
}