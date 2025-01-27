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

import android.app.backup.BackupManager
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.util.Log

/**
 * A base abstract class for a [PreferenceFragment] that implements a nested
 * [PreferenceScreen] of the main preference screen.
 */
abstract class SubScreenFragment : PreferenceFragment(), OnSharedPreferenceChangeListener {
    private var mSharedPreferenceChangeListener: OnSharedPreferenceChangeListener? = null

    fun setPreferenceEnabled(prefKey: String?, enabled: Boolean) {
        setPreferenceEnabled(prefKey, enabled, preferenceScreen)
    }

    val sharedPreferences: SharedPreferences
        get() = preferenceManager.sharedPreferences

    val applicationName: String
        /**
         * Gets the application name to display on the UI.
         */
        get() {
            val context: Context = activity
            val res = resources
            val applicationLabelRes = context.applicationInfo.labelRes
            return res.getString(applicationLabelRes)
        }

    override fun addPreferencesFromResource(preferencesResId: Int) {
        super.addPreferencesFromResource(preferencesResId)
        TwoStatePreferenceHelper.replaceCheckBoxPreferencesBySwitchPreferences(
            preferenceScreen
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSharedPreferenceChangeListener =
            OnSharedPreferenceChangeListener { prefs, key ->
                val fragment = this@SubScreenFragment
                val context: Context? = fragment.activity
                if (context == null || fragment.preferenceScreen == null) {
                    val tag = fragment.javaClass.simpleName
                    // TODO: Introduce a static function to register this class and ensure that
                    // onCreate must be called before "onSharedPreferenceChanged" is called.
                    Log.w(
                        tag,
                        "onSharedPreferenceChanged called before activity starts."
                    )
                    return@OnSharedPreferenceChangeListener
                }
                BackupManager(context).dataChanged()
                fragment.onSharedPreferenceChanged(prefs, key)
            }
        sharedPreferences.registerOnSharedPreferenceChangeListener(
            mSharedPreferenceChangeListener
        )
    }

    override fun onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(
            mSharedPreferenceChangeListener
        )
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        // This method may be overridden by an extended class.
    }

    companion object {
        fun setPreferenceEnabled(
            prefKey: String?, enabled: Boolean,
            screen: PreferenceScreen
        ) {
            val preference = screen.findPreference(prefKey)
            if (preference != null) {
                preference.isEnabled = enabled
            }
        }

        @JvmOverloads
        fun removePreference(prefKey: String?, screen: PreferenceScreen = getPreferenceScreen()) {
            val preference = screen.findPreference(prefKey)
            if (preference != null) {
                screen.removePreference(preference)
            }
        }

        @JvmOverloads
        fun updateListPreferenceSummaryToCurrentValue(
            prefKey: String?,
            screen: PreferenceScreen = getPreferenceScreen()
        ) {
            // Because the "%s" summary trick of {@link ListPreference} doesn't work properly before
            // KitKat, we need to update the summary programmatically.
            val listPreference =
                screen.findPreference(prefKey) as ListPreference ?: return
            val entries = listPreference.entries
            val entryIndex = listPreference.findIndexOfValue(listPreference.value)
            listPreference.summary = if (entryIndex < 0) null else entries[entryIndex]
        }
    }
}
