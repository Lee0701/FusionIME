package ee.oyatl.ime.keyboard.popup

import ee.oyatl.ime.keyboard.KeyboardView

class DefaultPopupManager: PopupManager {
    private val popups = mutableMapOf<Int, Popup>()

    override fun showPopup(
        key: KeyboardView.Key,
        initializer: () -> Popup
    ): Popup {
        val popup = popups.getOrPut(key.keyCode, initializer)
        popup.show()
        return popup
    }

    override fun getPopup(key: KeyboardView.Key): Popup? {
        return popups[key.keyCode]
    }

    override fun removePopup(key: KeyboardView.Key): Popup? {
        val popup = popups.remove(key.keyCode)
        popup?.hide()
        return popup
    }

    override fun clearPopups() {
        popups.values.forEach { it.hide() }
        popups.clear()
    }
}
