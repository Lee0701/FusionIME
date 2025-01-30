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

import android.content.SharedPreferences

/**
 * Class used to maintain settings.
 */
class Settings protected constructor(pref: SharedPreferences?) {
    init {
        mSharedPref = pref
        initConfs()
    }

    private fun initConfs() {
        mKeySound = mSharedPref!!.getBoolean(ANDPY_CONFS_KEYSOUND_KEY, true)
        mVibrate = mSharedPref!!.getBoolean(ANDPY_CONFS_VIBRATE_KEY, false)
        mPrediction = mSharedPref!!.getBoolean(ANDPY_CONFS_PREDICTION_KEY, true)
    }

    companion object {
        private const val ANDPY_CONFS_KEYSOUND_KEY = "Sound"
        private const val ANDPY_CONFS_VIBRATE_KEY = "Vibrate"
        private const val ANDPY_CONFS_PREDICTION_KEY = "Prediction"

        private var mKeySound = false
        private var mVibrate = false
        private var mPrediction = false

        private var mInstance: Settings? = null

        private var mRefCount = 0

        private var mSharedPref: SharedPreferences? = null

        fun getInstance(pref: SharedPreferences): Settings {
            if (mInstance == null) {
                mInstance = Settings(pref)
            }
            assert(pref === mSharedPref)
            mRefCount++
            return mInstance!!
        }

        fun writeBack() {
            val editor = mSharedPref!!.edit()
            editor.putBoolean(ANDPY_CONFS_VIBRATE_KEY, mVibrate)
            editor.putBoolean(ANDPY_CONFS_KEYSOUND_KEY, mKeySound)
            editor.putBoolean(ANDPY_CONFS_PREDICTION_KEY, mPrediction)
            editor.commit()
        }

        fun releaseInstance() {
            mRefCount--
            if (mRefCount == 0) {
                mInstance = null
            }
        }

        var keySound: Boolean
            get() = mKeySound
            set(v) {
                if (mKeySound == v) return
                mKeySound = v
            }

        var vibrate: Boolean
            get() = mVibrate
            set(v) {
                if (mVibrate == v) return
                mVibrate = v
            }

        var prediction: Boolean
            get() = mPrediction
            set(v) {
                if (mPrediction == v) return
                mPrediction = v
            }
    }
}
