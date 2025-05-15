package ee.oyatl.ime.keyboard.keyboardset

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import ee.oyatl.ime.keyboard.Keyboard

class StackedKeyboardSet(
    private val keyboardSets: List<KeyboardSet>
): KeyboardSet {
    private lateinit var view: LinearLayout

    constructor(vararg keyboardSets: KeyboardSet): this(keyboardSets.toList())

    override fun initView(context: Context): View {
        view = LinearLayout(context)
        view.orientation = LinearLayout.VERTICAL
        keyboardSets.forEach { keyboardSet ->
            view.addView(keyboardSet.initView(context))
        }
        return view
    }

    override fun getView(shiftState: Keyboard.ShiftState, candidates: Boolean): View {
        view.removeAllViews()
        keyboardSets.forEach { keyboardSet ->
            view.addView(keyboardSet.getView(shiftState, candidates))
        }
        return view
    }
}