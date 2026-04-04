package ee.oyatl.ime.keyboard.listener

interface KeyboardListener {
    fun onKeyDown(keyCode: Int, metaState: Int)
    fun onKeyUp(keyCode: Int, metaState: Int)
    fun onReset()
}