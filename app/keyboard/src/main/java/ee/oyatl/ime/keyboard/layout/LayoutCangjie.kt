package ee.oyatl.ime.keyboard.layout

import android.view.KeyEvent

object LayoutCangjie {

    val TABLE_QWERTY = mapOf(
        KeyEvent.KEYCODE_Q to listOf('手'.code, 'Q'.code),
        KeyEvent.KEYCODE_W to listOf('田'.code, 'W'.code),
        KeyEvent.KEYCODE_E to listOf('水'.code, 'E'.code),
        KeyEvent.KEYCODE_R to listOf('口'.code, 'R'.code),
        KeyEvent.KEYCODE_T to listOf('廿'.code, 'T'.code),
        KeyEvent.KEYCODE_Y to listOf('卜'.code, 'Y'.code),
        KeyEvent.KEYCODE_U to listOf('山'.code, 'U'.code),
        KeyEvent.KEYCODE_I to listOf('戈'.code, 'I'.code),
        KeyEvent.KEYCODE_O to listOf('人'.code, 'O'.code),
        KeyEvent.KEYCODE_P to listOf('心'.code, 'P'.code),

        KeyEvent.KEYCODE_A to listOf('日'.code, 'A'.code),
        KeyEvent.KEYCODE_S to listOf('尸'.code, 'S'.code),
        KeyEvent.KEYCODE_D to listOf('木'.code, 'D'.code),
        KeyEvent.KEYCODE_F to listOf('火'.code, 'F'.code),
        KeyEvent.KEYCODE_G to listOf('土'.code, 'G'.code),
        KeyEvent.KEYCODE_H to listOf('竹'.code, 'H'.code),
        KeyEvent.KEYCODE_J to listOf('十'.code, 'J'.code),
        KeyEvent.KEYCODE_K to listOf('大'.code, 'K'.code),
        KeyEvent.KEYCODE_L to listOf('中'.code, 'L'.code),
        KeyEvent.KEYCODE_MINUS to listOf('ー'.code, 'ー'.code),

        KeyEvent.KEYCODE_Z to listOf('重'.code, 'Z'.code),
        KeyEvent.KEYCODE_X to listOf('難'.code, 'X'.code),
        KeyEvent.KEYCODE_C to listOf('金'.code, 'C'.code),
        KeyEvent.KEYCODE_V to listOf('女'.code, 'V'.code),
        KeyEvent.KEYCODE_B to listOf('月'.code, 'B'.code),
        KeyEvent.KEYCODE_N to listOf('弓'.code, 'N'.code),
        KeyEvent.KEYCODE_M to listOf('一'.code, 'M'.code)
    )

    val KEY_MAP = mapOf(
        '日' to 'a',
        '月' to 'b',
        '金' to 'c',
        '木' to 'd',
        '水' to 'e',
        '火' to 'f',
        '土' to 'g',
        '竹' to 'h',
        '戈' to 'i',
        '十' to 'j',
        '大' to 'k',
        '中' to 'l',
        '一' to 'm',
        '弓' to 'n',
        '人' to 'o',
        '心' to 'p',
        '手' to 'q',
        '口' to 'r',
        '尸' to 's',
        '廿' to 't',
        '山' to 'u',
        '女' to 'v',
        '田' to 'w',
        '難' to 'x',
        '卜' to 'y',
        '重' to 'z'
    )
}