package ee.oyatl.ime.fusion.keyboard.core

import ee.oyatl.ime.fusion.hangul.JamoCombinationTable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KeyboardLayoutPreset(
    val keyboardTemplate: KeyboardTemplate,
    val layoutTable: LayoutTable = LayoutTable(),
    val altGraphTable: LayoutTable = LayoutTable(),
    val longPressTable: LongPressTable = LongPressTable(),
    val softKeyCodeMapper: SoftKeyCodeMapper = SoftKeyCodeMapper.Empty,
    val hardKeyCodeMapper: HardKeyCodeMapper = HardKeyCodeMapper(),
    val extra: Extra = Extra.None
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