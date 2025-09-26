package ee.oyatl.ime.fusion.korean

import android.content.Context
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.dictionary.manager.DictionaryCache
import ee.oyatl.ime.newdict.DiskHanjaDictionary
import ee.oyatl.ime.newdict.DiskTrieDictionary

class UnigramHanjaConverter: HanjaConverter {
    private lateinit var indexDict: DiskTrieDictionary
    private lateinit var vocabDict: DiskHanjaDictionary

    override fun load(context: Context) {
        val indexDictId = R.raw.hanja_index
        val vocabDictId = R.raw.hanja_content
        indexDict = DictionaryCache.get(indexDictId) {
            DiskTrieDictionary(context.resources.openRawResource(indexDictId))
        }
        vocabDict = DictionaryCache.get(vocabDictId) {
            DiskHanjaDictionary(context.resources.openRawResource(vocabDictId))
        }
    }

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