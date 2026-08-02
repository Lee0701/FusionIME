package ee.oyatl.ime.keyboard.listener

class CompoundKeyboardListener(
    val listeners: List<KeyboardListener>
): KeyboardListener {
    constructor(vararg listeners: KeyboardListener): this(listeners.toList())

    override fun onKeyDown(keyCode: Int, metaState: Int): Boolean {
        return listeners.any { it.onKeyDown(keyCode, metaState) }
    }

    override fun onKeyUp(keyCode: Int, metaState: Int): Boolean {
        return listeners.any { it.onKeyUp(keyCode, metaState) }
    }

    override fun onReset() {
        listeners.forEach { it.onReset() }
    }
}