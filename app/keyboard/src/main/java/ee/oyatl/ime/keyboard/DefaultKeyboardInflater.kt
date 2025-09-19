package ee.oyatl.ime.keyboard

class DefaultKeyboardInflater(
    override val keyboardParams: KeyboardParams
): KeyboardInflater {
    override fun inflate(
        configuration: KeyboardConfiguration,
        contentRows: List<String>,
        keyCodeMapper: KeyCodeMapper
    ): DefaultKeyboard {
        val keyCodeRows = contentRows.map { row -> row.map { KeyCodeMapper.keyCharToKeyCode(it) } }
        val result = mutableListOf<List<Keyboard.KeyItem>>()
        configuration.rows.forEach { row ->
            val resultRow = mutableListOf<Keyboard.KeyItem>()
            row.forEach { item ->
                when(item) {
                    is KeyboardConfiguration.Item.ContentKey -> {
                        val rowIndex = keyCodeRows.size - item.rowId - 1
                        val keyCode = keyCodeMapper[keyCodeRows[rowIndex][item.index]]
                        resultRow += Keyboard.KeyItem.NormalKey(keyCode)
                    }
                    is KeyboardConfiguration.Item.ContentRow -> {
                        val rowIndex = keyCodeRows.size - item.rowId - 1
                        val content = keyCodeRows[rowIndex].map {
                            val keyCode = keyCodeMapper[it]
                            Keyboard.KeyItem.NormalKey(keyCode)
                        }
                        resultRow += content
                    }
                    is KeyboardConfiguration.Item.Spacer -> {
                        resultRow += Keyboard.KeyItem.Spacer(item.width)
                    }
                    is KeyboardConfiguration.Item.TemplateKey -> {
                        val keyCode = keyCodeMapper[item.keyCode]
                        resultRow +=
                            if(item.special) Keyboard.KeyItem.SpecialKey(keyCode, item.width)
                            else Keyboard.KeyItem.NormalKey(keyCode, item.width)
                    }
                }
            }
            result += resultRow
        }
        return DefaultKeyboard(result, keyboardParams)
    }
}