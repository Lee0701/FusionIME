package ee.oyatl.ime.keyboard

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.popup.PopupManager
import ee.oyatl.ime.keyboard.touchhandler.TouchHandler

class SwitcherKeyboardView(
    context: Context,
    attrs: AttributeSet?
): FrameLayout(context, attrs), KeyboardView {
    override val view: View get() = this

    var map: Map<KeyboardState, KeyboardView> = mapOf()
        set(value) {
            field = value
            this.removeAllViews()
            value.values.forEach { this.addView(it.view) }
            this.state = value.keys.firstOrNull()
        }

    var state: KeyboardState? = null
        set(value) {
            field = value
            currentView.view.bringToFront()
        }

    private val currentView: KeyboardView get() = map[state] ?: map.values.first()
    override val rect: Rect
        get() = currentView.rect
    override val location: IntArray
        get() = currentView.location
    override val listener: KeyboardListener
        get() = currentView.listener
    override val popupManager: PopupManager
        get() = currentView.popupManager

    override fun findKey(
        x: Int,
        y: Int
    ): KeyboardView.Key? = null

    override fun onReset() {
        map.values.forEach { it.onReset() }
    }

    override fun setLabels(labels: Map<Int, String>) {
        currentView.setLabels(labels)
    }

    override fun setIcons(icons: Map<Int, Int>) {
        currentView.setIcons(icons)
    }
}