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
package com.android.inputmethod.latin.settings

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceFragment
import android.view.inputmethod.InputMethodSubtype
import com.android.inputmethod.latin.RichInputMethodManager
import com.android.inputmethod.latin.RichInputMethodSubtype
import javax.annotation.Nonnull

/**
 * Utility class for managing additional features settings.
 */
@Suppress("unused")
object AdditionalFeaturesSettingUtils {
    const val ADDITIONAL_FEATURES_SETTINGS_SIZE: Int = 0

    fun addAdditionalFeaturesPreferences(
        context: Context?, settingsFragment: PreferenceFragment?
    ) {
        // do nothing.
    }

    fun readAdditionalFeaturesPreferencesIntoArray(
        context: Context?,
        prefs: SharedPreferences?, additionalFeaturesPreferences: IntArray?
    ) {
        // do nothing.
    }

    @Nonnull
    fun createRichInputMethodSubtype(
        @Nonnull imm: RichInputMethodManager?,
        @Nonnull subtype: InputMethodSubtype,
        context: Context?
    ): RichInputMethodSubtype {
        return RichInputMethodSubtype(subtype)
    }
}
