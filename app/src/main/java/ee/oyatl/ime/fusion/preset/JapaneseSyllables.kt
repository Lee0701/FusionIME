package ee.oyatl.ime.fusion.preset

import ee.oyatl.ime.fusion.keyboard.core.KeyCode
import ee.oyatl.ime.fusion.keyboard.core.KeyboardConfiguration
import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.keyboard.core.KeyboardTemplate
import ee.oyatl.ime.fusion.keyboard.core.KeyboardTemplate.Basic
import ee.oyatl.ime.fusion.keyboard.core.LayoutTable
import ee.oyatl.ime.fusion.layout.LayoutKana.BOTTOM_LEFT_SYLLABLES
import ee.oyatl.ime.fusion.layout.LayoutKana.BOTTOM_RIGHT_SYLLABLES
import ee.oyatl.ime.fusion.layout.TabletKeyboard
import kotlin.math.ceil

object JapaneseSyllables {
    fun syllables(
        keys: String,
        keyLayout: KeyLayout
    ): KeyboardLayoutPreset {
        val contentRows = generateContentRows(keys, keyLayout)
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = Basic(
                    configuration = mobileKeyboardConfigurationSyllables(contentRows),
                    contentRows = emptyList()
                ),
                tablet = Basic(
                    configuration = tabletKeyboardConfigurationSyllables(contentRows),
                    contentRows = emptyList()
                )
            ),
            layoutTable = LayoutTable()
        )
    }

    enum class KeyLayout {
        Horizontal, VerticalLeft, VerticalRight
    }

    fun generateContentRows(
        keys: String,
        keyLayout: KeyLayout
    ): List<String> {
        return when(keyLayout) {
            KeyLayout.Horizontal -> group(keys, 10)
            KeyLayout.VerticalLeft -> transpose(group(keys, 5))
            KeyLayout.VerticalRight -> transpose(group(keys, 5).reversed())
        }
    }

    fun group(string: String, n: Int): List<String> {
        val len = ceil(string.length.toFloat() / n).toInt()
        return (0 until len).map { i -> string.substring(i * n, (i + 1) * n) }
    }

    fun transpose(layout: List<String>): List<String> {
        return layout[0].indices.map { i -> layout.map { it[i] }.joinToString("") }
    }

    fun mobileKeyboardConfigurationSyllables(
        contentRows: List<String>
    ): KeyboardConfiguration {
        val rows = contentRows.map { row ->
            row.map { item -> when(item) {
                '　' -> KeyboardConfiguration.Item.Spacer(width = 1f)
                else -> KeyboardConfiguration.Item.TemplateKey(-item.code)
            } }
        }
        val bottom = listOf(
            KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_SYM, 1.5f, true),
            KeyboardConfiguration.Item.TemplateKey(-BOTTOM_LEFT_SYLLABLES[0].code),
            KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_LANGUAGE_SWITCH, 1f, true),
            KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_SPACE, 2f),
            KeyboardConfiguration.Item.TemplateKey(-BOTTOM_RIGHT_SYLLABLES[0].code),
            KeyboardConfiguration.Item.TemplateKey(-BOTTOM_RIGHT_SYLLABLES[1].code),
            KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_ENTER, 1.5f, true),
            KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_DEL, 1f, true)
        )
        return KeyboardConfiguration(rows + listOf(bottom))
    }

    fun tabletKeyboardConfigurationSyllables(
        contentRows: List<String>
    ): KeyboardConfiguration {
        val rows = contentRows.map { row ->
            row.map { item -> when(item) {
                '　' -> KeyboardConfiguration.Item.Spacer(width = 1f)
                else -> KeyboardConfiguration.Item.TemplateKey(-item.code)
            } }.toMutableList()
        }
        rows[0].add(0, KeyboardConfiguration.Item.TemplateKey(-BOTTOM_LEFT_SYLLABLES[0].code))
        rows[0].add(KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_DEL, 1f, true))
        rows[1].add(0, KeyboardConfiguration.Item.Spacer(1f))
        rows[1].add(KeyboardConfiguration.Item.Spacer(1f))
        rows[2].add(0, KeyboardConfiguration.Item.TemplateKey(-BOTTOM_RIGHT_SYLLABLES[0].code))
        rows[2].add(KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_ENTER, 1f, true))
        rows[3].add(0, KeyboardConfiguration.Item.Spacer(1f))
        rows[3].add(KeyboardConfiguration.Item.Spacer(1f))
        rows[4].add(0, KeyboardConfiguration.Item.TemplateKey(-BOTTOM_RIGHT_SYLLABLES[1].code))
        rows[4].add(KeyboardConfiguration.Item.Spacer(1f))
        return KeyboardConfiguration(rows) + TabletKeyboard.bottom()
    }
}