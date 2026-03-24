package ee.oyatl.ime.keyboard.layout

object MobileKeyboardRows {
    val DEFAULT: List<String> = listOf(
        "qwert yuiop",
        "asdfg hjkl",
        "zxcvb nm"
    )

    val SEMICOLON: List<String> = listOf(
        "qwert yuiop",
        "asdfg hjkl;",
        "zxcvb nm"
    )

    val MINUS: List<String> = listOf(
        "qwert yuiop",
        "asdfg hjkl-",
        "zxcvb nm"
    )

    val SEMICOLON_QUOTE: List<String> = listOf(
        "qwert yuiop",
        "asdfg hjkl;",
        "zxcvb nm\'"
    )

    val HALF_GRID: List<String> = listOf(
        "qwert yuiop",
        "asdfg hjkl;",
        "zxcvb nm,."
    )

    val GRID: List<String> = listOf(
        "qwert yuiop",
        "asdfg hjkl;",
        "zxcvb nm,./"
    )

    val KS: List<String> = listOf(
        "qwert yuiop",
        "asdfg hjkl",
        "zxcv bnm"
    )

    val JIS: List<String> = listOf(
        "12345 67890-",
        "qwert yuiop[",
        "asdfg hjkl;'",
        "zxcvb nm,.",
    )

    val DVORAK: List<String> = listOf(
        "qwert yuiop",
        "asdfg hjkl;",
        "cvb nm,."
    )

    val NUMBERS: List<String> = listOf(
        "12345 67890"
    )
}