package ee.oyatl.ime.keyboard.listener

class InputOnKeyUp(
    val listener: KeyboardListener
): KeyboardListener {
    override fun onReset() = Unit

    override fun onKeyDown(keyCode: Int, metaState: Int): Boolean {
        return true
    }

    override fun onKeyUp(keyCode: Int, metaState: Int): Boolean {
        listener.onKeyDown(keyCode, metaState)
        listener.onKeyUp(keyCode, metaState)
        return true
    }
}