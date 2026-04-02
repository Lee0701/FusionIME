package ee.oyatl.ime.keyboard

data class Keyboard(
    val rows: List<List<KeyItem>>,
    val params: KeyboardParams
) {
    sealed interface KeyItem {
        val width: Float
        data class SplitSpacer(
            val absoluteWidth: Int
        ): KeyItem {
            override val width: Float = 0f
        }
        data class Spacer(
            override val width: Float
        ): KeyItem
        data class NormalKey(
            override val keyCode: Int,
            override val width: Float = 1f
        ): Key
        data class SpecialKey(
            override val keyCode: Int,
            override val width: Float = 1f
        ): Key
        interface Key: KeyItem {
            val keyCode: Int
        }
    }
}