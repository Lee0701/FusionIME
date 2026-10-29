package ee.oyatl.ime.keyboard.touchhandler

object EmptyTouchHandler: TouchHandler {
    override fun onReset() = Unit

    override fun onTouchDown(pointerId: Int, x: Int, y: Int) = Unit

    override fun onTouchMove(pointerId: Int, x: Int, y: Int) = Unit

    override fun onTouchUp(pointerId: Int, x: Int, y: Int) = Unit

    override fun onTouchCancel(pointerId: Int) = Unit
}