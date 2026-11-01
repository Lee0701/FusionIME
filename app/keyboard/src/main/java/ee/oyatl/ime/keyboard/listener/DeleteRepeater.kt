package ee.oyatl.ime.keyboard.listener

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import ee.oyatl.ime.fusion.keyboard.core.FusionKeyEvent
import ee.oyatl.ime.fusion.keyboard.core.KeyboardParams

class DeleteRepeater(
    val listener: KeyboardListener,
    val params: KeyboardParams
): KeyboardListener {
    private val handler = Handler(Looper.getMainLooper())

    override fun onKeyEvent(event: FusionKeyEvent): Boolean = when(event) {
        is FusionKeyEvent.Press -> {
            when(event.keyCode) {
                KeyEvent.KEYCODE_DEL -> {
                    onDeletePressed(event)
                    return true
                }
            }
            handler.removeCallbacksAndMessages(null)
            return false
        }
        is FusionKeyEvent.Release -> {
            when(event.keyCode) {
                KeyEvent.KEYCODE_DEL -> {
                    onDeleteReleased(event)
                    return true
                }
            }
            handler.removeCallbacksAndMessages(null)
            return false
        }
        is FusionKeyEvent.LongPress -> {
            return false
        }
        else -> {
            handler.removeCallbacksAndMessages(null)
            return false
        }
    }

    private fun repeat(event: FusionKeyEvent) {
        listener.onKeyEvent(FusionKeyEvent.Press(event.keyCode, event.shiftState, event.altGrState, event.source))
        listener.onKeyEvent(FusionKeyEvent.Release(event.keyCode, event.shiftState, event.altGrState, event.source))
        handler.postDelayed({ repeat(event) }, params.repeatInterval.toLong())
    }

    private fun onDeletePressed(event: FusionKeyEvent) {
        listener.onKeyEvent(event)
        handler.postDelayed({ repeat(event) }, params.repeatDelay.toLong())
    }

    private fun onDeleteReleased(event: FusionKeyEvent) {
        listener.onKeyEvent(event)
        handler.removeCallbacksAndMessages(null)
    }
}