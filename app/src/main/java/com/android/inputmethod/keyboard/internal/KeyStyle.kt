/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.inputmethod.keyboard.internal

import android.content.res.TypedArray
import javax.annotation.Nonnull

abstract class KeyStyle protected constructor(@Nonnull textsSet: KeyboardTextsSet) {
    private val mTextsSet: KeyboardTextsSet

    abstract fun getStringArray(a: TypedArray, index: Int): Array<String?>?
    abstract fun getString(a: TypedArray, index: Int): String?
    abstract fun getInt(a: TypedArray, index: Int, defaultValue: Int): Int
    abstract fun getFlags(a: TypedArray, index: Int): Int

    init {
        mTextsSet = textsSet
    }

    protected fun parseString(a: TypedArray, index: Int): String? {
        if (a.hasValue(index)) {
            return mTextsSet.resolveTextReference(a.getString(index)!!)
        }
        return null
    }

    protected fun parseStringArray(a: TypedArray, index: Int): Array<String>? {
        if (a.hasValue(index)) {
            val text: String? = mTextsSet.resolveTextReference(a.getString(index)!!)
            return MoreKeySpec.Companion.splitKeySpecs(text)
        }
        return null
    }
}
