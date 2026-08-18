package ee.oyatl.ime.keyboard.popup

import ee.oyatl.ime.keyboard.KeyboardView

object EmptyPopupManager: PopupManager {
    override fun getPopupPosition(key: KeyboardView.Key): Pair<Int, Int> = 0 to 0
    override fun createPreviewPopup(key: KeyboardView.Key): Popup? = null
}