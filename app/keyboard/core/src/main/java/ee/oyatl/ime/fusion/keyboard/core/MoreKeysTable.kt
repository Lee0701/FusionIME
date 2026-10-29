package ee.oyatl.ime.fusion.keyboard.core

import kotlinx.serialization.Serializable

@Serializable
data class MoreKeysTable(
    private val map: Map<Int, List<Int>> = emptyMap(),
    private val uppercaseOverrides: Map<Int, Int> = emptyMap()
) {
    fun candidatesFor(baseCodePoint: Int): List<Int> {
        map[baseCodePoint]?.let { return it }
        if(!Character.isUpperCase(baseCodePoint)) return emptyList()

        val lowercase = Character.toLowerCase(baseCodePoint)
        return map[lowercase].orEmpty().map { candidate ->
            uppercaseOverrides[candidate] ?: Character.toUpperCase(candidate)
        }
    }

    operator fun plus(other: MoreKeysTable): MoreKeysTable {
        return MoreKeysTable(
            map = this.map + other.map,
            uppercaseOverrides = this.uppercaseOverrides + other.uppercaseOverrides
        )
    }

    companion object {
        fun from(
            map: Map<Char, List<Char>> = emptyMap(),
            uppercaseOverrides: Map<Char, Char> = emptyMap()
        ): MoreKeysTable = MoreKeysTable(
            map.map { (k, v) -> k.code to v.map(Char::code) }.toMap(),
            uppercaseOverrides.map { (k, v) -> k.code to v.code }.toMap()
        )
    }
}
