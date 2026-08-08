package ee.oyatl.ime.viet

import android.content.Context
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.dictionary.DiskDictionary

class VietnameseConverter(
    context: Context,
    dictResdId: Int
) {
    private val dictionary: DiskDictionary = DiskDictionary(context.resources.openRawResource(dictResdId))

    fun convert(src: ChuQuocNguTableConverter.Result): List<CandidateView.Candidate> {
        val text = src.values.joinToString("")
        val result = (1 .. text.length).asSequence()
            .flatMap { l ->
                dictionary.search(text.take(l))
                    .map { Candidate(src, l, it.result, it.frequency.toFloat()) }
            }
            .sortedByDescending { it.score }
            .sortedByDescending { it.keyLength }
            .distinctBy { it.text }.toList()
        return result
    }

    data class Candidate(
        val src: ChuQuocNguTableConverter.Result,
        val keyLength: Int,
        override val text: CharSequence,
        val score: Float
    ): CandidateView.Candidate
}