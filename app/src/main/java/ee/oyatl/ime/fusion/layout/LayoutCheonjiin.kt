package ee.oyatl.ime.fusion.layout

import android.view.KeyEvent
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
        KeyEvent.KEYCODE_1 to "ㅣ",
        KeyEvent.KEYCODE_2 to "ㆍ",
        KeyEvent.KEYCODE_3 to "ㅡ",
        KeyEvent.KEYCODE_4 to "ㄱㅋ",
        KeyEvent.KEYCODE_5 to "ㄴㄹ",
        KeyEvent.KEYCODE_6 to "ㄷㅌ",
        KeyEvent.KEYCODE_7 to "ㅂㅍ",
        KeyEvent.KEYCODE_8 to "ㅅㅎ",
        KeyEvent.KEYCODE_9 to "ㅈㅊ",
        KeyEvent.KEYCODE_0 to "ㅇㅁ"
    ) + mapOf(
        flickLabel(KeyEvent.KEYCODE_1, FlickDirection.Left) to "ㅓ",
        flickLabel(KeyEvent.KEYCODE_1, FlickDirection.Up) to "ㅕ",
        flickLabel(KeyEvent.KEYCODE_1, FlickDirection.Right) to "ㅏ",
        flickLabel(KeyEvent.KEYCODE_1, FlickDirection.Down) to "ㅑ",
        flickLabel(KeyEvent.KEYCODE_2, FlickDirection.Left) to "ㅓ",
        flickLabel(KeyEvent.KEYCODE_2, FlickDirection.Up) to "ㅗ",
        flickLabel(KeyEvent.KEYCODE_2, FlickDirection.Right) to "ㅏ",
        flickLabel(KeyEvent.KEYCODE_2, FlickDirection.Down) to "ㅜ",
        flickLabel(KeyEvent.KEYCODE_3, FlickDirection.Left) to "ㅠ",
        flickLabel(KeyEvent.KEYCODE_3, FlickDirection.Up) to "ㅗ",
        flickLabel(KeyEvent.KEYCODE_3, FlickDirection.Right) to "ㅛ",
        flickLabel(KeyEvent.KEYCODE_3, FlickDirection.Down) to "ㅜ",
        flickLabel(KeyEvent.KEYCODE_4, FlickDirection.Left) to "ㄱ",
        flickLabel(KeyEvent.KEYCODE_4, FlickDirection.Right) to "ㅋ",
        flickLabel(KeyEvent.KEYCODE_4, FlickDirection.Down) to "ㄲ",
        flickLabel(KeyEvent.KEYCODE_5, FlickDirection.Left) to "ㄴ",
        flickLabel(KeyEvent.KEYCODE_5, FlickDirection.Right) to "ㄹ",
        flickLabel(KeyEvent.KEYCODE_6, FlickDirection.Left) to "ㄷ",
        flickLabel(KeyEvent.KEYCODE_6, FlickDirection.Right) to "ㅌ",
        flickLabel(KeyEvent.KEYCODE_6, FlickDirection.Down) to "ㄸ",
        flickLabel(KeyEvent.KEYCODE_7, FlickDirection.Left) to "ㅂ",
        flickLabel(KeyEvent.KEYCODE_7, FlickDirection.Right) to "ㅍ",
        flickLabel(KeyEvent.KEYCODE_7, FlickDirection.Down) to "ㅃ",
        flickLabel(KeyEvent.KEYCODE_8, FlickDirection.Left) to "ㅅ",
        flickLabel(KeyEvent.KEYCODE_8, FlickDirection.Right) to "ㅎ",
        flickLabel(KeyEvent.KEYCODE_8, FlickDirection.Down) to "ㅆ",
        flickLabel(KeyEvent.KEYCODE_9, FlickDirection.Left) to "ㅈ",
        flickLabel(KeyEvent.KEYCODE_9, FlickDirection.Right) to "ㅊ",
        flickLabel(KeyEvent.KEYCODE_9, FlickDirection.Down) to "ㅉ",
        flickLabel(KeyEvent.KEYCODE_0, FlickDirection.Left) to "ㅇ",
        flickLabel(KeyEvent.KEYCODE_0, FlickDirection.Right) to "ㅁ"
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

    private fun flickLabel(keyCode: Int, direction: FlickDirection): Int {
        return keyCode or direction.keyCodeFlag
    }
}
