/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.inputmethod.latin.settings

import android.content.Context
import android.os.Bundle
import android.preference.Preference
import com.android.inputmethod.keyboard.KeyboardTheme
import ee.oyatl.ime.fusion.R
import com.android.inputmethod.latin.settings.RadioButtonPreference.OnRadioButtonClickedListener

/**
 * "Keyboard theme" settings sub screen.
 */
class ThemeSettingsFragment : SubScreenFragment(), OnRadioButtonClickedListener {
    private var mSelectedThemeId = 0

    internal class KeyboardThemePreference(context: Context?, name: String?, id: Int) :
        RadioButtonPreference(context) {
        val mThemeId: Int

        init {
            title = name
            mThemeId = id
        }
    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        addPreferencesFromResource(R.xml.prefs_screen_theme)
        val screen = preferenceScreen
        val context: Context = activity
        val res = resources
        val keyboardThemeNames = res.getStringArray(R.array.keyboard_theme_names)
        val keyboardThemeIds = res.getIntArray(R.array.keyboard_theme_ids)
        for (index in keyboardThemeNames.indices) {
            val pref = KeyboardThemePreference(
                context, keyboardThemeNames[index], keyboardThemeIds[index]
            )
            screen.addPreference(pref)
            pref.setOnRadioButtonClickedListener(this)
        }
        val keyboardTheme: KeyboardTheme = KeyboardTheme.getKeyboardTheme(context)!!
        mSelectedThemeId = keyboardTheme.mThemeId
    }

    override fun onRadioButtonClicked(preference: RadioButtonPreference?) {
        if (preference is KeyboardThemePreference) {
            mSelectedThemeId = preference.mThemeId
            updateSelected()
        }
    }

    override fun onResume() {
        super.onResume()
        updateSelected()
    }

    override fun onPause() {
        super.onPause()
        KeyboardTheme.saveKeyboardThemeId(mSelectedThemeId, sharedPreferences)
    }

    private fun updateSelected() {
        val screen = preferenceScreen
        val count = screen.preferenceCount
        for (index in 0 until count) {
            val preference = screen.getPreference(index)
            if (preference is KeyboardThemePreference) {
                val pref = preference
                val selected = (mSelectedThemeId == pref.mThemeId)
                pref.isSelected = selected
            }
        }
    }

    companion object {
        fun updateKeyboardThemeSummary(pref: Preference) {
            val context = pref.context
            val res = context.resources
            val keyboardTheme: KeyboardTheme = KeyboardTheme.getKeyboardTheme(context)!!
            val keyboardThemeNames = res.getStringArray(R.array.keyboard_theme_names)
            val keyboardThemeIds = res.getIntArray(R.array.keyboard_theme_ids)
            for (index in keyboardThemeNames.indices) {
                if (keyboardTheme.mThemeId == keyboardThemeIds[index]) {
                    pref.summary = keyboardThemeNames[index]
                    return
                }
            }
        }
    }
}
