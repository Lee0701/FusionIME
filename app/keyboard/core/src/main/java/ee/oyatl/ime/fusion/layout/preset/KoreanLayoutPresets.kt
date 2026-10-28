package ee.oyatl.ime.fusion.layout.preset

import ee.oyatl.ime.fusion.keyboard.core.ExtKeyCode
import ee.oyatl.ime.fusion.layout.Hangul3Set
import ee.oyatl.ime.fusion.layout.HangulOld
import ee.oyatl.ime.fusion.keyboard.core.KeyCode
import ee.oyatl.ime.fusion.keyboard.core.KeyboardConfiguration
import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.keyboard.core.KeyboardTemplate
import ee.oyatl.ime.fusion.layout.LatinMoreKeysTable
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.layout.LayoutQwerty
import ee.oyatl.ime.fusion.layout.MobileKeyboard
import ee.oyatl.ime.fusion.layout.MobileKeyboardRows
import ee.oyatl.ime.fusion.layout.TabletKeyboard
import ee.oyatl.ime.fusion.layout.TabletKeyboardRows
import ee.oyatl.ime.fusion.keyboard.core.LayoutTable
import ee.oyatl.ime.fusion.keyboard.core.LongPressTable
import ee.oyatl.ime.fusion.keyboard.core.SoftKeyCodeMapper

object KoreanLayoutPresets {
    fun ksCompatible(
        layoutTable: LayoutTable
    ): KeyboardLayoutPreset {
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        MobileKeyboard.numbers(),
                        MobileKeyboard.alphabetic(semicolon = false),
                        MobileKeyboard.bottom()
                    ),
                    contentRows = MobileKeyboardRows.NUMBERS + MobileKeyboardRows.KS
                ),
                tablet = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        TabletKeyboard.numbers(),
                        TabletKeyboard.alphabetic(semicolon = false),
                        TabletKeyboard.bottom()
                    ),
                    contentRows = TabletKeyboardRows.NUMBERS + TabletKeyboardRows.KS
                )
            ),
            layoutTable = LayoutTable.fromShiftStates(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY) + layoutTable,
            longPressTable = LongPressTable.from(LatinMoreKeysTable.DEFAULT, LatinMoreKeysTable.DEFAULT_UPPERCASE_OVERRIDE)
        )
    }

    fun threeSet390391(
        layoutTable: LayoutTable,
        softKeyCodeMapper: SoftKeyCodeMapper
    ): KeyboardLayoutPreset {
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        MobileKeyboard.numbers(),
                        MobileKeyboard.alphabetic(semicolon = true, shiftDeleteWidth = 1f),
                        MobileKeyboard.bottom(left = ExtKeyCode.KEYCODE_PERIOD_COMMA, right = KeyCode.KEYCODE_SLASH)
                    ),
                    contentRows = MobileKeyboardRows.NUMBERS + MobileKeyboardRows.SEMICOLON_QUOTE
                ),
                tablet = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        TabletKeyboard.numbers(),
                        TabletKeyboard.alphabetic(semicolon = true),
                        TabletKeyboard.bottom()
                    ),
                    contentRows = TabletKeyboardRows.NUMBERS + TabletKeyboardRows.SEMICOLON_QUOTE_SLASH
                )
            ),
            layoutTable = layoutTable,
            softKeyCodeMapper = softKeyCodeMapper
        )
    }

    fun threeSet393(): KeyboardLayoutPreset {
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        MobileKeyboard.numbers(),
                        MobileKeyboard.alphabetic(semicolon = true, shiftDeleteWidth = 1f),
                        MobileKeyboard.bottom(ExtKeyCode.KEYCODE_PERIOD_COMMA, KeyCode.KEYCODE_SLASH)
                    ),
                    contentRows = MobileKeyboardRows.NUMBERS + MobileKeyboardRows.SEMICOLON_QUOTE
                ),
                tablet = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        TabletKeyboard.numbers(),
                        TabletKeyboard.alphabetic(semicolon = true, fillDeleteSpace = false),
                        TabletKeyboard.bottom()
                    ),
                    contentRows = TabletKeyboardRows.HANGUL_OLD_393
                )
            ),
            layoutTable = LayoutTable
                .fromShiftStates(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + HangulOld.TABLE_OLD_393),
            softKeyCodeMapper = SoftKeyCodeMapper.ByScreenMode(
                mobile = SoftKeyCodeMapper.Basic(Hangul3Set.KEYCODE_MAP_393_MOBILE),
                tablet = SoftKeyCodeMapper.Empty
            )
        )
    }
}