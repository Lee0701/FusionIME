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
package com.android.inputmethod.latin.utils

import android.view.inputmethod.InputMethodSubtype
import com.android.inputmethod.latin.RichInputMethodSubtype
import java.util.Locale

/**
 * This class determines that the language name on the spacebar should be displayed in what format.
 */
object LanguageOnSpacebarUtils {
    const val FORMAT_TYPE_NONE: Int = 0
    const val FORMAT_TYPE_LANGUAGE_ONLY: Int = 1
    const val FORMAT_TYPE_FULL_LOCALE: Int = 2

    private var sEnabledSubtypes = emptyList<InputMethodSubtype>()
    private var sIsSystemLanguageSameAsInputLanguage = false

    fun getLanguageOnSpacebarFormatType(
        subtype: RichInputMethodSubtype
    ): Int {
        if (subtype.isNoLanguage) {
            return FORMAT_TYPE_FULL_LOCALE
        }
        // Only this subtype is enabled and equals to the system locale.
        if (sEnabledSubtypes.size < 2 && sIsSystemLanguageSameAsInputLanguage) {
            return FORMAT_TYPE_NONE
        }
        val locale = subtype.locale ?: return FORMAT_TYPE_NONE
        val keyboardLanguage = locale.language
        val keyboardLayout = subtype.keyboardLayoutSetName
        var sameLanguageAndLayoutCount = 0
        for (ims in sEnabledSubtypes) {
            val language = SubtypeLocaleUtils.getSubtypeLocale(ims)!!.language
            if (keyboardLanguage == language && keyboardLayout == SubtypeLocaleUtils.getKeyboardLayoutSetName(
                    ims
                )
            ) {
                sameLanguageAndLayoutCount++
            }
        }
        // Display full locale name only when there are multiple subtypes that have the same
        // locale and keyboard layout. Otherwise displaying language name is enough.
        return if (sameLanguageAndLayoutCount > 1)
            FORMAT_TYPE_FULL_LOCALE
        else
            FORMAT_TYPE_LANGUAGE_ONLY
    }

    fun setEnabledSubtypes(enabledSubtypes: List<InputMethodSubtype>) {
        sEnabledSubtypes = enabledSubtypes
    }

    fun onSubtypeChanged(
        subtype: RichInputMethodSubtype,
        implicitlyEnabledSubtype: Boolean, systemLocale: Locale
    ) {
        val newLocale = subtype.locale
        if (systemLocale == newLocale) {
            sIsSystemLanguageSameAsInputLanguage = true
            return
        }
        if (systemLocale.language != newLocale.language) {
            sIsSystemLanguageSameAsInputLanguage = false
            return
        }
        // If the subtype is enabled explicitly, the language name should be displayed even when
        // the keyboard language and the system language are equal.
        sIsSystemLanguageSameAsInputLanguage = implicitlyEnabledSubtype
    }
}
