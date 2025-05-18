package ee.oyatl.ime.keyboard

import android.os.Handler
import android.os.Looper

class RepeatableKeyListener(
    private val listener: Keyboard.Listener
): Keyboard.Listener {
    private val handler = Handler(Looper.getMainLooper())
    private fun repeat() {
        listener.onSpecial(Keyboard.SpecialKey.Delete, true)
        listener.onSpecial(Keyboard.SpecialKey.Delete, false)
        handler.postDelayed({ repeat() }, 50)
    }

    override fun onChar(code: Int) {
        listener.onChar(code)
    }

    override fun onSpecial(type: Keyboard.SpecialKey, pressed: Boolean) {
        listener.onSpecial(Keyboard.SpecialKey.Delete, pressed)
        if(pressed) {
            handler.postDelayed({ repeat() }, 500)
        } else {
            handler.removeCallbacksAndMessages(null)
        }
    }
}