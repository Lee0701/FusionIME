package ee.oyatl.ime.keyboard

sealed interface KeyboardState {
    enum class Shift: KeyboardState {
        Released, Pressed, Locked
    }
    enum class Symbol: KeyboardState {
        Text, Symbol, Number
    }
}