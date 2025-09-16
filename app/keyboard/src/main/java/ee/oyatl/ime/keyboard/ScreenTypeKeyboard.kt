package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.View
import ee.oyatl.ime.keyboard.listener.KeyboardListener

class ScreenTypeKeyboard(
    val mobile: Keyboard,
    val tablet: Keyboard = mobile,
    val full: Keyboard = tablet
): Keyboard {
    private var screenType: KeyboardState.ScreenType = KeyboardState.ScreenType.Mobile
    private val currentKeyboard: Keyboard get() = when(screenType) {
        KeyboardState.ScreenType.Mobile -> mobile
        KeyboardState.ScreenType.Tablet -> tablet
        KeyboardState.ScreenType.Full -> full
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
        if(state is KeyboardState.ScreenType) screenType = state
        currentKeyboard.setState(state)
    }
}