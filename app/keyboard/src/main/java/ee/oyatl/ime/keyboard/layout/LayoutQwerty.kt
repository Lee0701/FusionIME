package ee.oyatl.ime.keyboard.layout

import android.view.KeyEvent

object LayoutQwerty {
    val TABLE_QWERTY = mapOf(
        KeyEvent.KEYCODE_Q to listOf('q'.code, 'Q'.code),
        KeyEvent.KEYCODE_W to listOf('w'.code, 'W'.code),
        KeyEvent.KEYCODE_E to listOf('e'.code, 'E'.code),
        KeyEvent.KEYCODE_R to listOf('r'.code, 'R'.code),
        KeyEvent.KEYCODE_T to listOf('t'.code, 'T'.code),
        KeyEvent.KEYCODE_Y to listOf('y'.code, 'Y'.code),
        KeyEvent.KEYCODE_U to listOf('u'.code, 'U'.code),
        KeyEvent.KEYCODE_I to listOf('i'.code, 'I'.code),
        KeyEvent.KEYCODE_O to listOf('o'.code, 'O'.code),
        KeyEvent.KEYCODE_P to listOf('p'.code, 'P'.code),
        KeyEvent.KEYCODE_LEFT_BRACKET to listOf('['.code, '}'.code, '['.code),
        KeyEvent.KEYCODE_RIGHT_BRACKET to listOf('['.code, '}'.code, ']'.code),

        KeyEvent.KEYCODE_A to listOf('a'.code, 'A'.code),
        KeyEvent.KEYCODE_S to listOf('s'.code, 'S'.code),
        KeyEvent.KEYCODE_D to listOf('d'.code, 'D'.code),
        KeyEvent.KEYCODE_F to listOf('f'.code, 'F'.code),
        KeyEvent.KEYCODE_G to listOf('g'.code, 'G'.code),
        KeyEvent.KEYCODE_H to listOf('h'.code, 'H'.code),
        KeyEvent.KEYCODE_J to listOf('j'.code, 'J'.code),
        KeyEvent.KEYCODE_K to listOf('k'.code, 'K'.code),
        KeyEvent.KEYCODE_L to listOf('l'.code, 'L'.code),
        KeyEvent.KEYCODE_SEMICOLON to listOf(';'.code, ':'.code, ':'.code),
        KeyEvent.KEYCODE_APOSTROPHE to listOf('\''.code, '"'.code, '\''.code),

        KeyEvent.KEYCODE_Z to listOf('z'.code, 'Z'.code),
        KeyEvent.KEYCODE_X to listOf('x'.code, 'X'.code),
        KeyEvent.KEYCODE_C to listOf('c'.code, 'C'.code),
        KeyEvent.KEYCODE_V to listOf('v'.code, 'V'.code),
        KeyEvent.KEYCODE_B to listOf('b'.code, 'B'.code),
        KeyEvent.KEYCODE_N to listOf('n'.code, 'N'.code),
        KeyEvent.KEYCODE_M to listOf('m'.code, 'M'.code),
        KeyEvent.KEYCODE_COMMA to listOf(','.code, '<'.code, ','.code),
        KeyEvent.KEYCODE_PERIOD to listOf('.'.code, '>'.code, '.'.code),
        KeyEvent.KEYCODE_SLASH to listOf('/'.code, '?'.code, '/'.code)
    )
}