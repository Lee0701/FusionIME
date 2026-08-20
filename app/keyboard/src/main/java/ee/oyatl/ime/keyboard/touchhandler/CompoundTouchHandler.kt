package ee.oyatl.ime.keyboard.touchhandler

class CompoundTouchHandler(
    val touchHandlers: List<TouchHandler>
): TouchHandler {
    constructor(vararg touchHandlers: TouchHandler): this(touchHandlers.toList())

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

    override fun onTouchCancel(pointerId: Int) {
        touchHandlers.forEach { it.onTouchCancel(pointerId) }
    }
}
