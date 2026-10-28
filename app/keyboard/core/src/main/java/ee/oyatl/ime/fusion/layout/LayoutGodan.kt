package ee.oyatl.ime.fusion.layout

import ee.oyatl.ime.fusion.keyboard.core.KeyCode
import ee.oyatl.ime.fusion.keyboard.core.KeyLabel
import ee.oyatl.ime.fusion.keyboard.core.KeyboardConfiguration

import ee.oyatl.ime.fusion.keyboard.core.KeyboardConfiguration.Item.TemplateKey

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
                    TemplateKey(KeyCode.KEYCODE_NUM, special = true),
                    TemplateKey(KeyCode.KEYCODE_A),
                    TemplateKey(KeyCode.KEYCODE_K),
                    TemplateKey(KeyCode.KEYCODE_H),
                    TemplateKey(KeyCode.KEYCODE_DEL, special = true)
                ),
                listOf(
                    TemplateKey(KeyCode.KEYCODE_DPAD_LEFT, special = true),
                    TemplateKey(KeyCode.KEYCODE_I),
                    TemplateKey(KeyCode.KEYCODE_S),
                    TemplateKey(KeyCode.KEYCODE_M),
                    TemplateKey(KeyCode.KEYCODE_DPAD_RIGHT, special = true)
                ),
                listOf(
                    TemplateKey(KeyCode.KEYCODE_LANGUAGE_SWITCH, special = true),
                    TemplateKey(KeyCode.KEYCODE_U),
                    TemplateKey(KeyCode.KEYCODE_T),
                    TemplateKey(KeyCode.KEYCODE_Y),
                    TemplateKey(KeyCode.KEYCODE_SPACE, special = true)
                ),
                listOf(
                    TemplateKey(
                        KeyCode.KEYCODE_SYM,
                        special = true,
                        merge = TemplateKey.Merge.Down
                    ),
                    TemplateKey(KeyCode.KEYCODE_E),
                    TemplateKey(KeyCode.KEYCODE_N),
                    TemplateKey(KeyCode.KEYCODE_R),
                    TemplateKey(
                        KeyCode.KEYCODE_ENTER,
                        special = true,
                        merge = TemplateKey.Merge.Down
                    )
                ),
                listOf(
                    TemplateKey(KeyCode.KEYCODE_SYM, special = true, merge = TemplateKey.Merge.Up),
                    TemplateKey(KeyCode.KEYCODE_O),
                    KeyboardConfiguration.Item.Spacer(width = 1f),
                    TemplateKey(KeyCode.KEYCODE_W),
                    TemplateKey(KeyCode.KEYCODE_ENTER, special = true, merge = TemplateKey.Merge.Up)
                )
            )
        )
    }

    val TABLE: Map<Int, List<Int>> = mapOf(
        // Main 14 Roman-letter keys.
        // main, up, down, left, right

        // Vowel keys.
        // Tap: normal vowel
        // Left: vowel + small tsu
        // Up: vowel + n
        // Right: small vowel / contracted sound
        KeyCode.KEYCODE_A to listOf('a'.code, '<'.code, '1'.code, '$'.code, '#'.code),
        KeyCode.KEYCODE_I to listOf('i'.code, '>'.code, '4'.code, '%'.code, '+'.code),
        KeyCode.KEYCODE_U to listOf('u'.code, '{'.code, '7'.code, '&'.code, '^'.code),
        KeyCode.KEYCODE_E to listOf('e'.code, '}'.code, 0, '='.code, '_'.code),
        KeyCode.KEYCODE_O to listOf('o'.code, '~'.code, 0, '@'.code, '|'.code),

        // Keys with GODAN auxiliary consonants and symbols.
        // Telephone-layout numbers on downward flicks.
        KeyCode.KEYCODE_K to listOf('k'.code, 'q'.code, '2'.code, 0, 'g'.code),
        KeyCode.KEYCODE_H to listOf('h'.code, 'f'.code, '3'.code, 'p'.code, 'b'.code),
        KeyCode.KEYCODE_S to listOf('s'.code, 'j'.code, '5'.code, 0, 'z'.code),
        KeyCode.KEYCODE_M to listOf('m'.code, 'l'.code, '6'.code, '/'.code, '-'.code),
        KeyCode.KEYCODE_T to listOf('t'.code, 'c'.code, '8'.code, 0, 'd'.code),
        KeyCode.KEYCODE_Y to listOf('y'.code, 'x'.code, '9'.code, '('.code, ')'.code),
        KeyCode.KEYCODE_N to listOf('n'.code, 0, '0'.code, ':'.code, '・'.code),
        KeyCode.KEYCODE_R to listOf('r'.code, '?'.code, '、'.code, '。'.code, '!'.code),
        KeyCode.KEYCODE_W to listOf('w'.code, 'v'.code, 0, '「'.code, '」'.code),
    )

    val LABELS: Map<Int, KeyLabel> = mapOf(
        KeyCode.KEYCODE_A to KeyLabel(
            text =  "A",
            left =  "あっ",
            up =    "あん",
            right = "ゃ",
            down =  "1"
        ),
        KeyCode.KEYCODE_I to KeyLabel(
            text =  "I",
            left =  "いっ",
            up =    "いん",
            right = "ぃ",
            down =  "4"
        ),
        KeyCode.KEYCODE_U to KeyLabel(
            text =  "U",
            left =  "うっ",
            up =    "うん",
            right = "ゅ",
            down =  "7"
        ),
        KeyCode.KEYCODE_E to KeyLabel(
            text =  "E",
            left =  "えっ",
            up =    "えん",
            right = "ぇ"
        ),
        KeyCode.KEYCODE_O to KeyLabel(
            text =  "O",
            left =  "おっ",
            up =    "おん",
            right = "ょ"
        ),

        KeyCode.KEYCODE_K to KeyLabel(
            text =  "K",
            up =    "Q",
            right = "G",
            down =  "2"
        ),
        KeyCode.KEYCODE_H to KeyLabel(
            text =  "H",
            left =  "P",
            up =    "F",
            right = "B",
            down =  "3"
        ),
        KeyCode.KEYCODE_S to KeyLabel(
            text =  "S",
            up =    "J",
            right = "Z",
            down =  "5"
        ),
        KeyCode.KEYCODE_M to KeyLabel(
            text =  "M",
            left =  "／",
            up =    "L",
            right = "ー",
            down =  "6"
        ),
        KeyCode.KEYCODE_T to KeyLabel(
            text =  "T",
            up =    "C",
            right = "D",
            down =  "8"
        ),
        KeyCode.KEYCODE_Y to KeyLabel(
            text =  "Y",
            left =  "（",
            up =    "X",
            right = "）",
            down =  "9"
        ),
        KeyCode.KEYCODE_N to KeyLabel(
            text =  "N",
            left =  "：",
            right = "・",
            down =  "0"
        ),
        KeyCode.KEYCODE_R to KeyLabel(
            text =  "R",
            left =  "。",
            up =    "？",
            right = "！",
            down =  "、"
        ),
        KeyCode.KEYCODE_W to KeyLabel(
            text =  "W",
            left =  "「",
            up =  "V",
            right = "」"
        ),
    )
}
