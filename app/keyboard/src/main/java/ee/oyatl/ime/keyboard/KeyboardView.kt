package ee.oyatl.ime.keyboard

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

abstract class KeyboardView(
    context: Context,
    attrs: AttributeSet?
): FrameLayout(context, attrs) {
    abstract var labels: Map<Int, String>
    abstract var icons: Map<Int, Int>
    abstract fun onReset()
}