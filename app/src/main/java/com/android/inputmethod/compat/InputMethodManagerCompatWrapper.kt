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
package com.android.inputmethod.compat

import android.content.Context
import android.os.IBinder
import android.view.inputmethod.InputMethodManager
import java.lang.reflect.Method

class InputMethodManagerCompatWrapper(context: Context) {
    val mImm: InputMethodManager

    init {
        mImm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    fun switchToNextInputMethod(token: IBinder?, onlyCurrentIme: Boolean): Boolean {
        return CompatUtils.invoke(
            mImm, false,  /* defaultValue */
            METHOD_switchToNextInputMethod, token, onlyCurrentIme
        ) as Boolean
    }

    fun shouldOfferSwitchingToNextInputMethod(token: IBinder?): Boolean {
        return CompatUtils.invoke(
            mImm, false,  /* defaultValue */
            METHOD_shouldOfferSwitchingToNextInputMethod, token
        ) as Boolean
    }

    companion object {
        // Note that InputMethodManager.switchToNextInputMethod() has been introduced
        // in API level 16 (Build.VERSION_CODES.JELLY_BEAN).
        private val METHOD_switchToNextInputMethod: Method? = CompatUtils.getMethod(
            InputMethodManager::class.java, "switchToNextInputMethod",
            IBinder::class.java,
            Boolean::class.javaPrimitiveType
        )

        // Note that InputMethodManager.shouldOfferSwitchingToNextInputMethod() has been introduced
        // in API level 19 (Build.VERSION_CODES.KITKAT).
        private val METHOD_shouldOfferSwitchingToNextInputMethod: Method? = CompatUtils.getMethod(
            InputMethodManager::class.java,
            "shouldOfferSwitchingToNextInputMethod", IBinder::class.java
        )
    }
}
