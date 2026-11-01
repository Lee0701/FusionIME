package ee.oyatl.ime.fusion.keyboard.core

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.decodeFromStream
import ee.oyatl.ime.fusion.hangul.JamoCombinationTable
import java.io.InputStream

abstract class KeyboardLayoutPresetLoader {
    private val yaml: Yaml = Yaml(
        configuration = YamlConfiguration(
            strictMode = false
        )
    )

    abstract fun openInput(name: String): InputStream

    fun load(name: String): KeyboardLayoutPresetDef {
        return import(name)
    }

    fun inflate(def: KeyboardLayoutPresetDef): KeyboardLayoutPreset {
        val keyboardTemplate = import<KeyboardTemplate>(def.keyboardTemplate)
        val layoutTable = import<LayoutTable>(def.layoutTable)
        val altGraphTable = import<LayoutTable>(def.altGraphTable)
        val moreKeysTable = import<MoreKeysTable>(def.moreKeysTable)
        val softKeyCodeMapper = import<KeyCodeMapper.ByScreenMode>(def.softKeyCodeMapper)
        val hardKeyCodeMapper = import<KeyCodeMapper>(def.hardKeyCodeMapper)
        val keyLabelTable = import<KeyLabelTable>(def.keyLabels)
        val extra = inflateExtra(def.extra)
        return KeyboardLayoutPreset(
            keyboardTemplate = keyboardTemplate,
            layoutTable = layoutTable,
            altGraphTable = altGraphTable,
            moreKeysTable = moreKeysTable,
            softKeyCodeMapper = softKeyCodeMapper,
            hardKeyCodeMapper = hardKeyCodeMapper,
            keyLabelTable = keyLabelTable,
            extra = extra
        )
    }

    fun inflateExtra(extra: KeyboardLayoutPresetDef.Extra): KeyboardLayout.Extra {
        return when(extra) {
            is KeyboardLayoutPresetDef.Extra.None -> KeyboardLayout.Extra.None
            is KeyboardLayoutPresetDef.Extra.Hangul -> {
                val jamoCombinationTable = import<JamoCombinationTable>(extra.jamoCombinationTable)
                KeyboardLayout.Extra.Hangul(
                    jamoCombinationTable = jamoCombinationTable
                )
            }
        }
    }

    private inline fun <reified T> importAll(
        names: List<String>
    ): List<T> {
        return names.map { import(it) }
    }

    private inline fun <reified T> import(
        name: String
    ): T {
        return yaml.decodeFromStream(openInput(name))
    }
}