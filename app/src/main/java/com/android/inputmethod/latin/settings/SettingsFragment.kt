/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.Activity
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.Settings.Secure
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.define.ProductionFlags
import com.android.inputmethod.latin.utils.ApplicationUtils
import com.android.inputmethod.latin.utils.FeedbackUtils
import com.android.inputmethodcommon.InputMethodSettingsFragment

class SettingsFragment : InputMethodSettingsFragment() {
    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setHasOptionsMenu(true)
        setInputMethodSettingsCategoryTitle(R.string.language_selection_title)
        setSubtypeEnablerTitle(R.string.select_language)
        addPreferencesFromResource(R.xml.prefs)
        val preferenceScreen = preferenceScreen
        preferenceScreen.setTitle(
            ApplicationUtils.getActivityTitleResId(activity, SettingsActivity::class.java)
        )
        if (!ProductionFlags.ENABLE_ACCOUNT_SIGN_IN) {
            val accountsPreference = findPreference(Settings.SCREEN_ACCOUNTS)
            preferenceScreen.removePreference(accountsPreference)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (FeedbackUtils.isHelpAndFeedbackFormSupported) {
            menu.add(
                NO_MENU_GROUP, MENU_HELP_AND_FEEDBACK,  /* itemId */
                MENU_HELP_AND_FEEDBACK,  /* order */R.string.help_and_feedback
            )
        }
        val aboutResId = FeedbackUtils.aboutKeyboardTitleResId
        if (aboutResId != 0) {
            menu.add(NO_MENU_GROUP, MENU_ABOUT,  /* itemId */MENU_ABOUT,  /* order */aboutResId)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val activity = activity
        if (!isUserSetupComplete(activity)) {
            // If setup is not complete, it's not safe to launch Help or other activities
            // because they might go to the Play Store.  See b/19866981.
            return true
        }
        val itemId = item.itemId
        if (itemId == MENU_HELP_AND_FEEDBACK) {
            FeedbackUtils.showHelpAndFeedbackForm(activity)
            return true
        }
        if (itemId == MENU_ABOUT) {
            val aboutIntent = FeedbackUtils.getAboutKeyboardIntent(activity)
            if (aboutIntent != null) {
                startActivity(aboutIntent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        // We don't care about menu grouping.
        private const val NO_MENU_GROUP = Menu.NONE

        // The first menu item id and order.
        private const val MENU_ABOUT = Menu.FIRST

        // The second menu item id and order.
        private const val MENU_HELP_AND_FEEDBACK = Menu.FIRST + 1

        private fun isUserSetupComplete(activity: Activity): Boolean {
            if (Build.VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
                return true
            }
            return Secure.getInt(activity.contentResolver, "user_setup_complete", 0) != 0
        }
    }
}
