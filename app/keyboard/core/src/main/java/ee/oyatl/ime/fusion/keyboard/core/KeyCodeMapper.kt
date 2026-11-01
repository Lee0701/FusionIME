package ee.oyatl.ime.fusion.keyboard.core

import kotlinx.serialization.Serializable

@Serializable
data class KeyCodeMapper(
    private val map: Map<Int, Int> = emptyMap()
) {
    operator fun get(keyCode: Int): Int {
        return map[keyCode] ?: keyCode
    }

    @Serializable
    data class ByScreenMode(
        val mobile: KeyCodeMapper = KeyCodeMapper(),
        val tablet: KeyCodeMapper = mobile,
        val full: KeyCodeMapper = tablet
    )
}