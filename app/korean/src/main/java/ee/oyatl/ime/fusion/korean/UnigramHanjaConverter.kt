package ee.oyatl.ime.fusion.korean

import android.content.Context
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.dictionary.manager.DictionaryCache
import ee.oyatl.ime.newdict.CachedDiskTrieDictionary
import ee.oyatl.ime.newdict.DiskHanjaDictionary
import ee.oyatl.ime.newdict.DiskTrieDictionary
import kotlinx.coroutines.yield

class UnigramHanjaConverter(
    val useCache: Boolean = true
): HanjaConverter {
    private var isLoaded: Boolean = false
    private lateinit var indexDict: DiskTrieDictionary
    private lateinit var vocabDict: DiskHanjaDictionary

    override fun load(context: Context) {
        val indexDictId = R.raw.hanja_index
        val vocabDictId = R.raw.hanja_content
        indexDict = DictionaryCache.get(indexDictId) {
            context.resources.openRawResource(indexDictId).use {
                if(useCache) CachedDiskTrieDictionary(it)
                else DiskTrieDictionary(it)
            }
        }
        vocabDict = DictionaryCache.get(vocabDictId) {
            context.resources.openRawResource(vocabDictId).use {
                DiskHanjaDictionary(it)
            }
        }
        isLoaded = true
    }

    override fun reset() {
        if(!isLoaded) return
        if(useCache) {
            val indexDict = this.indexDict
            if(indexDict is CachedDiskTrieDictionary) indexDict.clearCache()
        }
    }

    override suspend fun convert(text: String): List<CandidateView.Candidate> {
        val hanjaResult = (1..text.length).flatMap { l ->
            yield()
            indexDict.get(text.take(l))
                .map { vocabDict.get(it) }
                .filter { it.hanja.length == l }
                .map { Candidate(it.hanja, it.frequency.toFloat(), it.extra) }
        }
        yield()
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