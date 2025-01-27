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
package com.android.inputmethod.latin.utils

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object ApplicationUtils {
    private val TAG: String = ApplicationUtils::class.java.simpleName

    fun getActivityTitleResId(
        context: Context,
        cls: Class<out Activity?>
    ): Int {
        val cn = ComponentName(context, cls)
        try {
            val ai = context.packageManager.getActivityInfo(cn, 0)
            if (ai != null) {
                return ai.labelRes
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Failed to get settings activity title res id.", e)
        }
        return 0
    }

    /**
     * A utility method to get the application's PackageInfo.versionName
     * @return the application's PackageInfo.versionName
     */
    fun getVersionName(context: Context?): String? {
        try {
            if (context == null) {
                return ""
            }
            val packageName = context.packageName
            val info = context.packageManager.getPackageInfo(packageName, 0)
            return info.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Could not find version info.", e)
        }
        return ""
    }

    /**
     * A utility method to get the application's PackageInfo.versionCode
     * @return the application's PackageInfo.versionCode
     */
    fun getVersionCode(context: Context?): Int {
        try {
            if (context == null) {
                return 0
            }
            val packageName = context.packageName
            val info = context.packageManager.getPackageInfo(packageName, 0)
            return info.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Could not find version info.", e)
        }
        return 0
    }
}
