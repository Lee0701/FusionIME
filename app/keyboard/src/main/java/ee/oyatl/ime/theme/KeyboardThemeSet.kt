package ee.oyatl.ime.theme

enum class KeyboardThemeSet(
    val light: KeyboardTheme,
    val dark: KeyboardTheme
) {
    Static(KeyboardThemes.StaticLight, KeyboardThemes.StaticDark)
}