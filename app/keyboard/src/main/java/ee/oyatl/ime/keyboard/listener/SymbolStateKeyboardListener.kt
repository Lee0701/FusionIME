package ee.oyatl.ime.keyboard.listener

data class SymbolStateKeyboardListener(
    val text: KeyboardListener,
    val symbol: KeyboardListener,
    val number: KeyboardListener
): KeyboardListener {
    override fun onKeyDown(code: Int) {
        text.onKeyDown(code)
    }

    override fun onKeyUp(code: Int) {
        text.onKeyUp(code)
    }
}
