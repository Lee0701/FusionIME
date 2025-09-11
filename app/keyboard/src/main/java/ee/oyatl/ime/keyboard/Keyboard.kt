package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.View
import ee.oyatl.ime.keyboard.listener.KeyboardListener

interface Keyboard {
    val numRows: Int

    fun createView(context: Context, listener: KeyboardListener, params: KeyboardViewParams): View
    fun setShiftState(state: KeyboardState.Shift)

    enum class SpecialKey(
        val code: Int
    ) {
        Shift(-1), Caps(-2),
        Space(' '.code), Return('\n'.code), Delete('\b'.code),
        Language(-10), Symbols(-3), Numbers(-13);

        companion object {
            fun ofCode(code: Int): SpecialKey? {
                return entries.find { it.code == code }
            }
        }
    }
}