package ee.oyatl.ime.fusion.keyboard.core

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.encodeToStream
import ee.oyatl.ime.fusion.hangul.JamoCombinationTable
import java.io.OutputStream

abstract class KeyboardLayoutPresetWriter {
    private val yaml: Yaml = Yaml(
        configuration = YamlConfiguration(
            encodeDefaults = false
        )
    )

    abstract fun openOutput(name: String): OutputStream

    fun save(name: String, preset: KeyboardLayoutPreset) {
        val template = "$name.template"
        val layout = "$name.layout"
        val altGr = "$name.altgr"
        val moreKeys = "$name.morekeys"
        val softKeyCode = "$name.softkeycode"
        val hardKeyCode = "$name.hardkeycode"
        val keyLabels = "$name.labels"
        export<KeyboardTemplate>(template, preset.keyboardTemplate)
        export<LayoutTable>(layout, preset.layoutTable)
        export<LayoutTable>(altGr, preset.altGraphTable)
        export<MoreKeysTable>(moreKeys, preset.moreKeysTable)
        export<SoftKeyCodeMapper>(softKeyCode, preset.softKeyCodeMapper)
        export<HardKeyCodeMapper>(hardKeyCode, preset.hardKeyCodeMapper)
        export<KeyLabels>(keyLabels, preset.keyLabels)
        val extra = saveExtra(name, preset.extra)
        val def = KeyboardLayoutPresetDef(
            keyboardTemplate = template,
            layoutTable = layout,
            altGraphTable = altGr,
            moreKeysTable = moreKeys,
            softKeyCodeMapper = softKeyCode,
            hardKeyCodeMapper = hardKeyCode,
            keyLabels = keyLabels,
            extra = extra
        )
        export<KeyboardLayoutPresetDef>(name, def)
    }

    fun saveExtra(name: String, extra: KeyboardLayoutPreset.Extra): KeyboardLayoutPresetDef.Extra {
        return when(extra) {
            is KeyboardLayoutPreset.Extra.None -> KeyboardLayoutPresetDef.Extra.None
            is KeyboardLayoutPreset.Extra.Hangul -> {
                val combination = "$name.combination"
                export<JamoCombinationTable>(combination, extra.jamoCombinationTable)
                KeyboardLayoutPresetDef.Extra.Hangul(
                    jamoCombinationTable = combination
                )
            }
        }
    }

    private inline fun <reified T> exportAll(
        map: Map<String, T>
    ) {
        map.forEach { (name, content) -> export(name, content) }
    }

    private inline fun <reified T> export(
        name: String,
        content: T
    ) {
        yaml.encodeToStream(content, openOutput(name))
    }

}