package ee.oyatl.ime.keyboard.touchhandler

import android.view.KeyEvent
import ee.oyatl.ime.keyboard.KeyboardView
import ee.oyatl.ime.keyboard.popup.Popup
import ee.oyatl.ime.keyboard.popup.PreviewPopup

class SeekTouchHandler(
    override val keyboardView: KeyboardView
): TouchHandler {
    val pointers = mutableMapOf<Int, Pointer>()

    override fun onReset() {
        pointers.values.forEach { it.key?.onReleased() }
        pointers.clear()
    }

    override fun onTouchDown(pointerId: Int, x: Int, y: Int) {
        val key = keyboardView.findKey(x, y)
        val popup = key?.let { keyboardView.popupManager.createPreviewPopup(key) }
        val pointer = Pointer(pointerId, x, y, key, popup)
        if(key != null) {
            key.onPressed()
            keyboardView.listener.onKeyDown(key.keyCode, 0)
        }
        popup?.show()
        pointers += pointerId to pointer
    }

    override fun onTouchMove(pointerId: Int, x: Int, y: Int) {
        val pointer = pointers[pointerId] ?: return
        val oldKey = pointer.key
        val newKey = keyboardView.findKey(x, y)
        val popup = pointer.popup
        if(newKey != oldKey) {
            oldKey?.onReleased()
            newKey?.onPressed()
            if(oldKey?.keyCode == KeyEvent.KEYCODE_DEL) keyboardView.listener.onKeyUp(oldKey.keyCode, 0)
            if(popup is PreviewPopup) {
                if(newKey?.label?.isNotEmpty() == true) {
                    if(!popup.isShown) popup.show()
                    popup.label = newKey.label
                    popup.position = keyboardView.popupManager.getPopupPosition(newKey)
                    popup.update()
                } else if(popup.isShown) {
                    popup.hide()
                }
            }
        }
        val newPointer = pointer.copy(x = x, y = y, key = newKey)
        pointers += pointerId to newPointer
    }

    override fun onTouchUp(pointerId: Int, x: Int, y: Int) {
        val pointer = pointers[pointerId] ?: return
        val key = pointer.key
        if(key != null) {
            key.onReleased()
            keyboardView.listener.onKeyUp(key.keyCode, 0)
        }
        pointer.popup?.hide()
        pointers -= pointerId
    }

    data class Pointer(
        val id: Int,
        val x: Int,
        val y: Int,
        val key: KeyboardView.Key?,
        val popup: Popup?
    )
}