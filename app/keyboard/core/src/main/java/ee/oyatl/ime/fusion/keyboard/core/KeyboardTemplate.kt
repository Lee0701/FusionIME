package ee.oyatl.ime.fusion.keyboard.core

sealed interface KeyboardTemplate {
    data class Basic(
        val configuration: KeyboardConfiguration,
        val contentRows: List<String>
    ): KeyboardTemplate {
    }

    data class ByScreenMode(
        val mobile: KeyboardTemplate,
        val tablet: KeyboardTemplate = mobile,
        val full: KeyboardTemplate = tablet
    ): KeyboardTemplate
}