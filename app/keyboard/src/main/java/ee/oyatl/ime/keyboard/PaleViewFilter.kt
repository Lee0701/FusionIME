package ee.oyatl.ime.keyboard

import android.content.res.Configuration
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.View

class PaleViewFilter(
    originalColorWeight: Float = 0.30f
) {
    private val lightPaint = createPaint(originalColorWeight, 255f)
    private val darkPaint = createPaint(originalColorWeight, 0f)
    private var target: View? = null
    private var originalLayerType: Int? = null

    fun apply(view: View) {
        val nightMode = view.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val paint = if(nightMode == Configuration.UI_MODE_NIGHT_YES) darkPaint else lightPaint
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

    private fun createPaint(originalColorWeight: Float, blendColor: Float): Paint {
        val colorOffset = blendColor * (1f - originalColorWeight)
        return Paint().apply {
            colorFilter = ColorMatrixColorFilter(floatArrayOf(
                originalColorWeight, 0f, 0f, 0f, colorOffset,
                0f, originalColorWeight, 0f, 0f, colorOffset,
                0f, 0f, originalColorWeight, 0f, colorOffset,
                0f, 0f, 0f, 1f, 0f
            ))
        }
    }
}
