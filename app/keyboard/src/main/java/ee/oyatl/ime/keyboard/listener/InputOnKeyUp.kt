package ee.oyatl.ime.keyboard.listener

import ee.oyatl.ime.keyboard.touchhandler.FlickDirection

class InputOnKeyUp(
    val listener: KeyboardListener
): KeyboardListener, FlickListener {
    override fun onReset() = Unit

    override fun onKeyDown(keyCode: Int, metaState: Int): Boolean {
        return true
    }

    override fun onKeyUp(keyCode: Int, metaState: Int): Boolean {
        listener.onKeyDown(keyCode, metaState)
        listener.onKeyUp(keyCode, metaState)
        return true
    }

    override fun onFlick(
        keyCode: Int,
        direction: FlickDirection
    ): Boolean {
        return if(listener is FlickListener) listener.onFlick(keyCode, direction)
        else false
    }
}