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
import com.android.inputmethod.latin.R

/**
 * This class holds parameters to control how a gesture trail is drawn and animated on the screen.
 *
 * On the other hand, [GestureStrokeDrawingParams] class controls how each gesture stroke is
 * sampled and interpolated. This class controls how those gesture strokes are displayed as a
 * gesture trail and animated on the screen.
 *
 * @attr ref android.R.styleable#MainKeyboardView_gestureTrailFadeoutStartDelay
 * @attr ref android.R.styleable#MainKeyboardView_gestureTrailFadeoutDuration
 * @attr ref android.R.styleable#MainKeyboardView_gestureTrailUpdateInterval
 * @attr ref android.R.styleable#MainKeyboardView_gestureTrailColor
 * @attr ref android.R.styleable#MainKeyboardView_gestureTrailWidth
 */
internal class GestureTrailDrawingParams(mainKeyboardViewAttr: TypedArray) {
    val mTrailColor: Int
    val mTrailStartWidth: Float
    val mTrailEndWidth: Float
    val mTrailBodyRatio: Float
    var mTrailShadowEnabled: Boolean
    val mTrailShadowRatio: Float
    val mFadeoutStartDelay: Int
    val mFadeoutDuration: Int
    val mUpdateInterval: Int

    val mTrailLingerDuration: Int

    init {
        mTrailColor = mainKeyboardViewAttr.getColor(
            R.styleable.MainKeyboardView_gestureTrailColor, 0
        )
        mTrailStartWidth = mainKeyboardViewAttr.getDimension(
            R.styleable.MainKeyboardView_gestureTrailStartWidth, 0.0f
        )
        mTrailEndWidth = mainKeyboardViewAttr.getDimension(
            R.styleable.MainKeyboardView_gestureTrailEndWidth, 0.0f
        )
        val PERCENTAGE_INT: Int = 100
        mTrailBodyRatio = mainKeyboardViewAttr.getInt(
            R.styleable.MainKeyboardView_gestureTrailBodyRatio, PERCENTAGE_INT
        ).toFloat() / PERCENTAGE_INT.toFloat()
        val trailShadowRatioInt: Int = mainKeyboardViewAttr.getInt(
            R.styleable.MainKeyboardView_gestureTrailShadowRatio, 0
        )
        mTrailShadowEnabled = (trailShadowRatioInt > 0)
        mTrailShadowRatio = trailShadowRatioInt.toFloat() / PERCENTAGE_INT.toFloat()
        mFadeoutStartDelay = if (GestureTrailDrawingPoints.Companion.DEBUG_SHOW_POINTS)
            FADEOUT_START_DELAY_FOR_DEBUG
        else
            mainKeyboardViewAttr.getInt(
                R.styleable.MainKeyboardView_gestureTrailFadeoutStartDelay, 0
            )
        mFadeoutDuration = if (GestureTrailDrawingPoints.Companion.DEBUG_SHOW_POINTS)
            FADEOUT_DURATION_FOR_DEBUG
        else
            mainKeyboardViewAttr.getInt(
                R.styleable.MainKeyboardView_gestureTrailFadeoutDuration, 0
            )
        mTrailLingerDuration = mFadeoutStartDelay + mFadeoutDuration
        mUpdateInterval = mainKeyboardViewAttr.getInt(
            R.styleable.MainKeyboardView_gestureTrailUpdateInterval, 0
        )
    }

    companion object {
        private const val FADEOUT_START_DELAY_FOR_DEBUG: Int = 2000 // millisecond
        private const val FADEOUT_DURATION_FOR_DEBUG: Int = 200 // millisecond
    }
}
