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

import java.lang.reflect.Method

object CharacterCompat {
    // Note that Character.isAlphabetic(int), has been introduced in API level 19
    // (Build.VERSION_CODE.KITKAT).
    private val METHOD_isAlphabetic: Method? = CompatUtils.getMethod(
        Char::class.java, "isAlphabetic", Int::class.javaPrimitiveType
    )

    fun isAlphabetic(code: Int): Boolean {
        if (METHOD_isAlphabetic != null) {
            return CompatUtils.invoke(null, false, METHOD_isAlphabetic, code) as Boolean
        }
        when (Character.getType(code)) {
            Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER, Character.TITLECASE_LETTER, Character.MODIFIER_LETTER, Character.OTHER_LETTER, Character.LETTER_NUMBER -> return true
            else -> return false
        }
    }
}
