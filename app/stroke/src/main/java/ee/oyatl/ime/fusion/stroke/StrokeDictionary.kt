package ee.oyatl.ime.fusion.stroke

import ee.oyatl.ime.newdict.DiskTrieDictionary
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Looks up characters by stroke sequence in the trie written by GenStrokeDict,
 * whose keys are stroke digits '1' to '5' and whose entries are code points.
 */
class StrokeDictionary(
    sequences: InputStream,
    private val ranking: StrokeRanking
) {
    private val data: ByteBuffer = DiskTrieDictionary(sequences).data
    private val root: Int = data.getInt(data.capacity() - 4)

    /**
     * Characters for [strokes], which may contain [Stroke.WILDCARD].
     * Characters whose whole sequence matches come first, then those whose sequence
     * continues; each group is ordered by commonness in the chosen script.
     */
    fun search(strokes: String, traditional: Boolean, limit: Int = DEFAULT_LIMIT): List<Int> {
        if(strokes.isEmpty()) return emptyList()
        val nodes = matchNodes(strokes)

        val exact = HashSet<Int>()
        nodes.forEach { node -> forEachEntry(node) { exact += it } }

        // Fewest extra strokes for each character that continues past the input.
        val continued = HashMap<Int, Int>()
        nodes.forEach { node ->
            forEachDescendant(node) { codePoint, depth ->
                if(codePoint !in exact) {
                    val known = continued[codePoint]
                    if(known == null || depth < known) continued[codePoint] = depth
                }
            }
        }

        val result = ArrayList<Int>(minOf(limit, exact.size + continued.size))
        sorted(exact.map { sortKey(it, 0, traditional) }, result, limit)
        sorted(continued.map { (codePoint, depth) -> sortKey(codePoint, depth, traditional) }, result, limit)
        return result
    }

    private fun sorted(keys: List<Long>, result: MutableList<Int>, limit: Int) {
        val array = keys.toLongArray()
        array.sort()
        for(key in array) {
            if(result.size >= limit) return
            result += (key and CODE_POINT_MASK).toInt()
        }
    }

    // Packs the order into one number: script group, rank, extra strokes, then code point.
    private fun sortKey(codePoint: Int, depth: Int, traditional: Boolean): Long {
        val group = ranking.group(codePoint, traditional).toLong()
        val rank = ranking.rank(codePoint, traditional).toLong().coerceAtMost(RANK_MASK)
        val extra = depth.toLong().coerceAtMost(DEPTH_MASK)
        return (group shl 57) or (rank shl 29) or (extra shl 21) or codePoint.toLong()
    }

    // Nodes reached by the whole pattern, branching on wildcards.
    private fun matchNodes(pattern: String): List<Int> {
        var nodes = listOf(root)
        for(c in pattern) {
            val next = ArrayList<Int>()
            nodes.forEach { node ->
                val count = data.getShort(node).toInt()
                for(i in 0 until count) {
                    val offset = node + 2 + i * 6
                    if(c == Stroke.WILDCARD || data.getChar(offset) == c) next += data.getInt(offset + 2)
                }
            }
            if(next.isEmpty()) return emptyList()
            nodes = next
        }
        return nodes
    }

    // Node layout: child count (short), (char, address) per child, entry count (short), entries.
    private inline fun forEachEntry(node: Int, action: (Int) -> Unit) {
        val p = node + 2 + data.getShort(node) * 6
        val count = data.getShort(p).toInt()
        for(i in 0 until count) action(data.getInt(p + 2 + i * 4))
    }

    private fun forEachDescendant(node: Int, action: (codePoint: Int, depth: Int) -> Unit) {
        var nodes = IntArray(64)
        var depths = IntArray(64)
        var size = 0
        fun push(address: Int, depth: Int) {
            if(size == nodes.size) {
                nodes = nodes.copyOf(size * 2)
                depths = depths.copyOf(size * 2)
            }
            nodes[size] = address
            depths[size] = depth
            size++
        }
        fun pushChildren(p: Int, depth: Int) {
            val count = data.getShort(p).toInt()
            for(i in 0 until count) push(data.getInt(p + 2 + i * 6 + 2), depth)
        }
        pushChildren(node, 1)
        while(size > 0) {
            size--
            val p = nodes[size]
            val depth = depths[size]
            forEachEntry(p) { action(it, depth) }
            pushChildren(p, depth + 1)
        }
    }

    companion object {
        const val DEFAULT_LIMIT = 500
        private const val CODE_POINT_MASK = 0x1fffffL
        private const val DEPTH_MASK = 0xffL
        private const val RANK_MASK = 0xfffffffL
    }
}
