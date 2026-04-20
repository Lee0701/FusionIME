package ee.oyatl.ime.keyboard.popup

import android.view.View
import ee.oyatl.ime.keyboard.touchhandler.TouchHandler

class DefaultPopupManager(
    private val parent: View,
    private val keyboardView: TouchHandler.KeyboardViewInterface
): PopupManager {
    override fun getPopupPosition(key: TouchHandler.KeyInterface): Pair<Int, Int> {
        val y = keyboardView.rect.top + key.location[1] - keyboardView.location[1] - key.rect.height()
        return key.rect.left to y
    }

    override fun createPreviewPopup(key: TouchHandler.KeyInterface): Popup? {
        if(key.label.isNotEmpty()) {
            val popup = PreviewPopup(parent)
            popup.label = key.label
            popup.size = key.rect.width() to key.rect.height() * 2
            popup.position = getPopupPosition(key)
            return popup
        }
        return null
    }
}