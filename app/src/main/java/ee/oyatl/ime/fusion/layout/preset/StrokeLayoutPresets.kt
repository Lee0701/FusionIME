package ee.oyatl.ime.fusion.layout.preset

import ee.oyatl.ime.fusion.layout.LayoutStroke
import ee.oyatl.ime.keyboard.KeyboardLayoutPreset
import ee.oyatl.ime.keyboard.KeyboardTemplate
import ee.oyatl.ime.keyboard.LayoutTable

object StrokeLayoutPresets {
    fun stroke12Key(): KeyboardLayoutPreset {
        return KeyboardLayoutPreset(
            keyboardTemplate = KeyboardTemplate.ByScreenMode(
                mobile = KeyboardTemplate.Basic(
                    configuration = JapaneseLayoutPresets.mobileKeyboardConfiguration12Key(),
                    contentRows = LayoutStroke.ROWS_12KEY
                )
            ),
            layoutTable = LayoutTable.fromShiftStates(LayoutStroke.TABLE_12KEY)
        )
    }
}
