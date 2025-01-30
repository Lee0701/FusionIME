/*
 * Copyright (C) 2009 The Android Open Source Project
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
package com.android.inputmethod.pinyin

import android.content.Context
import android.content.res.Configuration
import android.view.WindowManager

/**
 * Global environment configurations for showing soft keyboard and candidate
 * view. All original dimension values are defined in float, and the real size
 * is calculated from the float values of and screen size. In this way, this
 * input method can work even when screen size is changed.
 */
class Environment private constructor() {
    var screenWidth: Int = 0
        private set
    var screenHeight: Int = 0
        private set
    var keyHeight: Int = 0
        private set
    var heightForCandidates: Int = 0
        private set
    var keyBalloonWidthPlus: Int = 0
        private set
    var keyBalloonHeightPlus: Int = 0
        private set
    private var mNormalKeyTextSize = 0
    private var mFunctionKeyTextSize = 0
    private var mNormalBalloonTextSize = 0
    private var mFunctionBalloonTextSize = 0
    val configuration: Configuration = Configuration()
    private val mDebug = false

    fun onConfigurationChanged(newConfig: Configuration, context: Context) {
        if (configuration.orientation != newConfig.orientation) {
            val wm = context
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val d = wm.defaultDisplay
            screenWidth = d.width
            screenHeight = d.height

            val scale: Int
            if (screenHeight > screenWidth) {
                keyHeight = (screenHeight * KEY_HEIGHT_RATIO_PORTRAIT).toInt()
                heightForCandidates = (screenHeight * CANDIDATES_AREA_HEIGHT_RATIO_PORTRAIT).toInt()
                scale = screenWidth
            } else {
                keyHeight = (screenHeight * KEY_HEIGHT_RATIO_LANDSCAPE).toInt()
                heightForCandidates =
                    (screenHeight * CANDIDATES_AREA_HEIGHT_RATIO_LANDSCAPE).toInt()
                scale = screenHeight
            }
            mNormalKeyTextSize = (scale * NORMAL_KEY_TEXT_SIZE_RATIO).toInt()
            mFunctionKeyTextSize = (scale * FUNCTION_KEY_TEXT_SIZE_RATIO).toInt()
            mNormalBalloonTextSize = (scale * NORMAL_BALLOON_TEXT_SIZE_RATIO).toInt()
            mFunctionBalloonTextSize = (scale * FUNCTION_BALLOON_TEXT_SIZE_RATIO).toInt()
            keyBalloonWidthPlus = (scale * KEY_BALLOON_WIDTH_PLUS_RATIO).toInt()
            keyBalloonHeightPlus = (scale * KEY_BALLOON_HEIGHT_PLUS_RATIO).toInt()
        }

        configuration.updateFrom(newConfig)
    }

    val keyXMarginFactor: Float
        get() = 1.0f

    val keyYMarginFactor: Float
        get() {
            if (Configuration.ORIENTATION_LANDSCAPE == configuration.orientation) {
                return 0.7f
            }
            return 1.0f
        }

    val skbHeight: Int
        get() {
            if (Configuration.ORIENTATION_PORTRAIT == configuration.orientation) {
                return keyHeight * 4
            } else if (Configuration.ORIENTATION_LANDSCAPE == configuration.orientation) {
                return keyHeight * 4
            }
            return 0
        }

    fun getKeyTextSize(isFunctionKey: Boolean): Int {
        return if (isFunctionKey) {
            mFunctionKeyTextSize
        } else {
            mNormalKeyTextSize
        }
    }

    fun getBalloonTextSize(isFunctionKey: Boolean): Int {
        return if (isFunctionKey) {
            mFunctionBalloonTextSize
        } else {
            mNormalBalloonTextSize
        }
    }

    fun hasHardKeyboard(): Boolean {
        if (configuration.keyboard == Configuration.KEYBOARD_NOKEYS
            || configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES
        ) {
            return false
        }
        return true
    }

    fun needDebug(): Boolean {
        return mDebug
    }

    companion object {
        /**
         * The key height for portrait mode. It is relative to the screen height.
         */
        private const val KEY_HEIGHT_RATIO_PORTRAIT = 0.105f

        /**
         * The key height for landscape mode. It is relative to the screen height.
         */
        private const val KEY_HEIGHT_RATIO_LANDSCAPE = 0.147f

        /**
         * The height of the candidates area for portrait mode. It is relative to
         * screen height.
         */
        private const val CANDIDATES_AREA_HEIGHT_RATIO_PORTRAIT = 0.084f

        /**
         * The height of the candidates area for portrait mode. It is relative to
         * screen height.
         */
        private const val CANDIDATES_AREA_HEIGHT_RATIO_LANDSCAPE = 0.125f

        /**
         * How much should the balloon width be larger than width of the real key.
         * It is relative to the smaller one of screen width and height.
         */
        private const val KEY_BALLOON_WIDTH_PLUS_RATIO = 0.08f

        /**
         * How much should the balloon height be larger than that of the real key.
         * It is relative to the smaller one of screen width and height.
         */
        private const val KEY_BALLOON_HEIGHT_PLUS_RATIO = 0.07f

        /**
         * The text size for normal keys. It is relative to the smaller one of
         * screen width and height.
         */
        private const val NORMAL_KEY_TEXT_SIZE_RATIO = 0.075f

        /**
         * The text size for function keys. It is relative to the smaller one of
         * screen width and height.
         */
        private const val FUNCTION_KEY_TEXT_SIZE_RATIO = 0.055f

        /**
         * The text size balloons of normal keys. It is relative to the smaller one
         * of screen width and height.
         */
        private const val NORMAL_BALLOON_TEXT_SIZE_RATIO = 0.14f

        /**
         * The text size balloons of function keys. It is relative to the smaller
         * one of screen width and height.
         */
        private const val FUNCTION_BALLOON_TEXT_SIZE_RATIO = 0.085f

        /**
         * The configurations are managed in a singleton.
         */
        private var mInstance: Environment? = null

        val instance: Environment
            get() {
                if (null == mInstance) {
                    mInstance =
                        Environment()
                }
                return mInstance!!
            }
    }
}
