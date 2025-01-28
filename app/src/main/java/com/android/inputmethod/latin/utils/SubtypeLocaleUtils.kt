/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.Build.VERSION_CODES
import android.util.Log
import android.view.inputmethod.InputMethodSubtype
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.common.Constants.Subtype.ExtraValue
import com.android.inputmethod.latin.common.LocaleUtils
import com.android.inputmethod.latin.common.StringUtils
import java.util.Locale
import kotlin.concurrent.Volatile

/**
 * A helper class to deal with subtype locales.
 */
// TODO: consolidate this into RichInputMethodSubtype
object SubtypeLocaleUtils {
    val TAG: String = SubtypeLocaleUtils::class.java.simpleName

    // This reference class {@link R} must be located in the same package as LatinIME.java.
    private val RESOURCE_PACKAGE_NAME: String = R::class.java.getPackage().name

    // Special language code to represent "no language".
    const val NO_LANGUAGE: String = "zz"
    const val QWERTY: String = "qwerty"
    const val EMOJI: String = "emoji"
    val UNKNOWN_KEYBOARD_LAYOUT: Int = R.string.subtype_generic

    @Volatile
    private var sInitialized = false
    private val sInitializeLock = Any()
    private var sResources: Resources? = null

    // Keyboard layout to its display name map.
    private val sKeyboardLayoutToDisplayNameMap = HashMap<String, String>()

    // Keyboard layout to subtype name resource id map.
    private val sKeyboardLayoutToNameIdsMap = HashMap<String, Int>()

    // Exceptional locale whose name should be displayed in Locale.ROOT.
    private val sExceptionalLocaleDisplayedInRootLocale = HashMap<String, Int>()

    // Exceptional locale to subtype name resource id map.
    private val sExceptionalLocaleToNameIdsMap = HashMap<String, Int>()

    // Exceptional locale to subtype name with layout resource id map.
    private val sExceptionalLocaleToWithLayoutNameIdsMap = HashMap<String, Int>()
    private const val SUBTYPE_NAME_RESOURCE_PREFIX = "string/subtype_"
    private const val SUBTYPE_NAME_RESOURCE_GENERIC_PREFIX = "string/subtype_generic_"
    private const val SUBTYPE_NAME_RESOURCE_WITH_LAYOUT_PREFIX = "string/subtype_with_layout_"
    private const val SUBTYPE_NAME_RESOURCE_NO_LANGUAGE_PREFIX = "string/subtype_no_language_"
    private const val SUBTYPE_NAME_RESOURCE_IN_ROOT_LOCALE_PREFIX = "string/subtype_in_root_locale_"

    // Keyboard layout set name for the subtypes that don't have a keyboardLayoutSet extra value.
    // This is for compatibility to keep the same subtype ids as pre-JellyBean.
    private val sLocaleAndExtraValueToKeyboardLayoutSetMap = HashMap<String, String>()

    // Note that this initialization method can be called multiple times.
    fun init(context: Context) {
        synchronized(sInitializeLock) {
            if (sInitialized == false) {
                initLocked(context)
                sInitialized = true
            }
        }
    }

    private fun initLocked(context: Context) {
        val res = context.resources
        sResources = res

        val predefinedLayoutSet = res.getStringArray(R.array.predefined_layouts)
        val layoutDisplayNames = res.getStringArray(
            R.array.predefined_layout_display_names
        )
        for (i in predefinedLayoutSet.indices) {
            val layoutName = predefinedLayoutSet[i]
            sKeyboardLayoutToDisplayNameMap[layoutName] =
                layoutDisplayNames[i]
            val resourceName = SUBTYPE_NAME_RESOURCE_GENERIC_PREFIX + layoutName
            val resId = res.getIdentifier(resourceName, null, RESOURCE_PACKAGE_NAME)
            sKeyboardLayoutToNameIdsMap[layoutName] = resId
            // Register subtype name resource id of "No language" with key "zz_<layout>"
            val noLanguageResName = SUBTYPE_NAME_RESOURCE_NO_LANGUAGE_PREFIX + layoutName
            val noLanguageResId = res.getIdentifier(
                noLanguageResName, null, RESOURCE_PACKAGE_NAME
            )
            val key = getNoLanguageLayoutKey(layoutName)
            sKeyboardLayoutToNameIdsMap[key] = noLanguageResId
        }

        val exceptionalLocaleInRootLocale = res.getStringArray(
            R.array.subtype_locale_displayed_in_root_locale
        )
        for (i in exceptionalLocaleInRootLocale.indices) {
            val localeString = exceptionalLocaleInRootLocale[i]
            val resourceName = SUBTYPE_NAME_RESOURCE_IN_ROOT_LOCALE_PREFIX + localeString
            val resId = res.getIdentifier(resourceName, null, RESOURCE_PACKAGE_NAME)
            sExceptionalLocaleDisplayedInRootLocale[localeString] = resId
        }

        val exceptionalLocales = res.getStringArray(
            R.array.subtype_locale_exception_keys
        )
        for (i in exceptionalLocales.indices) {
            val localeString = exceptionalLocales[i]
            val resourceName = SUBTYPE_NAME_RESOURCE_PREFIX + localeString
            val resId = res.getIdentifier(resourceName, null, RESOURCE_PACKAGE_NAME)
            sExceptionalLocaleToNameIdsMap[localeString] = resId
            val resourceNameWithLayout =
                SUBTYPE_NAME_RESOURCE_WITH_LAYOUT_PREFIX + localeString
            val resIdWithLayout = res.getIdentifier(
                resourceNameWithLayout, null, RESOURCE_PACKAGE_NAME
            )
            sExceptionalLocaleToWithLayoutNameIdsMap[localeString] =
                resIdWithLayout
        }

        val keyboardLayoutSetMap = res.getStringArray(
            R.array.locale_and_extra_value_to_keyboard_layout_set_map
        )
        var i = 0
        while (i + 1 < keyboardLayoutSetMap.size) {
            val key = keyboardLayoutSetMap[i]
            val keyboardLayoutSet = keyboardLayoutSetMap[i + 1]
            sLocaleAndExtraValueToKeyboardLayoutSetMap[key] = keyboardLayoutSet
            i += 2
        }
    }

    fun isExceptionalLocale(localeString: String): Boolean {
        return sExceptionalLocaleToNameIdsMap.containsKey(localeString)
    }

    private fun getNoLanguageLayoutKey(keyboardLayoutName: String): String {
        return NO_LANGUAGE + "_" + keyboardLayoutName
    }

    fun getSubtypeNameId(localeString: String, keyboardLayoutName: String): Int {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN
            && isExceptionalLocale(localeString)
        ) {
            return sExceptionalLocaleToWithLayoutNameIdsMap[localeString]!!
        }
        val key = if (NO_LANGUAGE == localeString)
            getNoLanguageLayoutKey(keyboardLayoutName)
        else
            keyboardLayoutName
        val nameId = sKeyboardLayoutToNameIdsMap[key]
        return nameId ?: UNKNOWN_KEYBOARD_LAYOUT
    }

    fun getDisplayLocaleOfSubtypeLocale(localeString: String): Locale {
        if (NO_LANGUAGE == localeString) {
            return sResources!!.configuration.locale
        }
        if (sExceptionalLocaleDisplayedInRootLocale.containsKey(localeString)) {
            return Locale.ROOT
        }
        return LocaleUtils.constructLocaleFromString(localeString)
    }

    fun getSubtypeLocaleDisplayNameInSystemLocale(
        localeString: String
    ): String {
        val displayLocale = sResources!!.configuration.locale
        return getSubtypeLocaleDisplayNameInternal(localeString, displayLocale)
    }

    fun getSubtypeLocaleDisplayName(localeString: String): String {
        val displayLocale = getDisplayLocaleOfSubtypeLocale(localeString)
        return getSubtypeLocaleDisplayNameInternal(localeString, displayLocale)
    }

    fun getSubtypeLanguageDisplayName(localeString: String): String {
        val displayLocale = getDisplayLocaleOfSubtypeLocale(localeString)
        val languageString =
            if (sExceptionalLocaleDisplayedInRootLocale.containsKey(localeString)) {
                localeString
            } else {
                LocaleUtils.constructLocaleFromString(
                    localeString
                ).language
            }
        return getSubtypeLocaleDisplayNameInternal(languageString, displayLocale)
    }

    private fun getSubtypeLocaleDisplayNameInternal(
        localeString: String,
        displayLocale: Locale
    ): String {
        if (NO_LANGUAGE == localeString) {
            // No language subtype should be displayed in system locale.
            return sResources!!.getString(R.string.subtype_no_language)
        }
        val exceptionalNameResId = if (displayLocale == Locale.ROOT
            && sExceptionalLocaleDisplayedInRootLocale.containsKey(localeString)
        ) {
            sExceptionalLocaleDisplayedInRootLocale[localeString]
        } else if (sExceptionalLocaleToNameIdsMap.containsKey(localeString)) {
            sExceptionalLocaleToNameIdsMap[localeString]
        } else {
            null
        }

        val displayName: String?
        if (exceptionalNameResId != null) {
            val getExceptionalName: RunInLocale<String> = object : RunInLocale<String>() {
                override fun job(res: Resources): String {
                    return res.getString(exceptionalNameResId)
                }
            }
            displayName = getExceptionalName.runInLocale(sResources!!, displayLocale)
        } else {
            displayName = LocaleUtils.constructLocaleFromString(localeString)
                .getDisplayName(displayLocale)
        }
        return StringUtils.capitalizeFirstCodePoint(displayName!!, displayLocale)
    }

    // InputMethodSubtype's display name in its locale.
    //        isAdditionalSubtype (T=true, F=false)
    // locale layout  |  display name
    // ------ ------- - ----------------------
    //  en_US qwerty  F  English (US)            exception
    //  en_GB qwerty  F  English (UK)            exception
    //  es_US spanish F  Español (EE.UU.)        exception
    //  fr    azerty  F  Français
    //  fr_CA qwerty  F  Français (Canada)
    //  fr_CH swiss   F  Français (Suisse)
    //  de    qwertz  F  Deutsch
    //  de_CH swiss   T  Deutsch (Schweiz)
    //  zz    qwerty  F  Alphabet (QWERTY)       in system locale
    //  fr    qwertz  T  Français (QWERTZ)
    //  de    qwerty  T  Deutsch (QWERTY)
    //  en_US azerty  T  English (US) (AZERTY)   exception
    //  zz    azerty  T  Alphabet (AZERTY)       in system locale
    private fun getReplacementString(
        subtype: InputMethodSubtype,
        displayLocale: Locale
    ): String {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN
            && subtype.containsExtraValueKey(ExtraValue.UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME)
        ) {
            return subtype.getExtraValueOf(ExtraValue.UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME)
        }
        return getSubtypeLocaleDisplayNameInternal(subtype.locale, displayLocale)!!
    }

    fun getSubtypeDisplayNameInSystemLocale(
        subtype: InputMethodSubtype
    ): String {
        val displayLocale = sResources!!.configuration.locale
        return getSubtypeDisplayNameInternal(subtype, displayLocale)
    }

    fun getSubtypeNameForLogging(subtype: InputMethodSubtype?): String {
        if (subtype == null) {
            return "<null subtype>"
        }
        return getSubtypeLocale(subtype).toString() + "/" + getKeyboardLayoutSetName(subtype)
    }

    private fun getSubtypeDisplayNameInternal(
        subtype: InputMethodSubtype,
        displayLocale: Locale
    ): String {
        val replacementString = getReplacementString(subtype, displayLocale)
        // TODO: rework this for multi-lingual subtypes
        val nameResId = subtype!!.nameResId
        val getSubtypeName: RunInLocale<String> = object : RunInLocale<String>() {
            override fun job(res: Resources): String {
                try {
                    return res.getString(nameResId, replacementString)
                } catch (e: Resources.NotFoundException) {
                    // TODO: Remove this catch when InputMethodManager.getCurrentInputMethodSubtype
                    // is fixed.
                    Log.w(
                        TAG,
                        ("""Unknown subtype: mode=${subtype.mode} nameResId=${subtype.nameResId} locale=${subtype.locale} extra=${subtype.extraValue}
${DebugLogUtils.getStackTrace(e)}""")
                    )
                    return ""
                }
            }
        }
        return StringUtils.capitalizeFirstCodePoint(
            getSubtypeName.runInLocale(sResources!!, displayLocale), displayLocale
        )
    }

    fun getSubtypeLocale(subtype: InputMethodSubtype): Locale {
        val localeString = subtype.locale
        return LocaleUtils.constructLocaleFromString(localeString)
    }

    fun getKeyboardLayoutSetDisplayName(
        subtype: InputMethodSubtype
    ): String {
        val layoutName = getKeyboardLayoutSetName(subtype)
        return getKeyboardLayoutSetDisplayName(layoutName)
    }

    fun getKeyboardLayoutSetDisplayName(layoutName: String): String {
        return sKeyboardLayoutToDisplayNameMap[layoutName]!!
    }

    fun getKeyboardLayoutSetName(subtype: InputMethodSubtype): String {
        var keyboardLayoutSet = subtype.getExtraValueOf(ExtraValue.KEYBOARD_LAYOUT_SET)
        if (keyboardLayoutSet == null) {
            // This subtype doesn't have a keyboardLayoutSet extra value, so lookup its keyboard
            // layout set in sLocaleAndExtraValueToKeyboardLayoutSetMap to keep it compatible with
            // pre-JellyBean.
            val key = subtype.locale + ":" + subtype.extraValue
            keyboardLayoutSet = sLocaleAndExtraValueToKeyboardLayoutSetMap[key]
        }
        // TODO: Remove this null check when InputMethodManager.getCurrentInputMethodSubtype is
        // fixed.
        if (keyboardLayoutSet == null) {
            Log.w(
                TAG, "KeyboardLayoutSet not found, use QWERTY: " +
                        "locale=" + subtype.locale + " extraValue=" + subtype.extraValue
            )
            return QWERTY
        }
        return keyboardLayoutSet
    }

    fun getCombiningRulesExtraValue(subtype: InputMethodSubtype): String {
        return subtype.getExtraValueOf(ExtraValue.COMBINING_RULES) ?: ""
    }
}
