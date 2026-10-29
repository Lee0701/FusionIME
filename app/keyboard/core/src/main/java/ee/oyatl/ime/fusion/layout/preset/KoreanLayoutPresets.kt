package ee.oyatl.ime.fusion.layout.preset

import ee.oyatl.ime.fusion.hangul.JamoCombinationTable
import ee.oyatl.ime.fusion.keyboard.core.ExtKeyCode
import ee.oyatl.ime.fusion.layout.Hangul3Set
import ee.oyatl.ime.fusion.layout.HangulOld
import ee.oyatl.ime.fusion.keyboard.core.KeyCode
import ee.oyatl.ime.fusion.keyboard.core.KeyboardConfiguration
import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.keyboard.core.KeyboardTemplate
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.layout.LayoutQwerty
import ee.oyatl.ime.fusion.layout.MobileKeyboard
import ee.oyatl.ime.fusion.layout.MobileKeyboardRows
import ee.oyatl.ime.fusion.layout.TabletKeyboard
import ee.oyatl.ime.fusion.layout.TabletKeyboardRows
import ee.oyatl.ime.fusion.keyboard.core.LayoutTable
import ee.oyatl.ime.fusion.keyboard.core.SoftKeyCodeMapper
import ee.oyatl.ime.fusion.layout.Hangul2Set

object KoreanLayoutPresets {
    fun hangul2SetKS(): KeyboardLayoutPreset {
        val layoutTable = LayoutTable.fromShiftStates(LayoutExt.TABLE_EXT + LayoutQwerty.TABLE_QWERTY + Hangul2Set.TABLE_KS)
        val jamoCombinationTable = JamoCombinationTable.from(Hangul2Set.COMBINATION_KS)
        return hangulKSCompatible().copy(
            layoutTable = layoutTable,
            extra = KeyboardLayoutPreset.Extra.Hangul(
                jamoCombinationTable = jamoCombinationTable
            )
        )
    }

    fun hangulOld2Set(): KeyboardLayoutPreset {
        val layoutTable = LayoutTable.fromShiftStates(LayoutExt.TABLE_EXT + LayoutQwerty.TABLE_QWERTY + HangulOld.TABLE_OLD_2SET)
        val jamoCombinationTable = JamoCombinationTable.from(HangulOld.COMBINATION_FULL)
        return hangulKSCompatible().copy(
            layoutTable = layoutTable,
            extra = KeyboardLayoutPreset.Extra.Hangul(
                jamoCombinationTable = jamoCombinationTable
            )
        )
    }

    private fun hangulKSCompatible(): KeyboardLayoutPreset {
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
            )
        )
    }

    fun hangul3Set390(): KeyboardLayoutPreset {
        val layoutTable = LayoutTable.fromShiftStates(Hangul3Set.TABLE_390)
        val softKeyCodeMapper = SoftKeyCodeMapper.ByScreenMode(
            mobile = SoftKeyCodeMapper.Basic(Hangul3Set.KEYCODE_MAP_390_MOBILE),
            tablet = SoftKeyCodeMapper.Empty
        )
        val jamoCombinationTable = JamoCombinationTable.from(Hangul3Set.COMBINATION_390_391)
        return hangul3Set390391().copy(
            layoutTable = layoutTable,
            softKeyCodeMapper = softKeyCodeMapper,
            extra = KeyboardLayoutPreset.Extra.Hangul(
                jamoCombinationTable = jamoCombinationTable
            )
        )
    }

    fun hangul3Set391(): KeyboardLayoutPreset {
        val layoutTable = LayoutTable.fromShiftStates(Hangul3Set.TABLE_391)
        val jamoCombinationTable = JamoCombinationTable.from(Hangul3Set.COMBINATION_390_391)
        return hangul3Set390391().copy(
            layoutTable = layoutTable,
            extra = KeyboardLayoutPreset.Extra.Hangul(
                jamoCombinationTable = jamoCombinationTable
            )
        )
    }

    fun hangul3Set391Strict(): KeyboardLayoutPreset {
        val layoutTable = LayoutTable.fromShiftStates(Hangul3Set.TABLE_391 + Hangul3Set.TABLE_391_STRICT)
        val jamoCombinationTable = JamoCombinationTable.from(Hangul3Set.COMBINATION_391_STRICT)
        return hangul3Set390391().copy(
            layoutTable = layoutTable,
            extra = KeyboardLayoutPreset.Extra.Hangul(
                jamoCombinationTable = jamoCombinationTable
            )
        )
    }

    private fun hangul3Set390391(): KeyboardLayoutPreset {
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
            )
        )
    }

    fun hangul3Set393(): KeyboardLayoutPreset {
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
                .fromShiftStates(LayoutExt.TABLE_EXT + LayoutQwerty.TABLE_QWERTY + HangulOld.TABLE_OLD_393),
            softKeyCodeMapper = SoftKeyCodeMapper.ByScreenMode(
                mobile = SoftKeyCodeMapper.Basic(Hangul3Set.KEYCODE_MAP_393_MOBILE),
                tablet = SoftKeyCodeMapper.Empty
            ),
            extra = KeyboardLayoutPreset.Extra.Hangul(
                jamoCombinationTable = JamoCombinationTable.from(HangulOld.COMBINATION_FULL)
            )
        )
    }
}