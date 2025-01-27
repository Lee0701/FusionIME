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

import android.Manifest.permission
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.preference.Preference
import android.preference.SwitchPreference
import android.text.TextUtils
import com.android.inputmethod.dictionarypack.DictionarySettingsActivity
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.permissions.PermissionsManager
import com.android.inputmethod.latin.permissions.PermissionsManager.PermissionsResultCallback
import com.android.inputmethod.latin.permissions.PermissionsUtil
import com.android.inputmethod.latin.userdictionary.UserDictionaryList
import com.android.inputmethod.latin.userdictionary.UserDictionarySettings
import java.util.TreeSet

/**
 * "Text correction" settings sub screen.
 *
 * This settings sub screen handles the following text correction preferences.
 * - Personal dictionary
 * - Add-on dictionaries
 * - Block offensive words
 * - Auto-correction
 * - Show correction suggestions
 * - Personalized suggestions
 * - Suggest Contact names
 * - Next-word suggestions
 */
class CorrectionSettingsFragment : SubScreenFragment(), OnSharedPreferenceChangeListener,
    PermissionsResultCallback {
    private var mUseContactsPreference: SwitchPreference? = null

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        addPreferencesFromResource(R.xml.prefs_screen_correction)

        val context: Context = activity
        val pm = context.packageManager

        val dictionaryLink = findPreference(Settings.Companion.PREF_CONFIGURE_DICTIONARIES_KEY)
        val intent = dictionaryLink.intent
        intent.setClassName(context.packageName, DictionarySettingsActivity::class.java.name)
        val number = pm.queryIntentActivities(intent, 0).size
        if (0 >= number) {
            removePreference(Settings.Companion.PREF_CONFIGURE_DICTIONARIES_KEY)
        }

        val editPersonalDictionary =
            findPreference(Settings.Companion.PREF_EDIT_PERSONAL_DICTIONARY)
        val editPersonalDictionaryIntent = editPersonalDictionary.intent
        val ri = if (USE_INTERNAL_PERSONAL_DICTIONARY_SETTINGS)
            null
        else
            pm.resolveActivity(
                editPersonalDictionaryIntent, PackageManager.MATCH_DEFAULT_ONLY
            )
        if (ri == null) {
            overwriteUserDictionaryPreference(editPersonalDictionary)
        }

        mUseContactsPreference =
            findPreference(Settings.Companion.PREF_KEY_USE_CONTACTS_DICT) as SwitchPreference
        turnOffUseContactsIfNoPermission()
    }

    private fun overwriteUserDictionaryPreference(userDictionaryPreference: Preference) {
        val activity = activity
        val localeList: TreeSet<String?> =
            UserDictionaryList.Companion.getUserDictionaryLocalesSet(activity)
        if (null == localeList) {
            // The locale list is null if and only if the user dictionary service is
            // not present or disabled. In this case we need to remove the preference.
            preferenceScreen.removePreference(userDictionaryPreference)
        } else if (localeList.size <= 1) {
            userDictionaryPreference.fragment = UserDictionarySettings::class.java.name
            // If the size of localeList is 0, we don't set the locale parameter in the
            // extras. This will be interpreted by the UserDictionarySettings class as
            // meaning "the current locale".
            // Note that with the current code for UserDictionaryList#getUserDictionaryLocalesSet()
            // the locale list always has at least one element, since it always includes the current
            // locale explicitly. @see UserDictionaryList.getUserDictionaryLocalesSet().
            if (localeList.size == 1) {
                val locale = localeList.toTypedArray()[0] as String
                userDictionaryPreference.extras.putString("locale", locale)
            }
        } else {
            userDictionaryPreference.fragment = UserDictionaryList::class.java.name
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (!TextUtils.equals(key, Settings.Companion.PREF_KEY_USE_CONTACTS_DICT)) {
            return
        }
        if (!sharedPreferences.getBoolean(key, false)) {
            // don't care if the preference is turned off.
            return
        }

        // Check for permissions.
        if (PermissionsUtil.checkAllPermissionsGranted(
                activity,  /* context */permission.READ_CONTACTS
            )
        ) {
            return  // all permissions granted, no need to request permissions.
        }

        PermissionsManager.Companion.get(activity /* context */)!!.requestPermissions(
            this,  /* PermissionsResultCallback */
            activity,  /* activity */
            permission.READ_CONTACTS
        )
    }

    override fun onRequestPermissionsResult(allGranted: Boolean) {
        turnOffUseContactsIfNoPermission()
    }

    private fun turnOffUseContactsIfNoPermission() {
        if (!PermissionsUtil.checkAllPermissionsGranted(
                activity, permission.READ_CONTACTS
            )
        ) {
            mUseContactsPreference!!.isChecked = false
        }
    }

    companion object {
        private const val DBG_USE_INTERNAL_PERSONAL_DICTIONARY_SETTINGS = false
        private val USE_INTERNAL_PERSONAL_DICTIONARY_SETTINGS =
            DBG_USE_INTERNAL_PERSONAL_DICTIONARY_SETTINGS
                    || Build.VERSION.SDK_INT <= VERSION_CODES.JELLY_BEAN_MR2
    }
}
