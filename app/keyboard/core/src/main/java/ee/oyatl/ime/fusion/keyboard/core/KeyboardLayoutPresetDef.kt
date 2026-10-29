package ee.oyatl.ime.fusion.keyboard.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KeyboardLayoutPresetDef(
    val keyboardTemplate: String,
    val layoutTable: String,
    val altGraphTable: String,
    val moreKeysTable: String,
    val softKeyCodeMapper: String,
    val hardKeyCodeMapper: String,
    val keyLabels: String,
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
            val jamoCombinationTable: String
        ): Extra
    }
}
