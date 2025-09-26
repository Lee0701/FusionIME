package ee.oyatl.ime.keyboard

data class LayoutTable(
    val map: Map<Int, Item>
) {
    operator fun get(keyCode: Int): Item? {
        return map[keyCode]
    }

    data class Item(
        val normal: Int,
        val shifted: Int = normal,
        val locked: Int = shifted
    ) {
        fun forShiftState(shift: KeyboardState.Shift): Int {
            return when(shift) {
                KeyboardState.Shift.Released -> normal
                KeyboardState.Shift.Pressed -> shifted
                KeyboardState.Shift.Locked -> locked
            }
        }
    }

    companion object {
        fun from(map: Map<Int, List<Int>>): LayoutTable {
            return LayoutTable(map.mapNotNull { (key, arr) ->
                val value =
                    if(arr.size >= 3) Item(arr[0], arr[1], arr[2])
                    else if(arr.size == 2) Item(arr[0], arr[1])
                    else if(arr.size == 1) Item(arr[0])
                    else return@mapNotNull null
                key to value
            }.toMap())
        }
    }
}