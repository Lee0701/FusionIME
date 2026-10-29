package ee.oyatl.ime.theme

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import ee.oyatl.ime.fusion.keyboard.core.KeyType

data class KeyboardTheme(
    @ColorRes val backgroundColor: Int,
    @DrawableRes val moreKeysBackground: Int,
    val keyThemes: Map<KeyType, KeyTheme>,
    val popupTheme: PopupTheme,
    val candidatesTheme: CandidatesTheme,
    val tabBarTheme: TabBarTheme
)

data class KeyTheme(
    @ColorRes val backgroundColor: Int,
    @ColorRes val foregroundColor: Int,
    @ColorRes val hintColor: Int
)

data class PopupTheme(
    @ColorRes val backgroundColor: Int,
    @ColorRes val foregroundColor: Int,
    @ColorRes val hintColor: Int
)

data class CandidatesTheme(
    @ColorRes val backgroundColor: Int,
    val itemThemes: Map<CandidateItemType, CandidateItemTheme>
)

data class CandidateItemTheme(
    @ColorRes val foregroundColor: Int,
    @ColorRes val extraColor: Int
)

enum class CandidateItemType {
    Default
}

data class TabBarTheme(
    @ColorRes val backgroundColor: Int,
    val itemTheme: TabBarItemTheme
)

data class TabBarItemTheme(
    @DrawableRes val backgroundDrawable: Int,
    @ColorRes val backgroundColor: Int,
    @ColorRes val foregroundColor: Int
)
