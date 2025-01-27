/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.inputmethod.latin.utils

import com.android.inputmethod.dictionarypack.DictionarySettingsFragment
import com.android.inputmethod.latin.about.AboutPreferences
import com.android.inputmethod.latin.settings.AccountsSettingsFragment
import com.android.inputmethod.latin.settings.AdvancedSettingsFragment
import com.android.inputmethod.latin.settings.AppearanceSettingsFragment
import com.android.inputmethod.latin.settings.CorrectionSettingsFragment
import com.android.inputmethod.latin.settings.CustomInputStyleSettingsFragment
import com.android.inputmethod.latin.settings.DebugSettingsFragment
import com.android.inputmethod.latin.settings.GestureSettingsFragment
import com.android.inputmethod.latin.settings.PreferencesSettingsFragment
import com.android.inputmethod.latin.settings.SettingsFragment
import com.android.inputmethod.latin.settings.ThemeSettingsFragment
import com.android.inputmethod.latin.spellcheck.SpellCheckerSettingsFragment
import com.android.inputmethod.latin.userdictionary.UserDictionaryAddWordFragment
import com.android.inputmethod.latin.userdictionary.UserDictionaryList
import com.android.inputmethod.latin.userdictionary.UserDictionaryLocalePicker
import com.android.inputmethod.latin.userdictionary.UserDictionarySettings

object FragmentUtils {
    private val sLatinImeFragments = HashSet<String>()

    init {
        sLatinImeFragments.add(DictionarySettingsFragment::class.java.name)
        sLatinImeFragments.add(AboutPreferences::class.java.name)
        sLatinImeFragments.add(PreferencesSettingsFragment::class.java.name)
        sLatinImeFragments.add(AccountsSettingsFragment::class.java.name)
        sLatinImeFragments.add(AppearanceSettingsFragment::class.java.name)
        sLatinImeFragments.add(ThemeSettingsFragment::class.java.name)
        sLatinImeFragments.add(CustomInputStyleSettingsFragment::class.java.name)
        sLatinImeFragments.add(GestureSettingsFragment::class.java.name)
        sLatinImeFragments.add(CorrectionSettingsFragment::class.java.name)
        sLatinImeFragments.add(AdvancedSettingsFragment::class.java.name)
        sLatinImeFragments.add(DebugSettingsFragment::class.java.name)
        sLatinImeFragments.add(SettingsFragment::class.java.name)
        sLatinImeFragments.add(SpellCheckerSettingsFragment::class.java.name)
        sLatinImeFragments.add(UserDictionaryAddWordFragment::class.java.name)
        sLatinImeFragments.add(UserDictionaryList::class.java.name)
        sLatinImeFragments.add(UserDictionaryLocalePicker::class.java.name)
        sLatinImeFragments.add(UserDictionarySettings::class.java.name)
    }

    fun isValidFragment(fragmentName: String): Boolean {
        return sLatinImeFragments.contains(fragmentName)
    }
}
