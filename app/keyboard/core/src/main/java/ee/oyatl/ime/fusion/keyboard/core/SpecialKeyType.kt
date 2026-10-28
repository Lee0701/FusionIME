package ee.oyatl.ime.fusion.keyboard.core

enum class SpecialKeyType(
    val keyCode: Int,
    val keyType: KeyType
) {
    Default(
        -1,
        KeyType.Modifier
    ),

    LeftShift(
        keyCode = KeyCode.KEYCODE_SHIFT_LEFT,
        keyType = KeyType.Modifier
    ),

    RightShift(
        keyCode = KeyCode.KEYCODE_SHIFT_RIGHT,
        keyType = KeyType.Modifier
    ),

    Language(
        keyCode = KeyCode.KEYCODE_LANGUAGE_SWITCH,
        keyType = KeyType.Modifier
    ),

    Symbol(
        keyCode = KeyCode.KEYCODE_SYM,
        keyType = KeyType.Modifier
    ),

    Numbers(
        keyCode = KeyCode.KEYCODE_NUM,
        keyType = KeyType.Modifier
    ),

    Return(
        keyCode = KeyCode.KEYCODE_ENTER,
        keyType = KeyType.Return
    ),

    Delete(
        keyCode = KeyCode.KEYCODE_DEL,
        keyType = KeyType.Modifier
    ),

    Space(
        keyCode = KeyCode.KEYCODE_SPACE,
        keyType = KeyType.Default
    ),

    Left(
        keyCode = KeyCode.KEYCODE_DPAD_LEFT,
        keyType = KeyType.Modifier
    ),

    Right(
        keyCode = KeyCode.KEYCODE_DPAD_RIGHT,
        keyType = KeyType.Modifier
    );

    companion object {
        val keyCodeMap = entries.associateBy { it.keyCode }
        fun ofKeyCode(keyCode: Int): SpecialKeyType? {
            return keyCodeMap[keyCode]
        }
    }
}