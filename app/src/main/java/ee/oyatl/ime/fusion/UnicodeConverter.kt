package ee.oyatl.ime.fusion

import ee.oyatl.ime.candidate.CandidateView
import java.nio.ByteBuffer

class UnicodeConverter(
    val prefix: String
) {
    fun convert(text: String): List<Candidate> {
        if(!text.lowercase().startsWith(prefix.lowercase())) return emptyList()
        val codeString = text.drop(prefix.length)
        if(codeString.length < 4) return emptyList()
        val charCode = codeString.toIntOrNull(16) ?: return emptyList()
        if(charCode !in 0x0000 .. 0x10ffff) return emptyList()
        val bytes = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(charCode).array()
        val string = String(bytes, Charsets.UTF_32)
        val unicodeString = "U+" + charCode.toString(16).padStart(4, '0').uppercase()
        return listOf(Candidate(string, unicodeString))
    }

    data class Candidate(
        override val text: CharSequence,
        override val extra: CharSequence
    ): CandidateView.Candidate, CandidateView.ExtraCandidate
}