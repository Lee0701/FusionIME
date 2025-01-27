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

import com.android.inputmethod.latin.common.ResizableIntArray
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * This class holds drawing points to represent a gesture stroke on the screen.
 */
class GestureStrokeDrawingPoints(drawingParams: GestureStrokeDrawingParams?) {
    private val mPreviewEventTimes: ResizableIntArray = ResizableIntArray(PREVIEW_CAPACITY)
    private val mPreviewXCoordinates: ResizableIntArray = ResizableIntArray(PREVIEW_CAPACITY)
    private val mPreviewYCoordinates: ResizableIntArray = ResizableIntArray(PREVIEW_CAPACITY)

    private val mDrawingParams: GestureStrokeDrawingParams?

    private var mStrokeId: Int = 0
    private var mLastPreviewSize: Int = 0
    private val mInterpolator: HermiteInterpolator = HermiteInterpolator()
    private var mLastInterpolatedPreviewIndex: Int = 0

    private var mLastX: Int = 0
    private var mLastY: Int = 0
    private var mDistanceFromLastSample: Double = 0.0

    private fun reset() {
        mStrokeId++
        mLastPreviewSize = 0
        mLastInterpolatedPreviewIndex = 0
        mPreviewEventTimes.setLength(0)
        mPreviewXCoordinates.setLength(0)
        mPreviewYCoordinates.setLength(0)
    }

    fun getGestureStrokeId(): Int {
        return mStrokeId
    }

    fun onDownEvent(x: Int, y: Int, elapsedTimeSinceFirstDown: Int) {
        reset()
        onMoveEvent(x, y, elapsedTimeSinceFirstDown)
    }

    private fun needsSampling(x: Int, y: Int): Boolean {
        mDistanceFromLastSample += hypot((x - mLastX).toDouble(), (y - mLastY).toDouble())
        mLastX = x
        mLastY = y
        val isDownEvent: Boolean = (mPreviewEventTimes.getLength() == 0)
        if (mDistanceFromLastSample >= mDrawingParams!!.mMinSamplingDistance || isDownEvent) {
            mDistanceFromLastSample = 0.0
            return true
        }
        return false
    }

    fun onMoveEvent(x: Int, y: Int, elapsedTimeSinceFirstDown: Int) {
        if (needsSampling(x, y)) {
            mPreviewEventTimes.add(elapsedTimeSinceFirstDown)
            mPreviewXCoordinates.add(x)
            mPreviewYCoordinates.add(y)
        }
    }

    /**
     * Append sampled preview points.
     *
     * @param eventTimes the event time array of gesture trail to be drawn.
     * @param xCoords the x-coordinates array of gesture trail to be drawn.
     * @param yCoords the y-coordinates array of gesture trail to be drawn.
     * @param types the point types array of gesture trail. This is valid only when
     * [GestureTrailDrawingPoints.DEBUG_SHOW_POINTS] is true.
     */
    fun appendPreviewStroke(
        eventTimes: ResizableIntArray,
        xCoords: ResizableIntArray, yCoords: ResizableIntArray,
        types: ResizableIntArray
    ) {
        val length: Int = mPreviewEventTimes.getLength() - mLastPreviewSize
        if (length <= 0) {
            return
        }
        eventTimes.append(mPreviewEventTimes, mLastPreviewSize, length)
        xCoords.append(mPreviewXCoordinates, mLastPreviewSize, length)
        yCoords.append(mPreviewYCoordinates, mLastPreviewSize, length)
        if (GestureTrailDrawingPoints.Companion.DEBUG_SHOW_POINTS) {
            types.fill(
                GestureTrailDrawingPoints.Companion.POINT_TYPE_SAMPLED,
                types.getLength(),
                length
            )
        }
        mLastPreviewSize = mPreviewEventTimes.getLength()
    }

    /**
     * Calculate interpolated points between the last interpolated point and the end of the trail.
     * And return the start index of the last interpolated segment of input arrays because it
     * may need to recalculate the interpolated points in the segment if further segments are
     * added to this stroke.
     *
     * @param lastInterpolatedIndex the start index of the last interpolated segment of
     * `eventTimes`, `xCoords`, and `yCoords`.
     * @param eventTimes the event time array of gesture trail to be drawn.
     * @param xCoords the x-coordinates array of gesture trail to be drawn.
     * @param yCoords the y-coordinates array of gesture trail to be drawn.
     * @param types the point types array of gesture trail. This is valid only when
     * [GestureTrailDrawingPoints.DEBUG_SHOW_POINTS] is true.
     * @return the start index of the last interpolated segment of input arrays.
     */
    fun interpolateStrokeAndReturnStartIndexOfLastSegment(
        lastInterpolatedIndex: Int,
        eventTimes: ResizableIntArray, xCoords: ResizableIntArray,
        yCoords: ResizableIntArray, types: ResizableIntArray
    ): Int {
        val size: Int = mPreviewEventTimes.getLength()
        val pt: IntArray = mPreviewEventTimes.getPrimitiveArray()
        val px: IntArray = mPreviewXCoordinates.getPrimitiveArray()
        val py: IntArray = mPreviewYCoordinates.getPrimitiveArray()
        mInterpolator.reset(px, py, 0, size)
        // The last segment of gesture stroke needs to be interpolated again because the slope of
        // the tangent at the last point isn't determined.
        var lastInterpolatedDrawIndex: Int = lastInterpolatedIndex
        var d1: Int = lastInterpolatedIndex
        for (p2 in mLastInterpolatedPreviewIndex + 1 until size) {
            val p1: Int = p2 - 1
            val p0: Int = p1 - 1
            val p3: Int = p2 + 1
            mLastInterpolatedPreviewIndex = p1
            lastInterpolatedDrawIndex = d1
            mInterpolator.setInterval(p0, p1, p2, p3)
            val m1: Double =
                atan2(mInterpolator.mSlope1Y.toDouble(), mInterpolator.mSlope1X.toDouble())
            val m2: Double =
                atan2(mInterpolator.mSlope2Y.toDouble(), mInterpolator.mSlope2X.toDouble())
            val deltaAngle: Double = abs(angularDiff(m2, m1))
            val segmentsByAngle: Int = ceil(
                deltaAngle / mDrawingParams!!.mMaxInterpolationAngularThreshold
            ) as Int
            val deltaDistance: Double = hypot(
                (mInterpolator.mP1X - mInterpolator.mP2X).toDouble(),
                (mInterpolator.mP1Y - mInterpolator.mP2Y).toDouble()
            )
            val segmentsByDistance: Int = ceil(
                deltaDistance
                        / mDrawingParams.mMaxInterpolationDistanceThreshold
            ) as Int
            val segments: Int = min(
                mDrawingParams.mMaxInterpolationSegments.toDouble(),
                max(segmentsByAngle.toDouble(), segmentsByDistance.toDouble())
            ).toInt()
            val t1: Int = eventTimes.get(d1)
            val dt: Int = pt.get(p2) - pt.get(p1)
            d1++
            for (i in 1 until segments) {
                val t: Float = i / segments.toFloat()
                mInterpolator.interpolate(t)
                eventTimes.addAt(d1, (dt * t).toInt() + t1)
                xCoords.addAt(d1, mInterpolator.mInterpolatedX.toInt())
                yCoords.addAt(d1, mInterpolator.mInterpolatedY.toInt())
                if (GestureTrailDrawingPoints.Companion.DEBUG_SHOW_POINTS) {
                    types.addAt(d1, GestureTrailDrawingPoints.Companion.POINT_TYPE_INTERPOLATED)
                }
                d1++
            }
            eventTimes.addAt(d1, pt.get(p2))
            xCoords.addAt(d1, px.get(p2))
            yCoords.addAt(d1, py.get(p2))
            if (GestureTrailDrawingPoints.Companion.DEBUG_SHOW_POINTS) {
                types.addAt(d1, GestureTrailDrawingPoints.Companion.POINT_TYPE_SAMPLED)
            }
        }
        return lastInterpolatedDrawIndex
    }

    init {
        mDrawingParams = drawingParams
    }

    companion object {
        const val PREVIEW_CAPACITY: Int = 256

        private val TWO_PI: Double = Math.PI * 2.0

        /**
         * Calculate the angular of rotation from `a0` to `a1`.
         *
         * @param a1 the angular to which the rotation ends.
         * @param a0 the angular from which the rotation starts.
         * @return the angular rotation value from a0 to a1, normalized to [-PI, +PI].
         */
        private fun angularDiff(a1: Double, a0: Double): Double {
            var deltaAngle: Double = a1 - a0
            while (deltaAngle > Math.PI) {
                deltaAngle -= TWO_PI
            }
            while (deltaAngle < -Math.PI) {
                deltaAngle += TWO_PI
            }
            return deltaAngle
        }
    }
}
