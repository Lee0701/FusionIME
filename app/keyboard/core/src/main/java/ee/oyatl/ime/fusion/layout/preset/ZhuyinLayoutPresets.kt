package ee.oyatl.ime.fusion.layout.preset

import ee.oyatl.ime.fusion.keyboard.core.KeyCode
import ee.oyatl.ime.fusion.keyboard.core.KeyboardConfiguration
import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.keyboard.core.KeyboardTemplate
import ee.oyatl.ime.fusion.keyboard.core.LayoutTable
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.layout.LayoutQwerty
import ee.oyatl.ime.fusion.layout.LayoutZhuyin
import ee.oyatl.ime.fusion.layout.MobileKeyboard
import ee.oyatl.ime.fusion.layout.MobileKeyboardRows
import ee.oyatl.ime.fusion.layout.TabletKeyboard
import ee.oyatl.ime.fusion.layout.TabletKeyboardRows

object ZhuyinLayoutPresets {
    fun bopomofo(): KeyboardLayoutPreset {
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        MobileKeyboard.numbers(),
                        MobileKeyboard.alphabetic(semicolon = true, shiftDeleteWidth = 1f, shift = false),
                        MobileKeyboard.bottom(left = KeyCode.KEYCODE_MINUS, right = KeyCode.KEYCODE_SLASH)
                    ),
                    contentRows = MobileKeyboardRows.NUMBERS + MobileKeyboardRows.HALF_GRID
                ),
                tablet = KeyboardTemplate.Basic(
                    configuration = KeyboardConfiguration(
                        TabletKeyboard.numbers(),
                        TabletKeyboard.alphabetic(semicolon = true, rightShift = false, fillDeleteSpace = false),
                        TabletKeyboard.bottom()
                    ),
                    contentRows = TabletKeyboardRows.NUMBERS + TabletKeyboardRows.SEMICOLON_SLASH_MINUS
                )
            ),
            layoutTable = LayoutTable
                .fromShiftStates(LayoutExt.TABLE_EXT + LayoutQwerty.TABLE_QWERTY + LayoutExt.TABLE_CHINESE + LayoutZhuyin.TABLE)
        )
    }
}