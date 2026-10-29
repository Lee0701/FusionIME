package ee.oyatl.ime.fusion.layout

import ee.oyatl.ime.fusion.keyboard.core.KeyCode

object LayoutRomaji {
    val KEYCODE_MAP_QWERTY = mapOf(
        KeyCode.KEYCODE_SEMICOLON to KeyCode.KEYCODE_MINUS
    )

    val TABLE_QWERTY = LayoutQwerty.TABLE_QWERTY + mapOf(
        KeyCode.KEYCODE_MINUS to listOf('ー'.code, 'ー'.code),
    )
}