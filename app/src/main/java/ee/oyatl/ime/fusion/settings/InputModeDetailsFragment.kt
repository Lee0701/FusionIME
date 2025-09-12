package ee.oyatl.ime.fusion.settings

import android.os.Bundle
import androidx.fragment.app.setFragmentResult
import androidx.preference.PreferenceFragmentCompat
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.mode.KoreanIMEMode
import ee.oyatl.ime.fusion.mode.LatinIMEMode

abstract class InputModeDetailsFragment(
    private val map: MutableMap<String, String>
): PreferenceFragmentCompat() {

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        preferenceManager.preferenceDataStore = StringMapPreferenceDataStore(map)
    }

    override fun onDetach() {
        val stringifiedMap = map.map { (key, value) -> "$key=$value" }.joinToString(";")
        val bundle = Bundle()
        bundle.putString(KEY_MAP, stringifiedMap)
        setFragmentResult(KEY_INPUT_MODE_DETAILS, bundle)
        super.onDetach()
    }

    class Latin(
        map: MutableMap<String, String>
    ): InputModeDetailsFragment(map) {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            setPreferencesFromResource(R.xml.pref_input_mode_latin, rootKey)
        }
    }

    class Korean(
        map: MutableMap<String, String>
    ): InputModeDetailsFragment(map) {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            setPreferencesFromResource(R.xml.pref_input_mode_korean, rootKey)
        }
    }

    companion object {
        const val KEY_INPUT_MODE_DETAILS: String = "inputModeDetails"
        const val KEY_MAP: String = "map"

        fun create(map: MutableMap<String, String>): InputModeDetailsFragment? {
            val type = map["type"]
            return when(type) {
                LatinIMEMode.TYPE -> Latin(map)
                KoreanIMEMode.TYPE -> Korean(map)
                else -> null
            }
        }
    }
}