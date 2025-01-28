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
package com.android.inputmethod.latin.utils

import android.util.Log
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.latin.NgramContext
import com.android.inputmethod.latin.NgramContext.WordInfo
import com.android.inputmethod.latin.common.StringUtils
import com.android.inputmethod.latin.define.DecoderSpecificConstants
import com.android.inputmethod.latin.settings.SpacingAndPunctuations
import java.util.Locale

// Note: this class is used as a parameter type of a native method. You should be careful when you
// rename this class or field name. See BinaryDictionary#addMultipleDictionaryEntriesNative().
class WordInputEventForPersonalization @UsedForTesting constructor(
    targetWord: CharSequence,
    ngramContext: NgramContext, timestamp: Int
) {
    val mTargetWord: IntArray =
        StringUtils.toCodePointArray(targetWord)
    val mPrevWordsCount: Int = ngramContext.prevWordCount
    val mPrevWordArray: Array<IntArray?> =
        arrayOfNulls(DecoderSpecificConstants.MAX_PREV_WORD_COUNT_FOR_N_GRAM)
    val mIsPrevWordBeginningOfSentenceArray: BooleanArray =
        BooleanArray(DecoderSpecificConstants.MAX_PREV_WORD_COUNT_FOR_N_GRAM)

    // Time stamp in seconds.
    val mTimestamp: Int

    init {
        ngramContext.outputToArray(mPrevWordArray, mIsPrevWordBeginningOfSentenceArray)
        mTimestamp = timestamp
    }

    companion object {
        private val TAG: String = WordInputEventForPersonalization::class.java.simpleName
        private const val DEBUG_TOKEN = false

        // Process a list of words and return a list of {@link WordInputEventForPersonalization}
        // objects.
        fun createInputEventFrom(
            tokens: List<String>, timestamp: Int,
            spacingAndPunctuations: SpacingAndPunctuations, locale: Locale?
        ): ArrayList<WordInputEventForPersonalization> {
            val inputEvents = ArrayList<WordInputEventForPersonalization>()
            val N = tokens.size
            var ngramContext: NgramContext = NgramContext.EMPTY_PREV_WORDS_INFO
            for (i in 0 until N) {
                val tempWord = tokens[i]
                if (StringUtils.isEmptyStringOrWhiteSpaces(tempWord)) {
                    // just skip this token
                    if (DEBUG_TOKEN) {
                        Log.d(
                            TAG,
                            "--- isEmptyStringOrWhiteSpaces: \"$tempWord\""
                        )
                    }
                    continue
                }
                if (!DictionaryInfoUtils.looksValidForDictionaryInsertion(
                        tempWord, spacingAndPunctuations
                    )
                ) {
                    if (DEBUG_TOKEN) {
                        Log.d(
                            TAG, ("--- not looksValidForDictionaryInsertion: \""
                                    + tempWord + "\"")
                        )
                    }
                    // Sentence terminator found. Split.
                    // TODO: Detect whether the context is beginning-of-sentence.
                    ngramContext = NgramContext.EMPTY_PREV_WORDS_INFO
                    continue
                }
                if (DEBUG_TOKEN) {
                    Log.d(
                        TAG,
                        "--- word: \"$tempWord\""
                    )
                }
                val inputEvent =
                    detectWhetherVaildWordOrNotAndGetInputEvent(
                        ngramContext, tempWord, timestamp, locale
                    )
                if (inputEvent == null) {
                    continue
                }
                inputEvents.add(inputEvent)
                ngramContext = ngramContext.getNextNgramContext(WordInfo(tempWord))
            }
            return inputEvents
        }

        private fun detectWhetherVaildWordOrNotAndGetInputEvent(
            ngramContext: NgramContext, targetWord: String, timestamp: Int,
            locale: Locale?
        ): WordInputEventForPersonalization? {
            if (locale == null) {
                return null
            }
            return WordInputEventForPersonalization(targetWord, ngramContext, timestamp)
        }
    }
}
