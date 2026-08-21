package ee.oyatl.ime.fusion.layout

import android.view.KeyEvent

object LayoutRomaji {
    val KEYCODE_MAP_QWERTY = mapOf(
        KeyEvent.KEYCODE_SEMICOLON to KeyEvent.KEYCODE_MINUS
    )

    val TABLE_QWERTY = LayoutQwerty.TABLE_QWERTY + mapOf(
        KeyEvent.KEYCODE_MINUS to listOf('ー'.code, 'ー'.code),
    )
}