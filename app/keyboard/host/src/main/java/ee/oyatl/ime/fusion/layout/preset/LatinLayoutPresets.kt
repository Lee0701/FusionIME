package ee.oyatl.ime.fusion.layout.preset

import ee.oyatl.ime.fusion.keyboard.core.HardKeyCodeMapper
import ee.oyatl.ime.fusion.keyboard.core.KeyCode
import ee.oyatl.ime.fusion.keyboard.core.KeyboardConfiguration
import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.keyboard.core.KeyboardTemplate
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.layout.LayoutLatin
import ee.oyatl.ime.fusion.layout.LayoutQwerty
import ee.oyatl.ime.fusion.layout.MobileKeyboard
import ee.oyatl.ime.fusion.layout.MobileKeyboardRows
import ee.oyatl.ime.fusion.layout.TabletKeyboard
import ee.oyatl.ime.fusion.layout.TabletKeyboardRows
import ee.oyatl.ime.fusion.layout.LatinMoreKeysTable
import ee.oyatl.ime.fusion.keyboard.core.LayoutTable
import ee.oyatl.ime.fusion.keyboard.core.MoreKeysTable
import ee.oyatl.ime.fusion.keyboard.core.SoftKeyCodeMapper

object LatinLayoutPresets {
    fun qwerty(
        semicolon: Boolean = false
    ): KeyboardLayoutPreset {
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        MobileKeyboard.numbers(),
                        MobileKeyboard.alphabetic(semicolon = semicolon),
                        MobileKeyboard.bottom()
                    ),
                    contentRows = MobileKeyboardRows.NUMBERS +
                            (if(semicolon) MobileKeyboardRows.SEMICOLON else MobileKeyboardRows.DEFAULT)
                ),
                tablet = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        TabletKeyboard.numbers(),
                        TabletKeyboard.alphabetic(semicolon = semicolon),
                        TabletKeyboard.bottom()
                    ),
                    contentRows = TabletKeyboardRows.NUMBERS +
                            (if(semicolon) TabletKeyboardRows.SEMICOLON else TabletKeyboardRows.DEFAULT)
                )
            ),
            layoutTable = LayoutTable.fromShiftStates(LayoutExt.TABLE_EXT + LayoutQwerty.TABLE_QWERTY),
            moreKeysTable = MoreKeysTable.from(LatinMoreKeysTable.DEFAULT, LatinMoreKeysTable.DEFAULT_UPPERCASE_OVERRIDE)
        )
    }

    fun spanishQwerty(): KeyboardLayoutPreset {
        val qwerty = qwerty(true)
        val layoutTable = qwerty.layoutTable + LayoutTable.fromShiftStates(LayoutLatin.TABLE_SPANISH_QWERTY)
        return qwerty.copy(
            layoutTable = layoutTable
        )
    }

    fun azerty(): KeyboardLayoutPreset {
        val qwerty = qwerty(true)
        return qwerty.copy(
            layoutTable = qwerty.layoutTable + LayoutTable.fromShiftStates(LayoutLatin.TABLE_AZERTY),
            altGraphTable = LayoutTable.fromShiftStates(LayoutLatin.TABLE_AZERTY_ALTGR),
            softKeyCodeMapper = SoftKeyCodeMapper.ByScreenMode(
                mobile = SoftKeyCodeMapper.Basic(LayoutLatin.KEYCODE_MAP_AZERTY
                        + LayoutLatin.SOFT_KEYCODE_MAP_AZERTY_NUMBERS
                        + LayoutLatin.SOFT_KEYCODE_MAP_AZERTY_MOBILE
                ),
                tablet = SoftKeyCodeMapper.Basic(LayoutLatin.KEYCODE_MAP_AZERTY
                        + LayoutLatin.SOFT_KEYCODE_MAP_AZERTY_NUMBERS
                ),
                full = SoftKeyCodeMapper.Basic(LayoutLatin.KEYCODE_MAP_AZERTY)
            ),
            hardKeyCodeMapper = HardKeyCodeMapper(LayoutLatin.KEYCODE_MAP_AZERTY)
        )
    }

    fun qwertz(): KeyboardLayoutPreset {
        val qwerty = qwerty(false)
        return qwerty.copy(
            layoutTable = qwerty.layoutTable + LayoutTable.fromShiftStates(LayoutLatin.TABLE_GERMAN_QWERTZ),
            altGraphTable = LayoutTable.fromShiftStates(LayoutLatin.TABLE_GERMAN_QWERTZ_ALTGR),
            softKeyCodeMapper = SoftKeyCodeMapper.Basic(LayoutLatin.KEYCODE_MAP_QWERTZ),
            hardKeyCodeMapper = HardKeyCodeMapper(LayoutLatin.KEYCODE_MAP_QWERTZ)
        )
    }

    fun germanQwertz(): KeyboardLayoutPreset {
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        MobileKeyboard.numbers(),
                        MobileKeyboard.alphabetic(semicolon = true),
                        MobileKeyboard.bottom()
                    ),
                    contentRows = MobileKeyboardRows.NUMBERS + MobileKeyboardRows.GERMAN
                ),
                tablet = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        TabletKeyboard.numbers(),
                        TabletKeyboard.alphabetic(semicolon = true),
                        TabletKeyboard.bottom()
                    ),
                    contentRows = TabletKeyboardRows.NUMBERS + TabletKeyboardRows.GERMAN
                )
            ),
            layoutTable = LayoutTable.fromShiftStates(LayoutExt.TABLE_EXT + LayoutQwerty.TABLE_QWERTY + LayoutLatin.TABLE_GERMAN_QWERTZ),
            altGraphTable = LayoutTable.fromShiftStates(LayoutLatin.TABLE_GERMAN_QWERTZ_ALTGR),
            moreKeysTable = MoreKeysTable.from(LatinMoreKeysTable.DEFAULT, LatinMoreKeysTable.DEFAULT_UPPERCASE_OVERRIDE),
            softKeyCodeMapper = SoftKeyCodeMapper.Basic(LayoutLatin.KEYCODE_MAP_QWERTZ + LayoutLatin.SOFT_KEYCODE_MAP_DEUTSCH_QWERTZ),
            hardKeyCodeMapper = HardKeyCodeMapper(LayoutLatin.KEYCODE_MAP_QWERTZ)
        )
    }

    fun dvorak(): KeyboardLayoutPreset {
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        MobileKeyboard.numbers(),
                        MobileKeyboard.alphabetic(semicolon = true),
                        MobileKeyboard.bottom(left = KeyCode.KEYCODE_X, right = KeyCode.KEYCODE_SLASH)
                    ),
                    contentRows = MobileKeyboardRows.NUMBERS + MobileKeyboardRows.DVORAK
                ),
                tablet = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        TabletKeyboard.numbers(),
                        TabletKeyboard.alphabetic(semicolon = true),
                        TabletKeyboard.bottom()
                    ),
                    contentRows = TabletKeyboardRows.NUMBERS + TabletKeyboardRows.DVORAK
                )
            ),
            layoutTable = LayoutTable.fromShiftStates(LayoutExt.TABLE_EXT + LayoutQwerty.TABLE_QWERTY),
            moreKeysTable = MoreKeysTable.from(LatinMoreKeysTable.DEFAULT, LatinMoreKeysTable.DEFAULT_UPPERCASE_OVERRIDE),
            softKeyCodeMapper = SoftKeyCodeMapper.Basic(LayoutLatin.KEYCODE_MAP_DVORAK)
        )
    }

    fun colemak(): KeyboardLayoutPreset {
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        MobileKeyboard.numbers(),
                        MobileKeyboard.alphabetic(semicolon = true),
                        MobileKeyboard.bottom()
                    ),
                    contentRows = MobileKeyboardRows.NUMBERS + MobileKeyboardRows.SEMICOLON
                ),
                tablet = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        TabletKeyboard.numbers(),
                        TabletKeyboard.alphabetic(semicolon = true),
                        TabletKeyboard.bottom()
                    ),
                    contentRows = TabletKeyboardRows.NUMBERS + TabletKeyboardRows.SEMICOLON
                )
            ),
            layoutTable = LayoutTable.fromShiftStates(LayoutExt.TABLE_EXT + LayoutQwerty.TABLE_QWERTY),
            moreKeysTable = MoreKeysTable.from(LatinMoreKeysTable.DEFAULT, LatinMoreKeysTable.DEFAULT_UPPERCASE_OVERRIDE),
            softKeyCodeMapper = SoftKeyCodeMapper.Basic(LayoutLatin.KEYCODE_MAP_COLEMAK)
        )
    }
}