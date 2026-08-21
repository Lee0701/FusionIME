package ee.oyatl.ime.keyboard.touchhandler

import ee.oyatl.ime.keyboard.DefaultKeyboardView
import ee.oyatl.ime.keyboard.KeyboardView

class CompoundTouchHandler(
    override val keyboardView: KeyboardView,
    val touchHandlers: List<TouchHandler>
): TouchHandler {
    constructor(keyboardView: DefaultKeyboardView, vararg touchHandlers: TouchHandler): this(keyboardView, touchHandlers.toList())

    override fun onReset() {
        touchHandlers.forEach { it.onReset() }
    }

    override fun onTouchDown(pointerId: Int, x: Int, y: Int) {
        touchHandlers.forEach { it.onTouchDown(pointerId, x, y) }
    }

    override fun onTouchMove(pointerId: Int, x: Int, y: Int) {
        touchHandlers.forEach { it.onTouchMove(pointerId, x, y) }
    }

    override fun onTouchUp(pointerId: Int, x: Int, y: Int) {
        touchHandlers.forEach { it.onTouchUp(pointerId, x, y) }
    }
}