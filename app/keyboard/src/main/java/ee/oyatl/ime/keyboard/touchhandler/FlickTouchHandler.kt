package ee.oyatl.ime.keyboard.touchhandler

import ee.oyatl.ime.keyboard.FlickKeyCode
import ee.oyatl.ime.keyboard.popup.Popup
import ee.oyatl.ime.keyboard.popup.PreviewPopup
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

class FlickTouchHandler(
    override val keyboardView: TouchHandler.KeyboardViewInterface,
    val threshold: Int,
    val diagonal: Boolean = false,
    val multiFlick: Boolean = false,
    val sendOnUp: Boolean = false
): TouchHandler {
    val pointers = mutableMapOf<Int, Pointer>()

    override fun onReset() {
        pointers.values.forEach { it.key?.onReleased() }
        pointers.clear()
    }

    override fun onTouchDown(pointerId: Int, x: Int, y: Int) {
        val key = keyboardView.findKey(x, y)
        val popup = key?.let { keyboardView.popupManager.createPreviewPopup(key) }
        val pointer = Pointer(pointerId, x, y, x, y, key, popup)
        if(key != null) {
            key.onPressed()
            keyboardView.listener.onKeyDown(key.keyCode, 0)
        }
        popup?.show()
        pointers += pointerId to pointer
    }

    override fun onTouchMove(pointerId: Int, x: Int, y: Int) {
        val pointer = pointers[pointerId] ?: return
        val diffX = (x - pointer.downX).toFloat()
        val diffY = (y - pointer.downY).toFloat()
        val dist = sqrt(diffX.pow(2) + diffY.pow(2))
        if(dist > threshold) {
            val angle = atan2(diffY, diffX) + PI
            val directions = FlickDirection.entries.filter { !it.diagonal or this.diagonal }
            val range = if(this.diagonal) 0.25 else 0.5
            val direction = directions.firstOrNull {
                it.contains(angle, range * PI)
            }
            val lastDirection = pointer.flicks.lastOrNull()
            if(direction != null) {
                val flicks = pointer.flicks.toMutableList()
                if(direction != lastDirection && (multiFlick || flicks.isEmpty())) {
                    if(pointer.key != null && pointer.key.keyCode >= 0) {
                        val keyCode = FlickKeyCode.FLAG_FLICK or direction.keyCodeFlag or pointer.key.keyCode
                        if(pointer.popup is PreviewPopup) {
                            val newLabel = keyboardView.labels[direction.keyCodeFlag or pointer.key.keyCode]
                            if(newLabel != null) pointer.popup.label = newLabel
                        }
                        keyboardView.listener.onKeyDown(keyCode, 0)
                        keyboardView.listener.onKeyUp(keyCode, 0)
                    }
                    flicks += direction
                }
                pointers += pointerId to pointer.copy(downX = x, downY = y, flicks = flicks.toList())
            }
        } else {
            pointers += pointerId to pointer.copy(x = x, y = y)
        }
    }

    override fun onTouchUp(pointerId: Int, x: Int, y: Int) {
        val pointer = pointers[pointerId] ?: return
        val key = pointer.key
        if(key != null) {
            key.onReleased()
            if(pointer.flicks.isEmpty() || sendOnUp) keyboardView.listener.onKeyUp(key.keyCode, 0)
        }
        pointer.popup?.hide()
        pointers -= pointerId
    }

    data class Pointer(
        val id: Int,
        val downX: Int,
        val downY: Int,
        val x: Int,
        val y: Int,
        val key: TouchHandler.KeyInterface?,
        val popup: Popup?,
        val flicks: List<FlickDirection> = listOf()
    )
}