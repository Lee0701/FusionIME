package ee.oyatl.ime.fusion.keyboard.core

import kotlinx.serialization.Serializable

@Serializable
data class KeyboardLayoutPreset(
    val keyboardTemplate: KeyboardTemplate = KeyboardTemplate.Basic(),
    val layoutTable: LayoutTable = LayoutTable(),
    val altGraphTable: LayoutTable = LayoutTable(),
    val moreKeysTable: MoreKeysTable = MoreKeysTable(),
    val softKeyCodeMapper: KeyCodeMapper.ByScreenMode = KeyCodeMapper.ByScreenMode(),
    val hardKeyCodeMapper: KeyCodeMapper = KeyCodeMapper(),
    val keyLabelTable: KeyLabelTable = KeyLabelTable(),
    val extra: KeyboardLayout.Extra = KeyboardLayout.Extra.None
)