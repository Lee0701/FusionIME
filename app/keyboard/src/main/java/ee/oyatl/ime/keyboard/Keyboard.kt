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
        data class Key(
            val keyCode: Int,
            val label: String = "",
            val iconRes: Int = 0,
            override val width: Float = 1f,
            val bkgRes: Int = R.drawable.key_bg,
            val themeRes: Int = R.style.Theme_FusionIME_Keyboard_Key
        ): KeyItem
    }
}