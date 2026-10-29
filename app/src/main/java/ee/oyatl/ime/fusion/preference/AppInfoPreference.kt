package ee.oyatl.ime.fusion.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.core.net.toUri
import androidx.preference.Preference
import ee.oyatl.ime.fusion.BuildConfig
import ee.oyatl.ime.fusion.R

class AppInfoPreference(
    context: Context,
    attrs: AttributeSet
): Preference(context, attrs) {
    init {
        summary = context.getString(R.string.settings_info_version) + BuildConfig.VERSION_NAME
    }
}