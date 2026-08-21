package ee.oyatl.ime.fusion.layout

import android.view.KeyEvent
import ee.oyatl.ime.keyboard.KeyLabel
import ee.oyatl.ime.keyboard.KeyboardConfiguration
import kotlin.math.ceil

object LayoutKana {
    const val KEYS_AIUEO = "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもや　ゆ　よらりるれろわゐ　ゑを"
    const val KEYS_IROHA = "いろはにほへとちりぬるをわかよたれそつねならむうゐのおくやまけふこえてあさきゆめみしゑひもせす　　　"
    const val BOTTOM_LEFT_SYLLABLES: String = "ん"
    const val BOTTOM_RIGHT_SYLLABLES: String = "*ー"

    val ROWS_12KEY: List<String> = listOf(
        "123",
        "456",
        "789",
        ",0."
    )

    val TABLE_12KEY = mapOf(
        KeyEvent.KEYCODE_1 to "1;@_:",
        KeyEvent.KEYCODE_2 to "2b|ac",
        KeyEvent.KEYCODE_3 to "3e~df",
        KeyEvent.KEYCODE_4 to $$"4h$gi",
        KeyEvent.KEYCODE_5 to "5k%jl",
        KeyEvent.KEYCODE_6 to "6n&mo",
        KeyEvent.KEYCODE_7 to "7qspr",
        KeyEvent.KEYCODE_8 to "8u^tv",
        KeyEvent.KEYCODE_9 to "9xzwy",
        KeyEvent.KEYCODE_0 to "0/<+-",
        KeyEvent.KEYCODE_COMMA to "*****",
        KeyEvent.KEYCODE_PERIOD to "#?>,!"
    )

    val LABELS_12KEY: Map<Int, KeyLabel.Flick> = mapOf(
        KeyEvent.KEYCODE_1 to KeyLabel.Flick(
            text =  "あ",
            left =  "い",
            up =    "う",
            right = "え",
            down =  "お"
        ),
        KeyEvent.KEYCODE_2 to KeyLabel.Flick(
            text =  "か",
            left =  "き",
            up =    "く",
            right = "け",
            down =  "こ"
        ),
        KeyEvent.KEYCODE_3 to KeyLabel.Flick(
            text =  "さ",
            left =  "し",
            up =    "す",
            right = "せ",
            down =  "そ"
        ),
        KeyEvent.KEYCODE_4 to KeyLabel.Flick(
            text =  "た",
            left =  "ち",
            up =    "つ",
            right = "て",
            down =  "と"
        ),
        KeyEvent.KEYCODE_5 to KeyLabel.Flick(
            text =  "な",
            left =  "に",
            up =    "ぬ",
            right = "ね",
            down =  "の"
        ),
        KeyEvent.KEYCODE_6 to KeyLabel.Flick(
            text =  "は",
            left =  "ひ",
            up =    "ふ",
            right = "へ",
            down =  "ほ"
        ),
        KeyEvent.KEYCODE_7 to KeyLabel.Flick(
            text =  "ま",
            left =  "み",
            up =    "む",
            right = "め",
            down =  "も"
        ),
        KeyEvent.KEYCODE_8 to KeyLabel.Flick(
            text =  "や",
            left =  "（",
            up =    "ゆ",
            right = "）",
            down =  "よ"
        ),
        KeyEvent.KEYCODE_9 to KeyLabel.Flick(
            text =  "ら",
            left =  "り",
            up =    "る",
            right = "れ",
            down =  "ろ"
        ),
        KeyEvent.KEYCODE_0 to KeyLabel.Flick(
            text =  "わ",
            left =  "を",
            up =    "ん",
            right = "ー",
            down =  "～"
        ),
        KeyEvent.KEYCODE_COMMA to KeyLabel.Flick(
            text =  "゛゜",
        ),
        KeyEvent.KEYCODE_PERIOD to KeyLabel.Flick(
            text =  "、",
            left =  "。",
            up =    "？",
            right = "！",
            down =  "…"
        )
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