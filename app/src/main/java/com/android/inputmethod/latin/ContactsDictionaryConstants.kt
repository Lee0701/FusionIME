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
 * limitations under the License.
 */
package com.android.inputmethod.latin

import android.provider.BaseColumns
import android.provider.ContactsContract

/**
 * Constants related to Contacts Content Provider.
 */
object ContactsDictionaryConstants {
    /**
     * Projections for [Contacts.CONTENT_URI]
     */
    val PROJECTION: Array<String> = arrayOf(
        BaseColumns._ID,
        ContactsContract.Contacts.DISPLAY_NAME,
        ContactsContract.Contacts.TIMES_CONTACTED,
        ContactsContract.Contacts.LAST_TIME_CONTACTED,
        ContactsContract.Contacts.IN_VISIBLE_GROUP
    )
    val PROJECTION_ID_ONLY: Array<String> = arrayOf(BaseColumns._ID)

    /**
     * Frequency for contacts information into the dictionary
     */
    const val FREQUENCY_FOR_CONTACTS: Int = 40
    const val FREQUENCY_FOR_CONTACTS_BIGRAM: Int = 90

    /**
     * Do not attempt to query contacts if there are more than this many entries.
     */
    const val MAX_CONTACTS_PROVIDER_QUERY_LIMIT: Int = 10000

    /**
     * Index of the column for 'name' in content providers:
     * Contacts & ContactsContract.Profile.
     */
    const val NAME_INDEX: Int = 1
    const val TIMES_CONTACTED_INDEX: Int = 2
    const val LAST_TIME_CONTACTED_INDEX: Int = 3
    const val IN_VISIBLE_GROUP_INDEX: Int = 4
}
