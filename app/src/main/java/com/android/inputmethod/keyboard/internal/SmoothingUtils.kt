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
import kotlin.math.pow

/**
 * Utilities to smooth coordinates. Currently, we calculate 3d least squares formula by using
 * Lagrangian smoothing
 */
@UsedForTesting
object SmoothingUtils {
    private val TAG: String = SmoothingUtils::class.java.getSimpleName()
    private const val DEBUG: Boolean = false

    /**
     * Find a most likely 3d least squares formula for specified coordinates.
     * "retval" should be a 1x4 size matrix.
     */
    @UsedForTesting
    @Throws(MatrixOperationFailedException::class)
    fun get3DParameters(
        xs: FloatArray, ys: FloatArray,
        retval: Array<FloatArray>
    ) {
        val COEFF_COUNT: Int = 4 // Coefficient count for 3d smoothing
        if (retval.size != COEFF_COUNT || retval.get(0).size != 1) {
            Log.d(
                TAG, ("--- invalid length of 3d retval " + retval.size + ", "
                        + retval.get(0).size)
            )
            return
        }
        val N: Int = xs.size
        // TODO: Never isntantiate the matrix
        val m0: Array<FloatArray> = Array(COEFF_COUNT) { FloatArray(COEFF_COUNT) }
        val m0Inv: Array<FloatArray> = Array(COEFF_COUNT) { FloatArray(COEFF_COUNT) }
        val m1: Array<FloatArray> = Array(COEFF_COUNT) { FloatArray(N) }
        val m2: Array<FloatArray> = Array(N) { FloatArray(1) }

        // m0
        for (i in 0 until COEFF_COUNT) {
            Arrays.fill(m0.get(i), 0f)
            for (j in 0 until COEFF_COUNT) {
                val pow: Int = i + j
                for (k in 0 until N) {
                    m0.get(i).get(j) += xs.get(k).pow(pow.toDouble()) as Float
                }
            }
        }
        // m0Inv
        MatrixUtils.inverse(m0, m0Inv)
        if (DEBUG) {
            MatrixUtils.dump("m0-1", m0Inv)
        }

        // m1
        for (i in 0 until COEFF_COUNT) {
            for (j in 0 until N) {
                m1.get(i).get(j) = if ((i == 0)) 1.0f else m1.get(i - 1).get(j) * xs.get(j)
            }
        }

        // m2
        for (i in 0 until N) {
            m2.get(i).get(0) = ys.get(i)
        }

        val m0Invxm1: Array<FloatArray> = Array(COEFF_COUNT) { FloatArray(N) }
        if (DEBUG) {
            MatrixUtils.dump("a0", m0Inv)
            MatrixUtils.dump("a1", m1)
        }
        MatrixUtils.multiply(m0Inv, m1, m0Invxm1)
        if (DEBUG) {
            MatrixUtils.dump("a2", m0Invxm1)
            MatrixUtils.dump("a3", m2)
        }
        MatrixUtils.multiply(m0Invxm1, m2, retval)
        if (DEBUG) {
            MatrixUtils.dump("result", retval)
        }
    }
}
