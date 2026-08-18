package ee.oyatl.ime.keyboard.touchhandler

import android.os.Handler
import android.os.Looper
import android.view.ViewConfiguration
import ee.oyatl.ime.keyboard.KeyboardParams
import ee.oyatl.ime.keyboard.KeyboardView
import ee.oyatl.ime.keyboard.listener.LongPressListener
import ee.oyatl.ime.keyboard.popup.SelectionPopup
import kotlin.math.hypot

class LongPressTouchHandler(
    override val keyboardView: KeyboardView,
    private val delegate: TouchHandler,
    private val params: KeyboardParams
): TouchHandler {
    private val handler = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(keyboardView.view.context).scaledTouchSlop
    private val pointers = mutableMapOf<Int, Pointer>()

    init {
        require(delegate.keyboardView === keyboardView)
    }

    override fun onReset() {
        pointers.values.forEach { pointer ->
            handler.removeCallbacks(pointer.activate)
        }
        pointers.clear()
        delegate.onReset()
    }

    override fun onTouchDown(pointerId: Int, x: Int, y: Int) {
        delegate.onTouchDown(pointerId, x, y)
        val key = keyboardView.findKey(x, y) ?: return

        val pointer = Pointer(pointerId, x, y, key)
        pointers[pointerId] = pointer
        handler.postDelayed(pointer.activate, params.longPressDelay.toLong())
    }

    override fun onTouchMove(pointerId: Int, x: Int, y: Int) {
        val pointer = pointers[pointerId]
        if(pointer == null) {
            delegate.onTouchMove(pointerId, x, y)
            return
        }
        if(pointer.activated) {
            val rawX = keyboardView.location[0] + x
            val rawY = keyboardView.location[1] + y
            pointer.popup?.selectAt(rawX, rawY)
            return
        }

        val moved = hypot(
            (x - pointer.downX).toDouble(),
            (y - pointer.downY).toDouble()
        ) > touchSlop
        val currentKey = keyboardView.findKey(x, y)
        if(moved || currentKey?.keyCode != pointer.key.keyCode) {
            cancelActivation(pointer)
        }
        delegate.onTouchMove(pointerId, x, y)
    }

    override fun onTouchUp(pointerId: Int, x: Int, y: Int) {
        val pointer = pointers.remove(pointerId)
        if(pointer == null) {
            delegate.onTouchUp(pointerId, x, y)
            return
        }
        handler.removeCallbacks(pointer.activate)
        if(!pointer.activated) {
            delegate.onTouchUp(pointerId, x, y)
            return
        }
        keyboardView.popupManager.removePopup(pointer.key)
    }

    override fun onTouchCancel(pointerId: Int) {
        val pointer = pointers.remove(pointerId)
        if(pointer != null) {
            handler.removeCallbacks(pointer.activate)
        }
        delegate.onTouchCancel(pointerId)
    }

    private fun activate(pointerId: Int) {
        val pointer = pointers[pointerId] ?: return
        if(pointer.cancelled || pointer.activated) return
        val listener = keyboardView.listener
        if(listener !is LongPressListener) return
        if(!listener.onKeyLongPress(pointer.key.keyCode, 0)) return

        delegate.onTouchCancel(pointerId)
        pointer.activated = true
    }

    private fun cancelActivation(pointer: Pointer) {
        if(pointer.cancelled) return
        pointer.cancelled = true
        handler.removeCallbacks(pointer.activate)
    }

    private inner class Pointer(
        val id: Int,
        val downX: Int,
        val downY: Int,
        val key: KeyboardView.Key
    ) {
        val activate = Runnable { activate(id) }
        var cancelled = false
        var activated = false
        val popup: SelectionPopup? get() = keyboardView.popupManager.getPopup(key) as? SelectionPopup
    }
}
