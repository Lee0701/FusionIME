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
package com.android.inputmethod.latin.common

import com.android.inputmethod.annotations.UsedForTesting
import java.util.Arrays

// TODO: This class is not thread-safe.
class ResizableIntArray(capacity: Int) {
    var primitiveArray: IntArray = intArrayOf()
        private set
    private var mLength = 0

    init {
        reset(capacity)
    }

    operator fun get(index: Int): Int {
        if (index < mLength) {
            return primitiveArray[index]
        }
        throw ArrayIndexOutOfBoundsException("length=$mLength; index=$index")
    }

    fun addAt(index: Int, `val`: Int) {
        if (index < mLength) {
            primitiveArray[index] = `val`
        } else {
            mLength = index
            add(`val`)
        }
    }

    fun add(`val`: Int) {
        val currentLength = mLength
        ensureCapacity(currentLength + 1)
        primitiveArray[currentLength] = `val`
        mLength = currentLength + 1
    }

    /**
     * Calculate the new capacity of `mArray`.
     * @param minimumCapacity the minimum capacity that the `mArray` should have.
     * @return the new capacity that the `mArray` should have. Returns zero when there is no
     * need to expand `mArray`.
     */
    private fun calculateCapacity(minimumCapacity: Int): Int {
        val currentCapcity = primitiveArray.size
        if (currentCapcity < minimumCapacity) {
            val nextCapacity = currentCapcity * 2
            // The following is the same as return Math.max(minimumCapacity, nextCapacity);
            return if (minimumCapacity > nextCapacity) minimumCapacity else nextCapacity
        }
        return 0
    }

    private fun ensureCapacity(minimumCapacity: Int) {
        val newCapacity = calculateCapacity(minimumCapacity)
        if (newCapacity > 0) {
            // TODO: Implement primitive array pool.
            primitiveArray = primitiveArray.copyOf(newCapacity)
        }
    }

    var length: Int
        get() = mLength
        set(newLength) {
            ensureCapacity(newLength)
            mLength = newLength
        }

    fun reset(capacity: Int) {
        // TODO: Implement primitive array pool.
        primitiveArray = IntArray(capacity)
        mLength = 0
    }

    fun set(ip: ResizableIntArray) {
        // TODO: Implement primitive array pool.
        primitiveArray = ip.primitiveArray
        mLength = ip.mLength
    }

    fun copy(ip: ResizableIntArray) {
        val newCapacity = calculateCapacity(ip.mLength)
        if (newCapacity > 0) {
            // TODO: Implement primitive array pool.
            primitiveArray = IntArray(newCapacity)
        }
        System.arraycopy(ip.primitiveArray, 0, primitiveArray, 0, ip.mLength)
        mLength = ip.mLength
    }

    fun append(src: ResizableIntArray, startPos: Int, length: Int) {
        if (length == 0) {
            return
        }
        val currentLength = mLength
        val newLength = currentLength + length
        ensureCapacity(newLength)
        System.arraycopy(
            src.primitiveArray, startPos,
            primitiveArray, currentLength, length
        )
        mLength = newLength
    }

    fun fill(value: Int, startPos: Int, length: Int) {
        require(!(startPos < 0 || length < 0)) { "startPos=$startPos; length=$length" }
        val endPos = startPos + length
        ensureCapacity(endPos)
        Arrays.fill(primitiveArray, startPos, endPos, value)
        if (mLength < endPos) {
            mLength = endPos
        }
    }

    /**
     * Shift to the left by elementCount, discarding elementCount pointers at the start.
     * @param elementCount how many elements to shift.
     */
    @UsedForTesting
    fun shift(elementCount: Int) {
        System.arraycopy(
            primitiveArray, elementCount,
            primitiveArray, 0, mLength - elementCount
        )
        mLength -= elementCount
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (i in 0 until mLength) {
            if (i != 0) {
                sb.append(",")
            }
            sb.append(primitiveArray[i])
        }
        return "[$sb]"
    }
}
