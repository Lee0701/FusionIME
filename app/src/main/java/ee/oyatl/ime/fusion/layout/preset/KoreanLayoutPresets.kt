package ee.oyatl.ime.fusion.layout.preset

import android.view.KeyEvent
import ee.oyatl.ime.fusion.Feature
import ee.oyatl.ime.keyboard.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.layout.ExtKeyCode
import ee.oyatl.ime.fusion.layout.Hangul3Set
import ee.oyatl.ime.fusion.layout.HangulOld
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.layout.LayoutQwerty
import ee.oyatl.ime.fusion.layout.MobileKeyboard
import ee.oyatl.ime.fusion.layout.MobileKeyboardRows
import ee.oyatl.ime.fusion.layout.TabletKeyboard
import ee.oyatl.ime.fusion.layout.TabletKeyboardRows
import ee.oyatl.ime.keyboard.KeyboardConfiguration
import ee.oyatl.ime.keyboard.KeyboardTemplate
import ee.oyatl.ime.keyboard.LayoutTable
import ee.oyatl.ime.keyboard.SoftKeyCodeMapper

object KoreanLayoutPresets {
    fun ksCompatible(
        layoutTable: LayoutTable,
        numberRow: Boolean = false,
        cursorKeys: Boolean = false
    ): KeyboardLayoutPreset {
        val numberRow = Feature.NumberRow.availableInCurrentVersion && numberRow
        val cursorKeys = Feature.CursorKeys.availableInCurrentVersion && cursorKeys
        val preset = LatinLayoutPresets.qwerty(
            numberRow = numberRow,
            cursorKeys = cursorKeys
        )
        return preset.copy(
            layoutTable = preset.layoutTable + layoutTable
        )
    }

    fun threeSet390391(
        layoutTable: LayoutTable,
        softKeyCodeMapper: SoftKeyCodeMapper,
        cursorKeys: Boolean = false
    ): KeyboardLayoutPreset {
        val cursorKeys = Feature.CursorKeys.availableInCurrentVersion && cursorKeys
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        MobileKeyboard.numbers(),
                        MobileKeyboard.alphabetic(semicolon = true, shiftDeleteWidth = 1f),
                        MobileKeyboard.bottom(left = ExtKeyCode.KEYCODE_PERIOD_COMMA, right = KeyEvent.KEYCODE_SLASH, dpad = cursorKeys)
                    ),
                    contentRows = MobileKeyboardRows.NUMBERS + MobileKeyboardRows.SEMICOLON_QUOTE
                ),
                tablet = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        TabletKeyboard.numbers(delete = true),
                        TabletKeyboard.alphabetic(semicolon = true, delete = false),
                        TabletKeyboard.bottom()
                    ),
                    contentRows = TabletKeyboardRows.NUMBERS + TabletKeyboardRows.SEMICOLON_QUOTE_SLASH
                )
            ),
            layoutTable = layoutTable,
            softKeyCodeMapper = softKeyCodeMapper
        )
    }

    fun threeSet393(
        cursorKeys: Boolean = false
    ): KeyboardLayoutPreset {
        val cursorKeys = Feature.CursorKeys.availableInCurrentVersion && cursorKeys
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        MobileKeyboard.numbers(),
                        MobileKeyboard.alphabetic(semicolon = true, shiftDeleteWidth = 1f),
                        MobileKeyboard.bottom(ExtKeyCode.KEYCODE_PERIOD_COMMA, KeyEvent.KEYCODE_SLASH, dpad = cursorKeys)
                    ),
                    contentRows = MobileKeyboardRows.NUMBERS + MobileKeyboardRows.SEMICOLON_QUOTE
                ),
                tablet = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        TabletKeyboard.numbers(delete = true),
                        TabletKeyboard.alphabetic(semicolon = true, delete = false, spacerOnDelete = false),
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