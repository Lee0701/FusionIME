package ee.oyatl.ime.keyboard

import android.graphics.Rect
import android.view.View
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.popup.PopupManager
import ee.oyatl.ime.theme.KeyboardTheme

interface KeyboardView {
    val view: View

    val rect: Rect
    val location: IntArray
    val listener: KeyboardListener
    val popupManager: PopupManager

    val keyboardTheme: KeyboardTheme?
    val keyboard: Keyboard?
    var labels: Map<Int, KeyLabel>

    fun onReset()
    fun findKey(x: Int, y: Int): Key?
    fun findKeys(keyCode: Int): List<Key>

    interface Key {
        val keyCode: Int
        val label: String
        val rect: Rect
        val location: IntArray
        fun onPressed()
        fun onReleased()
    }
}