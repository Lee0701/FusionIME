package ee.oyatl.ime.fusion.korean

import android.content.Context
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.newdict.DiskHanjaDictionary
import ee.oyatl.ime.newdict.DiskNGramDictionary
import ee.oyatl.ime.newdict.DiskTrieDictionary
import kotlin.math.pow

class DefaultHanjaConverter(
    context: Context
): HanjaConverter {
    private val indexDict: DiskTrieDictionary =
        DiskTrieDictionary(context.resources.openRawResource(R.raw.hanja_index))
    private val vocabDict: DiskHanjaDictionary =
        DiskHanjaDictionary(context.resources.openRawResource(R.raw.hanja_content))
    private val bigramDict: DiskNGramDictionary =
        DiskNGramDictionary(context.resources.openRawResource(R.raw.hanja_bigram))

    override fun convert(text: String): List<CandidateView.Candidate> {
        return convert(CompoundCandidate(listOf()), text)
            .sortedByDescending { it.score }
            .distinctBy { it.text }
            .sortedByDescending { it.text.length }
    }

    fun convert(context: CompoundCandidate, text: String): List<CompoundCandidate> {
        if(text.isEmpty()) return listOf(context)
        val current = (1 .. text.length)
            .flatMap { l -> indexDict.get(text.take(l)) }
            .map { it to vocabDict.get(it) }
            .map { (id, vocab) -> SingleCandidate(id, vocab.hanja, vocab.frequency.toFloat()) }
        val available = if(context.candidates.isEmpty()) {
            current
        } else {
            val bigramResult = bigramDict.get(listOf(context.candidates.last().id))
            current.filter { it.id in bigramResult }
        }
        return available.flatMap { word ->
            convert(CompoundCandidate(listOf(word)), text.drop(word.text.length))
                .map { context.copy(candidates = context.candidates + it.candidates) }
        }
    }

    data class CompoundCandidate(
        val candidates: List<SingleCandidate>
    ): CandidateView.Candidate {
        override val text: CharSequence = candidates.joinToString("") { it.text }
        val score: Float = candidates.map { it.score }.sum() / 2f.pow(candidates.size)
    }

    data class SingleCandidate(
        val id: Int,
        override val text: CharSequence,
        val score: Float,
    ): CandidateView.Candidate

    data class Candidate(
        override val text: CharSequence,
        val score: Float,
        override val extra: CharSequence = ""
    ): CandidateView.Candidate, CandidateView.ExtraCandidate
}