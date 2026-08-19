package ee.oyatl.ime.fusion

import ee.oyatl.ime.keyboard.KeyboardTemplate
import ee.oyatl.ime.keyboard.LayoutTable
import ee.oyatl.ime.keyboard.SoftKeyCodeMapper

data class KeyboardLayoutPreset(
    val keyboardTemplate: KeyboardTemplate,
    val layoutTable: LayoutTable = LayoutTable(),
    val softKeyCodeMapper: SoftKeyCodeMapper = SoftKeyCodeMapper.Empty
)