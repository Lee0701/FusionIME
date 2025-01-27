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
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Bundle
import android.preference.ListPreference
import com.android.inputmethod.latin.AudioAndHapticFeedbackManager
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.SystemBroadcastReceiver

/**
 * "Advanced" settings sub screen.
 *
 * This settings sub screen handles the following advanced preferences.
 * - Key popup dismiss delay
 * - Keypress vibration duration
 * - Keypress sound volume
 * - Show app icon
 * - Improve keyboard
 * - Debug settings
 */
class AdvancedSettingsFragment : SubScreenFragment() {
    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        addPreferencesFromResource(R.xml.prefs_screen_advanced)

        val res = resources
        val context: Context = activity

        // When we are called from the Settings application but we are not already running, some
        // singleton and utility classes may not have been initialized.  We have to call
        // initialization method of these classes here. See {@link LatinIME#onCreate()}.
        AudioAndHapticFeedbackManager.Companion.init(context)

        val prefs = preferenceManager.sharedPreferences

        if (!Settings.Companion.isInternal(prefs)) {
            removePreference(Settings.Companion.SCREEN_DEBUG)
        }

        if (!AudioAndHapticFeedbackManager.Companion.getInstance().hasVibrator()) {
            removePreference(Settings.Companion.PREF_VIBRATION_DURATION_SETTINGS)
        }

        // TODO: consolidate key preview dismiss delay with the key preview animation parameters.
        if (!Settings.Companion.readFromBuildConfigIfToShowKeyPreviewPopupOption(res)) {
            removePreference(Settings.Companion.PREF_KEY_PREVIEW_POPUP_DISMISS_DELAY)
        } else {
            // TODO: Cleanup this setup.
            val keyPreviewPopupDismissDelay =
                findPreference(Settings.Companion.PREF_KEY_PREVIEW_POPUP_DISMISS_DELAY) as ListPreference
            val popupDismissDelayDefaultValue = res.getInteger(
                R.integer.config_key_preview_linger_timeout
            ).toString()
            keyPreviewPopupDismissDelay.entries = arrayOf(
                res.getString(R.string.key_preview_popup_dismiss_no_delay),
                res.getString(R.string.key_preview_popup_dismiss_default_delay),
            )
            keyPreviewPopupDismissDelay.entryValues = arrayOf(
                "0",
                popupDismissDelayDefaultValue
            )
            if (null == keyPreviewPopupDismissDelay.value) {
                keyPreviewPopupDismissDelay.value = popupDismissDelayDefaultValue
            }
            keyPreviewPopupDismissDelay.isEnabled =
                Settings.Companion.readKeyPreviewPopupEnabled(
                    prefs,
                    res
                )
        }

        setupKeypressVibrationDurationSettings()
        setupKeypressSoundVolumeSettings()
        setupKeyLongpressTimeoutSettings()
        refreshEnablingsOfKeypressSoundAndVibrationSettings()
    }

    override fun onResume() {
        super.onResume()
        val prefs = preferenceManager.sharedPreferences
        updateListPreferenceSummaryToCurrentValue(Settings.Companion.PREF_KEY_PREVIEW_POPUP_DISMISS_DELAY)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        val res = resources
        if (key == Settings.Companion.PREF_POPUP_ON) {
            setPreferenceEnabled(
                Settings.Companion.PREF_KEY_PREVIEW_POPUP_DISMISS_DELAY,
                Settings.Companion.readKeyPreviewPopupEnabled(prefs, res)
            )
        } else if (key == Settings.Companion.PREF_SHOW_SETUP_WIZARD_ICON) {
            SystemBroadcastReceiver.Companion.toggleAppIcon(activity)
        }
        updateListPreferenceSummaryToCurrentValue(Settings.Companion.PREF_KEY_PREVIEW_POPUP_DISMISS_DELAY)
        refreshEnablingsOfKeypressSoundAndVibrationSettings()
    }

    private fun refreshEnablingsOfKeypressSoundAndVibrationSettings() {
        val prefs = sharedPreferences
        val res = resources
        setPreferenceEnabled(
            Settings.Companion.PREF_VIBRATION_DURATION_SETTINGS,
            Settings.Companion.readVibrationEnabled(prefs!!, res)
        )
        setPreferenceEnabled(
            Settings.Companion.PREF_KEYPRESS_SOUND_VOLUME,
            Settings.Companion.readKeypressSoundEnabled(prefs, res)
        )
    }

    private fun setupKeypressVibrationDurationSettings() {
        val pref = findPreference(
            Settings.Companion.PREF_VIBRATION_DURATION_SETTINGS
        ) as SeekBarDialogPreference
        if (pref == null) {
            return
        }
        val prefs = sharedPreferences
        val res = resources
        pref.setInterface(object : SeekBarDialogPreference.ValueProxy {
            override fun writeValue(value: Int, key: String?) {
                prefs!!.edit().putInt(key, value).apply()
            }

            override fun writeDefaultValue(key: String?) {
                prefs!!.edit().remove(key).apply()
            }

            override fun readValue(key: String?): Int {
                return Settings.Companion.readKeypressVibrationDuration(
                    prefs!!, res
                )
            }

            override fun readDefaultValue(key: String?): Int {
                return Settings.Companion.readDefaultKeypressVibrationDuration(res)
            }

            override fun feedbackValue(value: Int) {
                AudioAndHapticFeedbackManager.Companion.getInstance().vibrate(value.toLong())
            }

            override fun getValueText(value: Int): String {
                if (value < 0) {
                    return res.getString(R.string.settings_system_default)
                }
                return res.getString(R.string.abbreviation_unit_milliseconds, value)
            }
        })
    }

    private fun setupKeypressSoundVolumeSettings() {
        val pref = findPreference(
            Settings.Companion.PREF_KEYPRESS_SOUND_VOLUME
        ) as SeekBarDialogPreference
        if (pref == null) {
            return
        }
        val prefs = sharedPreferences
        val res = resources
        val am = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        pref.setInterface(object : SeekBarDialogPreference.ValueProxy {
            private val PERCENTAGE_FLOAT = 100.0f

            fun getValueFromPercentage(percentage: Int): Float {
                return percentage / PERCENTAGE_FLOAT
            }

            fun getPercentageFromValue(floatValue: Float): Int {
                return (floatValue * PERCENTAGE_FLOAT).toInt()
            }

            override fun writeValue(value: Int, key: String?) {
                prefs!!.edit().putFloat(key, getValueFromPercentage(value)).apply()
            }

            override fun writeDefaultValue(key: String?) {
                prefs!!.edit().remove(key).apply()
            }

            override fun readValue(key: String?): Int {
                return getPercentageFromValue(
                    Settings.Companion.readKeypressSoundVolume(
                        prefs!!, res
                    )
                )
            }

            override fun readDefaultValue(key: String?): Int {
                return getPercentageFromValue(Settings.Companion.readDefaultKeypressSoundVolume(res))
            }

            override fun getValueText(value: Int): String {
                if (value < 0) {
                    return res.getString(R.string.settings_system_default)
                }
                return value.toString()
            }

            override fun feedbackValue(value: Int) {
                am.playSoundEffect(
                    AudioManager.FX_KEYPRESS_STANDARD, getValueFromPercentage(value)
                )
            }
        })
    }

    private fun setupKeyLongpressTimeoutSettings() {
        val prefs = sharedPreferences
        val res = resources
        val pref = findPreference(
            Settings.Companion.PREF_KEY_LONGPRESS_TIMEOUT
        ) as SeekBarDialogPreference
        if (pref == null) {
            return
        }
        pref.setInterface(object : SeekBarDialogPreference.ValueProxy {
            override fun writeValue(value: Int, key: String?) {
                prefs!!.edit().putInt(key, value).apply()
            }

            override fun writeDefaultValue(key: String?) {
                prefs!!.edit().remove(key).apply()
            }

            override fun readValue(key: String?): Int {
                return Settings.Companion.readKeyLongpressTimeout(
                    prefs!!, res
                )
            }

            override fun readDefaultValue(key: String?): Int {
                return Settings.Companion.readDefaultKeyLongpressTimeout(res)
            }

            override fun getValueText(value: Int): String {
                return res.getString(R.string.abbreviation_unit_milliseconds, value)
            }

            override fun feedbackValue(value: Int) {}
        })
    }
}
