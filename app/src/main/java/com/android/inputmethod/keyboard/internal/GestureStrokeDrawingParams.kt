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

import android.content.res.TypedArray
import ee.oyatl.ime.fusion.R

/**
 * This class holds parameters to control how a gesture stroke is sampled and drawn on the screen.
 *
 * @attr ref android.R.styleable#MainKeyboardView_gestureTrailMinSamplingDistance
 * @attr ref android.R.styleable#MainKeyboardView_gestureTrailMaxInterpolationAngularThreshold
 * @attr ref android.R.styleable#MainKeyboardView_gestureTrailMaxInterpolationDistanceThreshold
 * @attr ref android.R.styleable#MainKeyboardView_gestureTrailMaxInterpolationSegments
 */
class GestureStrokeDrawingParams(mainKeyboardViewAttr: TypedArray) {
    val mMinSamplingDistance: Double // in pixel
    val mMaxInterpolationAngularThreshold: Double // in radian
    val mMaxInterpolationDistanceThreshold: Double // in pixel
    val mMaxInterpolationSegments: Int

    init {
        mMinSamplingDistance = mainKeyboardViewAttr.getDimension(
            R.styleable.MainKeyboardView_gestureTrailMinSamplingDistance,
            DEFAULT_MIN_SAMPLING_DISTANCE
        ).toDouble()
        val interpolationAngularDegree: Int = mainKeyboardViewAttr.getInteger(
            R.styleable
                .MainKeyboardView_gestureTrailMaxInterpolationAngularThreshold, 0
        )
        mMaxInterpolationAngularThreshold = if ((interpolationAngularDegree <= 0))
            Math.toRadians(DEFAULT_MAX_INTERPOLATION_ANGULAR_THRESHOLD.toDouble())
        else
            Math.toRadians(interpolationAngularDegree.toDouble())
        mMaxInterpolationDistanceThreshold = mainKeyboardViewAttr.getDimension(
            R.styleable
                .MainKeyboardView_gestureTrailMaxInterpolationDistanceThreshold,
            DEFAULT_MAX_INTERPOLATION_DISTANCE_THRESHOLD
        ).toDouble()
        mMaxInterpolationSegments = mainKeyboardViewAttr.getInteger(
            R.styleable.MainKeyboardView_gestureTrailMaxInterpolationSegments,
            DEFAULT_MAX_INTERPOLATION_SEGMENTS
        )
    }

    companion object {
        private const val DEFAULT_MIN_SAMPLING_DISTANCE: Float = 0.0f // dp
        private const val DEFAULT_MAX_INTERPOLATION_ANGULAR_THRESHOLD: Int = 15 // in degree
        private const val DEFAULT_MAX_INTERPOLATION_DISTANCE_THRESHOLD: Float = 0.0f // dp
        private const val DEFAULT_MAX_INTERPOLATION_SEGMENTS: Int = 4
    }
}
