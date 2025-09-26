package ee.oyatl.ime.keyboard.layout

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
}