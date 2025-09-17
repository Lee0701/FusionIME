package ee.oyatl.ime.fusion.korean

import android.content.Context
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.dictionary.manager.DictionaryCache
import ee.oyatl.ime.newdict.DiskHanjaDictionary
import ee.oyatl.ime.newdict.DiskNGramDictionary
import ee.oyatl.ime.newdict.DiskTrieDictionary

class BigramHanjaConverter(
    context: Context
): HanjaConverter {
    private val indexDictId = R.raw.hanja_index
    private val vocabDictId = R.raw.hanja_content
    private val bigramDictId = R.raw.hanja_bigram
    private val indexDict: DiskTrieDictionary = DictionaryCache.get(indexDictId) {
        DiskTrieDictionary(context.resources.openRawResource(indexDictId))
    }
    private val vocabDict: DiskHanjaDictionary = DictionaryCache.get(vocabDictId) {
        DiskHanjaDictionary(context.resources.openRawResource(vocabDictId))
    }
    private val bigramDict: DiskNGramDictionary = DictionaryCache.get(bigramDictId) {
        DiskNGramDictionary(context.resources.openRawResource(bigramDictId))
    }

    override fun convert(text: String): List<CandidateView.Candidate> {
        return convert(CompoundCandidate(listOf()), text, 0)
            .sortedByDescending { it.score }
            .distinctBy { it.text }
            .sortedByDescending { it.text.count { c -> c.code in 0x4e00 .. 0x9fff } }
            .sortedBy { it.candidates.size }
            .sortedByDescending { it.text.length }
            .filter { it.text.isNotEmpty() }
    }

    fun convert(context: CompoundCandidate, text: String, depth: Int): List<CompoundCandidate> {
        if(text.isEmpty() || depth > 4) return listOf(context)
        val current = (1 .. text.length)
            .flatMap { l -> indexDict.get(text.take(l)) }
            .map { it to vocabDict.get(it) }
            .map { (id, vocab) -> SingleCandidate(id, vocab.hanja, vocab.frequency.toFloat(), vocab.extra) }
        val available = if(depth == 0) {
            current
        } else {
            val bigramResult = bigramDict.get(listOf(context.candidates.last().id))
            current.filter { it.id in bigramResult }
        }
        if(available.isEmpty()) return listOf(context)
        return available.flatMap { word ->
            convert(CompoundCandidate(listOf(word)), text.drop(word.text.length), depth + 1)
                .map { context.copy(candidates = context.candidates + it.candidates) }
        }
    }

    data class CompoundCandidate(
        val candidates: List<SingleCandidate>
    ): CandidateView.Candidate, CandidateView.ExtraCandidate {
        override val text: CharSequence = candidates.joinToString("") { it.text }
        val score: Float = candidates.map { it.score }.sum() / candidates.size
        override val extra: CharSequence = if(candidates.size == 1) candidates.first().extra else ""
    }

    data class SingleCandidate(
        val id: Int,
        override val text: CharSequence,
        val score: Float,
        override val extra: String
    ): CandidateView.Candidate, CandidateView.ExtraCandidate
}