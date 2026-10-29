package ee.oyatl.ime.fusion.keyboard.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KeyboardLayoutPresetDef(
    val keyboardTemplate: String,
    val layoutTables: List<String>,
    val altGraphTables: List<String>,
    val longPressTables: List<String>,
    val softKeyCodeMapper: String,
    val hardKeyCodeMapper: String,
    val extra: Extra
) {
    @Serializable
    sealed interface Extra {
        @SerialName("none")
        @Serializable
        object None: Extra

        @SerialName("hangul")
        @Serializable
        data class Hangul(
            val jamoCombinationTables: List<String>
        ): Extra
    }
}
