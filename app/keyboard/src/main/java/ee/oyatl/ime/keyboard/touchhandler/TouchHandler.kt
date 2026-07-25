package ee.oyatl.ime.keyboard.touchhandler

import android.graphics.Rect
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.popup.PopupManager

interface TouchHandler {
    val keyboardView: KeyboardViewInterface

    fun onReset()
    fun onTouchDown(pointerId: Int, x: Int, y: Int)
    fun onTouchMove(pointerId: Int, x: Int, y: Int)
    fun onTouchUp(pointerId: Int, x: Int, y: Int)

    interface KeyInterface {
        val keyCode: Int
        val label: String
        val rect: Rect
        val location: IntArray
        fun onPressed()
        fun onReleased()
    }

    interface KeyboardViewInterface {
        val rect: Rect
        val location: IntArray
        val listener: KeyboardListener
        val popupManager: PopupManager
        val labels: Map<Int, String>
        val icons: Map<Int, Int>
        fun findKey(x: Int, y: Int): KeyInterface?
    }
}