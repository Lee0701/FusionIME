package ee.oyatl.ime.keyboard

import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.View

class PaleViewFilter(
    originalColorWeight: Float = 0.30f
) {
    private val paint = Paint().apply {
        val whiteOffset = 255f * (1f - originalColorWeight)
        colorFilter = ColorMatrixColorFilter(floatArrayOf(
            originalColorWeight, 0f, 0f, 0f, whiteOffset,
            0f, originalColorWeight, 0f, 0f, whiteOffset,
            0f, 0f, originalColorWeight, 0f, whiteOffset,
            0f, 0f, 0f, 1f, 0f
        ))
    }
    private var target: View? = null
    private var originalLayerType: Int? = null

    fun apply(view: View) {
        if(target !== view) {
            clear()
            target = view
            originalLayerType = view.layerType
            view.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
        } else {
            view.setLayerPaint(paint)
        }
    }

    fun clear() {
        val view = target
        val layerType = originalLayerType
        if(view != null && layerType != null) {
            view.setLayerType(layerType, null)
        }
        target = null
        originalLayerType = null
    }
}
