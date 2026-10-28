package ee.oyatl.ime.keyboard

import ee.oyatl.ime.keyboard.touchhandler.FlickDirection

data class KeyInput(
    val keyCode: Int,
    val shiftState: KeyboardState.Shift,
    val flickDirection: FlickDirection? = null
)