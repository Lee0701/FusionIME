package ee.oyatl.ime.keyboard.layout

object MobileKeyboardRows {
    val DEFAULT: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm"
    )

    val SEMICOLON: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl;",
        "zxcvbnm"
    )

    val MINUS: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl-",
        "zxcvbnm"
    )

    val SEMICOLON_QUOTE_SLASH: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl;",
        "zxcvbnm\'/"
    )

    val HALF_GRID: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl;",
        "zxcvbnm,."
    )

    val GRID: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl;",
        "zxcvbnm,./"
    )

    val JIS: List<String> = listOf(
        "1234567890-",
        "qwertyuiop[",
        "asdfghjkl;'",
        "zxcvbnm,.",
    )

    val DVORAK: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl;",
        "cvbnm,."
    )

    val NUMBERS: List<String> = listOf(
        "1234567890"
    )
}