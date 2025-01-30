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
package com.android.inputmethod.latin.settings

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build.VERSION_CODES
import android.preference.PreferenceManager
import android.util.Log
import com.android.inputmethod.compat.BuildCompatUtils
import com.android.inputmethod.latin.AudioAndHapticFeedbackManager
import com.android.inputmethod.latin.InputAttributes
import ee.oyatl.ime.fusion.R
import com.android.inputmethod.latin.common.StringUtils
import com.android.inputmethod.latin.utils.AdditionalSubtypeUtils
import com.android.inputmethod.latin.utils.ResourceUtils
import com.android.inputmethod.latin.utils.RunInLocale
import com.android.inputmethod.latin.utils.StatsUtils
import java.util.Locale
import java.util.concurrent.locks.ReentrantLock

class Settings private constructor() : OnSharedPreferenceChangeListener {
    private var mContext: Context? = null
    private var mRes: Resources? = null
    private var mPrefs: SharedPreferences? = null

    // TODO: Remove this method and add proxy method to SettingsValues.
    var current: SettingsValues? = null
        private set
    private val mSettingsValuesLock = ReentrantLock()

    private fun onCreate(context: Context) {
        mContext = context
        mRes = context.resources
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        this.mPrefs = prefs
        prefs.registerOnSharedPreferenceChangeListener(this)
        upgradeAutocorrectionSettings(prefs, mRes!!)
    }

    fun onDestroy() {
        mPrefs!!.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        mSettingsValuesLock.lock()
        try {
            if (current == null) {
                // TODO: Introduce a static function to register this class and ensure that
                // loadSettings must be called before "onSharedPreferenceChanged" is called.
                Log.w(TAG, "onSharedPreferenceChanged called before loadSettings.")
                return
            }
            loadSettings(mContext, current!!.mLocale, current!!.mInputAttributes)
            StatsUtils.onLoadSettings(current)
        } finally {
            mSettingsValuesLock.unlock()
        }
    }

    fun loadSettings(
        context: Context?, locale: Locale?,
        inputAttributes: InputAttributes
    ) {
        mSettingsValuesLock.lock()
        mContext = context
        try {
            val prefs = mPrefs
            val job: RunInLocale<SettingsValues> = object : RunInLocale<SettingsValues>() {
                override fun job(res: Resources): SettingsValues {
                    return SettingsValues(context!!, prefs!!, res, inputAttributes)
                }
            }
            current = job.runInLocale(mRes!!, locale)
        } finally {
            mSettingsValuesLock.unlock()
        }
    }

    val isInternal: Boolean
        get() = current!!.mIsInternal

    fun writeLastUsedPersonalizationToken(token: ByteArray?) {
        if (token == null) {
            mPrefs!!.edit().remove(PREF_LAST_USED_PERSONALIZATION_TOKEN).apply()
        } else {
            val tokenStr = StringUtils.byteArrayToHexString(token)
            mPrefs!!.edit().putString(PREF_LAST_USED_PERSONALIZATION_TOKEN, tokenStr).apply()
        }
    }

    fun readLastUsedPersonalizationToken(): ByteArray? {
        val tokenStr = mPrefs!!.getString(PREF_LAST_USED_PERSONALIZATION_TOKEN, null)
        return StringUtils.hexStringToByteArray(tokenStr)
    }

    fun writeLastPersonalizationDictWipedTime(timestamp: Long) {
        mPrefs!!.edit().putLong(PREF_LAST_PERSONALIZATION_DICT_WIPED_TIME, timestamp).apply()
    }

    fun readLastPersonalizationDictGeneratedTime(): Long {
        return mPrefs!!.getLong(PREF_LAST_PERSONALIZATION_DICT_WIPED_TIME, 0)
    }

    fun writeCorpusHandlesForPersonalization(corpusHandles: Set<String?>?) {
        mPrefs!!.edit().putStringSet(PREF_CORPUS_HANDLES_FOR_PERSONALIZATION, corpusHandles).apply()
    }

    fun readCorpusHandlesForPersonalization(): Set<String> {
        val emptySet = emptySet<String>()
        return mPrefs!!.getStringSet(PREF_CORPUS_HANDLES_FOR_PERSONALIZATION, emptySet)!!
    }

    private fun upgradeAutocorrectionSettings(prefs: SharedPreferences, res: Resources) {
        val thresholdSetting =
            prefs.getString(PREF_AUTO_CORRECTION_THRESHOLD_OBSOLETE, null)
        if (thresholdSetting != null) {
            val editor = prefs.edit()
            editor.remove(PREF_AUTO_CORRECTION_THRESHOLD_OBSOLETE)
            val autoCorrectionOff =
                res.getString(R.string.auto_correction_threshold_mode_index_off)
            if (thresholdSetting == autoCorrectionOff) {
                editor.putBoolean(PREF_AUTO_CORRECTION, false)
            } else {
                editor.putBoolean(PREF_AUTO_CORRECTION, true)
            }
            editor.commit()
        }
    }

    companion object {
        private val TAG: String = Settings::class.java.simpleName

        // Settings screens
        const val SCREEN_ACCOUNTS: String = "screen_accounts"
        const val SCREEN_THEME: String = "screen_theme"
        const val SCREEN_DEBUG: String = "screen_debug"

        // In the same order as xml/prefs.xml
        const val PREF_AUTO_CAP: String = "auto_cap"
        const val PREF_VIBRATE_ON: String = "vibrate_on"
        const val PREF_SOUND_ON: String = "sound_on"
        const val PREF_POPUP_ON: String = "popup_on"

        // PREF_VOICE_MODE_OBSOLETE is obsolete. Use PREF_VOICE_INPUT_KEY instead.
        const val PREF_VOICE_MODE_OBSOLETE: String = "voice_mode"
        const val PREF_VOICE_INPUT_KEY: String = "pref_voice_input_key"
        const val PREF_EDIT_PERSONAL_DICTIONARY: String = "edit_personal_dictionary"
        const val PREF_CONFIGURE_DICTIONARIES_KEY: String = "configure_dictionaries_key"

        // PREF_AUTO_CORRECTION_THRESHOLD_OBSOLETE is obsolete. Use PREF_AUTO_CORRECTION instead.
        const val PREF_AUTO_CORRECTION_THRESHOLD_OBSOLETE: String = "auto_correction_threshold"
        const val PREF_AUTO_CORRECTION: String = "pref_key_auto_correction"

        // PREF_SHOW_SUGGESTIONS_SETTING_OBSOLETE is obsolete. Use PREF_SHOW_SUGGESTIONS instead.
        const val PREF_SHOW_SUGGESTIONS_SETTING_OBSOLETE: String = "show_suggestions_setting"
        const val PREF_SHOW_SUGGESTIONS: String = "show_suggestions"
        const val PREF_KEY_USE_CONTACTS_DICT: String = "pref_key_use_contacts_dict"
        const val PREF_KEY_USE_PERSONALIZED_DICTS: String = "pref_key_use_personalized_dicts"
        const val PREF_KEY_USE_DOUBLE_SPACE_PERIOD: String = "pref_key_use_double_space_period"
        const val PREF_BLOCK_POTENTIALLY_OFFENSIVE: String = "pref_key_block_potentially_offensive"
        val ENABLE_SHOW_LANGUAGE_SWITCH_KEY_SETTINGS: Boolean =
            BuildCompatUtils.EFFECTIVE_SDK_INT <= VERSION_CODES.KITKAT
        val SHOULD_SHOW_LXX_SUGGESTION_UI: Boolean =
            BuildCompatUtils.EFFECTIVE_SDK_INT >= VERSION_CODES.LOLLIPOP
        const val PREF_SHOW_LANGUAGE_SWITCH_KEY: String = "pref_show_language_switch_key"
        const val PREF_INCLUDE_OTHER_IMES_IN_LANGUAGE_SWITCH_LIST: String =
            "pref_include_other_imes_in_language_switch_list"
        const val PREF_CUSTOM_INPUT_STYLES: String = "custom_input_styles"
        const val PREF_ENABLE_SPLIT_KEYBOARD: String = "pref_split_keyboard"

        // TODO: consolidate key preview dismiss delay with the key preview animation parameters.
        const val PREF_KEY_PREVIEW_POPUP_DISMISS_DELAY: String =
            "pref_key_preview_popup_dismiss_delay"
        const val PREF_BIGRAM_PREDICTIONS: String = "next_word_prediction"
        const val PREF_GESTURE_INPUT: String = "gesture_input"
        const val PREF_VIBRATION_DURATION_SETTINGS: String = "pref_vibration_duration_settings"
        const val PREF_KEYPRESS_SOUND_VOLUME: String = "pref_keypress_sound_volume"
        const val PREF_KEY_LONGPRESS_TIMEOUT: String = "pref_key_longpress_timeout"
        const val PREF_ENABLE_EMOJI_ALT_PHYSICAL_KEY: String = "pref_enable_emoji_alt_physical_key"
        const val PREF_GESTURE_PREVIEW_TRAIL: String = "pref_gesture_preview_trail"
        const val PREF_GESTURE_FLOATING_PREVIEW_TEXT: String = "pref_gesture_floating_preview_text"
        const val PREF_SHOW_SETUP_WIZARD_ICON: String = "pref_show_setup_wizard_icon"

        const val PREF_KEY_IS_INTERNAL: String = "pref_key_is_internal"

        const val PREF_ENABLE_METRICS_LOGGING: String = "pref_enable_metrics_logging"

        // This preference key is deprecated. Use {@link #PREF_SHOW_LANGUAGE_SWITCH_KEY} instead.
        // This is being used only for the backward compatibility.
        private const val PREF_SUPPRESS_LANGUAGE_SWITCH_KEY = "pref_suppress_language_switch_key"

        private const val PREF_LAST_USED_PERSONALIZATION_TOKEN =
            "pref_last_used_personalization_token"
        private const val PREF_LAST_PERSONALIZATION_DICT_WIPED_TIME =
            "pref_last_used_personalization_dict_wiped_time"
        private const val PREF_CORPUS_HANDLES_FOR_PERSONALIZATION =
            "pref_corpus_handles_for_personalization"

        // Emoji
        const val PREF_EMOJI_RECENT_KEYS: String = "emoji_recent_keys"
        const val PREF_EMOJI_CATEGORY_LAST_TYPED_ID: String = "emoji_category_last_typed_id"
        const val PREF_LAST_SHOWN_EMOJI_CATEGORY_ID: String = "last_shown_emoji_category_id"

        private const val UNDEFINED_PREFERENCE_VALUE_FLOAT = -1.0f
        private const val UNDEFINED_PREFERENCE_VALUE_INT = -1

        val instance: Settings = Settings()

        fun init(context: Context) {
            instance.onCreate(context)
        }

        fun readScreenMetrics(res: Resources): Int {
            return res.getInteger(R.integer.config_screen_metrics)
        }

        // Accessed from the settings interface, hence public
        fun readKeypressSoundEnabled(
            prefs: SharedPreferences,
            res: Resources
        ): Boolean {
            return prefs.getBoolean(
                PREF_SOUND_ON,
                res.getBoolean(R.bool.config_default_sound_enabled)
            )
        }

        fun readVibrationEnabled(
            prefs: SharedPreferences,
            res: Resources
        ): Boolean {
            val hasVibrator: Boolean =
                AudioAndHapticFeedbackManager.instance.hasVibrator()
            return hasVibrator && prefs.getBoolean(
                PREF_VIBRATE_ON,
                res.getBoolean(R.bool.config_default_vibration_enabled)
            )
        }

        fun readAutoCorrectEnabled(
            prefs: SharedPreferences,
            res: Resources?
        ): Boolean {
            return prefs.getBoolean(PREF_AUTO_CORRECTION, true)
        }

        fun readPlausibilityThreshold(res: Resources): Float {
            return res.getString(R.string.plausibility_threshold).toFloat()
        }

        fun readBlockPotentiallyOffensive(
            prefs: SharedPreferences,
            res: Resources
        ): Boolean {
            return prefs.getBoolean(
                PREF_BLOCK_POTENTIALLY_OFFENSIVE,
                res.getBoolean(R.bool.config_block_potentially_offensive)
            )
        }

        fun readFromBuildConfigIfGestureInputEnabled(res: Resources): Boolean {
            return res.getBoolean(R.bool.config_gesture_input_enabled_by_build_config)
        }

        fun readGestureInputEnabled(
            prefs: SharedPreferences,
            res: Resources
        ): Boolean {
            return readFromBuildConfigIfGestureInputEnabled(res)
                    && prefs.getBoolean(PREF_GESTURE_INPUT, true)
        }

        fun readFromBuildConfigIfToShowKeyPreviewPopupOption(res: Resources): Boolean {
            return res.getBoolean(R.bool.config_enable_show_key_preview_popup_option)
        }

        fun readKeyPreviewPopupEnabled(
            prefs: SharedPreferences,
            res: Resources
        ): Boolean {
            val defaultKeyPreviewPopup = res.getBoolean(
                R.bool.config_default_key_preview_popup
            )
            if (!readFromBuildConfigIfToShowKeyPreviewPopupOption(res)) {
                return defaultKeyPreviewPopup
            }
            return prefs.getBoolean(PREF_POPUP_ON, defaultKeyPreviewPopup)
        }

        fun readKeyPreviewPopupDismissDelay(
            prefs: SharedPreferences,
            res: Resources
        ): Int {
            return prefs.getString(
                PREF_KEY_PREVIEW_POPUP_DISMISS_DELAY,
                res.getInteger(
                    R.integer.config_key_preview_linger_timeout
                ).toString()
            )!!.toInt()
        }

        fun readShowsLanguageSwitchKey(prefs: SharedPreferences): Boolean {
            if (prefs.contains(PREF_SUPPRESS_LANGUAGE_SWITCH_KEY)) {
                val suppressLanguageSwitchKey = prefs.getBoolean(
                    PREF_SUPPRESS_LANGUAGE_SWITCH_KEY, false
                )
                val editor = prefs.edit()
                editor.remove(PREF_SUPPRESS_LANGUAGE_SWITCH_KEY)
                editor.putBoolean(PREF_SHOW_LANGUAGE_SWITCH_KEY, !suppressLanguageSwitchKey)
                editor.apply()
            }
            return prefs.getBoolean(PREF_SHOW_LANGUAGE_SWITCH_KEY, true)
        }

        fun readPrefAdditionalSubtypes(
            prefs: SharedPreferences,
            res: Resources
        ): String {
            val predefinedPrefSubtypes = AdditionalSubtypeUtils.createPrefSubtypes(
                res.getStringArray(R.array.predefined_subtypes)
            )
            return prefs.getString(PREF_CUSTOM_INPUT_STYLES, predefinedPrefSubtypes)!!
        }

        fun writePrefAdditionalSubtypes(
            prefs: SharedPreferences,
            prefSubtypes: String?
        ) {
            prefs.edit().putString(PREF_CUSTOM_INPUT_STYLES, prefSubtypes).apply()
        }

        fun readKeypressSoundVolume(
            prefs: SharedPreferences,
            res: Resources
        ): Float {
            val volume = prefs.getFloat(
                PREF_KEYPRESS_SOUND_VOLUME, UNDEFINED_PREFERENCE_VALUE_FLOAT
            )
            return if ((volume != UNDEFINED_PREFERENCE_VALUE_FLOAT))
                volume
            else
                readDefaultKeypressSoundVolume(res)
        }

        // Default keypress sound volume for unknown devices.
        // The negative value means system default.
        private val DEFAULT_KEYPRESS_SOUND_VOLUME: String = (-1.0f).toString()

        fun readDefaultKeypressSoundVolume(res: Resources): Float {
            return ResourceUtils.getDeviceOverrideValue(
                res,
                R.array.keypress_volumes,
                DEFAULT_KEYPRESS_SOUND_VOLUME
            )!!.toFloat()
        }

        fun readKeyLongpressTimeout(
            prefs: SharedPreferences,
            res: Resources
        ): Int {
            val milliseconds = prefs.getInt(
                PREF_KEY_LONGPRESS_TIMEOUT, UNDEFINED_PREFERENCE_VALUE_INT
            )
            return if ((milliseconds != UNDEFINED_PREFERENCE_VALUE_INT))
                milliseconds
            else
                readDefaultKeyLongpressTimeout(res)
        }

        fun readDefaultKeyLongpressTimeout(res: Resources): Int {
            return res.getInteger(R.integer.config_default_longpress_key_timeout)
        }

        fun readKeypressVibrationDuration(
            prefs: SharedPreferences,
            res: Resources
        ): Int {
            val milliseconds = prefs.getInt(
                PREF_VIBRATION_DURATION_SETTINGS, UNDEFINED_PREFERENCE_VALUE_INT
            )
            return if ((milliseconds != UNDEFINED_PREFERENCE_VALUE_INT))
                milliseconds
            else
                readDefaultKeypressVibrationDuration(res)
        }

        // Default keypress vibration duration for unknown devices.
        // The negative value means system default.
        private val DEFAULT_KEYPRESS_VIBRATION_DURATION: String = (-1).toString()

        fun readDefaultKeypressVibrationDuration(res: Resources): Int {
            return ResourceUtils.getDeviceOverrideValue(
                res,
                R.array.keypress_vibration_durations,
                DEFAULT_KEYPRESS_VIBRATION_DURATION
            )!!.toInt()
        }

        fun readKeyPreviewAnimationScale(
            prefs: SharedPreferences,
            prefKey: String?, defaultValue: Float
        ): Float {
            val fraction = prefs.getFloat(prefKey, UNDEFINED_PREFERENCE_VALUE_FLOAT)
            return if ((fraction != UNDEFINED_PREFERENCE_VALUE_FLOAT)) fraction else defaultValue
        }

        fun readKeyPreviewAnimationDuration(
            prefs: SharedPreferences,
            prefKey: String?, defaultValue: Int
        ): Int {
            val milliseconds = prefs.getInt(prefKey, UNDEFINED_PREFERENCE_VALUE_INT)
            return if ((milliseconds != UNDEFINED_PREFERENCE_VALUE_INT)) milliseconds else defaultValue
        }

        fun readKeyboardHeight(
            prefs: SharedPreferences,
            defaultValue: Float
        ): Float {
            val percentage = prefs.getFloat(
                DebugSettings.PREF_KEYBOARD_HEIGHT_SCALE, UNDEFINED_PREFERENCE_VALUE_FLOAT
            )
            return if ((percentage != UNDEFINED_PREFERENCE_VALUE_FLOAT)) percentage else defaultValue
        }

        fun readUseFullscreenMode(res: Resources): Boolean {
            return res.getBoolean(R.bool.config_use_fullscreen_mode)
        }

        fun readShowSetupWizardIcon(
            prefs: SharedPreferences,
            context: Context
        ): Boolean {
            if (!prefs.contains(PREF_SHOW_SETUP_WIZARD_ICON)) {
                val appInfo = context.applicationInfo
                val isApplicationInSystemImage =
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                // Default value
                return !isApplicationInSystemImage
            }
            return prefs.getBoolean(PREF_SHOW_SETUP_WIZARD_ICON, false)
        }

        fun readHasHardwareKeyboard(conf: Configuration): Boolean {
            // The standard way of finding out whether we have a hardware keyboard. This code is taken
            // from InputMethodService#onEvaluateInputShown, which canonically determines this.
            // In a nutshell, we have a keyboard if the configuration says the type of hardware keyboard
            // is NOKEYS and if it's not hidden (e.g. folded inside the device).
            return conf.keyboard != Configuration.KEYBOARD_NOKEYS
                    && conf.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_YES
        }

        fun isInternal(prefs: SharedPreferences): Boolean {
            return prefs.getBoolean(PREF_KEY_IS_INTERNAL, false)
        }

        fun writeEmojiRecentKeys(prefs: SharedPreferences, str: String?) {
            prefs.edit().putString(PREF_EMOJI_RECENT_KEYS, str).apply()
        }

        fun readEmojiRecentKeys(prefs: SharedPreferences): String {
            return prefs.getString(PREF_EMOJI_RECENT_KEYS, "")!!
        }

        fun writeLastTypedEmojiCategoryPageId(
            prefs: SharedPreferences, categoryId: Int, categoryPageId: Int
        ) {
            val key = PREF_EMOJI_CATEGORY_LAST_TYPED_ID + categoryId
            prefs.edit().putInt(key, categoryPageId).apply()
        }

        fun readLastTypedEmojiCategoryPageId(
            prefs: SharedPreferences, categoryId: Int
        ): Int {
            val key = PREF_EMOJI_CATEGORY_LAST_TYPED_ID + categoryId
            return prefs.getInt(key, 0)
        }

        fun writeLastShownEmojiCategoryId(
            prefs: SharedPreferences, categoryId: Int
        ) {
            prefs.edit().putInt(PREF_LAST_SHOWN_EMOJI_CATEGORY_ID, categoryId).apply()
        }

        fun readLastShownEmojiCategoryId(
            prefs: SharedPreferences, defValue: Int
        ): Int {
            return prefs.getInt(PREF_LAST_SHOWN_EMOJI_CATEGORY_ID, defValue)
        }
    }
}
