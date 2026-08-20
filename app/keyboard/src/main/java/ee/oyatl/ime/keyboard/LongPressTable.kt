package ee.oyatl.ime.keyboard

class LongPressTable(
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
}
