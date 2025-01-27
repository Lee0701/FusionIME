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
package com.android.inputmethod.latin.utils

import android.Manifest.permission
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings.Secure
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils
import android.util.Log
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.permissions.PermissionsUtil
import com.android.inputmethod.latin.settings.SettingsValues
import java.util.concurrent.TimeUnit

object ImportantNoticeUtils {
    private val TAG: String = ImportantNoticeUtils::class.java.simpleName

    // {@link SharedPreferences} name to save the last important notice version that has been
    // displayed to users.
    private const val PREFERENCE_NAME = "important_notice_pref"

    private const val KEY_SUGGEST_CONTACTS_NOTICE = "important_notice_suggest_contacts"

    @UsedForTesting
    const val KEY_TIMESTAMP_OF_CONTACTS_NOTICE: String = "timestamp_of_suggest_contacts_notice"

    @UsedForTesting
    val TIMEOUT_OF_IMPORTANT_NOTICE: Long = TimeUnit.HOURS.toMillis(23)

    // Copy of the hidden {@link Settings.Secure#USER_SETUP_COMPLETE} settings key.
    // The value is zero until each multiuser completes system setup wizard.
    // Caveat: This is a hidden API.
    private const val Settings_Secure_USER_SETUP_COMPLETE = "user_setup_complete"
    private const val USER_SETUP_IS_NOT_COMPLETE = 0

    @UsedForTesting
    fun isInSystemSetupWizard(context: Context): Boolean {
        try {
            val userSetupComplete = Secure.getInt(
                context.contentResolver, Settings_Secure_USER_SETUP_COMPLETE
            )
            return userSetupComplete == USER_SETUP_IS_NOT_COMPLETE
        } catch (e: SettingNotFoundException) {
            Log.w(
                TAG, "Can't find settings in Settings.Secure: key="
                        + Settings_Secure_USER_SETUP_COMPLETE
            )
            return false
        }
    }

    @UsedForTesting
    fun getImportantNoticePreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    @UsedForTesting
    fun hasContactsNoticeShown(context: Context): Boolean {
        return getImportantNoticePreferences(context).getBoolean(
            KEY_SUGGEST_CONTACTS_NOTICE, false
        )
    }

    fun shouldShowImportantNotice(
        context: Context,
        settingsValues: SettingsValues
    ): Boolean {
        // Check to see whether "Use Contacts" is enabled by the user.
        if (!settingsValues.mUseContactsDict) {
            return false
        }

        if (hasContactsNoticeShown(context)) {
            return false
        }

        // Don't show the dialog if we have all the permissions.
        if (PermissionsUtil.checkAllPermissionsGranted(
                context, permission.READ_CONTACTS
            )
        ) {
            return false
        }

        val importantNoticeTitle = getSuggestContactsNoticeTitle(context)
        if (TextUtils.isEmpty(importantNoticeTitle)) {
            return false
        }
        if (isInSystemSetupWizard(context)) {
            return false
        }
        if (hasContactsNoticeTimeoutPassed(context, System.currentTimeMillis())) {
            updateContactsNoticeShown(context)
            return false
        }
        return true
    }

    fun getSuggestContactsNoticeTitle(context: Context): String {
        return context.resources.getString(R.string.important_notice_suggest_contact_names)
    }

    @UsedForTesting
    fun hasContactsNoticeTimeoutPassed(
        context: Context, currentTimeInMillis: Long
    ): Boolean {
        val prefs = getImportantNoticePreferences(context)
        if (!prefs.contains(KEY_TIMESTAMP_OF_CONTACTS_NOTICE)) {
            prefs.edit()
                .putLong(KEY_TIMESTAMP_OF_CONTACTS_NOTICE, currentTimeInMillis)
                .apply()
        }
        val firstDisplayTimeInMillis = prefs.getLong(
            KEY_TIMESTAMP_OF_CONTACTS_NOTICE, currentTimeInMillis
        )
        val elapsedTime = currentTimeInMillis - firstDisplayTimeInMillis
        return elapsedTime >= TIMEOUT_OF_IMPORTANT_NOTICE
    }

    fun updateContactsNoticeShown(context: Context) {
        getImportantNoticePreferences(context)
            .edit()
            .putBoolean(KEY_SUGGEST_CONTACTS_NOTICE, true)
            .remove(KEY_TIMESTAMP_OF_CONTACTS_NOTICE)
            .apply()
    }
}
