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

import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.latin.define.DebugFlags

class TouchPositionCorrection {
    private var mEnabled: Boolean = false
    private var mXs: FloatArray?
    private var mYs: FloatArray?
    private var mRadii: FloatArray?

    fun load(data: Array<String>) {
        val dataLength: Int = data.size
        if (dataLength % TOUCH_POSITION_CORRECTION_RECORD_SIZE != 0) {
            if (DebugFlags.DEBUG_ENABLED) {
                throw RuntimeException(
                    "the size of touch position correction data is invalid"
                )
            }
            return
        }

        val length: Int = dataLength / TOUCH_POSITION_CORRECTION_RECORD_SIZE
        mXs = FloatArray(length)
        mYs = FloatArray(length)
        mRadii = FloatArray(length)
        try {
            for (i in 0 until dataLength) {
                val type: Int = i % TOUCH_POSITION_CORRECTION_RECORD_SIZE
                val index: Int = i / TOUCH_POSITION_CORRECTION_RECORD_SIZE
                val value: Float = data.get(i).toFloat()
                if (type == 0) {
                    mXs!!.get(index) = value
                } else if (type == 1) {
                    mYs!!.get(index) = value
                } else {
                    mRadii!!.get(index) = value
                }
            }
            mEnabled = dataLength > 0
        } catch (e: NumberFormatException) {
            if (DebugFlags.DEBUG_ENABLED) {
                throw RuntimeException(
                    "the number format for touch position correction data is invalid"
                )
            }
            mEnabled = false
            mXs = null
            mYs = null
            mRadii = null
        }
    }

    @UsedForTesting
    fun setEnabled(enabled: Boolean) {
        mEnabled = enabled
    }

    fun isValid(): Boolean {
        return mEnabled
    }

    fun getRows(): Int {
        return mRadii!!.size
    }

    @Suppress("unused")
    fun getX(row: Int): Float {
        return 0.0f
        // Touch position correction data for X coordinate is obsolete.
        // return mXs[row];
    }

    fun getY(row: Int): Float {
        return mYs!!.get(row)
    }

    fun getRadius(row: Int): Float {
        return mRadii!!.get(row)
    }

    companion object {
        private const val TOUCH_POSITION_CORRECTION_RECORD_SIZE: Int = 3
    }
}
