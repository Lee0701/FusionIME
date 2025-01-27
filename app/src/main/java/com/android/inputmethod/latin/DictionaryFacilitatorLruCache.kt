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
import android.util.Log
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Cache for dictionary facilitators of multiple locales.
 * This class automatically creates and releases up to 3 facilitator instances using LRU policy.
 */
class DictionaryFacilitatorLruCache(context: Context, dictionaryNamePrefix: String) {
    private val mContext: Context
    private val mDictionaryNamePrefix: String
    private val mLock: Any = Any()
    private val mDictionaryFacilitator: DictionaryFacilitator
    private var mUseContactsDictionary: Boolean = false
    private var mLocale: Locale? = null

    init {
        mContext = context
        mDictionaryNamePrefix = dictionaryNamePrefix
        mDictionaryFacilitator = DictionaryFacilitatorProvider.getDictionaryFacilitator(
            true /* isNeededForSpellChecking */
        )
    }

    private fun resetDictionariesForLocaleLocked() {
        // Nothing to do if the locale is null.  This would be the case before any get() calls.
        if (mLocale != null) {
            // Note: Given that personalized dictionaries are not used here; we can pass null account.
            mDictionaryFacilitator.resetDictionaries(
                mContext, mLocale!!,
                mUseContactsDictionary, false,  /* usePersonalizedDicts */
                false,  /* forceReloadMainDictionary */null,  /* account */
                mDictionaryNamePrefix, null /* listener */
            )
        }
    }

    fun setUseContactsDictionary(useContactsDictionary: Boolean) {
        synchronized(mLock) {
            if (mUseContactsDictionary == useContactsDictionary) {
                // The value has not been changed.
                return
            }
            mUseContactsDictionary = useContactsDictionary
            resetDictionariesForLocaleLocked()
            waitForLoadingMainDictionary(mDictionaryFacilitator)
        }
    }

    fun get(locale: Locale?): DictionaryFacilitator {
        synchronized(mLock) {
            if (!mDictionaryFacilitator.isForLocale(locale)) {
                mLocale = locale
                resetDictionariesForLocaleLocked()
            }
            waitForLoadingMainDictionary(mDictionaryFacilitator)
            return mDictionaryFacilitator
        }
    }

    fun closeDictionaries() {
        synchronized(mLock) {
            mDictionaryFacilitator.closeDictionaries()
        }
    }

    companion object {
        private const val TAG: String = "DictionaryFacilitatorLruCache"
        private const val WAIT_FOR_LOADING_MAIN_DICT_IN_MILLISECONDS: Int = 1000
        private const val MAX_RETRY_COUNT_FOR_WAITING_FOR_LOADING_DICT: Int = 5

        private fun waitForLoadingMainDictionary(
            dictionaryFacilitator: DictionaryFacilitator
        ) {
            for (i in 0 until MAX_RETRY_COUNT_FOR_WAITING_FOR_LOADING_DICT) {
                try {
                    dictionaryFacilitator.waitForLoadingMainDictionaries(
                        WAIT_FOR_LOADING_MAIN_DICT_IN_MILLISECONDS.toLong(), TimeUnit.MILLISECONDS
                    )
                    return
                } catch (e: InterruptedException) {
                    Log.i(TAG, "Interrupted during waiting for loading main dictionary.", e)
                    if (i < MAX_RETRY_COUNT_FOR_WAITING_FOR_LOADING_DICT - 1) {
                        Log.i(TAG, "Retry", e)
                    } else {
                        Log.w(
                            TAG, ("Give up retrying. Retried "
                                    + MAX_RETRY_COUNT_FOR_WAITING_FOR_LOADING_DICT + " times."), e
                        )
                    }
                }
            }
        }
    }
}
