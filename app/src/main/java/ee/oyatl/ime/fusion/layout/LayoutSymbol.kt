package ee.oyatl.ime.fusion.layout

import android.view.KeyEvent

object LayoutSymbol {
    val TABLE_G = mapOf(
        KeyEvent.KEYCODE_Q to listOf('1'.code, '~'.code),
        KeyEvent.KEYCODE_W to listOf('2'.code, '`'.code),
        KeyEvent.KEYCODE_E to listOf('3'.code, '|'.code),
        KeyEvent.KEYCODE_R to listOf('4'.code, '•'.code),
        KeyEvent.KEYCODE_T to listOf('5'.code, '√'.code),
        KeyEvent.KEYCODE_Y to listOf('6'.code, 'π'.code),
        KeyEvent.KEYCODE_U to listOf('7'.code, '÷'.code),
        KeyEvent.KEYCODE_I to listOf('8'.code, '×'.code),
        KeyEvent.KEYCODE_O to listOf('9'.code, '§'.code),
        KeyEvent.KEYCODE_P to listOf('0'.code, '∆'.code),

        KeyEvent.KEYCODE_A to listOf('@'.code, '£'.code),
        KeyEvent.KEYCODE_S to listOf('#'.code, '₩'.code),
        KeyEvent.KEYCODE_D to listOf('$'.code, '€'.code),
        KeyEvent.KEYCODE_F to listOf('_'.code, '¥'.code),
        KeyEvent.KEYCODE_G to listOf('&'.code, '^'.code),
        KeyEvent.KEYCODE_H to listOf('-'.code, '°'.code),
        KeyEvent.KEYCODE_J to listOf('+'.code, '='.code),
        KeyEvent.KEYCODE_K to listOf('('.code, '{'.code),
        KeyEvent.KEYCODE_L to listOf(')'.code, '}'.code),
        KeyEvent.KEYCODE_SEMICOLON to listOf('/'.code, '\\'.code),

        KeyEvent.KEYCODE_Z to listOf('*'.code, '%'.code),
        KeyEvent.KEYCODE_X to listOf('"'.code, '©'.code),
        KeyEvent.KEYCODE_C to listOf('\''.code, '®'.code),
        KeyEvent.KEYCODE_V to listOf(':'.code, '™'.code),
        KeyEvent.KEYCODE_B to listOf(';'.code, '✓'.code),
        KeyEvent.KEYCODE_N to listOf('!'.code, '['.code),
        KeyEvent.KEYCODE_M to listOf('?'.code, ']'.code)
    )

    val TABLE_N1 = mapOf(
        KeyEvent.KEYCODE_Q to listOf('1'.code, '%'.code),
        KeyEvent.KEYCODE_W to listOf('2'.code, '※'.code),
        KeyEvent.KEYCODE_E to listOf('3'.code, '='.code),
        KeyEvent.KEYCODE_R to listOf('4'.code, '&'.code),
        KeyEvent.KEYCODE_T to listOf('5'.code, '*'.code),
        KeyEvent.KEYCODE_Y to listOf('6'.code, '+'.code),
        KeyEvent.KEYCODE_U to listOf('7'.code, '÷'.code),
        KeyEvent.KEYCODE_I to listOf('8'.code, '×'.code),
        KeyEvent.KEYCODE_O to listOf('9'.code, '<'.code),
        KeyEvent.KEYCODE_P to listOf('0'.code, '>'.code),

        KeyEvent.KEYCODE_A to listOf('@'.code, '$'.code),
        KeyEvent.KEYCODE_S to listOf('#'.code, '₩'.code),
        KeyEvent.KEYCODE_D to listOf(':'.code, '★'.code),
        KeyEvent.KEYCODE_F to listOf(';'.code, '☆'.code),
        KeyEvent.KEYCODE_G to listOf('^'.code, '♥'.code),
        KeyEvent.KEYCODE_H to listOf('-'.code, '♡'.code),
        KeyEvent.KEYCODE_J to listOf('_'.code, '|'.code),
        KeyEvent.KEYCODE_K to listOf('/'.code, '\\'.code),
        KeyEvent.KEYCODE_L to listOf('('.code, '{'.code),
        KeyEvent.KEYCODE_SEMICOLON to listOf(')'.code, '}'.code),

        KeyEvent.KEYCODE_Z to listOf('\''.code, '←'.code),
        KeyEvent.KEYCODE_X to listOf('"'.code, '↑'.code),
        KeyEvent.KEYCODE_C to listOf('~'.code, '↓'.code),
        KeyEvent.KEYCODE_V to listOf('.'.code, '→'.code),
        KeyEvent.KEYCODE_B to listOf(','.code, '·'.code),
        KeyEvent.KEYCODE_N to listOf('!'.code, '['.code),
        KeyEvent.KEYCODE_M to listOf('?'.code, ']'.code)
    )

    val TABLE_N2 = mapOf(
        KeyEvent.KEYCODE_1 to listOf('1'.code, '1'.code),
        KeyEvent.KEYCODE_2 to listOf('2'.code, '2'.code),
        KeyEvent.KEYCODE_3 to listOf('3'.code, '3'.code),
        KeyEvent.KEYCODE_4 to listOf('4'.code, '4'.code),
        KeyEvent.KEYCODE_5 to listOf('5'.code, '5'.code),
        KeyEvent.KEYCODE_6 to listOf('6'.code, '6'.code),
        KeyEvent.KEYCODE_7 to listOf('7'.code, '7'.code),
        KeyEvent.KEYCODE_8 to listOf('8'.code, '8'.code),
        KeyEvent.KEYCODE_9 to listOf('9'.code, '9'.code),
        KeyEvent.KEYCODE_0 to listOf('0'.code, '0'.code),

        KeyEvent.KEYCODE_Q to listOf('%'.code, '≠'.code),
        KeyEvent.KEYCODE_W to listOf('₩'.code, '≒'.code),
        KeyEvent.KEYCODE_E to listOf('='.code, '÷'.code),
        KeyEvent.KEYCODE_R to listOf('&'.code, '×'.code),
        KeyEvent.KEYCODE_T to listOf('·'.code, '$'.code),
        KeyEvent.KEYCODE_Y to listOf('*'.code, '￥'.code),
        KeyEvent.KEYCODE_U to listOf('-'.code, '|'.code),
        KeyEvent.KEYCODE_I to listOf('+'.code, '\\'.code),
        KeyEvent.KEYCODE_O to listOf('<'.code, '{'.code),
        KeyEvent.KEYCODE_P to listOf('>'.code, '}'.code),

        KeyEvent.KEYCODE_A to listOf('@'.code, '○'.code),
        KeyEvent.KEYCODE_S to listOf('#'.code, '●'.code),
        KeyEvent.KEYCODE_D to listOf(':'.code, '□'.code),
        KeyEvent.KEYCODE_F to listOf(';'.code, '■'.code),
        KeyEvent.KEYCODE_G to listOf('^'.code, '※'.code),
        KeyEvent.KEYCODE_H to listOf('♡'.code, '♥'.code),
        KeyEvent.KEYCODE_J to listOf('_'.code, '☆'.code),
        KeyEvent.KEYCODE_K to listOf('/'.code, '★'.code),
        KeyEvent.KEYCODE_L to listOf('('.code, '['.code),
        KeyEvent.KEYCODE_SEMICOLON to listOf(')'.code, ']'.code),

        KeyEvent.KEYCODE_Z to listOf('\''.code, '←'.code),
        KeyEvent.KEYCODE_X to listOf('"'.code, '↑'.code),
        KeyEvent.KEYCODE_C to listOf('~'.code, '↓'.code),
        KeyEvent.KEYCODE_V to listOf('.'.code, '→'.code),
        KeyEvent.KEYCODE_B to listOf(','.code, '↔'.code),
        KeyEvent.KEYCODE_N to listOf('!'.code, '«'.code),
        KeyEvent.KEYCODE_M to listOf('?'.code, '»'.code)
    )

    val TABLE_O1 = mapOf(
        KeyEvent.KEYCODE_Q to listOf('1'.code, '!'.code),
        KeyEvent.KEYCODE_W to listOf('2'.code, '@'.code),
        KeyEvent.KEYCODE_E to listOf('3'.code, '#'.code),
        KeyEvent.KEYCODE_R to listOf('4'.code, '$'.code),
        KeyEvent.KEYCODE_T to listOf('5'.code, '%'.code),
        KeyEvent.KEYCODE_Y to listOf('6'.code, '^'.code),
        KeyEvent.KEYCODE_U to listOf('7'.code, '&'.code),
        KeyEvent.KEYCODE_I to listOf('8'.code, '*'.code),
        KeyEvent.KEYCODE_O to listOf('9'.code, '('.code),
        KeyEvent.KEYCODE_P to listOf('0'.code, ')'.code),

        KeyEvent.KEYCODE_A to listOf('~'.code, '※'.code),
        KeyEvent.KEYCODE_S to listOf('\''.code, '`'.code),
        KeyEvent.KEYCODE_D to listOf('['.code, '{'.code),
        KeyEvent.KEYCODE_F to listOf(']'.code, '}'.code),
        KeyEvent.KEYCODE_G to listOf('/'.code, '\\'.code),
        KeyEvent.KEYCODE_H to listOf('<'.code, '←'.code),
        KeyEvent.KEYCODE_J to listOf('>'.code, '↓'.code),
        KeyEvent.KEYCODE_K to listOf(':'.code, '↑'.code),
        KeyEvent.KEYCODE_L to listOf(';'.code, '→'.code),

        KeyEvent.KEYCODE_Z to listOf('_'.code, '|'.code),
        KeyEvent.KEYCODE_X to listOf('·'.code, '√'.code),
        KeyEvent.KEYCODE_C to listOf('='.code, '÷'.code),
        KeyEvent.KEYCODE_V to listOf('+'.code, '×'.code),
        KeyEvent.KEYCODE_B to listOf('?'.code, 'π'.code),
        KeyEvent.KEYCODE_N to listOf('-'.code, '「'.code),
        KeyEvent.KEYCODE_M to listOf('"'.code, '」'.code)
    )

    val TABLE_O2 = mapOf(
        KeyEvent.KEYCODE_1 to listOf('1'.code, '①'.code),
        KeyEvent.KEYCODE_2 to listOf('2'.code, '②'.code),
        KeyEvent.KEYCODE_3 to listOf('3'.code, '③'.code),
        KeyEvent.KEYCODE_4 to listOf('4'.code, '④'.code),
        KeyEvent.KEYCODE_5 to listOf('5'.code, '⑤'.code),
        KeyEvent.KEYCODE_6 to listOf('6'.code, '⑥'.code),
        KeyEvent.KEYCODE_7 to listOf('7'.code, '⑦'.code),
        KeyEvent.KEYCODE_8 to listOf('8'.code, '⑧'.code),
        KeyEvent.KEYCODE_9 to listOf('9'.code, '⑨'.code),
        KeyEvent.KEYCODE_0 to listOf('0'.code, '⓪'.code),

        KeyEvent.KEYCODE_Q to listOf('!'.code, '○'.code),
        KeyEvent.KEYCODE_W to listOf('@'.code, '●'.code),
        KeyEvent.KEYCODE_E to listOf('#'.code, '◎'.code),
        KeyEvent.KEYCODE_R to listOf('$'.code, '□'.code),
        KeyEvent.KEYCODE_T to listOf('%'.code, '■'.code),
        KeyEvent.KEYCODE_Y to listOf('^'.code, '♡'.code),
        KeyEvent.KEYCODE_U to listOf('&'.code, '♥'.code),
        KeyEvent.KEYCODE_I to listOf('*'.code, '☆'.code),
        KeyEvent.KEYCODE_O to listOf('('.code, '★'.code),
        KeyEvent.KEYCODE_P to listOf(')'.code, '₩'.code),

        KeyEvent.KEYCODE_A to listOf('~'.code, '※'.code),
        KeyEvent.KEYCODE_S to listOf('\''.code, '`'.code),
        KeyEvent.KEYCODE_D to listOf('['.code, '{'.code),
        KeyEvent.KEYCODE_F to listOf(']'.code, '}'.code),
        KeyEvent.KEYCODE_G to listOf('/'.code, '\\'.code),
        KeyEvent.KEYCODE_H to listOf('<'.code, '←'.code),
        KeyEvent.KEYCODE_J to listOf('>'.code, '↓'.code),
        KeyEvent.KEYCODE_K to listOf(':'.code, '↑'.code),
        KeyEvent.KEYCODE_L to listOf(';'.code, '→'.code),

        KeyEvent.KEYCODE_Z to listOf('_'.code, '|'.code),
        KeyEvent.KEYCODE_X to listOf('·'.code, '√'.code),
        KeyEvent.KEYCODE_C to listOf('='.code, '÷'.code),
        KeyEvent.KEYCODE_V to listOf('+'.code, '×'.code),
        KeyEvent.KEYCODE_B to listOf('?'.code, 'π'.code),
        KeyEvent.KEYCODE_N to listOf('-'.code, '「'.code),
        KeyEvent.KEYCODE_M to listOf('"'.code, '」'.code)
    )
}