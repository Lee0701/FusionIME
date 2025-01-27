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

import android.os.Build

object BuildCompatUtils {
    private val IS_RELEASE_BUILD: Boolean = Build.VERSION.CODENAME == "REL"

    /**
     * The "effective" API version.
     * [Build.VERSION.SDK_INT] if the platform is a release build.
     * [Build.VERSION.SDK_INT] plus 1 if the platform is a development build.
     */
    val EFFECTIVE_SDK_INT: Int = if (IS_RELEASE_BUILD)
        Build.VERSION.SDK_INT
    else
        Build.VERSION.SDK_INT + 1
}
