package ee.oyatl.ime.keyboard

interface KeyboardInflater {
    fun inflate(
        configuration: KeyboardConfiguration,
        contentRows: List<List<Int>>,
        params: KeyboardParams
    ): Keyboard
}