package ee.oyatl.ime.keyboard.layout

object TabletKeyboardRows {
    val DEFAULT: List<String> = listOf(
        "qwert yuiop",
        "asdfg hjkl",
        "zxcvb nm,."
    )

    val SEMICOLON: List<String> = listOf(
        "qwert yuiop",
        "asdfg hjkl;",
        "zxcvb nm,."
    )

    val MINUS: List<String> = listOf(
        "qwert yuiop",
        "asdfg hjkl-",
        "zxcvb nm,."
    )

    val SEMICOLON_QUOTE_SLASH: List<String> = listOf(
        "qwert yuiop",
        "asdfg hjkl;",
        "zxcvb nm\'/"
    )

    val SEMICOLON_SLASH: List<String> = listOf(
        "qwert yuiop",
        "asdfg hjkl;",
        "zxcvb nm,./"
    )

    val SEMICOLON_SLASH_MINUS: List<String> = listOf(
        "qwert yuiop-",
        "asdfg hjkl;",
        "zxcvb nm,./"
    )

    val KS: List<String> = listOf(
        "qwert yuiop",
        "asdfg hjkl",
        "zxcv bnm,."
    )

    val JIS: List<String> = listOf(
        "12345 67890-=",
        "qwert yuiop[]",
        "asdfg hjkl;'\\",
        "zxcvb nm,./`",
    )

    val HANGUL_OLD_393: List<String> = listOf(
        "`1234567890",
        "qwertyuiop[]",
        "asdfghjkl;'",
        "zxcvbnm,./",
    )

    val DVORAK: List<String> = listOf(
        "qwert yuiop",
        "asdfg hjkl;",
        "xcvb nm,./"
    )

    val NUMBERS: List<String> = listOf(
        "12345 67890"
    )
}