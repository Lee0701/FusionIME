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

import android.content.res.Resources
import android.content.res.TypedArray
import android.util.Xml
import com.android.inputmethod.keyboard.Key
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.utils.ResourceUtils
import org.xmlpull.v1.XmlPullParser
import java.util.ArrayDeque
import kotlin.math.max

/**
 * Container for keys in the keyboard. All keys in a row are at the same Y-coordinate.
 * Some of the key size defaults can be overridden per row from what the [Keyboard]
 * defines.
 */
class KeyboardRow(
    res: Resources, params: KeyboardParams,
    parser: XmlPullParser?, y: Int
) {
    private val mParams: KeyboardParams

    /** The height of this row.  */
    private val mRowHeight: Int

    private val mRowAttributesStack: ArrayDeque<RowAttributes> = ArrayDeque()

    // TODO: Add keyActionFlags.
    private class RowAttributes {
        /** Default width of a key in this row.  */
        val mDefaultKeyWidth: Float

        /** Default keyLabelFlags in this row.  */
        val mDefaultKeyLabelFlags: Int

        /** Default backgroundType for this row  */
        val mDefaultBackgroundType: Int

        /**
         * Parse and create key attributes. This constructor is used to parse Row tag.
         *
         * @param keyAttr an attributes array of Row tag.
         * @param defaultKeyWidth a default key width.
         * @param keyboardWidth the keyboard width that is required to calculate keyWidth attribute.
         */
        constructor(
            keyAttr: TypedArray, defaultKeyWidth: Float,
            keyboardWidth: Int
        ) {
            mDefaultKeyWidth = keyAttr.getFraction(
                R.styleable.Keyboard_Key_keyWidth,
                keyboardWidth, keyboardWidth, defaultKeyWidth
            )
            mDefaultKeyLabelFlags = keyAttr.getInt(R.styleable.Keyboard_Key_keyLabelFlags, 0)
            mDefaultBackgroundType = keyAttr.getInt(
                R.styleable.Keyboard_Key_backgroundType,
                Key.BACKGROUND_TYPE_NORMAL
            )
        }

        /**
         * Parse and update key attributes using default attributes. This constructor is used
         * to parse include tag.
         *
         * @param keyAttr an attributes array of include tag.
         * @param defaultRowAttr default Row attributes.
         * @param keyboardWidth the keyboard width that is required to calculate keyWidth attribute.
         */
        constructor(
            keyAttr: TypedArray, defaultRowAttr: RowAttributes,
            keyboardWidth: Int
        ) {
            mDefaultKeyWidth = keyAttr.getFraction(
                R.styleable.Keyboard_Key_keyWidth,
                keyboardWidth, keyboardWidth, defaultRowAttr.mDefaultKeyWidth
            )
            mDefaultKeyLabelFlags = (keyAttr.getInt(R.styleable.Keyboard_Key_keyLabelFlags, 0)
                    or defaultRowAttr.mDefaultKeyLabelFlags)
            mDefaultBackgroundType = keyAttr.getInt(
                R.styleable.Keyboard_Key_backgroundType,
                defaultRowAttr.mDefaultBackgroundType
            )
        }
    }

    private val mCurrentY: Int

    // Will be updated by {@link Key}'s constructor.
    private var mCurrentX: Float

    init {
        mParams = params
        val keyboardAttr: TypedArray = res.obtainAttributes(
            Xml.asAttributeSet(parser),
            R.styleable.Keyboard
        )
        mRowHeight = ResourceUtils.getDimensionOrFraction(
            keyboardAttr,
            R.styleable.Keyboard_rowHeight, params.mBaseHeight, params.mDefaultRowHeight.toFloat()
        ).toInt()
        keyboardAttr.recycle()
        val keyAttr: TypedArray = res.obtainAttributes(
            Xml.asAttributeSet(parser),
            R.styleable.Keyboard_Key
        )
        mRowAttributesStack.push(
            RowAttributes(
                keyAttr, params.mDefaultKeyWidth.toFloat(), params.mBaseWidth
            )
        )
        keyAttr.recycle()

        mCurrentY = y
        mCurrentX = 0.0f
    }

    fun getRowHeight(): Int {
        return mRowHeight
    }

    fun pushRowAttributes(keyAttr: TypedArray) {
        val newAttributes: RowAttributes = RowAttributes(
            keyAttr, mRowAttributesStack.peek(), mParams.mBaseWidth
        )
        mRowAttributesStack.push(newAttributes)
    }

    fun popRowAttributes() {
        mRowAttributesStack.pop()
    }

    fun getDefaultKeyWidth(): Float {
        return mRowAttributesStack.peek().mDefaultKeyWidth
    }

    fun getDefaultKeyLabelFlags(): Int {
        return mRowAttributesStack.peek().mDefaultKeyLabelFlags
    }

    fun getDefaultBackgroundType(): Int {
        return mRowAttributesStack.peek().mDefaultBackgroundType
    }

    fun setXPos(keyXPos: Float) {
        mCurrentX = keyXPos
    }

    fun advanceXPos(width: Float) {
        mCurrentX += width
    }

    fun getKeyY(): Int {
        return mCurrentY
    }

    fun getKeyX(keyAttr: TypedArray?): Float {
        if (keyAttr == null || !keyAttr.hasValue(R.styleable.Keyboard_Key_keyXPos)) {
            return mCurrentX
        }
        val keyXPos: Float = keyAttr.getFraction(
            R.styleable.Keyboard_Key_keyXPos,
            mParams.mBaseWidth, mParams.mBaseWidth, 0f
        )
        if (keyXPos >= 0) {
            return keyXPos + mParams.mLeftPadding
        }
        // If keyXPos is negative, the actual x-coordinate will be
        // keyboardWidth + keyXPos.
        // keyXPos shouldn't be less than mCurrentX because drawable area for this
        // key starts at mCurrentX. Or, this key will overlaps the adjacent key on
        // its left hand side.
        val keyboardRightEdge: Int = mParams.mOccupiedWidth - mParams.mRightPadding
        return max((keyXPos + keyboardRightEdge).toDouble(), mCurrentX.toDouble()).toFloat()
    }

    fun getKeyWidth(keyAttr: TypedArray?, keyXPos: Float): Float {
        if (keyAttr == null) {
            return getDefaultKeyWidth()
        }
        val widthType: Int = ResourceUtils.getEnumValue(
            keyAttr,
            R.styleable.Keyboard_Key_keyWidth, KEYWIDTH_NOT_ENUM
        )
        when (widthType) {
            KEYWIDTH_FILL_RIGHT -> {
                // If keyWidth is fillRight, the actual key width will be determined to fill
                // out the area up to the right edge of the keyboard.
                val keyboardRightEdge: Int = mParams.mOccupiedWidth - mParams.mRightPadding
                return keyboardRightEdge - keyXPos
            }

            else -> return keyAttr.getFraction(
                R.styleable.Keyboard_Key_keyWidth,
                mParams.mBaseWidth, mParams.mBaseWidth, getDefaultKeyWidth()
            )
        }
    }

    companion object {
        // keyWidth enum constants
        private const val KEYWIDTH_NOT_ENUM: Int = 0
        private val KEYWIDTH_FILL_RIGHT: Int = -1
    }
}
