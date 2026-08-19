package ee.oyatl.ime.fusion.layout.preset

import android.view.KeyEvent
import ee.oyatl.ime.fusion.Feature
import ee.oyatl.ime.fusion.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.layout.ExtKeyCode
import ee.oyatl.ime.fusion.layout.LayoutCangjie
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.layout.LayoutQwerty
import ee.oyatl.ime.fusion.layout.MobileKeyboard
import ee.oyatl.ime.fusion.layout.MobileKeyboardRows
import ee.oyatl.ime.fusion.layout.TabletKeyboard
import ee.oyatl.ime.fusion.layout.TabletKeyboardRows
import ee.oyatl.ime.keyboard.KeyboardConfiguration
import ee.oyatl.ime.keyboard.KeyboardTemplate
import ee.oyatl.ime.keyboard.LayoutTable
import kotlin.collections.plus

object CangjieLayoutPresets {
    fun cangjie(
        numberRow: Boolean = false,
        cursorKeys: Boolean = false
    ): KeyboardLayoutPreset {
        val qwerty = LatinLayoutPresets.qwerty(numberRow, cursorKeys)
        val layoutTable = qwerty.layoutTable + LayoutTable.fromShiftStates(LayoutExt.TABLE_CHINESE + LayoutCangjie.TABLE_QWERTY)
        return qwerty.copy(
            layoutTable = layoutTable
        )
    }

    fun dayi3(
        cursorKeys: Boolean = false
    ): KeyboardLayoutPreset {
        val cursorKeys = Feature.CursorKeys.availableInCurrentVersion && cursorKeys
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        MobileKeyboard.numbers(),
                        MobileKeyboard.alphabetic(semicolon = true, shiftDeleteWidth = 1f, shift = false),
                        MobileKeyboard.bottom(left = ExtKeyCode.KEYCODE_PERIOD_COMMA, right = KeyEvent.KEYCODE_SLASH, dpad = cursorKeys)
                    ),
                    contentRows = MobileKeyboardRows.NUMBERS + MobileKeyboardRows.HALF_GRID
                ),
                tablet = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        TabletKeyboard.numbers(delete = true),
                        TabletKeyboard.alphabetic(semicolon = true, rightShift = false, delete = false, spacerOnDelete = true),
                        TabletKeyboard.bottom()
                    ),
                    contentRows = TabletKeyboardRows.NUMBERS + TabletKeyboardRows.SEMICOLON_SLASH
                )
            ),
            layoutTable = LayoutTable
                .fromShiftStates(LayoutExt.TABLE + LayoutQwerty.TABLE_QWERTY + LayoutExt.TABLE_CHINESE + LayoutCangjie.TABLE_DAYI3)
        )
    }
}