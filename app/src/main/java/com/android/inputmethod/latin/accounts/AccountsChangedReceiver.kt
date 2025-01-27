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
package com.android.inputmethod.latin.accounts

import android.accounts.AccountManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.Log
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.latin.settings.LocalSettingsConstants

/**
 * [BroadcastReceiver] for [AccountManager.LOGIN_ACCOUNTS_CHANGED_ACTION].
 */
class AccountsChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (AccountManager.LOGIN_ACCOUNTS_CHANGED_ACTION != intent.action) {
            Log.w(
                TAG,
                "Received unknown broadcast: $intent"
            )
            return
        }

        // Ideally the account preference could live in a different preferences file
        // that wasn't being backed up and restored, however the preference fragments
        // currently only deal with the default shared preferences which is why
        // separating this out into a different file is not trivial currently.
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val currentAccount = prefs.getString(
            LocalSettingsConstants.PREF_ACCOUNT_NAME, null
        )
        removeUnknownAccountFromPreference(prefs, getAccountsForLogin(context), currentAccount)
    }

    /**
     * Helper method to help test this receiver.
     */
    @UsedForTesting
    protected fun getAccountsForLogin(context: Context?): Array<String?> {
        return LoginAccountUtils.getAccountsForLogin(context)
    }

    companion object {
        const val TAG: String = "AccountsChangedReceiver"

        /**
         * Removes the currentAccount from preferences if it's not found
         * in the list of current accounts.
         */
        private fun removeUnknownAccountFromPreference(
            prefs: SharedPreferences,
            accounts: Array<String?>, currentAccount: String?
        ) {
            if (currentAccount == null) {
                return
            }
            for (account in accounts) {
                if (TextUtils.equals(currentAccount, account)) {
                    return
                }
            }
            Log.i(
                TAG,
                "The current account was removed from the system: $currentAccount"
            )
            prefs.edit()
                .remove(LocalSettingsConstants.PREF_ACCOUNT_NAME)
                .apply()
        }
    }
}
