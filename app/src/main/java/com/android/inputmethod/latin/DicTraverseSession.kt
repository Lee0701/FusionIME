/*
 * Copyright (C) 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.inputmethod.latin

import com.android.inputmethod.latin.common.NativeSuggestOptions
import com.android.inputmethod.latin.define.DecoderSpecificConstants
import com.android.inputmethod.latin.utils.JniUtils
import java.util.Locale

class DicTraverseSession(locale: Locale?, dictionary: Long, dictSize: Long) {
    val mInputCodePoints: IntArray = IntArray(DecoderSpecificConstants.DICTIONARY_MAX_WORD_LENGTH)
    val mPrevWordCodePointArrays: Array<IntArray?> =
        arrayOfNulls(DecoderSpecificConstants.MAX_PREV_WORD_COUNT_FOR_N_GRAM)
    val mIsBeginningOfSentenceArray: BooleanArray =
        BooleanArray(DecoderSpecificConstants.MAX_PREV_WORD_COUNT_FOR_N_GRAM)
    val mOutputSuggestionCount: IntArray = IntArray(1)
    val mOutputCodePoints: IntArray =
        IntArray(DecoderSpecificConstants.DICTIONARY_MAX_WORD_LENGTH * MAX_RESULTS)
    val mSpaceIndices: IntArray = IntArray(MAX_RESULTS)
    val mOutputScores: IntArray = IntArray(MAX_RESULTS)
    val mOutputTypes: IntArray = IntArray(MAX_RESULTS)

    // Only one result is ever used
    val mOutputAutoCommitFirstWordConfidence: IntArray = IntArray(1)
    val mInputOutputWeightOfLangModelVsSpatialModel: FloatArray = FloatArray(1)

    val mNativeSuggestOptions: NativeSuggestOptions = NativeSuggestOptions()

    var session: Long
        private set

    init {
        session = createNativeDicTraverseSession(
            if (locale != null) locale.toString() else "", dictSize
        )
        initSession(dictionary)
    }

    @JvmOverloads
    fun initSession(dictionary: Long, previousWord: IntArray? = null, previousWordLength: Int = 0) {
        initDicTraverseSessionNative(
            session, dictionary, previousWord, previousWordLength
        )
    }

    private fun closeInternal() {
        if (session != 0L) {
            releaseDicTraverseSessionNative(session)
            session = 0
        }
    }

    fun close() {
        closeInternal()
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        try {
            closeInternal()
        } finally {
//            super.finalize()
        }
    }

    companion object {
        init {
            JniUtils.loadNativeLibrary()
        }

        // Must be equal to MAX_RESULTS in native/jni/src/defines.h
        private const val MAX_RESULTS: Int = 18
        @JvmStatic
        private external fun setDicTraverseSessionNative(locale: String, dictSize: Long): Long
        @JvmStatic
        private external fun initDicTraverseSessionNative(
            nativeDicTraverseSession: Long,
            dictionary: Long, previousWord: IntArray?, previousWordLength: Int
        )

        @JvmStatic
        private external fun releaseDicTraverseSessionNative(nativeDicTraverseSession: Long)

        private fun createNativeDicTraverseSession(locale: String, dictSize: Long): Long {
            return setDicTraverseSessionNative(locale, dictSize)
        }
    }
}
