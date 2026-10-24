package ee.oyatl.ime.keyboard.listener

import ee.oyatl.ime.keyboard.touchhandler.FlickDirection

class CompoundKeyboardListener(
    val listeners: List<KeyboardListener>
): KeyboardListener, FlickListener, LongPressListener {
    constructor(vararg listeners: KeyboardListener): this(listeners.toList())

    override fun onKeyDown(keyCode: Int, metaState: Int): Boolean {
        return listeners.any { it.onKeyDown(keyCode, metaState) }
    }

    override fun onKeyUp(keyCode: Int, metaState: Int): Boolean {
        return listeners.any { it.onKeyUp(keyCode, metaState) }
    }

    override fun onFlick(
        keyCode: Int,
        direction: FlickDirection
    ): Boolean {
        return listeners
            .filterIsInstance<FlickListener>()
            .any { it.onFlick(keyCode, direction) }
    }

    override fun onKeyLongPress(keyCode: Int, metaState: Int): Boolean {
        return listeners
            .filterIsInstance<LongPressListener>()
            .any { it.onKeyLongPress(keyCode, metaState) }
    }

    override fun onReset() {
        listeners.forEach { it.onReset() }
    }
}