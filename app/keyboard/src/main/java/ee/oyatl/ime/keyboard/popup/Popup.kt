package ee.oyatl.ime.keyboard.popup

import android.view.View

interface Popup {
    val view: View
    val isShown: Boolean
    fun show()
    fun hide()
    fun update()
}