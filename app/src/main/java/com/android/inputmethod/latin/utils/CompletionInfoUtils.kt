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
package com.android.inputmethod.latin.utils

import android.text.TextUtils
import android.view.inputmethod.CompletionInfo
import java.util.Arrays

/**
 * Utilities to do various stuff with CompletionInfo.
 */
object CompletionInfoUtils {
    fun removeNulls(src: Array<CompletionInfo?>): Array<CompletionInfo?> {
        var j = 0
        val dst = arrayOfNulls<CompletionInfo>(src.size)
        for (i in src.indices) {
            if (null != src[i] && !TextUtils.isEmpty(src[i]!!.text)) {
                dst[j] = src[i]
                ++j
            }
        }
        return Arrays.copyOfRange(dst, 0, j)
    }
}
