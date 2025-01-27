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

import android.annotation.TargetApi
import android.content.Context
import android.os.Build.VERSION_CODES
import android.provider.UserDictionary.Words
import java.util.Locale

object UserDictionaryCompatUtils {
    @Suppress("deprecation")
    fun addWord(
        context: Context, word: String,
        freq: Int, shortcut: String?, locale: Locale?
    ) {
        if (BuildCompatUtils.EFFECTIVE_SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            addWordWithShortcut(context, word, freq, shortcut, locale)
            return
        }
        // Fall back to the pre-JellyBean method.
        val currentLocale: Locale = context.getResources().getConfiguration().locale
        val localeType: Int = if (currentLocale == locale)
            Words.LOCALE_TYPE_CURRENT
        else
            Words.LOCALE_TYPE_ALL
        Words.addWord(context, word, freq, localeType)
    }

    // {@link UserDictionary.Words#addWord(Context,String,int,String,Locale)} was introduced
    // in API level 16 (Build.VERSION_CODES.JELLY_BEAN).
    @TargetApi(VERSION_CODES.JELLY_BEAN)
    private fun addWordWithShortcut(
        context: Context, word: String,
        freq: Int, shortcut: String?, locale: Locale?
    ) {
        Words.addWord(context, word, freq, shortcut, locale)
    }
}

