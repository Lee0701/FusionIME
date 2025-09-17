package ee.oyatl.ime.fusion.korean

import android.content.Context
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.dictionary.DiskDictionary

class JeongUnHanjaConverter: HanjaConverter {
    private lateinit var dict: DiskDictionary

    override fun load(context: Context) {
        dict = DiskDictionary(context.resources.openRawResource(R.raw.jeongun))
    }

    override fun convert(text: String): List<CandidateView.Candidate> {
        val result = (1 .. text.length).asSequence()
            .flatMap { l ->
                dict.search(text.take(l))
                    .map { Candidate(it.result, l) }
            }
            .toList()
        val maxLength = result.maxOfOrNull { it.inputLength } ?: 0
        return result.filter { it.inputLength == maxLength }
    }
    data class Candidate(
        override val text: CharSequence,
        override val inputLength: Int
    ): CandidateView.Candidate, CandidateView.VarLengthCandidate
}