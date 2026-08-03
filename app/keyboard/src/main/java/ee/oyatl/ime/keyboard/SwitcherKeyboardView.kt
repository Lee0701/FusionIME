package ee.oyatl.ime.keyboard

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import ee.oyatl.ime.keyboard.listener.EmptyListener
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.popup.EmptyPopupManager
import ee.oyatl.ime.keyboard.popup.PopupManager
import ee.oyatl.ime.keyboard.touchhandler.TouchHandler

class SwitcherKeyboardView(
    context: Context,
    attrs: AttributeSet?
): FrameLayout(context, attrs), KeyboardView {
    var map: Map<KeyboardState, KeyboardView> = mapOf()
        set(value) {
            field = value
            this.removeAllViews()
            value.values.filterIsInstance<View>().forEach { this.addView(it) }
            this.state = value.keys.firstOrNull()
        }

    var state: KeyboardState? = null
        set(value) {
            field = value
            val currentView = currentView
            if(currentView is View) currentView.bringToFront()
        }

    private val currentView: KeyboardView get() = map[state] ?: map.values.first()

    override var labels: Map<Int, KeyLabel>
        get() = currentView.labels
        set(value) {
            currentView.labels = value
        }

    override fun onReset() {
        map.values.forEach { it.onReset() }
    }

    override val rect: Rect = Rect()
    override val location: IntArray = intArrayOf()
    override val listener: KeyboardListener = EmptyListener
    override val popupManager: PopupManager = EmptyPopupManager

    override fun findKey(
        x: Int,
        y: Int
    ): TouchHandler.KeyInterface? = null
}