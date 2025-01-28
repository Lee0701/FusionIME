/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.util.LruCache
import com.android.inputmethod.compat.AppWorkaroundsUtils

class TargetPackageInfoGetterTask(
    context: Context?,
    result: AsyncResultHolder<AppWorkaroundsUtils>
) : AsyncTask<String?, Void?, PackageInfo?>() {
    private var mContext: Context?
    private val mResult: AsyncResultHolder<AppWorkaroundsUtils>

    init {
        mContext = context
        mResult = result
    }

    override fun doInBackground(vararg packageName: String?): PackageInfo? {
        val pm = mContext!!.packageManager
        mContext = null // Bazooka-powered anti-leak device
        try {
            val packageInfo = pm.getPackageInfo(packageName[0]!!, 0 /* flags */)
            sCache.put(packageName[0], packageInfo)
            return packageInfo
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
    }

    override fun onPostExecute(info: PackageInfo?) {
        mResult.set(AppWorkaroundsUtils(info))
    }

    companion object {
        private const val MAX_CACHE_ENTRIES = 64 // arbitrary
        private val sCache = LruCache<String, PackageInfo>(
            MAX_CACHE_ENTRIES
        )

        fun getCachedPackageInfo(packageName: String?): PackageInfo? {
            if (null == packageName) return null
            return sCache[packageName]
        }

        fun removeCachedPackageInfo(packageName: String) {
            sCache.remove(packageName)
        }
    }
}
