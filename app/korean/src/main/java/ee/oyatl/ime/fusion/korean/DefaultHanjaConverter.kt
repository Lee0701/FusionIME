package ee.oyatl.ime.fusion.korean

import android.content.Context
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.dictionary.DiskDictionary
import ee.oyatl.ime.newdict.DiskHanjaDictionary
import ee.oyatl.ime.newdict.DiskTrieDictionary

class DefaultHanjaConverter(
    context: Context
): HanjaConverter {
    private val indexDict: DiskTrieDictionary =
        DiskTrieDictionary(context.resources.openRawResource(R.raw.hanja_index))
    private val vocabDict: DiskHanjaDictionary =
        DiskHanjaDictionary(context.resources.openRawResource(R.raw.hanja_content))
    private val unigramsDict: DiskDictionary =
        DiskDictionary(context.resources.openRawResource(R.raw.unigrams))

    override fun convert(text: String): List<CandidateView.Candidate> {
        val hanjaResult = (1 .. text.length).map { l ->
            indexDict.get(text.take(l))
                .map { vocabDict.get(it) }
                .filter { it.hanja.length == l }
                .map { Candidate(it.hanja, it.frequency.toFloat(), it.extra) }
        }.flatten()
        val unigramResult = (1 .. text.length).asSequence()
            .map { l -> unigramsDict.search(text.take(l)) }
            .flatten()
            .map { Candidate(it.result, it.frequency.toFloat()) }
            .toList()
        return (unigramResult + hanjaResult)
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