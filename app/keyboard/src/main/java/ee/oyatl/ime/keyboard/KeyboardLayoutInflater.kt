package ee.oyatl.ime.keyboard

import ee.oyatl.ime.fusion.keyboard.core.KeyCodeMapper
import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayout
import ee.oyatl.ime.fusion.keyboard.core.KeyboardLayoutPreset
import ee.oyatl.ime.fusion.keyboard.core.KeyboardParams
import ee.oyatl.ime.fusion.keyboard.core.KeyboardState

object KeyboardLayoutInflater {
    fun inflate(
        preset: KeyboardLayoutPreset,
        params: KeyboardParams
    ): KeyboardLayout {
        val keyboard = KeyboardInflater.inflate(preset.keyboardTemplate, params)
        val softKeyCodeMapper = when(params.screenMode) {
            KeyboardState.ScreenMode.Mobile -> preset.softKeyCodeMapper.mobile
            KeyboardState.ScreenMode.Tablet -> preset.softKeyCodeMapper.tablet
            KeyboardState.ScreenMode.Full -> preset.softKeyCodeMapper.full
            else -> KeyCodeMapper()
        }
        return KeyboardLayout(
            params = params,
            keyboard = keyboard,
            layoutTable = preset.layoutTable,
            altGraphTable = preset.altGraphTable,
            moreKeysTable = preset.moreKeysTable,
            softKeyCodeMapper = softKeyCodeMapper,
            hardKeyCodeMapper = preset.hardKeyCodeMapper,
            keyLabels = preset.keyLabels,
            extra = preset.extra
        )
    }
}