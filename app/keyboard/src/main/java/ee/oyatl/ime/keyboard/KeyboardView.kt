package ee.oyatl.ime.keyboard

import android.graphics.Rect
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.popup.PopupManager
import ee.oyatl.ime.keyboard.touchhandler.TouchHandler.KeyInterface

interface KeyboardView {
    val rect: Rect
    val location: IntArray
    val listener: KeyboardListener
    val popupManager: PopupManager

    var labels: Map<Int, String>
    var icons: Map<Int, Int>

    fun onReset()
    fun findKey(x: Int, y: Int): KeyInterface?
}