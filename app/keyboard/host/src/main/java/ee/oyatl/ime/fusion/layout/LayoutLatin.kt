package ee.oyatl.ime.fusion.layout

import ee.oyatl.ime.fusion.keyboard.core.ExtKeyCode
import ee.oyatl.ime.fusion.keyboard.core.KeyCode

object LayoutLatin {
    const val COMB: Int = 0x80000000.toInt()

    val TABLE_SPANISH_QWERTY = mapOf(
        KeyCode.KEYCODE_SEMICOLON to listOf('ñ'.code, 'Ñ'.code)
    )

    val TABLE_AZERTY = mapOf(
        ExtKeyCode.KEYCODE_EXT_1 to listOf('1'.code, '!'.code, '1'.code),
        ExtKeyCode.KEYCODE_EXT_2 to listOf('2'.code, '@'.code, '2'.code),
        ExtKeyCode.KEYCODE_EXT_3 to listOf('3'.code, '#'.code, '3'.code),
        ExtKeyCode.KEYCODE_EXT_4 to listOf('4'.code, '$'.code, '4'.code),
        ExtKeyCode.KEYCODE_EXT_5 to listOf('5'.code, '%'.code, '5'.code),
        ExtKeyCode.KEYCODE_EXT_6 to listOf('6'.code, '^'.code, '6'.code),
        ExtKeyCode.KEYCODE_EXT_7 to listOf('7'.code, '&'.code, '7'.code),
        ExtKeyCode.KEYCODE_EXT_8 to listOf('8'.code, '*'.code, '8'.code),
        ExtKeyCode.KEYCODE_EXT_9 to listOf('9'.code, '('.code, '9'.code),
        ExtKeyCode.KEYCODE_EXT_0 to listOf('0'.code, ')'.code, '0'.code),

        KeyCode.KEYCODE_1 to listOf('&'.code, '1'.code),
        KeyCode.KEYCODE_2 to listOf('é'.code, '2'.code),
        KeyCode.KEYCODE_3 to listOf('"'.code, '3'.code),
        KeyCode.KEYCODE_4 to listOf('\''.code, '4'.code),
        KeyCode.KEYCODE_5 to listOf('('.code, '5'.code),
        KeyCode.KEYCODE_6 to listOf('-'.code, '6'.code),
        KeyCode.KEYCODE_7 to listOf('è'.code, '7'.code),
        KeyCode.KEYCODE_8 to listOf('_'.code, '8'.code),
        KeyCode.KEYCODE_9 to listOf('ç'.code, '9'.code),
        KeyCode.KEYCODE_0 to listOf('à'.code, '0'.code),

        KeyCode.KEYCODE_APOSTROPHE to listOf('²'.code),
        KeyCode.KEYCODE_LEFT_BRACKET to listOf(')'.code, '°'.code),
        KeyCode.KEYCODE_EQUALS to listOf('='.code, '+'.code),
        KeyCode.KEYCODE_RIGHT_BRACKET to listOf(COMB or '^'.code, COMB or '¨'.code),
        KeyCode.KEYCODE_SEMICOLON to listOf('$'.code, '£'.code),
        KeyCode.KEYCODE_GRAVE to listOf('ù'.code, '%'.code),
        KeyCode.KEYCODE_BACKSLASH to listOf('*'.code, 'µ'.code),
        KeyCode.KEYCODE_COMMA to listOf(','.code, '?'.code),
        KeyCode.KEYCODE_PERIOD to listOf(';'.code, '.'.code),
        KeyCode.KEYCODE_SLASH to listOf(':'.code, '/'.code),
        ExtKeyCode.KEYCODE_OEM_8 to listOf('!'.code, '§'.code),

        ExtKeyCode.KEYCODE_EXT_COMMA to listOf(','.code),
        ExtKeyCode.KEYCODE_EXT_PERIOD to listOf('.'.code),
        ExtKeyCode.KEYCODE_APOSTROPHE_QUESTION to listOf('\''.code, '?'.code)
    )

    val TABLE_AZERTY_ALTGR = mapOf(
        KeyCode.KEYCODE_2 to listOf(COMB or '~'.code),
        KeyCode.KEYCODE_3 to listOf('#'.code),
        KeyCode.KEYCODE_4 to listOf('{'.code),
        KeyCode.KEYCODE_5 to listOf('['.code),
        KeyCode.KEYCODE_6 to listOf('|'.code),
        KeyCode.KEYCODE_7 to listOf(COMB or '`'.code),
        KeyCode.KEYCODE_8 to listOf('\\'.code),
        KeyCode.KEYCODE_9 to listOf('^'.code),
        KeyCode.KEYCODE_0 to listOf('@'.code),
        KeyCode.KEYCODE_LEFT_BRACKET to listOf(']'.code),
        KeyCode.KEYCODE_EQUALS to listOf('}'.code),
        KeyCode.KEYCODE_E to listOf('€'.code),
        KeyCode.KEYCODE_SEMICOLON to listOf('¤'.code),
    )

    val TABLE_GERMAN_QWERTZ = mapOf(
        KeyCode.KEYCODE_BACKSLASH to listOf(COMB or '^'.code, '°'.code, COMB or '^'.code),
        KeyCode.KEYCODE_LEFT_BRACKET to listOf('ß'.code, '?'.code, 'ß'.code),
        KeyCode.KEYCODE_RIGHT_BRACKET to listOf(COMB or '´'.code, COMB or COMB or '`'.code),
        KeyCode.KEYCODE_SEMICOLON to listOf('ü'.code, 'Ü'.code, 'Ü'.code),
        KeyCode.KEYCODE_EQUALS to listOf('+'.code, '*'.code, '+'.code),
        KeyCode.KEYCODE_GRAVE to listOf('ö'.code, 'Ö'.code, 'Ö'.code),
        KeyCode.KEYCODE_APOSTROPHE to listOf('ä'.code, 'Ä'.code, 'Ä'.code),
        KeyCode.KEYCODE_SLASH to listOf('#'.code, '\''.code, '#'.code),
        KeyCode.KEYCODE_COMMA to listOf(','.code, ';'.code, ','.code),
        KeyCode.KEYCODE_PERIOD to listOf('.'.code, ':'.code, '.'.code),
        KeyCode.KEYCODE_1 to listOf('1'.code, '!'.code, '1'.code),
        KeyCode.KEYCODE_2 to listOf('2'.code, '"'.code, '2'.code),
        KeyCode.KEYCODE_3 to listOf('3'.code, '§'.code, '3'.code),
        KeyCode.KEYCODE_4 to listOf('4'.code, '$'.code, '4'.code),
        KeyCode.KEYCODE_5 to listOf('5'.code, '%'.code, '5'.code),
        KeyCode.KEYCODE_6 to listOf('6'.code, '&'.code, '6'.code),
        KeyCode.KEYCODE_7 to listOf('7'.code, '/'.code, '7'.code),
        KeyCode.KEYCODE_8 to listOf('8'.code, '('.code, '8'.code),
        KeyCode.KEYCODE_9 to listOf('9'.code, ')'.code, '9'.code),
        KeyCode.KEYCODE_0 to listOf('0'.code, '='.code, '0'.code),
    )

    val TABLE_GERMAN_QWERTZ_ALTGR = mapOf(
        KeyCode.KEYCODE_2 to listOf('²'.code),
        KeyCode.KEYCODE_3 to listOf('³'.code),
        KeyCode.KEYCODE_7 to listOf('{'.code),
        KeyCode.KEYCODE_8 to listOf('['.code),
        KeyCode.KEYCODE_9 to listOf(']'.code),
        KeyCode.KEYCODE_0 to listOf('}'.code),
        KeyCode.KEYCODE_E to listOf('€'.code),
        KeyCode.KEYCODE_M to listOf('µ'.code),
        KeyCode.KEYCODE_Q to listOf('@'.code),
        KeyCode.KEYCODE_EQUALS to listOf('~'.code),
        KeyCode.KEYCODE_LEFT_BRACKET to listOf('\\'.code, 'ẞ'.code),
    )

    val KEYCODE_MAP_AZERTY = mapOf(
        KeyCode.KEYCODE_GRAVE to KeyCode.KEYCODE_APOSTROPHE,
        KeyCode.KEYCODE_MINUS to KeyCode.KEYCODE_LEFT_BRACKET,
        KeyCode.KEYCODE_LEFT_BRACKET to KeyCode.KEYCODE_RIGHT_BRACKET,
        KeyCode.KEYCODE_RIGHT_BRACKET to KeyCode.KEYCODE_SEMICOLON,
        KeyCode.KEYCODE_SEMICOLON to KeyCode.KEYCODE_M,
        KeyCode.KEYCODE_APOSTROPHE to KeyCode.KEYCODE_GRAVE,
        KeyCode.KEYCODE_M to KeyCode.KEYCODE_COMMA,
        KeyCode.KEYCODE_COMMA to KeyCode.KEYCODE_PERIOD,
        KeyCode.KEYCODE_PERIOD to KeyCode.KEYCODE_SLASH,
        KeyCode.KEYCODE_SLASH to ExtKeyCode.KEYCODE_OEM_8,

        KeyCode.KEYCODE_Q to KeyCode.KEYCODE_A,
        KeyCode.KEYCODE_W to KeyCode.KEYCODE_Z,
        KeyCode.KEYCODE_A to KeyCode.KEYCODE_Q,
        KeyCode.KEYCODE_Z to KeyCode.KEYCODE_W,
    )

    val SOFT_KEYCODE_MAP_AZERTY_NUMBERS = mapOf(
        KeyCode.KEYCODE_1 to ExtKeyCode.KEYCODE_EXT_1,
        KeyCode.KEYCODE_2 to ExtKeyCode.KEYCODE_EXT_2,
        KeyCode.KEYCODE_3 to ExtKeyCode.KEYCODE_EXT_3,
        KeyCode.KEYCODE_4 to ExtKeyCode.KEYCODE_EXT_4,
        KeyCode.KEYCODE_5 to ExtKeyCode.KEYCODE_EXT_5,
        KeyCode.KEYCODE_6 to ExtKeyCode.KEYCODE_EXT_6,
        KeyCode.KEYCODE_7 to ExtKeyCode.KEYCODE_EXT_7,
        KeyCode.KEYCODE_8 to ExtKeyCode.KEYCODE_EXT_8,
        KeyCode.KEYCODE_9 to ExtKeyCode.KEYCODE_EXT_9,
        KeyCode.KEYCODE_0 to ExtKeyCode.KEYCODE_EXT_0,
    )

    val SOFT_KEYCODE_MAP_AZERTY_MOBILE = mapOf(
        KeyCode.KEYCODE_COMMA to ExtKeyCode.KEYCODE_EXT_COMMA,
        KeyCode.KEYCODE_PERIOD to ExtKeyCode.KEYCODE_EXT_PERIOD,
        KeyCode.KEYCODE_M to ExtKeyCode.KEYCODE_APOSTROPHE_QUESTION,
    )

    val KEYCODE_MAP_QWERTZ = mapOf(
        KeyCode.KEYCODE_GRAVE to KeyCode.KEYCODE_BACKSLASH,
        KeyCode.KEYCODE_MINUS to KeyCode.KEYCODE_LEFT_BRACKET,
        KeyCode.KEYCODE_EQUALS to KeyCode.KEYCODE_RIGHT_BRACKET,
        KeyCode.KEYCODE_LEFT_BRACKET to KeyCode.KEYCODE_SEMICOLON,
        KeyCode.KEYCODE_RIGHT_BRACKET to KeyCode.KEYCODE_EQUALS,
        KeyCode.KEYCODE_SEMICOLON to KeyCode.KEYCODE_GRAVE,
        KeyCode.KEYCODE_BACKSLASH to KeyCode.KEYCODE_SLASH,
        KeyCode.KEYCODE_SLASH to KeyCode.KEYCODE_MINUS,

        KeyCode.KEYCODE_Y to KeyCode.KEYCODE_Z,
        KeyCode.KEYCODE_Z to KeyCode.KEYCODE_Y,
    )

    val SOFT_KEYCODE_MAP_DEUTSCH_QWERTZ = mapOf(
        KeyCode.KEYCODE_COMMA to ExtKeyCode.KEYCODE_PERIOD_COMMA,
        KeyCode.KEYCODE_PERIOD to KeyCode.KEYCODE_LEFT_BRACKET,
    )

    val KEYCODE_MAP_DVORAK = mapOf(
        KeyCode.KEYCODE_MINUS to KeyCode.KEYCODE_LEFT_BRACKET,
        KeyCode.KEYCODE_EQUALS to KeyCode.KEYCODE_RIGHT_BRACKET,

        KeyCode.KEYCODE_Q to KeyCode.KEYCODE_APOSTROPHE,
        KeyCode.KEYCODE_W to KeyCode.KEYCODE_COMMA,
        KeyCode.KEYCODE_E to KeyCode.KEYCODE_PERIOD,
        KeyCode.KEYCODE_R to KeyCode.KEYCODE_P,
        KeyCode.KEYCODE_T to KeyCode.KEYCODE_Y,
        KeyCode.KEYCODE_Y to KeyCode.KEYCODE_F,
        KeyCode.KEYCODE_U to KeyCode.KEYCODE_G,
        KeyCode.KEYCODE_I to KeyCode.KEYCODE_C,
        KeyCode.KEYCODE_O to KeyCode.KEYCODE_R,
        KeyCode.KEYCODE_P to KeyCode.KEYCODE_L,
        KeyCode.KEYCODE_LEFT_BRACKET to KeyCode.KEYCODE_SLASH,
        KeyCode.KEYCODE_RIGHT_BRACKET to KeyCode.KEYCODE_EQUALS,

        KeyCode.KEYCODE_A to KeyCode.KEYCODE_A,
        KeyCode.KEYCODE_S to KeyCode.KEYCODE_O,
        KeyCode.KEYCODE_D to KeyCode.KEYCODE_E,
        KeyCode.KEYCODE_F to KeyCode.KEYCODE_U,
        KeyCode.KEYCODE_G to KeyCode.KEYCODE_I,
        KeyCode.KEYCODE_H to KeyCode.KEYCODE_D,
        KeyCode.KEYCODE_J to KeyCode.KEYCODE_H,
        KeyCode.KEYCODE_K to KeyCode.KEYCODE_T,
        KeyCode.KEYCODE_L to KeyCode.KEYCODE_N,
        KeyCode.KEYCODE_SEMICOLON to KeyCode.KEYCODE_S,
        KeyCode.KEYCODE_APOSTROPHE to KeyCode.KEYCODE_MINUS,

        KeyCode.KEYCODE_Z to KeyCode.KEYCODE_SEMICOLON,
        KeyCode.KEYCODE_X to KeyCode.KEYCODE_Q,
        KeyCode.KEYCODE_C to KeyCode.KEYCODE_J,
        KeyCode.KEYCODE_V to KeyCode.KEYCODE_K,
        KeyCode.KEYCODE_B to KeyCode.KEYCODE_X,
        KeyCode.KEYCODE_N to KeyCode.KEYCODE_B,
        KeyCode.KEYCODE_M to KeyCode.KEYCODE_M,
        KeyCode.KEYCODE_COMMA to KeyCode.KEYCODE_W,
        KeyCode.KEYCODE_PERIOD to KeyCode.KEYCODE_V,
        KeyCode.KEYCODE_SLASH to KeyCode.KEYCODE_Z,
    )

    val KEYCODE_MAP_COLEMAK = mapOf(
        KeyCode.KEYCODE_Q to KeyCode.KEYCODE_Q,
        KeyCode.KEYCODE_W to KeyCode.KEYCODE_W,
        KeyCode.KEYCODE_E to KeyCode.KEYCODE_F,
        KeyCode.KEYCODE_R to KeyCode.KEYCODE_P,
        KeyCode.KEYCODE_T to KeyCode.KEYCODE_G,
        KeyCode.KEYCODE_Y to KeyCode.KEYCODE_J,
        KeyCode.KEYCODE_U to KeyCode.KEYCODE_L,
        KeyCode.KEYCODE_I to KeyCode.KEYCODE_U,
        KeyCode.KEYCODE_O to KeyCode.KEYCODE_Y,
        KeyCode.KEYCODE_P to KeyCode.KEYCODE_SEMICOLON,

        KeyCode.KEYCODE_A to KeyCode.KEYCODE_A,
        KeyCode.KEYCODE_S to KeyCode.KEYCODE_R,
        KeyCode.KEYCODE_D to KeyCode.KEYCODE_S,
        KeyCode.KEYCODE_F to KeyCode.KEYCODE_T,
        KeyCode.KEYCODE_G to KeyCode.KEYCODE_D,
        KeyCode.KEYCODE_H to KeyCode.KEYCODE_H,
        KeyCode.KEYCODE_J to KeyCode.KEYCODE_N,
        KeyCode.KEYCODE_K to KeyCode.KEYCODE_E,
        KeyCode.KEYCODE_L to KeyCode.KEYCODE_I,
        KeyCode.KEYCODE_SEMICOLON to KeyCode.KEYCODE_O,

        KeyCode.KEYCODE_Z to KeyCode.KEYCODE_Z,
        KeyCode.KEYCODE_X to KeyCode.KEYCODE_X,
        KeyCode.KEYCODE_C to KeyCode.KEYCODE_C,
        KeyCode.KEYCODE_V to KeyCode.KEYCODE_V,
        KeyCode.KEYCODE_B to KeyCode.KEYCODE_B,
        KeyCode.KEYCODE_N to KeyCode.KEYCODE_K,
        KeyCode.KEYCODE_M to KeyCode.KEYCODE_M,
    )

}
