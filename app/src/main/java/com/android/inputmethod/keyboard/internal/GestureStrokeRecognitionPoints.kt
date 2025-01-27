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

import android.util.Log
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.InputPointers
import com.android.inputmethod.latin.common.ResizableIntArray
import kotlin.math.hypot

/**
 * This class holds event points to recognize a gesture stroke.
 * TODO: Should be package private class.
 */
class GestureStrokeRecognitionPoints(
    pointerId: Int,
    recognitionParams: GestureStrokeRecognitionParams?
) {
    private val mPointerId: Int
    private val mEventTimes: ResizableIntArray = ResizableIntArray(
        Constants.DEFAULT_GESTURE_POINTS_CAPACITY
    )
    private val mXCoordinates: ResizableIntArray = ResizableIntArray(
        Constants.DEFAULT_GESTURE_POINTS_CAPACITY
    )
    private val mYCoordinates: ResizableIntArray = ResizableIntArray(
        Constants.DEFAULT_GESTURE_POINTS_CAPACITY
    )

    private val mRecognitionParams: GestureStrokeRecognitionParams?

    private var mKeyWidth: Int = 0 // pixel
    private var mMinYCoordinate: Int = 0 // pixel
    private var mMaxYCoordinate: Int = 0 // pixel

    // Static threshold for starting gesture detection
    private var mDetectFastMoveSpeedThreshold: Int = 0 // pixel /sec
    private var mDetectFastMoveTime: Int = 0
    private var mDetectFastMoveX: Int = 0
    private var mDetectFastMoveY: Int = 0

    // Dynamic threshold for gesture after fast typing
    private var mAfterFastTyping: Boolean = false
    private var mGestureDynamicDistanceThresholdFrom: Int = 0 // pixel
    private var mGestureDynamicDistanceThresholdTo: Int = 0 // pixel

    // Variables for gesture sampling
    private var mGestureSamplingMinimumDistance: Int = 0 // pixel
    private var mLastMajorEventTime: Long = 0
    private var mLastMajorEventX: Int = 0
    private var mLastMajorEventY: Int = 0

    // Variables for gesture recognition
    private var mGestureRecognitionSpeedThreshold: Int = 0 // pixel / sec
    private var mIncrementalRecognitionSize: Int = 0
    private var mLastIncrementalBatchSize: Int = 0

    // TODO: Make this package private
    init {
        mPointerId = pointerId
        mRecognitionParams = recognitionParams
    }

    // TODO: Make this package private
    fun setKeyboardGeometry(keyWidth: Int, keyboardHeight: Int) {
        mKeyWidth = keyWidth
        mMinYCoordinate = -(keyboardHeight * EXTRA_GESTURE_TRAIL_AREA_ABOVE_KEYBOARD_RATIO).toInt()
        mMaxYCoordinate = keyboardHeight
        // TODO: Find an appropriate base metric for these length. Maybe diagonal length of the key?
        mDetectFastMoveSpeedThreshold =
            (keyWidth * mRecognitionParams!!.mDetectFastMoveSpeedThreshold).toInt()
        mGestureDynamicDistanceThresholdFrom =
            (keyWidth * mRecognitionParams.mDynamicDistanceThresholdFrom).toInt()
        mGestureDynamicDistanceThresholdTo =
            (keyWidth * mRecognitionParams.mDynamicDistanceThresholdTo).toInt()
        mGestureSamplingMinimumDistance =
            (keyWidth * mRecognitionParams.mSamplingMinimumDistance).toInt()
        mGestureRecognitionSpeedThreshold =
            (keyWidth * mRecognitionParams.mRecognitionSpeedThreshold).toInt()
        if (DEBUG) {
            Log.d(
                TAG, String.format(
                    "[%d] setKeyboardGeometry: keyWidth=%3d tT=%3d >> %3d tD=%3d >> %3d",
                    mPointerId, keyWidth,
                    mRecognitionParams.mDynamicTimeThresholdFrom,
                    mRecognitionParams.mDynamicTimeThresholdTo,
                    mGestureDynamicDistanceThresholdFrom,
                    mGestureDynamicDistanceThresholdTo
                )
            )
        }
    }

    // TODO: Make this package private
    fun getLength(): Int {
        return mEventTimes.getLength()
    }

    // TODO: Make this package private
    fun addDownEventPoint(
        x: Int, y: Int, elapsedTimeSinceFirstDown: Int,
        elapsedTimeSinceLastTyping: Int
    ) {
        reset()
        if (elapsedTimeSinceLastTyping < mRecognitionParams!!.mStaticTimeThresholdAfterFastTyping) {
            mAfterFastTyping = true
        }
        if (DEBUG) {
            Log.d(
                TAG, String.format(
                    "[%d] onDownEvent: dT=%3d%s", mPointerId,
                    elapsedTimeSinceLastTyping, if (mAfterFastTyping) " afterFastTyping" else ""
                )
            )
        }
        // Call {@link #addEventPoint(int,int,int,boolean)} to record this down event point as a
        // major event point.
        addEventPoint(x, y, elapsedTimeSinceFirstDown, true /* isMajorEvent */)
    }

    private fun getGestureDynamicDistanceThreshold(deltaTime: Int): Int {
        if (!mAfterFastTyping || deltaTime >= mRecognitionParams!!.mDynamicThresholdDecayDuration) {
            return mGestureDynamicDistanceThresholdTo
        }
        val decayedThreshold: Int =
            ((mGestureDynamicDistanceThresholdFrom - mGestureDynamicDistanceThresholdTo)
                    * deltaTime / mRecognitionParams!!.mDynamicThresholdDecayDuration)
        return mGestureDynamicDistanceThresholdFrom - decayedThreshold
    }

    private fun getGestureDynamicTimeThreshold(deltaTime: Int): Int {
        if (!mAfterFastTyping || deltaTime >= mRecognitionParams!!.mDynamicThresholdDecayDuration) {
            return mRecognitionParams!!.mDynamicTimeThresholdTo
        }
        val decayedThreshold: Int =
            ((mRecognitionParams!!.mDynamicTimeThresholdFrom
                    - mRecognitionParams.mDynamicTimeThresholdTo)
                    * deltaTime / mRecognitionParams.mDynamicThresholdDecayDuration)
        return mRecognitionParams.mDynamicTimeThresholdFrom - decayedThreshold
    }

    // TODO: Make this package private
    fun isStartOfAGesture(): Boolean {
        if (!hasDetectedFastMove()) {
            return false
        }
        val size: Int = getLength()
        if (size <= 0) {
            return false
        }
        val lastIndex: Int = size - 1
        val deltaTime: Int = mEventTimes.get(lastIndex) - mDetectFastMoveTime
        if (deltaTime < 0) {
            return false
        }
        val deltaDistance: Int = getDistance(
            mXCoordinates.get(lastIndex), mYCoordinates.get(lastIndex),
            mDetectFastMoveX, mDetectFastMoveY
        )
        val distanceThreshold: Int = getGestureDynamicDistanceThreshold(deltaTime)
        val timeThreshold: Int = getGestureDynamicTimeThreshold(deltaTime)
        val isStartOfAGesture: Boolean = deltaTime >= timeThreshold
                && deltaDistance >= distanceThreshold
        if (DEBUG) {
            Log.d(
                TAG, String.format(
                    "[%d] isStartOfAGesture: dT=%3d tT=%3d dD=%3d tD=%3d%s%s",
                    mPointerId, deltaTime, timeThreshold,
                    deltaDistance, distanceThreshold,
                    if (mAfterFastTyping) " afterFastTyping" else "",
                    if (isStartOfAGesture) " startOfAGesture" else ""
                )
            )
        }
        return isStartOfAGesture
    }

    // TODO: Make this package private
    fun duplicateLastPointWith(time: Int) {
        val lastIndex: Int = getLength() - 1
        if (lastIndex >= 0) {
            val x: Int = mXCoordinates.get(lastIndex)
            val y: Int = mYCoordinates.get(lastIndex)
            if (DEBUG) {
                Log.d(
                    TAG, String.format(
                        "[%d] duplicateLastPointWith: %d,%d|%d", mPointerId,
                        x, y, time
                    )
                )
            }
            // TODO: Have appendMajorPoint()
            appendPoint(x, y, time)
            updateIncrementalRecognitionSize(x, y, time)
        }
    }

    private fun reset() {
        mIncrementalRecognitionSize = 0
        mLastIncrementalBatchSize = 0
        mEventTimes.setLength(0)
        mXCoordinates.setLength(0)
        mYCoordinates.setLength(0)
        mLastMajorEventTime = 0
        mDetectFastMoveTime = 0
        mAfterFastTyping = false
    }

    private fun appendPoint(x: Int, y: Int, time: Int) {
        val lastIndex: Int = getLength() - 1
        // The point that is created by {@link duplicateLastPointWith(int)} may have later event
        // time than the next {@link MotionEvent}. To maintain the monotonicity of the event time,
        // drop the successive point here.
        if (lastIndex >= 0 && mEventTimes.get(lastIndex) > time) {
            Log.w(
                TAG, String.format(
                    "[%d] drop stale event: %d,%d|%d last: %d,%d|%d", mPointerId,
                    x, y, time, mXCoordinates.get(lastIndex), mYCoordinates.get(lastIndex),
                    mEventTimes.get(lastIndex)
                )
            )
            return
        }
        mEventTimes.add(time)
        mXCoordinates.add(x)
        mYCoordinates.add(y)
    }

    private fun updateMajorEvent(x: Int, y: Int, time: Int) {
        mLastMajorEventTime = time.toLong()
        mLastMajorEventX = x
        mLastMajorEventY = y
    }

    private fun hasDetectedFastMove(): Boolean {
        return mDetectFastMoveTime > 0
    }

    private fun detectFastMove(x: Int, y: Int, time: Int): Int {
        val size: Int = getLength()
        val lastIndex: Int = size - 1
        val lastX: Int = mXCoordinates.get(lastIndex)
        val lastY: Int = mYCoordinates.get(lastIndex)
        val dist: Int = getDistance(lastX, lastY, x, y)
        val msecs: Int = time - mEventTimes.get(lastIndex)
        if (msecs > 0) {
            val pixels: Int = getDistance(lastX, lastY, x, y)
            val pixelsPerSec: Int = pixels * MSEC_PER_SEC
            if (DEBUG_SPEED) {
                val speed: Float = pixelsPerSec.toFloat() / msecs / mKeyWidth
                Log.d(TAG, String.format("[%d] detectFastMove: speed=%5.2f", mPointerId, speed))
            }
            // Equivalent to (pixels / msecs < mStartSpeedThreshold / MSEC_PER_SEC)
            if (!hasDetectedFastMove() && pixelsPerSec > mDetectFastMoveSpeedThreshold * msecs) {
                if (DEBUG) {
                    val speed: Float = pixelsPerSec.toFloat() / msecs / mKeyWidth
                    Log.d(
                        TAG, String.format(
                            "[%d] detectFastMove: speed=%5.2f T=%3d points=%3d fastMove",
                            mPointerId, speed, time, size
                        )
                    )
                }
                mDetectFastMoveTime = time
                mDetectFastMoveX = x
                mDetectFastMoveY = y
            }
        }
        return dist
    }

    /**
     * Add an event point to this gesture stroke recognition points. Returns true if the event
     * point is on the valid gesture area.
     * @param x the x-coordinate of the event point
     * @param y the y-coordinate of the event point
     * @param time the elapsed time in millisecond from the first gesture down
     * @param isMajorEvent false if this is a historical move event
     * @return true if the event point is on the valid gesture area
     */
    // TODO: Make this package private
    fun addEventPoint(
        x: Int, y: Int, time: Int,
        isMajorEvent: Boolean
    ): Boolean {
        val size: Int = getLength()
        if (size <= 0) {
            // The first event of this stroke (a.k.a. down event).
            appendPoint(x, y, time)
            updateMajorEvent(x, y, time)
        } else {
            val distance: Int = detectFastMove(x, y, time)
            if (distance > mGestureSamplingMinimumDistance) {
                appendPoint(x, y, time)
            }
        }
        if (isMajorEvent) {
            updateIncrementalRecognitionSize(x, y, time)
            updateMajorEvent(x, y, time)
        }
        return y >= mMinYCoordinate && y < mMaxYCoordinate
    }

    private fun updateIncrementalRecognitionSize(x: Int, y: Int, time: Int) {
        val msecs: Int = (time - mLastMajorEventTime).toInt()
        if (msecs <= 0) {
            return
        }
        val pixels: Int = getDistance(mLastMajorEventX, mLastMajorEventY, x, y)
        val pixelsPerSec: Int = pixels * MSEC_PER_SEC
        // Equivalent to (pixels / msecs < mGestureRecognitionThreshold / MSEC_PER_SEC)
        if (pixelsPerSec < mGestureRecognitionSpeedThreshold * msecs) {
            mIncrementalRecognitionSize = getLength()
        }
    }

    // TODO: Make this package private
    fun hasRecognitionTimePast(
        currentTime: Long, lastRecognitionTime: Long
    ): Boolean {
        return currentTime > lastRecognitionTime + mRecognitionParams!!.mRecognitionMinimumTime
    }

    // TODO: Make this package private
    fun appendAllBatchPoints(out: InputPointers) {
        appendBatchPoints(out, getLength())
    }

    // TODO: Make this package private
    fun appendIncrementalBatchPoints(out: InputPointers) {
        appendBatchPoints(out, mIncrementalRecognitionSize)
    }

    private fun appendBatchPoints(out: InputPointers, size: Int) {
        val length: Int = size - mLastIncrementalBatchSize
        if (length <= 0) {
            return
        }
        out.append(
            mPointerId, mEventTimes, mXCoordinates, mYCoordinates,
            mLastIncrementalBatchSize, length
        )
        mLastIncrementalBatchSize = size
    }

    companion object {
        private val TAG: String = GestureStrokeRecognitionPoints::class.java.getSimpleName()
        private const val DEBUG: Boolean = false
        private const val DEBUG_SPEED: Boolean = false

        // The height of extra area above the keyboard to draw gesture trails.
        // Proportional to the keyboard height.
        const val EXTRA_GESTURE_TRAIL_AREA_ABOVE_KEYBOARD_RATIO: Float = 0.25f

        private const val MSEC_PER_SEC: Int = 1000

        private fun getDistance(x1: Int, y1: Int, x2: Int, y2: Int): Int {
            return hypot((x1 - x2).toDouble(), (y1 - y2).toDouble()) as Int
        }
    }
}
