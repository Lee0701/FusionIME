package ee.oyatl.ime.keyboard.listener

import ee.oyatl.ime.fusion.keyboard.core.FlickDirection

class InputOnKeyUp(
    val listener: KeyboardListener,
    val ignoreKeyCodes: Set<Int> = emptySet()
): KeyboardListener, FlickListener, LongPressListener {
    override fun onReset() = Unit

    override fun onKeyDown(keyCode: Int, metaState: Int): Boolean {
        if(keyCode in ignoreKeyCodes) return false
        return true
    }

    override fun onKeyUp(keyCode: Int, metaState: Int): Boolean {
        if(keyCode in ignoreKeyCodes) return false
        listener.onKeyDown(keyCode, metaState)
        listener.onKeyUp(keyCode, metaState)
        return true
    }

    override fun onFlick(
        keyCode: Int,
        direction: FlickDirection
    ): Boolean {
        if(keyCode in ignoreKeyCodes) return false
        if(listener is FlickListener) return listener.onFlick(keyCode, direction)
        return true
    }

    override fun onKeyLongPress(keyCode: Int, metaState: Int): Boolean {
        if(keyCode in ignoreKeyCodes) return false
        if(listener is LongPressListener) return listener.onKeyLongPress(keyCode, metaState)
        return true
    }
}