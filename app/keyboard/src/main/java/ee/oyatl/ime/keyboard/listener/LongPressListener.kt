package ee.oyatl.ime.keyboard.listener

interface LongPressListener {
    fun onKeyLongPress(keyCode: Int, metaState: Int): Boolean
    fun onReset()
}