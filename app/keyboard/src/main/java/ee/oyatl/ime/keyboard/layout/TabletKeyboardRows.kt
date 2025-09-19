package ee.oyatl.ime.keyboard.layout

object TabletKeyboardRows {
    val DEFAULT: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm,."
    )

    val SEMICOLON: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl;",
        "zxcvbnm,."
    )

    val MINUS: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl-",
        "zxcvbnm,."
    )

    val SEMICOLON_QUOTE: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm;\'/"
    )

    val DVORAK: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl;",
        "xcvbnm,./"
    )

    val NUMBERS: List<String> = listOf(
        "1234567890"
    )
}