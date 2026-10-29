package ee.oyatl.ime.fusion.keyboard.core

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.decodeFromStream
import ee.oyatl.ime.fusion.hangul.JamoCombinationTable
import java.io.File
import java.io.InputStream

abstract class KeyboardLayoutPresetLoader {
    private val yaml: Yaml = Yaml(
        configuration = YamlConfiguration(
            strictMode = false
        )
    )

    abstract fun open(name: String): InputStream

    fun load(name: String): KeyboardLayoutPresetDef {
        return import(name)
    }

    fun inflate(def: KeyboardLayoutPresetDef): KeyboardLayoutPreset {
        val keyboardTemplate = import<KeyboardTemplate>(def.keyboardTemplate)
        val layoutTable = importAll<LayoutTable>(def.layoutTables)
            .fold(LayoutTable()) { acc, table -> acc + table }
        val altGraphTable = importAll<LayoutTable>(def.altGraphTables)
            .fold(LayoutTable()) { acc, table -> acc + table }
        val longPressTable = importAll<LongPressTable>(def.longPressTables)
            .fold(LongPressTable()) { acc, table -> acc + table }
        val softKeyCodeMapper = import<SoftKeyCodeMapper>(def.softKeyCodeMapper)
        val hardKeyCodeMapper = import<HardKeyCodeMapper>(def.hardKeyCodeMapper)
        val extra = inflateExtra(def.extra)
        return KeyboardLayoutPreset(
            keyboardTemplate = keyboardTemplate,
            layoutTable = layoutTable,
            altGraphTable = altGraphTable,
            longPressTable = longPressTable,
            softKeyCodeMapper = softKeyCodeMapper,
            hardKeyCodeMapper = hardKeyCodeMapper,
            extra = extra
        )
    }

    fun inflateExtra(extra: KeyboardLayoutPresetDef.Extra): KeyboardLayoutPreset.Extra {
        return when(extra) {
            is KeyboardLayoutPresetDef.Extra.None -> KeyboardLayoutPreset.Extra.None
            is KeyboardLayoutPresetDef.Extra.Hangul -> {
                val jamoCombinationTable = importAll<JamoCombinationTable>(extra.jamoCombinationTables)
                    .fold(JamoCombinationTable()) { acc, table -> acc + table }
                KeyboardLayoutPreset.Extra.Hangul(
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
        return yaml.decodeFromStream(open(name))
    }

    companion object {
        const val EXT_YAML = ".yaml"
    }
}