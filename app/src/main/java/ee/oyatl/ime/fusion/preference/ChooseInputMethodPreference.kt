package ee.oyatl.ime.fusion.preference

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.InputMethodManager
import androidx.preference.Preference

class ChooseInputMethodPreference(
    context: Context,
    attrs: AttributeSet?,
): Preference(context, attrs) {
    override fun onClick() {
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showInputMethodPicker()
    }
}