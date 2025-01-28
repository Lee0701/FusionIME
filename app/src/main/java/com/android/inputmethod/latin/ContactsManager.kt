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

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.ContactsContract
import android.text.TextUtils
import android.util.Log
import com.android.inputmethod.latin.common.Constants
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Manages all interactions with Contacts DB.
 *
 * The manager provides an API for listening to meaning full updates by keeping a
 * measure of the current state of the content provider.
 */
class ContactsManager(context: Context) {
    protected class RankedContact internal constructor(cursor: Cursor) {
        val mName: String
        val mLastContactedTime: Long
        val mTimesContacted: Int
        val mInVisibleGroup: Boolean

        var affinity: Float = 0.0f
            private set

        init {
            mName = cursor.getString(
                ContactsDictionaryConstants.NAME_INDEX
            )
            mTimesContacted = cursor.getInt(
                ContactsDictionaryConstants.TIMES_CONTACTED_INDEX
            )
            mLastContactedTime = cursor.getLong(
                ContactsDictionaryConstants.LAST_TIME_CONTACTED_INDEX
            )
            mInVisibleGroup = cursor.getInt(
                ContactsDictionaryConstants.IN_VISIBLE_GROUP_INDEX
            ) == 1
        }

        /**
         * Calculates the affinity with the contact based on:
         * - How many times it has been contacted
         * - How long since the last contact.
         * - Whether the contact is in the visible group (i.e., Contacts list).
         *
         * Note: This affinity is limited by the fact that some apps currently do not update the
         * LAST_TIME_CONTACTED or TIMES_CONTACTED counters. As a result, a frequently messaged
         * contact may still have 0 affinity.
         */
        fun computeAffinity(maxTimesContacted: Int, currentTime: Long) {
            val timesWeight: Float = (mTimesContacted.toFloat() + 1) / (maxTimesContacted + 1)
            val timeSinceLastContact: Long = min(
                max(0.0, (currentTime - mLastContactedTime).toDouble()),
                TimeUnit.MILLISECONDS.convert(
                    180,
                    TimeUnit.DAYS
                )
                    .toDouble()
            ).toLong()
            val lastTimeWeight: Float = 0.5.pow(
                (timeSinceLastContact / (TimeUnit.MILLISECONDS.convert(
                    10,
                    TimeUnit.DAYS
                ))).toDouble()
            ) as Float
            val visibleWeight: Float = if (mInVisibleGroup) 1.0f else 0.0f
            affinity = (timesWeight + lastTimeWeight + visibleWeight) / 3
        }
    }

    private class AffinityComparator : Comparator<RankedContact> {
        override fun compare(contact1: RankedContact, contact2: RankedContact): Int {
            return java.lang.Float.compare(contact2.affinity, contact1.affinity)
        }
    }

    /**
     * Interface to implement for classes interested in getting notified for updates
     * to Contacts content provider.
     */
    interface ContactsChangedListener {
        fun onContactsChange()
    }

    /**
     * The number of contacts observed in the most recent instance of
     * contacts content provider.
     */
    private val mContactCountAtLastRebuild: AtomicInteger = AtomicInteger(0)

    /**
     * The hash code of list of valid contacts names in the most recent dictionary
     * rebuild.
     */
    private val mHashCodeAtLastRebuild: AtomicInteger = AtomicInteger(0)

    private val mContext: Context
    private val mObserver: ContactsContentObserver

    init {
        mContext = context
        mObserver = ContactsContentObserver(this,  /* ContactsManager */context)
    }

    // TODO: This was synchronized in previous version. Why?
    fun registerForUpdates(listener: ContactsChangedListener?) {
        mObserver.registerObserver(listener)
    }

    val contactCountAtLastRebuild: Int
        get() {
            return mContactCountAtLastRebuild.get()
        }

    val hashCodeAtLastRebuild: Int
        get() {
            return mHashCodeAtLastRebuild.get()
        }

    /**
     * Returns all the valid names in the Contacts DB. Callers should also
     * call [.updateLocalState] after they are done with result
     * so that the manager can cache local state for determining updates.
     *
     * These names are sorted by their affinity to the user, with favorite
     * contacts appearing first.
     */
    fun getValidNames(uri: Uri): ArrayList<String> {
        // Check all contacts since it's not possible to find out which names have changed.
        // This is needed because it's possible to receive extraneous onChange events even when no
        // name has changed.
        val cursor: Cursor? = mContext.getContentResolver().query(
            uri,
            ContactsDictionaryConstants.PROJECTION, null, null, null
        )
        val contacts: ArrayList<RankedContact> = ArrayList()
        var maxTimesContacted: Int = 0
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    while (!cursor.isAfterLast()) {
                        val name: String = cursor.getString(
                            ContactsDictionaryConstants.NAME_INDEX
                        )
                        if (isValidName(name)) {
                            val timesContacted: Int = cursor.getInt(
                                ContactsDictionaryConstants.TIMES_CONTACTED_INDEX
                            )
                            if (timesContacted > maxTimesContacted) {
                                maxTimesContacted = timesContacted
                            }
                            contacts.add(RankedContact(cursor))
                        }
                        cursor.moveToNext()
                    }
                }
            } finally {
                cursor.close()
            }
        }
        val currentTime: Long = System.currentTimeMillis()
        for (contact: RankedContact in contacts) {
            contact.computeAffinity(maxTimesContacted, currentTime)
        }
        Collections.sort(contacts, AffinityComparator())
        val names: HashSet<String> = HashSet()
        var i: Int = 0
        while (i < contacts.size && names.size < MAX_CONTACT_NAMES) {
            names.add(contacts.get(i).mName)
            ++i
        }
        return ArrayList(names)
    }

    val contactCount: Int
        /**
         * Returns the number of contacts in contacts content provider.
         */
        get() {
            // TODO: consider switching to a rawQuery("select count(*)...") on the database if
            // performance is a bottleneck.
            var cursor: Cursor? = null
            try {
                cursor = mContext.getContentResolver().query(
                    ContactsContract.Contacts.CONTENT_URI,
                    ContactsDictionaryConstants.PROJECTION_ID_ONLY, null, null, null
                )
                if (null == cursor) {
                    return 0
                }
                return cursor.getCount()
            } catch (e: SQLiteException) {
                Log.e(
                    TAG,
                    "SQLiteException in the remote Contacts process.",
                    e
                )
            } finally {
                if (null != cursor) {
                    cursor.close()
                }
            }
            return 0
        }

    /**
     * Updates the local state of the manager. This should be called when the callers
     * are done with all the updates of the content provider successfully.
     */
    fun updateLocalState(names: ArrayList<String>) {
        mContactCountAtLastRebuild.set(contactCount)
        mHashCodeAtLastRebuild.set(names.hashCode())
    }

    /**
     * Performs any necessary cleanup.
     */
    fun close() {
        mObserver.unregister()
    }

    companion object {
        private const val TAG: String = "ContactsManager"

        /**
         * Use at most this many of the highest affinity contacts.
         */
        const val MAX_CONTACT_NAMES: Int = 200

        private fun isValidName(name: String): Boolean {
            if (TextUtils.isEmpty(name) || name.indexOf(Constants.CODE_COMMERCIAL_AT.toChar()) != -1) {
                return false
            }
            val hasSpace: Boolean = name.indexOf(Constants.CODE_SPACE.toChar()) != -1
            if (!hasSpace) {
                // Only allow an isolated word if it does not contain a hyphen.
                // This helps to filter out mailing lists.
                return name.indexOf(Constants.CODE_DASH.toChar()) == -1
            }
            return true
        }
    }
}
