package ee.oyatl.ime.keyboard.listener

import android.view.KeyEvent

class InputOnKeyRelease(
    val listener: KeyboardListener
): KeyboardListener {
    override fun onKeyDown(keyCode: Int, metaState: Int) {
    }

    override fun onKeyUp(keyCode: Int, metaState: Int) {
        if(keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) return
        listener.onKeyDown(keyCode, metaState)
        listener.onKeyUp(keyCode, metaState)
    }

    override fun onReset() {
        listener.onReset()
    }
}