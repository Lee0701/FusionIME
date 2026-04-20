package ee.oyatl.ime.keyboard.popup

import ee.oyatl.ime.keyboard.touchhandler.TouchHandler

interface PopupManager {
    fun getPopupPosition(key: TouchHandler.KeyInterface): Pair<Int, Int>
    fun createPreviewPopup(key: TouchHandler.KeyInterface): Popup?
}