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

import android.text.TextUtils
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.latin.common.StringUtils
import com.android.inputmethod.latin.define.DecoderSpecificConstants
import javax.annotation.Nonnull
import kotlin.math.min

/**
 * Class to represent information of previous words. This class is used to add n-gram entries
 * into binary dictionaries, to get predictions, and to get suggestions.
 */
class NgramContext(maxPrevWordCount: Int, vararg prevWordsInfo: WordInfo?) {
    /**
     * Word information used to represent previous words information.
     */
    class WordInfo {
        // This is an empty char sequence when mIsBeginningOfSentence is true.
        val mWord: CharSequence?

        // TODO: Have sentence separator.
        // Whether the current context is beginning of sentence or not. This is true when composing
        // at the beginning of an input field or composing a word after a sentence separator.
        val mIsBeginningOfSentence: Boolean

        // Beginning of sentence.
        private constructor() {
            mWord = ""
            mIsBeginningOfSentence = true
        }

        constructor(word: CharSequence?) {
            mWord = word
            mIsBeginningOfSentence = false
        }

        val isValid: Boolean
            get() {
                return mWord != null
            }

        override fun hashCode(): Int {
            return arrayOf(mWord, mIsBeginningOfSentence).contentHashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is WordInfo) return false
            val wordInfo: WordInfo = other
            if (mWord == null || wordInfo.mWord == null) {
                return mWord === wordInfo.mWord
                        && mIsBeginningOfSentence == wordInfo.mIsBeginningOfSentence
            }
            return TextUtils.equals(mWord, wordInfo.mWord)
                    && mIsBeginningOfSentence == wordInfo.mIsBeginningOfSentence
        }

        companion object {
            @Nonnull
            val EMPTY_WORD_INFO: WordInfo = WordInfo("")

            @Nonnull
            val BEGINNING_OF_SENTENCE_WORD_INFO: WordInfo = WordInfo()
        }
    }

    // The words immediately before the considered word. EMPTY_WORD_INFO element means we don't
    // have any context for that previous word including the "beginning of sentence context" - we
    // just don't know what to predict using the information. An example of that is after a comma.
    // For simplicity of implementation, elements may also be EMPTY_WORD_INFO transiently after the
    // WordComposer was reset and before starting a new composing word, but we should never be
    // calling getSuggetions* in this situation.
    private val mPrevWordsInfo: Array<WordInfo?> = prevWordsInfo.toList().toTypedArray()
    val prevWordCount: Int = prevWordsInfo.size

    private val mMaxPrevWordCount: Int = maxPrevWordCount

    // Construct from the previous word information.
    constructor(vararg prevWordsInfo: WordInfo?) : this(
        DecoderSpecificConstants.MAX_PREV_WORD_COUNT_FOR_N_GRAM,
        *prevWordsInfo
    )

    /**
     * Create next prevWordsInfo using current prevWordsInfo.
     */
    @Nonnull
    fun getNextNgramContext(wordInfo: WordInfo?): NgramContext {
        val nextPrevWordCount: Int =
            min(mMaxPrevWordCount.toDouble(), (prevWordCount + 1).toDouble()).toInt()
        val prevWordsInfo: Array<WordInfo?> = arrayOfNulls(nextPrevWordCount)
        prevWordsInfo[0] = wordInfo
        System.arraycopy(mPrevWordsInfo, 0, prevWordsInfo, 1, nextPrevWordCount - 1)
        return NgramContext(mMaxPrevWordCount, *prevWordsInfo)
    }


    /**
     * Extracts the previous words context.
     *
     * @return a String with the previous words separated by white space.
     */
    fun extractPrevWordsContext(): String {
        val terms: ArrayList<String?> = ArrayList()
        for (i in mPrevWordsInfo.indices.reversed()) {
            if (mPrevWordsInfo.get(i) != null && mPrevWordsInfo.get(i)!!.isValid) {
                val wordInfo: WordInfo? = mPrevWordsInfo.get(i)
                if (wordInfo!!.mIsBeginningOfSentence) {
                    terms.add(BEGINNING_OF_SENTENCE_TAG)
                } else {
                    val term: String = wordInfo.mWord.toString()
                    if (!term.isEmpty()) {
                        terms.add(term)
                    }
                }
            }
        }
        return TextUtils.join(CONTEXT_SEPARATOR, terms)
    }

    /**
     * Extracts the previous words context.
     *
     * @return a String array with the previous words.
     */
    fun extractPrevWordsContextArray(): Array<String> {
        val prevTermList: ArrayList<String> = ArrayList()
        for (i in mPrevWordsInfo.indices.reversed()) {
            if (mPrevWordsInfo.get(i) != null && mPrevWordsInfo.get(i)!!.isValid) {
                val wordInfo: WordInfo? = mPrevWordsInfo.get(i)
                if (wordInfo!!.mIsBeginningOfSentence) {
                    prevTermList.add(BEGINNING_OF_SENTENCE_TAG)
                } else {
                    val term: String = wordInfo.mWord.toString()
                    if (!term.isEmpty()) {
                        prevTermList.add(term)
                    }
                }
            }
        }
        val contextStringArray: Array<String> = prevTermList.toTypedArray<String>()
        return contextStringArray
    }

    val isValid: Boolean
        get() {
            return prevWordCount > 0 && mPrevWordsInfo.get(0)!!.isValid
        }

    val isBeginningOfSentenceContext: Boolean
        get() {
            return prevWordCount > 0 && mPrevWordsInfo.get(0)!!.mIsBeginningOfSentence
        }

    // n is 1-indexed.
    // TODO: Remove
    fun getNthPrevWord(n: Int): CharSequence? {
        if (n <= 0 || n > prevWordCount) {
            return null
        }
        return mPrevWordsInfo.get(n - 1)!!.mWord
    }

    // n is 1-indexed.
    @UsedForTesting
    fun isNthPrevWordBeginningOfSentence(n: Int): Boolean {
        if (n <= 0 || n > prevWordCount) {
            return false
        }
        return mPrevWordsInfo.get(n - 1)!!.mIsBeginningOfSentence
    }

    fun outputToArray(
        codePointArrays: Array<IntArray?>,
        isBeginningOfSentenceArray: BooleanArray
    ) {
        for (i in 0 until prevWordCount) {
            val wordInfo: WordInfo? = mPrevWordsInfo.get(i)
            if (wordInfo == null || !wordInfo.isValid) {
                codePointArrays[i] = IntArray(0)
                isBeginningOfSentenceArray[i] = false
                continue
            }
            codePointArrays[i] = StringUtils.toCodePointArray(wordInfo.mWord!!)
            isBeginningOfSentenceArray[i] = wordInfo.mIsBeginningOfSentence
        }
    }

    override fun hashCode(): Int {
        var hashValue: Int = 0
        for (wordInfo: WordInfo? in mPrevWordsInfo) {
            if (wordInfo == null || WordInfo.EMPTY_WORD_INFO != wordInfo) {
                break
            }
            hashValue = hashValue xor wordInfo.hashCode()
        }
        return hashValue
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NgramContext) return false
        val prevWordsInfo: NgramContext = other

        val minLength: Int =
            min(prevWordCount.toDouble(), prevWordsInfo.prevWordCount.toDouble()).toInt()
        for (i in 0 until minLength) {
            if (mPrevWordsInfo.get(i) != prevWordsInfo.mPrevWordsInfo.get(i)) {
                return false
            }
        }
        val longerWordsInfo: Array<WordInfo?>
        val longerWordsInfoCount: Int
        if (prevWordCount > prevWordsInfo.prevWordCount) {
            longerWordsInfo = mPrevWordsInfo
            longerWordsInfoCount = prevWordCount
        } else {
            longerWordsInfo = prevWordsInfo.mPrevWordsInfo
            longerWordsInfoCount = prevWordsInfo.prevWordCount
        }
        for (i in minLength until longerWordsInfoCount) {
            if (longerWordsInfo.get(i) != null
                && WordInfo.EMPTY_WORD_INFO != longerWordsInfo.get(i)
            ) {
                return false
            }
        }
        return true
    }

    override fun toString(): String {
        val builder: StringBuffer = StringBuffer()
        for (i in 0 until prevWordCount) {
            val wordInfo: WordInfo? = mPrevWordsInfo.get(i)
            builder.append("PrevWord[")
            builder.append(i)
            builder.append("]: ")
            if (wordInfo == null) {
                builder.append("null. ")
                continue
            }
            if (!wordInfo.isValid) {
                builder.append("Empty. ")
                continue
            }
            builder.append(wordInfo.mWord)
            builder.append(", isBeginningOfSentence: ")
            builder.append(wordInfo.mIsBeginningOfSentence)
            builder.append(". ")
        }
        return builder.toString()
    }

    companion object {
        @Nonnull
        val EMPTY_PREV_WORDS_INFO: NgramContext = NgramContext(WordInfo.EMPTY_WORD_INFO)

        @Nonnull
        val BEGINNING_OF_SENTENCE: NgramContext =
            NgramContext(WordInfo.BEGINNING_OF_SENTENCE_WORD_INFO)

        const val BEGINNING_OF_SENTENCE_TAG: String = "<S>"

        const val CONTEXT_SEPARATOR: String = " "

        fun getEmptyPrevWordsContext(maxPrevWordCount: Int): NgramContext {
            return NgramContext(maxPrevWordCount, WordInfo.EMPTY_WORD_INFO)
        }
    }
}
