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

import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.InputPointers

/**
 * This class arbitrates batch input.
 * An instance of this class holds a [GestureStrokeRecognitionPoints].
 * And it arbitrates multiple strokes gestured by multiple fingers and aggregates those gesture
 * points into one batch input.
 */
class BatchInputArbiter(pointerId: Int, params: GestureStrokeRecognitionParams?) {
    interface BatchInputArbiterListener {
        fun onStartBatchInput()
        fun onUpdateBatchInput(
            aggregatedPointers: InputPointers, moveEventTime: Long
        )

        fun onStartUpdateBatchInputTimer()
        fun onEndBatchInput(aggregatedPointers: InputPointers, upEventTime: Long)
    }

    private val mRecognitionPoints: GestureStrokeRecognitionPoints

    init {
        mRecognitionPoints = GestureStrokeRecognitionPoints(pointerId, params)
    }

    fun setKeyboardGeometry(keyWidth: Int, keyboardHeight: Int) {
        mRecognitionPoints.setKeyboardGeometry(keyWidth, keyboardHeight)
    }

    /**
     * Calculate elapsed time since the first gesture down.
     * @param eventTime the time of this event.
     * @return the elapsed time in millisecond from the first gesture down.
     */
    fun getElapsedTimeSinceFirstDown(eventTime: Long): Int {
        return (eventTime - sGestureFirstDownTime).toInt()
    }

    /**
     * Add a down event point.
     * @param x the x-coordinate of this down event.
     * @param y the y-coordinate of this down event.
     * @param downEventTime the time of this down event.
     * @param lastLetterTypingTime the last typing input time.
     * @param activePointerCount the number of active pointers when this pointer down event occurs.
     */
    fun addDownEventPoint(
        x: Int, y: Int, downEventTime: Long,
        lastLetterTypingTime: Long, activePointerCount: Int
    ) {
        if (activePointerCount == 1) {
            sGestureFirstDownTime = downEventTime
        }
        val elapsedTimeSinceFirstDown: Int = getElapsedTimeSinceFirstDown(downEventTime)
        val elapsedTimeSinceLastTyping: Int = (downEventTime - lastLetterTypingTime).toInt()
        mRecognitionPoints.addDownEventPoint(
            x, y, elapsedTimeSinceFirstDown, elapsedTimeSinceLastTyping
        )
    }

    /**
     * Add a move event point.
     * @param x the x-coordinate of this move event.
     * @param y the y-coordinate of this move event.
     * @param moveEventTime the time of this move event.
     * @param isMajorEvent false if this is a historical move event.
     * @param listener [BatchInputArbiterListener.onStartUpdateBatchInputTimer] of this
     * `listener` may be called if enough move points have been added.
     * @return true if this move event occurs on the valid gesture area.
     */
    fun addMoveEventPoint(
        x: Int, y: Int, moveEventTime: Long,
        isMajorEvent: Boolean, listener: BatchInputArbiterListener
    ): Boolean {
        val beforeLength: Int = mRecognitionPoints.getLength()
        val onValidArea: Boolean = mRecognitionPoints.addEventPoint(
            x, y, getElapsedTimeSinceFirstDown(moveEventTime), isMajorEvent
        )
        if (mRecognitionPoints.getLength() > beforeLength) {
            listener.onStartUpdateBatchInputTimer()
        }
        return onValidArea
    }

    /**
     * Determine whether the batch input has started or not.
     * @param listener [BatchInputArbiterListener.onStartBatchInput] of this
     * `listener` will be called when the batch input has started successfully.
     * @return true if the batch input has started successfully.
     */
    fun mayStartBatchInput(listener: BatchInputArbiterListener): Boolean {
        if (!mRecognitionPoints.isStartOfAGesture()) {
            return false
        }
        synchronized(sAggregatedPointers) {
            sAggregatedPointers.reset()
            sLastRecognitionPointSize = 0
            sLastRecognitionTime = 0
            listener.onStartBatchInput()
        }
        return true
    }

    /**
     * Add synthetic move event point. After adding the point,
     * [.updateBatchInput] will be called internally.
     * @param syntheticMoveEventTime the synthetic move event time.
     * @param listener the listener to be passed to
     * [.updateBatchInput].
     */
    fun updateBatchInputByTimer(
        syntheticMoveEventTime: Long,
        listener: BatchInputArbiterListener
    ) {
        mRecognitionPoints.duplicateLastPointWith(
            getElapsedTimeSinceFirstDown(syntheticMoveEventTime)
        )
        updateBatchInput(syntheticMoveEventTime, listener)
    }

    /**
     * Determine whether we have enough gesture points to lookup dictionary.
     * @param moveEventTime the time of this move event.
     * @param listener [BatchInputArbiterListener.onUpdateBatchInput] of
     * this `listener` will be called when enough event points we have. Also
     * [BatchInputArbiterListener.onStartUpdateBatchInputTimer] will be called to have
     * possible future synthetic move event.
     */
    fun updateBatchInput(
        moveEventTime: Long,
        listener: BatchInputArbiterListener
    ) {
        synchronized(sAggregatedPointers) {
            mRecognitionPoints.appendIncrementalBatchPoints(sAggregatedPointers)
            val size: Int = sAggregatedPointers.getPointerSize()
            if (size > sLastRecognitionPointSize && mRecognitionPoints.hasRecognitionTimePast(
                    moveEventTime, sLastRecognitionTime
                )
            ) {
                listener.onUpdateBatchInput(sAggregatedPointers, moveEventTime)
                listener.onStartUpdateBatchInputTimer()
                // The listener may change the size of the pointers (when auto-committing
                // for example), so we need to get the size from the pointers again.
                sLastRecognitionPointSize = sAggregatedPointers.getPointerSize()
                sLastRecognitionTime = moveEventTime
            }
        }
    }

    /**
     * Determine whether the batch input has ended successfully or continues.
     * @param upEventTime the time of this up event.
     * @param activePointerCount the number of active pointers when this pointer up event occurs.
     * @param listener [BatchInputArbiterListener.onEndBatchInput] of this
     * `listener` will be called when the batch input has started successfully.
     * @return true if the batch input has ended successfully.
     */
    fun mayEndBatchInput(
        upEventTime: Long, activePointerCount: Int,
        listener: BatchInputArbiterListener
    ): Boolean {
        synchronized(sAggregatedPointers) {
            mRecognitionPoints.appendAllBatchPoints(sAggregatedPointers)
            if (activePointerCount == 1) {
                listener.onEndBatchInput(sAggregatedPointers, upEventTime)
                return true
            }
        }
        return false
    }

    companion object {
        // The starting time of the first stroke of a gesture input.
        private var sGestureFirstDownTime: Long = 0

        // The {@link InputPointers} that includes all events of a gesture input.
        private val sAggregatedPointers: InputPointers = InputPointers(
            Constants.DEFAULT_GESTURE_POINTS_CAPACITY
        )
        private var sLastRecognitionPointSize: Int = 0 // synchronized using sAggregatedPointers
        private var sLastRecognitionTime: Long = 0 // synchronized using sAggregatedPointers
    }
}
