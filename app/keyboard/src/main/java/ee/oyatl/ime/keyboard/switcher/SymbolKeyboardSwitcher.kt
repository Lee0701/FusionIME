package ee.oyatl.ime.keyboard.switcher

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import ee.oyatl.ime.keyboard.KeyboardState

class SymbolKeyboardSwitcher(
    context: Context,
    private val textView: View,
    private val symbolView: View,
    private val numberView: View
): KeyboardSwitcher<KeyboardState.Symbol> {
    private val switcherView: FrameLayout = FrameLayout(context)

    init {
        if(textView.parent == null) switcherView.addView(textView)
        if(symbolView.parent == null) switcherView.addView(symbolView)
        if(numberView.parent == null) switcherView.addView(numberView)
    }

    override val view: View
        get() = switcherView

    override fun switch(state: KeyboardState.Symbol) {
        when(state) {
            KeyboardState.Symbol.Text -> {
                textView.bringToFront()
            }
            KeyboardState.Symbol.Symbol -> {
                symbolView.bringToFront()
            }
            KeyboardState.Symbol.Number -> {
                numberView.bringToFront()
            }
        }
    }
}