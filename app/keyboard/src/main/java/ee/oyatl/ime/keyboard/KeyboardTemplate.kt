package ee.oyatl.ime.keyboard

import android.view.KeyEvent

interface KeyboardTemplate {
    fun inflate(params: KeyboardParams): Keyboard

    data class Basic(
        val configuration: KeyboardConfiguration,
        val contentRows: List<String>,
        val softKeyCodeMapper: SoftKeyCodeMapper = SoftKeyCodeMapper()
    ): KeyboardTemplate {
        override fun inflate(params: KeyboardParams): Keyboard {
            val keyCodeRows = contentRows.map { row -> row.map { SoftKeyCodeMapper.keyCharToKeyCode(it) } }
            val result = mutableListOf<List<Keyboard.KeyItem>>()
            configuration.rows.forEach { row ->
                val resultRow = mutableListOf<Keyboard.KeyItem>()
                row.forEach { item ->
                    when(item) {
                        is KeyboardConfiguration.Item.ContentKey -> {
                            val rowIndex = keyCodeRows.size - item.rowId - 1
                            val keyCode = softKeyCodeMapper[keyCodeRows[rowIndex][item.index]]
                            resultRow += Keyboard.KeyItem.Key(keyCode)
                        }
                        is KeyboardConfiguration.Item.ContentRow -> {
                            val rowIndex = keyCodeRows.size - item.rowId - 1
                            val content = keyCodeRows[rowIndex].map {
                                if(it == KeyEvent.KEYCODE_SPACE) {
                                    Keyboard.KeyItem.SplitSpacer(params.splitWidth)
                                } else {
                                    val keyCode = softKeyCodeMapper[it]
                                    Keyboard.KeyItem.Key(keyCode)
                                }
                            }
                            resultRow += content
                        }
                        is KeyboardConfiguration.Item.Spacer -> {
                            resultRow += Keyboard.KeyItem.Spacer(item.width)
                        }
                        is KeyboardConfiguration.Item.TemplateKey -> {
                            val keyCode = softKeyCodeMapper[item.keyCode]
                            if(params.splitWidth != 0 && keyCode == KeyEvent.KEYCODE_SPACE) {
                                resultRow += inflateKey(keyCode, item.copy(width = item.width / 2))
                                resultRow += Keyboard.KeyItem.SplitSpacer(params.splitWidth)
                                resultRow += inflateKey(keyCode, item.copy(width = item.width / 2))
                            } else {
                                resultRow += inflateKey(keyCode, item)
                            }
                        }
                    }
                }
                result += resultRow
            }
            return Keyboard(result, params)
        }

        private fun inflateKey(keyCode: Int, item: KeyboardConfiguration.Item.TemplateKey): Keyboard.KeyItem {
            return if(item.special) {
                val type = SpecialKeyType.ofKeyCode(keyCode) ?: SpecialKeyType.Default
                val iconRes = item.iconRes ?: type.iconRes ?: 0
                val bkgRes = R.drawable.key_bg
                Keyboard.KeyItem.Key(
                    keyCode,
                    width = item.width,
                    label = "",
                    iconRes = iconRes,
                    themeRes = type.themeRes,
                    bkgRes = bkgRes
                )
            } else {
                Keyboard.KeyItem.Key(keyCode, width = item.width)
            }
        }
    }

    data class ByScreenMode(
        val mobile: KeyboardTemplate,
        val tablet: KeyboardTemplate = mobile,
        val full: KeyboardTemplate = tablet
    ): KeyboardTemplate {
        override fun inflate(params: KeyboardParams): Keyboard {
            return when(params.screenMode) {
                KeyboardState.ScreenMode.Mobile -> mobile.inflate(params)
                KeyboardState.ScreenMode.Tablet -> tablet.inflate(params)
                KeyboardState.ScreenMode.Full -> full.inflate(params)
            }
        }
    }
}