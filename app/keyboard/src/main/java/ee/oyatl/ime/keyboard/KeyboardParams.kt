package ee.oyatl.ime.keyboard

data class KeyboardParams(
    val screenMode: KeyboardState.ScreenMode,
    val height: Int,
    val soundFeedback: Boolean,
    val hapticFeedback: Boolean,
    val soundVolume: Float,
    val vibrationDuration: Long,
    val shiftLockDelay: Int,
    val shiftAutoRelease: Boolean,
    val repeatDelay: Int,
    val repeatInterval: Int,
    val previewPopups: Boolean
)
