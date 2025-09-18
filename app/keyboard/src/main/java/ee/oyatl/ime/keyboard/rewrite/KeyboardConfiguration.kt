package ee.oyatl.ime.keyboard.rewrite

data class KeyboardConfiguration(
    val rows: List<List<Item>>
) {
    sealed interface Item {
        data class ContentRow(
            val rowId: Int
        ): Item
        data class TemplateKey(
            val key: Keyboard.KeyItem
        ): Item
    }
}