package ee.oyatl.ime.keyboard

interface KeyboardInflater {
    val keyboardParams: KeyboardParams
    fun inflate(
        configuration: KeyboardConfiguration,
        contentRows: List<String>,
        keyCodeMapper: KeyCodeMapper
    ): Keyboard
}