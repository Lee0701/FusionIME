package ee.oyatl.ime.keyboard.layout

import android.view.KeyEvent

object LayoutRomaji {
    val TABLE_QWERTY = LayoutQwerty.TABLE_QWERTY + mapOf(
        KeyEvent.KEYCODE_MINUS to listOf('ー'.code, 'ー'.code),
    )
}