package ee.oyatl.ime.fusion

import ee.oyatl.ime.candidate.CandidateView
import java.nio.ByteBuffer

class UnicodeConverter(
    val prefix: String
) {
    fun convert(text: String): List<Candidate> {
        val prefix = prefix.lowercase()
        if(!text.lowercase().startsWith(prefix)) return emptyList()
        val codeStrings = text.lowercase().drop(prefix.length).split(prefix)
        val converted = codeStrings.map { convertCode(it) }
        if(converted.any { it == null }) return emptyList()
        val candidate = Candidate(
            text = converted.filterNotNull().joinToString("") { it.text },
            extra = converted.filterNotNull().joinToString(" ") { it.extra }
        )
        return listOf(candidate)
    }

    fun convertCode(codeString: String): Candidate? {
        if(codeString.length < 4) return null
        val charCode = codeString.toIntOrNull(16) ?: return null
        if(charCode !in 0x0000 .. 0x10ffff) return null
        val bytes = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(charCode).array()
        val string = String(bytes, Charsets.UTF_32)
        val unicodeString = "U+" + charCode.toString(16).padStart(4, '0').uppercase()
        return Candidate(string, unicodeString)
    }

    data class Candidate(
        override val text: CharSequence,
        override val extra: CharSequence
    ): CandidateView.Candidate, CandidateView.ExtraCandidate
}