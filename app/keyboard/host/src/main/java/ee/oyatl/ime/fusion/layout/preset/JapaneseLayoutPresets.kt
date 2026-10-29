package ee.oyatl.ime.fusion.layout.preset

import ee.oyatl.ime.fusion.keyboard.core.ExtKeyCode
import ee.oyatl.ime.fusion.keyboard.core.KeyCode
import ee.oyatl.ime.fusion.keyboard.core.KeyLabels
import ee.oyatl.ime.fusion.keyboard.core.KeyboardConfiguration
import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.keyboard.core.KeyboardTemplate
import ee.oyatl.ime.fusion.keyboard.core.KeyboardTemplate.Basic
import ee.oyatl.ime.fusion.keyboard.core.LayoutTable
import ee.oyatl.ime.fusion.keyboard.core.SoftKeyCodeMapper
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.layout.LayoutGodan
import ee.oyatl.ime.fusion.layout.LayoutKana
import ee.oyatl.ime.fusion.layout.LayoutRomaji
import ee.oyatl.ime.fusion.layout.MobileKeyboard
import ee.oyatl.ime.fusion.layout.MobileKeyboardRows
import ee.oyatl.ime.fusion.layout.TabletKeyboard
import ee.oyatl.ime.fusion.layout.TabletKeyboardRows

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
                mobile = Basic(
                    configuration = mobileKeyboardConfiguration12Key(),
                    contentRows = LayoutKana.ROWS_12KEY
                )
            ),
            layoutTable = LayoutTable
                .fromFlick4Dirs(LayoutKana.TABLE_12KEY.mapValues { (_, list) -> list.map { it.code } }),
            keyLabels = KeyLabels(LayoutKana.LABELS_12KEY)
        )
    }

    fun godan(): KeyboardLayoutPreset {
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = Basic(
                    configuration = LayoutGodan.mobileKeyboardConfiguration(),
                    contentRows = emptyList()
                )
            ),
            layoutTable = LayoutTable.fromFlick4Dirs(LayoutGodan.TABLE),
            keyLabels = KeyLabels(LayoutGodan.LABELS)
        )
    }

    fun jis(): KeyboardLayoutPreset {
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = Basic(
                    configuration = KeyboardConfiguration(
                        MobileKeyboard.numbers(true),
                        MobileKeyboard.alphabetic(semicolon = true, shiftDeleteWidth = 1f),
                        MobileKeyboard.bottom(left = ExtKeyCode.KEYCODE_KANA_EQUALS, right = ExtKeyCode.KEYCODE_KANA_SLASH)
                    ),
                    contentRows = MobileKeyboardRows.JIS
                ),
                tablet = Basic(
                    configuration = KeyboardConfiguration(
                        TabletKeyboard.numbers(true),
                        TabletKeyboard.alphabetic(semicolon = true),
                        TabletKeyboard.bottom()
                    ),
                    contentRows = TabletKeyboardRows.JIS
                )
            ),
            layoutTable = LayoutTable.fromShiftStates(LayoutExt.TABLE_EXT + LayoutKana.TABLE_JIS),
            softKeyCodeMapper = SoftKeyCodeMapper.ByScreenMode(
                mobile = SoftKeyCodeMapper.Basic(mapOf(
                    KeyCode.KEYCODE_MINUS to ExtKeyCode.KEYCODE_KANA_MINUS,
                    KeyCode.KEYCODE_APOSTROPHE to ExtKeyCode.KEYCODE_KANA_APOSTROPHE,
                    KeyCode.KEYCODE_LEFT_BRACKET to ExtKeyCode.KEYCODE_KANA_VOICED_MARK
                ))
            )
        )
    }

    fun mobileKeyboardConfiguration12Key(): KeyboardConfiguration {
        val rows = (0 until 4).map { mutableListOf<KeyboardConfiguration.Item>() }

        rows[0] += KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_NUM, special = true)
        rows[1] += KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_DPAD_LEFT, special = true)
        rows[2] += KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_LANGUAGE_SWITCH, special = true)
        rows[3] += KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_SYM, special = true)

        rows[0] += KeyboardConfiguration.Item.ContentRow(3)
        rows[1] += KeyboardConfiguration.Item.ContentRow(2)
        rows[2] += KeyboardConfiguration.Item.ContentRow(1)
        rows[3] += KeyboardConfiguration.Item.ContentRow(0)

        rows[0] += KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_DEL, special = true)
        rows[1] += KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_DPAD_RIGHT, special = true)
        rows[2] += KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_SPACE, special = true)
        rows[3] += KeyboardConfiguration.Item.TemplateKey(KeyCode.KEYCODE_ENTER, special = true)

        return KeyboardConfiguration(rows)
    }

}