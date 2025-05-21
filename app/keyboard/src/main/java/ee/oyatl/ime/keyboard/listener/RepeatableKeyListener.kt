package ee.oyatl.ime.keyboard.listener

import android.os.Handler
import android.os.Looper

class RepeatableKeyListener(
    private val listener: KeyboardListener
): KeyboardListener {
    private val handler = Handler(Looper.getMainLooper())
    private fun repeat(code: Int) {
        listener.onKeyDown(code)
        listener.onKeyUp(code)
        handler.postDelayed({ repeat(code) }, 50)
    }

    override fun onKeyDown(code: Int) {
        listener.onKeyDown(code)
        listener.onKeyUp(code)
        handler.postDelayed({ repeat(code) }, 300)
    }

    override fun onKeyUp(code: Int) {
        handler.removeCallbacksAndMessages(null)
    }
}