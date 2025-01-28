/*
 * Copyright (C) 2013 The Android Open Source Project
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

import com.android.inputmethod.latin.common.StringUtils
import java.util.Locale

/**
 * The status of the current recapitalize process.
 */
class RecapitalizeStatus {
    /**
     * We store the location of the cursor and the string that was there before the recapitalize
     * action was done, and the location of the cursor and the string that was there after.
     */
    private var mCursorStartBefore = 0
    private var mStringBefore: String? = null
    var newCursorStart: Int = 0
        private set
    var newCursorEnd: Int = 0
        private set
    private var mRotationStyleCurrentIndex = 0
    private var mSkipOriginalMixedCaseMode = false
    private var mLocale: Locale? = null
    private var mSortedSeparators: IntArray = intArrayOf()
    var recapitalizedString: String? = null
        private set
    var isStarted: Boolean = false
        private set
    private var mIsEnabled = true

    init {
        // By default, initialize with fake values that won't match any real recapitalize.
        start(-1, -1, "", Locale.getDefault(), EMPTY_STORTED_SEPARATORS)
        stop()
    }

    fun start(
        cursorStart: Int, cursorEnd: Int, string: String?,
        locale: Locale?, sortedSeparators: IntArray
    ) {
        if (!mIsEnabled) {
            return
        }
        mCursorStartBefore = cursorStart
        mStringBefore = string
        newCursorStart = cursorStart
        newCursorEnd = cursorEnd
        recapitalizedString = string
        val initialMode = getStringMode(mStringBefore!!, sortedSeparators)
        mLocale = locale
        mSortedSeparators = sortedSeparators
        if (CAPS_MODE_ORIGINAL_MIXED_CASE == initialMode) {
            mRotationStyleCurrentIndex = 0
            mSkipOriginalMixedCaseMode = false
        } else {
            // Find the current mode in the array.
            var currentMode = ROTATION_STYLE.size - 1
            while (currentMode > 0) {
                if (ROTATION_STYLE[currentMode] == initialMode) {
                    break
                }
                --currentMode
            }
            mRotationStyleCurrentIndex = currentMode
            mSkipOriginalMixedCaseMode = true
        }
        isStarted = true
    }

    fun stop() {
        isStarted = false
    }

    fun enable() {
        mIsEnabled = true
    }

    fun disable() {
        mIsEnabled = false
    }

    fun mIsEnabled(): Boolean {
        return mIsEnabled
    }

    fun isSetAt(cursorStart: Int, cursorEnd: Int): Boolean {
        return cursorStart == newCursorStart && cursorEnd == newCursorEnd
    }

    /**
     * Rotate through the different possible capitalization modes.
     */
    fun rotate() {
        val oldResult = recapitalizedString
        var count = 0 // Protection against infinite loop.
        do {
            mRotationStyleCurrentIndex = (mRotationStyleCurrentIndex + 1) % ROTATION_STYLE.size
            if (CAPS_MODE_ORIGINAL_MIXED_CASE == ROTATION_STYLE[mRotationStyleCurrentIndex]
                && mSkipOriginalMixedCaseMode
            ) {
                mRotationStyleCurrentIndex =
                    (mRotationStyleCurrentIndex + 1) % ROTATION_STYLE.size
            }
            ++count
            when (ROTATION_STYLE[mRotationStyleCurrentIndex]) {
                CAPS_MODE_ORIGINAL_MIXED_CASE -> recapitalizedString = mStringBefore
                CAPS_MODE_ALL_LOWER -> recapitalizedString = mStringBefore?.lowercase(
                    mLocale!!
                )

                CAPS_MODE_FIRST_WORD_UPPER -> recapitalizedString = StringUtils.capitalizeEachWord(
                    mStringBefore!!, mSortedSeparators,
                    mLocale!!
                )

                CAPS_MODE_ALL_UPPER -> recapitalizedString = mStringBefore?.uppercase(
                    mLocale!!
                )

                else -> recapitalizedString = mStringBefore
            }
        } while (recapitalizedString == oldResult && count < ROTATION_STYLE.size + 1)
        newCursorEnd = newCursorStart + recapitalizedString!!.length
    }

    /**
     * Remove leading/trailing whitespace from the considered string.
     */
    fun trim() {
        val len = mStringBefore!!.length
        var nonWhitespaceStart = 0
        while (nonWhitespaceStart < len
        ) {
            val codePoint = mStringBefore!!.codePointAt(nonWhitespaceStart)
            if (!Character.isWhitespace(codePoint)) break
            nonWhitespaceStart = mStringBefore!!.offsetByCodePoints(nonWhitespaceStart, 1)
        }
        var nonWhitespaceEnd = len
        while (nonWhitespaceEnd > 0
        ) {
            val codePoint = mStringBefore!!.codePointBefore(nonWhitespaceEnd)
            if (!Character.isWhitespace(codePoint)) break
            nonWhitespaceEnd = mStringBefore!!.offsetByCodePoints(nonWhitespaceEnd, -1)
        }
        // If nonWhitespaceStart >= nonWhitespaceEnd, that means the selection contained only
        // whitespace, so we leave it as is.
        if ((0 != nonWhitespaceStart || len != nonWhitespaceEnd)
            && nonWhitespaceStart < nonWhitespaceEnd
        ) {
            newCursorEnd = mCursorStartBefore + nonWhitespaceEnd
            newCursorStart = mCursorStartBefore + nonWhitespaceStart
            mCursorStartBefore = newCursorStart
            mStringBefore =
                mStringBefore?.substring(nonWhitespaceStart, nonWhitespaceEnd)
            recapitalizedString = mStringBefore
        }
    }

    val currentMode: Int
        get() = ROTATION_STYLE[mRotationStyleCurrentIndex]

    companion object {
        const val NOT_A_RECAPITALIZE_MODE: Int = -1
        const val CAPS_MODE_ORIGINAL_MIXED_CASE: Int = 0
        const val CAPS_MODE_ALL_LOWER: Int = 1
        const val CAPS_MODE_FIRST_WORD_UPPER: Int = 2
        const val CAPS_MODE_ALL_UPPER: Int = 3

        // When adding a new mode, don't forget to update the CAPS_MODE_LAST constant.
        const val CAPS_MODE_LAST: Int = CAPS_MODE_ALL_UPPER

        private val ROTATION_STYLE = intArrayOf(
            CAPS_MODE_ORIGINAL_MIXED_CASE,
            CAPS_MODE_ALL_LOWER,
            CAPS_MODE_FIRST_WORD_UPPER,
            CAPS_MODE_ALL_UPPER
        )

        private fun getStringMode(string: String, sortedSeparators: IntArray): Int {
            return if (StringUtils.isIdenticalAfterUpcase(
                    string
                )
            ) {
                CAPS_MODE_ALL_UPPER
            } else if (StringUtils.isIdenticalAfterDowncase(
                    string
                )
            ) {
                CAPS_MODE_ALL_LOWER
            } else if (StringUtils.isIdenticalAfterCapitalizeEachWord(
                    string,
                    sortedSeparators
                )
            ) {
                CAPS_MODE_FIRST_WORD_UPPER
            } else {
                CAPS_MODE_ORIGINAL_MIXED_CASE
            }
        }

        fun modeToString(recapitalizeMode: Int): String {
            return when (recapitalizeMode) {
                NOT_A_RECAPITALIZE_MODE -> "undefined"
                CAPS_MODE_ORIGINAL_MIXED_CASE -> "mixedCase"
                CAPS_MODE_ALL_LOWER -> "allLower"
                CAPS_MODE_FIRST_WORD_UPPER -> "firstWordUpper"
                CAPS_MODE_ALL_UPPER -> "allUpper"
                else -> "unknown<$recapitalizeMode>"
            }
        }

        private val EMPTY_STORTED_SEPARATORS = intArrayOf()
    }
}
