/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.inputmethod.latin.permissions

import android.app.Activity
import android.content.Context

/**
 * Manager to perform permission related tasks. Always call on the UI thread.
 */
class PermissionsManager(context: Context) {
    interface PermissionsResultCallback {
        fun onRequestPermissionsResult(allGranted: Boolean)
    }

    private var mRequestCodeId: Int = 0

    private val mContext: Context
    private val mRequestIdToCallback: MutableMap<Int, PermissionsResultCallback> = HashMap()

    init {
        mContext = context
    }

    @get:Synchronized
    private val nextRequestId: Int
        get() {
            return ++mRequestCodeId
        }


    @Synchronized
    fun requestPermissions(
        callback: PermissionsResultCallback,
        activity: Activity?,
        vararg permissionsToRequest: String
    ) {
        val deniedPermissions: List<String> = PermissionsUtil.getDeniedPermissions(
            mContext, *permissionsToRequest
        )
        if (deniedPermissions.isEmpty()) {
            return
        }
        // otherwise request the permissions.
        val requestId: Int = nextRequestId
        val permissionsArray: Array<String> = deniedPermissions.toTypedArray()

        mRequestIdToCallback.put(requestId, callback)
        if (activity != null) {
            PermissionsUtil.requestPermissions(activity, requestId, permissionsArray)
        } else {
            PermissionsActivity.run(mContext, requestId, *permissionsArray)
        }
    }

    @Synchronized
    fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>?, grantResults: IntArray
    ) {
        val permissionsResultCallback: PermissionsResultCallback? =
            mRequestIdToCallback.get(requestCode)
        mRequestIdToCallback.remove(requestCode)

        val allGranted: Boolean = PermissionsUtil.allGranted(grantResults)
        permissionsResultCallback!!.onRequestPermissionsResult(allGranted)
    }

    companion object {
        private var sInstance: PermissionsManager? = null

        @Synchronized
        fun get(context: Context): PermissionsManager {
            if (sInstance == null) {
                sInstance = PermissionsManager(context)
            }
            return sInstance!!
        }
    }
}
