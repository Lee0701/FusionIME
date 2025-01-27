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
package com.android.inputmethod.latin.personalization

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.util.Patterns

object AccountUtils {
    private fun getAccounts(context: Context): Array<Account> {
        return AccountManager.get(context).getAccounts()
    }

    fun getDeviceAccountsEmailAddresses(context: Context): List<String> {
        val retval: ArrayList<String> = ArrayList()
        for (account: Account in getAccounts(context)) {
            val name: String = account.name
            if (Patterns.EMAIL_ADDRESS.matcher(name).matches()) {
                retval.add(name)
                retval.add(
                    name.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        .get(0)
                )
            }
        }
        return retval
    }

    /**
     * Get all device accounts having specified domain name.
     * @param context application context
     * @param domain domain name used for filtering
     * @return List of account names that contain the specified domain name
     */
    fun getDeviceAccountsWithDomain(
        context: Context, domain: String
    ): List<String> {
        val retval: ArrayList<String> = ArrayList()
        val atDomain: String = "@" + domain.lowercase()
        for (account: Account in getAccounts(context)) {
            if (account.name.lowercase().endsWith(atDomain)) {
                retval.add(account.name)
            }
        }
        return retval
    }
}
