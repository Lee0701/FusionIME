package ee.oyatl.ime.keyboard.listener

import ee.oyatl.ime.keyboard.listener.KeyboardListener

class CompoundKeyboardListener(
    val listeners: List<KeyboardListener>
): KeyboardListener {
    constructor(vararg listeners: KeyboardListener): this(listeners.toList())

    override fun onKeyDown(keyCode: Int, metaState: Int) {
        listeners.forEach { it.onKeyDown(keyCode, metaState) }
    }

    override fun onKeyUp(keyCode: Int, metaState: Int) {
        listeners.forEach { it.onKeyUp(keyCode, metaState) }
    }

    override fun onReset() {
        listeners.forEach { it.onReset() }
    }
}