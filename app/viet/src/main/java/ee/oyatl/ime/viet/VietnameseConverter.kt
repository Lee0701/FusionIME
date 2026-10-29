package ee.oyatl.ime.viet

import android.content.Context
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.dictionary.DiskDictionary

class VietnameseConverter(
    context: Context,
    dictResdId: Int
) {
    private val dictionary: DiskDictionary = DiskDictionary(context.resources.openRawResource(dictResdId))

    fun convert(text: String): List<CandidateView.Candidate> {
        val result = (1 .. text.length).asSequence()
            .flatMap { l ->
                val key = text.take(l)
                dictionary.search(key.replace(" ", ""))
                    .map { Candidate(key, it.result, it.frequency.toFloat()) }
            }
            .sortedByDescending { it.score }
            .sortedByDescending { it.src.length }
            .distinctBy { it.text }.toList()
        return result
    }

    data class Candidate(
        val src: String,
        override val text: CharSequence,
        val score: Float
    ): CandidateView.Candidate
}