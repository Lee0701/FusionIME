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
import com.android.inputmethod.latin.BinaryDictionary
import com.android.inputmethod.latin.utils.CombinedFormatUtils

class ProbabilityInfo @JvmOverloads constructor(
    probability: Int,
    timestamp: Int = BinaryDictionary.NOT_A_VALID_TIMESTAMP,
    level: Int = 0,
    count: Int = 0
) {
    val mProbability: Int = probability

    // mTimestamp, mLevel and mCount are historical info. These values are depend on the
    // implementation in native code; thus, we must not use them and have any assumptions about
    // them except for tests.
    val mTimestamp: Int = timestamp
    val mLevel: Int = level
    val mCount: Int = count

    fun hasHistoricalInfo(): Boolean {
        return mTimestamp != BinaryDictionary.NOT_A_VALID_TIMESTAMP
    }

    override fun hashCode(): Int {
        if (hasHistoricalInfo()) {
            return arrayOf<Any>(mProbability, mTimestamp, mLevel, mCount).contentHashCode()
        }
        return arrayOf<Any>(mProbability).contentHashCode()
    }

    override fun toString(): String {
        return CombinedFormatUtils.formatProbabilityInfo(this)
    }

    override fun equals(o: Any?): Boolean {
        if (o === this) return true
        if (o !is ProbabilityInfo) return false
        val p = o
        if (!hasHistoricalInfo() && !p.hasHistoricalInfo()) {
            return mProbability == p.mProbability
        }
        return mProbability == p.mProbability && mTimestamp == p.mTimestamp && mLevel == p.mLevel && mCount == p.mCount
    }

    companion object {
        @UsedForTesting
        fun max(
            probabilityInfo1: ProbabilityInfo?,
            probabilityInfo2: ProbabilityInfo?
        ): ProbabilityInfo? {
            if (probabilityInfo1 == null) {
                return probabilityInfo2
            }
            if (probabilityInfo2 == null) {
                return probabilityInfo1
            }
            return if ((probabilityInfo1.mProbability > probabilityInfo2.mProbability))
                probabilityInfo1
            else
                probabilityInfo2
        }
    }
}