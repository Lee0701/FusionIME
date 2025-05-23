package ee.oyatl.ime.keyboard.listener

class ClickKeyOnReleaseListener(
    private val listener: OnKeyClickListener
): KeyboardListener {
    override fun onKeyDown(code: Int) {
    }

    override fun onKeyUp(code: Int) {
        listener.onKeyClick(code)
    }
}