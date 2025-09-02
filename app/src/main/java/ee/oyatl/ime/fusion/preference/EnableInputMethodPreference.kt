package ee.oyatl.ime.fusion.preference

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.AttributeSet
import androidx.preference.Preference

class EnableInputMethodPreference(
    context: Context,
    attrs: AttributeSet?,
): Preference(context, attrs) {
    override fun onClick() {
        context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
    }
}