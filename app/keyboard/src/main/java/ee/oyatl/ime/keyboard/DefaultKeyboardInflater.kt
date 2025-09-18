package ee.oyatl.ime.keyboard

class DefaultKeyboardInflater: KeyboardInflater {
    override fun inflate(
        configuration: KeyboardConfiguration,
        contentRows: List<List<Int>>,
        params: KeyboardParams
    ): DefaultKeyboard {
        val result = mutableListOf<List<Keyboard.KeyItem>>()
        configuration.rows.forEach { row ->
            val resultRow = mutableListOf<Keyboard.KeyItem>()
            row.forEach { item ->
                when(item) {
                    is KeyboardConfiguration.Item.ContentRow -> {
                        val rowIndex = contentRows.size - item.rowId - 1
                        val content = contentRows[rowIndex].map { Keyboard.KeyItem.NormalKey(it) }
                        resultRow += content
                    }
                    is KeyboardConfiguration.Item.TemplateKey -> {
                        resultRow += item.key
                    }
                }
            }
            result += resultRow
        }
        return DefaultKeyboard(result, params)
    }
}