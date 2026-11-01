package ee.oyatl.ime.fusion.keyboard.core.table

import kotlinx.serialization.Serializable

@Serializable
data class KeyLabelTable(
    val map: Map<Int, KeyLabel> = emptyMap()
) {
    operator fun get(keyCode: Int): KeyLabel? {
        return map[keyCode]
    }
}