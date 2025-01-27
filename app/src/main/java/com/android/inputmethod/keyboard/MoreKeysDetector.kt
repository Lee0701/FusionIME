/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.inputmethod.keyboard

class MoreKeysDetector(slideAllowance: Float) : KeyDetector() {
    private val mSlideAllowanceSquare: Int
    private val mSlideAllowanceSquareTop: Int

    init {
        mSlideAllowanceSquare = (slideAllowance * slideAllowance).toInt()
        // Top slide allowance is slightly longer (sqrt(2) times) than other edges.
        mSlideAllowanceSquareTop = mSlideAllowanceSquare * 2
    }

    override fun alwaysAllowsKeySelectionByDraggingFinger(): Boolean {
        return true
    }

    override fun detectHitKey(x: Int, y: Int): Key? {
        val keyboard: Keyboard? = getKeyboard()
        if (keyboard == null) {
            return null
        }
        val touchX: Int = getTouchX(x)
        val touchY: Int = getTouchY(y)

        var nearestKey: Key? = null
        var nearestDist: Int = if ((y < 0)) mSlideAllowanceSquareTop else mSlideAllowanceSquare
        for (key: Key in keyboard.getSortedKeys()) {
            val dist: Int = key.squaredDistanceToEdge(touchX, touchY)
            if (dist < nearestDist) {
                nearestKey = key
                nearestDist = dist
            }
        }
        return nearestKey
    }
}
