package ee.oyatl.ime.keyboard.popup

import ee.oyatl.ime.keyboard.KeyboardView

interface PopupManager {
    fun getPopupPosition(key: KeyboardView.Key): Pair<Int, Int>
    fun createPreviewPopup(key: KeyboardView.Key): Popup?
}