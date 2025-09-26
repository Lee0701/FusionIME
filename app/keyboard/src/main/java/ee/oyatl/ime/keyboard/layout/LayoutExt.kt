package ee.oyatl.ime.keyboard.layout

import android.view.KeyEvent

object LayoutExt {
    val TABLE = mapOf(
        ExtKeyCode.KEYCODE_PERIOD_COMMA to listOf('.'.code, ','.code),
        ExtKeyCode.KEYCODE_COMMA_PERIOD to listOf(','.code, '.'.code)
    )

    val TABLE_CHINESE = mapOf(
        KeyEvent.KEYCODE_COMMA to listOf('，'.code, '《'.code),
        KeyEvent.KEYCODE_PERIOD to listOf('。'.code, '》'.code),
        ExtKeyCode.KEYCODE_PERIOD_COMMA to listOf('。'.code, '，'.code),
        ExtKeyCode.KEYCODE_COMMA_PERIOD to listOf('，'.code, '。'.code)
    )
}