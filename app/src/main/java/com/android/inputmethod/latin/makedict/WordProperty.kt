/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.inputmethod.latin.makedict

import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.latin.BinaryDictionary
import com.android.inputmethod.latin.Dictionary
import com.android.inputmethod.latin.NgramContext
import com.android.inputmethod.latin.NgramContext.WordInfo
import com.android.inputmethod.latin.common.StringUtils
import com.android.inputmethod.latin.utils.CombinedFormatUtils

/**
 * Utility class for a word with a probability.
 *
 * This is chiefly used to iterate a dictionary.
 */
class WordProperty : Comparable<WordProperty> {
    val mWord: String?
    val mProbabilityInfo: ProbabilityInfo
    val mNgrams: ArrayList<NgramProperty>?

    // TODO: Support mIsBeginningOfSentence.
    val mIsBeginningOfSentence: Boolean
    val mIsNotAWord: Boolean
    val mIsPossiblyOffensive: Boolean
    val mHasNgrams: Boolean

    private var mHashCode = 0

    // TODO: Support n-gram.
    @UsedForTesting
    constructor(
        word: String?, probabilityInfo: ProbabilityInfo,
        bigrams: ArrayList<WeightedString>?,
        isNotAWord: Boolean, isPossiblyOffensive: Boolean
    ) {
        mWord = word
        mProbabilityInfo = probabilityInfo
        if (null == bigrams) {
            mNgrams = null
        } else {
            mNgrams = ArrayList()
            val ngramContext = NgramContext(WordInfo(mWord))
            for (bigramTarget in bigrams) {
                mNgrams.add(NgramProperty(bigramTarget, ngramContext))
            }
        }
        mIsBeginningOfSentence = false
        mIsNotAWord = isNotAWord
        mIsPossiblyOffensive = isPossiblyOffensive
        mHasNgrams = bigrams != null && !bigrams.isEmpty()
    }

    // Construct word property using information from native code.
    // This represents invalid word when the probability is BinaryDictionary.NOT_A_PROBABILITY.
    constructor(
        codePoints: IntArray, isNotAWord: Boolean,
        isPossiblyOffensive: Boolean, hasBigram: Boolean,
        isBeginningOfSentence: Boolean, probabilityInfo: IntArray,
        ngramPrevWordsArray: ArrayList<Array<IntArray>>,
        ngramPrevWordIsBeginningOfSentenceArray: ArrayList<BooleanArray>,
        ngramTargets: ArrayList<IntArray>, ngramProbabilityInfo: ArrayList<IntArray>
    ) {
        mWord = StringUtils.getStringFromNullTerminatedCodePointArray(codePoints)
        mProbabilityInfo = createProbabilityInfoFromArray(probabilityInfo)
        val ngrams = ArrayList<NgramProperty>()
        mIsBeginningOfSentence = isBeginningOfSentence
        mIsNotAWord = isNotAWord
        mIsPossiblyOffensive = isPossiblyOffensive
        mHasNgrams = hasBigram

        val relatedNgramCount = ngramTargets.size
        for (i in 0 until relatedNgramCount) {
            val ngramTargetString =
                StringUtils.getStringFromNullTerminatedCodePointArray(
                    ngramTargets[i]
                )
            val ngramTarget = WeightedString(
                ngramTargetString,
                createProbabilityInfoFromArray(ngramProbabilityInfo[i])
            )
            val prevWords = ngramPrevWordsArray[i]
            val isBeginningOfSentenceArray =
                ngramPrevWordIsBeginningOfSentenceArray[i]
            val wordInfoArray = arrayOfNulls<WordInfo>(prevWords.size)
            for (j in prevWords.indices) {
                wordInfoArray[j] = if (isBeginningOfSentenceArray[j])
                    WordInfo.Companion.BEGINNING_OF_SENTENCE_WORD_INFO
                else
                    WordInfo(
                        StringUtils.getStringFromNullTerminatedCodePointArray(
                            prevWords[j]
                        )
                    )
            }
            val ngramContext = NgramContext(*wordInfoArray)
            ngrams.add(NgramProperty(ngramTarget, ngramContext))
        }
        mNgrams = if (ngrams.isEmpty()) null else ngrams
    }

    @get:UsedForTesting
    val bigrams: ArrayList<WeightedString?>?
        // TODO: Remove
        get() {
            if (null == mNgrams) {
                return null
            }
            val bigrams =
                ArrayList<WeightedString?>()
            for (ngram in mNgrams) {
                if (ngram.mNgramContext.prevWordCount == 1) {
                    bigrams.add(ngram.mTargetWord)
                }
            }
            return bigrams
        }

    val probability: Int
        get() = mProbabilityInfo.mProbability

    /**
     * Three-way comparison.
     *
     * A Word x is greater than a word y if x has a higher frequency. If they have the same
     * frequency, they are sorted in lexicographic order.
     */
    override fun compareTo(w: WordProperty): Int {
        if (probability < w.probability) return 1
        if (probability > w.probability) return -1
        return mWord!!.compareTo(w.mWord!!)
    }

    /**
     * Equality test.
     *
     * Words are equal if they have the same frequency, the same spellings, and the same
     * attributes.
     */
    override fun equals(o: Any?): Boolean {
        if (o === this) return true
        if (o !is WordProperty) return false
        val w = o
        return mProbabilityInfo == w.mProbabilityInfo
                && mWord == w.mWord && equals(mNgrams, w.mNgrams)
                && mIsNotAWord == w.mIsNotAWord && mIsPossiblyOffensive == w.mIsPossiblyOffensive && mHasNgrams == w.mHasNgrams
    }

    override fun hashCode(): Int {
        if (mHashCode == 0) {
            mHashCode = computeHashCode(this)
        }
        return mHashCode
    }

    @get:UsedForTesting
    val isValid: Boolean
        get() = probability != Dictionary.Companion.NOT_A_PROBABILITY

    override fun toString(): String {
        return CombinedFormatUtils.formatWordProperty(this)
    }

    companion object {
        private fun createProbabilityInfoFromArray(probabilityInfo: IntArray): ProbabilityInfo {
            return ProbabilityInfo(
                probabilityInfo[BinaryDictionary.Companion.FORMAT_WORD_PROPERTY_PROBABILITY_INDEX],
                probabilityInfo[BinaryDictionary.Companion.FORMAT_WORD_PROPERTY_TIMESTAMP_INDEX],
                probabilityInfo[BinaryDictionary.Companion.FORMAT_WORD_PROPERTY_LEVEL_INDEX],
                probabilityInfo[BinaryDictionary.Companion.FORMAT_WORD_PROPERTY_COUNT_INDEX]
            )
        }

        private fun computeHashCode(word: WordProperty): Int {
            return arrayOf(
                word.mWord,
                word.mProbabilityInfo,
                word.mNgrams,
                word.mIsNotAWord,
                word.mIsPossiblyOffensive
            ).contentHashCode()
        }

        // TDOO: Have a utility method like java.util.Objects.equals.
        private fun <T> equals(a: ArrayList<T>?, b: ArrayList<T>?): Boolean {
            if (null == a) {
                return null == b
            }
            return a == b
        }
    }
}
