package ee.oyatl.ime.keyboard.popup

interface SelectionPopup: Popup {
    val selectedCodePoint: Int?
    fun selectAt(rawX: Int, rawY: Int)
}
