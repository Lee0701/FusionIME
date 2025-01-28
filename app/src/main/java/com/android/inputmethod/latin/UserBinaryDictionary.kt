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

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.UserDictionary.Words
import android.text.TextUtils
import android.util.Log
import com.android.inputmethod.annotations.ExternallyReferenced
import com.android.inputmethod.latin.utils.SubtypeLocaleUtils
import java.io.File
import java.util.Locale

/**
 * An expandable dictionary that stores the words in the user dictionary provider into a binary
 * dictionary file to use it from native code.
 */
class UserBinaryDictionary protected constructor(
    context: Context, locale: Locale,
    alsoUseMoreRestrictiveLocales: Boolean,
    dictFile: File?, name: String
) : ExpandableBinaryDictionary(
    context,
    ExpandableBinaryDictionary.getDictName(name, locale, dictFile),
    locale,
    Dictionary.TYPE_USER,
    dictFile
) {
    private var mObserver: ContentObserver?
    private var mLocaleString: String? = null
    private val mAlsoUseMoreRestrictiveLocales: Boolean

    init {
        if (null == locale) throw NullPointerException() // Catch the error earlier

        val localeStr: String = locale.toString()
        if (SubtypeLocaleUtils.NO_LANGUAGE == localeStr) {
            // If we don't have a locale, insert into the "all locales" user dictionary.
            mLocaleString = USER_DICTIONARY_ALL_LANGUAGES
        } else {
            mLocaleString = localeStr
        }
        mAlsoUseMoreRestrictiveLocales = alsoUseMoreRestrictiveLocales
        val cres: ContentResolver = context.getContentResolver()

        mObserver = object : ContentObserver(null) {
            override fun onChange(self: Boolean) {
                // This hook is deprecated as of API level 16 (Build.VERSION_CODES.JELLY_BEAN),
                // but should still be supported for cases where the IME is running on an older
                // version of the platform.
                onChange(self, null)
            }

            // The following hook is only available as of API level 16
            // (Build.VERSION_CODES.JELLY_BEAN), and as such it will only work on JellyBean+
            // devices. On older versions of the platform, the hook above will be called instead.
            override fun onChange(self: Boolean, uri: Uri?) {
                setNeedsToRecreate()
            }
        }
        cres.registerContentObserver(Words.CONTENT_URI, true, mObserver!!)
        reloadDictionaryIfRequired()
    }

    @Synchronized
    override fun close() {
        if (mObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mObserver!!)
            mObserver = null
        }
        super.close()
    }

    public override fun loadInitialContentsLocked() {
        // Split the locale. For example "en" => ["en"], "de_DE" => ["de", "DE"],
        // "en_US_foo_bar_qux" => ["en", "US", "foo_bar_qux"] because of the limit of 3.
        // This is correct for locale processing.
        // For this example, we'll look at the "en_US_POSIX" case.
        val localeElements: Array<String?> =
            if (TextUtils.isEmpty(mLocaleString)) arrayOf() else mLocaleString!!.split(
                "_".toRegex(),
                limit = 3
            ).toTypedArray()
        val length: Int = localeElements.size

        val request: StringBuilder = StringBuilder("(locale is NULL)")
        var localeSoFar: String = ""
        // At start, localeElements = ["en", "US", "POSIX"] ; localeSoFar = "" ;
        // and request = "(locale is NULL)"
        for (i in 0 until length) {
            // i | localeSoFar    | localeElements
            // 0 | ""             | ["en", "US", "POSIX"]
            // 1 | "en_"          | ["en", "US", "POSIX"]
            // 2 | "en_US_"       | ["en", "en_US", "POSIX"]
            localeElements[i] = localeSoFar + localeElements.get(i)
            localeSoFar = localeElements.get(i) + "_"
            // i | request
            // 0 | "(locale is NULL)"
            // 1 | "(locale is NULL) or (locale=?)"
            // 2 | "(locale is NULL) or (locale=?) or (locale=?)"
            request.append(" or (locale=?)")
        }

        // At the end, localeElements = ["en", "en_US", "en_US_POSIX"]; localeSoFar = en_US_POSIX_"
        // and request = "(locale is NULL) or (locale=?) or (locale=?) or (locale=?)"
        val requestArguments: Array<String?>
        // If length == 3, we already have all the arguments we need (common prefix is meaningless
        // inside variants
        if (mAlsoUseMoreRestrictiveLocales && length < 3) {
            request.append(" or (locale like ?)")
            // The following creates an array with one more (null) position
            val localeElementsWithMoreRestrictiveLocalesIncluded: Array<String?> =
                localeElements.copyOf(length + 1)
            localeElementsWithMoreRestrictiveLocalesIncluded[length] =
                localeElements.get(length - 1) + "_%"
            requestArguments = localeElementsWithMoreRestrictiveLocalesIncluded
            // If for example localeElements = ["en"]
            // then requestArguments = ["en", "en_%"]
            // and request = (locale is NULL) or (locale=?) or (locale like ?)
            // If localeElements = ["en", "en_US"]
            // then requestArguments = ["en", "en_US", "en_US_%"]
        } else {
            requestArguments = localeElements
        }
        val requestString: String = request.toString()
        addWordsFromProjectionLocked(PROJECTION_QUERY, requestString, requestArguments)
    }

    @Throws(IllegalArgumentException::class)
    private fun addWordsFromProjectionLocked(
        query: Array<String>,
        request: String,
        requestArguments: Array<String?>
    ) {
        var cursor: Cursor? = null
        try {
            cursor = mContext.getContentResolver().query(
                Words.CONTENT_URI, query, request, requestArguments, null
            )
            addWordsLocked(cursor)
        } catch (e: SQLiteException) {
            Log.e(TAG, "SQLiteException in the remote User dictionary process.", e)
        } finally {
            try {
                if (null != cursor) cursor.close()
            } catch (e: SQLiteException) {
                Log.e(TAG, "SQLiteException in the remote User dictionary process.", e)
            }
        }
    }

    private fun addWordsLocked(cursor: Cursor?) {
        if (cursor == null) return
        if (cursor.moveToFirst()) {
            val indexWord: Int = cursor.getColumnIndex(Words.WORD)
            val indexFrequency: Int = cursor.getColumnIndex(Words.FREQUENCY)
            while (!cursor.isAfterLast()) {
                val word: String = cursor.getString(indexWord)
                val frequency: Int = cursor.getInt(indexFrequency)
                val adjustedFrequency: Int = scaleFrequencyFromDefaultToLatinIme(frequency)
                // Safeguard against adding really long words.
                if (word.length <= MAX_WORD_LENGTH) {
                    runGCIfRequiredLocked(true /* mindsBlockByGC */)
                    addUnigramLocked(
                        word, adjustedFrequency, false,  /* isNotAWord */
                        false,  /* isPossiblyOffensive */
                        BinaryDictionary.NOT_A_VALID_TIMESTAMP
                    )
                }
                cursor.moveToNext()
            }
        }
    }

    companion object {
        private val TAG: String = ExpandableBinaryDictionary::class.java.getSimpleName()

        // The user dictionary provider uses an empty string to mean "all languages".
        private const val USER_DICTIONARY_ALL_LANGUAGES: String = ""
        private const val HISTORICAL_DEFAULT_USER_DICTIONARY_FREQUENCY: Int = 250
        private const val LATINIME_DEFAULT_USER_DICTIONARY_FREQUENCY: Int = 160

        private val PROJECTION_QUERY: Array<String> = arrayOf(Words.WORD, Words.FREQUENCY)

        private const val NAME: String = "userunigram"

        // Note: This method is called by {@link DictionaryFacilitator} using Java reflection.
        @ExternallyReferenced
        fun getDictionary(
            context: Context, locale: Locale, dictFile: File?,
            dictNamePrefix: String, account: String?
        ): UserBinaryDictionary {
            return UserBinaryDictionary(
                context, locale, false,  /* alsoUseMoreRestrictiveLocales */
                dictFile, dictNamePrefix + NAME
            )
        }

        private fun scaleFrequencyFromDefaultToLatinIme(defaultFrequency: Int): Int {
            // The default frequency for the user dictionary is 250 for historical reasons.
            // Latin IME considers a good value for the default user dictionary frequency
            // is about 160 considering the scale we use. So we are scaling down the values.
            if (defaultFrequency > Int.MAX_VALUE / LATINIME_DEFAULT_USER_DICTIONARY_FREQUENCY) {
                return ((defaultFrequency / HISTORICAL_DEFAULT_USER_DICTIONARY_FREQUENCY)
                        * LATINIME_DEFAULT_USER_DICTIONARY_FREQUENCY)
            }
            return ((defaultFrequency * LATINIME_DEFAULT_USER_DICTIONARY_FREQUENCY)
                    / HISTORICAL_DEFAULT_USER_DICTIONARY_FREQUENCY)
        }
    }
}
