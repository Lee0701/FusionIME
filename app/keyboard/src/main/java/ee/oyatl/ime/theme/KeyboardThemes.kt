package ee.oyatl.ime.theme

import ee.oyatl.ime.keyboard.R

object KeyboardThemes {
    val StaticLight = KeyboardTheme(
        backgroundColor = R.color.keyboard_bg_light,
        moreKeysBackground = R.drawable.more_keys_bg,
        keyThemes = mapOf(
            KeyType.Default to KeyTheme(
                backgroundColor = R.color.key_bg_static_light,
                foregroundColor = R.color.black,
                hintColor = R.color.gray
            ),
            KeyType.Modifier to KeyTheme(
                backgroundColor = R.color.key_bg_static_light_mod,
                foregroundColor = R.color.black,
                hintColor = R.color.gray
            ),
            KeyType.Return to KeyTheme(
                backgroundColor = R.color.key_bg_static_light_return,
                foregroundColor = R.color.black,
                hintColor = R.color.gray
            ),
        ),
        popupTheme = PopupTheme(
            backgroundColor = R.color.key_bg_light_unpressed,
            foregroundColor = R.color.black,
            hintColor = R.color.gray
        ),
        candidatesTheme = CandidatesTheme(
            backgroundColor = R.color.candidate_bg_light,
            itemThemes = mapOf(
                CandidateItemType.Default to CandidateItemTheme(
                    backgroundColor = R.color.transparent,
                    foregroundColor = R.color.black,
                    extraColor = R.color.black
                )
            )
        ),
        tabBarTheme = TabBarTheme(
            backgroundColor = R.color.candidate_bg_light,
            itemTheme = TabBarItemTheme(
                backgroundDrawable = R.drawable.mode_switcher_tab_background,
                backgroundColor = R.color.tabbar_item_light,
                foregroundColor = R.color.black
            )
        )
    )
}