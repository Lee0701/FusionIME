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
package com.android.inputmethod.compat

import android.app.ActivityManager
import android.content.Context
import java.lang.reflect.Method
import kotlin.concurrent.Volatile

object ActivityManagerCompatUtils {
    private val LOCK: Any = Any()

    @Volatile
    private var sBoolean: Boolean? = null
    private val METHOD_isLowRamDevice: Method? = CompatUtils.getMethod(
        ActivityManager::class.java, "isLowRamDevice"
    )

    fun isLowRamDevice(context: Context): Boolean {
        if (sBoolean == null) {
            synchronized(LOCK) {
                if (sBoolean == null) {
                    val am: ActivityManager =
                        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    sBoolean = CompatUtils.invoke(am, false, METHOD_isLowRamDevice) as Boolean?
                }
            }
        }
        return sBoolean == true
    }
}
