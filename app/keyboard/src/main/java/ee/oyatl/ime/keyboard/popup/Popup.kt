package ee.oyatl.ime.keyboard.popup

import android.content.Context
import android.view.View

interface Popup {
    val view: View
    fun show(parent: View, x: Int, y: Int)
    fun hide()
}