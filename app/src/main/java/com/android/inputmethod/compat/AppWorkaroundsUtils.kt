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
package com.android.inputmethod.compat

import android.content.pm.PackageInfo
import android.os.Build.VERSION_CODES

/**
 * A class to encapsulate work-arounds specific to particular apps.
 */
class AppWorkaroundsUtils(packageInfo: PackageInfo?) {
    private val mPackageInfo: PackageInfo? // May be null
    private val mIsBrokenByRecorrection: Boolean

    init {
        mPackageInfo = packageInfo
        mIsBrokenByRecorrection = AppWorkaroundsHelper.evaluateIsBrokenByRecorrection(
            packageInfo
        )
    }

    fun isBrokenByRecorrection(): Boolean {
        return mIsBrokenByRecorrection
    }

    fun isBeforeJellyBean(): Boolean {
        val applicationInfo = mPackageInfo?.applicationInfo ?: return false
        return applicationInfo.targetSdkVersion < VERSION_CODES.JELLY_BEAN
    }

    override fun toString(): String {
        val applicationInfo = mPackageInfo?.applicationInfo ?: return ""
        val s: StringBuilder = StringBuilder()
        s.append("Target application : ")
            .append(applicationInfo.name)
            .append("\nPackage : ")
            .append(applicationInfo.packageName)
            .append("\nTarget app sdk version : ")
            .append(applicationInfo.targetSdkVersion)
        return s.toString()
    }
}
