package ee.oyatl.ime.keyboard.listener

import ee.oyatl.ime.keyboard.touchhandler.FlickDirection

class ParallelKeyboardListener(
    val listeners: List<KeyboardListener>
): KeyboardListener, FlickListener, LongPressListener {
    constructor(vararg listeners: KeyboardListener): this(listeners.toList())

    override fun onKeyDown(keyCode: Int, metaState: Int): Boolean {
        return listeners.map { it.onKeyDown(keyCode, metaState) }.all { it }
    }

    override fun onKeyUp(keyCode: Int, metaState: Int): Boolean {
        return listeners.map { it.onKeyUp(keyCode, metaState) }.all { it }
    }

    override fun onFlick(
        keyCode: Int,
        direction: FlickDirection
    ): Boolean {
        return listeners
            .filterIsInstance<FlickListener>()
            .map { it.onFlick(keyCode, direction) }
            .all { it }
    }

    override fun onKeyLongPress(keyCode: Int, metaState: Int): Boolean {
        return listeners
            .filterIsInstance<LongPressListener>()
            .map { it.onKeyLongPress(keyCode, metaState) }
            .all { it }
    }

    override fun onReset() {
        listeners.forEach { it.onReset() }
    }
}