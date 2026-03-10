package ee.oyatl.ime.keyboard.layout

import android.view.KeyEvent

object LayoutLatin {
    val KEYCODE_MAP_DVORAK = mapOf(
        KeyEvent.KEYCODE_MINUS to KeyEvent.KEYCODE_LEFT_BRACKET,
        KeyEvent.KEYCODE_EQUALS to KeyEvent.KEYCODE_RIGHT_BRACKET,

        KeyEvent.KEYCODE_Q to KeyEvent.KEYCODE_APOSTROPHE,
        KeyEvent.KEYCODE_W to KeyEvent.KEYCODE_COMMA,
        KeyEvent.KEYCODE_E to KeyEvent.KEYCODE_PERIOD,
        KeyEvent.KEYCODE_R to KeyEvent.KEYCODE_P,
        KeyEvent.KEYCODE_T to KeyEvent.KEYCODE_Y,
        KeyEvent.KEYCODE_Y to KeyEvent.KEYCODE_F,
        KeyEvent.KEYCODE_U to KeyEvent.KEYCODE_G,
        KeyEvent.KEYCODE_I to KeyEvent.KEYCODE_C,
        KeyEvent.KEYCODE_O to KeyEvent.KEYCODE_R,
        KeyEvent.KEYCODE_P to KeyEvent.KEYCODE_L,
        KeyEvent.KEYCODE_LEFT_BRACKET to KeyEvent.KEYCODE_SLASH,
        KeyEvent.KEYCODE_RIGHT_BRACKET to KeyEvent.KEYCODE_EQUALS,

        KeyEvent.KEYCODE_A to KeyEvent.KEYCODE_A,
        KeyEvent.KEYCODE_S to KeyEvent.KEYCODE_O,
        KeyEvent.KEYCODE_D to KeyEvent.KEYCODE_E,
        KeyEvent.KEYCODE_F to KeyEvent.KEYCODE_U,
        KeyEvent.KEYCODE_G to KeyEvent.KEYCODE_I,
        KeyEvent.KEYCODE_H to KeyEvent.KEYCODE_D,
        KeyEvent.KEYCODE_J to KeyEvent.KEYCODE_H,
        KeyEvent.KEYCODE_K to KeyEvent.KEYCODE_T,
        KeyEvent.KEYCODE_L to KeyEvent.KEYCODE_N,
        KeyEvent.KEYCODE_SEMICOLON to KeyEvent.KEYCODE_S,
        KeyEvent.KEYCODE_APOSTROPHE to KeyEvent.KEYCODE_MINUS,

        KeyEvent.KEYCODE_Z to KeyEvent.KEYCODE_SEMICOLON,
        KeyEvent.KEYCODE_X to KeyEvent.KEYCODE_Q,
        KeyEvent.KEYCODE_C to KeyEvent.KEYCODE_J,
        KeyEvent.KEYCODE_V to KeyEvent.KEYCODE_K,
        KeyEvent.KEYCODE_B to KeyEvent.KEYCODE_X,
        KeyEvent.KEYCODE_N to KeyEvent.KEYCODE_B,
        KeyEvent.KEYCODE_M to KeyEvent.KEYCODE_M,
        KeyEvent.KEYCODE_COMMA to KeyEvent.KEYCODE_W,
        KeyEvent.KEYCODE_PERIOD to KeyEvent.KEYCODE_V,
        KeyEvent.KEYCODE_SLASH to KeyEvent.KEYCODE_Z,
    )

    val KEYCODE_MAP_COLEMAK = mapOf(
        KeyEvent.KEYCODE_Q to KeyEvent.KEYCODE_Q,
        KeyEvent.KEYCODE_W to KeyEvent.KEYCODE_W,
        KeyEvent.KEYCODE_E to KeyEvent.KEYCODE_F,
        KeyEvent.KEYCODE_R to KeyEvent.KEYCODE_P,
        KeyEvent.KEYCODE_T to KeyEvent.KEYCODE_G,
        KeyEvent.KEYCODE_Y to KeyEvent.KEYCODE_J,
        KeyEvent.KEYCODE_U to KeyEvent.KEYCODE_L,
        KeyEvent.KEYCODE_I to KeyEvent.KEYCODE_U,
        KeyEvent.KEYCODE_O to KeyEvent.KEYCODE_Y,
        KeyEvent.KEYCODE_P to KeyEvent.KEYCODE_SEMICOLON,

        KeyEvent.KEYCODE_A to KeyEvent.KEYCODE_A,
        KeyEvent.KEYCODE_S to KeyEvent.KEYCODE_R,
        KeyEvent.KEYCODE_D to KeyEvent.KEYCODE_S,
        KeyEvent.KEYCODE_F to KeyEvent.KEYCODE_T,
        KeyEvent.KEYCODE_G to KeyEvent.KEYCODE_D,
        KeyEvent.KEYCODE_H to KeyEvent.KEYCODE_H,
        KeyEvent.KEYCODE_J to KeyEvent.KEYCODE_N,
        KeyEvent.KEYCODE_K to KeyEvent.KEYCODE_E,
        KeyEvent.KEYCODE_L to KeyEvent.KEYCODE_I,
        KeyEvent.KEYCODE_SEMICOLON to KeyEvent.KEYCODE_O,

        KeyEvent.KEYCODE_Z to KeyEvent.KEYCODE_Z,
        KeyEvent.KEYCODE_X to KeyEvent.KEYCODE_X,
        KeyEvent.KEYCODE_C to KeyEvent.KEYCODE_C,
        KeyEvent.KEYCODE_V to KeyEvent.KEYCODE_V,
        KeyEvent.KEYCODE_B to KeyEvent.KEYCODE_B,
        KeyEvent.KEYCODE_N to KeyEvent.KEYCODE_K,
        KeyEvent.KEYCODE_M to KeyEvent.KEYCODE_M,
    )

}