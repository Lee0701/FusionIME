package ee.oyatl.ime.fusion.layout

import android.view.KeyEvent
import ee.oyatl.ime.keyboard.KeyLabel
import ee.oyatl.ime.keyboard.KeyboardConfiguration
import ee.oyatl.ime.keyboard.touchhandler.FlickDirection

object LayoutCheonjiin {
    val CONTENT_ROWS = listOf(
        "123",
        "456",
        "789",
        ",0."
    )

    val TABLE = mapOf(
        KeyEvent.KEYCODE_1 to listOf(0x3163),
        KeyEvent.KEYCODE_2 to listOf(0x318d),
        KeyEvent.KEYCODE_3 to listOf(0x3161),
        KeyEvent.KEYCODE_4 to listOf(0x3131),
        KeyEvent.KEYCODE_5 to listOf(0x3134),
        KeyEvent.KEYCODE_6 to listOf(0x3137),
        KeyEvent.KEYCODE_7 to listOf(0x3142),
        KeyEvent.KEYCODE_8 to listOf(0x3145),
        KeyEvent.KEYCODE_9 to listOf(0x3148),
        KeyEvent.KEYCODE_0 to listOf(0x3147),
        KeyEvent.KEYCODE_COMMA to listOf(','.code),
        KeyEvent.KEYCODE_PERIOD to listOf('.'.code)
    )

    val LABELS = mapOf(
        KeyEvent.KEYCODE_1 to KeyLabel.Flick(
            text = "ㅣ",
            up = "ㅕ",
            down = "ㅑ",
            left = "ㅓ",
            right = "ㅏ"
        ),
        KeyEvent.KEYCODE_2 to KeyLabel.Flick(
            text = "ㆍ",
            up = "ㅗ",
            down = "ㅜ",
            left = "ㅓ",
            right = "ㅏ"
        ),
        KeyEvent.KEYCODE_3 to KeyLabel.Flick(
            text = "ㅡ",
            up = "ㅗ",
            down = "ㅜ",
            left = "ㅠ",
            right = "ㅛ"
        ),
        KeyEvent.KEYCODE_4 to KeyLabel.Flick(
            text = "ㄱㅋ",
            down = "ㄲ",
            left = "ㄱ",
            right = "ㅋ"
        ),
        KeyEvent.KEYCODE_5 to KeyLabel.Flick(
            text = "ㄴㄹ",
            left = "ㄴ",
            right = "ㄹ"
        ),
        KeyEvent.KEYCODE_6 to KeyLabel.Flick(
            text = "ㄷㅌ",
            down = "ㄸ",
            left = "ㄷ",
            right = "ㅌ"
        ),
        KeyEvent.KEYCODE_7 to KeyLabel.Flick(
            text = "ㅂㅍ",
            down = "ㅃ",
            left = "ㅂ",
            right = "ㅍ"
        ),
        KeyEvent.KEYCODE_8 to KeyLabel.Flick(
            text = "ㅅㅎ",
            down = "ㅆ",
            left = "ㅅ",
            right = "ㅎ"
        ),
        KeyEvent.KEYCODE_9 to KeyLabel.Flick(
            text = "ㅈㅊ",
            down = "ㅉ",
            left = "ㅈ",
            right = "ㅊ"
        ),
        KeyEvent.KEYCODE_0 to KeyLabel.Flick(
            text = "ㅇㅁ",
            left = "ㅇ",
            right = "ㅁ"
        )
    )

    val CONSONANT_FLICK_INDICES = mapOf(
        KeyEvent.KEYCODE_4 to mapOf(FlickDirection.Left to 0, FlickDirection.Right to 1, FlickDirection.Down to 2),
        KeyEvent.KEYCODE_5 to mapOf(FlickDirection.Left to 0, FlickDirection.Right to 1),
        KeyEvent.KEYCODE_6 to mapOf(FlickDirection.Left to 0, FlickDirection.Right to 1, FlickDirection.Down to 2),
        KeyEvent.KEYCODE_7 to mapOf(FlickDirection.Left to 0, FlickDirection.Right to 1, FlickDirection.Down to 2),
        KeyEvent.KEYCODE_8 to mapOf(FlickDirection.Left to 0, FlickDirection.Right to 1, FlickDirection.Down to 2),
        KeyEvent.KEYCODE_9 to mapOf(FlickDirection.Left to 0, FlickDirection.Right to 1, FlickDirection.Down to 2),
        KeyEvent.KEYCODE_0 to mapOf(FlickDirection.Left to 0, FlickDirection.Right to 1)
    )

    val VOWEL_FLICK_OUTPUTS = mapOf(
        KeyEvent.KEYCODE_1 to mapOf(
            FlickDirection.Left to 0x3153,
            FlickDirection.Up to 0x3155,
            FlickDirection.Right to 0x314f,
            FlickDirection.Down to 0x3151
        ),
        KeyEvent.KEYCODE_2 to mapOf(
            FlickDirection.Left to 0x3153,
            FlickDirection.Up to 0x3157,
            FlickDirection.Right to 0x314f,
            FlickDirection.Down to 0x315c
        ),
        KeyEvent.KEYCODE_3 to mapOf(
            FlickDirection.Left to 0x3160,
            FlickDirection.Up to 0x3157,
            FlickDirection.Right to 0x315b,
            FlickDirection.Down to 0x315c
        )
    )

    val KEYBOARD_CONFIGURATION = KeyboardConfiguration(
        listOf(
            listOf(
                KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_NUM, special = true),
                KeyboardConfiguration.Item.ContentRow(3),
                KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DEL, special = true)
            ),
            listOf(
                KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DPAD_LEFT, special = true),
                KeyboardConfiguration.Item.ContentRow(2),
                KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DPAD_RIGHT, special = true)
            ),
            listOf(
                KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_LANGUAGE_SWITCH, special = true),
                KeyboardConfiguration.Item.ContentRow(1),
                KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SPACE, special = true)
            ),
            listOf(
                KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SYM, special = true),
                KeyboardConfiguration.Item.ContentRow(0),
                KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_ENTER, special = true)
            )
        )
    )
}
