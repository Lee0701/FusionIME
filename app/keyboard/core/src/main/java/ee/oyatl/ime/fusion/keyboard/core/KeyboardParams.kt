package ee.oyatl.ime.fusion.keyboard.core

data class KeyboardParams(
    val screenMode: KeyboardState.ScreenMode,
    val height: Int,
    val splitWidth: Int,
    val soundFeedback: Boolean,
    val hapticFeedback: Boolean,
    val soundVolume: Float,
    val vibrationDuration: Long,
    val touchMode: TouchMode,
    val touchCorrection: Boolean,
    val flickSensitivity: Int,
    val longPressDelay: Int,
    val shiftLockDelay: Int,
    val shiftAutoRelease: Boolean,
    val repeatDelay: Int,
    val repeatInterval: Int,
    val previewPopups: Boolean,
    val extra: Map<String, String>
)
