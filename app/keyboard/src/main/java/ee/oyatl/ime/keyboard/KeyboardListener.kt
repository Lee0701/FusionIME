package ee.oyatl.ime.keyboard

interface KeyboardListener {
    fun onKeyDown(keyCode: Int, metaState: Int)
    fun onKeyUp(keyCode: Int, metaState: Int)
}