package ee.oyatl.ime.fusion.layout.preset

import ee.oyatl.ime.fusion.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.layout.LayoutPinyin
import ee.oyatl.ime.keyboard.LayoutTable
import ee.oyatl.ime.keyboard.SoftKeyCodeMapper

object PinyinLayoutPresets {
    fun pinyin(
        preset: KeyboardLayoutPreset
    ): KeyboardLayoutPreset {
        val softKeyCodeMapper = SoftKeyCodeMapper.Basic(LayoutPinyin.KEYCODE_MAP_PINYIN)
        val layoutTable = preset.layoutTable + LayoutTable.fromShiftStates(LayoutExt.TABLE_CHINESE)
        return preset.copy(
            layoutTable = layoutTable,
            softKeyCodeMapper = softKeyCodeMapper
        )
    }
}