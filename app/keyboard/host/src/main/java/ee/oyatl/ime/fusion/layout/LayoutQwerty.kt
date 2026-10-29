package ee.oyatl.ime.fusion.layout

import ee.oyatl.ime.fusion.keyboard.core.KeyCode

object LayoutQwerty {
    val TABLE_QWERTY = mapOf(
        KeyCode.KEYCODE_GRAVE to listOf('`'.code, '~'.code, '`'.code),
        KeyCode.KEYCODE_1 to listOf('1'.code, '!'.code, '1'.code),
        KeyCode.KEYCODE_2 to listOf('2'.code, '@'.code, '2'.code),
        KeyCode.KEYCODE_3 to listOf('3'.code, '#'.code, '3'.code),
        KeyCode.KEYCODE_4 to listOf('4'.code, '$'.code, '4'.code),
        KeyCode.KEYCODE_5 to listOf('5'.code, '%'.code, '5'.code),
        KeyCode.KEYCODE_6 to listOf('6'.code, '^'.code, '6'.code),
        KeyCode.KEYCODE_7 to listOf('7'.code, '&'.code, '7'.code),
        KeyCode.KEYCODE_8 to listOf('8'.code, '*'.code, '8'.code),
        KeyCode.KEYCODE_9 to listOf('9'.code, '('.code, '9'.code),
        KeyCode.KEYCODE_0 to listOf('0'.code, ')'.code, '0'.code),
        KeyCode.KEYCODE_MINUS to listOf('-'.code, '_'.code, '-'.code),
        KeyCode.KEYCODE_EQUALS to listOf('='.code, '+'.code, '='.code),

        KeyCode.KEYCODE_Q to listOf('q'.code, 'Q'.code),
        KeyCode.KEYCODE_W to listOf('w'.code, 'W'.code),
        KeyCode.KEYCODE_E to listOf('e'.code, 'E'.code),
        KeyCode.KEYCODE_R to listOf('r'.code, 'R'.code),
        KeyCode.KEYCODE_T to listOf('t'.code, 'T'.code),
        KeyCode.KEYCODE_Y to listOf('y'.code, 'Y'.code),
        KeyCode.KEYCODE_U to listOf('u'.code, 'U'.code),
        KeyCode.KEYCODE_I to listOf('i'.code, 'I'.code),
        KeyCode.KEYCODE_O to listOf('o'.code, 'O'.code),
        KeyCode.KEYCODE_P to listOf('p'.code, 'P'.code),
        KeyCode.KEYCODE_LEFT_BRACKET to listOf('['.code, '}'.code, '['.code),
        KeyCode.KEYCODE_RIGHT_BRACKET to listOf(']'.code, '}'.code, ']'.code),
        KeyCode.KEYCODE_BACKSLASH to listOf('\\'.code, '|'.code, '\\'.code),

        KeyCode.KEYCODE_A to listOf('a'.code, 'A'.code),
        KeyCode.KEYCODE_S to listOf('s'.code, 'S'.code),
        KeyCode.KEYCODE_D to listOf('d'.code, 'D'.code),
        KeyCode.KEYCODE_F to listOf('f'.code, 'F'.code),
        KeyCode.KEYCODE_G to listOf('g'.code, 'G'.code),
        KeyCode.KEYCODE_H to listOf('h'.code, 'H'.code),
        KeyCode.KEYCODE_J to listOf('j'.code, 'J'.code),
        KeyCode.KEYCODE_K to listOf('k'.code, 'K'.code),
        KeyCode.KEYCODE_L to listOf('l'.code, 'L'.code),
        KeyCode.KEYCODE_SEMICOLON to listOf(';'.code, ':'.code, ';'.code),
        KeyCode.KEYCODE_APOSTROPHE to listOf('\''.code, '"'.code, '\''.code),

        KeyCode.KEYCODE_Z to listOf('z'.code, 'Z'.code),
        KeyCode.KEYCODE_X to listOf('x'.code, 'X'.code),
        KeyCode.KEYCODE_C to listOf('c'.code, 'C'.code),
        KeyCode.KEYCODE_V to listOf('v'.code, 'V'.code),
        KeyCode.KEYCODE_B to listOf('b'.code, 'B'.code),
        KeyCode.KEYCODE_N to listOf('n'.code, 'N'.code),
        KeyCode.KEYCODE_M to listOf('m'.code, 'M'.code),
        KeyCode.KEYCODE_COMMA to listOf(','.code, '<'.code, ','.code),
        KeyCode.KEYCODE_PERIOD to listOf('.'.code, '>'.code, '.'.code),
        KeyCode.KEYCODE_SLASH to listOf('/'.code, '?'.code, '/'.code)
    )

}