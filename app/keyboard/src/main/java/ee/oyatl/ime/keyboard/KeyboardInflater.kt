package ee.oyatl.ime.keyboard

interface KeyboardInflater {
    val keyboardParams: KeyboardParams
    val keyCodeMapper: KeyCodeMapper
    fun inflate(
        configuration: KeyboardConfiguration,
        contentRows: List<List<Int>>
    ): Keyboard
}