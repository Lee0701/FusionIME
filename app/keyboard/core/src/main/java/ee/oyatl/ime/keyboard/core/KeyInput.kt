package ee.oyatl.ime.keyboard.core

data class KeyInput(
    val keyCode: Int,
    val shiftState: KeyboardState.Shift,
    val flickDirection: FlickDirection? = null
)