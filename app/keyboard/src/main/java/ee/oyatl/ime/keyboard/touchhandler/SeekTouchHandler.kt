package ee.oyatl.ime.keyboard.touchhandler

import android.view.KeyEvent
import ee.oyatl.ime.keyboard.KeyboardParams
import ee.oyatl.ime.keyboard.KeyboardView
import ee.oyatl.ime.keyboard.popup.PreviewPopup

class SeekTouchHandler(
    val keyboardView: KeyboardView,
    private val keyboardParams: KeyboardParams
): TouchHandler {
    val pointers = mutableMapOf<Int, Pointer>()

    override fun onReset() {
        pointers.values.forEach { it.key?.onReleased() }
        pointers.clear()
    }

    override fun onTouchDown(pointerId: Int, x: Int, y: Int) {
        val key = keyboardView.findKey(x, y)
        val pointer = Pointer(pointerId, x, y, key)
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
        val oldKey = pointer.key
        val newKey = keyboardView.findKey(x, y)
        if(newKey != oldKey) {
            oldKey?.let { key -> keyboardView.findKeys(key.keyCode).forEach { it.onReleased() } }
            newKey?.let { key -> keyboardView.findKeys(key.keyCode).forEach { it.onPressed() } }
            if(oldKey?.keyCode == KeyEvent.KEYCODE_DEL) keyboardView.listener.onKeyUp(oldKey.keyCode, 0)
            if(oldKey != null) keyboardView.popupManager.removePopup(oldKey)
            if(newKey != null) keyboardView.popupManager.showPopup(newKey) { PreviewPopup(keyboardView, newKey) }
        }
        val newPointer = pointer.copy(x = x, y = y, key = newKey)
        pointers += pointerId to newPointer
    }

    override fun onTouchUp(pointerId: Int, x: Int, y: Int) {
        val pointer = pointers[pointerId] ?: return
        val key = pointer.key
        if(key != null) {
            keyboardView.findKeys(key.keyCode).forEach { it.onReleased() }
            keyboardView.listener.onKeyUp(key.keyCode, 0)
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
        val x: Int,
        val y: Int,
        val key: KeyboardView.Key?
    )
}
