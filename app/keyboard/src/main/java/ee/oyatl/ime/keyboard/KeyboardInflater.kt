package ee.oyatl.ime.keyboard

import android.view.KeyCharacterMap

object KeyboardInflater {
    private val keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

    fun inflate(
        template: List<String>,
        table: Map<Int, List<Int>> = mapOf()
    ): List<List<List<Int>>> {
        val layerCount = table.values.maxOfOrNull { it.size } ?: 1
        return (0 until layerCount).map { layer ->
            template.map { row ->
                row.map { ch ->
                    val keyCode = charToKeyCode(ch)
                    table[keyCode]?.get(layer) ?: ch.code
                }
            }
        }
    }

    private fun charToKeyCode(char: Char): Int {
        return keyCharacterMap.getEvents(charArrayOf(char))?.firstOrNull()?.keyCode ?: char.code
    }
}