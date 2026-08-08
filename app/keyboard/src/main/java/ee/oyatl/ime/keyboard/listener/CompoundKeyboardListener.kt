package ee.oyatl.ime.keyboard.listener

import ee.oyatl.ime.keyboard.touchhandler.FlickDirection

class CompoundKeyboardListener(
    val listeners: List<KeyboardListener>
): KeyboardListener, FlickListener, SwipeListener {
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