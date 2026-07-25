package ee.oyatl.ime.keyboard

import android.content.Context
import android.util.AttributeSet

class SwitcherKeyboardView(
    context: Context,
    attrs: AttributeSet?
): KeyboardView(context, attrs) {
    var map: Map<KeyboardState, KeyboardView> = mapOf()
        set(value) {
            field = value
            this.removeAllViews()
            value.values.forEach { this.addView(it) }
            this.state = value.keys.firstOrNull()
        }

    var state: KeyboardState? = null
        set(value) {
            field = value
            currentView.bringToFront()
        }

    private val currentView: KeyboardView get() = map[state] ?: map.values.first()

    override var labels: Map<Int, String>
        get() = currentView.labels
        set(value) {
            currentView.labels = value
        }

    override var icons: Map<Int, Int>
        get() = currentView.icons
        set(value) {
            currentView.icons = value
        }

    override fun onReset() {
        map.values.forEach { it.onReset() }
    }
}