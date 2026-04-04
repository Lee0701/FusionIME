package ee.oyatl.ime.keyboard.listener

object EmptyListener: KeyboardListener {
    override fun onKeyDown(keyCode: Int, metaState: Int) = Unit
    override fun onKeyUp(keyCode: Int, metaState: Int) = Unit
    override fun onReset() = Unit
}