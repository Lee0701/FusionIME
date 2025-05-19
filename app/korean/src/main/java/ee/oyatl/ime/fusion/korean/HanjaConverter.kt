package ee.oyatl.ime.fusion.korean

import android.content.Context
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.dictionary.DiskDictionary
import ee.oyatl.ime.dictionary.DiskIndexDictionary
import ee.oyatl.ime.dictionary.DiskVocabDictionary

class HanjaConverter(
    context: Context
) {
    private val hanjaDict: DiskDictionary = DiskDictionary(context.resources.openRawResource(R.raw.hanja))
    private val vocabDict: DiskVocabDictionary = DiskVocabDictionary(context.resources.openRawResource(R.raw.vocab))
    private val unigramsDict: DiskIndexDictionary = DiskIndexDictionary(context.resources.openRawResource(R.raw.unigrams))
    private val bigramsDict: DiskIndexDictionary = DiskIndexDictionary(context.resources.openRawResource(R.raw.bigrams))

    fun convert(text: String): List<CandidateView.Candidate> {
        val hanjaResult = (1 .. text.length).map { l ->
            hanjaDict.search(text.take(l))
                .filter { it.result.length == l }
                .map { Candidate(it.result, it.frequency.toFloat()) }
        }.flatten()
        val unigramResult = (1 .. text.length).asSequence()
            .map { l -> unigramsDict.search(text.take(l)) }
            .flatten()
            .map { vocabDict[it] }
            .map { Candidate(it.result, it.frequency.toFloat()) }
            .toList()
        val bigramResult = (1 .. text.length).asSequence()
            .map { l ->
                val firsts = unigramsDict.search(text.take(l)).take(3)
                val seconds = (1 .. l + 1).flatMap { l2 ->
                    unigramsDict.search(text.drop(l).take(l2))
                }
                firsts.flatMap { first -> seconds.map { second -> listOf(first, second) } }
            }
            .flatten().map { it to bigramsDict.search(it) }
            .filter { (_, value) -> value.isNotEmpty() }
            .map { (key, value) -> key.map { vocabDict[it] } to value }
            .map { (key, value) -> Candidate(
                text = key.joinToString("") { it.result },
                score = value[0].toFloat()
            ) }
            .sortedByDescending { it.score }
            .distinct()
            .toList()
        return (unigramResult + bigramResult + hanjaResult)
            .sortedByDescending { it.score }
            .distinctBy { it.text }
            .sortedByDescending { it.text.length }
    }

    data class Candidate(
        override val text: CharSequence,
        val score: Float
    ): CandidateView.Candidate
}