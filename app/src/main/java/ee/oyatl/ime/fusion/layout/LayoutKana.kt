package ee.oyatl.ime.fusion.layout

import android.view.KeyEvent
import ee.oyatl.ime.keyboard.FlickKeyCode
import ee.oyatl.ime.keyboard.KeyboardConfiguration

object LayoutKana {
    val ROWS_SYLLABLES: List<String> = listOf(
        "わらやまはなたさかあ",
        "ゐり　みひにちしきい",
        "　るゆむふぬつすくう",
        "ゑれ　めへねてせけえ",
        "をろよもほのとそこお"
    )
    const val BOTTOM_LEFT_SYLLABLES: String = "ん"
    const val BOTTOM_RIGHT_SYLLABLES: String = "*ー"

    val ROWS_12KEY: List<String> = listOf(
        "123",
        "456",
        "789",
        ",0."
    )

    fun mobileKeyboardConfigurationSyllables(): KeyboardConfiguration {
        val rows = ROWS_SYLLABLES.map { row ->
            row.map { item -> when(item) {
                '　' -> KeyboardConfiguration.Item.Spacer(width = 1f)
                else -> KeyboardConfiguration.Item.TemplateKey(-item.code)
            } }
        }
        val bottom = listOf(
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SYM, 1.5f, true),
            KeyboardConfiguration.Item.TemplateKey(-BOTTOM_LEFT_SYLLABLES[0].code),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_LANGUAGE_SWITCH, 1f, true),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SPACE, 2f),
            KeyboardConfiguration.Item.TemplateKey(-BOTTOM_RIGHT_SYLLABLES[0].code),
            KeyboardConfiguration.Item.TemplateKey(-BOTTOM_RIGHT_SYLLABLES[1].code),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_ENTER, 1.5f, true),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DEL, 1f, true)
        )
        return KeyboardConfiguration(rows + listOf(bottom))
    }

    fun tabletKeyboardConfigurationSyllables(): KeyboardConfiguration {
        val rows = ROWS_SYLLABLES.map { row ->
            row.map { item -> when(item) {
                '　' -> KeyboardConfiguration.Item.Spacer(width = 1f)
                else -> KeyboardConfiguration.Item.TemplateKey(-item.code)
            } }.toMutableList()
        }
        rows[0].add(0, KeyboardConfiguration.Item.TemplateKey(-BOTTOM_LEFT_SYLLABLES[0].code))
        rows[0].add(KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DEL, 1f, true))
        rows[1].add(0, KeyboardConfiguration.Item.Spacer(1f))
        rows[1].add(KeyboardConfiguration.Item.Spacer(1f))
        rows[2].add(0, KeyboardConfiguration.Item.TemplateKey(-BOTTOM_RIGHT_SYLLABLES[0].code))
        rows[2].add(KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_ENTER, 1f, true))
        rows[3].add(0, KeyboardConfiguration.Item.Spacer(1f))
        rows[3].add(KeyboardConfiguration.Item.Spacer(1f))
        rows[4].add(0, KeyboardConfiguration.Item.TemplateKey(-BOTTOM_RIGHT_SYLLABLES[1].code))
        rows[4].add(KeyboardConfiguration.Item.Spacer(1f))
        return KeyboardConfiguration(rows) + TabletKeyboard.bottom()
    }

    fun mobileKeyboardConfiguration12Key(): KeyboardConfiguration {
        val rows = (0 until 4).map { mutableListOf<KeyboardConfiguration.Item>() }

        rows[0] += KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_NUM, special = true)
        rows[1] += KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DPAD_LEFT, special = true)
        rows[2] += KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_LANGUAGE_SWITCH, special = true)
        rows[3] += KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SYM, special = true)

        rows[0] += KeyboardConfiguration.Item.ContentRow(3)
        rows[1] += KeyboardConfiguration.Item.ContentRow(2)
        rows[2] += KeyboardConfiguration.Item.ContentRow(1)
        rows[3] += KeyboardConfiguration.Item.ContentRow(0)

        rows[0] += KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DEL, special = true)
        rows[1] += KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DPAD_RIGHT, special = true)
        rows[2] += KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SPACE, special = true)
        rows[3] += KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_ENTER, special = true)

        return KeyboardConfiguration(rows)
    }

    val TABLE_12KEY = mapOf(
        KeyEvent.KEYCODE_1 or FlickKeyCode.DIRECTION_NONE  to listOf('1'.code),
        KeyEvent.KEYCODE_1 or FlickKeyCode.DIRECTION_LEFT  to listOf('_'.code),
        KeyEvent.KEYCODE_1 or FlickKeyCode.DIRECTION_UP    to listOf(';'.code),
        KeyEvent.KEYCODE_1 or FlickKeyCode.DIRECTION_RIGHT to listOf(':'.code),
        KeyEvent.KEYCODE_1 or FlickKeyCode.DIRECTION_DOWN  to listOf('@'.code),

        KeyEvent.KEYCODE_2 or FlickKeyCode.DIRECTION_NONE  to listOf('2'.code),
        KeyEvent.KEYCODE_2 or FlickKeyCode.DIRECTION_LEFT  to listOf('a'.code),
        KeyEvent.KEYCODE_2 or FlickKeyCode.DIRECTION_UP    to listOf('b'.code),
        KeyEvent.KEYCODE_2 or FlickKeyCode.DIRECTION_RIGHT to listOf('c'.code),
        KeyEvent.KEYCODE_2 or FlickKeyCode.DIRECTION_DOWN  to listOf('|'.code),

        KeyEvent.KEYCODE_3 or FlickKeyCode.DIRECTION_NONE  to listOf('3'.code),
        KeyEvent.KEYCODE_3 or FlickKeyCode.DIRECTION_LEFT  to listOf('d'.code),
        KeyEvent.KEYCODE_3 or FlickKeyCode.DIRECTION_UP    to listOf('e'.code),
        KeyEvent.KEYCODE_3 or FlickKeyCode.DIRECTION_RIGHT to listOf('f'.code),
        KeyEvent.KEYCODE_3 or FlickKeyCode.DIRECTION_DOWN  to listOf('~'.code),

        KeyEvent.KEYCODE_4 or FlickKeyCode.DIRECTION_NONE  to listOf('4'.code),
        KeyEvent.KEYCODE_4 or FlickKeyCode.DIRECTION_LEFT  to listOf('g'.code),
        KeyEvent.KEYCODE_4 or FlickKeyCode.DIRECTION_UP    to listOf('h'.code),
        KeyEvent.KEYCODE_4 or FlickKeyCode.DIRECTION_RIGHT to listOf('i'.code),
        KeyEvent.KEYCODE_4 or FlickKeyCode.DIRECTION_DOWN  to listOf('$'.code),

        KeyEvent.KEYCODE_5 or FlickKeyCode.DIRECTION_NONE  to listOf('5'.code),
        KeyEvent.KEYCODE_5 or FlickKeyCode.DIRECTION_LEFT  to listOf('j'.code),
        KeyEvent.KEYCODE_5 or FlickKeyCode.DIRECTION_UP    to listOf('k'.code),
        KeyEvent.KEYCODE_5 or FlickKeyCode.DIRECTION_RIGHT to listOf('l'.code),
        KeyEvent.KEYCODE_5 or FlickKeyCode.DIRECTION_DOWN  to listOf('%'.code),

        KeyEvent.KEYCODE_6 or FlickKeyCode.DIRECTION_NONE  to listOf('6'.code),
        KeyEvent.KEYCODE_6 or FlickKeyCode.DIRECTION_LEFT  to listOf('m'.code),
        KeyEvent.KEYCODE_6 or FlickKeyCode.DIRECTION_UP    to listOf('n'.code),
        KeyEvent.KEYCODE_6 or FlickKeyCode.DIRECTION_RIGHT to listOf('o'.code),
        KeyEvent.KEYCODE_6 or FlickKeyCode.DIRECTION_DOWN  to listOf('&'.code),

        KeyEvent.KEYCODE_7 or FlickKeyCode.DIRECTION_NONE  to listOf('7'.code),
        KeyEvent.KEYCODE_7 or FlickKeyCode.DIRECTION_LEFT  to listOf('p'.code),
        KeyEvent.KEYCODE_7 or FlickKeyCode.DIRECTION_UP    to listOf('q'.code),
        KeyEvent.KEYCODE_7 or FlickKeyCode.DIRECTION_RIGHT to listOf('r'.code),
        KeyEvent.KEYCODE_7 or FlickKeyCode.DIRECTION_DOWN  to listOf('s'.code),

        KeyEvent.KEYCODE_8 or FlickKeyCode.DIRECTION_NONE  to listOf('8'.code),
        KeyEvent.KEYCODE_8 or FlickKeyCode.DIRECTION_LEFT  to listOf('t'.code),
        KeyEvent.KEYCODE_8 or FlickKeyCode.DIRECTION_UP    to listOf('u'.code),
        KeyEvent.KEYCODE_8 or FlickKeyCode.DIRECTION_RIGHT to listOf('v'.code),
        KeyEvent.KEYCODE_8 or FlickKeyCode.DIRECTION_DOWN  to listOf('^'.code),

        KeyEvent.KEYCODE_9 or FlickKeyCode.DIRECTION_NONE  to listOf('9'.code),
        KeyEvent.KEYCODE_9 or FlickKeyCode.DIRECTION_LEFT  to listOf('w'.code),
        KeyEvent.KEYCODE_9 or FlickKeyCode.DIRECTION_UP    to listOf('x'.code),
        KeyEvent.KEYCODE_9 or FlickKeyCode.DIRECTION_RIGHT to listOf('y'.code),
        KeyEvent.KEYCODE_9 or FlickKeyCode.DIRECTION_DOWN  to listOf('z'.code),

        KeyEvent.KEYCODE_0 or FlickKeyCode.DIRECTION_NONE  to listOf('0'.code),
        KeyEvent.KEYCODE_0 or FlickKeyCode.DIRECTION_LEFT  to listOf('+'.code),
        KeyEvent.KEYCODE_0 or FlickKeyCode.DIRECTION_UP    to listOf('/'.code),
        KeyEvent.KEYCODE_0 or FlickKeyCode.DIRECTION_RIGHT to listOf('-'.code),
        KeyEvent.KEYCODE_0 or FlickKeyCode.DIRECTION_DOWN  to listOf('<'.code),

        KeyEvent.KEYCODE_COMMA or FlickKeyCode.DIRECTION_NONE to listOf('*'.code),

        KeyEvent.KEYCODE_PERIOD or FlickKeyCode.DIRECTION_NONE  to listOf('#'.code),
        KeyEvent.KEYCODE_PERIOD or FlickKeyCode.DIRECTION_LEFT  to listOf(','.code),
        KeyEvent.KEYCODE_PERIOD or FlickKeyCode.DIRECTION_UP    to listOf('?'.code),
        KeyEvent.KEYCODE_PERIOD or FlickKeyCode.DIRECTION_RIGHT to listOf('!'.code),
        KeyEvent.KEYCODE_PERIOD or FlickKeyCode.DIRECTION_DOWN  to listOf('>'.code)
    )

    val LABELS_12KEY: Map<Int, String> = mapOf(
        KeyEvent.KEYCODE_1 or FlickKeyCode.DIRECTION_NONE  to "あ",
        KeyEvent.KEYCODE_1 or FlickKeyCode.DIRECTION_LEFT  to "い",
        KeyEvent.KEYCODE_1 or FlickKeyCode.DIRECTION_UP    to "う",
        KeyEvent.KEYCODE_1 or FlickKeyCode.DIRECTION_RIGHT to "え",
        KeyEvent.KEYCODE_1 or FlickKeyCode.DIRECTION_DOWN  to "お",

        KeyEvent.KEYCODE_2 or FlickKeyCode.DIRECTION_NONE  to "か",
        KeyEvent.KEYCODE_2 or FlickKeyCode.DIRECTION_LEFT  to "き",
        KeyEvent.KEYCODE_2 or FlickKeyCode.DIRECTION_UP    to "く",
        KeyEvent.KEYCODE_2 or FlickKeyCode.DIRECTION_RIGHT to "け",
        KeyEvent.KEYCODE_2 or FlickKeyCode.DIRECTION_DOWN  to "こ",

        KeyEvent.KEYCODE_3 or FlickKeyCode.DIRECTION_NONE  to "さ",
        KeyEvent.KEYCODE_3 or FlickKeyCode.DIRECTION_LEFT  to "し",
        KeyEvent.KEYCODE_3 or FlickKeyCode.DIRECTION_UP    to "す",
        KeyEvent.KEYCODE_3 or FlickKeyCode.DIRECTION_RIGHT to "せ",
        KeyEvent.KEYCODE_3 or FlickKeyCode.DIRECTION_DOWN  to "そ",

        KeyEvent.KEYCODE_4 or FlickKeyCode.DIRECTION_NONE  to "た",
        KeyEvent.KEYCODE_4 or FlickKeyCode.DIRECTION_LEFT  to "ち",
        KeyEvent.KEYCODE_4 or FlickKeyCode.DIRECTION_UP    to "つ",
        KeyEvent.KEYCODE_4 or FlickKeyCode.DIRECTION_RIGHT to "て",
        KeyEvent.KEYCODE_4 or FlickKeyCode.DIRECTION_DOWN  to "と",

        KeyEvent.KEYCODE_5 or FlickKeyCode.DIRECTION_NONE  to "な",
        KeyEvent.KEYCODE_5 or FlickKeyCode.DIRECTION_LEFT  to "に",
        KeyEvent.KEYCODE_5 or FlickKeyCode.DIRECTION_UP    to "ぬ",
        KeyEvent.KEYCODE_5 or FlickKeyCode.DIRECTION_RIGHT to "ね",
        KeyEvent.KEYCODE_5 or FlickKeyCode.DIRECTION_DOWN  to "の",

        KeyEvent.KEYCODE_6 or FlickKeyCode.DIRECTION_NONE  to "は",
        KeyEvent.KEYCODE_6 or FlickKeyCode.DIRECTION_LEFT  to "ひ",
        KeyEvent.KEYCODE_6 or FlickKeyCode.DIRECTION_UP    to "ふ",
        KeyEvent.KEYCODE_6 or FlickKeyCode.DIRECTION_RIGHT to "へ",
        KeyEvent.KEYCODE_6 or FlickKeyCode.DIRECTION_DOWN  to "ほ",

        KeyEvent.KEYCODE_7 or FlickKeyCode.DIRECTION_NONE  to "ま",
        KeyEvent.KEYCODE_7 or FlickKeyCode.DIRECTION_LEFT  to "み",
        KeyEvent.KEYCODE_7 or FlickKeyCode.DIRECTION_UP    to "む",
        KeyEvent.KEYCODE_7 or FlickKeyCode.DIRECTION_RIGHT to "め",
        KeyEvent.KEYCODE_7 or FlickKeyCode.DIRECTION_DOWN  to "も",

        KeyEvent.KEYCODE_8 or FlickKeyCode.DIRECTION_NONE  to "や",
        KeyEvent.KEYCODE_8 or FlickKeyCode.DIRECTION_LEFT  to "（",
        KeyEvent.KEYCODE_8 or FlickKeyCode.DIRECTION_UP    to "ゆ",
        KeyEvent.KEYCODE_8 or FlickKeyCode.DIRECTION_RIGHT to "）",
        KeyEvent.KEYCODE_8 or FlickKeyCode.DIRECTION_DOWN  to "よ",

        KeyEvent.KEYCODE_9 or FlickKeyCode.DIRECTION_NONE  to "ら",
        KeyEvent.KEYCODE_9 or FlickKeyCode.DIRECTION_LEFT  to "り",
        KeyEvent.KEYCODE_9 or FlickKeyCode.DIRECTION_UP    to "る",
        KeyEvent.KEYCODE_9 or FlickKeyCode.DIRECTION_RIGHT to "れ",
        KeyEvent.KEYCODE_9 or FlickKeyCode.DIRECTION_DOWN  to "ろ",

        KeyEvent.KEYCODE_0 or FlickKeyCode.DIRECTION_NONE  to "わ",
        KeyEvent.KEYCODE_0 or FlickKeyCode.DIRECTION_LEFT  to "を",
        KeyEvent.KEYCODE_0 or FlickKeyCode.DIRECTION_UP    to "ん",
        KeyEvent.KEYCODE_0 or FlickKeyCode.DIRECTION_RIGHT to "ー",
        KeyEvent.KEYCODE_0 or FlickKeyCode.DIRECTION_DOWN  to "～",

        KeyEvent.KEYCODE_COMMA or FlickKeyCode.DIRECTION_NONE to "゛゜",

        KeyEvent.KEYCODE_PERIOD or FlickKeyCode.DIRECTION_NONE  to "、",
        KeyEvent.KEYCODE_PERIOD or FlickKeyCode.DIRECTION_LEFT  to "。",
        KeyEvent.KEYCODE_PERIOD or FlickKeyCode.DIRECTION_UP    to "？",
        KeyEvent.KEYCODE_PERIOD or FlickKeyCode.DIRECTION_RIGHT to "！",
        KeyEvent.KEYCODE_PERIOD or FlickKeyCode.DIRECTION_DOWN  to "…"
    )

    val TABLE_JIS = mapOf(
        KeyEvent.KEYCODE_GRAVE to listOf('ろ'.code),
        KeyEvent.KEYCODE_1 to listOf('ぬ'.code),
        KeyEvent.KEYCODE_2 to listOf('ふ'.code),
        KeyEvent.KEYCODE_3 to listOf('あ'.code, 'ぁ'.code),
        KeyEvent.KEYCODE_4 to listOf('う'.code, 'ぅ'.code),
        KeyEvent.KEYCODE_5 to listOf('え'.code, 'ぇ'.code),
        KeyEvent.KEYCODE_6 to listOf('お'.code, 'ぉ'.code),
        KeyEvent.KEYCODE_7 to listOf('や'.code, 'ゃ'.code),
        KeyEvent.KEYCODE_8 to listOf('ゆ'.code, 'ゅ'.code),
        KeyEvent.KEYCODE_9 to listOf('よ'.code, 'ょ'.code),
        KeyEvent.KEYCODE_0 to listOf('わ'.code, 'を'.code),
        KeyEvent.KEYCODE_MINUS to listOf('ほ'.code, 'ー'.code),
        KeyEvent.KEYCODE_EQUALS to listOf('へ'.code, 'ゑ'.code),

        KeyEvent.KEYCODE_Q to listOf('た'.code),
        KeyEvent.KEYCODE_W to listOf('て'.code),
        KeyEvent.KEYCODE_E to listOf('い'.code, 'ぃ'.code),
        KeyEvent.KEYCODE_R to listOf('す'.code),
        KeyEvent.KEYCODE_T to listOf('か'.code),
        KeyEvent.KEYCODE_Y to listOf('ん'.code),
        KeyEvent.KEYCODE_U to listOf('な'.code),
        KeyEvent.KEYCODE_I to listOf('に'.code),
        KeyEvent.KEYCODE_O to listOf('ら'.code),
        KeyEvent.KEYCODE_P to listOf('せ'.code),
        KeyEvent.KEYCODE_LEFT_BRACKET to listOf('゛'.code, '「'.code),
        KeyEvent.KEYCODE_RIGHT_BRACKET to listOf('゜'.code, '」'.code),
        KeyEvent.KEYCODE_BACKSLASH to listOf('む'.code),

        KeyEvent.KEYCODE_A to listOf('ち'.code),
        KeyEvent.KEYCODE_S to listOf('と'.code),
        KeyEvent.KEYCODE_D to listOf('し'.code),
        KeyEvent.KEYCODE_F to listOf('は'.code),
        KeyEvent.KEYCODE_G to listOf('き'.code),
        KeyEvent.KEYCODE_H to listOf('く'.code),
        KeyEvent.KEYCODE_J to listOf('ま'.code),
        KeyEvent.KEYCODE_K to listOf('の'.code),
        KeyEvent.KEYCODE_L to listOf('り'.code),
        KeyEvent.KEYCODE_SEMICOLON to listOf('れ'.code),
        KeyEvent.KEYCODE_APOSTROPHE to listOf('け'.code),

        KeyEvent.KEYCODE_Z to listOf('つ'.code, 'っ'.code),
        KeyEvent.KEYCODE_X to listOf('さ'.code),
        KeyEvent.KEYCODE_C to listOf('そ'.code),
        KeyEvent.KEYCODE_V to listOf('ひ'.code, 'ゐ'.code),
        KeyEvent.KEYCODE_B to listOf('こ'.code),
        KeyEvent.KEYCODE_N to listOf('み'.code),
        KeyEvent.KEYCODE_M to listOf('も'.code),
        KeyEvent.KEYCODE_COMMA to listOf('ね'.code, '、'.code),
        KeyEvent.KEYCODE_PERIOD to listOf('る'.code, '。'.code),
        KeyEvent.KEYCODE_SLASH to listOf('め'.code, '・'.code),

        ExtKeyCode.KEYCODE_KANA_VOICED_MARK to listOf('゛'.code, '゜'.code),
        ExtKeyCode.KEYCODE_KANA_MINUS to listOf('ほ'.code, 'ー'.code),
        ExtKeyCode.KEYCODE_KANA_EQUALS to listOf('へ'.code, 'ゑ'.code),
        ExtKeyCode.KEYCODE_KANA_APOSTROPHE to listOf('け'.code, 'む'.code),
        ExtKeyCode.KEYCODE_KANA_SLASH to listOf('め'.code, 'ろ'.code)
    )
}