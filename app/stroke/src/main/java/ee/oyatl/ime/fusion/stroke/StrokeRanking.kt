package ee.oyatl.ime.fusion.stroke

import java.io.DataInputStream
import java.io.InputStream

/**
 * Character rankings and script flags, read from the table written by GenStrokeDict.
 */
class StrokeRanking(
    private val entries: Map<Int, Entry>
) {
    data class Entry(
        val simplifiedRank: Int,
        val traditionalRank: Int,
        val flags: Int
    )

    /** Rank among common characters of the script, or [Int.MAX_VALUE] if unranked. */
    fun rank(codePoint: Int, traditional: Boolean): Int {
        val entry = entries[codePoint] ?: return Int.MAX_VALUE
        val rank = if(traditional) entry.traditionalRank else entry.simplifiedRank
        return if(rank < 0) Int.MAX_VALUE else rank
    }

    /**
     * 0 for characters of the script, 1 for characters of the other script only,
     * 2 for characters that may not render on older devices.
     */
    fun group(codePoint: Int, traditional: Boolean): Int {
        val flags = entries[codePoint]?.flags ?: return 0
        if(flags and FLAG_UNLIKELY_FONT_SUPPORT != 0) return 2
        val otherScriptOnly = if(traditional) FLAG_SIMPLIFIED_ONLY else FLAG_TRADITIONAL_ONLY
        if(flags and otherScriptOnly != 0) return 1
        return 0
    }

    companion object {
        private const val VERSION = 1
        const val FLAG_TRADITIONAL_ONLY = 1
        const val FLAG_SIMPLIFIED_ONLY = 2
        const val FLAG_UNLIKELY_FONT_SUPPORT = 4

        fun read(input: InputStream): StrokeRanking {
            val dis = DataInputStream(input.buffered())
            val version = dis.readInt()
            require(version == VERSION) { "Unsupported stroke ranking version $version" }
            val count = dis.readInt()
            val entries = HashMap<Int, Entry>(count * 2)
            repeat(count) {
                val codePoint = dis.readInt()
                entries[codePoint] = Entry(dis.readInt(), dis.readInt(), dis.readInt())
            }
            return StrokeRanking(entries)
        }
    }
}
