package ee.oyatl.ime.keyboard.rewrite

interface KeyboardInflater {
    fun inflate(
        configuration: KeyboardConfiguration,
        contentRows: List<List<Int>>,
        params: KeyboardParams
    ): Keyboard
}