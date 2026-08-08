package ee.oyatl.ime.keyboard.touchhandler

import android.graphics.Rect
import android.view.KeyCharacterMap
import android.view.KeyEvent
import ee.oyatl.ime.keyboard.KeyboardView
import ee.oyatl.ime.keyboard.listener.SwipeListener
import kotlin.math.pow
import kotlin.math.sqrt

class SwipeTouchHandler(
    override val keyboardView: KeyboardView
): TouchHandler {
    val rect: Rect = Rect()
    val keyCharacterMap: KeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
    var isSwiping = false
    val pointers = mutableListOf<SwipeListener.Pointer>()
    val listener = keyboardView.listener

    override fun onReset() {
        pointers.clear()
    }

    override fun onTouchDown(pointerId: Int, x: Int, y: Int) {
        setKeyboardDimens()
        pointers.clear()
        addPointer(x, y)
        val key = keyboardView.findKey(x, y)
        if(key != null) {
            keyboardView.findKeys(key.keyCode).forEach { it.onPressed() }
            listener.onKeyDown(key.keyCode, 0)
        }
    }

    override fun onTouchMove(pointerId: Int, x: Int, y: Int) {
        addPointer(x, y)
        val first = pointers.firstOrNull()
        val last = pointers.lastOrNull()
        if(!isSwiping && first != null && last != null && listener is SwipeListener) {
            val timeDiff = last.time - first.time
            val dist = sqrt((last.x - first.x).pow(2) + (last.y - first.y).pow(2))
            if(timeDiff > 300 && dist > 0.05 || dist > 0.1) {
                val key = pointers.firstOrNull()?.let { keyboardView.findKey(it.rawX, it.rawY) }
                if(key != null) {
                    keyboardView.findKeys(key.keyCode).forEach { it.onReleased() }
                }
                isSwiping = true
                listener.onReset()
                listener.onSwipeStart()
            }
        }
        if(isSwiping) {
            if(listener is SwipeListener) {
                listener.onSwipeMove(pointers.toList())
            }
        }
    }

    override fun onTouchUp(pointerId: Int, x: Int, y: Int) {
        addPointer(x, y)
        if(isSwiping) {
            if(listener is SwipeListener) {
                listener.onSwipeEnd(pointers.toList())
            }
        } else {
            val key = pointers.firstOrNull()?.let { keyboardView.findKey(it.rawX, it.rawY) }
            if(key != null) {
                keyboardView.findKeys(key.keyCode).forEach { it.onReleased() }
                listener.onKeyUp(key.keyCode, 0)
            }
        }
        isSwiping = false
    }

    private fun setKeyboardDimens() {
        val keyCodes = KeyEvent.KEYCODE_A ..KeyEvent.KEYCODE_Z
        val keySet = keyCodes.flatMap { keyboardView.findKeys(it) }
        val left = keySet.minOf { it.rect.left }
        val right = keySet.maxOf { it.rect.right }
        val top = keySet.minOf { it.rect.top }
        val bottom = keySet.maxOf { it.rect.bottom }
        rect.set(left, top, right, bottom)
    }

    private fun addPointer(x: Int, y: Int) {
        pointers += SwipeListener.Pointer(
            rawX = x,
            rawY = y,
            x = (x - rect.left) / rect.width().toFloat(),
            y = (y - rect.top) / rect.height().toFloat(),
            time = System.currentTimeMillis(),
            touchedKey = keyboardView.findKey(x, y)?.keyCode?.let { keyCharacterMap.get(it, 0).toChar() } ?: 'a'
        )
    }
}