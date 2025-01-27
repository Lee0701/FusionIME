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
package com.android.inputmethod.keyboard

import android.content.Context
import android.graphics.Paint
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.keyboard.Key.Spacer
import com.android.inputmethod.keyboard.MoreKeysKeyboardView
import com.android.inputmethod.keyboard.internal.KeyboardBuilder
import com.android.inputmethod.keyboard.internal.KeyboardParams
import com.android.inputmethod.keyboard.internal.MoreKeySpec
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.common.StringUtils
import com.android.inputmethod.latin.utils.TypefaceUtils
import javax.annotation.Nonnull
import kotlin.math.max
import kotlin.math.min

class MoreKeysKeyboard internal constructor(params: MoreKeysKeyboardParams) : Keyboard(params) {
    private val mDefaultKeyCoordX: Int

    init {
        mDefaultKeyCoordX = params.getDefaultKeyCoordX() + params.mDefaultKeyWidth / 2
    }

    fun getDefaultCoordX(): Int {
        return mDefaultKeyCoordX
    }

    @UsedForTesting
    class MoreKeysKeyboardParams : KeyboardParams() {
        var mIsMoreKeysFixedOrder: Boolean = false

        /* package */
        var mTopRowAdjustment: Int = 0
        var mNumRows: Int = 0
        var mNumColumns: Int = 0
        var mTopKeys: Int = 0
        var mLeftKeys: Int = 0
        var mRightKeys: Int = 0 // includes default key.
        var mDividerWidth: Int = 0
        var mColumnWidth: Int = 0

        /**
         * Set keyboard parameters of more keys keyboard.
         *
         * @param numKeys number of keys in this more keys keyboard.
         * @param numColumn number of columns of this more keys keyboard.
         * @param keyWidth more keys keyboard key width in pixel, including horizontal gap.
         * @param rowHeight more keys keyboard row height in pixel, including vertical gap.
         * @param coordXInParent coordinate x of the key preview in parent keyboard.
         * @param parentKeyboardWidth parent keyboard width in pixel.
         * @param isMoreKeysFixedColumn true if more keys keyboard should have
         * `numColumn` columns. Otherwise more keys keyboard should have
         * `numColumn` columns at most.
         * @param isMoreKeysFixedOrder true if the order of more keys is determined by the order in
         * the more keys' specification. Otherwise the order of more keys is automatically
         * determined.
         * @param dividerWidth width of divider, zero for no dividers.
         */
        fun setParameters(
            numKeys: Int, numColumn: Int, keyWidth: Int,
            rowHeight: Int, coordXInParent: Int, parentKeyboardWidth: Int,
            isMoreKeysFixedColumn: Boolean, isMoreKeysFixedOrder: Boolean,
            dividerWidth: Int
        ) {
            mIsMoreKeysFixedOrder = isMoreKeysFixedOrder
            require(
                parentKeyboardWidth / keyWidth >= min(
                    numKeys.toDouble(),
                    numColumn.toDouble()
                )
            ) {
                ("Keyboard is too small to hold more keys: "
                        + parentKeyboardWidth + " " + keyWidth + " " + numKeys + " " + numColumn)
            }
            mDefaultKeyWidth = keyWidth
            mDefaultRowHeight = rowHeight

            val numRows: Int = (numKeys + numColumn - 1) / numColumn
            mNumRows = numRows
            val numColumns: Int = if (isMoreKeysFixedColumn) min(
                numKeys.toDouble(),
                numColumn.toDouble()
            ).toInt() else
                getOptimizedColumns(numKeys, numColumn)
            mNumColumns = numColumns
            val topKeys: Int = numKeys % numColumns
            mTopKeys = if (topKeys == 0) numColumns else topKeys

            val numLeftKeys: Int = (numColumns - 1) / 2
            val numRightKeys: Int = numColumns - numLeftKeys // including default key.
            // Maximum number of keys we can layout both side of the parent key
            val maxLeftKeys: Int = coordXInParent / keyWidth
            val maxRightKeys: Int = (parentKeyboardWidth - coordXInParent) / keyWidth
            var leftKeys: Int
            var rightKeys: Int
            if (numLeftKeys > maxLeftKeys) {
                leftKeys = maxLeftKeys
                rightKeys = numColumns - leftKeys
            } else if (numRightKeys > maxRightKeys + 1) {
                rightKeys = maxRightKeys + 1 // include default key
                leftKeys = numColumns - rightKeys
            } else {
                leftKeys = numLeftKeys
                rightKeys = numRightKeys
            }
            // If the left keys fill the left side of the parent key, entire more keys keyboard
            // should be shifted to the right unless the parent key is on the left edge.
            if (maxLeftKeys == leftKeys && leftKeys > 0) {
                leftKeys--
                rightKeys++
            }
            // If the right keys fill the right side of the parent key, entire more keys
            // should be shifted to the left unless the parent key is on the right edge.
            if (maxRightKeys == rightKeys - 1 && rightKeys > 1) {
                leftKeys++
                rightKeys--
            }
            mLeftKeys = leftKeys
            mRightKeys = rightKeys

            // Adjustment of the top row.
            mTopRowAdjustment = if (isMoreKeysFixedOrder)
                getFixedOrderTopRowAdjustment()
            else
                getAutoOrderTopRowAdjustment()
            mDividerWidth = dividerWidth
            mColumnWidth = mDefaultKeyWidth + mDividerWidth
            mOccupiedWidth = mNumColumns * mColumnWidth - mDividerWidth
            mBaseWidth = mOccupiedWidth
            // Need to subtract the bottom row's gutter only.
            mOccupiedHeight =
                (mNumRows * mDefaultRowHeight - mVerticalGap + mTopPadding + mBottomPadding)
            mBaseHeight = mOccupiedHeight
        }

        private fun getFixedOrderTopRowAdjustment(): Int {
            if (mNumRows == 1 || mTopKeys % 2 == 1 || mTopKeys == mNumColumns || mLeftKeys == 0 || mRightKeys == 1) {
                return 0
            }
            return -1
        }

        private fun getAutoOrderTopRowAdjustment(): Int {
            if (mNumRows == 1 || mTopKeys == 1 || mNumColumns % 2 == mTopKeys % 2 || mLeftKeys == 0 || mRightKeys == 1) {
                return 0
            }
            return -1
        }

        // Return key position according to column count (0 is default).
        /* package */
        fun getColumnPos(n: Int): Int {
            return if (mIsMoreKeysFixedOrder) getFixedOrderColumnPos(n) else getAutomaticColumnPos(n)
        }

        private fun getFixedOrderColumnPos(n: Int): Int {
            val col: Int = n % mNumColumns
            val row: Int = n / mNumColumns
            if (!isTopRow(row)) {
                return col - mLeftKeys
            }
            val rightSideKeys: Int = mTopKeys / 2
            val leftSideKeys: Int = mTopKeys - (rightSideKeys + 1)
            val pos: Int = col - leftSideKeys
            val numLeftKeys: Int = mLeftKeys + mTopRowAdjustment
            val numRightKeys: Int = mRightKeys - 1
            if (numRightKeys >= rightSideKeys && numLeftKeys >= leftSideKeys) {
                return pos
            } else if (numRightKeys < rightSideKeys) {
                return pos - (rightSideKeys - numRightKeys)
            } else { // numLeftKeys < leftSideKeys
                return pos + (leftSideKeys - numLeftKeys)
            }
        }

        private fun getAutomaticColumnPos(n: Int): Int {
            val col: Int = n % mNumColumns
            val row: Int = n / mNumColumns
            var leftKeys: Int = mLeftKeys
            if (isTopRow(row)) {
                leftKeys += mTopRowAdjustment
            }
            if (col == 0) {
                // default position.
                return 0
            }

            var pos: Int = 0
            var right: Int = 1 // include default position key.
            var left: Int = 0
            var i: Int = 0
            while (true) {
                // Assign right key if available.
                if (right < mRightKeys) {
                    pos = right
                    right++
                    i++
                }
                if (i >= col) break
                // Assign left key if available.
                if (left < leftKeys) {
                    left++
                    pos = -left
                    i++
                }
                if (i >= col) break
            }
            return pos
        }

        private fun getOptimizedColumns(numKeys: Int, maxColumns: Int): Int {
            var numColumns: Int = min(numKeys.toDouble(), maxColumns.toDouble()).toInt()
            while (getTopRowEmptySlots(numKeys, numColumns) >= mNumRows) {
                numColumns--
            }
            return numColumns
        }

        fun getDefaultKeyCoordX(): Int {
            return mLeftKeys * mColumnWidth + mLeftPadding
        }

        fun getX(n: Int, row: Int): Int {
            val x: Int = getColumnPos(n) * mColumnWidth + getDefaultKeyCoordX()
            if (isTopRow(row)) {
                return x + mTopRowAdjustment * (mColumnWidth / 2)
            }
            return x
        }

        fun getY(row: Int): Int {
            return (mNumRows - 1 - row) * mDefaultRowHeight + mTopPadding
        }

        fun markAsEdgeKey(key: Key, row: Int) {
            if (row == 0) key.markAsTopEdge(this)
            if (isTopRow(row)) key.markAsBottomEdge(this)
        }

        private fun isTopRow(rowCount: Int): Boolean {
            return mNumRows > 1 && rowCount == mNumRows - 1
        }

        companion object {
            private fun getTopRowEmptySlots(numKeys: Int, numColumns: Int): Int {
                val remainings: Int = numKeys % numColumns
                return if (remainings == 0) 0 else numColumns - remainings
            }
        }
    }

    class Builder(
        context: Context, key: Key, keyboard: Keyboard,
        isSingleMoreKeyWithPreview: Boolean, keyPreviewVisibleWidth: Int,
        keyPreviewVisibleHeight: Int, paintToMeasure: Paint
    ) :
        KeyboardBuilder<MoreKeysKeyboardParams?>(context, MoreKeysKeyboardParams()) {
        private val mParentKey: Key

        /**
         * The builder of MoreKeysKeyboard.
         * @param context the context of [MoreKeysKeyboardView].
         * @param key the [Key] that invokes more keys keyboard.
         * @param keyboard the [Keyboard] that contains the parentKey.
         * @param isSingleMoreKeyWithPreview true if the `key` has just a single
         * "more key" and its key popup preview is enabled.
         * @param keyPreviewVisibleWidth the width of visible part of key popup preview.
         * @param keyPreviewVisibleHeight the height of visible part of key popup preview
         * @param paintToMeasure the [Paint] object to measure a "more key" width
         */
        init {
            load(keyboard.mMoreKeysTemplate, keyboard.mId)

            // TODO: More keys keyboard's vertical gap is currently calculated heuristically.
            // Should revise the algorithm.
            mParams!!.mVerticalGap = keyboard.mVerticalGap / 2
            // This {@link MoreKeysKeyboard} is invoked from the <code>key</code>.
            mParentKey = key

            val keyWidth: Int
            val rowHeight: Int
            if (isSingleMoreKeyWithPreview) {
                // Use pre-computed width and height if this more keys keyboard has only one key to
                // mitigate visual flicker between key preview and more keys keyboard.
                // Caveats for the visual assets: To achieve this effect, both the key preview
                // backgrounds and the more keys keyboard panel background have the exact same
                // left/right/top paddings. The bottom paddings of both backgrounds don't need to
                // be considered because the vertical positions of both backgrounds were already
                // adjusted with their bottom paddings deducted.
                keyWidth = keyPreviewVisibleWidth
                rowHeight = keyPreviewVisibleHeight + mParams.mVerticalGap
            } else {
                val padding: Float = (context.getResources().getDimension(
                    R.dimen.config_more_keys_keyboard_key_horizontal_padding
                )
                        + (if (key.hasLabelsInMoreKeys())
                    mParams.mDefaultKeyWidth * LABEL_PADDING_RATIO
                else
                    0.0f))
                keyWidth = getMaxKeyWidth(key, mParams.mDefaultKeyWidth, padding, paintToMeasure)
                rowHeight = keyboard.mMostCommonKeyHeight
            }
            val dividerWidth: Int
            if (key.needsDividersInMoreKeys()) {
                dividerWidth = (keyWidth * DIVIDER_RATIO).toInt()
            } else {
                dividerWidth = 0
            }
            val moreKeys: Array<MoreKeySpec?>? = key.getMoreKeys()
            mParams.setParameters(
                moreKeys!!.size, key.getMoreKeysColumnNumber(), keyWidth,
                rowHeight, key.getX() + key.getWidth() / 2, keyboard.mId!!.mWidth,
                key.isMoreKeysFixedColumn(), key.isMoreKeysFixedOrder(), dividerWidth
            )
        }

        @Nonnull
        override fun build(): MoreKeysKeyboard {
            val params: MoreKeysKeyboardParams = mParams!!
            val moreKeyFlags: Int = mParentKey.getMoreKeyLabelFlags()
            val moreKeys: Array<MoreKeySpec?>? = mParentKey.getMoreKeys()
            for (n in moreKeys!!.indices) {
                val moreKeySpec: MoreKeySpec? = moreKeys.get(n)
                val row: Int = n / params.mNumColumns
                val x: Int = params.getX(n, row)
                val y: Int = params.getY(row)
                val key: Key = moreKeySpec!!.buildKey(x, y, moreKeyFlags, params)
                params.markAsEdgeKey(key, row)
                params.onAddKey(key)

                val pos: Int = params.getColumnPos(n)
                // The "pos" value represents the offset from the default position. Negative means
                // left of the default position.
                if (params.mDividerWidth > 0 && pos != 0) {
                    val dividerX: Int = if ((pos > 0))
                        x - params.mDividerWidth
                    else
                        x + params.mDefaultKeyWidth
                    val divider: Key = MoreKeyDivider(
                        params, dividerX, y, params.mDividerWidth, params.mDefaultRowHeight
                    )
                    params.onAddKey(divider)
                }
            }
            return MoreKeysKeyboard(params)
        }

        companion object {
            private const val LABEL_PADDING_RATIO: Float = 0.2f
            private const val DIVIDER_RATIO: Float = 0.2f

            private fun getMaxKeyWidth(
                parentKey: Key, minKeyWidth: Int,
                padding: Float, paint: Paint
            ): Int {
                var maxWidth: Int = minKeyWidth
                for (spec: MoreKeySpec in parentKey.getMoreKeys()) {
                    val label: String? = spec.mLabel
                    // If the label is single letter, minKeyWidth is enough to hold the label.
                    if (label != null && StringUtils.codePointCount(label) > 1) {
                        maxWidth = max(
                            maxWidth.toDouble(),
                            (TypefaceUtils.getStringWidth(
                                label,
                                paint
                            ) + padding).toInt().toDouble()
                        ).toInt()
                    }
                }
                return maxWidth
            }
        }
    }

    // Used as a divider maker. A divider is drawn by {@link MoreKeysKeyboardView}.
    class MoreKeyDivider(
        params: KeyboardParams, x: Int, y: Int,
        width: Int, height: Int
    ) :
        Spacer(params, x, y, width, height)
}
