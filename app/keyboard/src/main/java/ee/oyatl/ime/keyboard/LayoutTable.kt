package ee.oyatl.ime.keyboard

import ee.oyatl.ime.keyboard.touchhandler.FlickDirection

data class LayoutTable(
    val map: Map<Int, Item> = emptyMap()
) {
    operator fun get(keyCode: Int): Item? {
        return map[keyCode]
    }

    operator fun plus(other: LayoutTable): LayoutTable {
        return LayoutTable(this.map + other.map)
    }

    fun mapKeyCodes(keyCodeMap: Map<Int, Int>): LayoutTable {
        // Convert from "QWERTY to LAYOUT" to "LAYOUT TO QWERTY" by swapping keys and values
        val revKeyCodeMap = keyCodeMap.map { (k, v) -> v to k }.toMap()
        return LayoutTable(map.mapKeys { (key, _) -> revKeyCodeMap[key] ?: key })
    }

    interface Item {
        val normal: Int
    }

    data class DefaultItem(
        override val normal: Int,
        val shifted: Int = normal,
        val locked: Int = shifted
    ): Item {
        fun forShiftState(shift: KeyboardState.Shift): Int {
            return when(shift) {
                KeyboardState.Shift.Released -> normal
                KeyboardState.Shift.Pressed -> shifted
                KeyboardState.Shift.Locked -> locked
            }
        }
    }

    data class FlickItem(
        override val normal: Int,
        val up: Int,
        val down: Int,
        val left: Int,
        val right: Int
    ): Item {
        fun forFlickDirection(direction: FlickDirection?): Int {
            return when(direction) {
                FlickDirection.Up -> up
                FlickDirection.Down -> down
                FlickDirection.Left -> left
                FlickDirection.Right -> right
                null -> normal
                else -> 0
            }
        }
    }

    companion object {
        fun fromShiftStates(map: Map<Int, List<Int>>): LayoutTable {
            return LayoutTable(map.mapNotNull { (key, arr) ->
                val value =
                    if(arr.size >= 3) DefaultItem(arr[0], arr[1], arr[2])
                    else if(arr.size == 2) DefaultItem(arr[0], arr[1])
                    else if(arr.size == 1) DefaultItem(arr[0])
                    else return@mapNotNull null
                key to value
            }.toMap())
        }
        fun fromFlick4Dirs(map: Map<Int, List<Int>>): LayoutTable {
            return LayoutTable(map.mapNotNull { (key, arr) ->
                when (arr.size) {
                    5 -> key to FlickItem(arr[0], arr[1], arr[2], arr[3], arr[4])
                    1 -> key to FlickItem(arr[0], arr[0], arr[0], arr[0], arr[0])
                    else -> null
                }
            }.toMap())
        }
    }
}