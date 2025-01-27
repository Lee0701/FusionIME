/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.inputmethod.compat

import android.content.Context
import android.os.Build.VERSION_CODES
import android.os.UserManager
import androidx.annotation.IntDef
import java.lang.reflect.Method

/**
 * A temporary solution until `UserManagerCompat.isUserUnlocked()` in the support-v4 library
 * becomes publicly available.
 */
object UserManagerCompatUtils {
    private var METHOD_isUserUnlocked: Method? = null

    init {
        // We do not try to search the method in Android M and prior.
        if (BuildCompatUtils.EFFECTIVE_SDK_INT <= VERSION_CODES.M) {
            METHOD_isUserUnlocked = null
        } else {
            METHOD_isUserUnlocked = CompatUtils.getMethod(UserManager::class.java, "isUserUnlocked")
        }
    }

    const val LOCK_STATE_UNKNOWN: Int = 0
    const val LOCK_STATE_UNLOCKED: Int = 1
    const val LOCK_STATE_LOCKED: Int = 2

    /**
     * Check if the calling user is running in an "unlocked" state. A user is unlocked only after
     * they've entered their credentials (such as a lock pattern or PIN), and credential-encrypted
     * private app data storage is available.
     * @param context context from which [UserManager] should be obtained.
     * @return One of [LockState].
     */
    @LockState
    fun getUserLockState(context: Context): Int {
        if (METHOD_isUserUnlocked == null) {
            return LOCK_STATE_UNKNOWN
        }
        val userManager: UserManager? = context.getSystemService(UserManager::class.java)
        if (userManager == null) {
            return LOCK_STATE_UNKNOWN
        }
        val result: Boolean? =
            CompatUtils.invoke(userManager, null, METHOD_isUserUnlocked) as Boolean?
        if (result == null) {
            return LOCK_STATE_UNKNOWN
        }
        return if (result) LOCK_STATE_UNLOCKED else LOCK_STATE_LOCKED
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef([LOCK_STATE_UNKNOWN, LOCK_STATE_UNLOCKED, LOCK_STATE_LOCKED])
    annotation class LockState
}
