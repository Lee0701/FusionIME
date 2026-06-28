package ee.oyatl.ime.keyboard.touchhandler

import ee.oyatl.ime.keyboard.KeyboardView

interface TouchHandler {
    val keyboardView: KeyboardView

    fun onReset()
    fun onTouchDown(pointerId: Int, x: Int, y: Int)
    fun onTouchMove(pointerId: Int, x: Int, y: Int)
    fun onTouchUp(pointerId: Int, x: Int, y: Int)
}