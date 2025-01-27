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
package com.android.inputmethod.latin.common

class NativeSuggestOptions {
    val options: IntArray

    init {
        options = IntArray(OPTIONS_SIZE)
    }

    fun setIsGesture(value: Boolean) {
        setBooleanOption(IS_GESTURE, value)
    }

    fun setUseFullEditDistance(value: Boolean) {
        setBooleanOption(USE_FULL_EDIT_DISTANCE, value)
    }

    fun setBlockOffensiveWords(value: Boolean) {
        setBooleanOption(BLOCK_OFFENSIVE_WORDS, value)
    }

    fun setWeightForLocale(value: Float) {
        // We're passing this option as a fixed point value, in thousands. This is decoded in
        // native code by SuggestOptions#weightForLocale().
        setIntegerOption(WEIGHT_FOR_LOCALE_IN_THOUSANDS, (value * 1000).toInt())
    }

    private fun setBooleanOption(key: Int, value: Boolean) {
        options[key] = if (value) 1 else 0
    }

    private fun setIntegerOption(key: Int, value: Int) {
        options[key] = value
    }

    companion object {
        // Need to update suggest_options.h when you add, remove or reorder options.
        private const val IS_GESTURE = 0
        private const val USE_FULL_EDIT_DISTANCE = 1
        private const val BLOCK_OFFENSIVE_WORDS = 2
        private const val SPACE_AWARE_GESTURE_ENABLED = 3
        private const val WEIGHT_FOR_LOCALE_IN_THOUSANDS = 4
        private const val OPTIONS_SIZE = 5
    }
}
