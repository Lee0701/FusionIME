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
package com.android.inputmethod.latin.utils

import android.os.Build
import android.os.Build.VERSION_CODES
import android.text.TextUtils
import android.util.Log
import android.view.inputmethod.InputMethodSubtype
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.compat.InputMethodSubtypeCompatUtils
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.Constants.Subtype.ExtraValue
import com.android.inputmethod.latin.common.StringUtils

object AdditionalSubtypeUtils {
    private val TAG: String = AdditionalSubtypeUtils::class.java.simpleName

    private val EMPTY_SUBTYPE_ARRAY = arrayOfNulls<InputMethodSubtype>(0)

    @UsedForTesting
    fun isAdditionalSubtype(subtype: InputMethodSubtype): Boolean {
        return subtype.containsExtraValueKey(ExtraValue.IS_ADDITIONAL_SUBTYPE)
    }

    private const val LOCALE_AND_LAYOUT_SEPARATOR = ":"
    private const val INDEX_OF_LOCALE = 0
    private const val INDEX_OF_KEYBOARD_LAYOUT = 1
    private const val INDEX_OF_EXTRA_VALUE = 2
    private const val LENGTH_WITHOUT_EXTRA_VALUE = (INDEX_OF_KEYBOARD_LAYOUT + 1)
    private const val LENGTH_WITH_EXTRA_VALUE = (INDEX_OF_EXTRA_VALUE + 1)
    private const val PREF_SUBTYPE_SEPARATOR = ";"

    private fun createAdditionalSubtypeInternal(
        localeString: String, keyboardLayoutSetName: String,
        isAsciiCapable: Boolean, isEmojiCapable: Boolean
    ): InputMethodSubtype {
        val nameId = SubtypeLocaleUtils.getSubtypeNameId(localeString, keyboardLayoutSetName)
        val platformVersionDependentExtraValues = getPlatformVersionDependentExtraValue(
            localeString, keyboardLayoutSetName, isAsciiCapable, isEmojiCapable
        )
        val platformVersionIndependentSubtypeId =
            getPlatformVersionIndependentSubtypeId(localeString, keyboardLayoutSetName)
        // NOTE: In KitKat and later, InputMethodSubtypeBuilder#setIsAsciiCapable is also available.
        // TODO: Use InputMethodSubtypeBuilder#setIsAsciiCapable when appropriate.
        return InputMethodSubtypeCompatUtils.newInputMethodSubtype(
            nameId,
            R.drawable.ic_ime_switcher_dark, localeString, Constants.Subtype.KEYBOARD_MODE,
            platformVersionDependentExtraValues,
            false,  /* isAuxiliary */false,  /* overrideImplicitlyEnabledSubtype */
            platformVersionIndependentSubtypeId
        )!!
    }

    fun createDummyAdditionalSubtype(
        localeString: String, keyboardLayoutSetName: String
    ): InputMethodSubtype {
        return createAdditionalSubtypeInternal(
            localeString, keyboardLayoutSetName,
            false,  /* isAsciiCapable */false /* isEmojiCapable */
        )
    }

    fun createAsciiEmojiCapableAdditionalSubtype(
        localeString: String, keyboardLayoutSetName: String
    ): InputMethodSubtype {
        return createAdditionalSubtypeInternal(
            localeString, keyboardLayoutSetName,
            true,  /* isAsciiCapable */true /* isEmojiCapable */
        )
    }

    fun getPrefSubtype(subtype: InputMethodSubtype): String {
        val localeString = subtype.locale
        val keyboardLayoutSetName = SubtypeLocaleUtils.getKeyboardLayoutSetName(subtype)
        val layoutExtraValue = ExtraValue.KEYBOARD_LAYOUT_SET + "=" + keyboardLayoutSetName
        val extraValue = StringUtils.removeFromCommaSplittableTextIfExists(
            layoutExtraValue, StringUtils.removeFromCommaSplittableTextIfExists(
                ExtraValue.IS_ADDITIONAL_SUBTYPE, subtype.extraValue
            )
        )!!
        val basePrefSubtype = (localeString + LOCALE_AND_LAYOUT_SEPARATOR
                + keyboardLayoutSetName)
        return if (extraValue.isEmpty())
            basePrefSubtype
        else
            basePrefSubtype + LOCALE_AND_LAYOUT_SEPARATOR + extraValue
    }

    fun createAdditionalSubtypesArray(prefSubtypes: String): Array<InputMethodSubtype?> {
        if (TextUtils.isEmpty(prefSubtypes)) {
            return EMPTY_SUBTYPE_ARRAY
        }
        val prefSubtypeArray =
            prefSubtypes.split(PREF_SUBTYPE_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
        val subtypesList = ArrayList<InputMethodSubtype?>(prefSubtypeArray.size)
        for (prefSubtype in prefSubtypeArray) {
            val elems = prefSubtype.split(LOCALE_AND_LAYOUT_SEPARATOR.toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()
            if (elems.size != LENGTH_WITHOUT_EXTRA_VALUE
                && elems.size != LENGTH_WITH_EXTRA_VALUE
            ) {
                Log.w(
                    TAG, ("Unknown additional subtype specified: " + prefSubtype + " in "
                            + prefSubtypes)
                )
                continue
            }
            val localeString = elems[INDEX_OF_LOCALE]
            val keyboardLayoutSetName = elems[INDEX_OF_KEYBOARD_LAYOUT]
            // Here we assume that all the additional subtypes have AsciiCapable and EmojiCapable.
            // This is actually what the setting dialog for additional subtype is doing.
            val subtype = createAsciiEmojiCapableAdditionalSubtype(
                localeString, keyboardLayoutSetName
            )
            if (subtype.nameResId == SubtypeLocaleUtils.UNKNOWN_KEYBOARD_LAYOUT) {
                // Skip unknown keyboard layout subtype. This may happen when predefined keyboard
                // layout has been removed.
                continue
            }
            subtypesList.add(subtype)
        }
        return subtypesList.toTypedArray<InputMethodSubtype?>()
    }

    fun createPrefSubtypes(subtypes: Array<InputMethodSubtype>?): String {
        if (subtypes == null || subtypes.size == 0) {
            return ""
        }
        val sb = StringBuilder()
        for (subtype in subtypes) {
            if (sb.length > 0) {
                sb.append(PREF_SUBTYPE_SEPARATOR)
            }
            sb.append(getPrefSubtype(subtype))
        }
        return sb.toString()
    }

    fun createPrefSubtypes(prefSubtypes: Array<String?>?): String {
        if (prefSubtypes == null || prefSubtypes.size == 0) {
            return ""
        }
        val sb = StringBuilder()
        for (prefSubtype in prefSubtypes) {
            if (sb.length > 0) {
                sb.append(PREF_SUBTYPE_SEPARATOR)
            }
            sb.append(prefSubtype)
        }
        return sb.toString()
    }

    /**
     * Returns the extra value that is optimized for the running OS.
     *
     *
     * Historically the extra value has been used as the last resort to annotate various kinds of
     * attributes. Some of these attributes are valid only on some platform versions. Thus we cannot
     * assume that the extra values stored in a persistent storage are always valid. We need to
     * regenerate the extra value on the fly instead.
     *
     * @param localeString the locale string (e.g., "en_US").
     * @param keyboardLayoutSetName the keyboard layout set name (e.g., "dvorak").
     * @param isAsciiCapable true when ASCII characters are supported with this layout.
     * @param isEmojiCapable true when Unicode Emoji characters are supported with this layout.
     * @return extra value that is optimized for the running OS.
     * @see .getPlatformVersionIndependentSubtypeId
     */
    private fun getPlatformVersionDependentExtraValue(
        localeString: String,
        keyboardLayoutSetName: String, isAsciiCapable: Boolean,
        isEmojiCapable: Boolean
    ): String {
        val extraValueItems = ArrayList<String?>()
        extraValueItems.add(ExtraValue.KEYBOARD_LAYOUT_SET + "=" + keyboardLayoutSetName)
        if (isAsciiCapable) {
            extraValueItems.add(ExtraValue.ASCII_CAPABLE)
        }
        if (Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN &&
            SubtypeLocaleUtils.isExceptionalLocale(localeString)
        ) {
            extraValueItems.add(
                ExtraValue.UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME + "=" +
                        SubtypeLocaleUtils.getKeyboardLayoutSetDisplayName(keyboardLayoutSetName)
            )
        }
        if (isEmojiCapable && Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
            extraValueItems.add(ExtraValue.EMOJI_CAPABLE)
        }
        extraValueItems.add(ExtraValue.IS_ADDITIONAL_SUBTYPE)
        return TextUtils.join(",", extraValueItems)
    }

    /**
     * Returns the subtype ID that is supposed to be compatible between different version of OSes.
     *
     *
     * From the compatibility point of view, it is important to keep subtype id predictable and
     * stable between different OSes. For this purpose, the calculation code in this method is
     * carefully chosen and then fixed. Treat the following code as no more or less than a
     * hash function. Each component to be hashed can be different from the corresponding value
     * that is used to instantiate [InputMethodSubtype] actually.
     * For example, you don't need to update `compatibilityExtraValueItems` in this
     * method even when we need to add some new extra values for the actual instance of
     * [InputMethodSubtype].
     *
     * @param localeString the locale string (e.g., "en_US").
     * @param keyboardLayoutSetName the keyboard layout set name (e.g., "dvorak").
     * @return a platform-version independent subtype ID.
     * @see .getPlatformVersionDependentExtraValue
     */
    private fun getPlatformVersionIndependentSubtypeId(
        localeString: String,
        keyboardLayoutSetName: String
    ): Int {
        // For compatibility reasons, we concatenate the extra values in the following order.
        // - KeyboardLayoutSet
        // - AsciiCapable
        // - UntranslatableReplacementStringInSubtypeName
        // - EmojiCapable
        // - isAdditionalSubtype
        val compatibilityExtraValueItems = ArrayList<String?>()
        compatibilityExtraValueItems.add(ExtraValue.KEYBOARD_LAYOUT_SET + "=" + keyboardLayoutSetName)
        compatibilityExtraValueItems.add(ExtraValue.ASCII_CAPABLE)
        if (SubtypeLocaleUtils.isExceptionalLocale(localeString)) {
            compatibilityExtraValueItems.add(
                ExtraValue.UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME + "=" +
                        SubtypeLocaleUtils.getKeyboardLayoutSetDisplayName(keyboardLayoutSetName)
            )
        }
        compatibilityExtraValueItems.add(ExtraValue.EMOJI_CAPABLE)
        compatibilityExtraValueItems.add(ExtraValue.IS_ADDITIONAL_SUBTYPE)
        val compatibilityExtraValues = TextUtils.join(",", compatibilityExtraValueItems)
        return arrayOf<Any>(
            localeString,
            Constants.Subtype.KEYBOARD_MODE,
            compatibilityExtraValues,
            false,  /* isAuxiliary */
            false /* overrideImplicitlyEnabledSubtype */
        ).contentHashCode()
    }
}
