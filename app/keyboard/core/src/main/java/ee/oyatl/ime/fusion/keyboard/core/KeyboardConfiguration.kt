package ee.oyatl.ime.fusion.keyboard.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KeyboardConfiguration(
    val rows: List<List<Item>>
) {
    constructor(vararg configurations: KeyboardConfiguration): this(configurations.flatMap { it.rows })

    operator fun plus(other: KeyboardConfiguration): KeyboardConfiguration {
        return KeyboardConfiguration(this.rows + other.rows)
    }

    @Serializable
    sealed interface Item {
        @SerialName("content-key")
        @Serializable
        data class ContentKey(
            val rowId: Int,
            val index: Int
        ): Item

        @SerialName("content-row")
        @Serializable
        data class ContentRow(
            val rowId: Int
        ): Item

        @SerialName("spacer")
        @Serializable
        data class Spacer(
            val width: Float
        ): Item

        @SerialName("template-key")
        @Serializable
        data class TemplateKey(
            val keyCode: Int,
            val width: Float = 1f,
            val special: Boolean = false,
            val label: String? = null,
            val iconRes: Int? = null,
            val merge: Merge = Merge.None
        ): Item {
            @Serializable
            enum class Merge {
                None, Up, Down
            }
        }
    }
}