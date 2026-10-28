package ee.oyatl.ime.fusion.keyboard.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface KeyboardTemplate {
    @SerialName("basic")
    @Serializable
    data class Basic(
        val configuration: KeyboardConfiguration,
        val contentRows: List<String>
    ): KeyboardTemplate

    @SerialName("by-screen-mode")
    @Serializable
    data class ByScreenMode(
        val mobile: KeyboardTemplate,
        val tablet: KeyboardTemplate = mobile,
        val full: KeyboardTemplate = tablet
    ): KeyboardTemplate
}