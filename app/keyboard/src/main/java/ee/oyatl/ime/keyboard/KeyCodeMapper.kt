package ee.oyatl.ime.keyboard

import android.view.KeyCharacterMap

class KeyCodeMapper(
    val map: Map<Int, Int>
) {
    operator fun get(keyCode: Int): Int {
        return map[keyCode] ?: keyCode
    }

    companion object {
        private val keyCharacterMap: KeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
        fun keyCharToKeyCode(keyChar: Char): Int {
            return keyCharacterMap.getEvents(charArrayOf(keyChar)).firstOrNull()?.keyCode ?: 0
        }

        fun from(base: List<String>, mappings: List<String>): KeyCodeMapper {
            return KeyCodeMapper(base.zip(mappings).flatMap { (baseRow, mapRow) ->
                baseRow.zip(mapRow).map { (base, target) ->
                    val baseKeyCode = keyCharToKeyCode(base)
                    val targetKeyCode = keyCharToKeyCode(target)
                    baseKeyCode to targetKeyCode
                }
            }.toMap())
        }
    }
}