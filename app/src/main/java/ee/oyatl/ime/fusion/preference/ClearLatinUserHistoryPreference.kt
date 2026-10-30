package ee.oyatl.ime.fusion.preference

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import com.android.inputmethod.latin.DictionaryFacilitator.DictionaryInitializationListener
import com.android.inputmethod.latin.DictionaryFacilitatorProvider
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.mode.LatinIMEMode
import java.util.Locale

class ClearLatinUserHistoryPreference(
    context: Context,
    attrs: AttributeSet
): Preference(context, attrs), DictionaryInitializationListener {
    override fun onClick() {
        super.onClick()
        val localeName = preferenceDataStore?.getString("locale", null) ?: return
        val locale = LatinIMEMode.Params.parseLocale(localeName)
        AlertDialog.Builder(context)
            .setTitle(R.string.settings_input_mode_clear_user_history_title)
            .setMessage(context.getString(R.string.settings_input_mode_clear_user_history_confirm_language, locale.displayName))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val result = clear(locale)
                AlertDialog.Builder(context)
                    .setTitle(R.string.settings_input_mode_clear_user_history_title)
                    .setMessage(
                        if(result) R.string.settings_input_mode_clear_user_history_cleared
                        else R.string.settings_input_mode_clear_user_history_failed
                    )
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .show()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .show()
    }

    private fun clear(locale: Locale): Boolean {
        val dictionaryFacilitator = DictionaryFacilitatorProvider.getDictionaryFacilitator(false)
        dictionaryFacilitator.resetDictionaries(
            context,
            locale,
            false,
            true,
            false,
            null,
            "",
            this
        )
        return dictionaryFacilitator.clearUserHistoryDictionary(context)
    }

    override fun onUpdateMainDictionaryAvailability(isMainDictionaryAvailable: Boolean) = Unit
}