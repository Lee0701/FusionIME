/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.inputmethod.latin.utils

import android.content.Context
import com.android.inputmethod.latin.DictionaryFacilitator
import com.android.inputmethod.latin.settings.SettingsValues

@Suppress("unused")
class StatsUtilsManager {
    fun onCreate(context: Context?, dictionaryFacilitator: DictionaryFacilitator?) {
    }

    fun onLoadSettings(context: Context?, settingsValues: SettingsValues?) {
    }

    fun onStartInputView() {
    }

    fun onFinishInputView() {
    }

    fun onDestroy(context: Context?) {
    }

    companion object {
        private val sInstance = StatsUtilsManager()
        private var sTestInstance: StatsUtilsManager? = null

        val instance: StatsUtilsManager?
            /**
             * @return the singleton instance of [StatsUtilsManager].
             */
            get() = if (sTestInstance != null) sTestInstance else sInstance

        fun setTestInstance(testInstance: StatsUtilsManager?) {
            sTestInstance = testInstance
        }
    }
}
