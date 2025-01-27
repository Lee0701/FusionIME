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

import android.content.res.TypedArray
import android.graphics.Typeface
import android.util.SparseIntArray
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.utils.ResourceUtils
import javax.annotation.Nonnull

class KeyVisualAttributes private constructor(@Nonnull keyAttr: TypedArray) {
    var mTypeface: Typeface? = null

    val mLetterRatio: Float
    val mLetterSize: Int
    val mLabelRatio: Float
    val mLabelSize: Int
    val mLargeLetterRatio: Float
    val mHintLetterRatio: Float
    val mShiftedLetterHintRatio: Float
    val mHintLabelRatio: Float
    val mPreviewTextRatio: Float

    val mTextColor: Int
    val mTextInactivatedColor: Int
    val mTextShadowColor: Int
    val mFunctionalTextColor: Int
    val mHintLetterColor: Int
    val mHintLabelColor: Int
    val mShiftedLetterHintInactivatedColor: Int
    val mShiftedLetterHintActivatedColor: Int
    val mPreviewTextColor: Int

    val mHintLabelVerticalAdjustment: Float
    val mLabelOffCenterRatio: Float
    val mHintLabelOffCenterRatio: Float

    init {
        if (keyAttr.hasValue(R.styleable.Keyboard_Key_keyTypeface)) {
            mTypeface = Typeface.defaultFromStyle(
                keyAttr.getInt(R.styleable.Keyboard_Key_keyTypeface, Typeface.NORMAL)
            )
        } else {
            mTypeface = null
        }

        mLetterRatio = ResourceUtils.getFraction(
            keyAttr,
            R.styleable.Keyboard_Key_keyLetterSize
        )
        mLetterSize = ResourceUtils.getDimensionPixelSize(
            keyAttr,
            R.styleable.Keyboard_Key_keyLetterSize
        )
        mLabelRatio = ResourceUtils.getFraction(
            keyAttr,
            R.styleable.Keyboard_Key_keyLabelSize
        )
        mLabelSize = ResourceUtils.getDimensionPixelSize(
            keyAttr,
            R.styleable.Keyboard_Key_keyLabelSize
        )
        mLargeLetterRatio = ResourceUtils.getFraction(
            keyAttr,
            R.styleable.Keyboard_Key_keyLargeLetterRatio
        )
        mHintLetterRatio = ResourceUtils.getFraction(
            keyAttr,
            R.styleable.Keyboard_Key_keyHintLetterRatio
        )
        mShiftedLetterHintRatio = ResourceUtils.getFraction(
            keyAttr,
            R.styleable.Keyboard_Key_keyShiftedLetterHintRatio
        )
        mHintLabelRatio = ResourceUtils.getFraction(
            keyAttr,
            R.styleable.Keyboard_Key_keyHintLabelRatio
        )
        mPreviewTextRatio = ResourceUtils.getFraction(
            keyAttr,
            R.styleable.Keyboard_Key_keyPreviewTextRatio
        )

        mTextColor = keyAttr.getColor(R.styleable.Keyboard_Key_keyTextColor, 0)
        mTextInactivatedColor = keyAttr.getColor(
            R.styleable.Keyboard_Key_keyTextInactivatedColor, 0
        )
        mTextShadowColor = keyAttr.getColor(R.styleable.Keyboard_Key_keyTextShadowColor, 0)
        mFunctionalTextColor = keyAttr.getColor(R.styleable.Keyboard_Key_functionalTextColor, 0)
        mHintLetterColor = keyAttr.getColor(R.styleable.Keyboard_Key_keyHintLetterColor, 0)
        mHintLabelColor = keyAttr.getColor(R.styleable.Keyboard_Key_keyHintLabelColor, 0)
        mShiftedLetterHintInactivatedColor = keyAttr.getColor(
            R.styleable.Keyboard_Key_keyShiftedLetterHintInactivatedColor, 0
        )
        mShiftedLetterHintActivatedColor = keyAttr.getColor(
            R.styleable.Keyboard_Key_keyShiftedLetterHintActivatedColor, 0
        )
        mPreviewTextColor = keyAttr.getColor(R.styleable.Keyboard_Key_keyPreviewTextColor, 0)

        mHintLabelVerticalAdjustment = ResourceUtils.getFraction(
            keyAttr,
            R.styleable.Keyboard_Key_keyHintLabelVerticalAdjustment, 0.0f
        )
        mLabelOffCenterRatio = ResourceUtils.getFraction(
            keyAttr,
            R.styleable.Keyboard_Key_keyLabelOffCenterRatio, 0.0f
        )
        mHintLabelOffCenterRatio = ResourceUtils.getFraction(
            keyAttr,
            R.styleable.Keyboard_Key_keyHintLabelOffCenterRatio, 0.0f
        )
    }

    companion object {
        private val VISUAL_ATTRIBUTE_IDS: IntArray = intArrayOf(
            R.styleable.Keyboard_Key_keyTypeface,
            R.styleable.Keyboard_Key_keyLetterSize,
            R.styleable.Keyboard_Key_keyLabelSize,
            R.styleable.Keyboard_Key_keyLargeLetterRatio,
            R.styleable.Keyboard_Key_keyHintLetterRatio,
            R.styleable.Keyboard_Key_keyShiftedLetterHintRatio,
            R.styleable.Keyboard_Key_keyHintLabelRatio,
            R.styleable.Keyboard_Key_keyPreviewTextRatio,
            R.styleable.Keyboard_Key_keyTextColor,
            R.styleable.Keyboard_Key_keyTextInactivatedColor,
            R.styleable.Keyboard_Key_keyTextShadowColor,
            R.styleable.Keyboard_Key_functionalTextColor,
            R.styleable.Keyboard_Key_keyHintLetterColor,
            R.styleable.Keyboard_Key_keyHintLabelColor,
            R.styleable.Keyboard_Key_keyShiftedLetterHintInactivatedColor,
            R.styleable.Keyboard_Key_keyShiftedLetterHintActivatedColor,
            R.styleable.Keyboard_Key_keyPreviewTextColor,
            R.styleable.Keyboard_Key_keyHintLabelVerticalAdjustment,
            R.styleable.Keyboard_Key_keyLabelOffCenterRatio,
            R.styleable.Keyboard_Key_keyHintLabelOffCenterRatio
        )
        private val sVisualAttributeIds: SparseIntArray = SparseIntArray()
        private const val ATTR_DEFINED: Int = 1
        private const val ATTR_NOT_FOUND: Int = 0

        init {
            for (attrId: Int in VISUAL_ATTRIBUTE_IDS) {
                sVisualAttributeIds.put(attrId, ATTR_DEFINED)
            }
        }

        fun newInstance(@Nonnull keyAttr: TypedArray): KeyVisualAttributes? {
            val indexCount: Int = keyAttr.getIndexCount()
            for (i in 0 until indexCount) {
                val attrId: Int = keyAttr.getIndex(i)
                if (sVisualAttributeIds.get(attrId, ATTR_NOT_FOUND) == ATTR_NOT_FOUND) {
                    continue
                }
                return KeyVisualAttributes(keyAttr)
            }
            return null
        }
    }
}
