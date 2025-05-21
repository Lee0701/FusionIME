package ee.oyatl.ime.keyboard

import android.content.Context
import android.view.View
import android.widget.LinearLayout

class StackedKeyboard(
    private val keyboards: List<Keyboard>
): Keyboard {
    constructor(vararg keyboards: Keyboard): this(keyboards.toList())

    override val numRows: Int = keyboards.sumOf { it.numRows }

    override fun createView(context: Context, listener: Keyboard.Listener, height: Int): View {
        val view = LinearLayout(context)
        view.orientation = LinearLayout.VERTICAL
        keyboards.forEach { view.addView(it.createView(context, listener, height)) }
        return view
    }

    override fun changeState(state: KeyboardStateSet) {
        keyboards.forEach { it.changeState(state) }
    }
}