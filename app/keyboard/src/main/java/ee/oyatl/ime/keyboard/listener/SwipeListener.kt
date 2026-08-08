package ee.oyatl.ime.keyboard.listener

interface SwipeListener: KeyboardListener {
    fun onSwipeStart()
    fun onSwipeEnd(pointers: List<Pointer>)
    fun onSwipeMove(pointers: List<Pointer>)

    abstract class Delegate: SwipeListener {
        override fun onSwipeStart() = Unit
        override fun onSwipeEnd(pointers: List<Pointer>) = Unit
        override fun onSwipeMove(pointers: List<Pointer>) = Unit
        override fun onKeyDown(keyCode: Int, metaState: Int): Boolean = false
        override fun onKeyUp(keyCode: Int, metaState: Int): Boolean = false
        override fun onReset() = Unit
    }

    data class Pointer(
        val x: Float,
        val y: Float,
        val time: Long,
        val touchedKey: Char
    )
}