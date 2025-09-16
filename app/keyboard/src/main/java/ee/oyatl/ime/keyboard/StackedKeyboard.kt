package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import ee.oyatl.ime.keyboard.listener.KeyboardListener

class StackedKeyboard(
    private val keyboards: List<Keyboard>
): Keyboard {
    constructor(vararg keyboards: Keyboard): this(keyboards.toList())

    override val numRows: Int = keyboards.sumOf { it.numRows }

    override fun createView(context: Context, listener: KeyboardListener, params: KeyboardViewParams): View {
        val view = LinearLayout(context)
        view.orientation = LinearLayout.VERTICAL
        keyboards.forEach { view.addView(it.createView(context, listener, params)) }
        return view
    }

    override fun setState(state: KeyboardState) {
        keyboards.forEach { it.setState(state) }
    }
}