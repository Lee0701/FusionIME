package ee.oyatl.ime.fusion.layout

import ee.oyatl.ime.fusion.keyboard.core.ExtKeyCode
import ee.oyatl.ime.fusion.keyboard.core.KeyCode
import ee.oyatl.ime.fusion.keyboard.core.KeyLabel

object LayoutKana {
    val ROWS_12KEY: List<String> = listOf(
        "123",
        "456",
        "789",
        ",0."
    )

    val TABLE_12KEY = mapOf(
        KeyCode.KEYCODE_1 to "1;@_:",
        KeyCode.KEYCODE_2 to "2b|ac",
        KeyCode.KEYCODE_3 to "3e~df",
        KeyCode.KEYCODE_4 to $$"4h$gi",
        KeyCode.KEYCODE_5 to "5k%jl",
        KeyCode.KEYCODE_6 to "6n&mo",
        KeyCode.KEYCODE_7 to "7qspr",
        KeyCode.KEYCODE_8 to "8u^tv",
        KeyCode.KEYCODE_9 to "9xzwy",
        KeyCode.KEYCODE_0 to "0/<+-",
        KeyCode.KEYCODE_COMMA to "*****",
        KeyCode.KEYCODE_PERIOD to "#?>,!"
    )

    val LABELS_12KEY: Map<Int, KeyLabel> = mapOf(
        KeyCode.KEYCODE_1 to KeyLabel(
            text =  "あ",
            left =  "い",
            up =    "う",
            right = "え",
            down =  "お"
        ),
        KeyCode.KEYCODE_2 to KeyLabel(
            text =  "か",
            left =  "き",
            up =    "く",
            right = "け",
            down =  "こ"
        ),
        KeyCode.KEYCODE_3 to KeyLabel(
            text =  "さ",
            left =  "し",
            up =    "す",
            right = "せ",
            down =  "そ"
        ),
        KeyCode.KEYCODE_4 to KeyLabel(
            text =  "た",
            left =  "ち",
            up =    "つ",
            right = "て",
            down =  "と"
        ),
        KeyCode.KEYCODE_5 to KeyLabel(
            text =  "な",
            left =  "に",
            up =    "ぬ",
            right = "ね",
            down =  "の"
        ),
        KeyCode.KEYCODE_6 to KeyLabel(
            text =  "は",
            left =  "ひ",
            up =    "ふ",
            right = "へ",
            down =  "ほ"
        ),
        KeyCode.KEYCODE_7 to KeyLabel(
            text =  "ま",
            left =  "み",
            up =    "む",
            right = "め",
            down =  "も"
        ),
        KeyCode.KEYCODE_8 to KeyLabel(
            text =  "や",
            left =  "（",
            up =    "ゆ",
            right = "）",
            down =  "よ"
        ),
        KeyCode.KEYCODE_9 to KeyLabel(
            text =  "ら",
            left =  "り",
            up =    "る",
            right = "れ",
            down =  "ろ"
        ),
        KeyCode.KEYCODE_0 to KeyLabel(
            text =  "わ",
            left =  "を",
            up =    "ん",
            right = "ー",
            down =  "～"
        ),
        KeyCode.KEYCODE_COMMA to KeyLabel(
            text =  "゛゜",
        ),
        KeyCode.KEYCODE_PERIOD to KeyLabel(
            text =  "、",
            left =  "。",
            up =    "？",
            right = "！",
            down =  "…"
        )
    )

    val TABLE_JIS = mapOf(
        KeyCode.KEYCODE_GRAVE to listOf('ろ'.code),
        KeyCode.KEYCODE_1 to listOf('ぬ'.code),
        KeyCode.KEYCODE_2 to listOf('ふ'.code),
        KeyCode.KEYCODE_3 to listOf('あ'.code, 'ぁ'.code),
        KeyCode.KEYCODE_4 to listOf('う'.code, 'ぅ'.code),
        KeyCode.KEYCODE_5 to listOf('え'.code, 'ぇ'.code),
        KeyCode.KEYCODE_6 to listOf('お'.code, 'ぉ'.code),
        KeyCode.KEYCODE_7 to listOf('や'.code, 'ゃ'.code),
        KeyCode.KEYCODE_8 to listOf('ゆ'.code, 'ゅ'.code),
        KeyCode.KEYCODE_9 to listOf('よ'.code, 'ょ'.code),
        KeyCode.KEYCODE_0 to listOf('わ'.code, 'を'.code),
        KeyCode.KEYCODE_MINUS to listOf('ほ'.code, 'ー'.code),
        KeyCode.KEYCODE_EQUALS to listOf('へ'.code, 'ゑ'.code),

        KeyCode.KEYCODE_Q to listOf('た'.code),
        KeyCode.KEYCODE_W to listOf('て'.code),
        KeyCode.KEYCODE_E to listOf('い'.code, 'ぃ'.code),
        KeyCode.KEYCODE_R to listOf('す'.code),
        KeyCode.KEYCODE_T to listOf('か'.code),
        KeyCode.KEYCODE_Y to listOf('ん'.code),
        KeyCode.KEYCODE_U to listOf('な'.code),
        KeyCode.KEYCODE_I to listOf('に'.code),
        KeyCode.KEYCODE_O to listOf('ら'.code),
        KeyCode.KEYCODE_P to listOf('せ'.code),
        KeyCode.KEYCODE_LEFT_BRACKET to listOf('゛'.code, '「'.code),
        KeyCode.KEYCODE_RIGHT_BRACKET to listOf('゜'.code, '」'.code),
        KeyCode.KEYCODE_BACKSLASH to listOf('む'.code),

        KeyCode.KEYCODE_A to listOf('ち'.code),
        KeyCode.KEYCODE_S to listOf('と'.code),
        KeyCode.KEYCODE_D to listOf('し'.code),
        KeyCode.KEYCODE_F to listOf('は'.code),
        KeyCode.KEYCODE_G to listOf('き'.code),
        KeyCode.KEYCODE_H to listOf('く'.code),
        KeyCode.KEYCODE_J to listOf('ま'.code),
        KeyCode.KEYCODE_K to listOf('の'.code),
        KeyCode.KEYCODE_L to listOf('り'.code),
        KeyCode.KEYCODE_SEMICOLON to listOf('れ'.code),
        KeyCode.KEYCODE_APOSTROPHE to listOf('け'.code),

        KeyCode.KEYCODE_Z to listOf('つ'.code, 'っ'.code),
        KeyCode.KEYCODE_X to listOf('さ'.code),
        KeyCode.KEYCODE_C to listOf('そ'.code),
        KeyCode.KEYCODE_V to listOf('ひ'.code, 'ゐ'.code),
        KeyCode.KEYCODE_B to listOf('こ'.code),
        KeyCode.KEYCODE_N to listOf('み'.code),
        KeyCode.KEYCODE_M to listOf('も'.code),
        KeyCode.KEYCODE_COMMA to listOf('ね'.code, '、'.code),
        KeyCode.KEYCODE_PERIOD to listOf('る'.code, '。'.code),
        KeyCode.KEYCODE_SLASH to listOf('め'.code, '・'.code),

        ExtKeyCode.KEYCODE_KANA_VOICED_MARK to listOf('゛'.code, '゜'.code),
        ExtKeyCode.KEYCODE_KANA_MINUS to listOf('ほ'.code, 'ー'.code),
        ExtKeyCode.KEYCODE_KANA_EQUALS to listOf('へ'.code, 'ゑ'.code),
        ExtKeyCode.KEYCODE_KANA_APOSTROPHE to listOf('け'.code, 'む'.code),
        ExtKeyCode.KEYCODE_KANA_SLASH to listOf('め'.code, 'ろ'.code)
    )
}