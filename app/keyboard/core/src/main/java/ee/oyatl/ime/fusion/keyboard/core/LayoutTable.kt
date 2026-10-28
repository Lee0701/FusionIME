package ee.oyatl.ime.fusion.keyboard.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LayoutTable(
    val map: Map<Int, Item> = emptyMap()
) {
    operator fun get(input: KeyInput): Int? {
        val item = get(map[input.keyCode] ?: return null, input)
        if(item is Item.Output) return item.char
        return null
    }

    fun get(item: Item?, input: KeyInput): Item? {
        return when(item) {
            is Item.Output -> item
            is Item.ShiftState -> get(when(input.shiftState) {
                KeyboardState.Shift.Released -> item.normal
                KeyboardState.Shift.Pressed -> item.shifted ?: item.normal
                KeyboardState.Shift.Locked -> item.locked ?: item.shifted ?: item.normal
                KeyboardState.Shift.AutoShifted -> item.locked ?: item.shifted ?: item.normal
            }, input)
            is Item.Flick -> get(when(input.flickDirection) {
                FlickDirection.Up -> item.up ?: item.normal
                FlickDirection.Down -> item.down ?: item.normal
                FlickDirection.Left -> item.left ?: item.normal
                FlickDirection.Right -> item.right ?: item.normal
                else -> item.normal
            }, input)
            else -> null
        }
    }

    operator fun plus(other: LayoutTable): LayoutTable {
        return LayoutTable(this.map + other.map)
    }

    fun mapKeyCodes(keyCodeMap: Map<Int, Int>): LayoutTable {
        // Convert from "QWERTY to LAYOUT" to "LAYOUT TO QWERTY" by swapping keys and values
        val revKeyCodeMap = keyCodeMap.map { (k, v) -> v to k }.toMap()
        return LayoutTable(map.mapKeys { (key, _) -> revKeyCodeMap[key] ?: key })
    }

    @Serializable
    sealed interface Item {
        @SerialName("output")
        @Serializable
        data class Output(
            val char: Int
        ): Item
        @SerialName("shift")
        @Serializable
        data class ShiftState(
            val normal: Item? = null,
            val shifted: Item? = null,
            val locked: Item? = null
        ): Item
        @SerialName("flick")
        @Serializable
        data class Flick(
            val normal: Item? = null,
            val up: Item? = null,
            val down: Item? = null,
            val left: Item? = null,
            val right: Item? = null
        ): Item
    }

    companion object {
        fun fromShiftStates(map: Map<Int, List<Int>>): LayoutTable {
            return LayoutTable(map.mapNotNull { (key, arr) ->
                val items = arr.map { Item.Output(it) }
                val value =
                    if(items.size >= 3) Item.ShiftState(items[0], items[1], items[2])
                    else if(items.size == 2) Item.ShiftState(items[0], items[1])
                    else if(items.size == 1) Item.ShiftState(items[0])
                    else return@mapNotNull null
                key to value
            }.toMap())
        }
        fun fromFlick4Dirs(map: Map<Int, List<Int>>): LayoutTable {
            return LayoutTable(map.mapNotNull { (key, arr) ->
                val items = arr.map { Item.Output(it) }
                when (items.size) {
                    5 -> key to Item.Flick(items[0], items[1], items[2], items[3], items[4])
                    1 -> key to Item.Flick(items[0], items[0], items[0], items[0], items[0])
                    else -> null
                }
            }.toMap())
        }
    }
}