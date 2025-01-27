/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.util.Log
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.keyboard.internal.MatrixUtils.MatrixOperationFailedException
import java.util.Arrays
import kotlin.math.abs

/**
 * Utilities for matrix operations. Don't instantiate objects inside this class to prevent
 * unexpected performance regressions.
 */
@UsedForTesting
object MatrixUtils {
    val TAG: String = MatrixUtils::class.java.getSimpleName()

    /**
     * A utility function to inverse matrix.
     * Find a pivot and swap the row of squareMatrix0 and squareMatrix1
     */
    private fun findPivotAndSwapRow(
        row: Int, squareMatrix0: Array<FloatArray>,
        squareMatrix1: Array<FloatArray>, size: Int
    ) {
        var ip: Int = row
        var pivot: Float = abs(squareMatrix0.get(row).get(row).toDouble()).toFloat()
        for (i in row + 1 until size) {
            if (pivot < abs(squareMatrix0.get(i).get(row).toDouble())) {
                ip = i
                pivot = abs(squareMatrix0.get(i).get(row).toDouble()).toFloat()
            }
        }
        if (ip != row) {
            for (j in 0 until size) {
                val temp0: Float = squareMatrix0.get(ip).get(j)
                squareMatrix0.get(ip).get(j) = squareMatrix0.get(row).get(j)
                squareMatrix0.get(row).get(j) = temp0
                val temp1: Float = squareMatrix1.get(ip).get(j)
                squareMatrix1.get(ip).get(j) = squareMatrix1.get(row).get(j)
                squareMatrix1.get(row).get(j) = temp1
            }
        }
    }

    /**
     * A utility function to inverse matrix. This function calculates answer for each row by
     * sweeping method of Gauss Jordan elimination
     */
    @Throws(MatrixOperationFailedException::class)
    private fun sweep(
        row: Int, squareMatrix0: Array<FloatArray>,
        squareMatrix1: Array<FloatArray>, size: Int
    ) {
        val pivot: Float = squareMatrix0.get(row).get(row)
        if (pivot == 0f) {
            throw MatrixOperationFailedException("Inverse failed. Invalid pivot")
        }
        for (j in 0 until size) {
            squareMatrix0.get(row).get(j) /= pivot
            squareMatrix1.get(row).get(j) /= pivot
        }
        for (i in 0 until size) {
            val sweepTargetValue: Float = squareMatrix0.get(i).get(row)
            if (i != row) {
                for (j in row until size) {
                    squareMatrix0.get(i).get(j) -= sweepTargetValue * squareMatrix0.get(row).get(j)
                }
                for (j in 0 until size) {
                    squareMatrix1.get(i).get(j) -= sweepTargetValue * squareMatrix1.get(row).get(j)
                }
            }
        }
    }

    /**
     * A function to inverse matrix.
     * The inverse matrix of squareMatrix will be output to inverseMatrix. Please notice that
     * the value of squareMatrix is modified in this function and can't be resuable.
     */
    @UsedForTesting
    @Throws(MatrixOperationFailedException::class)
    fun inverse(
        squareMatrix: Array<FloatArray>,
        inverseMatrix: Array<FloatArray>
    ) {
        val size: Int = squareMatrix.size
        if (squareMatrix.get(0).size != size || inverseMatrix.size != size || inverseMatrix.get(0).size != size) {
            throw MatrixOperationFailedException(
                "--- invalid length. column should be 2 times larger than row."
            )
        }
        for (i in 0 until size) {
            Arrays.fill(inverseMatrix.get(i), 0.0f)
            inverseMatrix.get(i).get(i) = 1.0f
        }
        for (i in 0 until size) {
            findPivotAndSwapRow(i, squareMatrix, inverseMatrix, size)
            sweep(i, squareMatrix, inverseMatrix, size)
        }
    }

    /**
     * A matrix operation to multiply m0 and m1.
     */
    @UsedForTesting
    @Throws(MatrixOperationFailedException::class)
    fun multiply(
        m0: Array<FloatArray>, m1: Array<FloatArray>,
        retval: Array<FloatArray>
    ) {
        if (m0.get(0).size != m1.size) {
            throw MatrixOperationFailedException(
                "--- invalid length for multiply " + m0.get(0).size + ", " + m1.size
            )
        }
        val m0h: Int = m0.size
        val m0w: Int = m0.get(0).size
        val m1w: Int = m1.get(0).size
        if (retval.size != m0h || retval.get(0).size != m1w) {
            throw MatrixOperationFailedException(
                "--- invalid length of retval " + retval.size + ", " + retval.get(0).size
            )
        }

        for (i in 0 until m0h) {
            Arrays.fill(retval.get(i), 0f)
            for (j in 0 until m1w) {
                for (k in 0 until m0w) {
                    retval.get(i).get(j) += m0.get(i).get(k) * m1.get(k).get(j)
                }
            }
        }
    }

    /**
     * A utility function to dump the specified matrix in a readable way
     */
    @UsedForTesting
    fun dump(title: String, a: Array<FloatArray>) {
        val column: Int = a.get(0).size
        val row: Int = a.size
        Log.d(TAG, "Dump matrix: " + title)
        Log.d(TAG, "/*---------------------")
        val sb: StringBuilder = StringBuilder()
        for (i in 0 until row) {
            sb.setLength(0)
            for (j in 0 until column) {
                sb.append(String.format("%4f", a.get(i).get(j))).append(' ')
            }
            Log.d(TAG, sb.toString())
        }
        Log.d(TAG, "---------------------*/")
    }

    class MatrixOperationFailedException(msg: String) : Exception(msg) {
        init {
            Log.d(TAG, msg)
        }

        companion object {
            private const val serialVersionUID: Long = 4384485606788583829L
        }
    }
}
