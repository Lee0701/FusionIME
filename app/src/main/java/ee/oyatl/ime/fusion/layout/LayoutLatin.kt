package ee.oyatl.ime.fusion.layout

import android.view.KeyCharacterMap
import android.view.KeyEvent

object LayoutLatin {
    const val COMB = KeyCharacterMap.COMBINING_ACCENT

    val TABLE_SPANISH_QWERTY = mapOf(
        KeyEvent.KEYCODE_SEMICOLON to listOf('ñ'.code, 'Ñ'.code)
    )

    val TABLE_AZERTY = mapOf(
        KeyEvent.KEYCODE_1 to listOf('&'.code, '1'.code),
        KeyEvent.KEYCODE_2 to listOf('é'.code, '2'.code),
        KeyEvent.KEYCODE_3 to listOf('"'.code, '3'.code),
        KeyEvent.KEYCODE_4 to listOf('\''.code, '4'.code),
        KeyEvent.KEYCODE_5 to listOf('('.code, '5'.code),
        KeyEvent.KEYCODE_6 to listOf('-'.code, '6'.code),
        KeyEvent.KEYCODE_7 to listOf('è'.code, '7'.code),
        KeyEvent.KEYCODE_8 to listOf('_'.code, '8'.code),
        KeyEvent.KEYCODE_9 to listOf('ç'.code, '9'.code),
        KeyEvent.KEYCODE_0 to listOf('à'.code, '0'.code),

        KeyEvent.KEYCODE_APOSTROPHE to listOf('²'.code),
        KeyEvent.KEYCODE_LEFT_BRACKET to listOf(')'.code, '°'.code),
        KeyEvent.KEYCODE_EQUALS to listOf('='.code, '+'.code),
        KeyEvent.KEYCODE_RIGHT_BRACKET to listOf(COMB or '^'.code, COMB or '¨'.code),
        KeyEvent.KEYCODE_SEMICOLON to listOf('$'.code, '£'.code),
        KeyEvent.KEYCODE_GRAVE to listOf('ù'.code, '%'.code),
        KeyEvent.KEYCODE_BACKSLASH to listOf('*'.code, 'µ'.code),
        KeyEvent.KEYCODE_COMMA to listOf(','.code, '?'.code),
        KeyEvent.KEYCODE_PERIOD to listOf(';'.code, '.'.code),
        KeyEvent.KEYCODE_SLASH to listOf(':'.code, '/'.code),
        ExtKeyCode.KEYCODE_OEM_8 to listOf('!'.code, '§'.code),

        ExtKeyCode.KEYCODE_EXT_COMMA to listOf(','.code),
        ExtKeyCode.KEYCODE_EXT_PERIOD to listOf('.'.code),
        ExtKeyCode.KEYCODE_APOSTROPHE_QUESTION to listOf('\''.code, '?'.code)
    )

    val TABLE_GERMAN_QWERTZ = mapOf(
        KeyEvent.KEYCODE_BACKSLASH to listOf(COMB or '^'.code, '°'.code, COMB or '^'.code),
        KeyEvent.KEYCODE_LEFT_BRACKET to listOf('ß'.code, '?'.code, 'ß'.code),
        KeyEvent.KEYCODE_RIGHT_BRACKET to listOf(COMB or '´'.code, COMB or COMB or '`'.code),
        KeyEvent.KEYCODE_SEMICOLON to listOf('ü'.code, 'Ü'.code, 'Ü'.code),
        KeyEvent.KEYCODE_EQUALS to listOf('+'.code, '*'.code, '+'.code),
        KeyEvent.KEYCODE_GRAVE to listOf('ö'.code, 'Ö'.code, 'Ö'.code),
        KeyEvent.KEYCODE_APOSTROPHE to listOf('ä'.code, 'Ä'.code, 'Ä'.code),
        KeyEvent.KEYCODE_SLASH to listOf('#'.code, '\''.code, '#'.code),
        KeyEvent.KEYCODE_COMMA to listOf(','.code, ';'.code, ','.code),
        KeyEvent.KEYCODE_PERIOD to listOf('.'.code, ':'.code, '.'.code),
        KeyEvent.KEYCODE_1 to listOf('1'.code, '!'.code, '1'.code),
        KeyEvent.KEYCODE_2 to listOf('2'.code, '"'.code, '2'.code),
        KeyEvent.KEYCODE_3 to listOf('3'.code, '§'.code, '3'.code),
        KeyEvent.KEYCODE_4 to listOf('4'.code, '$'.code, '4'.code),
        KeyEvent.KEYCODE_5 to listOf('5'.code, '%'.code, '5'.code),
        KeyEvent.KEYCODE_6 to listOf('6'.code, '&'.code, '6'.code),
        KeyEvent.KEYCODE_7 to listOf('7'.code, '/'.code, '7'.code),
        KeyEvent.KEYCODE_8 to listOf('8'.code, '('.code, '8'.code),
        KeyEvent.KEYCODE_9 to listOf('9'.code, ')'.code, '9'.code),
        KeyEvent.KEYCODE_0 to listOf('0'.code, '='.code, '0'.code),
    )

    val KEYCODE_MAP_AZERTY = mapOf(
        KeyEvent.KEYCODE_GRAVE to KeyEvent.KEYCODE_APOSTROPHE,
        KeyEvent.KEYCODE_MINUS to KeyEvent.KEYCODE_LEFT_BRACKET,
        KeyEvent.KEYCODE_LEFT_BRACKET to KeyEvent.KEYCODE_RIGHT_BRACKET,
        KeyEvent.KEYCODE_RIGHT_BRACKET to KeyEvent.KEYCODE_SEMICOLON,
        KeyEvent.KEYCODE_SEMICOLON to KeyEvent.KEYCODE_M,
        KeyEvent.KEYCODE_APOSTROPHE to KeyEvent.KEYCODE_GRAVE,
        KeyEvent.KEYCODE_M to KeyEvent.KEYCODE_COMMA,
        KeyEvent.KEYCODE_COMMA to KeyEvent.KEYCODE_PERIOD,
        KeyEvent.KEYCODE_PERIOD to KeyEvent.KEYCODE_SLASH,
        KeyEvent.KEYCODE_SLASH to ExtKeyCode.KEYCODE_OEM_8,

        KeyEvent.KEYCODE_Q to KeyEvent.KEYCODE_A,
        KeyEvent.KEYCODE_W to KeyEvent.KEYCODE_Z,
        KeyEvent.KEYCODE_A to KeyEvent.KEYCODE_Q,
        KeyEvent.KEYCODE_Z to KeyEvent.KEYCODE_W,
    )

    val SOFT_KEYCODE_MAP_AZERTY = mapOf(
        KeyEvent.KEYCODE_COMMA to ExtKeyCode.KEYCODE_EXT_COMMA,
        KeyEvent.KEYCODE_PERIOD to ExtKeyCode.KEYCODE_EXT_PERIOD,
        KeyEvent.KEYCODE_M to ExtKeyCode.KEYCODE_APOSTROPHE_QUESTION,
    )

    val KEYCODE_MAP_QWERTZ = mapOf(
        KeyEvent.KEYCODE_GRAVE to KeyEvent.KEYCODE_BACKSLASH,
        KeyEvent.KEYCODE_MINUS to KeyEvent.KEYCODE_LEFT_BRACKET,
        KeyEvent.KEYCODE_EQUALS to KeyEvent.KEYCODE_RIGHT_BRACKET,
        KeyEvent.KEYCODE_LEFT_BRACKET to KeyEvent.KEYCODE_SEMICOLON,
        KeyEvent.KEYCODE_RIGHT_BRACKET to KeyEvent.KEYCODE_EQUALS,
        KeyEvent.KEYCODE_SEMICOLON to KeyEvent.KEYCODE_GRAVE,
        KeyEvent.KEYCODE_BACKSLASH to KeyEvent.KEYCODE_SLASH,
        KeyEvent.KEYCODE_SLASH to KeyEvent.KEYCODE_MINUS,

        KeyEvent.KEYCODE_Y to KeyEvent.KEYCODE_Z,
        KeyEvent.KEYCODE_Z to KeyEvent.KEYCODE_Y,
    )

    val KEYCODE_MAP_DVORAK = mapOf(
        KeyEvent.KEYCODE_MINUS to KeyEvent.KEYCODE_LEFT_BRACKET,
        KeyEvent.KEYCODE_EQUALS to KeyEvent.KEYCODE_RIGHT_BRACKET,

        KeyEvent.KEYCODE_Q to KeyEvent.KEYCODE_APOSTROPHE,
        KeyEvent.KEYCODE_W to KeyEvent.KEYCODE_COMMA,
        KeyEvent.KEYCODE_E to KeyEvent.KEYCODE_PERIOD,
        KeyEvent.KEYCODE_R to KeyEvent.KEYCODE_P,
        KeyEvent.KEYCODE_T to KeyEvent.KEYCODE_Y,
        KeyEvent.KEYCODE_Y to KeyEvent.KEYCODE_F,
        KeyEvent.KEYCODE_U to KeyEvent.KEYCODE_G,
        KeyEvent.KEYCODE_I to KeyEvent.KEYCODE_C,
        KeyEvent.KEYCODE_O to KeyEvent.KEYCODE_R,
        KeyEvent.KEYCODE_P to KeyEvent.KEYCODE_L,
        KeyEvent.KEYCODE_LEFT_BRACKET to KeyEvent.KEYCODE_SLASH,
        KeyEvent.KEYCODE_RIGHT_BRACKET to KeyEvent.KEYCODE_EQUALS,

        KeyEvent.KEYCODE_A to KeyEvent.KEYCODE_A,
        KeyEvent.KEYCODE_S to KeyEvent.KEYCODE_O,
        KeyEvent.KEYCODE_D to KeyEvent.KEYCODE_E,
        KeyEvent.KEYCODE_F to KeyEvent.KEYCODE_U,
        KeyEvent.KEYCODE_G to KeyEvent.KEYCODE_I,
        KeyEvent.KEYCODE_H to KeyEvent.KEYCODE_D,
        KeyEvent.KEYCODE_J to KeyEvent.KEYCODE_H,
        KeyEvent.KEYCODE_K to KeyEvent.KEYCODE_T,
        KeyEvent.KEYCODE_L to KeyEvent.KEYCODE_N,
        KeyEvent.KEYCODE_SEMICOLON to KeyEvent.KEYCODE_S,
        KeyEvent.KEYCODE_APOSTROPHE to KeyEvent.KEYCODE_MINUS,

        KeyEvent.KEYCODE_Z to KeyEvent.KEYCODE_SEMICOLON,
        KeyEvent.KEYCODE_X to KeyEvent.KEYCODE_Q,
        KeyEvent.KEYCODE_C to KeyEvent.KEYCODE_J,
        KeyEvent.KEYCODE_V to KeyEvent.KEYCODE_K,
        KeyEvent.KEYCODE_B to KeyEvent.KEYCODE_X,
        KeyEvent.KEYCODE_N to KeyEvent.KEYCODE_B,
        KeyEvent.KEYCODE_M to KeyEvent.KEYCODE_M,
        KeyEvent.KEYCODE_COMMA to KeyEvent.KEYCODE_W,
        KeyEvent.KEYCODE_PERIOD to KeyEvent.KEYCODE_V,
        KeyEvent.KEYCODE_SLASH to KeyEvent.KEYCODE_Z,
    )

    val KEYCODE_MAP_COLEMAK = mapOf(
        KeyEvent.KEYCODE_Q to KeyEvent.KEYCODE_Q,
        KeyEvent.KEYCODE_W to KeyEvent.KEYCODE_W,
        KeyEvent.KEYCODE_E to KeyEvent.KEYCODE_F,
        KeyEvent.KEYCODE_R to KeyEvent.KEYCODE_P,
        KeyEvent.KEYCODE_T to KeyEvent.KEYCODE_G,
        KeyEvent.KEYCODE_Y to KeyEvent.KEYCODE_J,
        KeyEvent.KEYCODE_U to KeyEvent.KEYCODE_L,
        KeyEvent.KEYCODE_I to KeyEvent.KEYCODE_U,
        KeyEvent.KEYCODE_O to KeyEvent.KEYCODE_Y,
        KeyEvent.KEYCODE_P to KeyEvent.KEYCODE_SEMICOLON,

        KeyEvent.KEYCODE_A to KeyEvent.KEYCODE_A,
        KeyEvent.KEYCODE_S to KeyEvent.KEYCODE_R,
        KeyEvent.KEYCODE_D to KeyEvent.KEYCODE_S,
        KeyEvent.KEYCODE_F to KeyEvent.KEYCODE_T,
        KeyEvent.KEYCODE_G to KeyEvent.KEYCODE_D,
        KeyEvent.KEYCODE_H to KeyEvent.KEYCODE_H,
        KeyEvent.KEYCODE_J to KeyEvent.KEYCODE_N,
        KeyEvent.KEYCODE_K to KeyEvent.KEYCODE_E,
        KeyEvent.KEYCODE_L to KeyEvent.KEYCODE_I,
        KeyEvent.KEYCODE_SEMICOLON to KeyEvent.KEYCODE_O,

        KeyEvent.KEYCODE_Z to KeyEvent.KEYCODE_Z,
        KeyEvent.KEYCODE_X to KeyEvent.KEYCODE_X,
        KeyEvent.KEYCODE_C to KeyEvent.KEYCODE_C,
        KeyEvent.KEYCODE_V to KeyEvent.KEYCODE_V,
        KeyEvent.KEYCODE_B to KeyEvent.KEYCODE_B,
        KeyEvent.KEYCODE_N to KeyEvent.KEYCODE_K,
        KeyEvent.KEYCODE_M to KeyEvent.KEYCODE_M,
    )

}
