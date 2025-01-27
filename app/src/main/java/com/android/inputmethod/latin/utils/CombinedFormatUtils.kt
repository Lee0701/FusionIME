/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.inputmethod.latin.utils

import com.android.inputmethod.latin.makedict.DictionaryHeader
import com.android.inputmethod.latin.makedict.ProbabilityInfo
import com.android.inputmethod.latin.makedict.WordProperty

object CombinedFormatUtils {
    const val DICTIONARY_TAG: String = "dictionary"
    const val BIGRAM_TAG: String = "bigram"
    const val NGRAM_TAG: String = "ngram"
    const val NGRAM_PREV_WORD_TAG: String = "prev_word"
    const val PROBABILITY_TAG: String = "f"
    const val HISTORICAL_INFO_TAG: String = "historicalInfo"
    const val HISTORICAL_INFO_SEPARATOR: String = ":"
    const val WORD_TAG: String = "word"
    const val BEGINNING_OF_SENTENCE_TAG: String = "beginning_of_sentence"
    const val NOT_A_WORD_TAG: String = "not_a_word"
    const val POSSIBLY_OFFENSIVE_TAG: String = "possibly_offensive"
    const val TRUE_VALUE: String = "true"

    fun formatAttributeMap(attributeMap: HashMap<String, String?>): String {
        val builder = StringBuilder()
        builder.append(DICTIONARY_TAG + "=")
        if (attributeMap.containsKey(DictionaryHeader.Companion.DICTIONARY_ID_KEY)) {
            builder.append(attributeMap[DictionaryHeader.Companion.DICTIONARY_ID_KEY])
        }
        for (key in attributeMap.keys) {
            if (key == DictionaryHeader.Companion.DICTIONARY_ID_KEY) {
                continue
            }
            val value = attributeMap[key]
            builder.append(",$key=$value")
        }
        builder.append("\n")
        return builder.toString()
    }

    fun formatWordProperty(wordProperty: WordProperty): String {
        val builder = StringBuilder()
        builder.append(" " + WORD_TAG + "=" + wordProperty.mWord)
        builder.append(",")
        builder.append(formatProbabilityInfo(wordProperty.mProbabilityInfo))
        if (wordProperty.mIsBeginningOfSentence) {
            builder.append("," + BEGINNING_OF_SENTENCE_TAG + "=" + TRUE_VALUE)
        }
        if (wordProperty.mIsNotAWord) {
            builder.append("," + NOT_A_WORD_TAG + "=" + TRUE_VALUE)
        }
        if (wordProperty.mIsPossiblyOffensive) {
            builder.append("," + POSSIBLY_OFFENSIVE_TAG + "=" + TRUE_VALUE)
        }
        builder.append("\n")
        if (wordProperty.mHasNgrams) {
            for (ngramProperty in wordProperty.mNgrams!!) {
                builder.append(" " + NGRAM_TAG + "=" + ngramProperty.mTargetWord.mWord)
                builder.append(",")
                builder.append(formatProbabilityInfo(ngramProperty.mTargetWord.mProbabilityInfo))
                builder.append("\n")
                for (i in 0 until ngramProperty.mNgramContext.prevWordCount) {
                    builder.append(
                        ("  " + NGRAM_PREV_WORD_TAG + "[" + i + "]="
                                + ngramProperty.mNgramContext.getNthPrevWord(i + 1))
                    )
                    if (ngramProperty.mNgramContext.isNthPrevWordBeginningOfSentence(i + 1)) {
                        builder.append("," + BEGINNING_OF_SENTENCE_TAG + "=true")
                    }
                    builder.append("\n")
                }
            }
        }
        return builder.toString()
    }

    fun formatProbabilityInfo(probabilityInfo: ProbabilityInfo): String {
        val builder = StringBuilder()
        builder.append(PROBABILITY_TAG + "=" + probabilityInfo.mProbability)
        if (probabilityInfo.hasHistoricalInfo()) {
            builder.append(",")
            builder.append(HISTORICAL_INFO_TAG + "=")
            builder.append(probabilityInfo.mTimestamp)
            builder.append(HISTORICAL_INFO_SEPARATOR)
            builder.append(probabilityInfo.mLevel)
            builder.append(HISTORICAL_INFO_SEPARATOR)
            builder.append(probabilityInfo.mCount)
        }
        return builder.toString()
    }

    fun isLiteralTrue(value: String?): Boolean {
        return TRUE_VALUE.equals(value, ignoreCase = true)
    }
}
