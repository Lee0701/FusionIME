package ee.oyatl.ime.keyboard

import android.view.KeyEvent

class DefaultKeyboardInflater(
    override val keyboardParams: KeyboardParams
): KeyboardInflater {
    override fun inflate(
        configuration: KeyboardConfiguration,
        contentRows: List<String>,
        softKeyCodeMapper: SoftKeyCodeMapper
    ): DefaultKeyboard {
        val keyCodeRows = contentRows.map { row -> row.map { SoftKeyCodeMapper.keyCharToKeyCode(it) } }
        val result = mutableListOf<List<Keyboard.KeyItem>>()
        configuration.rows.forEach { row ->
            val resultRow = mutableListOf<Keyboard.KeyItem>()
            row.forEach { item ->
                when(item) {
                    is KeyboardConfiguration.Item.ContentKey -> {
                        val rowIndex = keyCodeRows.size - item.rowId - 1
                        val keyCode = softKeyCodeMapper[keyCodeRows[rowIndex][item.index]]
                        resultRow += Keyboard.KeyItem.NormalKey(keyCode)
                    }
                    is KeyboardConfiguration.Item.ContentRow -> {
                        val rowIndex = keyCodeRows.size - item.rowId - 1
                        val content = keyCodeRows[rowIndex].map {
                            if(it == KeyEvent.KEYCODE_SPACE) {
                                Keyboard.KeyItem.SplitSpacer(keyboardParams.splitWidth)
                            } else {
                                val keyCode = softKeyCodeMapper[it]
                                Keyboard.KeyItem.NormalKey(keyCode)
                            }
                        }
                        resultRow += content
                    }
                    is KeyboardConfiguration.Item.Spacer -> {
                        resultRow += Keyboard.KeyItem.Spacer(item.width)
                    }
                    is KeyboardConfiguration.Item.TemplateKey -> {
                        val keyCode = softKeyCodeMapper[item.keyCode]
                        if(keyboardParams.splitWidth != 0 && keyCode == KeyEvent.KEYCODE_SPACE) {
                            resultRow += inflateKey(keyCode, item.copy(width = item.width / 2))
                            resultRow += Keyboard.KeyItem.SplitSpacer(keyboardParams.splitWidth)
                            resultRow += inflateKey(keyCode, item.copy(width = item.width / 2))
                        } else {
                            resultRow += inflateKey(keyCode, item)
                        }
                    }
                }
            }
            result += resultRow
        }
        return DefaultKeyboard(result, keyboardParams)
    }

    private fun inflateKey(keyCode: Int, item: KeyboardConfiguration.Item.TemplateKey): Keyboard.KeyItem {
        val result =
            if(item.special) Keyboard.KeyItem.SpecialKey(keyCode, item.width)
            else Keyboard.KeyItem.NormalKey(keyCode, item.width)
        return result
    }
}