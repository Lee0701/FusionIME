package ee.oyatl.ime.fusion.stroke.host

import ee.oyatl.ime.newdict.TrieDictionary
import java.io.DataOutputStream
import java.io.File

/**
 * Conway Stroke Data, parsed from the text files of
 * <https://github.com/stroke-input/stroke-input-data>.
 */
class StrokeData(
    /** Stroke sequence (digits 1 to 5) to code points, in file order. */
    val sequences: Map<String, List<Int>>,
    /** Code point to rank in each ranking file, 0 being the most common. */
    val simplifiedRanks: Map<Int, Int>,
    val traditionalRanks: Map<Int, Int>,
    val traditionalOnly: Set<Int>,
    val simplifiedOnly: Set<Int>,
    val unlikelyFontSupport: Set<Int>
) {
    fun writeSequences(file: File) {
        val trie = TrieDictionary()
        sequences.forEach { (sequence, codePoints) ->
            trie.insert(sequence, codePoints.sortedWith(neutralOrder))
        }
        DataOutputStream(file.outputStream().buffered()).use { trie.write(it) }
    }

    /**
     * Writes one record per character that has a rank or a flag:
     * code point, simplified rank, traditional rank (-1 if unranked) and flags.
     */
    fun writeRanking(file: File) {
        val codePoints = (simplifiedRanks.keys + traditionalRanks.keys +
                traditionalOnly + simplifiedOnly + unlikelyFontSupport).sorted()
        DataOutputStream(file.outputStream().buffered()).use { os ->
            os.writeInt(RANKING_VERSION)
            os.writeInt(codePoints.size)
            codePoints.forEach { codePoint ->
                os.writeInt(codePoint)
                os.writeInt(simplifiedRanks[codePoint] ?: -1)
                os.writeInt(traditionalRanks[codePoint] ?: -1)
                os.writeInt(flagsOf(codePoint))
            }
        }
    }

    fun flagsOf(codePoint: Int): Int {
        var flags = 0
        if(codePoint in traditionalOnly) flags = flags or FLAG_TRADITIONAL_ONLY
        if(codePoint in simplifiedOnly) flags = flags or FLAG_SIMPLIFIED_ONLY
        if(codePoint in unlikelyFontSupport) flags = flags or FLAG_UNLIKELY_FONT_SUPPORT
        return flags
    }

    // Order used inside each trie entry, before the input mode re-sorts for its script.
    private val neutralOrder: Comparator<Int> = compareBy(
        { minOf(simplifiedRanks[it] ?: Int.MAX_VALUE, traditionalRanks[it] ?: Int.MAX_VALUE) },
        { it }
    )

    companion object {
        const val RANKING_VERSION = 1
        const val FLAG_TRADITIONAL_ONLY = 1
        const val FLAG_SIMPLIFIED_ONLY = 2
        const val FLAG_UNLIKELY_FONT_SUPPORT = 4

        fun load(dir: File): StrokeData = StrokeData(
            sequences = parseSequences(File(dir, "sequence-characters.txt").readLines()),
            simplifiedRanks = parseRanking(File(dir, "ranking-simplified.txt").readLines()),
            traditionalRanks = parseRanking(File(dir, "ranking-traditional.txt").readLines()),
            traditionalOnly = parseCharacters(File(dir, "characters-traditional.txt").readLines()),
            simplifiedOnly = parseCharacters(File(dir, "characters-simplified.txt").readLines()),
            unlikelyFontSupport = parseCharacters(File(dir, "characters-unlikely.txt").readLines())
        )

        fun parseSequences(lines: List<String>): Map<String, List<Int>> =
            dataLines(lines).associate { line ->
                val (sequence, characters) = line.split('\t')
                require(sequence.all { it in '1'..'5' }) { "Bad stroke sequence: $sequence" }
                sequence to characters.codePoints().toArray().toList()
            }

        // Characters ranked by their first appearance, across all lines.
        fun parseRanking(lines: List<String>): Map<Int, Int> {
            val ranks = LinkedHashMap<Int, Int>()
            dataLines(lines).forEach { line ->
                line.codePoints().forEach { codePoint ->
                    if(!Character.isWhitespace(codePoint)) ranks.putIfAbsent(codePoint, ranks.size)
                }
            }
            return ranks
        }

        fun parseCharacters(lines: List<String>): Set<Int> =
            dataLines(lines).flatMap { it.codePoints().toArray().toList() }
                .filterNot(Character::isWhitespace)
                .toSet()

        private fun dataLines(lines: List<String>): List<String> =
            lines.map { it.trimEnd() }.filter { it.isNotEmpty() && !it.startsWith('#') }
    }
}
