package ee.oyatl.ime.fusion.keyboard.host

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.encodeToStream
import ee.oyatl.ime.fusion.keyboard.core.HardKeyCodeMapper
import ee.oyatl.ime.fusion.keyboard.core.KeyboardConfiguration
import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPresetDef
import ee.oyatl.ime.fusion.keyboard.core.KeyboardTemplate
import ee.oyatl.ime.fusion.keyboard.core.LayoutTable
import ee.oyatl.ime.fusion.keyboard.core.LongPressTable
import ee.oyatl.ime.fusion.keyboard.core.SoftKeyCodeMapper
import ee.oyatl.ime.fusion.layout.Hangul2Set
import ee.oyatl.ime.fusion.layout.LatinMoreKeysTable
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.layout.LayoutQwerty
import ee.oyatl.ime.fusion.layout.MobileKeyboard
import ee.oyatl.ime.fusion.layout.MobileKeyboardRows
import ee.oyatl.ime.fusion.layout.TabletKeyboard
import ee.oyatl.ime.fusion.layout.TabletKeyboardRows
import java.io.File

object KeyboardLayoutExporter {
    const val EXPORT_DIR = "app/src/main/assets/keyboard"
    const val EXT_YAML = ".yaml"

    const val DEF_HANGUL_2SET_KS = "def_hangul_2set_ks"

    const val KEYBOARD_TEMPLATE_HANGUL_KS_COMPATIBLE = "keyboard_template_hangul_ks_compatible"

    const val LAYOUT_TABLE_EXT = "layout_table_ext"
    const val LAYOUT_TABLE_QWERTY = "layout_table_qwerty"
    const val LAYOUT_TABLE_HANGUL_2SET_KS = "layout_table_hangul_2set_ks"

    const val MORE_KEYS_TABLE_LATIN_DEFAULT = "more_keys_table_latin_default"

    const val SOFT_KEYCODE_MAPPER_EMPTY = "soft_keycode_mapper_empty"

    const val HARD_KEYCODE_MAPPER_EMPTY = "hard_keycode_mapper_empty"

    private val yaml: Yaml = Yaml(
        configuration = YamlConfiguration(
            encodeDefaults = false
        )
    )

    private inline fun <reified T> exportAll(
        map: Map<String, T>
    ) {
        map.forEach { (name, content) -> export(name, content) }
    }

    private inline fun <reified T> export(
        name: String,
        content: T
    ) {
        yaml.encodeToStream(content, getFile(name).outputStream())
    }

    private fun getFile(name: String): File {
        val exportDir = File(EXPORT_DIR)
        exportDir.mkdirs()
        return File(exportDir, name + EXT_YAML)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val numberRow = false
        val cursorKeys = false
        val keyboardTemplateHangulKSCompatible = KeyboardTemplate.ByScreenMode(
            mobile = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    if (numberRow) MobileKeyboard.numbers() else KeyboardConfiguration(),
                    MobileKeyboard.alphabetic(semicolon = false),
                    MobileKeyboard.bottom(dpad = cursorKeys)
                ),
                contentRows = (if(numberRow) MobileKeyboardRows.NUMBERS else listOf()) + MobileKeyboardRows.KS
            ),
            tablet = KeyboardTemplate.Basic(
                configuration = KeyboardConfiguration(
                    if(numberRow) TabletKeyboard.numbers(delete = true) else KeyboardConfiguration(),
                    TabletKeyboard.alphabetic(semicolon = false, delete = !numberRow),
                    TabletKeyboard.bottom()
                ),
                contentRows = (if(numberRow) TabletKeyboardRows.NUMBERS else listOf()) + TabletKeyboardRows.KS
            )
        )

        val defHangul2SetKS = KeyboardLayoutPresetDef(
            keyboardTemplate = KEYBOARD_TEMPLATE_HANGUL_KS_COMPATIBLE,
            layoutTables = listOf(LAYOUT_TABLE_EXT, LAYOUT_TABLE_QWERTY, LAYOUT_TABLE_HANGUL_2SET_KS),
            altGraphTables = listOf(),
            longPressTables = listOf(MORE_KEYS_TABLE_LATIN_DEFAULT),
            softKeyCodeMapper = SOFT_KEYCODE_MAPPER_EMPTY,
            hardKeyCodeMapper = HARD_KEYCODE_MAPPER_EMPTY
        )

        val keyboardTemplates = mapOf(
            KEYBOARD_TEMPLATE_HANGUL_KS_COMPATIBLE to keyboardTemplateHangulKSCompatible
        )
        val layoutTables = mapOf(
            LAYOUT_TABLE_EXT to LayoutTable.fromShiftStates(LayoutExt.TABLE),
            LAYOUT_TABLE_QWERTY to LayoutTable.fromShiftStates(LayoutQwerty.TABLE_QWERTY),
            LAYOUT_TABLE_HANGUL_2SET_KS to LayoutTable.fromShiftStates(Hangul2Set.TABLE_KS)
        )
        val moreKeysTables = mapOf(
            MORE_KEYS_TABLE_LATIN_DEFAULT to LongPressTable.from(LatinMoreKeysTable.DEFAULT, LatinMoreKeysTable.DEFAULT_UPPERCASE_OVERRIDE)
        )
        val softKeyCodeMappers = mapOf(
            SOFT_KEYCODE_MAPPER_EMPTY to SoftKeyCodeMapper.Empty
        )
        val hardKeyCodeMappers = mapOf(
            HARD_KEYCODE_MAPPER_EMPTY to HardKeyCodeMapper()
        )
        val keyboardLayoutPresetDefs = mapOf(
            DEF_HANGUL_2SET_KS to defHangul2SetKS
        )
        exportAll<KeyboardTemplate>(keyboardTemplates)
        exportAll<LayoutTable>(layoutTables)
        exportAll<LongPressTable>(moreKeysTables)
        exportAll<SoftKeyCodeMapper>(softKeyCodeMappers)
        exportAll<HardKeyCodeMapper>(hardKeyCodeMappers)
        exportAll<KeyboardLayoutPresetDef>(keyboardLayoutPresetDefs)
    }
}