package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.View
import ee.oyatl.ime.keyboard.listener.KeyboardListener

class ScreenModeKeyboard(
    val mobile: Keyboard,
    val tablet: Keyboard = mobile,
    val full: Keyboard = tablet
): Keyboard {
    private var screenMode: KeyboardState.ScreenMode = KeyboardState.ScreenMode.Mobile
    private val currentKeyboard: Keyboard get() = when(screenMode) {
        KeyboardState.ScreenMode.Mobile -> mobile
        KeyboardState.ScreenMode.Tablet -> tablet
        KeyboardState.ScreenMode.Full -> full
    }
    override val numRows: Int get() = currentKeyboard.numRows

    override fun createView(
        context: Context,
        listener: KeyboardListener,
        params: KeyboardViewParams
    ): View {
        return currentKeyboard.createView(context, listener, params)
    }

    override fun setState(state: KeyboardState) {
        if(state is KeyboardState.ScreenMode) screenMode = state
        currentKeyboard.setState(state)
    }
}