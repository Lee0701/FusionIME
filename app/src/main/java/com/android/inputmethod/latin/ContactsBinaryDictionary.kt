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
package com.android.inputmethod.latin

import android.Manifest.permission
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.android.inputmethod.annotations.ExternallyReferenced
import com.android.inputmethod.latin.ContactsManager.ContactsChangedListener
import com.android.inputmethod.latin.NgramContext.WordInfo
import com.android.inputmethod.latin.common.StringUtils
import com.android.inputmethod.latin.permissions.PermissionsUtil
import com.android.inputmethod.latin.personalization.AccountUtils
import java.io.File
import java.util.Locale

class ContactsBinaryDictionary protected constructor(
    context: Context, locale: Locale,
    dictFile: File?, name: String
) : ExpandableBinaryDictionary(
    context,
    getDictName(name, locale, dictFile),
    locale,
    TYPE_CONTACTS,
    dictFile
), ContactsChangedListener {
    /**
     * Whether to use "firstname lastname" in bigram predictions.
     */
    private val mUseFirstLastBigrams: Boolean
    private val mContactsManager: ContactsManager

    init {
        mUseFirstLastBigrams = ContactsDictionaryUtils.useFirstLastBigramsForLocale(locale)
        mContactsManager = ContactsManager(context)
        mContactsManager.registerForUpdates(this /* listener */)
        reloadDictionaryIfRequired()
    }

    @Synchronized
    override fun close() {
        mContactsManager.close()
        super.close()
    }

    /**
     * Typically called whenever the dictionary is created for the first time or
     * recreated when we think that there are updates to the dictionary.
     * This is called asynchronously.
     */
    public override fun loadInitialContentsLocked() {
        loadDeviceAccountsEmailAddressesLocked()
        loadDictionaryForUriLocked(ContactsContract.Profile.CONTENT_URI)
        // TODO: Switch this URL to the newer ContactsContract too
        loadDictionaryForUriLocked(ContactsContract.Contacts.CONTENT_URI)
    }

    /**
     * Loads device accounts to the dictionary.
     */
    private fun loadDeviceAccountsEmailAddressesLocked() {
        val accountVocabulary: List<String?> =
            AccountUtils.getDeviceAccountsEmailAddresses(mContext)
        if (accountVocabulary == null || accountVocabulary.isEmpty()) {
            return
        }
        for (word: String? in accountVocabulary) {
            if (DEBUG) {
                Log.d(TAG, "loadAccountVocabulary: " + word)
            }
            runGCIfRequiredLocked(true /* mindsBlockByGC */)
            addUnigramLocked(
                word!!, ContactsDictionaryConstants.FREQUENCY_FOR_CONTACTS,
                false,  /* isNotAWord */false,  /* isPossiblyOffensive */
                BinaryDictionary.NOT_A_VALID_TIMESTAMP
            )
        }
    }

    /**
     * Loads data within content providers to the dictionary.
     */
    private fun loadDictionaryForUriLocked(uri: Uri) {
        if (!PermissionsUtil.checkAllPermissionsGranted(
                mContext, permission.READ_CONTACTS
            )
        ) {
            Log.i(TAG, "No permission to read contacts. Not loading the Dictionary.")
        }

        val validNames: ArrayList<String> = mContactsManager.getValidNames(uri)
        for (name in validNames) {
            addNameLocked(name)
        }
        if (uri == ContactsContract.Contacts.CONTENT_URI) {
            // Since we were able to add content successfully, update the local
            // state of the manager.
            mContactsManager.updateLocalState(validNames)
        }
    }

    /**
     * Adds the words in a name (e.g., firstname/lastname) to the binary dictionary along with their
     * bigrams depending on locale.
     */
    private fun addNameLocked(name: String) {
        val len: Int = StringUtils.codePointCount(name)
        var ngramContext: NgramContext = NgramContext.getEmptyPrevWordsContext(
            BinaryDictionary.MAX_PREV_WORD_COUNT_FOR_N_GRAM
        )
        // TODO: Better tokenization for non-Latin writing systems
        var i: Int = 0
        while (i < len) {
            if (Character.isLetter(name.codePointAt(i))) {
                val end: Int = ContactsDictionaryUtils.getWordEndPosition(name, len, i)
                val word: String = name.substring(i, end)
                if (DEBUG_DUMP) {
                    Log.d(TAG, "addName word = " + word)
                }
                i = end - 1
                // Don't add single letter words, possibly confuses
                // capitalization of i.
                val wordLen: Int = StringUtils.codePointCount(word)
                if (wordLen <= ExpandableBinaryDictionary.MAX_WORD_LENGTH && wordLen > 1) {
                    if (DEBUG) {
                        Log.d(TAG, "addName " + name + ", " + word + ", " + ngramContext)
                    }
                    runGCIfRequiredLocked(true /* mindsBlockByGC */)
                    addUnigramLocked(
                        word,
                        ContactsDictionaryConstants.FREQUENCY_FOR_CONTACTS, false,  /* isNotAWord */
                        false,  /* isPossiblyOffensive */
                        BinaryDictionary.NOT_A_VALID_TIMESTAMP
                    )
                    if (ngramContext.isValid && mUseFirstLastBigrams) {
                        runGCIfRequiredLocked(true /* mindsBlockByGC */)
                        addNgramEntryLocked(
                            ngramContext,
                            word,
                            ContactsDictionaryConstants.FREQUENCY_FOR_CONTACTS_BIGRAM,
                            BinaryDictionary.NOT_A_VALID_TIMESTAMP
                        )
                    }
                    ngramContext = ngramContext.getNextNgramContext(
                        WordInfo(word)
                    )
                }
            }
            i++
        }
    }

    override fun onContactsChange() {
        setNeedsToRecreate()
    }

    companion object {
        private val TAG: String = ContactsBinaryDictionary::class.java.getSimpleName()
        private const val NAME: String = "contacts"

        private const val DEBUG: Boolean = false
        private const val DEBUG_DUMP: Boolean = false

        // Note: This method is called by {@link DictionaryFacilitator} using Java reflection.
        @ExternallyReferenced
        fun getDictionary(
            context: Context, locale: Locale,
            dictFile: File?, dictNamePrefix: String, account: String?
        ): ContactsBinaryDictionary {
            return ContactsBinaryDictionary(context, locale, dictFile, dictNamePrefix + NAME)
        }
    }
}
