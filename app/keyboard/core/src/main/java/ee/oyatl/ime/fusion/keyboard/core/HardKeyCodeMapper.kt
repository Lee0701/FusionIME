package ee.oyatl.ime.fusion.keyboard.core

import kotlinx.serialization.Serializable

@Serializable
data class HardKeyCodeMapper(
    private val map: Map<Int, Int> = emptyMap()
) {
    operator fun get(keyCode: Int): Int {
        return map[keyCode] ?: keyCode
    }
}