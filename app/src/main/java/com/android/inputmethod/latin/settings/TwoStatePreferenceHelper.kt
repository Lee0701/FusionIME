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

import android.os.Build
import android.os.Build.VERSION_CODES
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceGroup
import android.preference.SwitchPreference

object TwoStatePreferenceHelper {
    private const val EMPTY_TEXT = ""

    fun replaceCheckBoxPreferencesBySwitchPreferences(group: PreferenceGroup) {
        // The keyboard settings keeps using a CheckBoxPreference on KitKat or previous.
        if (Build.VERSION.SDK_INT <= VERSION_CODES.KITKAT) {
            return
        }
        // The keyboard settings starts using a SwitchPreference without switch on/off text on
        // API versions newer than KitKat.
        replaceAllCheckBoxPreferencesBySwitchPreferences(group)
    }

    private fun replaceAllCheckBoxPreferencesBySwitchPreferences(
        group: PreferenceGroup
    ) {
        val preferences = ArrayList<Preference>()
        val count = group.preferenceCount
        for (index in 0 until count) {
            preferences.add(group.getPreference(index))
        }
        group.removeAll()
        for (index in 0 until count) {
            val preference = preferences[index]
            if (preference is CheckBoxPreference) {
                addSwitchPreferenceBasedOnCheckBoxPreference(preference, group)
            } else {
                group.addPreference(preference)
                if (preference is PreferenceGroup) {
                    replaceAllCheckBoxPreferencesBySwitchPreferences(
                        preference
                    )
                }
            }
        }
    }

    fun addSwitchPreferenceBasedOnCheckBoxPreference(
        checkBox: CheckBoxPreference,
        group: PreferenceGroup
    ) {
        val switchPref = SwitchPreference(checkBox.context)
        switchPref.title = checkBox.title
        switchPref.key = checkBox.key
        switchPref.order = checkBox.order
        switchPref.isPersistent = checkBox.isPersistent
        switchPref.isEnabled = checkBox.isEnabled
        switchPref.isChecked = checkBox.isChecked
        switchPref.summary = checkBox.summary
        switchPref.summaryOn = checkBox.summaryOn
        switchPref.summaryOff = checkBox.summaryOff
        switchPref.switchTextOn = EMPTY_TEXT
        switchPref.switchTextOff = EMPTY_TEXT
        group.addPreference(switchPref)
        switchPref.dependency = checkBox.dependency
    }
}
