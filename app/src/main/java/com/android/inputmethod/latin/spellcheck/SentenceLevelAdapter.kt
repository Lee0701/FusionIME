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
package com.android.inputmethod.latin.spellcheck

import android.annotation.TargetApi
import android.content.res.Resources
import android.os.Build.VERSION_CODES
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import com.android.inputmethod.compat.TextInfoCompatUtils
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.settings.SpacingAndPunctuations
import com.android.inputmethod.latin.utils.RunInLocale
import java.util.Locale

/**
 * This code is mostly lifted directly from android.service.textservice.SpellCheckerService in
 * the framework; maybe that should be protected instead, so that implementers don't have to
 * rewrite everything for any small change.
 */
class SentenceLevelAdapter(res: Resources, locale: Locale?) {
    private object EmptySentenceSuggestionsInfosInitializationHolder {
        val emptySentenceSuggestionsInfo: Array<SentenceSuggestionsInfo?> = arrayOf()
            get() = EmptySentenceSuggestionsInfosInitializationHolder.field
    }

    /**
     * Container for split TextInfo parameters
     */
    class SentenceWordItem(ti: TextInfo?, start: Int, end: Int) {
        val mTextInfo: TextInfo? = ti
        val mStart: Int = start
        val mLength: Int = end - start
    }

    /**
     * Container for originally queried TextInfo and parameters
     */
    class SentenceTextInfoParams(ti: TextInfo, items: ArrayList<SentenceWordItem>) {
        val mOriginalTextInfo: TextInfo = ti
        val mItems: ArrayList<SentenceWordItem> = items
        val mSize: Int = items.size
    }

    private class WordIterator(res: Resources, locale: Locale?) {
        private val mSpacingAndPunctuations: SpacingAndPunctuations?

        init {
            val job: RunInLocale<SpacingAndPunctuations> =
                object : RunInLocale<SpacingAndPunctuations>() {
                    override fun job(r: Resources): SpacingAndPunctuations {
                        return SpacingAndPunctuations(r)
                    }
                }
            mSpacingAndPunctuations = job.runInLocale(res, locale)
        }

        fun getEndOfWord(sequence: CharSequence, fromIndex: Int): Int {
            val length = sequence.length
            var index =
                if (fromIndex < 0) 0 else Character.offsetByCodePoints(sequence, fromIndex, 1)
            while (index < length) {
                val codePoint = Character.codePointAt(sequence, index)
                if (mSpacingAndPunctuations!!.isWordSeparator(codePoint)) {
                    // If it's a period, we want to stop here only if it's followed by another
                    // word separator. In all other cases we stop here.
                    if (Constants.CODE_PERIOD == codePoint) {
                        val indexOfNextCodePoint =
                            index + Character.charCount(Constants.CODE_PERIOD)
                        if (indexOfNextCodePoint < length
                            && mSpacingAndPunctuations.isWordSeparator(
                                Character.codePointAt(sequence, indexOfNextCodePoint)
                            )
                        ) {
                            return index
                        }
                    } else {
                        return index
                    }
                }
                index += Character.charCount(codePoint)
            }
            return index
        }

        fun getBeginningOfNextWord(sequence: CharSequence, fromIndex: Int): Int {
            val length = sequence.length
            if (fromIndex >= length) {
                return -1
            }
            var index =
                if (fromIndex < 0) 0 else Character.offsetByCodePoints(sequence, fromIndex, 1)
            while (index < length) {
                val codePoint = Character.codePointAt(sequence, index)
                if (!mSpacingAndPunctuations!!.isWordSeparator(codePoint)) {
                    return index
                }
                index += Character.charCount(codePoint)
            }
            return -1
        }
    }

    private val mWordIterator = WordIterator(res, locale)

    fun getSplitWords(originalTextInfo: TextInfo): SentenceTextInfoParams {
        val wordIterator = mWordIterator
        val originalText =
            TextInfoCompatUtils.getCharSequenceOrString(originalTextInfo)
        val cookie = originalTextInfo.cookie
        val start = -1
        val end = originalText!!.length
        val wordItems = ArrayList<SentenceWordItem>()
        var wordStart = wordIterator.getBeginningOfNextWord(originalText, start)
        var wordEnd = wordIterator.getEndOfWord(originalText, wordStart)
        while (wordStart <= end && wordEnd != -1 && wordStart != -1) {
            if (wordEnd >= start && wordEnd > wordStart) {
                val ti = TextInfoCompatUtils.newInstance(
                    originalText, wordStart,
                    wordEnd, cookie, originalText.subSequence(wordStart, wordEnd).hashCode()
                )
                wordItems.add(SentenceWordItem(ti, wordStart, wordEnd))
            }
            wordStart = wordIterator.getBeginningOfNextWord(originalText, wordEnd)
            if (wordStart == -1) {
                break
            }
            wordEnd = wordIterator.getEndOfWord(originalText, wordStart)
        }
        return SentenceTextInfoParams(originalTextInfo, wordItems)
    }

    companion object {
        private val EMPTY_SUGGESTIONS_INFO = SuggestionsInfo(0, null)

        @TargetApi(VERSION_CODES.JELLY_BEAN)
        fun reconstructSuggestions(
            originalTextInfoParams: SentenceTextInfoParams?, results: Array<SuggestionsInfo>?
        ): SentenceSuggestionsInfo? {
            if (results == null || results.size == 0) {
                return null
            }
            if (originalTextInfoParams == null) {
                return null
            }
            val originalCookie = originalTextInfoParams.mOriginalTextInfo.cookie
            val originalSequence =
                originalTextInfoParams.mOriginalTextInfo.sequence

            val querySize = originalTextInfoParams.mSize
            val offsets = IntArray(querySize)
            val lengths = IntArray(querySize)
            val reconstructedSuggestions = arrayOfNulls<SuggestionsInfo>(querySize)
            for (i in 0 until querySize) {
                val item = originalTextInfoParams.mItems[i]
                var result: SuggestionsInfo? = null
                for (j in results.indices) {
                    val cur = results[j]
                    if (cur != null && cur.sequence == item.mTextInfo!!.sequence) {
                        result = cur
                        result.setCookieAndSequence(originalCookie, originalSequence)
                        break
                    }
                }
                offsets[i] = item.mStart
                lengths[i] = item.mLength
                reconstructedSuggestions[i] = result
                    ?: EMPTY_SUGGESTIONS_INFO
            }
            return SentenceSuggestionsInfo(reconstructedSuggestions, offsets, lengths)
        }
    }
}
