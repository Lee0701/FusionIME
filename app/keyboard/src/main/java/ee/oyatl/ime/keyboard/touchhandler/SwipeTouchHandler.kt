package ee.oyatl.ime.keyboard.touchhandler

import android.graphics.Rect
import android.view.KeyCharacterMap
import android.view.KeyEvent
import ee.oyatl.ime.keyboard.listener.SwipeListener

class SwipeTouchHandler(
    override val keyboardView: TouchHandler.KeyboardViewInterface
): TouchHandler {
    val rect: Rect = Rect()
    val keyCharacterMap: KeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
    val pointers = mutableListOf<SwipeListener.Pointer>()
    val listener = keyboardView.listener

    override fun onReset() {
        pointers.clear()
    }

    override fun onTouchDown(pointerId: Int, x: Int, y: Int) {
        setKeyboardDimens()
        pointers.clear()
        addPointer(x, y)
        if(listener is SwipeListener) listener.onSwipeStart()
    }

    override fun onTouchMove(pointerId: Int, x: Int, y: Int) {
        addPointer(x, y)
        if(listener is SwipeListener) listener.onSwipeMove(pointers.toList())
    }

    override fun onTouchUp(pointerId: Int, x: Int, y: Int) {
        addPointer(x, y)
        if(listener is SwipeListener) listener.onSwipeEnd(pointers.toList())
    }

    private fun setKeyboardDimens() {
        val keyCodes = KeyEvent.KEYCODE_A ..KeyEvent.KEYCODE_Z
        val keySet = keyCodes.mapNotNull { keyboardView.findKey(it) }
        val left = keySet.minOf { it.rect.left }
        val right = keySet.maxOf { it.rect.right }
        val top = keySet.minOf { it.rect.top }
        val bottom = keySet.maxOf { it.rect.bottom }
        rect.set(left, top, right, bottom)
    }

    private fun addPointer(x: Int, y: Int) {
        pointers += SwipeListener.Pointer(
            x = (x - rect.left) / rect.width().toFloat(),
            y = (y - rect.top) / rect.height().toFloat(),
            time = System.currentTimeMillis(),
            touchedKey = keyboardView.findKey(x, y)?.keyCode?.let { keyCharacterMap.get(it, 0).toChar() } ?: 'a'
        )
    }
}