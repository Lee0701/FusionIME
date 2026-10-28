package ee.oyatl.ime.keyboard

import android.view.KeyEvent
import ee.oyatl.ime.theme.KeyType

enum class SpecialKeyType(
    val keyCode: Int,
    val keyType: KeyType,
    val iconRes: Int?
) {
    Default(
        -1,
        KeyType.Modifier,
        iconRes = null
    ),

    LeftShift(
        keyCode = KeyEvent.KEYCODE_SHIFT_LEFT,
        keyType = KeyType.Modifier,
        iconRes = R.drawable.keyic_shift
    ),

    RightShift(
        keyCode = KeyEvent.KEYCODE_SHIFT_RIGHT,
        keyType = KeyType.Modifier,
        iconRes = R.drawable.keyic_shift
    ),

    Language(
        keyCode = KeyEvent.KEYCODE_LANGUAGE_SWITCH,
        keyType = KeyType.Modifier,
        iconRes = R.drawable.keyic_language
    ),

    Symbol(
        keyCode = KeyEvent.KEYCODE_SYM,
        keyType = KeyType.Modifier,
        iconRes = R.drawable.keyic_option
    ),

    Numbers(
        keyCode = KeyEvent.KEYCODE_NUM,
        keyType = KeyType.Modifier,
        iconRes = R.drawable.keyic_numbers
    ),

    Return(
        keyCode = KeyEvent.KEYCODE_ENTER,
        keyType = KeyType.Return,
        iconRes = R.drawable.keyic_return
    ),

    Delete(
        keyCode = KeyEvent.KEYCODE_DEL,
        keyType = KeyType.Modifier,
        iconRes = R.drawable.keyic_delete
    ),

    Space(
        keyCode = KeyEvent.KEYCODE_SPACE,
        keyType = KeyType.Default,
        iconRes = R.drawable.keyic_space
    ),

    Left(
        keyCode = KeyEvent.KEYCODE_DPAD_LEFT,
        keyType = KeyType.Modifier,
        iconRes = R.drawable.keyic_left
    ),

    Right(
        keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
        keyType = KeyType.Modifier,
        iconRes = R.drawable.keyic_right
    );

    companion object {
        val keyCodeMap = SpecialKeyType.entries.associateBy { it.keyCode }
        fun ofKeyCode(keyCode: Int): SpecialKeyType? {
            return keyCodeMap[keyCode]
        }
    }
}