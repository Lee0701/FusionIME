package ee.oyatl.ime.fusion.keyboard.core

import kotlinx.serialization.Serializable

sealed interface KeyboardState {
    @Serializable
    enum class Shift: KeyboardState {
        Released, Pressed, Locked, AutoShifted
    }
    @Serializable
    enum class Symbol: KeyboardState {
        Text, Symbol, Number
    }
    @Serializable
    enum class ScreenMode: KeyboardState {
        Mobile, Tablet, Full, MoreKeys
    }
}