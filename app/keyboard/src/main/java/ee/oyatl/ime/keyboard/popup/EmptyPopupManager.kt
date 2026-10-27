package ee.oyatl.ime.keyboard.popup

import ee.oyatl.ime.keyboard.KeyboardView

object EmptyPopupManager: PopupManager {
    override fun showPopup(
        key: KeyboardView.Key,
        initializer: () -> Popup
    ): Popup? = null

    override fun getPopup(key: KeyboardView.Key): Popup? = null

    override fun removePopup(key: KeyboardView.Key): Popup? = null

    override fun clearPopups() = Unit
}