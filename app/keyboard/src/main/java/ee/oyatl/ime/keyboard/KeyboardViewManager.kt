package ee.oyatl.ime.keyboard

import android.view.View

interface KeyboardViewManager {
    val view: View
    fun setLabels(labels: Map<Int, String>)
    fun setIcons(icons: Map<Int, Int>)
}