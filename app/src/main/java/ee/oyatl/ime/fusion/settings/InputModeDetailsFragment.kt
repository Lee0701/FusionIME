package ee.oyatl.ime.fusion.settings

import android.os.Bundle
import androidx.fragment.app.setFragmentResult
import androidx.preference.PreferenceFragmentCompat
import ee.oyatl.ime.fusion.Feature
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.mode.CangjieIMEMode
import ee.oyatl.ime.fusion.mode.KoreanIMEMode
import ee.oyatl.ime.fusion.mode.LatinIMEMode
import ee.oyatl.ime.fusion.mode.MozcIMEMode
import ee.oyatl.ime.fusion.mode.VietIMEMode

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
        save()
        super.onDetach()
    }

    fun save() {
        val stringifiedMap = map.map { (key, value) -> "$key=$value" }.joinToString(";")
        val bundle = Bundle()
        bundle.putString(KEY_MAP, stringifiedMap)
        setFragmentResult(KEY_INPUT_MODE_DETAILS, bundle)
    }

    class Latin(
        map: MutableMap<String, String>
    ): InputModeDetailsFragment(map) {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            addPreferencesFromResource(R.xml.pref_input_mode_latin)
        }
    }

    class Korean(
        map: MutableMap<String, String>
    ): InputModeDetailsFragment(map) {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            addPreferencesFromResource(R.xml.pref_input_mode_korean_layout)
            if(Feature.BigramHanjaConverter.availableInCurrentVersion)
                addPreferencesFromResource(R.xml.pref_input_mode_korean_converter)
        }
    }

    class Mozc(
        map: MutableMap<String, String>
    ): InputModeDetailsFragment(map) {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            addPreferencesFromResource(R.xml.pref_input_mode_mozc_layout)
            if(Feature.MozcCandidateHeight.availableInCurrentVersion)
                addPreferencesFromResource(R.xml.pref_input_mode_mozc_candidate)
        }
    }

    class Viet(
        map: MutableMap<String, String>
    ): InputModeDetailsFragment(map) {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            addPreferencesFromResource(R.xml.pref_input_mode_viet)
        }
    }

    class Cangjie(
        map: MutableMap<String, String>
    ): InputModeDetailsFragment(map) {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            setPreferencesFromResource(R.xml.pref_input_mode_cangjie, rootKey)
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
                MozcIMEMode.TYPE -> Mozc(map)
                VietIMEMode.TYPE -> Viet(map)
                CangjieIMEMode.TYPE -> Cangjie(map)
                else -> null
            }
        }
    }
}