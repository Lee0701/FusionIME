package ee.oyatl.ime.fusion.setup

import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import ee.oyatl.ime.fusion.R
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
            Language.entries.map { getString(it.nameRes) }.toTypedArray()
        )
        binding.languages.onItemClickListener = this
        binding.finish.setOnClickListener {
            val checked = (0 until binding.languages.adapter.count)
                .filter { binding.languages.checkedItemPositions.get(it) }
            val entries = checked
                .map { Language.entries[it].content }
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
        val systemLocaleTags = (0 until systemLocales.size()).map { systemLocales.get(it) }.map { locale ->
            if(locale.language == "zh") locale.toLanguageTag()
            else locale.language
        }
        systemLocaleTags.forEach { locale ->
            val index = Language.entries.indexOfFirst { it.locale == locale }
            if(index != -1) binding.languages.setItemChecked(index, true)
        }
        binding.languages.setItemChecked(0, true)
        if(systemLocaleTags.isNotEmpty()) binding.finish.isEnabled = true
    }

    enum class Language(
        val locale: String,
        @StringRes val nameRes: Int,
        val content: Map<String, String>
    ) {
        English("en", R.string.latin_locale_en, mapOf("type" to LatinIMEMode.TYPE, "locale" to "en")),
        German("de", R.string.latin_locale_de, mapOf("type" to LatinIMEMode.TYPE, "locale" to "de", "layout" to LatinIMEMode.Layout.Deutsch.name)),
        Spanish("es", R.string.latin_locale_es, mapOf("type" to LatinIMEMode.TYPE, "locale" to "es")),
        French("fr", R.string.latin_locale_fr, mapOf("type" to LatinIMEMode.TYPE, "locale" to "fr", "layout" to LatinIMEMode.Layout.Azerty.name)),
        Italian("it", R.string.latin_locale_it, mapOf("type" to LatinIMEMode.TYPE, "locale" to "it")),
        Portuguese("pt", R.string.latin_locale_pt_br, mapOf("type" to LatinIMEMode.TYPE, "locale" to "pt_br")),
        Japanese("ja", R.string.input_mode_mozc, mapOf("type" to MozcIMEMode.TYPE)),
        Korean("ko", R.string.input_mode_korean, mapOf("type" to KoreanIMEMode.TYPE)),
        SimplifiedChinese("zh-Hans-CN", R.string.input_mode_pinyin, mapOf("type" to PinyinIMEMode.TYPE)),
        TraditionalChinese("zh-Hant-TW", R.string.input_mode_zhuyin, mapOf("type" to ZhuyinIMEMode.TYPE)),
        Cantonese("zh-Hant-HK", R.string.input_mode_jyutping, mapOf("type" to JyutpingIMEMode.TYPE)),
        Vietnamese("vi", R.string.input_mode_viet, mapOf("type" to VietIMEMode.TYPE))
    }
}