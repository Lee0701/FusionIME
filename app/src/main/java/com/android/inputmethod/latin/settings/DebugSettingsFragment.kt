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
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Process
import android.preference.Preference
import android.preference.Preference.OnPreferenceClickListener
import android.preference.PreferenceGroup
import android.preference.TwoStatePreference
import com.android.inputmethod.latin.DictionaryDumpBroadcastReceiver
import com.android.inputmethod.latin.DictionaryFacilitatorImpl
import ee.oyatl.ime.fusion.R
import com.android.inputmethod.latin.utils.ApplicationUtils
import com.android.inputmethod.latin.utils.ResourceUtils
import java.util.Locale

/**
 * "Debug mode" settings sub screen.
 *
 * This settings sub screen handles a several preference options for debugging.
 */
class DebugSettingsFragment : SubScreenFragment(), OnPreferenceClickListener {
    private var mServiceNeedsRestart = false
    private var mDebugMode: TwoStatePreference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.prefs_screen_debug)

        if (!Settings.SHOULD_SHOW_LXX_SUGGESTION_UI) {
            removePreference(DebugSettings.PREF_SHOULD_SHOW_LXX_SUGGESTION_UI, preferenceScreen)
        }

        val dictDumpPreferenceGroup =
            findPreference(PREF_KEY_DUMP_DICTS) as PreferenceGroup
        for (dictName in DictionaryFacilitatorImpl.DICT_TYPE_TO_CLASS.keys) {
            val pref: Preference = DictDumpPreference(activity, dictName)
            pref.onPreferenceClickListener = this
            dictDumpPreferenceGroup.addPreference(pref)
        }
        val res = resources
        setupKeyPreviewAnimationDuration(
            DebugSettings.PREF_KEY_PREVIEW_SHOW_UP_DURATION,
            res.getInteger(R.integer.config_key_preview_show_up_duration)
        )
        setupKeyPreviewAnimationDuration(
            DebugSettings.PREF_KEY_PREVIEW_DISMISS_DURATION,
            res.getInteger(R.integer.config_key_preview_dismiss_duration)
        )
        val defaultKeyPreviewShowUpStartScale = ResourceUtils.getFloatFromFraction(
            res, R.fraction.config_key_preview_show_up_start_scale
        )
        val defaultKeyPreviewDismissEndScale = ResourceUtils.getFloatFromFraction(
            res, R.fraction.config_key_preview_dismiss_end_scale
        )
        setupKeyPreviewAnimationScale(
            DebugSettings.PREF_KEY_PREVIEW_SHOW_UP_START_X_SCALE,
            defaultKeyPreviewShowUpStartScale
        )
        setupKeyPreviewAnimationScale(
            DebugSettings.PREF_KEY_PREVIEW_SHOW_UP_START_Y_SCALE,
            defaultKeyPreviewShowUpStartScale
        )
        setupKeyPreviewAnimationScale(
            DebugSettings.PREF_KEY_PREVIEW_DISMISS_END_X_SCALE,
            defaultKeyPreviewDismissEndScale
        )
        setupKeyPreviewAnimationScale(
            DebugSettings.PREF_KEY_PREVIEW_DISMISS_END_Y_SCALE,
            defaultKeyPreviewDismissEndScale
        )
        setupKeyboardHeight(
            DebugSettings.PREF_KEYBOARD_HEIGHT_SCALE, SettingsValues.DEFAULT_SIZE_SCALE
        )

        mServiceNeedsRestart = false
        mDebugMode = findPreference(DebugSettings.PREF_DEBUG_MODE) as TwoStatePreference
        updateDebugMode()
    }

    private class DictDumpPreference(context: Context?, dictName: String) :
        Preference(context) {
        val mDictName: String

        init {
            key = PREF_KEY_DUMP_DICT_PREFIX + dictName
            title = "Dump $dictName dictionary"
            mDictName = dictName
        }
    }

    override fun onPreferenceClick(pref: Preference): Boolean {
        val context: Context = activity
        if (pref is DictDumpPreference) {
            val dictName = pref.mDictName
            val intent: Intent = Intent(
                DictionaryDumpBroadcastReceiver.DICTIONARY_DUMP_INTENT_ACTION
            )
            intent.putExtra(DictionaryDumpBroadcastReceiver.DICTIONARY_NAME_KEY, dictName)
            context.sendBroadcast(intent)
            return true
        }
        return true
    }

    override fun onStop() {
        super.onStop()
        if (mServiceNeedsRestart) {
            Process.killProcess(Process.myPid())
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        if (key == DebugSettings.PREF_DEBUG_MODE && mDebugMode != null) {
            mDebugMode!!.isChecked = prefs.getBoolean(
                DebugSettings.PREF_DEBUG_MODE,
                false
            )
            updateDebugMode()
            mServiceNeedsRestart = true
            return
        }
        if (key == DebugSettings.PREF_FORCE_NON_DISTINCT_MULTITOUCH) {
            mServiceNeedsRestart = true
            return
        }
    }

    private fun updateDebugMode() {
        val isDebugMode = mDebugMode?.isChecked == true
        val version = getString(
            R.string.version_text, ApplicationUtils.getVersionName(activity)
        )
        if (!isDebugMode) {
            mDebugMode?.title = version
            mDebugMode?.summary = null
        } else {
            mDebugMode?.title = getString(R.string.prefs_debug_mode)
            mDebugMode?.summary = version
        }
    }

    private fun setupKeyPreviewAnimationScale(prefKey: String, defaultValue: Float) {
        val prefs = sharedPreferences
        val res = resources
        val pref =
            findPreference(prefKey) as SeekBarDialogPreference ?: return
        pref.setInterface(object : SeekBarDialogPreference.ValueProxy {
            private val PERCENTAGE_FLOAT = 100.0f

            fun getValueFromPercentage(percentage: Int): Float {
                return percentage / PERCENTAGE_FLOAT
            }

            fun getPercentageFromValue(floatValue: Float): Int {
                return (floatValue * PERCENTAGE_FLOAT).toInt()
            }

            override fun writeValue(value: Int, key: String?) {
                prefs.edit().putFloat(key, getValueFromPercentage(value)).apply()
            }

            override fun writeDefaultValue(key: String?) {
                prefs.edit().remove(key).apply()
            }

            override fun readValue(key: String?): Int {
                return getPercentageFromValue(
                    Settings.readKeyPreviewAnimationScale(
                        prefs, key, defaultValue
                    )
                )
            }

            override fun readDefaultValue(key: String?): Int {
                return getPercentageFromValue(defaultValue)
            }

            override fun getValueText(value: Int): String {
                if (value < 0) {
                    return res.getString(R.string.settings_system_default)
                }
                return String.format(Locale.ROOT, "%d%%", value)
            }

            override fun feedbackValue(value: Int) {}
        })
    }

    private fun setupKeyPreviewAnimationDuration(prefKey: String, defaultValue: Int) {
        val prefs = sharedPreferences
        val res = resources
        val pref =
            findPreference(prefKey) as SeekBarDialogPreference ?: return
        pref.setInterface(object : SeekBarDialogPreference.ValueProxy {
            override fun writeValue(value: Int, key: String?) {
                prefs.edit().putInt(key, value).apply()
            }

            override fun writeDefaultValue(key: String?) {
                prefs.edit().remove(key).apply()
            }

            override fun readValue(key: String?): Int {
                return Settings.readKeyPreviewAnimationDuration(
                    prefs, key, defaultValue
                )
            }

            override fun readDefaultValue(key: String?): Int {
                return defaultValue
            }

            override fun getValueText(value: Int): String {
                return res.getString(R.string.abbreviation_unit_milliseconds, value.toString())
            }

            override fun feedbackValue(value: Int) {}
        })
    }

    private fun setupKeyboardHeight(prefKey: String, defaultValue: Float) {
        val prefs = sharedPreferences
        val pref =
            findPreference(prefKey) as SeekBarDialogPreference ?: return
        pref.setInterface(object : SeekBarDialogPreference.ValueProxy {
            private val PERCENTAGE_FLOAT = 100.0f
            fun getValueFromPercentage(percentage: Int): Float {
                return percentage / PERCENTAGE_FLOAT
            }

            fun getPercentageFromValue(floatValue: Float): Int {
                return (floatValue * PERCENTAGE_FLOAT).toInt()
            }

            override fun writeValue(value: Int, key: String?) {
                prefs.edit().putFloat(key, getValueFromPercentage(value)).apply()
            }

            override fun writeDefaultValue(key: String?) {
                prefs.edit().remove(key).apply()
            }

            override fun readValue(key: String?): Int {
                return getPercentageFromValue(
                    Settings.readKeyboardHeight(
                        prefs, defaultValue
                    )
                )
            }

            override fun readDefaultValue(key: String?): Int {
                return getPercentageFromValue(defaultValue)
            }

            override fun getValueText(value: Int): String {
                return String.format(Locale.ROOT, "%d%%", value)
            }

            override fun feedbackValue(value: Int) {}
        })
    }

    companion object {
        private const val PREF_KEY_DUMP_DICTS = "pref_key_dump_dictionaries"
        private const val PREF_KEY_DUMP_DICT_PREFIX = "pref_key_dump_dictionaries"
    }
}
