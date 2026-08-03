package ee.oyatl.ime.keyboard.listener

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import ee.oyatl.ime.keyboard.FlickKeyCode
import ee.oyatl.ime.keyboard.KeyboardParams
import ee.oyatl.ime.keyboard.touchhandler.FlickDirection

class DeleteRepeater(
    val listener: KeyboardListener,
    val params: KeyboardParams
): KeyboardListener, FlickListener {
    private val handler = Handler(Looper.getMainLooper())

    override fun onReset() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun onKeyDown(keyCode: Int, metaState: Int): Boolean {
        when(keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                onDeletePressed(keyCode, metaState)
                return true
            }
        }
        return false
    }

    override fun onKeyUp(keyCode: Int, metaState: Int): Boolean {
        when(keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                onDeleteReleased(keyCode, metaState)
                return true
            }
        }
        return false
    }

    override fun onFlick(
        keyCode: Int,
        direction: FlickDirection
    ): Boolean {
        handler.removeCallbacksAndMessages(null)
        return false
    }

    private fun repeat(code: Int, metaState: Int) {
        listener.onKeyDown(code, metaState)
        listener.onKeyUp(code, metaState)
        handler.postDelayed({ repeat(code, metaState) }, params.repeatInterval.toLong())
    }

    private fun onDeletePressed(code: Int, metaState: Int) {
        listener.onKeyDown(code, metaState)
        handler.postDelayed({ repeat(code, metaState) }, params.repeatDelay.toLong())
    }

    private fun onDeleteReleased(code: Int, metaState: Int) {
        listener.onKeyUp(code, metaState)
        handler.removeCallbacksAndMessages(null)
    }
}