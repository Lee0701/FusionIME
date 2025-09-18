package ee.oyatl.ime.keyboard.layout

object KeyboardTemplates {
    val MOBILE: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm"
    )

    val MOBILE_SEMICOLON: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl;",
        "zxcvbnm"
    )

    val MOBILE_MINUS: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl-",
        "zxcvbnm"
    )

    val MOBILE_QUOTE: List<String> = listOf(
        "1234567890",
        "qwertyuiop",
        "asdfghjkl;",
        "zxcvbnm\'",
    )

    val MOBILE_GRID: List<String> = listOf(
        "1234567890",
        "qwertyuiop",
        "asdfghjkl;",
        "zxcvbnm,./",
    )

    val MOBILE_DVORAK: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl;",
        "cvbnm,.",
    )

}