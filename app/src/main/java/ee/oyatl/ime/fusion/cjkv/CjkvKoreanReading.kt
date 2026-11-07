package ee.oyatl.ime.fusion.cjkv

/**
 * Lays out a character's Korean readings in one row: its hun/eum labels
 * (무거울 중), followed by the readings no label covers (了: 마칠 료 · 요).
 *
 * Each piece carries what tapping it types: the eum alone types the reading
 * (중), the hun types the whole label (무거울 중).
 */
internal object CjkvKoreanReading {
    data class Segment(val text: String, val typed: String? = null)

    /**
     * [hunEum] is the stored label list: labels joined by " · ", each label
     * possibly listing several meanings joined by ", " (마칠 료, 깨달을 료).
     * [readings] are the bare eum readings (료 요).
     */
    fun segments(hunEum: String?, readings: List<String>): List<Segment> {
        val labels = hunEum?.split(" · ")?.filter(String::isNotBlank).orEmpty()
        val segments = mutableListOf<Segment>()
        val covered = mutableSetOf<String>()
        labels.forEachIndexed { index, label ->
            if(index > 0) segments += Segment(" · ")
            label.split(", ").filter(String::isNotBlank).forEachIndexed { partIndex, part ->
                if(partIndex > 0) segments += Segment(", ")
                segments += splitLabel(part.trim(), covered)
            }
        }
        readings.filter { it.isNotBlank() && it !in covered }.distinct().forEach { reading ->
            if(segments.isNotEmpty()) segments += Segment(" · ")
            segments += Segment(reading, reading)
        }
        return segments
    }

    private fun splitLabel(label: String, covered: MutableSet<String>): List<Segment> {
        val space = label.lastIndexOf(' ')
        val eum = if(space > 0) label.substring(space + 1) else ""
        // Labels without a trailing one-syllable reading (땅이름, 竝과 同字) are typed whole.
        if(space <= 0 || !isHangulSyllable(eum)) return listOf(Segment(label, label))
        covered += eum
        return listOf(
            Segment(label.substring(0, space), label),
            Segment(" "),
            Segment(eum, eum),
        )
    }

    private fun isHangulSyllable(text: String): Boolean = text.length == 1 && text[0] in '가'..'힣'
}
