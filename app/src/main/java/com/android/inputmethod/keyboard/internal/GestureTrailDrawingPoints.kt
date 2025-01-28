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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.os.SystemClock
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.ResizableIntArray
import kotlin.math.ceil
import kotlin.math.max

/**
 * This class holds drawing points to represent a gesture trail. The gesture trail may contain
 * multiple non-contiguous gesture strokes and will be animated asynchronously from gesture input.
 *
 * On the other hand, [GestureStrokeDrawingPoints] class holds drawing points of each gesture
 * stroke. This class holds drawing points of those gesture strokes to draw as a gesture trail.
 * Drawing points in this class will be asynchronously removed when fading out animation goes.
 */
internal class GestureTrailDrawingPoints {
    // These three {@link ResizableIntArray}s should be synchronized by {@link #mEventTimes}.
    private val mXCoordinates: ResizableIntArray = ResizableIntArray(DEFAULT_CAPACITY)
    private val mYCoordinates: ResizableIntArray = ResizableIntArray(DEFAULT_CAPACITY)
    private val mEventTimes: ResizableIntArray = ResizableIntArray(DEFAULT_CAPACITY)
    private val mPointTypes: ResizableIntArray = ResizableIntArray(
        if (DEBUG_SHOW_POINTS) DEFAULT_CAPACITY else 0
    )
    private var mCurrentStrokeId: Int = -1

    // The wall time of the zero value in {@link #mEventTimes}
    private var mCurrentTimeBase: Long = 0
    private var mTrailStartIndex: Int = 0
    private var mLastInterpolatedDrawIndex: Int = 0

    fun addStroke(stroke: GestureStrokeDrawingPoints, downTime: Long) {
        synchronized(mEventTimes) {
            addStrokeLocked(stroke, downTime)
        }
    }

    private fun addStrokeLocked(stroke: GestureStrokeDrawingPoints, downTime: Long) {
        val trailSize: Int = mEventTimes.length
        stroke.appendPreviewStroke(mEventTimes, mXCoordinates, mYCoordinates, mPointTypes)
        if (mEventTimes.length == trailSize) {
            return
        }
        val eventTimes: IntArray = mEventTimes.primitiveArray
        val strokeId: Int = stroke.getGestureStrokeId()
        // Because interpolation algorithm in {@link GestureStrokeDrawingPoints} can't determine
        // the interpolated points in the last segment of gesture stroke, it may need recalculation
        // of interpolation when new segments are added to the stroke.
        // {@link #mLastInterpolatedDrawIndex} holds the start index of the last segment. It may
        // be updated by the interpolation
        // {@link GestureStrokeDrawingPoints#interpolatePreviewStroke}
        // or by animation {@link #drawGestureTrail(Canvas,Paint,Rect,GestureTrailDrawingParams)}
        // below.
        val lastInterpolatedIndex: Int = if ((strokeId == mCurrentStrokeId))
            mLastInterpolatedDrawIndex
        else
            trailSize
        mLastInterpolatedDrawIndex = stroke.interpolateStrokeAndReturnStartIndexOfLastSegment(
            lastInterpolatedIndex, mEventTimes, mXCoordinates, mYCoordinates, mPointTypes
        )
        if (strokeId != mCurrentStrokeId) {
            val elapsedTime: Int = (downTime - mCurrentTimeBase).toInt()
            for (i in mTrailStartIndex until trailSize) {
                // Decay the previous strokes' event times.
                eventTimes[i] -= elapsedTime
            }
            val xCoords: IntArray = mXCoordinates.primitiveArray
            val downIndex: Int = trailSize
            xCoords[downIndex] = markAsDownEvent(xCoords[downIndex])
            mCurrentTimeBase = downTime - eventTimes[downIndex]
            mCurrentStrokeId = strokeId
        }
    }

    private val mRoundedLine: RoundedLine = RoundedLine()
    private val mRoundedLineBounds: Rect = Rect()

    /**
     * Draw gesture trail
     * @param canvas The canvas to draw the gesture trail
     * @param paint The paint object to be used to draw the gesture trail
     * @param outBoundsRect the bounding box of this gesture trail drawing
     * @param params The drawing parameters of gesture trail
     * @return true if some gesture trails remain to be drawn
     */
    fun drawGestureTrail(
        canvas: Canvas, paint: Paint,
        outBoundsRect: Rect, params: GestureTrailDrawingParams
    ): Boolean {
        synchronized(mEventTimes) {
            return drawGestureTrailLocked(canvas, paint, outBoundsRect, params)
        }
    }

    private fun drawGestureTrailLocked(
        canvas: Canvas, paint: Paint,
        outBoundsRect: Rect, params: GestureTrailDrawingParams
    ): Boolean {
        // Initialize bounds rectangle.
        outBoundsRect.setEmpty()
        val trailSize: Int = mEventTimes.length
        if (trailSize == 0) {
            return false
        }

        val eventTimes: IntArray = mEventTimes.primitiveArray
        val xCoords: IntArray = mXCoordinates.primitiveArray
        val yCoords: IntArray = mYCoordinates.primitiveArray
        val pointTypes: IntArray = mPointTypes.primitiveArray
        val sinceDown: Int = (SystemClock.uptimeMillis() - mCurrentTimeBase).toInt()
        var startIndex: Int
        startIndex = mTrailStartIndex
        while (startIndex < trailSize) {
            val elapsedTime: Int = sinceDown - eventTimes.get(startIndex)
            // Skip too old trail points.
            if (elapsedTime < params.mTrailLingerDuration) {
                break
            }
            startIndex++
        }
        mTrailStartIndex = startIndex

        if (startIndex < trailSize) {
            paint.setColor(params.mTrailColor)
            paint.setStyle(Paint.Style.FILL)
            val roundedLine: RoundedLine = mRoundedLine
            var p1x: Int = getXCoordValue(xCoords.get(startIndex))
            var p1y: Int = yCoords.get(startIndex)
            val lastTime: Int = sinceDown - eventTimes.get(startIndex)
            var r1: Float = getWidth(lastTime, params) / 2.0f
            for (i in startIndex + 1 until trailSize) {
                val elapsedTime: Int = sinceDown - eventTimes.get(i)
                val p2x: Int = getXCoordValue(xCoords.get(i))
                val p2y: Int = yCoords.get(i)
                val r2: Float = getWidth(elapsedTime, params) / 2.0f
                // Draw trail line only when the current point isn't a down point.
                if (!isDownEventXCoord(xCoords.get(i))) {
                    val body1: Float = r1 * params.mTrailBodyRatio
                    val body2: Float = r2 * params.mTrailBodyRatio
                    val path: Path = roundedLine.makePath(
                        p1x.toFloat(),
                        p1y.toFloat(),
                        body1,
                        p2x.toFloat(),
                        p2y.toFloat(),
                        body2
                    )
                    if (!path.isEmpty()) {
                        roundedLine.getBounds(mRoundedLineBounds)
                        if (params.mTrailShadowEnabled) {
                            val shadow2: Float = r2 * params.mTrailShadowRatio
                            paint.setShadowLayer(shadow2, 0.0f, 0.0f, params.mTrailColor)
                            val shadowInset: Int = -ceil(shadow2.toDouble()).toInt()
                            mRoundedLineBounds.inset(shadowInset, shadowInset)
                        }
                        // Take union for the bounds.
                        outBoundsRect.union(mRoundedLineBounds)
                        val alpha: Int = getAlpha(elapsedTime, params)
                        paint.setAlpha(alpha)
                        canvas.drawPath(path, paint)
                    }
                }
                p1x = p2x
                p1y = p2y
                r1 = r2
            }
            if (DEBUG_SHOW_POINTS) {
                debugDrawPoints(canvas, startIndex, trailSize, paint)
            }
        }

        val newSize: Int = trailSize - startIndex
        if (newSize < startIndex) {
            mTrailStartIndex = 0
            if (newSize > 0) {
                System.arraycopy(eventTimes, startIndex, eventTimes, 0, newSize)
                System.arraycopy(xCoords, startIndex, xCoords, 0, newSize)
                System.arraycopy(yCoords, startIndex, yCoords, 0, newSize)
                if (DEBUG_SHOW_POINTS) {
                    System.arraycopy(pointTypes, startIndex, pointTypes, 0, newSize)
                }
            }
            mEventTimes.length = newSize
            mXCoordinates.length = newSize
            mYCoordinates.length = newSize
            if (DEBUG_SHOW_POINTS) {
                mPointTypes.length = newSize
            }
            // The start index of the last segment of the stroke
            // {@link mLastInterpolatedDrawIndex} should also be updated because all array
            // elements have just been shifted for compaction or been zeroed.
            mLastInterpolatedDrawIndex =
                max((mLastInterpolatedDrawIndex - startIndex).toDouble(), 0.0).toInt()
        }
        return newSize > 0
    }

    private fun debugDrawPoints(
        canvas: Canvas, startIndex: Int, endIndex: Int,
        paint: Paint
    ) {
        val xCoords: IntArray = mXCoordinates.primitiveArray
        val yCoords: IntArray = mYCoordinates.primitiveArray
        val pointTypes: IntArray = mPointTypes.primitiveArray
        // {@link Paint} that is zero width stroke and anti alias off draws exactly 1 pixel.
        paint.setAntiAlias(false)
        paint.setStrokeWidth(0f)
        for (i in startIndex until endIndex) {
            val pointType: Int = pointTypes.get(i)
            if (pointType == POINT_TYPE_INTERPOLATED) {
                paint.setColor(Color.RED)
            } else if (pointType == POINT_TYPE_SAMPLED) {
                paint.setColor(-0x5fff01)
            } else {
                paint.setColor(Color.GREEN)
            }
            canvas.drawPoint(
                getXCoordValue(xCoords.get(i)).toFloat(),
                yCoords.get(i).toFloat(),
                paint
            )
        }
        paint.setAntiAlias(true)
    }

    companion object {
        const val DEBUG_SHOW_POINTS: Boolean = false
        const val POINT_TYPE_SAMPLED: Int = 1
        const val POINT_TYPE_INTERPOLATED: Int = 2

        private val DEFAULT_CAPACITY: Int = GestureStrokeDrawingPoints.PREVIEW_CAPACITY

        // Use this value as imaginary zero because x-coordinates may be zero.
        private val DOWN_EVENT_MARKER: Int = -128

        private fun markAsDownEvent(xCoord: Int): Int {
            return DOWN_EVENT_MARKER - xCoord
        }

        private fun isDownEventXCoord(xCoordOrMark: Int): Boolean {
            return xCoordOrMark <= DOWN_EVENT_MARKER
        }

        private fun getXCoordValue(xCoordOrMark: Int): Int {
            return if (isDownEventXCoord(xCoordOrMark))
                DOWN_EVENT_MARKER - xCoordOrMark
            else
                xCoordOrMark
        }

        /**
         * Calculate the alpha of a gesture trail.
         * A gesture trail starts from fully opaque. After mFadeStartDelay has been passed, the alpha
         * of a trail reduces in proportion to the elapsed time. Then after mFadeDuration has been
         * passed, a trail becomes fully transparent.
         *
         * @param elapsedTime the elapsed time since a trail has been made.
         * @param params gesture trail display parameters
         * @return the width of a gesture trail
         */
        private fun getAlpha(elapsedTime: Int, params: GestureTrailDrawingParams): Int {
            if (elapsedTime < params.mFadeoutStartDelay) {
                return Constants.Color.ALPHA_OPAQUE
            }
            val decreasingAlpha: Int = (Constants.Color.ALPHA_OPAQUE
                    * (elapsedTime - params.mFadeoutStartDelay)
                    / params.mFadeoutDuration)
            return Constants.Color.ALPHA_OPAQUE - decreasingAlpha
        }

        /**
         * Calculate the width of a gesture trail.
         * A gesture trail starts from the width of mTrailStartWidth and reduces its width in proportion
         * to the elapsed time. After mTrailEndWidth has been passed, the width becomes mTraiLEndWidth.
         *
         * @param elapsedTime the elapsed time since a trail has been made.
         * @param params gesture trail display parameters
         * @return the width of a gesture trail
         */
        private fun getWidth(elapsedTime: Int, params: GestureTrailDrawingParams): Float {
            val deltaWidth: Float = params.mTrailStartWidth - params.mTrailEndWidth
            return params.mTrailStartWidth - (deltaWidth * elapsedTime) / params.mTrailLingerDuration
        }
    }
}
