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
import android.content.Intent
import android.os.Bundle
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback


/**
 * An activity to help request permissions. It's used when no other activity is available, e.g. in
 * InputMethodService. This activity assumes that all permissions are not granted yet.
 */
class PermissionsActivity

    : Activity(), OnRequestPermissionsResultCallback {
    private var mPendingRequestCode: Int = INVALID_REQUEST_CODE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPendingRequestCode = if ((savedInstanceState != null))
            savedInstanceState.getInt(EXTRA_PERMISSION_REQUEST_CODE, INVALID_REQUEST_CODE)
        else
            INVALID_REQUEST_CODE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_PERMISSION_REQUEST_CODE, mPendingRequestCode)
    }

    override fun onResume() {
        super.onResume()
        // Only do request when there is no pending request to avoid duplicated requests.
        if (mPendingRequestCode == INVALID_REQUEST_CODE) {
            val extras: Bundle? = getIntent().getExtras()
            val permissionsToRequest: Array<String?>? =
                extras!!.getStringArray(EXTRA_PERMISSION_REQUESTED_PERMISSIONS)
            mPendingRequestCode = extras.getInt(EXTRA_PERMISSION_REQUEST_CODE)
            // Assuming that all supplied permissions are not granted yet, so that we don't need to
            // check them again.
            PermissionsUtil.requestPermissions(
                this, mPendingRequestCode,
                permissionsToRequest!!
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        mPendingRequestCode = INVALID_REQUEST_CODE
        PermissionsManager.Companion.get(this)!!.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )
        finish()
    }

    companion object {
        /**
         * Key to retrieve requested permissions from the intent.
         */
        const val EXTRA_PERMISSION_REQUESTED_PERMISSIONS: String = "requested_permissions"

        /**
         * Key to retrieve request code from the intent.
         */
        const val EXTRA_PERMISSION_REQUEST_CODE: String = "request_code"

        private val INVALID_REQUEST_CODE: Int = -1

        /**
         * Starts a PermissionsActivity and checks/requests supplied permissions.
         */
        fun run(
            context: Context, requestCode: Int, vararg permissionStrings: String
        ) {
            val intent: Intent = Intent(
                context.getApplicationContext(),
                PermissionsActivity::class.java
            )
            intent.putExtra(EXTRA_PERMISSION_REQUESTED_PERMISSIONS, permissionStrings)
            intent.putExtra(EXTRA_PERMISSION_REQUEST_CODE, requestCode)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            context.startActivity(intent)
        }
    }
}
