package ee.oyatl.ime.keyboard.touchhandler

import ee.oyatl.ime.keyboard.KeyLabel
import ee.oyatl.ime.keyboard.KeyboardParams
import ee.oyatl.ime.keyboard.KeyboardView
import ee.oyatl.ime.keyboard.listener.FlickListener
import ee.oyatl.ime.keyboard.popup.PreviewPopup
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

class FlickTouchHandler(
    val keyboardView: KeyboardView,
    private val keyboardParams: KeyboardParams,
    val diagonal: Boolean = false,
    val multiFlick: Boolean = false,
    val sendOnUp: Boolean = false
): TouchHandler {
    val pointers = mutableMapOf<Int, Pointer>()

    override fun onReset() {
        keyboardView.popupManager.clearPopups()
        pointers.values.forEach {
            it.key?.onReleased()
        }
        pointers.clear()
    }

    override fun onTouchDown(pointerId: Int, x: Int, y: Int) {
        val key = keyboardView.findKey(x, y)
        val pointer = Pointer(pointerId, x, y, x, y, key)
        if(key != null) {
            keyboardView.findKeys(key.keyCode).forEach { it.onPressed() }
            keyboardView.listener.onKeyDown(key.keyCode, 0)
            if(keyboardParams.previewPopups && key.label.isNotEmpty()) {
                keyboardView.popupManager.showPopup(key) { PreviewPopup(keyboardView, key) }
            }
        }
        pointers += pointerId to pointer
    }

    override fun onTouchMove(pointerId: Int, x: Int, y: Int) {
        val pointer = pointers[pointerId] ?: return
        val diffX = (x - pointer.downX).toFloat()
        val diffY = (y - pointer.downY).toFloat()
        val dist = sqrt(diffX.pow(2) + diffY.pow(2))
        if(dist > keyboardParams.flickSensitivity) {
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
                        val popup = keyboardView.popupManager.getPopup(pointer.key)
                        if(popup is PreviewPopup) {
                            val newLabel = keyboardView.labels[pointer.key.keyCode]
                            if(newLabel is KeyLabel.Flick) newLabel.forDirection(direction)?.let { popup.label = it }
                        }
                        val listener = keyboardView.listener
                        if(listener is FlickListener) {
                            listener.onFlick(pointer.key.keyCode, direction)
                        }
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
            keyboardView.findKeys(key.keyCode).forEach { it.onReleased() }
            if(pointer.flicks.isEmpty() || sendOnUp) keyboardView.listener.onKeyUp(key.keyCode, 0)
            keyboardView.popupManager.removePopup(key)
        }
        pointers -= pointerId
    }

    override fun onTouchCancel(pointerId: Int) {
        val pointer = pointers.remove(pointerId) ?: return
        pointer.key?.let { key ->
            keyboardView.findKeys(key.keyCode).forEach { it.onReleased() }
            if(keyboardView.popupManager.getPopup(key) is PreviewPopup)
                keyboardView.popupManager.removePopup(key)
        }
    }

    data class Pointer(
        val id: Int,
        val downX: Int,
        val downY: Int,
        val x: Int,
        val y: Int,
        val key: KeyboardView.Key?,
        val flicks: List<FlickDirection> = listOf()
    )
}
