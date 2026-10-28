package ee.oyatl.ime.fusion.keyboard.core

import kotlinx.serialization.Serializable

@Serializable
data class KeyboardLayoutPreset(
    val keyboardTemplate: KeyboardTemplate,
    val layoutTable: LayoutTable = LayoutTable(),
    val altGraphTable: LayoutTable? = null,
    val longPressTable: LongPressTable = LongPressTable(),
    val softKeyCodeMapper: SoftKeyCodeMapper = SoftKeyCodeMapper.Empty,
    val hardKeyCodeMapper: HardKeyCodeMapper = HardKeyCodeMapper()
)