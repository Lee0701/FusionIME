package ee.oyatl.ime.keyboard.popup

import ee.oyatl.ime.keyboard.touchhandler.TouchHandler

object EmptyPopupManager: PopupManager {
    override fun getPopupPosition(key: TouchHandler.KeyInterface): Pair<Int, Int> = 0 to 0
    override fun createPreviewPopup(key: TouchHandler.KeyInterface): Popup? = null
}