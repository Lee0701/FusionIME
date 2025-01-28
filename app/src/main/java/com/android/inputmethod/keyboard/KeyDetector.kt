/*
 * Copyright (C) 2010 The Android Open Source Project
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

/**
 * This class handles key detection.
 */
open class KeyDetector @JvmOverloads constructor(
    keyHysteresisDistance: Float = 0.0f,
    keyHysteresisDistanceForSlidingModifier: Float = 0.0f
) {
    private val mKeyHysteresisDistanceSquared: Int
    private val mKeyHysteresisDistanceForSlidingModifierSquared: Int

    private var mKeyboard: Keyboard? = null
    private var mCorrectionX: Int = 0
    private var mCorrectionY: Int = 0

    /**
     * Key detection object constructor with key hysteresis distances.
     *
     * @param keyHysteresisDistance if the pointer movement distance is smaller than this, the
     * movement will not be handled as meaningful movement. The unit is pixel.
     * @param keyHysteresisDistanceForSlidingModifier the same parameter for sliding input that
     * starts from a modifier key such as shift and symbols key.
     */
    init {
        mKeyHysteresisDistanceSquared = (keyHysteresisDistance * keyHysteresisDistance).toInt()
        mKeyHysteresisDistanceForSlidingModifierSquared =
            (keyHysteresisDistanceForSlidingModifier * keyHysteresisDistanceForSlidingModifier).toInt()
    }

    fun setKeyboard(
        keyboard: Keyboard, correctionX: Float,
        correctionY: Float
    ) {
        if (keyboard == null) {
            throw NullPointerException()
        }
        mCorrectionX = correctionX.toInt()
        mCorrectionY = correctionY.toInt()
        mKeyboard = keyboard
    }

    fun getKeyHysteresisDistanceSquared(isSlidingFromModifier: Boolean): Int {
        return if (isSlidingFromModifier)
            mKeyHysteresisDistanceForSlidingModifierSquared
        else
            mKeyHysteresisDistanceSquared
    }

    fun getTouchX(x: Int): Int {
        return x + mCorrectionX
    }

    // TODO: Remove vertical correction.
    fun getTouchY(y: Int): Int {
        return y + mCorrectionY
    }

    fun getKeyboard(): Keyboard? {
        return mKeyboard
    }

    open fun alwaysAllowsKeySelectionByDraggingFinger(): Boolean {
        return false
    }

    /**
     * Detect the key whose hitbox the touch point is in.
     *
     * @param x The x-coordinate of a touch point
     * @param y The y-coordinate of a touch point
     * @return the key that the touch point hits.
     */
    open fun detectHitKey(x: Int, y: Int): Key? {
        if (mKeyboard == null) {
            return null
        }
        val touchX: Int = getTouchX(x)
        val touchY: Int = getTouchY(y)

        var minDistance: Int = Int.MAX_VALUE
        var primaryKey: Key? = null
        for (key in mKeyboard!!.getNearestKeys(touchX, touchY)) {
            if(key == null) continue
            // An edge key always has its enlarged hitbox to respond to an event that occurred in
            // the empty area around the key. (@see Key#markAsLeftEdge(KeyboardParams)} etc.)
            if (!key.isOnKey(touchX, touchY)) {
                continue
            }
            val distance: Int = key.squaredDistanceToEdge(touchX, touchY)
            if (distance > minDistance) {
                continue
            }
            // To take care of hitbox overlaps, we compare key's code here too.
            if (primaryKey == null || distance < minDistance || key.code > primaryKey.code) {
                minDistance = distance
                primaryKey = key
            }
        }
        return primaryKey
    }
}
