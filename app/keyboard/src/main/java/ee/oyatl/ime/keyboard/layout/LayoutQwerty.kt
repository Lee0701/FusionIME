package ee.oyatl.ime.keyboard.layout

object LayoutQwerty {
    val ROWS_LOWER: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm"
    )
    val ROWS_UPPER: List<String> = listOf(
        "QWERTYUIOP",
        "ASDFGHJKL",
        "ZXCVBNM"
    )

    val ROWS_ROMAJI_LOWER: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl-",
        "zxcvbnm"
    )
    val ROWS_ROMAJI_UPPER: List<String> = listOf(
        "QWERTYUIOP",
        "ASDFGHJKL-",
        "ZXCVBNM"
    )

    val ROWS_PINYIN_LOWER: List<String> = listOf(
        "qwertyuiop",
        "asdfghjkl'",
        "zxcvbnm"
    )
    val ROWS_PINYIN_UPPER: List<String> = listOf(
        "QWERTYUIOP",
        "ASDFGHJKL'",
        "ZXCVBNM"
    )

    const val NUMBER_ROW_LOWER: String = "1234567890"
    const val NUMBER_ROW_UPPER: String = "!@#$%^&*()"
}