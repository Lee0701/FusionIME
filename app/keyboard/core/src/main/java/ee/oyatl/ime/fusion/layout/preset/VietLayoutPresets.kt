package ee.oyatl.ime.fusion.layout.preset

import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.keyboard.core.LayoutTable

object VietLayoutPresets {
    fun qwerty(
        isHanNom: Boolean
    ): KeyboardLayoutPreset {
        val qwerty = LatinLayoutPresets.qwerty(false)
        val additionalTable = LayoutTable.fromShiftStates(
            if(isHanNom) LayoutExt.TABLE_HAN_NOM
            else mapOf()
        )
        val layoutTable = qwerty.layoutTable + additionalTable
        return qwerty.copy(
            layoutTable = layoutTable
        )
    }
}