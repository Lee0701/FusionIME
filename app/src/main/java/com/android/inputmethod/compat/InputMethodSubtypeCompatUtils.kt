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

import android.os.Build
import android.os.Build.VERSION_CODES
import android.text.TextUtils
import android.util.Log
import android.view.inputmethod.InputMethodSubtype
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.latin.RichInputMethodSubtype
import com.android.inputmethod.latin.common.Constants.Subtype.ExtraValue
import com.android.inputmethod.latin.common.LocaleUtils
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.Locale

object InputMethodSubtypeCompatUtils {
    private val TAG: String = InputMethodSubtypeCompatUtils::class.java.getSimpleName()

    // Note that InputMethodSubtype(int nameId, int iconId, String locale, String mode,
    // String extraValue, boolean isAuxiliary, boolean overridesImplicitlyEnabledSubtype, int id)
    // has been introduced in API level 17 (Build.VERSION_CODE.JELLY_BEAN_MR1).
    private val CONSTRUCTOR_INPUT_METHOD_SUBTYPE: Constructor<*>? = CompatUtils.getConstructor(
        InputMethodSubtype::class.java,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        String::class.java,
        String::class.java,
        String::class.java,
        Boolean::class.javaPrimitiveType,
        Boolean::class.javaPrimitiveType,
        Int::class.javaPrimitiveType
    )

    init {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
            if (CONSTRUCTOR_INPUT_METHOD_SUBTYPE == null) {
                Log.w(TAG, "Warning!!! Constructor is not defined.")
            }
        }
    }

    // Note that {@link InputMethodSubtype#isAsciiCapable()} has been introduced in API level 19
    // (Build.VERSION_CODE.KITKAT).
    private val METHOD_isAsciiCapable: Method? = CompatUtils.getMethod(
        InputMethodSubtype::class.java, "isAsciiCapable"
    )

    @Suppress("deprecation")
    fun newInputMethodSubtype(
        nameId: Int, iconId: Int, locale: String?,
        mode: String?, extraValue: String?, isAuxiliary: Boolean,
        overridesImplicitlyEnabledSubtype: Boolean, id: Int
    ): InputMethodSubtype? {
        if (CONSTRUCTOR_INPUT_METHOD_SUBTYPE == null
            || Build.VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR1
        ) {
            return InputMethodSubtype(
                nameId, iconId, locale, mode, extraValue, isAuxiliary,
                overridesImplicitlyEnabledSubtype
            )
        }
        return CompatUtils.newInstance(
            CONSTRUCTOR_INPUT_METHOD_SUBTYPE,
            nameId, iconId, locale, mode, extraValue, isAuxiliary,
            overridesImplicitlyEnabledSubtype, id
        ) as InputMethodSubtype?
    }

    fun isAsciiCapable(subtype: RichInputMethodSubtype): Boolean {
        return isAsciiCapable(subtype.rawSubtype)
    }

    fun isAsciiCapable(subtype: InputMethodSubtype): Boolean {
        return isAsciiCapableWithAPI(subtype)
                || subtype.containsExtraValueKey(ExtraValue.ASCII_CAPABLE)
    }

    // Note that InputMethodSubtype.getLanguageTag() is expected to be available in Android N+.
    private val GET_LANGUAGE_TAG: Method? = CompatUtils.getMethod(
        InputMethodSubtype::class.java, "getLanguageTag"
    )

    fun getLocaleObject(subtype: InputMethodSubtype): Locale {
        // Locale.forLanguageTag() is available only in Android L and later.
        if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            val languageTag: String? =
                CompatUtils.invoke(subtype, null, GET_LANGUAGE_TAG) as String?
            if (!TextUtils.isEmpty(languageTag)) {
                return Locale.forLanguageTag(languageTag)
            }
        }
        return LocaleUtils.constructLocaleFromString(subtype.getLocale())!!
    }

    @UsedForTesting
    fun isAsciiCapableWithAPI(subtype: InputMethodSubtype?): Boolean {
        return CompatUtils.invoke(subtype, false, METHOD_isAsciiCapable) as Boolean
    }
}
