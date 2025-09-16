package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.View
import ee.oyatl.ime.keyboard.listener.KeyboardListener
import ee.oyatl.ime.keyboard.listener.SymbolStateKeyboardListener
import ee.oyatl.ime.keyboard.switcher.SymbolKeyboardSwitcher

class SymbolStateKeyboard(
    private val text: Keyboard,
    private val symbol: Keyboard = text,
    private val number: Keyboard = DefaultNumberKeyboard(),
): Keyboard {
    private var keyboardSwitcher: SymbolKeyboardSwitcher? = null

    override val numRows: Int = listOf(text, symbol, number).maxOf { it.numRows }

    override fun createView(context: Context, listener: KeyboardListener, params: KeyboardViewParams): View {
        val keyboardSwitcher = SymbolKeyboardSwitcher(
            context,
            text.createView(context, getListener(listener, KeyboardState.Symbol.Text), params.copy(params.keyHeight * 4 / text.numRows)),
            symbol.createView(context, getListener(listener, KeyboardState.Symbol.Symbol), params.copy(params.keyHeight * 4 / symbol.numRows)),
            number.createView(context, getListener(listener, KeyboardState.Symbol.Number), params.copy(params.keyHeight * 4 / number.numRows))
        )
        this.keyboardSwitcher = keyboardSwitcher
        return keyboardSwitcher.view
    }

    override fun setState(state: KeyboardState) {
        if(state is KeyboardState.Symbol) keyboardSwitcher?.switch(state)
        listOf(text, symbol, number).forEach { it.setState(state) }
    }

    private fun getListener(listener: KeyboardListener, state: KeyboardState.Symbol): KeyboardListener {
        if(listener !is SymbolStateKeyboardListener) return listener
        return when(state) {
            KeyboardState.Symbol.Text -> listener.text
            KeyboardState.Symbol.Symbol -> listener.symbol
            KeyboardState.Symbol.Number -> listener.number
        }
    }
}