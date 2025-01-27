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
package com.android.inputmethod.latin.makedict

import com.android.inputmethod.annotations.UsedForTesting

/**
 * A string with a probability.
 *
 * This represents an "attribute", that is either a bigram or a shortcut.
 */
class WeightedString(word: String, probabilityInfo: ProbabilityInfo) {
    val mWord: String = word
    var mProbabilityInfo: ProbabilityInfo

    constructor(word: String, probability: Int) : this(word, ProbabilityInfo(probability))

    init {
        mProbabilityInfo = probabilityInfo
    }

    @get:UsedForTesting
    var probability: Int
        get() = mProbabilityInfo.mProbability
        set(probability) {
            mProbabilityInfo = ProbabilityInfo(probability)
        }

    override fun hashCode(): Int {
        return arrayOf(mWord, mProbabilityInfo).contentHashCode()
    }

    override fun equals(o: Any?): Boolean {
        if (o === this) return true
        if (o !is WeightedString) return false
        val w = o
        return mWord == w.mWord && mProbabilityInfo == w.mProbabilityInfo
    }
}