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

import android.view.textservice.TextInfo
import com.android.inputmethod.annotations.UsedForTesting
import java.lang.reflect.Constructor
import java.lang.reflect.Method

@UsedForTesting
object TextInfoCompatUtils {
    // Note that TextInfo.getCharSequence() is supposed to be available in API level 21 and later.
    private val TEXT_INFO_GET_CHAR_SEQUENCE: Method? = CompatUtils.getMethod(
        TextInfo::class.java, "getCharSequence"
    )
    private val TEXT_INFO_CONSTRUCTOR_FOR_CHAR_SEQUENCE: Constructor<*>? =
        CompatUtils.getConstructor(
            TextInfo::class.java,
            CharSequence::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )

    @UsedForTesting
    fun isCharSequenceSupported(): Boolean {
        return TEXT_INFO_GET_CHAR_SEQUENCE != null &&
                TEXT_INFO_CONSTRUCTOR_FOR_CHAR_SEQUENCE != null
    }

    @UsedForTesting
    fun newInstance(
        charSequence: CharSequence, start: Int, end: Int, cookie: Int,
        sequenceNumber: Int
    ): TextInfo? {
        if (TEXT_INFO_CONSTRUCTOR_FOR_CHAR_SEQUENCE != null) {
            return CompatUtils.newInstance(
                TEXT_INFO_CONSTRUCTOR_FOR_CHAR_SEQUENCE,
                charSequence, start, end, cookie, sequenceNumber
            ) as TextInfo?
        }
        return TextInfo(
            charSequence.subSequence(start, end).toString(), cookie,
            sequenceNumber
        )
    }

    /**
     * Returns the result of [TextInfo.getCharSequence] when available. Otherwise returns
     * the result of [TextInfo.getText] as fall back.
     * @param textInfo the instance for which [TextInfo.getCharSequence] or
     * [TextInfo.getText] is called.
     * @return the result of [TextInfo.getCharSequence] when available. Otherwise returns
     * the result of [TextInfo.getText] as fall back. If `textInfo` is `null`,
     * returns `null`.
     */
    @UsedForTesting
    fun getCharSequenceOrString(textInfo: TextInfo?): CharSequence? {
        val defaultValue: CharSequence? = (if (textInfo == null) null else textInfo.getText())
        return CompatUtils.invoke(
            textInfo, defaultValue,
            TEXT_INFO_GET_CHAR_SEQUENCE
        ) as CharSequence?
    }
}
