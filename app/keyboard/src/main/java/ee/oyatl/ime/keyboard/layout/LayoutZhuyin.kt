package ee.oyatl.ime.keyboard.layout

import android.view.KeyEvent

object LayoutZhuyin {

    val EXTRA_KEYS = listOf("-")

    val TABLE = mapOf(
        KeyEvent.KEYCODE_1 to listOf('ㄅ'.code),
        KeyEvent.KEYCODE_2 to listOf('ㄉ'.code),
        KeyEvent.KEYCODE_3 to listOf('ˇ'.code),
        KeyEvent.KEYCODE_4 to listOf('ˋ'.code),
        KeyEvent.KEYCODE_5 to listOf('ㄓ'.code),
        KeyEvent.KEYCODE_6 to listOf('ˊ'.code),
        KeyEvent.KEYCODE_7 to listOf('˙'.code),
        KeyEvent.KEYCODE_8 to listOf('ㄚ'.code),
        KeyEvent.KEYCODE_9 to listOf('ㄞ'.code),
        KeyEvent.KEYCODE_0 to listOf('ㄢ'.code),

        KeyEvent.KEYCODE_Q to listOf('ㄆ'.code),
        KeyEvent.KEYCODE_W to listOf('ㄊ'.code),
        KeyEvent.KEYCODE_E to listOf('ㄍ'.code),
        KeyEvent.KEYCODE_R to listOf('ㄐ'.code),
        KeyEvent.KEYCODE_T to listOf('ㄔ'.code),
        KeyEvent.KEYCODE_Y to listOf('ㄗ'.code),
        KeyEvent.KEYCODE_U to listOf('ㄧ'.code),
        KeyEvent.KEYCODE_I to listOf('ㄛ'.code),
        KeyEvent.KEYCODE_O to listOf('ㄟ'.code),
        KeyEvent.KEYCODE_P to listOf('ㄣ'.code),

        KeyEvent.KEYCODE_A to listOf('ㄇ'.code),
        KeyEvent.KEYCODE_S to listOf('ㄋ'.code),
        KeyEvent.KEYCODE_D to listOf('ㄎ'.code),
        KeyEvent.KEYCODE_F to listOf('ㄑ'.code),
        KeyEvent.KEYCODE_G to listOf('ㄕ'.code),
        KeyEvent.KEYCODE_H to listOf('ㄘ'.code),
        KeyEvent.KEYCODE_J to listOf('ㄨ'.code),
        KeyEvent.KEYCODE_K to listOf('ㄜ'.code),
        KeyEvent.KEYCODE_L to listOf('ㄠ'.code),
        KeyEvent.KEYCODE_SEMICOLON to listOf('ㄤ'.code),

        KeyEvent.KEYCODE_Z to listOf('ㄈ'.code),
        KeyEvent.KEYCODE_X to listOf('ㄌ'.code),
        KeyEvent.KEYCODE_C to listOf('ㄏ'.code),
        KeyEvent.KEYCODE_V to listOf('ㄒ'.code),
        KeyEvent.KEYCODE_B to listOf('ㄖ'.code),
        KeyEvent.KEYCODE_N to listOf('ㄙ'.code),
        KeyEvent.KEYCODE_M to listOf('ㄩ'.code),
        KeyEvent.KEYCODE_COMMA to listOf('ㄝ'.code),
        KeyEvent.KEYCODE_PERIOD to listOf('ㄡ'.code),
        KeyEvent.KEYCODE_SLASH to listOf('ㄥ'.code),
        KeyEvent.KEYCODE_MINUS to listOf('ㄦ'.code)
    )

    val CODES_MAP = mapOf(
        'ㄅ' to 12549,
        'ㄉ' to 12553,
        'ˇ' to 711,
        'ˋ' to 715,
        'ㄓ' to 12563,
        'ˊ' to 714,
        '˙' to 729,
        'ㄚ' to 12570,
        'ㄞ' to 12574,
        'ㄢ' to 12578,
        'ㄆ' to 12550,
        'ㄊ' to 12554,
        'ㄍ' to 12557,
        'ㄐ' to 12560,
        'ㄔ' to 12564,
        'ㄗ' to 12567,
        'ㄧ' to 12583,
        'ㄛ' to 12571,
        'ㄟ' to 12575,
        'ㄣ' to 12579,
        'ㄇ' to 12551,
        'ㄋ' to 12555,
        'ㄎ' to 12558,
        'ㄑ' to 12561,
        'ㄕ' to 12565,
        'ㄘ' to 12568,
        'ㄨ' to 12584,
        'ㄜ' to 12572,
        'ㄠ' to 12576,
        'ㄤ' to 12580,
        'ㄈ' to 12552,
        'ㄌ' to 12556,
        'ㄏ' to 12559,
        'ㄒ' to 12562,
        'ㄖ' to 12566,
        'ㄙ' to 12569,
        'ㄩ' to 12585,
        'ㄝ' to 12573,
        'ㄡ' to 12577,
        'ㄥ' to 12581,
        'ㄦ' to 12582
    )
}