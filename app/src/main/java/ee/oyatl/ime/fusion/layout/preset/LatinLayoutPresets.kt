package ee.oyatl.ime.fusion.layout.preset

import android.view.KeyEvent
import ee.oyatl.ime.fusion.Feature
import ee.oyatl.ime.keyboard.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.layout.LayoutLatin
import ee.oyatl.ime.fusion.layout.LayoutQwerty
import ee.oyatl.ime.fusion.layout.MobileKeyboard
import ee.oyatl.ime.fusion.layout.MobileKeyboardRows
import ee.oyatl.ime.fusion.layout.TabletKeyboard
import ee.oyatl.ime.fusion.layout.TabletKeyboardRows
import ee.oyatl.ime.keyboard.KeyboardConfiguration
import ee.oyatl.ime.keyboard.KeyboardTemplate
import ee.oyatl.ime.keyboard.LayoutTable
import ee.oyatl.ime.keyboard.SoftKeyCodeMapper

object LatinLayoutPresets {
    fun qwerty(
        semicolon: Boolean = false,
        numberRow: Boolean = false,
        cursorKeys: Boolean = false
    ): KeyboardLayoutPreset {
        val numberRow = Feature.NumberRow.availableInCurrentVersion && numberRow
        val cursorKeys = Feature.CursorKeys.availableInCurrentVersion && cursorKeys
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        if(numberRow) MobileKeyboard.numbers() else KeyboardConfiguration(),
                        MobileKeyboard.alphabetic(semicolon = semicolon),
                        MobileKeyboard.bottom(dpad = cursorKeys)
                    ),
                    contentRows = (if(numberRow) MobileKeyboardRows.NUMBERS else listOf()) +
                            (if(semicolon) MobileKeyboardRows.SEMICOLON else MobileKeyboardRows.DEFAULT)
                ),
                tablet = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        if(numberRow) TabletKeyboard.numbers(delete = true) else KeyboardConfiguration(),
                        TabletKeyboard.alphabetic(semicolon = semicolon, delete = !numberRow),
                        TabletKeyboard.bottom()
                    ),
                    contentRows = (if(numberRow) TabletKeyboardRows.NUMBERS else listOf()) +
                            (if(semicolon) TabletKeyboardRows.SEMICOLON else TabletKeyboardRows.DEFAULT)
                )
            ),
            layoutTable = LayoutTable
                .fromShiftStates(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY)
        )
    }

    fun dvorak(
        numberRow: Boolean = false,
        cursorKeys: Boolean = false
    ): KeyboardLayoutPreset {
        val numberRow = Feature.NumberRow.availableInCurrentVersion && numberRow
        val cursorKeys = Feature.CursorKeys.availableInCurrentVersion && cursorKeys
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        if(numberRow) MobileKeyboard.numbers() else KeyboardConfiguration(),
                        MobileKeyboard.alphabetic(semicolon = true),
                        MobileKeyboard.bottom(left = KeyEvent.KEYCODE_X, right = KeyEvent.KEYCODE_SLASH, dpad = cursorKeys)
                    ),
                    contentRows = (if(numberRow) MobileKeyboardRows.NUMBERS else listOf()) + MobileKeyboardRows.DVORAK
                ),
                tablet = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        if(numberRow) TabletKeyboard.numbers(delete = true) else KeyboardConfiguration(),
                        TabletKeyboard.alphabetic(semicolon = true, delete = !numberRow),
                        TabletKeyboard.bottom()
                    ),
                    contentRows = (if(numberRow) TabletKeyboardRows.NUMBERS else listOf()) + TabletKeyboardRows.DVORAK
                )
            ),
            layoutTable = LayoutTable
                .fromShiftStates(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY),
            softKeyCodeMapper = SoftKeyCodeMapper.Basic(LayoutLatin.KEYCODE_MAP_DVORAK)
        )
    }

    fun colemak(
        numberRow: Boolean = false,
        cursorKeys: Boolean = false
    ): KeyboardLayoutPreset {
        val numberRow = Feature.NumberRow.availableInCurrentVersion && numberRow
        val cursorKeys = Feature.CursorKeys.availableInCurrentVersion && cursorKeys
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        if(numberRow) MobileKeyboard.numbers() else KeyboardConfiguration(),
                        MobileKeyboard.alphabetic(semicolon = true),
                        MobileKeyboard.bottom(dpad = cursorKeys)
                    ),
                    contentRows = (if(numberRow) MobileKeyboardRows.NUMBERS else listOf()) + MobileKeyboardRows.SEMICOLON
                ),
                tablet = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        if(numberRow) TabletKeyboard.numbers(delete = true) else KeyboardConfiguration(),
                        TabletKeyboard.alphabetic(semicolon = true, delete = !numberRow),
                        TabletKeyboard.bottom()
                    ),
                    contentRows = (if(numberRow) TabletKeyboardRows.NUMBERS else listOf()) + TabletKeyboardRows.SEMICOLON
                )
            ),
            layoutTable = LayoutTable
                .fromShiftStates(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY),
            softKeyCodeMapper = SoftKeyCodeMapper.Basic(LayoutLatin.KEYCODE_MAP_COLEMAK)
        )
    }
}