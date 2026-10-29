package ee.oyatl.ime.fusion.layout

import ee.oyatl.ime.fusion.keyboard.core.KeyCode

object LayoutSymbol {
    val TABLE_G = mapOf(
        KeyCode.KEYCODE_Q to listOf('1'.code, '~'.code),
        KeyCode.KEYCODE_W to listOf('2'.code, '`'.code),
        KeyCode.KEYCODE_E to listOf('3'.code, '|'.code),
        KeyCode.KEYCODE_R to listOf('4'.code, '•'.code),
        KeyCode.KEYCODE_T to listOf('5'.code, '√'.code),
        KeyCode.KEYCODE_Y to listOf('6'.code, 'π'.code),
        KeyCode.KEYCODE_U to listOf('7'.code, '÷'.code),
        KeyCode.KEYCODE_I to listOf('8'.code, '×'.code),
        KeyCode.KEYCODE_O to listOf('9'.code, '§'.code),
        KeyCode.KEYCODE_P to listOf('0'.code, '∆'.code),

        KeyCode.KEYCODE_A to listOf('@'.code, '£'.code),
        KeyCode.KEYCODE_S to listOf('#'.code, '₩'.code),
        KeyCode.KEYCODE_D to listOf('$'.code, '€'.code),
        KeyCode.KEYCODE_F to listOf('_'.code, '¥'.code),
        KeyCode.KEYCODE_G to listOf('&'.code, '^'.code),
        KeyCode.KEYCODE_H to listOf('-'.code, '°'.code),
        KeyCode.KEYCODE_J to listOf('+'.code, '='.code),
        KeyCode.KEYCODE_K to listOf('('.code, '{'.code),
        KeyCode.KEYCODE_L to listOf(')'.code, '}'.code),
        KeyCode.KEYCODE_SEMICOLON to listOf('/'.code, '\\'.code),

        KeyCode.KEYCODE_Z to listOf('*'.code, '%'.code),
        KeyCode.KEYCODE_X to listOf('"'.code, '©'.code),
        KeyCode.KEYCODE_C to listOf('\''.code, '®'.code),
        KeyCode.KEYCODE_V to listOf(':'.code, '™'.code),
        KeyCode.KEYCODE_B to listOf(';'.code, '✓'.code),
        KeyCode.KEYCODE_N to listOf('!'.code, '['.code),
        KeyCode.KEYCODE_M to listOf('?'.code, ']'.code)
    )

    val TABLE_N1 = mapOf(
        KeyCode.KEYCODE_Q to listOf('1'.code, '%'.code),
        KeyCode.KEYCODE_W to listOf('2'.code, '※'.code),
        KeyCode.KEYCODE_E to listOf('3'.code, '='.code),
        KeyCode.KEYCODE_R to listOf('4'.code, '&'.code),
        KeyCode.KEYCODE_T to listOf('5'.code, '*'.code),
        KeyCode.KEYCODE_Y to listOf('6'.code, '+'.code),
        KeyCode.KEYCODE_U to listOf('7'.code, '÷'.code),
        KeyCode.KEYCODE_I to listOf('8'.code, '×'.code),
        KeyCode.KEYCODE_O to listOf('9'.code, '<'.code),
        KeyCode.KEYCODE_P to listOf('0'.code, '>'.code),

        KeyCode.KEYCODE_A to listOf('@'.code, '$'.code),
        KeyCode.KEYCODE_S to listOf('#'.code, '₩'.code),
        KeyCode.KEYCODE_D to listOf(':'.code, '★'.code),
        KeyCode.KEYCODE_F to listOf(';'.code, '☆'.code),
        KeyCode.KEYCODE_G to listOf('^'.code, '♥'.code),
        KeyCode.KEYCODE_H to listOf('-'.code, '♡'.code),
        KeyCode.KEYCODE_J to listOf('_'.code, '|'.code),
        KeyCode.KEYCODE_K to listOf('/'.code, '\\'.code),
        KeyCode.KEYCODE_L to listOf('('.code, '{'.code),
        KeyCode.KEYCODE_SEMICOLON to listOf(')'.code, '}'.code),

        KeyCode.KEYCODE_Z to listOf('\''.code, '←'.code),
        KeyCode.KEYCODE_X to listOf('"'.code, '↑'.code),
        KeyCode.KEYCODE_C to listOf('~'.code, '↓'.code),
        KeyCode.KEYCODE_V to listOf('.'.code, '→'.code),
        KeyCode.KEYCODE_B to listOf(','.code, '·'.code),
        KeyCode.KEYCODE_N to listOf('!'.code, '['.code),
        KeyCode.KEYCODE_M to listOf('?'.code, ']'.code)
    )

    val TABLE_N2 = mapOf(
        KeyCode.KEYCODE_1 to listOf('1'.code, '1'.code),
        KeyCode.KEYCODE_2 to listOf('2'.code, '2'.code),
        KeyCode.KEYCODE_3 to listOf('3'.code, '3'.code),
        KeyCode.KEYCODE_4 to listOf('4'.code, '4'.code),
        KeyCode.KEYCODE_5 to listOf('5'.code, '5'.code),
        KeyCode.KEYCODE_6 to listOf('6'.code, '6'.code),
        KeyCode.KEYCODE_7 to listOf('7'.code, '7'.code),
        KeyCode.KEYCODE_8 to listOf('8'.code, '8'.code),
        KeyCode.KEYCODE_9 to listOf('9'.code, '9'.code),
        KeyCode.KEYCODE_0 to listOf('0'.code, '0'.code),

        KeyCode.KEYCODE_Q to listOf('%'.code, '≠'.code),
        KeyCode.KEYCODE_W to listOf('₩'.code, '≒'.code),
        KeyCode.KEYCODE_E to listOf('='.code, '÷'.code),
        KeyCode.KEYCODE_R to listOf('&'.code, '×'.code),
        KeyCode.KEYCODE_T to listOf('·'.code, '$'.code),
        KeyCode.KEYCODE_Y to listOf('*'.code, '￥'.code),
        KeyCode.KEYCODE_U to listOf('-'.code, '|'.code),
        KeyCode.KEYCODE_I to listOf('+'.code, '\\'.code),
        KeyCode.KEYCODE_O to listOf('<'.code, '{'.code),
        KeyCode.KEYCODE_P to listOf('>'.code, '}'.code),

        KeyCode.KEYCODE_A to listOf('@'.code, '○'.code),
        KeyCode.KEYCODE_S to listOf('#'.code, '●'.code),
        KeyCode.KEYCODE_D to listOf(':'.code, '□'.code),
        KeyCode.KEYCODE_F to listOf(';'.code, '■'.code),
        KeyCode.KEYCODE_G to listOf('^'.code, '※'.code),
        KeyCode.KEYCODE_H to listOf('♡'.code, '♥'.code),
        KeyCode.KEYCODE_J to listOf('_'.code, '☆'.code),
        KeyCode.KEYCODE_K to listOf('/'.code, '★'.code),
        KeyCode.KEYCODE_L to listOf('('.code, '['.code),
        KeyCode.KEYCODE_SEMICOLON to listOf(')'.code, ']'.code),

        KeyCode.KEYCODE_Z to listOf('\''.code, '←'.code),
        KeyCode.KEYCODE_X to listOf('"'.code, '↑'.code),
        KeyCode.KEYCODE_C to listOf('~'.code, '↓'.code),
        KeyCode.KEYCODE_V to listOf('.'.code, '→'.code),
        KeyCode.KEYCODE_B to listOf(','.code, '↔'.code),
        KeyCode.KEYCODE_N to listOf('!'.code, '«'.code),
        KeyCode.KEYCODE_M to listOf('?'.code, '»'.code)
    )

    val TABLE_O1 = mapOf(
        KeyCode.KEYCODE_Q to listOf('1'.code, '!'.code),
        KeyCode.KEYCODE_W to listOf('2'.code, '@'.code),
        KeyCode.KEYCODE_E to listOf('3'.code, '#'.code),
        KeyCode.KEYCODE_R to listOf('4'.code, '$'.code),
        KeyCode.KEYCODE_T to listOf('5'.code, '%'.code),
        KeyCode.KEYCODE_Y to listOf('6'.code, '^'.code),
        KeyCode.KEYCODE_U to listOf('7'.code, '&'.code),
        KeyCode.KEYCODE_I to listOf('8'.code, '*'.code),
        KeyCode.KEYCODE_O to listOf('9'.code, '('.code),
        KeyCode.KEYCODE_P to listOf('0'.code, ')'.code),

        KeyCode.KEYCODE_A to listOf('~'.code, '※'.code),
        KeyCode.KEYCODE_S to listOf('\''.code, '`'.code),
        KeyCode.KEYCODE_D to listOf('['.code, '{'.code),
        KeyCode.KEYCODE_F to listOf(']'.code, '}'.code),
        KeyCode.KEYCODE_G to listOf('/'.code, '\\'.code),
        KeyCode.KEYCODE_H to listOf('<'.code, '←'.code),
        KeyCode.KEYCODE_J to listOf('>'.code, '↓'.code),
        KeyCode.KEYCODE_K to listOf(':'.code, '↑'.code),
        KeyCode.KEYCODE_L to listOf(';'.code, '→'.code),

        KeyCode.KEYCODE_Z to listOf('_'.code, '|'.code),
        KeyCode.KEYCODE_X to listOf('·'.code, '√'.code),
        KeyCode.KEYCODE_C to listOf('='.code, '÷'.code),
        KeyCode.KEYCODE_V to listOf('+'.code, '×'.code),
        KeyCode.KEYCODE_B to listOf('?'.code, 'π'.code),
        KeyCode.KEYCODE_N to listOf('-'.code, '「'.code),
        KeyCode.KEYCODE_M to listOf('"'.code, '」'.code)
    )

    val TABLE_O2 = mapOf(
        KeyCode.KEYCODE_1 to listOf('1'.code, '①'.code),
        KeyCode.KEYCODE_2 to listOf('2'.code, '②'.code),
        KeyCode.KEYCODE_3 to listOf('3'.code, '③'.code),
        KeyCode.KEYCODE_4 to listOf('4'.code, '④'.code),
        KeyCode.KEYCODE_5 to listOf('5'.code, '⑤'.code),
        KeyCode.KEYCODE_6 to listOf('6'.code, '⑥'.code),
        KeyCode.KEYCODE_7 to listOf('7'.code, '⑦'.code),
        KeyCode.KEYCODE_8 to listOf('8'.code, '⑧'.code),
        KeyCode.KEYCODE_9 to listOf('9'.code, '⑨'.code),
        KeyCode.KEYCODE_0 to listOf('0'.code, '⓪'.code),

        KeyCode.KEYCODE_Q to listOf('!'.code, '○'.code),
        KeyCode.KEYCODE_W to listOf('@'.code, '●'.code),
        KeyCode.KEYCODE_E to listOf('#'.code, '◎'.code),
        KeyCode.KEYCODE_R to listOf('$'.code, '□'.code),
        KeyCode.KEYCODE_T to listOf('%'.code, '■'.code),
        KeyCode.KEYCODE_Y to listOf('^'.code, '♡'.code),
        KeyCode.KEYCODE_U to listOf('&'.code, '♥'.code),
        KeyCode.KEYCODE_I to listOf('*'.code, '☆'.code),
        KeyCode.KEYCODE_O to listOf('('.code, '★'.code),
        KeyCode.KEYCODE_P to listOf(')'.code, '₩'.code),

        KeyCode.KEYCODE_A to listOf('~'.code, '※'.code),
        KeyCode.KEYCODE_S to listOf('\''.code, '`'.code),
        KeyCode.KEYCODE_D to listOf('['.code, '{'.code),
        KeyCode.KEYCODE_F to listOf(']'.code, '}'.code),
        KeyCode.KEYCODE_G to listOf('/'.code, '\\'.code),
        KeyCode.KEYCODE_H to listOf('<'.code, '←'.code),
        KeyCode.KEYCODE_J to listOf('>'.code, '↓'.code),
        KeyCode.KEYCODE_K to listOf(':'.code, '↑'.code),
        KeyCode.KEYCODE_L to listOf(';'.code, '→'.code),

        KeyCode.KEYCODE_Z to listOf('_'.code, '|'.code),
        KeyCode.KEYCODE_X to listOf('·'.code, '√'.code),
        KeyCode.KEYCODE_C to listOf('='.code, '÷'.code),
        KeyCode.KEYCODE_V to listOf('+'.code, '×'.code),
        KeyCode.KEYCODE_B to listOf('?'.code, 'π'.code),
        KeyCode.KEYCODE_N to listOf('-'.code, '「'.code),
        KeyCode.KEYCODE_M to listOf('"'.code, '」'.code)
    )

    val KEYCODE_MAP_SYMBOLS = mapOf(
        KeyCode.KEYCODE_LANGUAGE_SWITCH to KeyCode.KEYCODE_NUM
    )
}