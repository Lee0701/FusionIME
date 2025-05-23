package ee.oyatl.ime.fusion.korean.layout

import android.view.KeyEvent

object Hangul3Set {
    val TABLE_391 = mapOf(
        KeyEvent.KEYCODE_GRAVE to listOf(0x2a, 0x203b),
        KeyEvent.KEYCODE_1 to listOf(0x11c2, 0x11a9),
        KeyEvent.KEYCODE_2 to listOf(0x11bb, 0x11b0),
        KeyEvent.KEYCODE_3 to listOf(0x11b8, 0x11bd),
        KeyEvent.KEYCODE_4 to listOf(0x116d, 0x11b5),
        KeyEvent.KEYCODE_5 to listOf(0x1172, 0x11b4),
        KeyEvent.KEYCODE_6 to listOf(0x1163, 0x3d),
        KeyEvent.KEYCODE_7 to listOf(0x1168, 0x201c),
        KeyEvent.KEYCODE_8 to listOf(0x1174, 0x201d),
        KeyEvent.KEYCODE_9 to listOf(0x100116e, 0x27),
        KeyEvent.KEYCODE_0 to listOf(0x110f, 0x7e),
        KeyEvent.KEYCODE_MINUS to listOf(0x29, 0x3b),
        KeyEvent.KEYCODE_EQUALS to listOf(0x3e, 0x2b),

        KeyEvent.KEYCODE_Q to listOf(0x11ba, 0x11c1),
        KeyEvent.KEYCODE_W to listOf(0x11af, 0x11c0),
        KeyEvent.KEYCODE_E to listOf(0x1167, 0x11ac),
        KeyEvent.KEYCODE_R to listOf(0x1162, 0x11b6),
        KeyEvent.KEYCODE_T to listOf(0x1165, 0x11b3),
        KeyEvent.KEYCODE_Y to listOf(0x1105, 0x35),
        KeyEvent.KEYCODE_U to listOf(0x1103, 0x36),
        KeyEvent.KEYCODE_I to listOf(0x1106, 0x37),
        KeyEvent.KEYCODE_O to listOf(0x110e, 0x38),
        KeyEvent.KEYCODE_P to listOf(0x1111, 0x39),
        KeyEvent.KEYCODE_LEFT_BRACKET to listOf(0x28, 0x25),
        KeyEvent.KEYCODE_RIGHT_BRACKET to listOf(0x3c, 0x2f),
        KeyEvent.KEYCODE_BACKSLASH to listOf(0x3a, 0x5c),

        KeyEvent.KEYCODE_A to listOf(0x11bc, 0x11ae),
        KeyEvent.KEYCODE_S to listOf(0x11ab, 0x11ad),
        KeyEvent.KEYCODE_D to listOf(0x1175, 0x11b2),
        KeyEvent.KEYCODE_F to listOf(0x1161, 0x11b1),
        KeyEvent.KEYCODE_G to listOf(0x1173, 0x1164),
        KeyEvent.KEYCODE_H to listOf(0x1102, 0x30),
        KeyEvent.KEYCODE_J to listOf(0x110b, 0x31),
        KeyEvent.KEYCODE_K to listOf(0x1100, 0x32),
        KeyEvent.KEYCODE_L to listOf(0x110c, 0x33),
        KeyEvent.KEYCODE_SEMICOLON to listOf(0x1107, 0x34),
        KeyEvent.KEYCODE_APOSTROPHE to listOf(0x1110, 0xb7),

        KeyEvent.KEYCODE_Z to listOf(0x11b7, 0x11be),
        KeyEvent.KEYCODE_X to listOf(0x11a8, 0x11b9),
        KeyEvent.KEYCODE_C to listOf(0x1166, 0x11bf),
        KeyEvent.KEYCODE_V to listOf(0x1169, 0x11aa),
        KeyEvent.KEYCODE_B to listOf(0x116e, 0x3f),
        KeyEvent.KEYCODE_N to listOf(0x1109, 0x2d),
        KeyEvent.KEYCODE_M to listOf(0x1112, 0x22),
        KeyEvent.KEYCODE_COMMA to listOf(0x2c, 0x2c),
        KeyEvent.KEYCODE_PERIOD to listOf(0x2e, 0x2e),
        KeyEvent.KEYCODE_SLASH to listOf(0x1001169, 0x21)
    )

    val COMBINATION_391 = mapOf(
        0x1100 to 0x1100 to 0x1101,
        0x1103 to 0x1103 to 0x1104,
        0x1107 to 0x1107 to 0x1108,
        0x1109 to 0x1109 to 0x110a,
        0x110c to 0x110c to 0x110d,

        0x1001169 to 0x1161 to 0x116a,
        0x1001169 to 0x1162 to 0x116b,
        0x1001169 to 0x1175 to 0x116c,
        0x100116e to 0x1165 to 0x116f,
        0x100116e to 0x1166 to 0x1170,
        0x100116e to 0x1175 to 0x1171
    )
}