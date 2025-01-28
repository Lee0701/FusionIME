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
package com.android.inputmethod.keyboard.internal

import android.util.SparseIntArray
import com.android.inputmethod.keyboard.Key
import com.android.inputmethod.keyboard.KeyboardId
import com.android.inputmethod.keyboard.internal.MoreKeySpec.LettersOnBaseLayout
import com.android.inputmethod.latin.common.Constants
import java.util.SortedSet
import java.util.TreeSet

open class KeyboardParams @JvmOverloads constructor(keysCache: UniqueKeysCache = UniqueKeysCache.NO_CACHE) {
    var mId: KeyboardId? = null
    var mThemeId: Int = 0

    /** Total height and width of the keyboard, including the paddings and keys  */
    var mOccupiedHeight: Int = 0
    var mOccupiedWidth: Int = 0

    /** Base height and width of the keyboard used to calculate rows' or keys' heights and
     * widths
     */
    var mBaseHeight: Int = 0
    var mBaseWidth: Int = 0

    var mTopPadding: Int = 0
    var mBottomPadding: Int = 0
    var mLeftPadding: Int = 0
    var mRightPadding: Int = 0

    var mKeyVisualAttributes: KeyVisualAttributes? = null

    var mDefaultRowHeight: Int = 0
    var mDefaultKeyWidth: Int = 0
    var mHorizontalGap: Int = 0
    var mVerticalGap: Int = 0

    var mMoreKeysTemplate: Int = 0
    var mMaxMoreKeysKeyboardColumn: Int = 0

    var GRID_WIDTH: Int = 0
    var GRID_HEIGHT: Int = 0

    // Keys are sorted from top-left to bottom-right order.
    val mSortedKeys: SortedSet<Key> = TreeSet(
        ROW_COLUMN_COMPARATOR
    )

    val mShiftKeys: ArrayList<Key> = ArrayList()

    val mAltCodeKeysWhileTyping: ArrayList<Key> = ArrayList()

    val mIconsSet: KeyboardIconsSet = KeyboardIconsSet()

    val mTextsSet: KeyboardTextsSet = KeyboardTextsSet()

    val mKeyStyles: KeyStylesSet = KeyStylesSet(mTextsSet)

    private val mUniqueKeysCache: UniqueKeysCache
    var mAllowRedundantMoreKeys: Boolean = false

    var mMostCommonKeyHeight: Int = 0
    var mMostCommonKeyWidth: Int = 0

    var mProximityCharsCorrectionEnabled: Boolean = false

    val mTouchPositionCorrection: TouchPositionCorrection = TouchPositionCorrection()

    protected fun clearKeys() {
        mSortedKeys.clear()
        mShiftKeys.clear()
        clearHistogram()
    }

    fun onAddKey(newKey: Key) {
        val key: Key = mUniqueKeysCache.getUniqueKey(newKey)
        val isSpacer: Boolean = key.isSpacer
        if (isSpacer && key.width == 0) {
            // Ignore zero width {@link Spacer}.
            return
        }
        mSortedKeys.add(key)
        if (isSpacer) {
            return
        }
        updateHistogram(key)
        if (key.code == Constants.CODE_SHIFT) {
            mShiftKeys.add(key)
        }
        if (key.altCodeWhileTyping()) {
            mAltCodeKeysWhileTyping.add(key)
        }
    }

    fun removeRedundantMoreKeys() {
        if (mAllowRedundantMoreKeys) {
            return
        }
        val lettersOnBaseLayout: LettersOnBaseLayout =
            LettersOnBaseLayout()
        for (key: Key in mSortedKeys) {
            lettersOnBaseLayout.addLetter(key)
        }
        val allKeys: ArrayList<Key> = ArrayList(mSortedKeys)
        mSortedKeys.clear()
        for (key: Key in allKeys) {
            val filteredKey: Key = Key.removeRedundantMoreKeys(key, lettersOnBaseLayout)
            mSortedKeys.add(mUniqueKeysCache.getUniqueKey(filteredKey))
        }
    }

    private var mMaxHeightCount: Int = 0
    private var mMaxWidthCount: Int = 0
    private val mHeightHistogram: SparseIntArray = SparseIntArray()
    private val mWidthHistogram: SparseIntArray = SparseIntArray()

    init {
        mUniqueKeysCache = keysCache
    }

    private fun clearHistogram() {
        mMostCommonKeyHeight = 0
        mMaxHeightCount = 0
        mHeightHistogram.clear()

        mMaxWidthCount = 0
        mMostCommonKeyWidth = 0
        mWidthHistogram.clear()
    }

    private fun updateHistogram(key: Key) {
        val height: Int = key.height + mVerticalGap
        val heightCount: Int = updateHistogramCounter(mHeightHistogram, height)
        if (heightCount > mMaxHeightCount) {
            mMaxHeightCount = heightCount
            mMostCommonKeyHeight = height
        }

        val width: Int = key.width + mHorizontalGap
        val widthCount: Int = updateHistogramCounter(mWidthHistogram, width)
        if (widthCount > mMaxWidthCount) {
            mMaxWidthCount = widthCount
            mMostCommonKeyWidth = width
        }
    }

    companion object {
        // Comparator to sort {@link Key}s from top-left to bottom-right order.
        private val ROW_COLUMN_COMPARATOR: Comparator<Key> = object : Comparator<Key> {
            override fun compare(lhs: Key, rhs: Key): Int {
                if (lhs.y < rhs.y) return -1
                if (lhs.y > rhs.y) return 1
                if (lhs.x < rhs.x) return -1
                if (lhs.x > rhs.x) return 1
                return 0
            }
        }

        private fun updateHistogramCounter(histogram: SparseIntArray, key: Int): Int {
            val index: Int = histogram.indexOfKey(key)
            val count: Int = (if (index >= 0) histogram.get(key) else 0) + 1
            histogram.put(key, count)
            return count
        }
    }
}
