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

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.content.Context
import android.os.Bundle
import android.os.Handler
import java.io.IOException

/**
 * Utility class that handles generation/invalidation of auth tokens in the app.
 */
class AuthUtils(context: Context?) {
    private val mAccountManager: AccountManager = AccountManager.get(context)

    /**
     * @see AccountManager.invalidateAuthToken
     */
    fun invalidateAuthToken(accountType: String?, authToken: String?) {
        mAccountManager.invalidateAuthToken(accountType, authToken)
    }

    /**
     * @see AccountManager.getAuthToken
     */
    fun getAuthToken(
        account: Account?,
        authTokenType: String?, options: Bundle?, notifyAuthFailure: Boolean,
        callback: AccountManagerCallback<Bundle?>?, handler: Handler?
    ): AccountManagerFuture<Bundle> {
        return mAccountManager.getAuthToken(
            account, authTokenType, options, notifyAuthFailure,
            callback, handler
        )
    }

    /**
     * @see AccountManager.blockingGetAuthToken
     */
    @Throws(
        OperationCanceledException::class,
        AuthenticatorException::class,
        IOException::class
    )
    fun blockingGetAuthToken(
        account: Account?, authTokenType: String?,
        notifyAuthFailure: Boolean
    ): String {
        return mAccountManager.blockingGetAuthToken(account, authTokenType, notifyAuthFailure)
    }
}
