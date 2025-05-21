package ee.oyatl.ime.fusion.korean.layout

import android.view.KeyEvent

object Hangul2Set {
    val TABLE_KS = mapOf(
        KeyEvent.KEYCODE_1 to listOf(0x31, 0x21),
        KeyEvent.KEYCODE_2 to listOf(0x32, 0x40),
        KeyEvent.KEYCODE_3 to listOf(0x33, 0x23),
        KeyEvent.KEYCODE_4 to listOf(0x34, 0x24),
        KeyEvent.KEYCODE_5 to listOf(0x35, 0x25),
        KeyEvent.KEYCODE_6 to listOf(0x36, 0x5e),
        KeyEvent.KEYCODE_7 to listOf(0x37, 0x26),
        KeyEvent.KEYCODE_8 to listOf(0x38, 0x2a),
        KeyEvent.KEYCODE_9 to listOf(0x39, 0x28),
        KeyEvent.KEYCODE_0 to listOf(0x30, 0x29),

        KeyEvent.KEYCODE_Q to listOf(0x3142, 0x3143),
        KeyEvent.KEYCODE_W to listOf(0x3148, 0x3149),
        KeyEvent.KEYCODE_E to listOf(0x3137, 0x3138),
        KeyEvent.KEYCODE_R to listOf(0x3131, 0x3132),
        KeyEvent.KEYCODE_T to listOf(0x3145, 0x3146),
        KeyEvent.KEYCODE_Y to listOf(0x315b, 0x315b),
        KeyEvent.KEYCODE_U to listOf(0x3155, 0x3155),
        KeyEvent.KEYCODE_I to listOf(0x3151, 0x3151),
        KeyEvent.KEYCODE_O to listOf(0x3150, 0x3152),
        KeyEvent.KEYCODE_P to listOf(0x3154, 0x3156),

        KeyEvent.KEYCODE_A to listOf(0x3141, 0x3141),
        KeyEvent.KEYCODE_S to listOf(0x3134, 0x3134),
        KeyEvent.KEYCODE_D to listOf(0x3147, 0x3147),
        KeyEvent.KEYCODE_F to listOf(0x3139, 0x3139),
        KeyEvent.KEYCODE_G to listOf(0x314e, 0x314e),
        KeyEvent.KEYCODE_H to listOf(0x3157, 0x3157),
        KeyEvent.KEYCODE_J to listOf(0x3153, 0x3153),
        KeyEvent.KEYCODE_K to listOf(0x314f, 0x314f),
        KeyEvent.KEYCODE_L to listOf(0x3163, 0x3163),

        KeyEvent.KEYCODE_Z to listOf(0x314b, 0x314b),
        KeyEvent.KEYCODE_X to listOf(0x314c, 0x314c),
        KeyEvent.KEYCODE_C to listOf(0x314a, 0x314a),
        KeyEvent.KEYCODE_V to listOf(0x314d, 0x314d),
        KeyEvent.KEYCODE_B to listOf(0x3160, 0x3160),
        KeyEvent.KEYCODE_N to listOf(0x315c, 0x315c),
        KeyEvent.KEYCODE_M to listOf(0x3161, 0x3161)
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