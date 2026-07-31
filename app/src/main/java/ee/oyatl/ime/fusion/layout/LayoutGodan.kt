package ee.oyatl.ime.fusion.layout

import android.view.KeyEvent
import ee.oyatl.ime.keyboard.FlickKeyCode
import ee.oyatl.ime.keyboard.KeyboardConfiguration

/**
 * Gboard-style GODAN layout.
 *
 * Mozc performs the actual kana composition through
 * KeyboardSpecification.GODAN_KANA / GODAN_TO_HIRAGANA.
 * This table only maps taps and flicks to the ASCII key codes expected by Mozc.
 */
object LayoutGodan {
    private fun key(code: Int, width: Float = 1f, special: Boolean = false) =
        KeyboardConfiguration.Item.TemplateKey(code, width, special)
    fun mobileKeyboardConfiguration(): KeyboardConfiguration {
    return KeyboardConfiguration(
        listOf(
            listOf(
                key(KeyEvent.KEYCODE_NUM, special = true),
                key(KeyEvent.KEYCODE_A),
                key(KeyEvent.KEYCODE_K),
                key(KeyEvent.KEYCODE_H),
                key(KeyEvent.KEYCODE_DEL, special = true)
            ),
            listOf(
                key(KeyEvent.KEYCODE_DPAD_LEFT, special = true),
                key(KeyEvent.KEYCODE_I),
                key(KeyEvent.KEYCODE_S),
                key(KeyEvent.KEYCODE_M),
                key(KeyEvent.KEYCODE_DPAD_RIGHT, special = true)
            ),
            listOf(
                key(KeyEvent.KEYCODE_LANGUAGE_SWITCH, special = true),
                key(KeyEvent.KEYCODE_U),
                key(KeyEvent.KEYCODE_T),
                key(KeyEvent.KEYCODE_Y),
                key(KeyEvent.KEYCODE_SPACE, special = true)
            ),
            listOf(
                key(KeyEvent.KEYCODE_SYM, special = true),
                key(KeyEvent.KEYCODE_E),
                key(KeyEvent.KEYCODE_N),
                key(KeyEvent.KEYCODE_R),
                key(KeyEvent.KEYCODE_ENTER, special = true)
            ),
            listOf(
                key(KeyEvent.KEYCODE_SYM, special = true),
                key(KeyEvent.KEYCODE_O),
                key(KeyEvent.KEYCODE_LANGUAGE_SWITCH, special = true),
                key(KeyEvent.KEYCODE_W),
                key(KeyEvent.KEYCODE_ENTER, special = true)
            )
        )
    )
}

    val TABLE: Map<Int, List<Int>> = mapOf(
        // Main 14 Roman-letter keys.

        // Vowel keys.
        // Tap: normal vowel
        // Left: vowel + small tsu
        // Up: vowel + n
        // Right: small vowel / contracted sound
        KeyEvent.KEYCODE_A or FlickKeyCode.DIRECTION_NONE to listOf('a'.code),
        KeyEvent.KEYCODE_A or FlickKeyCode.DIRECTION_LEFT to listOf('$'.code),
        KeyEvent.KEYCODE_A or FlickKeyCode.DIRECTION_UP to listOf('<'.code),
        KeyEvent.KEYCODE_A or FlickKeyCode.DIRECTION_RIGHT to listOf('#'.code),

        KeyEvent.KEYCODE_I or FlickKeyCode.DIRECTION_NONE to listOf('i'.code),
        KeyEvent.KEYCODE_I or FlickKeyCode.DIRECTION_LEFT to listOf('%'.code),
        KeyEvent.KEYCODE_I or FlickKeyCode.DIRECTION_UP to listOf('>'.code),
        KeyEvent.KEYCODE_I or FlickKeyCode.DIRECTION_RIGHT to listOf('+'.code),

        KeyEvent.KEYCODE_U or FlickKeyCode.DIRECTION_NONE to listOf('u'.code),
        KeyEvent.KEYCODE_U or FlickKeyCode.DIRECTION_LEFT to listOf('&'.code),
        KeyEvent.KEYCODE_U or FlickKeyCode.DIRECTION_UP to listOf('{'.code),
        KeyEvent.KEYCODE_U or FlickKeyCode.DIRECTION_RIGHT to listOf('^'.code),

        KeyEvent.KEYCODE_E or FlickKeyCode.DIRECTION_NONE to listOf('e'.code),
        KeyEvent.KEYCODE_E or FlickKeyCode.DIRECTION_LEFT to listOf('='.code),
        KeyEvent.KEYCODE_E or FlickKeyCode.DIRECTION_UP to listOf('}'.code),
        KeyEvent.KEYCODE_E or FlickKeyCode.DIRECTION_RIGHT to listOf('_'.code),

        KeyEvent.KEYCODE_O or FlickKeyCode.DIRECTION_NONE to listOf('o'.code),
        KeyEvent.KEYCODE_O or FlickKeyCode.DIRECTION_LEFT to listOf('@'.code),
        KeyEvent.KEYCODE_O or FlickKeyCode.DIRECTION_UP to listOf('~'.code),
        KeyEvent.KEYCODE_O or FlickKeyCode.DIRECTION_RIGHT to listOf('|'.code),

        KeyEvent.KEYCODE_K or FlickKeyCode.DIRECTION_NONE to listOf('k'.code),
        KeyEvent.KEYCODE_H or FlickKeyCode.DIRECTION_NONE to listOf('h'.code),
        KeyEvent.KEYCODE_S or FlickKeyCode.DIRECTION_NONE to listOf('s'.code),
        KeyEvent.KEYCODE_M or FlickKeyCode.DIRECTION_NONE to listOf('m'.code),
        KeyEvent.KEYCODE_T or FlickKeyCode.DIRECTION_NONE to listOf('t'.code),
        KeyEvent.KEYCODE_Y or FlickKeyCode.DIRECTION_NONE to listOf('y'.code),
        KeyEvent.KEYCODE_N or FlickKeyCode.DIRECTION_NONE to listOf('n'.code),
        KeyEvent.KEYCODE_R or FlickKeyCode.DIRECTION_NONE to listOf('r'.code),
        KeyEvent.KEYCODE_W or FlickKeyCode.DIRECTION_NONE to listOf('w'.code),

        // GODAN auxiliary consonants and symbols.
        KeyEvent.KEYCODE_K or FlickKeyCode.DIRECTION_UP to listOf('q'.code),
        KeyEvent.KEYCODE_K or FlickKeyCode.DIRECTION_RIGHT to listOf('g'.code),

        KeyEvent.KEYCODE_H or FlickKeyCode.DIRECTION_LEFT to listOf('p'.code),
        KeyEvent.KEYCODE_H or FlickKeyCode.DIRECTION_UP to listOf('f'.code),
        KeyEvent.KEYCODE_H or FlickKeyCode.DIRECTION_RIGHT to listOf('b'.code),

        KeyEvent.KEYCODE_S or FlickKeyCode.DIRECTION_UP to listOf('j'.code),
        KeyEvent.KEYCODE_S or FlickKeyCode.DIRECTION_RIGHT to listOf('z'.code),

        KeyEvent.KEYCODE_M or FlickKeyCode.DIRECTION_LEFT to listOf('/'.code),
        KeyEvent.KEYCODE_M or FlickKeyCode.DIRECTION_UP to listOf('l'.code),
        KeyEvent.KEYCODE_M or FlickKeyCode.DIRECTION_RIGHT to listOf('-'.code),

        KeyEvent.KEYCODE_T or FlickKeyCode.DIRECTION_UP to listOf('c'.code),
        KeyEvent.KEYCODE_T or FlickKeyCode.DIRECTION_RIGHT to listOf('d'.code),

        KeyEvent.KEYCODE_Y or FlickKeyCode.DIRECTION_LEFT to listOf('('.code),
        KeyEvent.KEYCODE_Y or FlickKeyCode.DIRECTION_UP to listOf('x'.code),
        KeyEvent.KEYCODE_Y or FlickKeyCode.DIRECTION_RIGHT to listOf(')'.code),

        KeyEvent.KEYCODE_N or FlickKeyCode.DIRECTION_LEFT to listOf(':'.code),
        KeyEvent.KEYCODE_N or FlickKeyCode.DIRECTION_RIGHT to listOf('・'.code),

        KeyEvent.KEYCODE_R or FlickKeyCode.DIRECTION_UP to listOf('?'.code),
        KeyEvent.KEYCODE_R or FlickKeyCode.DIRECTION_RIGHT to listOf('!'.code),

        KeyEvent.KEYCODE_W or FlickKeyCode.DIRECTION_DOWN to listOf('v'.code),

        // Telephone-layout numbers on downward flicks.
        KeyEvent.KEYCODE_A or FlickKeyCode.DIRECTION_DOWN to listOf('1'.code),
        KeyEvent.KEYCODE_K or FlickKeyCode.DIRECTION_DOWN to listOf('2'.code),
        KeyEvent.KEYCODE_H or FlickKeyCode.DIRECTION_DOWN to listOf('3'.code),

        KeyEvent.KEYCODE_I or FlickKeyCode.DIRECTION_DOWN to listOf('4'.code),
        KeyEvent.KEYCODE_S or FlickKeyCode.DIRECTION_DOWN to listOf('5'.code),
        KeyEvent.KEYCODE_M or FlickKeyCode.DIRECTION_DOWN to listOf('6'.code),

        KeyEvent.KEYCODE_U or FlickKeyCode.DIRECTION_DOWN to listOf('7'.code),
        KeyEvent.KEYCODE_T or FlickKeyCode.DIRECTION_DOWN to listOf('8'.code),
        KeyEvent.KEYCODE_Y or FlickKeyCode.DIRECTION_DOWN to listOf('9'.code),

        KeyEvent.KEYCODE_N or FlickKeyCode.DIRECTION_DOWN to listOf('0'.code),

        KeyEvent.KEYCODE_R or FlickKeyCode.DIRECTION_DOWN to listOf('、'.code),
        KeyEvent.KEYCODE_R or FlickKeyCode.DIRECTION_LEFT to listOf('。'.code),
        KeyEvent.KEYCODE_W or FlickKeyCode.DIRECTION_LEFT to listOf('「'.code),
        KeyEvent.KEYCODE_W or FlickKeyCode.DIRECTION_RIGHT to listOf('」'.code)

    )

    val LABELS: Map<Int, String> = mapOf(
        KeyEvent.KEYCODE_A or FlickKeyCode.DIRECTION_NONE to "A",
        KeyEvent.KEYCODE_A or FlickKeyCode.DIRECTION_LEFT to "あっ",
        KeyEvent.KEYCODE_A or FlickKeyCode.DIRECTION_UP to "あん",
        KeyEvent.KEYCODE_A or FlickKeyCode.DIRECTION_RIGHT to "ゃ",
        KeyEvent.KEYCODE_A or FlickKeyCode.DIRECTION_DOWN to "1",

        KeyEvent.KEYCODE_I or FlickKeyCode.DIRECTION_NONE to "I",
        KeyEvent.KEYCODE_I or FlickKeyCode.DIRECTION_LEFT to "いっ",
        KeyEvent.KEYCODE_I or FlickKeyCode.DIRECTION_UP to "いん",
        KeyEvent.KEYCODE_I or FlickKeyCode.DIRECTION_RIGHT to "ぃ",
        KeyEvent.KEYCODE_I or FlickKeyCode.DIRECTION_DOWN to "4",

        KeyEvent.KEYCODE_U or FlickKeyCode.DIRECTION_NONE to "U",
        KeyEvent.KEYCODE_U or FlickKeyCode.DIRECTION_LEFT to "うっ",
        KeyEvent.KEYCODE_U or FlickKeyCode.DIRECTION_UP to "うん",
        KeyEvent.KEYCODE_U or FlickKeyCode.DIRECTION_RIGHT to "ゅ",
        KeyEvent.KEYCODE_U or FlickKeyCode.DIRECTION_DOWN to "7",

        KeyEvent.KEYCODE_E or FlickKeyCode.DIRECTION_NONE to "E",
        KeyEvent.KEYCODE_E or FlickKeyCode.DIRECTION_LEFT to "えっ",
        KeyEvent.KEYCODE_E or FlickKeyCode.DIRECTION_UP to "えん",
        KeyEvent.KEYCODE_E or FlickKeyCode.DIRECTION_RIGHT to "ぇ",

        KeyEvent.KEYCODE_O or FlickKeyCode.DIRECTION_NONE to "O",
        KeyEvent.KEYCODE_O or FlickKeyCode.DIRECTION_LEFT to "おっ",
        KeyEvent.KEYCODE_O or FlickKeyCode.DIRECTION_UP to "おん",
        KeyEvent.KEYCODE_O or FlickKeyCode.DIRECTION_RIGHT to "ょ",

        KeyEvent.KEYCODE_K or FlickKeyCode.DIRECTION_NONE to "K",
        KeyEvent.KEYCODE_K or FlickKeyCode.DIRECTION_UP to "Q",
        KeyEvent.KEYCODE_K or FlickKeyCode.DIRECTION_RIGHT to "G",

        KeyEvent.KEYCODE_H or FlickKeyCode.DIRECTION_NONE to "H",
        KeyEvent.KEYCODE_H or FlickKeyCode.DIRECTION_LEFT to "P",
        KeyEvent.KEYCODE_H or FlickKeyCode.DIRECTION_UP to "F",
        KeyEvent.KEYCODE_H or FlickKeyCode.DIRECTION_RIGHT to "B",

        KeyEvent.KEYCODE_S or FlickKeyCode.DIRECTION_NONE to "S",
        KeyEvent.KEYCODE_S or FlickKeyCode.DIRECTION_UP to "J",
        KeyEvent.KEYCODE_S or FlickKeyCode.DIRECTION_RIGHT to "Z",

        KeyEvent.KEYCODE_M or FlickKeyCode.DIRECTION_NONE to "M",
        KeyEvent.KEYCODE_M or FlickKeyCode.DIRECTION_LEFT to "/",
        KeyEvent.KEYCODE_M or FlickKeyCode.DIRECTION_UP to "L",
        KeyEvent.KEYCODE_M or FlickKeyCode.DIRECTION_RIGHT to "−",

        KeyEvent.KEYCODE_T or FlickKeyCode.DIRECTION_NONE to "T",
        KeyEvent.KEYCODE_T or FlickKeyCode.DIRECTION_UP to "C",
        KeyEvent.KEYCODE_T or FlickKeyCode.DIRECTION_RIGHT to "D",

        KeyEvent.KEYCODE_Y or FlickKeyCode.DIRECTION_NONE to "Y",
        KeyEvent.KEYCODE_Y or FlickKeyCode.DIRECTION_LEFT to "（",
        KeyEvent.KEYCODE_Y or FlickKeyCode.DIRECTION_UP to "X",
        KeyEvent.KEYCODE_Y or FlickKeyCode.DIRECTION_RIGHT to "）",

        KeyEvent.KEYCODE_N or FlickKeyCode.DIRECTION_NONE to "N",
        KeyEvent.KEYCODE_N or FlickKeyCode.DIRECTION_LEFT to "：",
        KeyEvent.KEYCODE_N or FlickKeyCode.DIRECTION_RIGHT to "・",

        KeyEvent.KEYCODE_R or FlickKeyCode.DIRECTION_NONE to "R",
        KeyEvent.KEYCODE_R or FlickKeyCode.DIRECTION_UP to "？",
        KeyEvent.KEYCODE_R or FlickKeyCode.DIRECTION_RIGHT to "！",

        KeyEvent.KEYCODE_W or FlickKeyCode.DIRECTION_NONE to "W",
        KeyEvent.KEYCODE_W or FlickKeyCode.DIRECTION_DOWN to "V",

        KeyEvent.KEYCODE_A or FlickKeyCode.DIRECTION_DOWN to "1",
        KeyEvent.KEYCODE_K or FlickKeyCode.DIRECTION_DOWN to "2",
        KeyEvent.KEYCODE_H or FlickKeyCode.DIRECTION_DOWN to "3",

        KeyEvent.KEYCODE_I or FlickKeyCode.DIRECTION_DOWN to "4",
        KeyEvent.KEYCODE_S or FlickKeyCode.DIRECTION_DOWN to "5",
        KeyEvent.KEYCODE_M or FlickKeyCode.DIRECTION_DOWN to "6",

        KeyEvent.KEYCODE_U or FlickKeyCode.DIRECTION_DOWN to "7",
        KeyEvent.KEYCODE_T or FlickKeyCode.DIRECTION_DOWN to "8",
        KeyEvent.KEYCODE_Y or FlickKeyCode.DIRECTION_DOWN to "9",

        KeyEvent.KEYCODE_N or FlickKeyCode.DIRECTION_DOWN to "0",

        KeyEvent.KEYCODE_R or FlickKeyCode.DIRECTION_DOWN to "、",
        KeyEvent.KEYCODE_R or FlickKeyCode.DIRECTION_LEFT to "。",
        KeyEvent.KEYCODE_W or FlickKeyCode.DIRECTION_LEFT to "「",
        KeyEvent.KEYCODE_W or FlickKeyCode.DIRECTION_RIGHT to "」",

    )
}
