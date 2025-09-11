package ee.oyatl.ime.keyboard.layout

object KeyboardTemplates {
    val MOBILE: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm"
    )

    val MOBILE_MINUS: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl-",
        "zxcvbnm"
    )

    val MOBILE_WITH_QUOTE: List<String> = listOf(
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
        "',.pyfgcrl",
        "aoeuidhtns",
        "jkxbmwv",
    )

    val MOBILE_COLEMAK: List<String> = listOf(
        "qwfpgjluy;",
        "arstdhneio",
        "zxcvbkm",
    )
}