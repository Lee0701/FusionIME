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
package com.android.inputmethod.latin.personalization

import android.content.Context
import com.android.inputmethod.annotations.ExternallyReferenced
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.latin.BinaryDictionary
import com.android.inputmethod.latin.Dictionary
import com.android.inputmethod.latin.ExpandableBinaryDictionary
import com.android.inputmethod.latin.NgramContext
import com.android.inputmethod.latin.define.ProductionFlags
import com.android.inputmethod.latin.makedict.DictionaryHeader
import java.io.File
import java.util.Locale
import javax.annotation.Nonnull

/**
 * Locally gathers statistics about the words user types and various other signals like
 * auto-correction cancellation or manual picks. This allows the keyboard to adapt to the
 * typist over time.
 */
class UserHistoryDictionary internal constructor(
    context: Context, locale: Locale,
    account: String?
) :
    ExpandableBinaryDictionary(
        context,
        getUserHistoryDictName(NAME, locale, null,  /* dictFile */account),
        locale,
        Dictionary.TYPE_USER_HISTORY,
        null
    ) {
    // TODO: Make this constructor private
    init {
        if (mLocale != null && mLocale.toString().length > 1) {
            reloadDictionaryIfRequired()
        }
    }

    override fun close() {
        // Flush pending writes.
        asyncFlushBinaryDictionary()
        super.close()
    }

    override val headerAttributeMap: MutableMap<String?, String?>
        get() {
            val attributeMap: MutableMap<String?, String?>? =
                super.headerAttributeMap
            attributeMap!!.put(
                DictionaryHeader.USES_FORGETTING_CURVE_KEY,
                DictionaryHeader.ATTRIBUTE_VALUE_TRUE
            )
            attributeMap.put(
                DictionaryHeader.HAS_HISTORICAL_INFO_KEY,
                DictionaryHeader.ATTRIBUTE_VALUE_TRUE
            )
            return attributeMap
        }

    override fun loadInitialContentsLocked() {
        // No initial contents.
    }

    override fun isValidWord(word: String): Boolean {
        // Strings out of this dictionary should not be considered existing words.
        return false
    }

    companion object {
        val NAME: String = UserHistoryDictionary::class.java.getSimpleName()

        /**
         * @returns the name of the [UserHistoryDictionary].
         */
        @UsedForTesting
        fun getUserHistoryDictName(
            name: String, locale: Locale,
            dictFile: File?, account: String?
        ): String {
            if (!ProductionFlags.ENABLE_PER_ACCOUNT_USER_HISTORY_DICTIONARY) {
                return ExpandableBinaryDictionary.getDictName(name, locale, dictFile)
            }
            return getUserHistoryDictNamePerAccount(name, locale, dictFile, account)
        }

        /**
         * Uses the currently signed in account to determine the dictionary name.
         */
        private fun getUserHistoryDictNamePerAccount(
            name: String, locale: Locale,
            dictFile: File?, account: String?
        ): String {
            if (dictFile != null) {
                return dictFile.getName()
            }
            var dictName: String = name + "." + locale.toString()
            if (account != null) {
                dictName += "." + account
            }
            return dictName
        }

        // Note: This method is called by {@link DictionaryFacilitator} using Java reflection.
        @Suppress("unused")
        @ExternallyReferenced
        fun getDictionary(
            context: Context, locale: Locale,
            dictFile: File?, dictNamePrefix: String?, account: String?
        ): UserHistoryDictionary {
            return PersonalizationHelper.getUserHistoryDictionary(context, locale, account)
        }

        /**
         * Add a word to the user history dictionary.
         *
         * @param userHistoryDictionary the user history dictionary
         * @param ngramContext the n-gram context
         * @param word the word the user inputted
         * @param isValid whether the word is valid or not
         * @param timestamp the timestamp when the word has been inputted
         */
        fun addToDictionary(
            userHistoryDictionary: ExpandableBinaryDictionary,
            @Nonnull ngramContext: NgramContext, word: String, isValid: Boolean,
            timestamp: Int
        ) {
            if (word.length > BinaryDictionary.DICTIONARY_MAX_WORD_LENGTH) {
                return
            }
            userHistoryDictionary.updateEntriesForWord(
                ngramContext, word,
                isValid, 1,  /* count */timestamp
            )
        }
    }
}
