package ee.oyatl.ime.fusion.keyboard.core

import ee.oyatl.ime.fusion.hangul.JamoCombinationTable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class KeyboardLayout(
    val params: KeyboardParams,
    val keyboard: Keyboard,
    val layoutTable: LayoutTable,
    val altGraphTable: LayoutTable,
    val moreKeysTable: MoreKeysTable,
    val softKeyCodeMapper: KeyCodeMapper,
    val hardKeyCodeMapper: KeyCodeMapper,
    val keyLabelTable: KeyLabelTable,
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
            val jamoCombinationTable: JamoCombinationTable
        ): Extra
    }
}