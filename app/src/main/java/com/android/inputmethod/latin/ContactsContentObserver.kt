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
package com.android.inputmethod.latin

import android.Manifest.permission
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.SystemClock
import android.provider.ContactsContract
import android.util.Log
import com.android.inputmethod.latin.ContactsManager.ContactsChangedListener
import com.android.inputmethod.latin.define.DebugFlags
import com.android.inputmethod.latin.permissions.PermissionsUtil
import com.android.inputmethod.latin.utils.ExecutorUtils
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A content observer that listens to updates to content provider [Contacts.CONTENT_URI].
 */
class ContactsContentObserver(manager: ContactsManager, context: Context) : Runnable {
    private val mContext: Context
    private val mManager: ContactsManager
    private val mRunning: AtomicBoolean = AtomicBoolean(false)

    private var mContentObserver: ContentObserver? = null
    private var mContactsChangedListener: ContactsChangedListener? = null

    init {
        mManager = manager
        mContext = context
    }

    fun registerObserver(listener: ContactsChangedListener?) {
        if (!PermissionsUtil.checkAllPermissionsGranted(
                mContext, permission.READ_CONTACTS
            )
        ) {
            Log.i(TAG, "No permission to read contacts. Not registering the observer.")
            // do nothing if we do not have the permission to read contacts.
            return
        }

        if (DebugFlags.DEBUG_ENABLED) {
            Log.d(TAG, "registerObserver()")
        }
        mContactsChangedListener = listener
        mContentObserver = object : ContentObserver(null /* handler */) {
            override fun onChange(self: Boolean) {
                ExecutorUtils.getBackgroundExecutor(ExecutorUtils.KEYBOARD)
                    ?.execute(this@ContactsContentObserver)
            }
        }
        val contentResolver: ContentResolver = mContext.getContentResolver()
        contentResolver.registerContentObserver(
            ContactsContract.Contacts.CONTENT_URI,
            true,
            mContentObserver!!
        )
    }

    override fun run() {
        if (!PermissionsUtil.checkAllPermissionsGranted(
                mContext, permission.READ_CONTACTS
            )
        ) {
            Log.i(TAG, "No permission to read contacts. Not updating the contacts.")
            unregister()
            return
        }

        if (!mRunning.compareAndSet(false,  /* expect */true /* update */)) {
            if (DebugFlags.DEBUG_ENABLED) {
                Log.d(TAG, "run() : Already running. Don't waste time checking again.")
            }
            return
        }
        if (haveContentsChanged()) {
            if (DebugFlags.DEBUG_ENABLED) {
                Log.d(TAG, "run() : Contacts have changed. Notifying listeners.")
            }
            mContactsChangedListener!!.onContactsChange()
        }
        mRunning.set(false)
    }

    fun haveContentsChanged(): Boolean {
        if (!PermissionsUtil.checkAllPermissionsGranted(
                mContext, permission.READ_CONTACTS
            )
        ) {
            Log.i(TAG, "No permission to read contacts. Marking contacts as not changed.")
            return false
        }

        val startTime: Long = SystemClock.uptimeMillis()
        val contactCount: Int = mManager.contactCount
        if (contactCount > ContactsDictionaryConstants.MAX_CONTACTS_PROVIDER_QUERY_LIMIT) {
            // If there are too many contacts then return false. In this rare case it is impossible
            // to include all of them anyways and the cost of rebuilding the dictionary is too high.
            // TODO: Sort and check only the most recent contacts?
            return false
        }
        if (contactCount != mManager.contactCountAtLastRebuild) {
            if (DebugFlags.DEBUG_ENABLED) {
                Log.d(
                    TAG, ("haveContentsChanged() : Count changed from "
                            + mManager.contactCountAtLastRebuild + " to " + contactCount)
                )
            }
            return true
        }
        val names: ArrayList<String> =
            mManager.getValidNames(ContactsContract.Contacts.CONTENT_URI)
        if (names.hashCode() != mManager.hashCodeAtLastRebuild) {
            return true
        }
        if (DebugFlags.DEBUG_ENABLED) {
            Log.d(
                TAG, ("haveContentsChanged() : No change detected in "
                        + (SystemClock.uptimeMillis() - startTime) + " ms)")
            )
        }
        return false
    }

    fun unregister() {
        mContext.getContentResolver().unregisterContentObserver(mContentObserver!!)
    }

    companion object {
        private const val TAG: String = "ContactsContentObserver"
    }
}
