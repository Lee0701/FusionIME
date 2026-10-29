package ee.oyatl.ime.fusion.layout.preset

import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.keyboard.core.LayoutTable

object VietLayoutPresets {
    fun chuQuocNgu(
        preset: KeyboardLayoutPreset,
        isHanNom: Boolean
    ): KeyboardLayoutPreset {
        val additionalTable = LayoutTable.fromShiftStates(
            if(isHanNom) LayoutExt.TABLE_HAN_NOM
            else mapOf()
        )
        val layoutTable = preset.layoutTable + additionalTable
        return preset.copy(
            layoutTable = layoutTable
        )
    }
}