package ee.oyatl.ime.fusion.setup

import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import ee.oyatl.ime.fusion.databinding.FragmentSetupLanguagesBinding
import ee.oyatl.ime.fusion.mode.JyutpingIMEMode
import ee.oyatl.ime.fusion.mode.KoreanIMEMode
import ee.oyatl.ime.fusion.mode.LatinIMEMode
import ee.oyatl.ime.fusion.mode.MozcIMEMode
import ee.oyatl.ime.fusion.mode.PinyinIMEMode
import ee.oyatl.ime.fusion.mode.VietIMEMode
import ee.oyatl.ime.fusion.mode.ZhuyinIMEMode
import ee.oyatl.ime.fusion.settings.InputModeDetailsFragment
import ee.oyatl.ime.fusion.settings.InputModeSettingsFragment
import org.json.JSONArray
import java.util.Locale

class LanguagesFragment: Fragment(), AdapterView.OnItemClickListener {
    lateinit var binding: FragmentSetupLanguagesBinding
    lateinit var preference: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetupLanguagesBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        preference = PreferenceManager.getDefaultSharedPreferences(context)
        binding.languages.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_list_item_multiple_choice,
            LanguagePreset.entries.map { it.locale.displayName }.toTypedArray()
        )
        binding.languages.onItemClickListener = this
        binding.finish.setOnClickListener {
            val checked = (0 until binding.languages.adapter.count)
                .filter { binding.languages.checkedItemPositions.get(it) }
            val entries = checked
                .map { LanguagePreset.entries[it].content }
                .map { InputModeDetailsFragment.stringifyMap(it) }
            val json = JSONArray(entries).toString()
            preference.edit {
                putString(InputModeSettingsFragment.PREF_KEY, json)
            }
            activity?.finish()
        }
        setSystemLocales()
    }

    override fun onItemClick(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
    ) {
        binding.finish.isEnabled = binding.languages.checkedItemCount > 0
    }

    private fun setSystemLocales() {
        val systemLocales = Resources.getSystem().configuration.locales
        val systemLanguages = (0 until systemLocales.size())
            .map { systemLocales.get(it) }.map { LanguagePreset.match(it) }
        systemLanguages.forEach { preset ->
            val index = LanguagePreset.entries.indexOfFirst { it == preset }
            if(index != -1) binding.languages.setItemChecked(index, true)
        }
        binding.languages.setItemChecked(0, true)
        if(systemLanguages.isNotEmpty()) binding.finish.isEnabled = true
    }

    companion object {
        private fun localeOf(
            language: String,
            region: String? = null,
            script: String? = null
        ): Locale {
            val builder = Locale.Builder()
            builder.setLanguage(language)
            if(region != null) builder.setRegion(region)
            if(script != null) builder.setScript(script)
            return builder.build()
        }
    }

    enum class LanguagePreset(
        val locale: Locale,
        val content: Map<String, String>
    ) {
        English(Locale.ENGLISH, mapOf("type" to LatinIMEMode.TYPE, "locale" to "en")),
        German(Locale.GERMAN, mapOf("type" to LatinIMEMode.TYPE, "locale" to "de", "layout" to LatinIMEMode.Layout.Deutsch.name)),
        Spanish(localeOf("es"), mapOf("type" to LatinIMEMode.TYPE, "locale" to "es")),
        French(Locale.FRENCH, mapOf("type" to LatinIMEMode.TYPE, "locale" to "fr", "layout" to LatinIMEMode.Layout.Azerty.name)),
        Italian(Locale.ITALIAN, mapOf("type" to LatinIMEMode.TYPE, "locale" to "it")),
        BrazilianPortuguese(localeOf("pt", "BR"), mapOf("type" to LatinIMEMode.TYPE, "locale" to "pt_br")),
        Japanese(Locale.JAPANESE, mapOf("type" to MozcIMEMode.TYPE)),
        Korean(Locale.KOREAN, mapOf("type" to KoreanIMEMode.TYPE)),
        SimplifiedChinese(Locale.SIMPLIFIED_CHINESE, mapOf("type" to PinyinIMEMode.TYPE)),
        TraditionalChinese(Locale.TRADITIONAL_CHINESE, mapOf("type" to ZhuyinIMEMode.TYPE)),
        Cantonese(localeOf("zh", "HK"), mapOf("type" to JyutpingIMEMode.TYPE)),
        Vietnamese(localeOf("vi"), mapOf("type" to VietIMEMode.TYPE));

        companion object {
            fun match(locale: Locale): LanguagePreset? {
                return when(locale.language) {
                    "en" -> English
                    "de" -> German
                    "es" -> Spanish
                    "fr" -> French
                    "it" -> Italian
                    "pt" -> {
                        if(locale.country == "BR") BrazilianPortuguese
                        else null
                    }
                    "ja" -> Japanese
                    "ko" -> Korean
                    "zh" -> {
                        if(locale.country == "CN") SimplifiedChinese
                        else if(locale.country == "TW") TraditionalChinese
                        else if(locale.country == "HK") Cantonese
                        else null
                    }
                    "vi" -> Vietnamese
                    else -> null
                }
            }
        }
    }
}