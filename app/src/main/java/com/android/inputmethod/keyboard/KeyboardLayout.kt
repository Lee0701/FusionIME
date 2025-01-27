/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.inputmethod.keyboard

import com.android.inputmethod.annotations.UsedForTesting
import javax.annotation.Nonnull

/**
 * KeyboardLayout maintains the keyboard layout information.
 */
class KeyboardLayout(
    layoutKeys: ArrayList<Key>, mostCommonKeyWidth: Int,
    mostCommonKeyHeight: Int, keyboardWidth: Int, keyboardHeight: Int
) {
    private val mKeyCodes: IntArray

    private val mKeyXCoordinates: IntArray
    private val mKeyYCoordinates: IntArray

    private val mKeyWidths: IntArray
    private val mKeyHeights: IntArray

    val mMostCommonKeyWidth: Int
    val mMostCommonKeyHeight: Int

    val mKeyboardWidth: Int
    val mKeyboardHeight: Int

    init {
        mMostCommonKeyWidth = mostCommonKeyWidth
        mMostCommonKeyHeight = mostCommonKeyHeight
        mKeyboardWidth = keyboardWidth
        mKeyboardHeight = keyboardHeight

        mKeyCodes = IntArray(layoutKeys.size)
        mKeyXCoordinates = IntArray(layoutKeys.size)
        mKeyYCoordinates = IntArray(layoutKeys.size)
        mKeyWidths = IntArray(layoutKeys.size)
        mKeyHeights = IntArray(layoutKeys.size)

        for (i in layoutKeys.indices) {
            val key: Key = layoutKeys.get(i)
            mKeyCodes.get(i) = key.getCode().lowercaseChar()
            mKeyXCoordinates.get(i) = key.getX()
            mKeyYCoordinates.get(i) = key.getY()
            mKeyWidths.get(i) = key.getWidth()
            mKeyHeights.get(i) = key.getHeight()
        }
    }

    @UsedForTesting
    fun getKeyCodes(): IntArray {
        return mKeyCodes
    }

    /**
     * The x-coordinate for the top-left corner of the keys.
     *
     */
    fun getKeyXCoordinates(): IntArray {
        return mKeyXCoordinates
    }

    /**
     * The y-coordinate for the top-left corner of the keys.
     */
    fun getKeyYCoordinates(): IntArray {
        return mKeyYCoordinates
    }

    /**
     * The widths of the keys which are smaller than the true hit-area due to the gaps
     * between keys. The mostCommonKey(Width/Height) represents the true key width/height
     * including the gaps.
     */
    fun getKeyWidths(): IntArray {
        return mKeyWidths
    }

    /**
     * The heights of the keys which are smaller than the true hit-area due to the gaps
     * between keys. The mostCommonKey(Width/Height) represents the true key width/height
     * including the gaps.
     */
    fun getKeyHeights(): IntArray {
        return mKeyHeights
    }

    companion object {
        /**
         * Factory method to create [KeyboardLayout] objects.
         */
        fun newKeyboardLayout(
            @Nonnull sortedKeys: List<Key>,
            mostCommonKeyWidth: Int, mostCommonKeyHeight: Int,
            occupiedWidth: Int, occupiedHeight: Int
        ): KeyboardLayout {
            val layoutKeys: ArrayList<Key> = ArrayList()
            for (key: Key in sortedKeys) {
                if (!ProximityInfo.Companion.needsProximityInfo(key)) {
                    continue
                }
                if (key.getCode() != ','.code) {
                    layoutKeys.add(key)
                }
            }
            return KeyboardLayout(
                layoutKeys, mostCommonKeyWidth,
                mostCommonKeyHeight, occupiedWidth, occupiedHeight
            )
        }
    }
}
