package ee.oyatl.ime.keyboard.popup

import ee.oyatl.ime.keyboard.KeyboardView

interface PopupManager {
    fun showPopup(key: KeyboardView.Key, initializer: () -> Popup): Popup?
    fun getPopup(key: KeyboardView.Key): Popup?
    fun removePopup(key: KeyboardView.Key): Popup?
    fun clearPopups()
}
