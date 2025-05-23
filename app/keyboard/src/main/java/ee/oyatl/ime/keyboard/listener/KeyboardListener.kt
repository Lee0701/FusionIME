package ee.oyatl.ime.keyboard.listener

interface KeyboardListener {
    fun onKeyDown(code: Int)
    fun onKeyUp(code: Int)
}