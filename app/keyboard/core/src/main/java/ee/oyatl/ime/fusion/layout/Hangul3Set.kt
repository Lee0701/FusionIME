package ee.oyatl.ime.fusion.layout

import ee.oyatl.ime.fusion.keyboard.core.ExtKeyCode
import ee.oyatl.ime.fusion.keyboard.core.KeyCode

object Hangul3Set {
    val KEYCODE_MAP_390_MOBILE = mapOf(
        KeyCode.KEYCODE_B to ExtKeyCode.KEYCODE_390_0,
        KeyCode.KEYCODE_N to ExtKeyCode.KEYCODE_390_1,
        KeyCode.KEYCODE_M to ExtKeyCode.KEYCODE_390_2,
        KeyCode.KEYCODE_APOSTROPHE to ExtKeyCode.KEYCODE_390_3
    )

    val KEYCODE_MAP_393_MOBILE = mapOf(
        KeyCode.KEYCODE_B to ExtKeyCode.KEYCODE_390_0,
        KeyCode.KEYCODE_N to ExtKeyCode.KEYCODE_390_1,
        KeyCode.KEYCODE_M to ExtKeyCode.KEYCODE_390_2,
        KeyCode.KEYCODE_APOSTROPHE to ExtKeyCode.KEYCODE_390_3,
        KeyCode.KEYCODE_SLASH to ExtKeyCode.KEYCODE_PERIOD_COMMA,
        ExtKeyCode.KEYCODE_PERIOD_COMMA to KeyCode.KEYCODE_GRAVE
    )

    val TABLE_390 = mapOf(
        KeyCode.KEYCODE_GRAVE to listOf(0x60, 0x7e),
        KeyCode.KEYCODE_1 to listOf(0x11c2, 0x11bd),
        KeyCode.KEYCODE_2 to listOf(0x11bb, 0x40),
        KeyCode.KEYCODE_3 to listOf(0x11b8, 0x23),
        KeyCode.KEYCODE_4 to listOf(0x116d, 0x24),
        KeyCode.KEYCODE_5 to listOf(0x1172, 0x25),
        KeyCode.KEYCODE_6 to listOf(0x1163, 0x5e),
        KeyCode.KEYCODE_7 to listOf(0x1168, 0x26),
        KeyCode.KEYCODE_8 to listOf(0x1174, 0x2a),
        KeyCode.KEYCODE_9 to listOf(0x116e, 0x28),
        KeyCode.KEYCODE_0 to listOf(0x110f, 0x29),
        KeyCode.KEYCODE_MINUS to listOf(0x2d, 0x5f),
        KeyCode.KEYCODE_EQUALS to listOf(0x3d, 0x2b),

        KeyCode.KEYCODE_Q to listOf(0x11ba, 0x11c1),
        KeyCode.KEYCODE_W to listOf(0x11af, 0x11c0),
        KeyCode.KEYCODE_E to listOf(0x1167, 0x11bf),
        KeyCode.KEYCODE_R to listOf(0x1162, 0x1164),
        KeyCode.KEYCODE_T to listOf(0x1165, 0x3b),
        KeyCode.KEYCODE_Y to listOf(0x1105, 0x3c),
        KeyCode.KEYCODE_U to listOf(0x1103, 0x37),
        KeyCode.KEYCODE_I to listOf(0x1106, 0x38),
        KeyCode.KEYCODE_O to listOf(0x110e, 0x39),
        KeyCode.KEYCODE_P to listOf(0x1111, 0x3e),
        KeyCode.KEYCODE_LEFT_BRACKET to listOf(0x5b, 0x7b),
        KeyCode.KEYCODE_RIGHT_BRACKET to listOf(0x5d, 0x7d),
        KeyCode.KEYCODE_BACKSLASH to listOf(0x5c, 0x7c),

        KeyCode.KEYCODE_A to listOf(0x11bc, 0x11ae),
        KeyCode.KEYCODE_S to listOf(0x11ab, 0x11ad),
        KeyCode.KEYCODE_D to listOf(0x1175, 0x11b0),
        KeyCode.KEYCODE_F to listOf(0x1161, 0x11a9),
        KeyCode.KEYCODE_G to listOf(0x1173, 0x2f),
        KeyCode.KEYCODE_H to listOf(0x1102, 0x27),
        KeyCode.KEYCODE_J to listOf(0x110b, 0x34),
        KeyCode.KEYCODE_K to listOf(0x1100, 0x35),
        KeyCode.KEYCODE_L to listOf(0x110c, 0x36),
        KeyCode.KEYCODE_SEMICOLON to listOf(0x1107, 0x3a),
        KeyCode.KEYCODE_APOSTROPHE to listOf(0x1110, 0x22),

        KeyCode.KEYCODE_Z to listOf(0x11b7, 0x11be),
        KeyCode.KEYCODE_X to listOf(0x11a8, 0x11b9),
        KeyCode.KEYCODE_C to listOf(0x1166, 0x11b1),
        KeyCode.KEYCODE_V to listOf(0x1169, 0x11b6),
        KeyCode.KEYCODE_B to listOf(0x116e, 0x21),
        KeyCode.KEYCODE_N to listOf(0x1109, 0x30),
        KeyCode.KEYCODE_M to listOf(0x1112, 0x31),
        KeyCode.KEYCODE_COMMA to listOf(0x2c, 0x32),
        KeyCode.KEYCODE_PERIOD to listOf(0x2e, 0x33),
        KeyCode.KEYCODE_SLASH to listOf(0x1169, 0x3f),

        ExtKeyCode.KEYCODE_390_0 to listOf(0x116e, 0x30),
        ExtKeyCode.KEYCODE_390_1 to listOf(0x1109, 0x31),
        ExtKeyCode.KEYCODE_390_2 to listOf(0x1112, 0x32),
        ExtKeyCode.KEYCODE_390_3 to listOf(0x1110, 0x33)
    )

    val TABLE_391 = mapOf(
        KeyCode.KEYCODE_GRAVE to listOf(0x2a, 0x203b),
        KeyCode.KEYCODE_1 to listOf(0x11c2, 0x11a9),
        KeyCode.KEYCODE_2 to listOf(0x11bb, 0x11b0),
        KeyCode.KEYCODE_3 to listOf(0x11b8, 0x11bd),
        KeyCode.KEYCODE_4 to listOf(0x116d, 0x11b5),
        KeyCode.KEYCODE_5 to listOf(0x1172, 0x11b4),
        KeyCode.KEYCODE_6 to listOf(0x1163, 0x3d),
        KeyCode.KEYCODE_7 to listOf(0x1168, 0x201c),
        KeyCode.KEYCODE_8 to listOf(0x1174, 0x201d),
        KeyCode.KEYCODE_9 to listOf(0x116e, 0x27),
        KeyCode.KEYCODE_0 to listOf(0x110f, 0x7e),
        KeyCode.KEYCODE_MINUS to listOf(0x29, 0x3b),
        KeyCode.KEYCODE_EQUALS to listOf(0x3e, 0x2b),

        KeyCode.KEYCODE_Q to listOf(0x11ba, 0x11c1),
        KeyCode.KEYCODE_W to listOf(0x11af, 0x11c0),
        KeyCode.KEYCODE_E to listOf(0x1167, 0x11ac),
        KeyCode.KEYCODE_R to listOf(0x1162, 0x11b6),
        KeyCode.KEYCODE_T to listOf(0x1165, 0x11b3),
        KeyCode.KEYCODE_Y to listOf(0x1105, 0x35),
        KeyCode.KEYCODE_U to listOf(0x1103, 0x36),
        KeyCode.KEYCODE_I to listOf(0x1106, 0x37),
        KeyCode.KEYCODE_O to listOf(0x110e, 0x38),
        KeyCode.KEYCODE_P to listOf(0x1111, 0x39),
        KeyCode.KEYCODE_LEFT_BRACKET to listOf(0x28, 0x25),
        KeyCode.KEYCODE_RIGHT_BRACKET to listOf(0x3c, 0x2f),
        KeyCode.KEYCODE_BACKSLASH to listOf(0x3a, 0x5c),

        KeyCode.KEYCODE_A to listOf(0x11bc, 0x11ae),
        KeyCode.KEYCODE_S to listOf(0x11ab, 0x11ad),
        KeyCode.KEYCODE_D to listOf(0x1175, 0x11b2),
        KeyCode.KEYCODE_F to listOf(0x1161, 0x11b1),
        KeyCode.KEYCODE_G to listOf(0x1173, 0x1164),
        KeyCode.KEYCODE_H to listOf(0x1102, 0x30),
        KeyCode.KEYCODE_J to listOf(0x110b, 0x31),
        KeyCode.KEYCODE_K to listOf(0x1100, 0x32),
        KeyCode.KEYCODE_L to listOf(0x110c, 0x33),
        KeyCode.KEYCODE_SEMICOLON to listOf(0x1107, 0x34),
        KeyCode.KEYCODE_APOSTROPHE to listOf(0x1110, 0xb7),

        KeyCode.KEYCODE_Z to listOf(0x11b7, 0x11be),
        KeyCode.KEYCODE_X to listOf(0x11a8, 0x11b9),
        KeyCode.KEYCODE_C to listOf(0x1166, 0x11bf),
        KeyCode.KEYCODE_V to listOf(0x1169, 0x11aa),
        KeyCode.KEYCODE_B to listOf(0x116e, 0x3f),
        KeyCode.KEYCODE_N to listOf(0x1109, 0x2d),
        KeyCode.KEYCODE_M to listOf(0x1112, 0x22),
        KeyCode.KEYCODE_COMMA to listOf(0x2c, 0x2c),
        KeyCode.KEYCODE_PERIOD to listOf(0x2e, 0x2e),
        KeyCode.KEYCODE_SLASH to listOf(0x1169, 0x21)
    )

    val COMBINATION_390_391 = mapOf(
        0x1100 to 0x1100 to 0x1101,
        0x1103 to 0x1103 to 0x1104,
        0x1107 to 0x1107 to 0x1108,
        0x1109 to 0x1109 to 0x110a,
        0x110c to 0x110c to 0x110d,

        0x1169 to 0x1161 to 0x116a,
        0x1169 to 0x1162 to 0x116b,
        0x1169 to 0x1175 to 0x116c,
        0x116e to 0x1165 to 0x116f,
        0x116e to 0x1166 to 0x1170,
        0x116e to 0x1175 to 0x1171,

        0x11a8 to 0x11a8 to 0x11a9,
        0x11a8 to 0x11ba to 0x11aa,
        0x11ab to 0x11bd to 0x11ac,
        0x11ab to 0x11c2 to 0x11ad,
        0x11af to 0x11a8 to 0x11b0,
        0x11af to 0x11b7 to 0x11b1,
        0x11af to 0x11b8 to 0x11b2,
        0x11af to 0x11ba to 0x11b3,
        0x11af to 0x11c0 to 0x11b4,
        0x11af to 0x11c1 to 0x11b5,
        0x11af to 0x11c2 to 0x11b6,
        0x11b8 to 0x11ba to 0x11b9,
        0x11ba to 0x11ba to 0x11bb
    )

    val TABLE_391_STRICT = TABLE_391 + mapOf(
        KeyCode.KEYCODE_9 to listOf(0x100116e, 0x27),
        KeyCode.KEYCODE_SLASH to listOf(0x1001169, 0x21)
    )

    val COMBINATION_391_STRICT = mapOf(
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