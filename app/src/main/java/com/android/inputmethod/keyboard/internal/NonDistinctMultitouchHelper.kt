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
import android.view.MotionEvent
import com.android.inputmethod.keyboard.Key
import com.android.inputmethod.keyboard.KeyDetector
import com.android.inputmethod.keyboard.PointerTracker
import com.android.inputmethod.latin.common.CoordinateUtils

class NonDistinctMultitouchHelper {
    private var mOldPointerCount: Int = 1
    private var mOldKey: Key? = null
    private val mLastCoords: IntArray = CoordinateUtils.newInstance()

    fun processMotionEvent(me: MotionEvent, keyDetector: KeyDetector) {
        val pointerCount: Int = me.getPointerCount()
        val oldPointerCount: Int = mOldPointerCount
        mOldPointerCount = pointerCount
        // Ignore continuous multi-touch events because we can't trust the coordinates
        // in multi-touch events.
        if (pointerCount > 1 && oldPointerCount > 1) {
            return
        }

        // Use only main pointer tracker.
        val mainTracker: PointerTracker = PointerTracker.getPointerTracker(
            MAIN_POINTER_TRACKER_ID
        )
        val action: Int = me.getActionMasked()
        val index: Int = me.getActionIndex()
        val eventTime: Long = me.getEventTime()
        val downTime: Long = me.getDownTime()

        // In single-touch.
        if (oldPointerCount == 1 && pointerCount == 1) {
            if (me.getPointerId(index) == mainTracker.mPointerId) {
                mainTracker.processMotionEvent(me, keyDetector)
                return
            }
            // Inject a copied event.
            injectMotionEvent(
                action, me.getX(index), me.getY(index), downTime, eventTime,
                mainTracker, keyDetector
            )
            return
        }

        // Single-touch to multi-touch transition.
        if (oldPointerCount == 1 && pointerCount == 2) {
            // Send an up event for the last pointer, be cause we can't trust the coordinates of
            // this multi-touch event.
            mainTracker.getLastCoordinates(mLastCoords)
            val x: Int = CoordinateUtils.x(mLastCoords)
            val y: Int = CoordinateUtils.y(mLastCoords)
            mOldKey = mainTracker.getKeyOn(x, y)
            // Inject an artifact up event for the old key.
            injectMotionEvent(
                MotionEvent.ACTION_UP, x.toFloat(), y.toFloat(), downTime, eventTime,
                mainTracker, keyDetector
            )
            return
        }

        // Multi-touch to single-touch transition.
        if (oldPointerCount == 2 && pointerCount == 1) {
            // Send a down event for the latest pointer if the key is different from the previous
            // key.
            val x: Int = me.getX(index).toInt()
            val y: Int = me.getY(index).toInt()
            val newKey: Key? = mainTracker.getKeyOn(x, y)
            if (mOldKey !== newKey) {
                // Inject an artifact down event for the new key.
                // An artifact up event for the new key will usually be injected as a single-touch.
                injectMotionEvent(
                    MotionEvent.ACTION_DOWN, x.toFloat(), y.toFloat(), downTime, eventTime,
                    mainTracker, keyDetector
                )
                if (action == MotionEvent.ACTION_UP) {
                    // Inject an artifact up event for the new key also.
                    injectMotionEvent(
                        MotionEvent.ACTION_UP, x.toFloat(), y.toFloat(), downTime, eventTime,
                        mainTracker, keyDetector
                    )
                }
            }
            return
        }

        Log.w(
            TAG, ("Unknown touch panel behavior: pointer count is "
                    + pointerCount + " (previously " + oldPointerCount + ")")
        )
    }

    companion object {
        private val TAG: String = NonDistinctMultitouchHelper::class.java.getSimpleName()

        private const val MAIN_POINTER_TRACKER_ID: Int = 0
        private fun injectMotionEvent(
            action: Int, x: Float, y: Float,
            downTime: Long, eventTime: Long, tracker: PointerTracker,
            keyDetector: KeyDetector
        ) {
            val me: MotionEvent = MotionEvent.obtain(
                downTime, eventTime, action, x, y, 0 /* metaState */
            )
            try {
                tracker.processMotionEvent(me, keyDetector)
            } finally {
                me.recycle()
            }
        }
    }
}
