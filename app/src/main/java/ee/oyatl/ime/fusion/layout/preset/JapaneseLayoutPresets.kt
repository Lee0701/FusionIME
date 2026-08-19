package ee.oyatl.ime.fusion.layout.preset

import android.view.KeyEvent
import ee.oyatl.ime.fusion.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.layout.ExtKeyCode
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.layout.LayoutGodan
import ee.oyatl.ime.fusion.layout.LayoutKana
import ee.oyatl.ime.fusion.layout.LayoutKana.BOTTOM_LEFT_SYLLABLES
import ee.oyatl.ime.fusion.layout.LayoutKana.BOTTOM_RIGHT_SYLLABLES
import ee.oyatl.ime.fusion.layout.LayoutRomaji
import ee.oyatl.ime.fusion.layout.MobileKeyboard
import ee.oyatl.ime.fusion.layout.MobileKeyboardRows
import ee.oyatl.ime.fusion.layout.TabletKeyboard
import ee.oyatl.ime.fusion.layout.TabletKeyboardRows
import ee.oyatl.ime.keyboard.KeyboardConfiguration
import ee.oyatl.ime.keyboard.KeyboardTemplate
import ee.oyatl.ime.keyboard.LayoutTable
import ee.oyatl.ime.keyboard.SoftKeyCodeMapper
import kotlin.collections.plus
import kotlin.math.ceil

object JapaneseLayoutPresets {
    fun romajiQwertyCompatible(
        preset: KeyboardLayoutPreset
    ): KeyboardLayoutPreset {
        val layoutTable = preset.layoutTable + LayoutTable.fromShiftStates(LayoutRomaji.TABLE_QWERTY)
        val softKeyCodeMapper = SoftKeyCodeMapper.ByScreenMode(
            mobile = SoftKeyCodeMapper.Basic(LayoutRomaji.KEYCODE_MAP_QWERTY),
            full = SoftKeyCodeMapper.Empty
        )
        return preset.copy(
            layoutTable = layoutTable,
            softKeyCodeMapper = softKeyCodeMapper
        )
    }

    fun kana12Key(): KeyboardLayoutPreset {
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = mobileKeyboardConfiguration12Key(),
                    contentRows = LayoutKana.ROWS_12KEY
                )
            ),
            layoutTable = LayoutTable
                .fromFlick4Dirs(LayoutKana.TABLE_12KEY.mapValues { (_, list) -> list.map { it.code } })
        )
    }

    fun godan(): KeyboardLayoutPreset {
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = LayoutGodan.mobileKeyboardConfiguration(),
                    contentRows = emptyList()
                )
            ),
            layoutTable = LayoutTable.fromFlick4Dirs(LayoutGodan.TABLE)
        )
    }

    fun jis(): KeyboardLayoutPreset {
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        MobileKeyboard.numbers(),
                        MobileKeyboard.alphabetic(semicolon = true, shiftDeleteWidth = 1f),
                        MobileKeyboard.bottom(left = ExtKeyCode.KEYCODE_KANA_EQUALS, right = ExtKeyCode.KEYCODE_KANA_SLASH, dpad = true)
                    ),
                    contentRows = MobileKeyboardRows.JIS
                ),
                tablet = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        TabletKeyboard.numbers(delete = true, spacerOnDelete = false),
                        TabletKeyboard.alphabetic(semicolon = true, delete = false, spacerOnDelete = true),
                        TabletKeyboard.bottom()
                    ),
                    contentRows = TabletKeyboardRows.JIS
                )
            ),
            layoutTable = LayoutTable.fromShiftStates(LayoutExt.TABLE + LayoutKana.TABLE_JIS),
            softKeyCodeMapper = SoftKeyCodeMapper.ByScreenMode(
                mobile = SoftKeyCodeMapper.Basic(mapOf(
                    KeyEvent.KEYCODE_MINUS to ExtKeyCode.KEYCODE_KANA_MINUS,
                    KeyEvent.KEYCODE_APOSTROPHE to ExtKeyCode.KEYCODE_KANA_APOSTROPHE,
                    KeyEvent.KEYCODE_LEFT_BRACKET to ExtKeyCode.KEYCODE_KANA_VOICED_MARK
                ))
            )
        )
    }

    fun syllables(
        keys: String,
        keyLayout: KeyLayout
    ): KeyboardLayoutPreset {
        val contentRows = generateContentRows(keys, keyLayout)
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = mobileKeyboardConfigurationSyllables(contentRows),
                    contentRows = emptyList()
                ),
                tablet = KeyboardTemplate.Basic(
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
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SYM, 1.5f, true),
            KeyboardConfiguration.Item.TemplateKey(-BOTTOM_LEFT_SYLLABLES[0].code),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_LANGUAGE_SWITCH, 1f, true),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SPACE, 2f),
            KeyboardConfiguration.Item.TemplateKey(-BOTTOM_RIGHT_SYLLABLES[0].code),
            KeyboardConfiguration.Item.TemplateKey(-BOTTOM_RIGHT_SYLLABLES[1].code),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_ENTER, 1.5f, true),
            KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DEL, 1f, true)
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
        rows[0].add(KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DEL, 1f, true))
        rows[1].add(0, KeyboardConfiguration.Item.Spacer(1f))
        rows[1].add(KeyboardConfiguration.Item.Spacer(1f))
        rows[2].add(0, KeyboardConfiguration.Item.TemplateKey(-BOTTOM_RIGHT_SYLLABLES[0].code))
        rows[2].add(KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_ENTER, 1f, true))
        rows[3].add(0, KeyboardConfiguration.Item.Spacer(1f))
        rows[3].add(KeyboardConfiguration.Item.Spacer(1f))
        rows[4].add(0, KeyboardConfiguration.Item.TemplateKey(-BOTTOM_RIGHT_SYLLABLES[1].code))
        rows[4].add(KeyboardConfiguration.Item.Spacer(1f))
        return KeyboardConfiguration(rows) + TabletKeyboard.bottom()
    }

    fun mobileKeyboardConfiguration12Key(): KeyboardConfiguration {
        val rows = (0 until 4).map { mutableListOf<KeyboardConfiguration.Item>() }

        rows[0] += KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_NUM, special = true)
        rows[1] += KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DPAD_LEFT, special = true)
        rows[2] += KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_LANGUAGE_SWITCH, special = true)
        rows[3] += KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SYM, special = true)

        rows[0] += KeyboardConfiguration.Item.ContentRow(3)
        rows[1] += KeyboardConfiguration.Item.ContentRow(2)
        rows[2] += KeyboardConfiguration.Item.ContentRow(1)
        rows[3] += KeyboardConfiguration.Item.ContentRow(0)

        rows[0] += KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DEL, special = true)
        rows[1] += KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_DPAD_RIGHT, special = true)
        rows[2] += KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_SPACE, special = true)
        rows[3] += KeyboardConfiguration.Item.TemplateKey(KeyEvent.KEYCODE_ENTER, special = true)

        return KeyboardConfiguration(rows)
    }

}