package ee.oyatl.ime.fusion.keyboard.core.preset

import ee.oyatl.ime.fusion.keyboard.core.table.KeyCodeMapper
import ee.oyatl.ime.fusion.keyboard.core.table.KeyLabelTable
import ee.oyatl.ime.fusion.keyboard.core.table.LayoutTable
import ee.oyatl.ime.fusion.keyboard.core.table.MoreKeysTable
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