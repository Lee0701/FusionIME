package ee.oyatl.ime.keyboard

import android.graphics.Rect
import android.view.View
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.popup.PopupManager

interface KeyboardView {
    val view: View
    val rect: Rect
    val location: IntArray
    val listener: KeyboardListener
    val popupManager: PopupManager
    fun findKey(x: Int, y: Int): Key?
    fun onReset()
    fun setLabels(labels: Map<Int, String>)
    fun setIcons(icons: Map<Int, Int>)

    interface Key {
        val keyCode: Int
        val label: String
        val rect: Rect
        val location: IntArray
        fun onPressed()
        fun onReleased()
    }
}