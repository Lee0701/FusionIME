package ee.oyatl.ime.fusion.layout.preset

import ee.oyatl.ime.fusion.keyboard.core.ExtKeyCode
import ee.oyatl.ime.fusion.keyboard.core.KeyCode
import ee.oyatl.ime.fusion.keyboard.core.KeyboardConfiguration
import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.keyboard.core.KeyboardTemplate
import ee.oyatl.ime.fusion.layout.LayoutCangjie
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.layout.LayoutQwerty
import ee.oyatl.ime.fusion.layout.MobileKeyboard
import ee.oyatl.ime.fusion.layout.MobileKeyboardRows
import ee.oyatl.ime.fusion.layout.TabletKeyboard
import ee.oyatl.ime.fusion.layout.TabletKeyboardRows
import ee.oyatl.ime.fusion.keyboard.core.LayoutTable
import kotlin.collections.plus

object CangjieLayoutPresets {
    fun cangjie(): KeyboardLayoutPreset {
        val qwerty = LatinLayoutPresets.qwerty(false)
        val layoutTable = qwerty.layoutTable + LayoutTable.fromShiftStates(LayoutExt.TABLE_CHINESE + LayoutCangjie.TABLE_QWERTY)
        return qwerty.copy(
            layoutTable = layoutTable
        )
    }

    fun dayi3(): KeyboardLayoutPreset {
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        MobileKeyboard.numbers(),
                        MobileKeyboard.alphabetic(semicolon = true, shiftDeleteWidth = 1f, shift = false),
                        MobileKeyboard.bottom(left = ExtKeyCode.KEYCODE_PERIOD_COMMA, right = KeyCode.KEYCODE_SLASH)
                    ),
                    contentRows = MobileKeyboardRows.NUMBERS + MobileKeyboardRows.HALF_GRID
                ),
                tablet = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        TabletKeyboard.numbers(),
                        TabletKeyboard.alphabetic(semicolon = true, rightShift = false),
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