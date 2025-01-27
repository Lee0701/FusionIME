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
package com.android.inputmethod.latin

import com.android.inputmethod.latin.common.Constants
import java.util.Locale

/**
 * Utility methods related contacts dictionary.
 */
object ContactsDictionaryUtils {
    /**
     * Returns the index of the last letter in the word, starting from position startIndex.
     */
    fun getWordEndPosition(
        string: String, len: Int,
        startIndex: Int
    ): Int {
        var end: Int
        var cp: Int = 0
        end = startIndex + 1
        while (end < len) {
            cp = string.codePointAt(end)
            if (cp != Constants.CODE_DASH && cp != Constants.CODE_SINGLE_QUOTE && !Character.isLetter(
                    cp
                )
            ) {
                break
            }
            end += Character.charCount(cp)
        }
        return end
    }

    /**
     * Returns true if the locale supports using first name and last name as bigrams.
     */
    fun useFirstLastBigramsForLocale(locale: Locale?): Boolean {
        // TODO: Add firstname/lastname bigram rules for other languages.
        if (locale != null && locale.getLanguage() == Locale.ENGLISH.getLanguage()) {
            return true
        }
        return false
    }
}
