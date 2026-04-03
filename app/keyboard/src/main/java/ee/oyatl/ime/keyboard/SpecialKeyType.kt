package ee.oyatl.ime.keyboard

import android.view.KeyEvent

enum class SpecialKeyType(
    val keyCode: Int,
    val themeRes: Int,
    val iconRes: Int?
) {
    Default(
        -1,
        R.style.Theme_FusionIME_Keyboard_Key_Modifier,
        iconRes = null
    ),

    LeftShift(
        keyCode = KeyEvent.KEYCODE_SHIFT_LEFT,
        themeRes = R.style.Theme_FusionIME_Keyboard_Key_Modifier,
        iconRes = R.drawable.keyic_shift
    ),

    RightShift(
        keyCode = KeyEvent.KEYCODE_SHIFT_RIGHT,
        themeRes = R.style.Theme_FusionIME_Keyboard_Key_Modifier,
        iconRes = R.drawable.keyic_shift
    ),

    Language(
        keyCode = KeyEvent.KEYCODE_LANGUAGE_SWITCH,
        themeRes = R.style.Theme_FusionIME_Keyboard_Key_Modifier,
        iconRes = R.drawable.keyic_language
    ),

    Symbol(
        keyCode = KeyEvent.KEYCODE_SYM,
        themeRes = R.style.Theme_FusionIME_Keyboard_Key_Modifier,
        iconRes = R.drawable.keyic_option
    ),

    Numbers(
        keyCode = KeyEvent.KEYCODE_NUM,
        themeRes = R.style.Theme_FusionIME_Keyboard_Key_Modifier,
        iconRes = R.drawable.keyic_numbers
    ),

    Return(
        keyCode = KeyEvent.KEYCODE_ENTER,
        themeRes = R.style.Theme_FusionIME_Keyboard_Key_Return,
        iconRes = R.drawable.keyic_return
    ),

    Delete(
        keyCode = KeyEvent.KEYCODE_DEL,
        themeRes = R.style.Theme_FusionIME_Keyboard_Key_Modifier,
        iconRes = R.drawable.keyic_delete
    ),

    Space(
        keyCode = KeyEvent.KEYCODE_SPACE,
        themeRes = R.style.Theme_FusionIME_Keyboard_Key,
        iconRes = R.drawable.keyic_space
    ),

    Left(
        keyCode = KeyEvent.KEYCODE_DPAD_LEFT,
        themeRes = R.style.Theme_FusionIME_Keyboard_Key_Modifier,
        iconRes = R.drawable.keyic_left
    ),

    Right(
        keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
        themeRes = R.style.Theme_FusionIME_Keyboard_Key_Modifier,
        iconRes = R.drawable.keyic_right
    );

    companion object {
        val keyCodeMap = SpecialKeyType.entries.associateBy { it.keyCode }
        fun ofKeyCode(keyCode: Int): SpecialKeyType? {
            return keyCodeMap[keyCode]
        }
    }
}