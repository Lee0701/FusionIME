package ee.oyatl.ime.fusion.layout

import android.view.KeyEvent
import ee.oyatl.ime.keyboard.KeyLabel
import ee.oyatl.ime.keyboard.KeyboardConfiguration

import ee.oyatl.ime.keyboard.KeyboardConfiguration.Item.TemplateKey

/**
 * Gboard-style GODAN layout.
 *
 * Mozc performs the actual kana composition through
 * KeyboardSpecification.GODAN_KANA / GODAN_TO_HIRAGANA.
 * This table only maps taps and flicks to the ASCII key codes expected by Mozc.
 */
object LayoutGodan {
    fun mobileKeyboardConfiguration(): KeyboardConfiguration {
    return KeyboardConfiguration(
        listOf(
            listOf(
                TemplateKey(KeyEvent.KEYCODE_NUM, special = true),
                TemplateKey(KeyEvent.KEYCODE_A),
                TemplateKey(KeyEvent.KEYCODE_K),
                TemplateKey(KeyEvent.KEYCODE_H),
                TemplateKey(KeyEvent.KEYCODE_DEL, special = true)
            ),
            listOf(
                TemplateKey(KeyEvent.KEYCODE_DPAD_LEFT, special = true),
                TemplateKey(KeyEvent.KEYCODE_I),
                TemplateKey(KeyEvent.KEYCODE_S),
                TemplateKey(KeyEvent.KEYCODE_M),
                TemplateKey(KeyEvent.KEYCODE_DPAD_RIGHT, special = true)
            ),
            listOf(
                TemplateKey(KeyEvent.KEYCODE_LANGUAGE_SWITCH, special = true),
                TemplateKey(KeyEvent.KEYCODE_U),
                TemplateKey(KeyEvent.KEYCODE_T),
                TemplateKey(KeyEvent.KEYCODE_Y),
                TemplateKey(KeyEvent.KEYCODE_SPACE, special = true)
            ),
            listOf(
                TemplateKey(KeyEvent.KEYCODE_SYM, special = true, merge = TemplateKey.Merge.Down, iconRes = 0),
                TemplateKey(KeyEvent.KEYCODE_E),
                TemplateKey(KeyEvent.KEYCODE_N),
                TemplateKey(KeyEvent.KEYCODE_R),
                TemplateKey(KeyEvent.KEYCODE_ENTER, special = true, merge = TemplateKey.Merge.Down, iconRes = 0)
            ),
            listOf(
                TemplateKey(KeyEvent.KEYCODE_SYM, special = true, merge = TemplateKey.Merge.Up),
                TemplateKey(KeyEvent.KEYCODE_O),
                KeyboardConfiguration.Item.Spacer(width = 1f),
                TemplateKey(KeyEvent.KEYCODE_W),
                TemplateKey(KeyEvent.KEYCODE_ENTER, special = true, merge = TemplateKey.Merge.Up)
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
        KeyEvent.KEYCODE_A to listOf('a'.code, '<'.code, '1'.code, '$'.code, '#'.code),
        KeyEvent.KEYCODE_I to listOf('i'.code, '>'.code, '4'.code, '%'.code, '+'.code),
        KeyEvent.KEYCODE_U to listOf('u'.code, '{'.code, '7'.code, '&'.code, '^'.code),
        KeyEvent.KEYCODE_E to listOf('e'.code, '}'.code, 0, '='.code, '_'.code),
        KeyEvent.KEYCODE_O to listOf('o'.code, '~'.code, 0, '@'.code, '|'.code),

        // Keys with GODAN auxiliary consonants and symbols.
        // Telephone-layout numbers on downward flicks.
        KeyEvent.KEYCODE_K to listOf('k'.code, 'q'.code, '2'.code, 0, 'g'.code),
        KeyEvent.KEYCODE_H to listOf('h'.code, 'f'.code, '3'.code, 'p'.code, 'b'.code),
        KeyEvent.KEYCODE_S to listOf('s'.code, 'j'.code, '5'.code, 0, 'z'.code),
        KeyEvent.KEYCODE_M to listOf('m'.code, 'l'.code, '6'.code, '/'.code, '-'.code),
        KeyEvent.KEYCODE_T to listOf('t'.code, 'c'.code, '8'.code, 0, 'd'.code),
        KeyEvent.KEYCODE_Y to listOf('y'.code, 'x'.code, '9'.code, '('.code, ')'.code),
        KeyEvent.KEYCODE_N to listOf('n'.code, 0, '0'.code, ':'.code, '・'.code),
        KeyEvent.KEYCODE_R to listOf('r'.code, '?'.code, '、'.code, '。'.code, '!'.code),
        KeyEvent.KEYCODE_W to listOf('w'.code, 0, 'v'.code, '「'.code, '」'.code),
    )

    val LABELS: Map<Int, KeyLabel.Flick> = mapOf(
        KeyEvent.KEYCODE_A to KeyLabel.Flick(
            text =  "A",
            left =  "あっ",
            up =    "あん",
            right = "ゃ",
            down =  "1"
        ),
        KeyEvent.KEYCODE_I to KeyLabel.Flick(
            text =  "I",
            left =  "いっ",
            up =    "いん",
            right = "ぃ",
            down =  "4"
        ),
        KeyEvent.KEYCODE_U to KeyLabel.Flick(
            text =  "U",
            left =  "うっ",
            up =    "うん",
            right = "ゅ",
            down =  "7"
        ),
        KeyEvent.KEYCODE_E to KeyLabel.Flick(
            text =  "E",
            left =  "えっ",
            up =    "えん",
            right = "ぇ"
        ),
        KeyEvent.KEYCODE_O to KeyLabel.Flick(
            text =  "O",
            left =  "おっ",
            up =    "おん",
            right = "ょ"
        ),

        KeyEvent.KEYCODE_K to KeyLabel.Flick(
            text =  "K",
            up =    "Q",
            right = "G",
            down =  "2"
        ),
        KeyEvent.KEYCODE_H to KeyLabel.Flick(
            text =  "H",
            left =  "P",
            up =    "F",
            right = "B",
            down =  "3"
        ),
        KeyEvent.KEYCODE_S to KeyLabel.Flick(
            text =  "S",
            up =    "J",
            right = "Z",
            down =  "5"
        ),
        KeyEvent.KEYCODE_M to KeyLabel.Flick(
            text =  "M",
            left =  "／",
            up =    "L",
            right = "ー",
            down =  "6"
        ),
        KeyEvent.KEYCODE_T to KeyLabel.Flick(
            text =  "T",
            up =    "C",
            right = "D",
            down =  "8"
        ),
        KeyEvent.KEYCODE_Y to KeyLabel.Flick(
            text =  "Y",
            left =  "（",
            up =    "X",
            right = "）",
            down =  "9"
        ),
        KeyEvent.KEYCODE_N to KeyLabel.Flick(
            text =  "N",
            left =  "：",
            right = "・",
            down =  "0"
        ),
        KeyEvent.KEYCODE_R to KeyLabel.Flick(
            text =  "R",
            left =  "。",
            up =    "？",
            right = "！",
            down =  "、"
        ),
        KeyEvent.KEYCODE_W to KeyLabel.Flick(
            text =  "W",
            left =  "「",
            right = "」",
            down =  "V"
        ),
    )
}
