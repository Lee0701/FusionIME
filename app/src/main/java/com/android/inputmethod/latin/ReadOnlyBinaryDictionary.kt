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
package com.android.inputmethod.latin

import com.android.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import com.android.inputmethod.latin.common.ComposedData
import com.android.inputmethod.latin.settings.SettingsValuesForSuggestion
import java.util.Locale
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * This class provides binary dictionary reading operations with locking. An instance of this class
 * can be used by multiple threads. Note that different session IDs must be used when multiple
 * threads get suggestions using this class.
 */
class ReadOnlyBinaryDictionary(
    filename: String?, offset: Long, length: Long,
    useFullEditDistance: Boolean, locale: Locale?, dictType: String
) : Dictionary(dictType, locale) {
    /**
     * A lock for accessing binary dictionary. Only closing binary dictionary is the operation
     * that change the state of dictionary.
     */
    private val mLock: ReentrantReadWriteLock = ReentrantReadWriteLock()

    private val mBinaryDictionary: BinaryDictionary

    init {
        mBinaryDictionary = BinaryDictionary(
            filename, offset, length, useFullEditDistance,
            locale, dictType, false /* isUpdatable */
        )
    }

    val isValidDictionary: Boolean
        get() {
            return mBinaryDictionary.isValidDictionary
        }

    override fun getSuggestions(
        composedData: ComposedData,
        ngramContext: NgramContext, proximityInfoHandle: Long,
        settingsValuesForSuggestion: SettingsValuesForSuggestion,
        sessionId: Int, weightForLocale: Float,
        inOutWeightOfLangModelVsSpatialModel: FloatArray?
    ): ArrayList<SuggestedWordInfo>? {
        if (mLock.readLock().tryLock()) {
            try {
                return mBinaryDictionary.getSuggestions(
                    composedData, ngramContext,
                    proximityInfoHandle, settingsValuesForSuggestion, sessionId,
                    weightForLocale, inOutWeightOfLangModelVsSpatialModel
                )
            } finally {
                mLock.readLock().unlock()
            }
        }
        return null
    }

    override fun isInDictionary(word: String): Boolean {
        if (mLock.readLock().tryLock()) {
            try {
                return mBinaryDictionary.isInDictionary(word)
            } finally {
                mLock.readLock().unlock()
            }
        }
        return false
    }

    override fun shouldAutoCommit(candidate: SuggestedWordInfo): Boolean {
        if (mLock.readLock().tryLock()) {
            try {
                return mBinaryDictionary.shouldAutoCommit(candidate)
            } finally {
                mLock.readLock().unlock()
            }
        }
        return false
    }

    override fun getFrequency(word: String): Int {
        if (mLock.readLock().tryLock()) {
            try {
                return mBinaryDictionary.getFrequency(word)
            } finally {
                mLock.readLock().unlock()
            }
        }
        return Dictionary.NOT_A_PROBABILITY
    }

    override fun getMaxFrequencyOfExactMatches(word: String): Int {
        if (mLock.readLock().tryLock()) {
            try {
                return mBinaryDictionary.getMaxFrequencyOfExactMatches(word)
            } finally {
                mLock.readLock().unlock()
            }
        }
        return Dictionary.NOT_A_PROBABILITY
    }

    override fun close() {
        mLock.writeLock().lock()
        try {
            mBinaryDictionary.close()
        } finally {
            mLock.writeLock().unlock()
        }
    }
}
