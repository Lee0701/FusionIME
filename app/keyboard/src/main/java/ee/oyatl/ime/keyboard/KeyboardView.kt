package ee.oyatl.ime.keyboard

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

abstract class KeyboardView(
    context: Context,
    attrs: AttributeSet?
): FrameLayout(context, attrs) {
    abstract fun onReset()
    abstract fun setLabels(labels: Map<Int, String>)
    abstract fun setIcons(icons: Map<Int, Int>)
}