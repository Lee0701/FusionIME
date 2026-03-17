package ee.oyatl.ime.fusion.settings

import android.os.Bundle
import androidx.fragment.app.setFragmentResult
import androidx.preference.PreferenceFragmentCompat
import ee.oyatl.ime.fusion.Feature
import ee.oyatl.ime.fusion.R
import ee.oyatl.ime.fusion.mode.CangjieIMEMode
import ee.oyatl.ime.fusion.mode.IMEMode
import ee.oyatl.ime.fusion.mode.KoreanIMEMode
import ee.oyatl.ime.fusion.mode.LatinIMEMode
import ee.oyatl.ime.fusion.mode.MozcIMEMode
import ee.oyatl.ime.fusion.mode.PinyinIMEMode
import ee.oyatl.ime.fusion.mode.VietIMEMode

abstract class InputModeDetailsFragment: PreferenceFragmentCompat() {

    val map: MutableMap<String, String> = mutableMapOf()

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        if(savedInstanceState != null) {
            val map = savedInstanceState.getString(KEY_MAP, "")
            this.map += parseMap(map)
        }
        preferenceManager.preferenceDataStore = StringMapPreferenceDataStore(map)
        activity?.title = IMEMode.Params.parse(map)?.getLabel(requireContext())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_MAP, stringifyMap(map))
    }

    override fun onPause() {
        super.onPause()
        save()
    }

    fun save() {
        val stringifiedMap = stringifyMap(map)
        val bundle = Bundle()
        bundle.putString(KEY_MAP, stringifiedMap)
        setFragmentResult(KEY_INPUT_MODE_DETAILS, bundle)
    }

    class Latin: InputModeDetailsFragment() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            addPreferencesFromResource(R.xml.pref_input_mode_latin)
            if(Feature.NumberRow.availableInCurrentVersion)
                addPreferencesFromResource(R.xml.pref_input_mode_number_row)
        }
    }

    class Korean: InputModeDetailsFragment() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            addPreferencesFromResource(R.xml.pref_input_mode_korean_layout)
            if(Feature.NumberRow.availableInCurrentVersion)
                addPreferencesFromResource(R.xml.pref_input_mode_number_row)
            if(Feature.BigramHanjaConverter.availableInCurrentVersion)
                addPreferencesFromResource(R.xml.pref_input_mode_korean_converter)
        }
    }

    class Mozc: InputModeDetailsFragment() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            addPreferencesFromResource(R.xml.pref_input_mode_mozc_layout)
            if(Feature.NumberRow.availableInCurrentVersion)
                addPreferencesFromResource(R.xml.pref_input_mode_number_row)
            if(Feature.MozcCandidateHeight.availableInCurrentVersion)
                addPreferencesFromResource(R.xml.pref_input_mode_mozc_candidate)
        }
    }

    class Pinyin: InputModeDetailsFragment() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            if(Feature.NumberRow.availableInCurrentVersion)
                addPreferencesFromResource(R.xml.pref_input_mode_number_row)
        }
    }

    class Viet: InputModeDetailsFragment() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            addPreferencesFromResource(R.xml.pref_input_mode_viet)
            if(Feature.NumberRow.availableInCurrentVersion)
                addPreferencesFromResource(R.xml.pref_input_mode_number_row)
        }
    }

    class Cangjie: InputModeDetailsFragment() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            addPreferencesFromResource(R.xml.pref_input_mode_cangjie_layout)
            if(Feature.NumberRow.availableInCurrentVersion)
                addPreferencesFromResource(R.xml.pref_input_mode_number_row)
            addPreferencesFromResource(R.xml.pref_input_mode_cangjie_extra)

        }
    }

    companion object {
        const val KEY_INPUT_MODE_DETAILS: String = "inputModeDetails"
        const val KEY_MAP: String = "map"

        fun create(map: Map<String, String>): InputModeDetailsFragment? {
            val type = map["type"]
            val fragment = when(type) {
                LatinIMEMode.TYPE -> Latin()
                KoreanIMEMode.TYPE -> Korean()
                MozcIMEMode.TYPE -> Mozc()
                PinyinIMEMode.TYPE -> Pinyin()
                VietIMEMode.TYPE -> Viet()
                CangjieIMEMode.TYPE -> Cangjie()
                else -> null
            }
            fragment?.map += map
            return fragment
        }

        fun parseMap(map: String): Map<String, String> {
            return map
                .split(';').map { it.split('=') }
                .associate { (key, value) -> key to value }.toMutableMap()
        }

        fun stringifyMap(map: Map<String, String>): String {
            return map.map { (key, value) -> "$key=$value" }.joinToString(";")
        }
    }
}