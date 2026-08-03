package ee.oyatl.ime.keyboard.listener

object EmptyListener: KeyboardListener {
    override fun onKeyDown(keyCode: Int, metaState: Int): Boolean = false
    override fun onKeyUp(keyCode: Int, metaState: Int): Boolean = false
    override fun onReset() = Unit
}