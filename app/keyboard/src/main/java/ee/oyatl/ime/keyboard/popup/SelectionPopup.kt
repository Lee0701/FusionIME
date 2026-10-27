package ee.oyatl.ime.keyboard.popup

interface SelectionPopup: Popup {
    val listener: Listener
    fun selectAt(rawX: Int, rawY: Int)

    fun interface Listener {
        fun onSelect(codePoint: Int)
    }
}
