package ee.oyatl.ime.keyboard

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import ee.oyatl.ime.keyboard.core.KeyLabel
import ee.oyatl.ime.keyboard.core.KeyboardState
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.popup.PopupManager
import ee.oyatl.ime.theme.KeyboardTheme

class SwitcherKeyboardView(
    context: Context,
    attrs: AttributeSet?
): FrameLayout(context, attrs), KeyboardView {
    override val view: View = this

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
            map.values.filterIsInstance<View>().forEach {
                it.visibility = if(it === currentView) VISIBLE else GONE
            }
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

    override fun findKeys(keyCode: Int): List<KeyboardView.Key> {
        return currentView.findKeys(keyCode)
    }

    override val keyboardTheme: KeyboardTheme?
        get() = currentView.keyboardTheme
    override val keyboard: Keyboard?
        get() = currentView.keyboard
    override var labels: Map<Int, KeyLabel>
        get() = currentView.labels
        set(value) {
            currentView.labels = value
        }

    override fun onReset() {
        map.values.forEach { it.onReset() }
    }
}