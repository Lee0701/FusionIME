package ee.oyatl.ime.fusion.layout

import ee.oyatl.ime.fusion.keyboard.core.ExtKeyCode
import ee.oyatl.ime.fusion.keyboard.core.KeyCode

object LayoutExt {
    val TABLE = mapOf(
        ExtKeyCode.KEYCODE_PERIOD_COMMA to listOf('.'.code, ','.code),
        ExtKeyCode.KEYCODE_COMMA_PERIOD to listOf(','.code, '.'.code)
    )

    val TABLE_CHINESE = mapOf(
        KeyCode.KEYCODE_COMMA to listOf('，'.code, '《'.code),
        KeyCode.KEYCODE_PERIOD to listOf('。'.code, '》'.code),
        ExtKeyCode.KEYCODE_PERIOD_COMMA to listOf('。'.code, '，'.code),
        ExtKeyCode.KEYCODE_COMMA_PERIOD to listOf('，'.code, '。'.code)
    )

    val TABLE_HAN_NOM = mapOf(
        KeyCode.KEYCODE_COMMA to listOf('、'.code, '《'.code),
        KeyCode.KEYCODE_PERIOD to listOf('。'.code, '》'.code),
        ExtKeyCode.KEYCODE_PERIOD_COMMA to listOf('。'.code, '、'.code),
        ExtKeyCode.KEYCODE_COMMA_PERIOD to listOf('、'.code, '。'.code)
    )
}