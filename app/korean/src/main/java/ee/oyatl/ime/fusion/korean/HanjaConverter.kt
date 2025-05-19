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

    fun predict(text: String): List<CandidateView.Candidate> {
        val result = decodeConversion(convertRecursive(text, DEPTH))
            .sortedByDescending { if(it is Candidate) it.score else 0f }
        return result
    }

    fun convert(text: String): List<CandidateView.Candidate> {
        val result = (1 .. text.length).map { l ->
            hanjaDict.search(text.take(l))
                .filter { it.result.length == l }
                .map { SingleCandidate(it.result, it.frequency.toFloat()) }
        }.flatten()
            .sortedByDescending { it.score }
            .sortedByDescending { it.text.length }
        return listOf(result.firstOrNull(), SingleCandidate(text.take(1), 0f)).filterNotNull() + result.drop(1)
    }

    private fun convertRecursive(text: String, depth: Int): List<List<Int>> {
        val firsts = (1..text.length).associateWith { len -> unigramsDict.search(text.take(len)) }
        val unigrams = firsts.values.flatten().map { listOf(it) }
        if(depth == 0) return unigrams
        val result = firsts.map { (off, items) ->
            val firstsTopN = items.sortedByDescending { vocabDict[it].frequency }.take(depth)
            val seconds = convertRecursive(text.drop(off), depth - 1)
            val secondsTopN = seconds
            secondsTopN.map { second ->
                firstsTopN.map { listOf(it) + second }
            }.flatten()
        }.flatten()
        return unigrams + result
    }

    private fun decodeConversion(data: List<List<Int>>): List<CandidateView.Candidate> {
        return data
            .map { list ->
                val candidates = list.map { vocabDict[it] }.map { SingleCandidate(it.result, it.frequency.toFloat()) }
                val bigramScore = getBigramScore(list)
                CompoundCandidate(candidates, bigramScore)
            }
            .filter { candidate ->
                candidate.text.length == 1 || candidate.text.length > candidate.candidates.size
            }
            .filter { candidate ->
                candidate.candidates.size == 1 || candidate.bigramScore > 0f
            }
    }

    private fun getBigramScore(list: List<Int>): Float {
        return list.zipWithNext().sumOf { (a, b) -> bigramsDict.search(listOf(a, b)).sum() }.toFloat() / list.size
//        return bigramsDict.search(list).sum().toFloat()
    }

    interface Candidate: CandidateView.Candidate {
        val score: Float
    }

    data class SingleCandidate(
        override val text: CharSequence,
        override val score: Float
    ): Candidate

    data class CompoundCandidate(
        val candidates: List<Candidate>,
        val bigramScore: Float
    ): Candidate {
        override val text: CharSequence = candidates.joinToString("") { it.text }
        private val averageScore: Float = candidates.sumOf { it.score.toDouble() }.toFloat() / candidates.size
        override val score: Float = (averageScore + bigramScore) / 2 * text.length
    }

    companion object {
        const val DEPTH = 3
        const val THRESHOLD = 3
    }
}