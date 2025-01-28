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

import com.android.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import com.android.inputmethod.latin.define.ProductionFlags
import java.util.TreeSet

/**
 * A TreeSet of SuggestedWordInfo that is bounded in size and throws everything that's smaller
 * than its limit
 */
class SuggestionResults private constructor(
    comparator: Comparator<SuggestedWordInfo>, capacity: Int,
    isBeginningOfSentence: Boolean,
    firstSuggestionExceedsConfidenceThreshold: Boolean
) :
    TreeSet<SuggestedWordInfo>(comparator) {
    var mRawSuggestions: ArrayList<SuggestedWordInfo>? = null

    // TODO: Instead of a boolean , we may want to include the context of this suggestion results,
    // such as {@link NgramContext}.
    val mIsBeginningOfSentence: Boolean
    val mFirstSuggestionExceedsConfidenceThreshold: Boolean
    private val mCapacity = capacity

    constructor(
        capacity: Int, isBeginningOfSentence: Boolean,
        firstSuggestionExceedsConfidenceThreshold: Boolean
    ) : this(
        sSuggestedWordInfoComparator, capacity, isBeginningOfSentence,
        firstSuggestionExceedsConfidenceThreshold
    )

    override fun add(e: SuggestedWordInfo): Boolean {
        if (size < mCapacity) return super.add(e)
        if (comparator().compare(e, last()) > 0) return false
        super.add(e)
        pollLast() // removes the last element
        return true
    }

    internal class SuggestedWordInfoComparator : Comparator<SuggestedWordInfo> {
        // This comparator ranks the word info with the higher frequency first. That's because
        // that's the order we want our elements in.
        override fun compare(o1: SuggestedWordInfo, o2: SuggestedWordInfo): Int {
            if (o1.mScore > o2.mScore) return -1
            if (o1.mScore < o2.mScore) return 1
            if (o1.mCodePointCount < o2.mCodePointCount) return -1
            if (o1.mCodePointCount > o2.mCodePointCount) return 1
            return o1.word.compareTo(o2.word)
        }
    }

    init {
        mRawSuggestions = if (ProductionFlags.INCLUDE_RAW_SUGGESTIONS) {
            ArrayList()
        } else {
            null
        }
        mIsBeginningOfSentence = isBeginningOfSentence
        mFirstSuggestionExceedsConfidenceThreshold = firstSuggestionExceedsConfidenceThreshold
    }

    companion object {
        private val sSuggestedWordInfoComparator = SuggestedWordInfoComparator()
    }
}
