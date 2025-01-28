/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.util.SparseArray
import com.android.inputmethod.keyboard.internal.KeyVisualAttributes
import com.android.inputmethod.keyboard.internal.KeyboardIconsSet
import com.android.inputmethod.keyboard.internal.KeyboardParams
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.CoordinateUtils
import java.util.Collections
import kotlin.math.max
import kotlin.math.min

/**
 * Loads an XML description of a keyboard and stores the attributes of the keys. A keyboard
 * consists of rows of keys.
 *
 * The layout file for a keyboard contains XML that looks like the following snippet:
 * <pre>
 * &lt;Keyboard
 * latin:keyWidth="10%p"
 * latin:rowHeight="50px"
 * latin:horizontalGap="2%p"
 * latin:verticalGap="2%p" &gt;
 * &lt;Row latin:keyWidth="10%p" &gt;
 * &lt;Key latin:keyLabel="A" /&gt;
 * ...
 * &lt;/Row&gt;
 * ...
 * &lt;/Keyboard&gt;
</pre> *
 */
open class Keyboard {
    val mId: KeyboardId
    val mThemeId: Int

    /** Total height of the keyboard, including the padding and keys  */
    val mOccupiedHeight: Int

    /** Total width of the keyboard, including the padding and keys  */
    val mOccupiedWidth: Int

    /** Base height of the keyboard, used to calculate rows' height  */
    val mBaseHeight: Int

    /** Base width of the keyboard, used to calculate keys' width  */
    val mBaseWidth: Int

    /** The padding above the keyboard  */
    val mTopPadding: Int

    /** Default gap between rows  */
    val mVerticalGap: Int

    /** Per keyboard key visual parameters  */
    val mKeyVisualAttributes: KeyVisualAttributes?

    val mMostCommonKeyHeight: Int
    val mMostCommonKeyWidth: Int

    /** More keys keyboard template  */
    val mMoreKeysTemplate: Int

    /** Maximum column for more keys keyboard  */
    val mMaxMoreKeysKeyboardColumn: Int

    /**
     * Return the sorted list of keys of this keyboard.
     * The keys are sorted from top-left to bottom-right order.
     * The list may contain [Key.Spacer] object as well.
     * @return the sorted unmodifiable list of [Key]s of this keyboard.
     */
    /** List of keys in this keyboard  */
    open val sortedKeys: List<Key>

    val mShiftKeys: List<Key>

    val mAltCodeKeysWhileTyping: List<Key>

    val mIconsSet: KeyboardIconsSet

    private val mKeyCache: SparseArray<Key?> = SparseArray()

    val proximityInfo: ProximityInfo

    val keyboardLayout: KeyboardLayout

    private val mProximityCharsCorrectionEnabled: Boolean

    constructor(params: KeyboardParams) {
        mId = params.mId!!
        mThemeId = params.mThemeId
        mOccupiedHeight = params.mOccupiedHeight
        mOccupiedWidth = params.mOccupiedWidth
        mBaseHeight = params.mBaseHeight
        mBaseWidth = params.mBaseWidth
        mMostCommonKeyHeight = params.mMostCommonKeyHeight
        mMostCommonKeyWidth = params.mMostCommonKeyWidth
        mMoreKeysTemplate = params.mMoreKeysTemplate
        mMaxMoreKeysKeyboardColumn = params.mMaxMoreKeysKeyboardColumn
        mKeyVisualAttributes = params.mKeyVisualAttributes
        mTopPadding = params.mTopPadding
        mVerticalGap = params.mVerticalGap

        sortedKeys = Collections.unmodifiableList(ArrayList(params.mSortedKeys))
        mShiftKeys = Collections.unmodifiableList(params.mShiftKeys)
        mAltCodeKeysWhileTyping = Collections.unmodifiableList(params.mAltCodeKeysWhileTyping)
        mIconsSet = params.mIconsSet

        proximityInfo = ProximityInfo(
            params.GRID_WIDTH, params.GRID_HEIGHT,
            mOccupiedWidth, mOccupiedHeight, mMostCommonKeyWidth, mMostCommonKeyHeight,
            sortedKeys, params.mTouchPositionCorrection
        )
        mProximityCharsCorrectionEnabled = params.mProximityCharsCorrectionEnabled
        keyboardLayout = KeyboardLayout.newKeyboardLayout(
            sortedKeys, mMostCommonKeyWidth,
            mMostCommonKeyHeight, mOccupiedWidth, mOccupiedHeight
        )
    }

    protected constructor(keyboard: Keyboard) {
        mId = keyboard.mId
        mThemeId = keyboard.mThemeId
        mOccupiedHeight = keyboard.mOccupiedHeight
        mOccupiedWidth = keyboard.mOccupiedWidth
        mBaseHeight = keyboard.mBaseHeight
        mBaseWidth = keyboard.mBaseWidth
        mMostCommonKeyHeight = keyboard.mMostCommonKeyHeight
        mMostCommonKeyWidth = keyboard.mMostCommonKeyWidth
        mMoreKeysTemplate = keyboard.mMoreKeysTemplate
        mMaxMoreKeysKeyboardColumn = keyboard.mMaxMoreKeysKeyboardColumn
        mKeyVisualAttributes = keyboard.mKeyVisualAttributes
        mTopPadding = keyboard.mTopPadding
        mVerticalGap = keyboard.mVerticalGap

        sortedKeys = keyboard.sortedKeys
        mShiftKeys = keyboard.mShiftKeys
        mAltCodeKeysWhileTyping = keyboard.mAltCodeKeysWhileTyping
        mIconsSet = keyboard.mIconsSet

        proximityInfo = keyboard.proximityInfo
        mProximityCharsCorrectionEnabled = keyboard.mProximityCharsCorrectionEnabled
        keyboardLayout = keyboard.keyboardLayout
    }

    fun hasProximityCharsCorrection(code: Int): Boolean {
        if (!mProximityCharsCorrectionEnabled) {
            return false
        }
        // Note: The native code has the main keyboard layout only at this moment.
        // TODO: Figure out how to handle proximity characters information of all layouts.
        val canAssumeNativeHasProximityCharsInfoOfAllKeys: Boolean =
            (mId!!.mElementId == KeyboardId.ELEMENT_ALPHABET
                    || mId.mElementId == KeyboardId.ELEMENT_ALPHABET_AUTOMATIC_SHIFTED)
        return canAssumeNativeHasProximityCharsInfoOfAllKeys || Character.isLetter(code)
    }

    fun getKey(code: Int): Key? {
        if (code == Constants.CODE_UNSPECIFIED) {
            return null
        }
        synchronized(mKeyCache) {
            val index: Int = mKeyCache.indexOfKey(code)
            if (index >= 0) {
                return mKeyCache.valueAt(index)
            }

            for (key: Key in sortedKeys) {
                if (key.code == code) {
                    mKeyCache.put(code, key)
                    return key
                }
            }
            mKeyCache.put(code, null)
            return null
        }
    }

    fun hasKey(aKey: Key): Boolean {
        if (mKeyCache.indexOfValue(aKey) >= 0) {
            return true
        }

        for (key: Key? in sortedKeys) {
            if (key === aKey) {
                mKeyCache.put(key.code, key)
                return true
            }
        }
        return false
    }

    override fun toString(): String {
        return mId.toString()
    }

    /**
     * Returns the array of the keys that are closest to the given point.
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return the list of the nearest keys to the given point. If the given
     * point is out of range, then an array of size zero is returned.
     */
    open fun getNearestKeys(x: Int, y: Int): List<Key?> {
        // Avoid dead pixels at edges of the keyboard
        val adjustedX: Int =
            max(0.0, min(x.toDouble(), (mOccupiedWidth - 1).toDouble())).toInt()
        val adjustedY: Int =
            max(0.0, min(y.toDouble(), (mOccupiedHeight - 1).toDouble())).toInt()
        return proximityInfo.getNearestKeys(adjustedX, adjustedY)
    }

    fun getCoordinates(codePoints: IntArray): IntArray {
        val length: Int = codePoints.size
        val coordinates: IntArray = CoordinateUtils.newCoordinateArray(length)
        for (i in 0 until length) {
            val key: Key? = getKey(codePoints.get(i))
            if (null != key) {
                CoordinateUtils.setXYInArray(
                    coordinates, i,
                    key.x + key.width / 2, key.y + key.height / 2
                )
            } else {
                CoordinateUtils.setXYInArray(
                    coordinates, i,
                    Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE
                )
            }
        }
        return coordinates
    }
}
