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

/**
 * Interpolates XY-coordinates using Cubic Hermite Curve.
 */
class HermiteInterpolator {
    private var mXCoords: IntArray
    private var mYCoords: IntArray
    private var mMinPos: Int = 0
    private var mMaxPos: Int = 0

    // Working variable to calculate interpolated value.
    /** The coordinates of the start point of the interval.  */
    var mP1X: Int = 0
    var mP1Y: Int = 0

    /** The coordinates of the end point of the interval.  */
    var mP2X: Int = 0
    var mP2Y: Int = 0

    /** The slope of the tangent at the start point.  */
    var mSlope1X: Float = 0f
    var mSlope1Y: Float = 0f

    /** The slope of the tangent at the end point.  */
    var mSlope2X: Float = 0f
    var mSlope2Y: Float = 0f

    /** The interpolated coordinates.
     * The return variables of [.interpolate] to avoid instantiations.
     */
    var mInterpolatedX: Float = 0f
    var mInterpolatedY: Float = 0f

    /**
     * Reset this interpolator to point XY-coordinates data.
     * @param xCoords the array of x-coordinates. Valid data are in left-open interval
     * `[minPos, maxPos)`.
     * @param yCoords the array of y-coordinates. Valid data are in left-open interval
     * `[minPos, maxPos)`.
     * @param minPos the minimum index of left-open interval of valid data.
     * @param maxPos the maximum index of left-open interval of valid data.
     */
    fun reset(
        xCoords: IntArray, yCoords: IntArray, minPos: Int,
        maxPos: Int
    ) {
        mXCoords = xCoords
        mYCoords = yCoords
        mMinPos = minPos
        mMaxPos = maxPos
    }

    /**
     * Set interpolation interval.
     *
     *
     * The start and end coordinates of the interval will be set in [.mP1X], [.mP1Y],
     * [.mP2X], and [.mP2Y]. The slope of the tangents at start and end points will be
     * set in [.mSlope1X], [.mSlope1Y], [.mSlope2X], and [.mSlope2Y].
     *
     * @param p0 the index just before interpolation interval. If `p1` points the start
     * of valid points, `p0` must be less than `minPos` of
     * [.reset].
     * @param p1 the start index of interpolation interval.
     * @param p2 the end index of interpolation interval.
     * @param p3 the index just after interpolation interval. If `p2` points the end of
     * valid points, `p3` must be equal or greater than `maxPos` of
     * [.reset].
     */
    fun setInterval(p0: Int, p1: Int, p2: Int, p3: Int) {
        mP1X = mXCoords.get(p1)
        mP1Y = mYCoords.get(p1)
        mP2X = mXCoords.get(p2)
        mP2Y = mYCoords.get(p2)
        // A(ax,ay) is the vector p1->p2.
        val ax: Int = mP2X - mP1X
        val ay: Int = mP2Y - mP1Y

        // Calculate the slope of the tangent at p1.
        if (p0 >= mMinPos) {
            // p1 has previous valid point p0.
            // The slope of the tangent is half of the vector p0->p2.
            mSlope1X = (mP2X - mXCoords.get(p0)) / 2.0f
            mSlope1Y = (mP2Y - mYCoords.get(p0)) / 2.0f
        } else if (p3 < mMaxPos) {
            // p1 has no previous valid point, but p2 has next valid point p3.
            // B(bx,by) is the slope vector of the tangent at p2.
            val bx: Float = (mXCoords.get(p3) - mP1X) / 2.0f
            val by: Float = (mYCoords.get(p3) - mP1Y) / 2.0f
            val crossProdAB: Float = ax * by - ay * bx
            val dotProdAB: Float = ax * bx + ay * by
            val normASquare: Float = (ax * ax + ay * ay).toFloat()
            val invHalfNormASquare: Float = 1.0f / normASquare / 2.0f
            // The slope of the tangent is the mirror image of vector B to vector A.
            mSlope1X = invHalfNormASquare * (dotProdAB * ax + crossProdAB * ay)
            mSlope1Y = invHalfNormASquare * (dotProdAB * ay - crossProdAB * ax)
        } else {
            // p1 and p2 have no previous valid point. (Interval has only point p1 and p2)
            mSlope1X = ax.toFloat()
            mSlope1Y = ay.toFloat()
        }

        // Calculate the slope of the tangent at p2.
        if (p3 < mMaxPos) {
            // p2 has next valid point p3.
            // The slope of the tangent is half of the vector p1->p3.
            mSlope2X = (mXCoords.get(p3) - mP1X) / 2.0f
            mSlope2Y = (mYCoords.get(p3) - mP1Y) / 2.0f
        } else if (p0 >= mMinPos) {
            // p2 has no next valid point, but p1 has previous valid point p0.
            // B(bx,by) is the slope vector of the tangent at p1.
            val bx: Float = (mP2X - mXCoords.get(p0)) / 2.0f
            val by: Float = (mP2Y - mYCoords.get(p0)) / 2.0f
            val crossProdAB: Float = ax * by - ay * bx
            val dotProdAB: Float = ax * bx + ay * by
            val normASquare: Float = (ax * ax + ay * ay).toFloat()
            val invHalfNormASquare: Float = 1.0f / normASquare / 2.0f
            // The slope of the tangent is the mirror image of vector B to vector A.
            mSlope2X = invHalfNormASquare * (dotProdAB * ax + crossProdAB * ay)
            mSlope2Y = invHalfNormASquare * (dotProdAB * ay - crossProdAB * ax)
        } else {
            // p1 and p2 has no previous valid point. (Interval has only point p1 and p2)
            mSlope2X = ax.toFloat()
            mSlope2Y = ay.toFloat()
        }
    }

    /**
     * Calculate interpolation value at `t` in unit interval `[0,1]`.
     *
     *
     * On the unit interval [0,1], given a starting point p1 at t=0 and an ending point p2 at t=1
     * with the slope of the tangent m1 at p1 and m2 at p2, the polynomial of cubic Hermite curve
     * can be defined by
     * p(t) = (1+2t)(1-t)(1-t)*p1 + t(1-t)(1-t)*m1 + (3-2t)t^2*p2 + (t-1)t^2*m2
     * where t is an element of [0,1].
     *
     *
     * The interpolated XY-coordinates will be set in [.mInterpolatedX] and
     * [.mInterpolatedY].
     *
     * @param t the interpolation parameter. The value must be in close interval `[0,1]`.
     */
    fun interpolate(t: Float) {
        val omt: Float = 1.0f - t
        val tm2: Float = 2.0f * t
        val k1: Float = 1.0f + tm2
        val k2: Float = 3.0f - tm2
        val omt2: Float = omt * omt
        val t2: Float = t * t
        mInterpolatedX = (k1 * mP1X + t * mSlope1X) * omt2 + (k2 * mP2X - omt * mSlope2X) * t2
        mInterpolatedY = (k1 * mP1Y + t * mSlope1Y) * omt2 + (k2 * mP2Y - omt * mSlope2Y) * t2
    }
}
