package ee.oyatl.ime.fusion.layout

import ee.oyatl.ime.fusion.keyboard.core.KeyCode

object Hangul2Set {
    val TABLE_KS = mapOf(
        KeyCode.KEYCODE_1 to listOf(0x31, 0x21),
        KeyCode.KEYCODE_2 to listOf(0x32, 0x40),
        KeyCode.KEYCODE_3 to listOf(0x33, 0x23),
        KeyCode.KEYCODE_4 to listOf(0x34, 0x24),
        KeyCode.KEYCODE_5 to listOf(0x35, 0x25),
        KeyCode.KEYCODE_6 to listOf(0x36, 0x5e),
        KeyCode.KEYCODE_7 to listOf(0x37, 0x26),
        KeyCode.KEYCODE_8 to listOf(0x38, 0x2a),
        KeyCode.KEYCODE_9 to listOf(0x39, 0x28),
        KeyCode.KEYCODE_0 to listOf(0x30, 0x29),

        KeyCode.KEYCODE_Q to listOf(0x3142, 0x3143),
        KeyCode.KEYCODE_W to listOf(0x3148, 0x3149),
        KeyCode.KEYCODE_E to listOf(0x3137, 0x3138),
        KeyCode.KEYCODE_R to listOf(0x3131, 0x3132),
        KeyCode.KEYCODE_T to listOf(0x3145, 0x3146),
        KeyCode.KEYCODE_Y to listOf(0x315b, 0x315b),
        KeyCode.KEYCODE_U to listOf(0x3155, 0x3155),
        KeyCode.KEYCODE_I to listOf(0x3151, 0x3151),
        KeyCode.KEYCODE_O to listOf(0x3150, 0x3152),
        KeyCode.KEYCODE_P to listOf(0x3154, 0x3156),

        KeyCode.KEYCODE_A to listOf(0x3141, 0x3141),
        KeyCode.KEYCODE_S to listOf(0x3134, 0x3134),
        KeyCode.KEYCODE_D to listOf(0x3147, 0x3147),
        KeyCode.KEYCODE_F to listOf(0x3139, 0x3139),
        KeyCode.KEYCODE_G to listOf(0x314e, 0x314e),
        KeyCode.KEYCODE_H to listOf(0x3157, 0x3157),
        KeyCode.KEYCODE_J to listOf(0x3153, 0x3153),
        KeyCode.KEYCODE_K to listOf(0x314f, 0x314f),
        KeyCode.KEYCODE_L to listOf(0x3163, 0x3163),

        KeyCode.KEYCODE_Z to listOf(0x314b, 0x314b),
        KeyCode.KEYCODE_X to listOf(0x314c, 0x314c),
        KeyCode.KEYCODE_C to listOf(0x314a, 0x314a),
        KeyCode.KEYCODE_V to listOf(0x314d, 0x314d),
        KeyCode.KEYCODE_B to listOf(0x3160, 0x3160),
        KeyCode.KEYCODE_N to listOf(0x315c, 0x315c),
        KeyCode.KEYCODE_M to listOf(0x3161, 0x3161)
    )

    val COMB_KS = mapOf(
        0x1169 to 0x1161 to 0x116a,
        0x1169 to 0x1162 to 0x116b,
        0x1169 to 0x1175 to 0x116c,
        0x116e to 0x1165 to 0x116f,
        0x116e to 0x1166 to 0x1170,
        0x116e to 0x1175 to 0x1171,
        0x1173 to 0x1175 to 0x1174,

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
        0x11b8 to 0x11ba to 0x11b9
    )
}