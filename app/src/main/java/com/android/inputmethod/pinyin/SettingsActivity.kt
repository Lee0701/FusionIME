/*
 * Copyright (C) 2009 The Android Open Source Project
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
package com.android.inputmethod.pinyin

import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.Preference.OnPreferenceChangeListener
import android.preference.PreferenceActivity
import android.preference.PreferenceGroup
import android.preference.PreferenceManager
import ee.oyatl.ime.fusion.R

/**
 * Setting activity of Pinyin IME.
 */
class SettingsActivity : PreferenceActivity(), OnPreferenceChangeListener {
    private var mKeySoundPref: CheckBoxPreference? = null
    private var mVibratePref: CheckBoxPreference? = null
    private var mPredictionPref: CheckBoxPreference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.settings)

        val prefSet = preferenceScreen

        mKeySoundPref = prefSet
            .findPreference(getString(R.string.setting_sound_key)) as CheckBoxPreference
        mVibratePref = prefSet
            .findPreference(getString(R.string.setting_vibrate_key)) as CheckBoxPreference
        mPredictionPref = prefSet
            .findPreference(getString(R.string.setting_prediction_key)) as CheckBoxPreference

        prefSet.onPreferenceChangeListener = this

        Settings.getInstance(
            PreferenceManager
                .getDefaultSharedPreferences(applicationContext)
        )

        updatePreference(prefSet, getString(R.string.setting_advanced_key))

        updateWidgets()
    }

    override fun onResume() {
        super.onResume()
        updateWidgets()
    }

    override fun onDestroy() {
        Settings.releaseInstance()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        Settings.keySound = mKeySoundPref!!.isChecked
        Settings.vibrate = mVibratePref!!.isChecked
        Settings.prediction = mPredictionPref!!.isChecked

        Settings.writeBack()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        return true
    }

    private fun updateWidgets() {
        mKeySoundPref!!.isChecked = Settings.keySound
        mVibratePref!!.isChecked = Settings.vibrate
        mPredictionPref!!.isChecked = Settings.prediction
    }

    fun updatePreference(parentPref: PreferenceGroup, prefKey: String?) {
        val preference = parentPref.findPreference(prefKey) ?: return
        val intent = preference.intent
        if (intent != null) {
            val pm = packageManager
            val list = pm.queryIntentActivities(intent, 0)
            val listSize = list.size
            if (listSize == 0) parentPref.removePreference(preference)
        }
    }

    companion object {
        private const val TAG = "SettingsActivity"
    }
}
