package ee.oyatl.ime.keyboard.layout

import android.view.KeyEvent
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