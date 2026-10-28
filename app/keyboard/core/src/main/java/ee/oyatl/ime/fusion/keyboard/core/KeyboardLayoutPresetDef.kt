package ee.oyatl.ime.fusion.keyboard.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("keyboard-layout-preset-def")
@Serializable
data class KeyboardLayoutPresetDef(
    val keyboardTemplate: String,
    val layoutTables: List<String>,
    val altGraphTables: List<String>,
    val longPressTables: List<String>,
    val softKeyCodeMapper: String,
    val hardKeyCodeMapper: String
)
