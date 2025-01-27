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
package com.android.inputmethod.compat

import android.inputmethodservice.InputMethodService
import android.os.Build.VERSION_CODES
import android.view.View

object ViewOutlineProviderCompatUtils {
    private val EMPTY_INSETS_UPDATER: InsetsUpdater = object : InsetsUpdater {
        override fun setInsets(insets: InputMethodService.Insets) {}
    }

    fun setInsetsOutlineProvider(view: View): InsetsUpdater {
        if (BuildCompatUtils.EFFECTIVE_SDK_INT < VERSION_CODES.LOLLIPOP) {
            return EMPTY_INSETS_UPDATER
        }
        return ViewOutlineProviderCompatUtilsLXX.setInsetsOutlineProvider(view)
    }

    interface InsetsUpdater {
        fun setInsets(insets: InputMethodService.Insets)
    }
}
