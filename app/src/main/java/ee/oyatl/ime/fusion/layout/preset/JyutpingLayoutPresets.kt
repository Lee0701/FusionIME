package ee.oyatl.ime.fusion.layout.preset

import ee.oyatl.ime.keyboard.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.keyboard.LayoutTable

object JyutpingLayoutPresets {
    fun jyutping(
        preset: KeyboardLayoutPreset
    ): KeyboardLayoutPreset {
        val layoutTable = preset.layoutTable + LayoutTable.fromShiftStates(LayoutExt.TABLE_CHINESE)
        return preset.copy(
            layoutTable = layoutTable
        )
    }
}