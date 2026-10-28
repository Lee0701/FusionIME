package ee.oyatl.ime.theme

import ee.oyatl.ime.keyboard.R

object KeyboardThemes {
    val StaticLight = KeyboardTheme(
        backgroundColor = R.color.keyboard_bg_static_light,
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
            backgroundColor = R.color.candidate_bg_static_light,
            itemThemes = mapOf(
                CandidateItemType.Default to CandidateItemTheme(
                    backgroundColor = R.color.transparent,
                    foregroundColor = R.color.black,
                    extraColor = R.color.black
                )
            )
        ),
        tabBarTheme = TabBarTheme(
            backgroundColor = R.color.candidate_bg_static_light,
            itemTheme = TabBarItemTheme(
                backgroundDrawable = R.drawable.mode_switcher_tab_background,
                backgroundColor = R.color.tabbar_item_static_light,
                foregroundColor = R.color.black
            )
        )
    )

    val StaticDark = KeyboardTheme(
        backgroundColor = R.color.keyboard_bg_static_dark,
        moreKeysBackground = R.drawable.more_keys_bg,
        keyThemes = mapOf(
            KeyType.Default to KeyTheme(
                backgroundColor = R.color.key_bg_static_dark,
                foregroundColor = R.color.white,
                hintColor = R.color.gray
            ),
            KeyType.Modifier to KeyTheme(
                backgroundColor = R.color.key_bg_static_dark_mod,
                foregroundColor = R.color.white,
                hintColor = R.color.gray
            ),
            KeyType.Return to KeyTheme(
                backgroundColor = R.color.key_bg_static_dark_return,
                foregroundColor = R.color.white,
                hintColor = R.color.gray
            ),
        ),
        popupTheme = PopupTheme(
            backgroundColor = R.color.key_bg_dark_unpressed,
            foregroundColor = R.color.white,
            hintColor = R.color.gray
        ),
        candidatesTheme = CandidatesTheme(
            backgroundColor = R.color.candidate_bg_static_dark,
            itemThemes = mapOf(
                CandidateItemType.Default to CandidateItemTheme(
                    backgroundColor = R.color.transparent,
                    foregroundColor = R.color.white,
                    extraColor = R.color.white
                )
            )
        ),
        tabBarTheme = TabBarTheme(
            backgroundColor = R.color.candidate_bg_static_dark,
            itemTheme = TabBarItemTheme(
                backgroundDrawable = R.drawable.mode_switcher_tab_background,
                backgroundColor = R.color.tabbar_item_static_dark,
                foregroundColor = R.color.white
            )
        )
    )

    val DynamicLight = KeyboardTheme(
        backgroundColor = R.color.keyboard_bg_dynamic_light,
        moreKeysBackground = R.drawable.more_keys_bg,
        keyThemes = mapOf(
            KeyType.Default to KeyTheme(
                backgroundColor = R.color.key_bg_dynamic_light,
                foregroundColor = R.color.black,
                hintColor = R.color.gray
            ),
            KeyType.Modifier to KeyTheme(
                backgroundColor = R.color.key_bg_dynamic_light_mod,
                foregroundColor = R.color.black,
                hintColor = R.color.gray
            ),
            KeyType.Return to KeyTheme(
                backgroundColor = R.color.key_bg_dynamic_light_return,
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
            backgroundColor = R.color.candidate_bg_dynamic_light,
            itemThemes = mapOf(
                CandidateItemType.Default to CandidateItemTheme(
                    backgroundColor = R.color.transparent,
                    foregroundColor = R.color.black,
                    extraColor = R.color.black
                )
            )
        ),
        tabBarTheme = TabBarTheme(
            backgroundColor = R.color.candidate_bg_dynamic_light,
            itemTheme = TabBarItemTheme(
                backgroundDrawable = R.drawable.mode_switcher_tab_background,
                backgroundColor = R.color.tabbar_item_dynamic_light,
                foregroundColor = R.color.black
            )
        )
    )

    val DynamicDark = KeyboardTheme(
        backgroundColor = R.color.keyboard_bg_dynamic_dark,
        moreKeysBackground = R.drawable.more_keys_bg,
        keyThemes = mapOf(
            KeyType.Default to KeyTheme(
                backgroundColor = R.color.key_bg_dynamic_dark,
                foregroundColor = R.color.white,
                hintColor = R.color.gray
            ),
            KeyType.Modifier to KeyTheme(
                backgroundColor = R.color.key_bg_dynamic_dark_mod,
                foregroundColor = R.color.white,
                hintColor = R.color.gray
            ),
            KeyType.Return to KeyTheme(
                backgroundColor = R.color.key_bg_dynamic_dark_return,
                foregroundColor = R.color.white,
                hintColor = R.color.gray
            ),
        ),
        popupTheme = PopupTheme(
            backgroundColor = R.color.key_bg_dark_unpressed,
            foregroundColor = R.color.white,
            hintColor = R.color.gray
        ),
        candidatesTheme = CandidatesTheme(
            backgroundColor = R.color.candidate_bg_dynamic_dark,
            itemThemes = mapOf(
                CandidateItemType.Default to CandidateItemTheme(
                    backgroundColor = R.color.transparent,
                    foregroundColor = R.color.white,
                    extraColor = R.color.white
                )
            )
        ),
        tabBarTheme = TabBarTheme(
            backgroundColor = R.color.candidate_bg_dynamic_dark,
            itemTheme = TabBarItemTheme(
                backgroundDrawable = R.drawable.mode_switcher_tab_background,
                backgroundColor = R.color.tabbar_item_dynamic_dark,
                foregroundColor = R.color.white
            )
        )
    )
}