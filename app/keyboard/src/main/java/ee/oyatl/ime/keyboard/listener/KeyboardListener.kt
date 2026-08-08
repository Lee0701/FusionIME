package ee.oyatl.ime.keyboard.listener

interface KeyboardListener {
    fun onKeyDown(keyCode: Int, metaState: Int): Boolean
    fun onKeyUp(keyCode: Int, metaState: Int): Boolean
    fun onReset()
}