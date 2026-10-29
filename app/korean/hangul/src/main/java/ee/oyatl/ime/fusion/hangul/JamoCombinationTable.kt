package ee.oyatl.ime.fusion.hangul

import kotlinx.serialization.Serializable

@Serializable
data class JamoCombinationTable(
    val map: Map<Long, Int> = emptyMap()
) {
    operator fun get(key: Pair<Int, Int>): Int? = map[encodeKey(key)]

    operator fun plus(other: JamoCombinationTable): JamoCombinationTable {
        return JamoCombinationTable(map + other.map)
    }

    companion object {
        fun from(
            map: Map<Pair<Int, Int>, Int>
        ): JamoCombinationTable {
            return JamoCombinationTable(map.mapKeys { (key, _) -> encodeKey(key) }.toMap())
        }

        private fun encodeKey(key: Pair<Int, Int>): Long {
            return (key.first.toLong() shl Int.SIZE_BITS) or key.second.toLong()
        }
    }
}