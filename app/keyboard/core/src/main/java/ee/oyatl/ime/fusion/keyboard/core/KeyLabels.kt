package ee.oyatl.ime.fusion.keyboard.core

import kotlinx.serialization.Serializable

@Serializable
data class KeyLabels(
    val map: Map<Int, KeyLabel> = emptyMap()
)