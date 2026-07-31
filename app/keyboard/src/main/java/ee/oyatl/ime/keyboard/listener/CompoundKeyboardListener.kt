package ee.oyatl.ime.keyboard.listener

class CompoundKeyboardListener(
    val listeners: List<KeyboardListener>
): KeyboardListener, SwipeListener {
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

    override fun onSwipeStart() {
        listeners.forEach { if(it is SwipeListener) it.onSwipeStart() }
    }

    override fun onSwipeEnd(pointers: List<SwipeListener.Pointer>) {
        listeners.forEach { if(it is SwipeListener) it.onSwipeEnd(pointers) }
    }

    override fun onSwipeMove(pointers: List<SwipeListener.Pointer>) {
        listeners.forEach { if(it is SwipeListener) it.onSwipeMove(pointers) }
    }

    operator fun plus(another: CompoundKeyboardListener): CompoundKeyboardListener {
        return CompoundKeyboardListener(this.listeners + another.listeners)
    }

    operator fun plus(another: KeyboardListener): CompoundKeyboardListener {
        return CompoundKeyboardListener(this.listeners + another)
    }
}