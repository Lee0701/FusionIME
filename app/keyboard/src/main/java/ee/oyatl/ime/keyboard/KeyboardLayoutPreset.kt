package ee.oyatl.ime.keyboard

data class KeyboardLayoutPreset(
    val keyboardTemplate: KeyboardTemplate,
    val layoutTable: LayoutTable = LayoutTable(),
    val softKeyCodeMapper: SoftKeyCodeMapper = SoftKeyCodeMapper.Empty
)