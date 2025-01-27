/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.inputmethod.latin.common

import java.util.Locale
import javax.annotation.Nonnull

/**
 * A class to help with handling Locales in string form.
 *
 * This file has the same meaning and features (and shares all of its code) with the one with the
 * same name in Latin IME. They need to be kept synchronized; for any update/bugfix to
 * this file, consider also updating/fixing the version in Latin IME.
 */
object LocaleUtils {
    // Locale match level constants.
    // A higher level of match is guaranteed to have a higher numerical value.
    // Some room is left within constants to add match cases that may arise necessary
    // in the future, for example differentiating between the case where the countries
    // are both present and different, and the case where one of the locales does not
    // specify the countries. This difference is not needed now.
    // Nothing matches.
    const val LOCALE_NO_MATCH: Int = 0

    // The languages matches, but the country are different. Or, the reference locale requires a
    // country and the tested locale does not have one.
    const val LOCALE_LANGUAGE_MATCH_COUNTRY_DIFFER: Int = 3

    // The languages and country match, but the variants are different. Or, the reference locale
    // requires a variant and the tested locale does not have one.
    const val LOCALE_LANGUAGE_AND_COUNTRY_MATCH_VARIANT_DIFFER: Int = 6

    // The required locale is null or empty so it will accept anything, and the tested locale
    // is non-null and non-empty.
    const val LOCALE_ANY_MATCH: Int = 10

    // The language matches, and the tested locale specifies a country but the reference locale
    // does not require one.
    const val LOCALE_LANGUAGE_MATCH: Int = 15

    // The language and the country match, and the tested locale specifies a variant but the
    // reference locale does not require one.
    const val LOCALE_LANGUAGE_AND_COUNTRY_MATCH: Int = 20

    // The compared locales are fully identical. This is the best match level.
    const val LOCALE_FULL_MATCH: Int = 30

    // The level at which a match is "normally" considered a locale match with standard algorithms.
    // Don't use this directly, use #isMatch to test.
    private const val LOCALE_MATCH = LOCALE_ANY_MATCH

    // Make this match the maximum match level. If this evolves to have more than 2 digits
    // when written in base 10, also adjust the getMatchLevelSortedString method.
    private const val MATCH_LEVEL_MAX = 30

    /**
     * Return how well a tested locale matches a reference locale.
     *
     * This will check the tested locale against the reference locale and return a measure of how
     * a well it matches the reference. The general idea is that the tested locale has to match
     * every specified part of the required locale. A full match occur when they are equal, a
     * partial match when the tested locale agrees with the reference locale but is more specific,
     * and a difference when the tested locale does not comply with all requirements from the
     * reference locale.
     * In more detail, if the reference locale specifies at least a language and the testedLocale
     * does not specify one, or specifies a different one, LOCALE_NO_MATCH is returned. If the
     * reference locale is empty or null, it will match anything - in the form of LOCALE_FULL_MATCH
     * if the tested locale is empty or null, and LOCALE_ANY_MATCH otherwise. If the reference and
     * tested locale agree on the language, but not on the country,
     * LOCALE_LANGUAGE_MATCH_COUNTRY_DIFFER is returned if the reference locale specifies a country,
     * and LOCALE_LANGUAGE_MATCH otherwise.
     * If they agree on both the language and the country, but not on the variant,
     * LOCALE_LANGUAGE_AND_COUNTRY_MATCH_VARIANT_DIFFER is returned if the reference locale
     * specifies a variant, and LOCALE_LANGUAGE_AND_COUNTRY_MATCH otherwise. If everything matches,
     * LOCALE_FULL_MATCH is returned.
     * Examples:
     * en <=> en_US  => LOCALE_LANGUAGE_MATCH
     * en_US <=> en => LOCALE_LANGUAGE_MATCH_COUNTRY_DIFFER
     * en_US_POSIX <=> en_US_Android  =>  LOCALE_LANGUAGE_AND_COUNTRY_MATCH_VARIANT_DIFFER
     * en_US <=> en_US_Android => LOCALE_LANGUAGE_AND_COUNTRY_MATCH
     * sp_US <=> en_US  =>  LOCALE_NO_MATCH
     * de <=> de  => LOCALE_FULL_MATCH
     * en_US <=> en_US => LOCALE_FULL_MATCH
     * "" <=> en_US => LOCALE_ANY_MATCH
     *
     * @param referenceLocale the reference locale to test against.
     * @param testedLocale the locale to test.
     * @return a constant that measures how well the tested locale matches the reference locale.
     */
    fun getMatchLevel(
        referenceLocale: String?,
        testedLocale: String?
    ): Int {
        if (StringUtils.isEmpty(referenceLocale)) {
            return if (StringUtils.isEmpty(testedLocale)) LOCALE_FULL_MATCH else LOCALE_ANY_MATCH
        }
        if (null == testedLocale) return LOCALE_NO_MATCH
        val referenceParams = referenceLocale!!.split("_".toRegex(), limit = 3).toTypedArray()
        val testedParams = testedLocale.split("_".toRegex(), limit = 3).toTypedArray()
        // By spec of String#split, [0] cannot be null and length cannot be 0.
        if (referenceParams[0] != testedParams[0]) return LOCALE_NO_MATCH
        when (referenceParams.size) {
            1 -> return if (1 == testedParams.size) LOCALE_FULL_MATCH else LOCALE_LANGUAGE_MATCH
            2 -> {
                if (1 == testedParams.size) return LOCALE_LANGUAGE_MATCH_COUNTRY_DIFFER
                if (referenceParams[1] != testedParams[1]) return LOCALE_LANGUAGE_MATCH_COUNTRY_DIFFER
                if (3 == testedParams.size) return LOCALE_LANGUAGE_AND_COUNTRY_MATCH
                return LOCALE_FULL_MATCH
            }

            3 -> {
                if (1 == testedParams.size) return LOCALE_LANGUAGE_MATCH_COUNTRY_DIFFER
                if (referenceParams[1] != testedParams[1]) return LOCALE_LANGUAGE_MATCH_COUNTRY_DIFFER
                if (2 == testedParams.size) return LOCALE_LANGUAGE_AND_COUNTRY_MATCH_VARIANT_DIFFER
                if (referenceParams[2] != testedParams[2]) return LOCALE_LANGUAGE_AND_COUNTRY_MATCH_VARIANT_DIFFER
                return LOCALE_FULL_MATCH
            }
        }
        // It should be impossible to come here
        return LOCALE_NO_MATCH
    }

    /**
     * Return a string that represents this match level, with better matches first.
     *
     * The strings are sorted in lexicographic order: a better match will always be less than
     * a worse match when compared together.
     */
    fun getMatchLevelSortedString(matchLevel: Int): String {
        // This works because the match levels are 0~99 (actually 0~30)
        // Ideally this should use a number of digits equals to the 1og10 of the greater matchLevel
        return String.format(Locale.ROOT, "%02d", MATCH_LEVEL_MAX - matchLevel)
    }

    /**
     * Find out whether a match level should be considered a match.
     *
     * This method takes a match level as returned by the #getMatchLevel method, and returns whether
     * it should be considered a match in the usual sense with standard Locale functions.
     *
     * @param level the match level, as returned by getMatchLevel.
     * @return whether this is a match or not.
     */
    fun isMatch(level: Int): Boolean {
        return LOCALE_MATCH <= level
    }

    private val sLocaleCache = HashMap<String?, Locale>()

    /**
     * Creates a locale from a string specification.
     * @param localeString a string specification of a locale, in a format of "ll_cc_variant" where
     * "ll" is a language code, "cc" is a country code.
     */
    @Nonnull
    fun constructLocaleFromString(@Nonnull localeString: String?): Locale? {
        synchronized(sLocaleCache) {
            if (sLocaleCache.containsKey(localeString)) {
                return sLocaleCache[localeString]
            }
            val elements = localeString!!.split("_".toRegex(), limit = 3).toTypedArray()
            val locale = if (elements.size == 1) {
                Locale(elements[0] /* language */)
            } else if (elements.size == 2) {
                Locale(elements[0],  /* language */elements[1] /* country */)
            } else { // localeParams.length == 3
                Locale(
                    elements[0],  /* language */elements[1],  /* country */
                    elements[2] /* variant */
                )
            }
            sLocaleCache[localeString] = locale
            return locale
        }
    }

    // TODO: Get this information from the framework instead of maintaining here by ourselves.
    private val sRtlLanguageCodes = HashSet<String>()

    init {
        // List of known Right-To-Left language codes.
        sRtlLanguageCodes.add("ar") // Arabic
        sRtlLanguageCodes.add("fa") // Persian
        sRtlLanguageCodes.add("iw") // Hebrew
        sRtlLanguageCodes.add("ku") // Kurdish
        sRtlLanguageCodes.add("ps") // Pashto
        sRtlLanguageCodes.add("sd") // Sindhi
        sRtlLanguageCodes.add("ug") // Uyghur
        sRtlLanguageCodes.add("ur") // Urdu
        sRtlLanguageCodes.add("yi") // Yiddish
    }

    fun isRtlLanguage(@Nonnull locale: Locale): Boolean {
        return sRtlLanguageCodes.contains(locale.language)
    }
}
