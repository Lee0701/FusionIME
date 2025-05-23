package ee.oyatl.ime.viet

import android.content.Context
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.dictionary.DiskDictionary

class HanNomConverter(
    context: Context
) {
    private val dictionary: DiskDictionary = DiskDictionary(context.resources.openRawResource(R.raw.viet))

    fun convert(text: String, mode: String): List<CandidateView.Candidate> {
        val result = (1 .. text.length).asSequence()
            .map { l ->
                dictionary.search(text.take(l))
                    .filter { it.extra == mode }
                    .map { Candidate(text.take(l), it.result, it.frequency.toFloat()) }
            }
            .flatten()
            .sortedByDescending { it.score }
            .sortedByDescending { it.key.length }
            .distinctBy { it.text }.toList()
        return result
    }

    data class Candidate(
        val key: CharSequence,
        override val text: CharSequence,
        val score: Float
    ): CandidateView.Candidate
}