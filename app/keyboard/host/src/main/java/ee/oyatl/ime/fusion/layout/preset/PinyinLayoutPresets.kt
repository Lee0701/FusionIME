package ee.oyatl.ime.fusion.layout.preset

import ee.oyatl.ime.fusion.keyboard.core.table.KeyCodeMapper
import ee.oyatl.ime.fusion.keyboard.core.preset.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.layout.LayoutExt
import ee.oyatl.ime.fusion.layout.LayoutPinyin
import ee.oyatl.ime.fusion.keyboard.core.table.LayoutTable

object PinyinLayoutPresets {
    fun pinyin(
        preset: KeyboardLayoutPreset
    ): KeyboardLayoutPreset {
        val softKeyCodeMapper = KeyCodeMapper(LayoutPinyin.KEYCODE_MAP_PINYIN)
        val layoutTable = preset.layoutTable + LayoutTable.fromShiftStates(LayoutExt.TABLE_CHINESE)
        return preset.copy(
            layoutTable = layoutTable,
            softKeyCodeMapper = KeyCodeMapper.ByScreenMode(softKeyCodeMapper)
        )
    }
}