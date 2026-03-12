package ee.oyatl.ime.keyboard

import android.view.KeyCharacterMap

class SoftKeyCodeMapper(
    val map: Map<Int, Int> = emptyMap()
) {
    operator fun get(keyCode: Int): Int {
        return map[keyCode] ?: keyCode
    }

    companion object {
        private val keyCharacterMap: KeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

        fun keyCharToKeyCode(keyChar: Char): Int {
            return keyCharacterMap.getEvents(charArrayOf(keyChar)).firstOrNull()?.keyCode ?: 0
        }
    }
}