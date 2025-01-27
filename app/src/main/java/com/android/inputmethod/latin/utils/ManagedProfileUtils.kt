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
import com.android.inputmethod.annotations.UsedForTesting

class ManagedProfileUtils private constructor() {
    fun hasWorkProfile(context: Context?): Boolean {
        return false
    }

    companion object {
        private val INSTANCE = ManagedProfileUtils()
        private var sTestInstance: ManagedProfileUtils? = null

        @UsedForTesting
        fun setTestInstance(testInstance: ManagedProfileUtils?) {
            sTestInstance = testInstance
        }

        val instance: ManagedProfileUtils
            get() = if (sTestInstance == null) INSTANCE else sTestInstance!!
    }
}