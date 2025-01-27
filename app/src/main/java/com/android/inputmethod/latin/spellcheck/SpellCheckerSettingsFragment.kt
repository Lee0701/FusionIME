/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.inputmethod.latin.spellcheck

import android.Manifest.permission
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.preference.PreferenceScreen
import android.preference.SwitchPreference
import android.text.TextUtils
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.permissions.PermissionsManager
import com.android.inputmethod.latin.permissions.PermissionsManager.PermissionsResultCallback
import com.android.inputmethod.latin.permissions.PermissionsUtil
import com.android.inputmethod.latin.settings.SubScreenFragment
import com.android.inputmethod.latin.settings.TwoStatePreferenceHelper
import com.android.inputmethod.latin.utils.ApplicationUtils

/**
 * Preference screen.
 */
class SpellCheckerSettingsFragment : SubScreenFragment(), OnSharedPreferenceChangeListener,
    PermissionsResultCallback {
    private var mLookupContactsPreference: SwitchPreference? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        addPreferencesFromResource(R.xml.spell_checker_settings)
        val preferenceScreen: PreferenceScreen = getPreferenceScreen()
        preferenceScreen.setTitle(
            ApplicationUtils.getActivityTitleResId(
                getActivity(), SpellCheckerSettingsActivity::class.java
            )
        )
        TwoStatePreferenceHelper.replaceCheckBoxPreferencesBySwitchPreferences(preferenceScreen)

        mLookupContactsPreference = findPreference(
            AndroidSpellCheckerService.Companion.PREF_USE_CONTACTS_KEY
        ) as SwitchPreference?
        turnOffLookupContactsIfNoPermission()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (!TextUtils.equals(key, AndroidSpellCheckerService.Companion.PREF_USE_CONTACTS_KEY)) {
            return
        }

        if (!sharedPreferences.getBoolean(key, false)) {
            // don't care if the preference is turned off.
            return
        }

        // Check for permissions.
        if (PermissionsUtil.checkAllPermissionsGranted(
                getActivity(),  /* context */permission.READ_CONTACTS
            )
        ) {
            return  // all permissions granted, no need to request permissions.
        }

        PermissionsManager.Companion.get(getActivity() /* context */)!!.requestPermissions(
            this,  /* PermissionsResultCallback */
            getActivity(),  /* activity */permission.READ_CONTACTS
        )
    }

    override fun onRequestPermissionsResult(allGranted: Boolean) {
        turnOffLookupContactsIfNoPermission()
    }

    private fun turnOffLookupContactsIfNoPermission() {
        if (!PermissionsUtil.checkAllPermissionsGranted(
                getActivity(), permission.READ_CONTACTS
            )
        ) {
            mLookupContactsPreference!!.setChecked(false)
        }
    }
}
