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
package com.android.inputmethod.latin.spellcheck

import android.annotation.TargetApi
import android.content.res.Resources
import android.os.Binder
import android.os.Build.VERSION_CODES
import android.text.TextUtils
import android.util.Log
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import com.android.inputmethod.compat.TextInfoCompatUtils
import com.android.inputmethod.latin.NgramContext
import com.android.inputmethod.latin.NgramContext.WordInfo
import com.android.inputmethod.latin.spellcheck.SentenceLevelAdapter.SentenceTextInfoParams
import com.android.inputmethod.latin.spellcheck.SentenceLevelAdapter.SentenceWordItem
import com.android.inputmethod.latin.utils.SpannableStringUtils
import java.util.Locale

class AndroidSpellCheckerSession(service: AndroidSpellCheckerService) :
    AndroidWordLevelSpellCheckerSession(service) {
    private val mResources: Resources
    private var mSentenceLevelAdapter: SentenceLevelAdapter? = null

    init {
        mResources = service.getResources()
    }

    @TargetApi(VERSION_CODES.JELLY_BEAN)
    private fun fixWronglyInvalidatedWordWithSingleQuote(
        ti: TextInfo,
        ssi: SentenceSuggestionsInfo
    ): SentenceSuggestionsInfo? {
        val typedText: CharSequence? = TextInfoCompatUtils.getCharSequenceOrString(ti)
        if (!typedText.toString().contains(AndroidSpellCheckerService.Companion.SINGLE_QUOTE)) {
            return null
        }
        val N: Int = ssi.getSuggestionsCount()
        val additionalOffsets: ArrayList<Int> = ArrayList()
        val additionalLengths: ArrayList<Int> = ArrayList()
        val additionalSuggestionsInfos: ArrayList<SuggestionsInfo> = ArrayList()
        var currentWord: CharSequence? = null
        for (i in 0 until N) {
            val si: SuggestionsInfo = ssi.getSuggestionsInfoAt(i)
            val flags: Int = si.getSuggestionsAttributes()
            if ((flags and SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY) == 0) {
                continue
            }
            val offset: Int = ssi.getOffsetAt(i)
            val length: Int = ssi.getLengthAt(i)
            val subText: CharSequence = typedText!!.subSequence(offset, offset + length)
            val ngramContext: NgramContext =
                NgramContext(WordInfo(currentWord))
            currentWord = subText
            if (!subText.toString().contains(AndroidSpellCheckerService.Companion.SINGLE_QUOTE)) {
                continue
            }
            // Split preserving spans.
            val splitTexts: Array<CharSequence?>? = SpannableStringUtils.split(
                subText,
                AndroidSpellCheckerService.Companion.SINGLE_QUOTE,
                true /* preserveTrailingEmptySegments */
            )
            if (splitTexts == null || splitTexts.size <= 1) {
                continue
            }
            val splitNum: Int = splitTexts.size
            for (j in 0 until splitNum) {
                val splitText: CharSequence? = splitTexts.get(j)
                if (TextUtils.isEmpty(splitText)) {
                    continue
                }
                if (mSuggestionsCache.getSuggestionsFromCache(splitText.toString()) == null) {
                    continue
                }
                val newLength: Int = splitText!!.length
                // Neither RESULT_ATTR_IN_THE_DICTIONARY nor RESULT_ATTR_LOOKS_LIKE_TYPO
                val newFlags: Int = 0
                val newSi: SuggestionsInfo = SuggestionsInfo(
                    newFlags,
                    AndroidWordLevelSpellCheckerSession.Companion.EMPTY_STRING_ARRAY
                )
                newSi.setCookieAndSequence(si.getCookie(), si.getSequence())
                if (DBG) {
                    Log.d(
                        TAG, ("Override and remove old span over: " + splitText + ", "
                                + offset + "," + newLength)
                    )
                }
                additionalOffsets.add(offset)
                additionalLengths.add(newLength)
                additionalSuggestionsInfos.add(newSi)
            }
        }
        val additionalSize: Int = additionalOffsets.size
        if (additionalSize <= 0) {
            return null
        }
        val suggestionsSize: Int = N + additionalSize
        val newOffsets: IntArray = IntArray(suggestionsSize)
        val newLengths: IntArray = IntArray(suggestionsSize)
        val newSuggestionsInfos: Array<SuggestionsInfo?> = arrayOfNulls(suggestionsSize)
        var i: Int
        i = 0
        while (i < N) {
            newOffsets.get(i) = ssi.getOffsetAt(i)
            newLengths.get(i) = ssi.getLengthAt(i)
            newSuggestionsInfos.get(i) = ssi.getSuggestionsInfoAt(i)
            ++i
        }
        while (i < suggestionsSize) {
            newOffsets.get(i) = additionalOffsets.get(i - N)
            newLengths.get(i) = additionalLengths.get(i - N)
            newSuggestionsInfos.get(i) = additionalSuggestionsInfos.get(i - N)
            ++i
        }
        return SentenceSuggestionsInfo(newSuggestionsInfos, newOffsets, newLengths)
    }

    override fun onGetSentenceSuggestionsMultiple(
        textInfos: Array<TextInfo>,
        suggestionsLimit: Int
    ): Array<SentenceSuggestionsInfo>? {
        val retval: Array<SentenceSuggestionsInfo>? = splitAndSuggest(textInfos, suggestionsLimit)
        if (retval == null || retval.size != textInfos.size) {
            return retval
        }
        for (i in retval.indices) {
            val tempSsi: SentenceSuggestionsInfo? =
                fixWronglyInvalidatedWordWithSingleQuote(textInfos.get(i), retval.get(i))
            if (tempSsi != null) {
                retval.get(i) = tempSsi
            }
        }
        return retval
    }

    /**
     * Get sentence suggestions for specified texts in an array of TextInfo. This is taken from
     * SpellCheckerService#onGetSentenceSuggestionsMultiple that we can't use because it's
     * using private variables.
     * The default implementation splits the input text to words and returns
     * [SentenceSuggestionsInfo] which contains suggestions for each word.
     * This function will run on the incoming IPC thread.
     * So, this is not called on the main thread,
     * but will be called in series on another thread.
     * @param textInfos an array of the text metadata
     * @param suggestionsLimit the maximum number of suggestions to be returned
     * @return an array of [SentenceSuggestionsInfo] returned by
     * [android.service.textservice.SpellCheckerService.Session.onGetSuggestions]
     */
    private fun splitAndSuggest(
        textInfos: Array<TextInfo>?,
        suggestionsLimit: Int
    ): Array<SentenceSuggestionsInfo?> {
        if (textInfos == null || textInfos.size == 0) {
            return SentenceLevelAdapter.Companion.getEmptySentenceSuggestionsInfo()
        }
        var sentenceLevelAdapter: SentenceLevelAdapter?
        synchronized(this) {
            sentenceLevelAdapter = mSentenceLevelAdapter
            if (sentenceLevelAdapter == null) {
                val localeStr: String = getLocale()
                if (!TextUtils.isEmpty(localeStr)) {
                    sentenceLevelAdapter = SentenceLevelAdapter(
                        mResources,
                        Locale(localeStr)
                    )
                    mSentenceLevelAdapter = sentenceLevelAdapter
                }
            }
        }
        if (sentenceLevelAdapter == null) {
            return SentenceLevelAdapter.Companion.getEmptySentenceSuggestionsInfo()
        }
        val infosSize: Int = textInfos.size
        val retval: Array<SentenceSuggestionsInfo?> = arrayOfNulls(infosSize)
        for (i in 0 until infosSize) {
            val textInfoParams: SentenceTextInfoParams =
                sentenceLevelAdapter!!.getSplitWords(textInfos.get(i))
            val mItems: ArrayList<SentenceWordItem?>? =
                textInfoParams.mItems
            val itemsSize: Int = mItems!!.size
            val splitTextInfos: Array<TextInfo?> = arrayOfNulls(itemsSize)
            for (j in 0 until itemsSize) {
                splitTextInfos.get(j) = mItems.get(j)!!.mTextInfo
            }
            retval.get(i) = SentenceLevelAdapter.Companion.reconstructSuggestions(
                textInfoParams, onGetSuggestionsMultiple(
                    splitTextInfos, suggestionsLimit, true
                )
            )
        }
        return retval
    }

    override fun onGetSuggestionsMultiple(
        textInfos: Array<TextInfo>,
        suggestionsLimit: Int, sequentialWords: Boolean
    ): Array<SuggestionsInfo> {
        val ident: Long = Binder.clearCallingIdentity()
        try {
            val length: Int = textInfos.size
            val retval: Array<SuggestionsInfo?> = arrayOfNulls(length)
            for (i in 0 until length) {
                val prevWord: CharSequence?
                if (sequentialWords && i > 0) {
                    val prevTextInfo: TextInfo = textInfos.get(i - 1)
                    val prevWordCandidate: CharSequence? =
                        TextInfoCompatUtils.getCharSequenceOrString(prevTextInfo)
                    // Note that an empty string would be used to indicate the initial word
                    // in the future.
                    prevWord = if (TextUtils.isEmpty(prevWordCandidate)) null else prevWordCandidate
                } else {
                    prevWord = null
                }
                val ngramContext: NgramContext =
                    NgramContext(WordInfo(prevWord))
                val textInfo: TextInfo = textInfos.get(i)
                retval.get(i) = onGetSuggestionsInternal(textInfo, ngramContext, suggestionsLimit)
                retval.get(i)!!.setCookieAndSequence(textInfo.getCookie(), textInfo.getSequence())
            }
            return retval
        } finally {
            Binder.restoreCallingIdentity(ident)
        }
    }

    companion object {
        private val TAG: String = AndroidSpellCheckerSession::class.java.getSimpleName()
        private const val DBG: Boolean = false
    }
}
