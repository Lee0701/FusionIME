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
package com.android.inputmethod.latin.suggestions

import android.content.Context
import android.content.res.Resources
import android.graphics.Paint
import android.graphics.drawable.Drawable
import com.android.inputmethod.keyboard.Key
import com.android.inputmethod.keyboard.Key.Spacer
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.keyboard.internal.KeyboardBuilder
import com.android.inputmethod.keyboard.internal.KeyboardIconsSet
import com.android.inputmethod.keyboard.internal.KeyboardParams
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.SuggestedWords
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.utils.TypefaceUtils
import kotlin.math.max
import kotlin.math.min

class MoreSuggestions internal constructor(
    params: MoreSuggestionsParam,
    suggestedWords: SuggestedWords?
) :
    Keyboard(params) {
    val mSuggestedWords: SuggestedWords?

    init {
        mSuggestedWords = suggestedWords
    }

    class MoreSuggestionsParam : KeyboardParams() {
        private val mWidths: IntArray = IntArray(SuggestedWords.Companion.MAX_SUGGESTIONS)
        private val mRowNumbers: IntArray = IntArray(SuggestedWords.Companion.MAX_SUGGESTIONS)
        private val mColumnOrders: IntArray = IntArray(SuggestedWords.Companion.MAX_SUGGESTIONS)
        private val mNumColumnsInRow: IntArray = IntArray(SuggestedWords.Companion.MAX_SUGGESTIONS)
        private var mNumRows: Int = 0
        var mDivider: Drawable? = null
        var mDividerWidth: Int = 0

        fun layout(
            suggestedWords: SuggestedWords, fromIndex: Int,
            maxWidth: Int, minWidth: Int, maxRow: Int, paint: Paint,
            res: Resources
        ): Int {
            clearKeys()
            mDivider = res.getDrawable(R.drawable.more_suggestions_divider)
            mDividerWidth = mDivider.getIntrinsicWidth()
            val padding: Float = res.getDimension(
                R.dimen.config_more_suggestions_key_horizontal_padding
            )

            var row: Int = 0
            var index: Int = fromIndex
            var rowStartIndex: Int = fromIndex
            val size: Int = min(
                suggestedWords.size().toDouble(),
                SuggestedWords.Companion.MAX_SUGGESTIONS.toDouble()
            ).toInt()
            while (index < size) {
                val word: String?
                if (isIndexSubjectToAutoCorrection(suggestedWords, index)) {
                    // INDEX_OF_AUTO_CORRECTION and INDEX_OF_TYPED_WORD got swapped.
                    word = suggestedWords.getLabel(SuggestedWords.Companion.INDEX_OF_TYPED_WORD)
                } else {
                    word = suggestedWords.getLabel(index)
                }
                // TODO: Should take care of text x-scaling.
                mWidths.get(index) = (TypefaceUtils.getStringWidth(
                    word!!, paint
                ) + padding).toInt()
                val numColumn: Int = index - rowStartIndex + 1
                val columnWidth: Int =
                    (maxWidth - mDividerWidth * (numColumn - 1)) / numColumn
                if (numColumn > MAX_COLUMNS_IN_ROW
                    || !fitInWidth(rowStartIndex, index + 1, columnWidth)
                ) {
                    if ((row + 1) >= maxRow) {
                        break
                    }
                    mNumColumnsInRow.get(row) = index - rowStartIndex
                    rowStartIndex = index
                    row++
                }
                mColumnOrders.get(index) = index - rowStartIndex
                mRowNumbers.get(index) = row
                index++
            }
            mNumColumnsInRow.get(row) = index - rowStartIndex
            mNumRows = row + 1
            mOccupiedWidth = max(
                minWidth.toDouble(), calcurateMaxRowWidth(fromIndex, index).toDouble()
            ).toInt()
            mBaseWidth = mOccupiedWidth
            mOccupiedHeight = mNumRows * mDefaultRowHeight + mVerticalGap
            mBaseHeight = mOccupiedHeight
            return index - fromIndex
        }

        fun fitInWidth(startIndex: Int, endIndex: Int, width: Int): Boolean {
            for (index in startIndex until endIndex) {
                if (mWidths.get(index) > width) return false
            }
            return true
        }

        fun calcurateMaxRowWidth(startIndex: Int, endIndex: Int): Int {
            var maxRowWidth: Int = 0
            var index: Int = startIndex
            for (row in 0 until mNumRows) {
                val numColumnInRow: Int = mNumColumnsInRow.get(row)
                var maxKeyWidth: Int = 0
                while (index < endIndex && mRowNumbers.get(index) == row) {
                    maxKeyWidth =
                        max(maxKeyWidth.toDouble(), mWidths.get(index).toDouble()).toInt()
                    index++
                }
                maxRowWidth = max(
                    maxRowWidth.toDouble(),
                    (maxKeyWidth * numColumnInRow + mDividerWidth * (numColumnInRow - 1)).toDouble()
                ).toInt()
            }
            return maxRowWidth
        }

        fun getNumColumnInRow(index: Int): Int {
            return mNumColumnsInRow.get(mRowNumbers.get(index))
        }

        fun getColumnNumber(index: Int): Int {
            val columnOrder: Int = mColumnOrders.get(index)
            val numColumn: Int = getNumColumnInRow(index)
            return COLUMN_ORDER_TO_NUMBER.get(numColumn - 1).get(columnOrder)
        }

        fun getX(index: Int): Int {
            val columnNumber: Int = getColumnNumber(index)
            return columnNumber * (getWidth(index) + mDividerWidth)
        }

        fun getY(index: Int): Int {
            val row: Int = mRowNumbers.get(index)
            return (mNumRows - 1 - row) * mDefaultRowHeight + mTopPadding
        }

        fun getWidth(index: Int): Int {
            val numColumnInRow: Int = getNumColumnInRow(index)
            return (mOccupiedWidth - mDividerWidth * (numColumnInRow - 1)) / numColumnInRow
        }

        fun markAsEdgeKey(key: Key, index: Int) {
            val row: Int = mRowNumbers.get(index)
            if (row == 0) key.markAsBottomEdge(this)
            if (row == mNumRows - 1) key.markAsTopEdge(this)

            val numColumnInRow: Int = mNumColumnsInRow.get(row)
            val column: Int = getColumnNumber(index)
            if (column == 0) key.markAsLeftEdge(this)
            if (column == numColumnInRow - 1) key.markAsRightEdge(this)
        }

        companion object {
            private const val MAX_COLUMNS_IN_ROW: Int = 3
            private val COLUMN_ORDER_TO_NUMBER: Array<IntArray> = arrayOf(
                intArrayOf(0),  // center
                intArrayOf(1, 0),  // right-left
                intArrayOf(1, 0, 2),  // center-left-right
            )
        }
    }

    class Builder(context: Context, paneView: MoreSuggestionsView) :
        KeyboardBuilder<MoreSuggestionsParam?>(context, MoreSuggestionsParam()) {
        private val mPaneView: MoreSuggestionsView
        private var mSuggestedWords: SuggestedWords? = null
        private var mFromIndex: Int = 0
        private var mToIndex: Int = 0

        init {
            mPaneView = paneView
        }

        fun layout(
            suggestedWords: SuggestedWords, fromIndex: Int,
            maxWidth: Int, minWidth: Int, maxRow: Int,
            parentKeyboard: Keyboard
        ): Builder {
            val xmlId: Int = R.xml.kbd_suggestions_pane_template
            load(xmlId, parentKeyboard.mId)
            mParams!!.mTopPadding = parentKeyboard.mVerticalGap / 2
            mParams.mVerticalGap = mParams.mTopPadding
            mPaneView.updateKeyboardGeometry(mParams.mDefaultRowHeight)
            val count: Int = mParams.layout(
                suggestedWords, fromIndex, maxWidth, minWidth, maxRow,
                mPaneView.newLabelPaint(null /* key */), mResources
            )
            mFromIndex = fromIndex
            mToIndex = fromIndex + count
            mSuggestedWords = suggestedWords
            return this
        }

        override fun build(): MoreSuggestions {
            val params: MoreSuggestionsParam = mParams!!
            for (index in mFromIndex until mToIndex) {
                val x: Int = params.getX(index)
                val y: Int = params.getY(index)
                val width: Int = params.getWidth(index)
                val word: String?
                val info: String?
                if (isIndexSubjectToAutoCorrection(mSuggestedWords!!, index)) {
                    // INDEX_OF_AUTO_CORRECTION and INDEX_OF_TYPED_WORD got swapped.
                    word = mSuggestedWords!!.getLabel(SuggestedWords.Companion.INDEX_OF_TYPED_WORD)
                    info =
                        mSuggestedWords!!.getDebugString(SuggestedWords.Companion.INDEX_OF_TYPED_WORD)
                } else {
                    word = mSuggestedWords!!.getLabel(index)
                    info = mSuggestedWords!!.getDebugString(index)
                }
                val key: Key = MoreSuggestionKey(word, info, index, params)
                params.markAsEdgeKey(key, index)
                params.onAddKey(key)
                val columnNumber: Int = params.getColumnNumber(index)
                val numColumnInRow: Int = params.getNumColumnInRow(index)
                if (columnNumber < numColumnInRow - 1) {
                    val divider: Divider = Divider(
                        params, params.mDivider, x + width, y,
                        params.mDividerWidth, params.mDefaultRowHeight
                    )
                    params.onAddKey(divider)
                }
            }
            return MoreSuggestions(params, mSuggestedWords)
        }
    }

    internal class MoreSuggestionKey(
        word: String?, info: String?, index: Int,
        params: MoreSuggestionsParam
    ) : Key(
        word,  /* label */KeyboardIconsSet.Companion.ICON_UNDEFINED, Constants.CODE_OUTPUT_TEXT,
        word,  /* outputText */info, 0,  /* labelFlags */
        Key.Companion.BACKGROUND_TYPE_NORMAL,
        params.getX(index), params.getY(index), params.getWidth(index),
        params.mDefaultRowHeight, params.mHorizontalGap, params.mVerticalGap
    ) {
        val mSuggestedWordIndex: Int

        init {
            mSuggestedWordIndex = index
        }
    }

    private class Divider(
        params: KeyboardParams, icon: Drawable?, x: Int,
        y: Int, width: Int, height: Int
    ) :
        Spacer(params, x, y, width, height) {
        private val mIcon: Drawable?

        init {
            mIcon = icon
        }

        override fun getIcon(iconSet: KeyboardIconsSet, alpha: Int): Drawable? {
            // KeyboardIconsSet and alpha are unused. Use the icon that has been passed to the
            // constructor.
            // TODO: Drawable itself should have an alpha value.
            mIcon!!.setAlpha(128)
            return mIcon
        }
    }

    companion object {
        fun isIndexSubjectToAutoCorrection(
            suggestedWords: SuggestedWords,
            index: Int
        ): Boolean {
            return suggestedWords.mWillAutoCorrect && index == SuggestedWords.Companion.INDEX_OF_AUTO_CORRECTION
        }
    }
}
