package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.View
import android.widget.FrameLayout

class SwitcherKeyboardViewManager(
    context: Context,
    val map: Map<KeyboardState, KeyboardViewManager>
): KeyboardViewManager {
    var state: KeyboardState = map.keys.first()
        set(value) {
            field = value
            currentView.view.bringToFront()
        }
    private val currentView: KeyboardViewManager get() = map[state] ?: map.values.first()

    override val view: View = FrameLayout(context)

    init {
        map.values.forEach { (view as FrameLayout).addView(it.view) }
    }

    override fun setLabels(labels: Map<Int, String>) {
        currentView.setLabels(labels)
    }

    override fun setIcons(icons: Map<Int, Int>) {
        currentView.setIcons(icons)
    }
}