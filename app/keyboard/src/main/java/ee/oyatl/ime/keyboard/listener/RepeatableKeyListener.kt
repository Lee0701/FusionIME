package ee.oyatl.ime.keyboard.listener

import android.os.Handler
import android.os.Looper

class RepeatableKeyListener(
    private val listener: KeyboardListener,
    private val delay: Int = 300,
    private val interval: Int = 50,
): KeyboardListener {
    private val handler = Handler(Looper.getMainLooper())
    private fun repeat(code: Int) {
        listener.onKeyDown(code)
        listener.onKeyUp(code)
        handler.postDelayed({ repeat(code) }, interval.toLong())
    }

    override fun onKeyDown(code: Int) {
        listener.onKeyDown(code)
        listener.onKeyUp(code)
        handler.postDelayed({ repeat(code) }, delay.toLong())
    }

    override fun onKeyUp(code: Int) {
        handler.removeCallbacksAndMessages(null)
    }
}