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