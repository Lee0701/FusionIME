package ee.oyatl.ime.fusion.keyboard.core

class HardKeyCodeMapper(
    private val map: Map<Int, Int> = emptyMap()
) {
    operator fun get(keyCode: Int): Int {
        return map[keyCode] ?: keyCode
    }
}