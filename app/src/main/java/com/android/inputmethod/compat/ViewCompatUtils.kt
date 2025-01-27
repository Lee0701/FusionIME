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

import android.view.View
import java.lang.reflect.Method

// TODO: Use {@link androidx.core.view.ViewCompat} instead of this utility class.
// Currently {@link #getPaddingEnd(View)} and {@link #setPaddingRelative(View,int,int,int,int)}
// are missing from android-support-v4 static library in KitKat SDK.
object ViewCompatUtils {
    // Note that View.getPaddingEnd(), View.setPaddingRelative(int,int,int,int) have been
    // introduced in API level 17 (Build.VERSION_CODE.JELLY_BEAN_MR1).
    private val METHOD_getPaddingEnd: Method? = CompatUtils.getMethod(
        View::class.java, "getPaddingEnd"
    )
    private val METHOD_setPaddingRelative: Method? = CompatUtils.getMethod(
        View::class.java, "setPaddingRelative",
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType
    )

    // Note that View.setTextAlignment(int) has been introduced in API level 17.
    private val METHOD_setTextAlignment: Method? = CompatUtils.getMethod(
        View::class.java, "setTextAlignment", Int::class.javaPrimitiveType
    )

    fun getPaddingEnd(view: View): Int {
        if (METHOD_getPaddingEnd == null) {
            return view.getPaddingRight()
        }
        return CompatUtils.invoke(view, 0, METHOD_getPaddingEnd) as Int
    }

    fun setPaddingRelative(
        view: View, start: Int, top: Int,
        end: Int, bottom: Int
    ) {
        if (METHOD_setPaddingRelative == null) {
            view.setPadding(start, top, end, bottom)
            return
        }
        CompatUtils.invoke(view, null, METHOD_setPaddingRelative, start, top, end, bottom)
    }

    // These TEXT_ALIGNMENT_* constants have been introduced in API 17.
    const val TEXT_ALIGNMENT_INHERIT: Int = 0
    const val TEXT_ALIGNMENT_GRAVITY: Int = 1
    const val TEXT_ALIGNMENT_TEXT_START: Int = 2
    const val TEXT_ALIGNMENT_TEXT_END: Int = 3
    const val TEXT_ALIGNMENT_CENTER: Int = 4
    const val TEXT_ALIGNMENT_VIEW_START: Int = 5
    const val TEXT_ALIGNMENT_VIEW_END: Int = 6

    fun setTextAlignment(view: View?, textAlignment: Int) {
        CompatUtils.invoke(view, null, METHOD_setTextAlignment, textAlignment)
    }
}
