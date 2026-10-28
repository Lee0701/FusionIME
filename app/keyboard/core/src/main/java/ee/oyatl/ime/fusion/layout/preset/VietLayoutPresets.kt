package ee.oyatl.ime.fusion.layout.preset

import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.keyboard.core.LayoutTable

object VietLayoutPresets {
    fun qwerty(
        numberRow: Boolean,
        cursorKeys: Boolean,
        isHanNom: Boolean
    ): KeyboardLayoutPreset {
        val qwerty = LatinLayoutPresets.qwerty(false, numberRow, cursorKeys)
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