package ee.oyatl.ime.keyboard

data class KeyboardStateSet(
    val shift: KeyboardState.Shift = KeyboardState.Shift.Released,
    val symbol: KeyboardState.Symbol = KeyboardState.Symbol.Text
)