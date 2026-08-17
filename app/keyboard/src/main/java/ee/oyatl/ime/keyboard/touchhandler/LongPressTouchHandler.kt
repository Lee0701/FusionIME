package ee.oyatl.ime.keyboard.touchhandler

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewConfiguration
import ee.oyatl.ime.keyboard.KeyboardView
import ee.oyatl.ime.keyboard.LongPressTable
import ee.oyatl.ime.keyboard.popup.SelectionPopup
import kotlin.math.hypot

class LongPressTouchHandler(
    override val keyboardView: KeyboardView,
    private val delegate: TouchHandler,
    private val table: LongPressTable,
    private val onLongPressStateChanged: (Boolean) -> Unit = {},
    private val timeoutMillis: Long = ViewConfiguration.getLongPressTimeout().toLong()
): TouchHandler {
    private val handler = Handler(Looper.getMainLooper())
    private val touchSlop = (keyboardView as? View)?.let {
        ViewConfiguration.get(it.context).scaledTouchSlop
    } ?: 0
    private val pointers = mutableMapOf<Int, Pointer>()
    private var longPressActive = false

    init {
        require(delegate.keyboardView === keyboardView)
    }

    override fun onReset() {
        pointers.values.forEach { pointer ->
            handler.removeCallbacks(pointer.activate)
            pointer.popup?.hide()
        }
        pointers.clear()
        updateLongPressState()
        delegate.onReset()
    }

    override fun onTouchDown(pointerId: Int, x: Int, y: Int) {
        delegate.onTouchDown(pointerId, x, y)
        val key = keyboardView.findKey(x, y) ?: return
        val baseCodePoint = key.label.singleCodePointOrNull() ?: return
        val candidates = table.candidatesFor(baseCodePoint)
        if(candidates.isEmpty()) return

        val pointer = Pointer(pointerId, x, y, key, candidates)
        pointers[pointerId] = pointer
        handler.postDelayed(pointer.activate, timeoutMillis)
    }

    override fun onTouchMove(pointerId: Int, x: Int, y: Int) {
        val pointer = pointers[pointerId]
        if(pointer == null) {
            delegate.onTouchMove(pointerId, x, y)
            return
        }
        if(pointer.activated) {
            pointer.popup?.selectAt(rawX(x), rawY(y))
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

        pointer.popup?.selectAt(rawX(x), rawY(y))
        val codePoint = pointer.popup?.selectedCodePoint
        pointer.popup?.hide()
        updateLongPressState()
        if(codePoint != null) {
            keyboardView.listener.onKeyUp(-codePoint, 0)
        }
    }

    override fun onTouchCancel(pointerId: Int) {
        val pointer = pointers.remove(pointerId)
        if(pointer != null) {
            handler.removeCallbacks(pointer.activate)
            pointer.popup?.hide()
        }
        updateLongPressState()
        delegate.onTouchCancel(pointerId)
    }

    private fun activate(pointerId: Int) {
        val pointer = pointers[pointerId] ?: return
        if(pointer.cancelled || pointer.activated) return
        val popup = keyboardView.popupManager.createLongPressPopup(
            pointer.key,
            pointer.candidates
        ) ?: return

        delegate.onTouchCancel(pointerId)
        pointer.popup = popup
        pointer.activated = true
        popup.show()
        popup.selectAt(rawX(pointer.downX), rawY(pointer.downY))
        updateLongPressState()
    }

    private fun cancelActivation(pointer: Pointer) {
        if(pointer.cancelled) return
        pointer.cancelled = true
        handler.removeCallbacks(pointer.activate)
    }

    private fun rawX(x: Int): Int = keyboardView.location[0] + x
    private fun rawY(y: Int): Int = keyboardView.location[1] + y

    private fun updateLongPressState() {
        val active = pointers.values.any(Pointer::activated)
        if(active != longPressActive) {
            longPressActive = active
            onLongPressStateChanged(active)
        }
    }

    private fun String.singleCodePointOrNull(): Int? {
        if(isEmpty() || codePointCount(0, length) != 1) return null
        return codePointAt(0)
    }

    private inner class Pointer(
        val id: Int,
        val downX: Int,
        val downY: Int,
        val key: TouchHandler.KeyInterface,
        val candidates: List<Int>
    ) {
        val activate = Runnable { activate(id) }
        var cancelled = false
        var activated = false
        var popup: SelectionPopup? = null
    }
}
