package ee.oyatl.ime.keyboard

data class KeyboardConfiguration(
    val rows: List<List<Item>>
) {
    constructor(vararg configurations: KeyboardConfiguration): this(configurations.flatMap { it.rows })

    operator fun plus(other: KeyboardConfiguration): KeyboardConfiguration {
        return KeyboardConfiguration(this.rows + other.rows)
    }

    sealed interface Item {
        data class ContentKey(
            val rowId: Int,
            val index: Int
        ): Item
        data class ContentRow(
            val rowId: Int
        ): Item
        data class Spacer(
            val width: Float
        ): Item
        data class TemplateKey(
            val keyCode: Int,
            val width: Float = 1f,
            val special: Boolean = false
        ): Item
    }
}