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

import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.Log
import ee.oyatl.ime.fusion.R
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.define.DebugFlags
import kotlin.math.abs
import kotlin.math.hypot

// This hack is applied to certain classes of tablets.
class BogusMoveEventDetector {
    private var mAccumulatedDistanceThreshold: Int = 0
    private var mRadiusThreshold: Int = 0

    // Accumulated distance from actual and artificial down keys.
    /* package */
    var mAccumulatedDistanceFromDownKey: Int = 0
    private var mActualDownX: Int = 0
    private var mActualDownY: Int = 0

    fun setKeyboardGeometry(keyWidth: Int, keyHeight: Int) {
        val keyDiagonal: Float = hypot(keyWidth.toDouble(), keyHeight.toDouble()).toFloat()
        mAccumulatedDistanceThreshold =
            (keyDiagonal * BOGUS_MOVE_ACCUMULATED_DISTANCE_THRESHOLD).toInt()
        mRadiusThreshold = (keyDiagonal * BOGUS_MOVE_RADIUS_THRESHOLD).toInt()
    }

    fun onActualDownEvent(x: Int, y: Int) {
        mActualDownX = x
        mActualDownY = y
    }

    fun onDownKey() {
        mAccumulatedDistanceFromDownKey = 0
    }

    fun onMoveKey(distance: Int) {
        mAccumulatedDistanceFromDownKey += distance
    }

    fun hasTraveledLongDistance(x: Int, y: Int): Boolean {
        if (!sNeedsProximateBogusDownMoveUpEventHack) {
            return false
        }
        val dx: Int = abs((x - mActualDownX).toDouble()).toInt()
        val dy: Int = abs((y - mActualDownY).toDouble()).toInt()
        // A bogus move event should be a horizontal movement. A vertical movement might be
        // a sloppy typing and should be ignored.
        return dx >= dy && mAccumulatedDistanceFromDownKey >= mAccumulatedDistanceThreshold
    }

    fun getAccumulatedDistanceFromDownKey(): Int {
        return mAccumulatedDistanceFromDownKey
    }

    fun getDistanceFromDownEvent(x: Int, y: Int): Int {
        return getDistance(x, y, mActualDownX, mActualDownY)
    }

    fun isCloseToActualDownEvent(x: Int, y: Int): Boolean {
        return sNeedsProximateBogusDownMoveUpEventHack
                && getDistanceFromDownEvent(x, y) < mRadiusThreshold
    }

    companion object {
        private val TAG: String = BogusMoveEventDetector::class.java.getSimpleName()
        private val DEBUG_MODE: Boolean = DebugFlags.DEBUG_ENABLED

        // Move these thresholds to resource.
        // These thresholds' unit is a diagonal length of a key.
        private const val BOGUS_MOVE_ACCUMULATED_DISTANCE_THRESHOLD: Float = 0.53f
        private const val BOGUS_MOVE_RADIUS_THRESHOLD: Float = 1.14f

        private var sNeedsProximateBogusDownMoveUpEventHack: Boolean = false

        fun init(res: Resources) {
            // The proximate bogus down move up event hack is needed for a device such like,
            // 1) is large tablet, or 2) is small tablet and the screen density is less than hdpi.
            // Though it seems odd to use screen density as criteria of the quality of the touch
            // screen, the small table that has a less density screen than hdpi most likely has been
            // made with the touch screen that needs the hack.
            val screenMetrics: Int = res.getInteger(R.integer.config_screen_metrics)
            val isLargeTablet: Boolean = (screenMetrics == Constants.SCREEN_METRICS_LARGE_TABLET)
            val isSmallTablet: Boolean = (screenMetrics == Constants.SCREEN_METRICS_SMALL_TABLET)
            val densityDpi: Int = res.getDisplayMetrics().densityDpi
            val hasLowDensityScreen: Boolean = (densityDpi < DisplayMetrics.DENSITY_HIGH)
            val needsTheHack: Boolean = isLargeTablet || (isSmallTablet && hasLowDensityScreen)
            if (DEBUG_MODE) {
                val sw: Int = res.getConfiguration().smallestScreenWidthDp
                Log.d(
                    TAG, ("needsProximateBogusDownMoveUpEventHack=" + needsTheHack
                            + " smallestScreenWidthDp=" + sw + " densityDpi=" + densityDpi
                            + " screenMetrics=" + screenMetrics)
                )
            }
            sNeedsProximateBogusDownMoveUpEventHack = needsTheHack
        }

        private fun getDistance(x1: Int, y1: Int, x2: Int, y2: Int): Int {
            return hypot((x1 - x2).toDouble(), (y1 - y2).toDouble()).toInt()
        }
    }
}
