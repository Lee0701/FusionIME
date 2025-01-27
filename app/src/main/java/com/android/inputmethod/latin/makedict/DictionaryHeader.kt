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
package com.android.inputmethod.latin.makedict

import com.android.inputmethod.latin.makedict.FormatSpec.DictionaryOptions
import com.android.inputmethod.latin.makedict.FormatSpec.FormatOptions
import javax.annotation.Nonnull

/**
 * Class representing dictionary header.
 */
class DictionaryHeader(
    headerSize: Int,
    @Nonnull dictionaryOptions: DictionaryOptions,
    @Nonnull formatOptions: FormatOptions
) {
    val mBodyOffset: Int =
        if (formatOptions.mVersion < FormatSpec.VERSION4) headerSize else 0

    @Nonnull
    val mDictionaryOptions: DictionaryOptions = dictionaryOptions

    @Nonnull
    val mFormatOptions: FormatOptions = formatOptions

    @Nonnull
    val mLocaleString: String

    @Nonnull
    val mVersionString: String

    @Nonnull
    val mIdString: String

    init {
        val localeString =
            dictionaryOptions.mAttributes[DICTIONARY_LOCALE_KEY]
                ?: throw UnsupportedFormatException("Cannot create a FileHeader without a locale")
        val versionString =
            dictionaryOptions.mAttributes[DICTIONARY_VERSION_KEY]
                ?: throw UnsupportedFormatException(
                    "Cannot create a FileHeader without a version"
                )
        val idString =
            dictionaryOptions.mAttributes[DICTIONARY_ID_KEY]
                ?: throw UnsupportedFormatException("Cannot create a FileHeader without an ID")
        mLocaleString = localeString
        mVersionString = versionString
        mIdString = idString
    }

    val description: String?
        // Helper method to get the description
        get() =// TODO: Right now each dictionary file comes with a description in its own language.
            // It will display as is no matter the device's locale. It should be internationalized.
            mDictionaryOptions.mAttributes[DICTIONARY_DESCRIPTION_KEY]

    companion object {
        // Note that these are corresponding definitions in native code in latinime::HeaderPolicy
        // and latinime::HeaderReadWriteUtils.
        // TODO: Standardize the key names and bump up the format version, taking care not to
        // break format version 2 dictionaries.
        const val DICTIONARY_VERSION_KEY: String = "version"
        const val DICTIONARY_LOCALE_KEY: String = "locale"
        const val DICTIONARY_ID_KEY: String = "dictionary"
        const val DICTIONARY_DESCRIPTION_KEY: String = "description"
        const val DICTIONARY_DATE_KEY: String = "date"
        const val HAS_HISTORICAL_INFO_KEY: String = "HAS_HISTORICAL_INFO"
        const val USES_FORGETTING_CURVE_KEY: String = "USES_FORGETTING_CURVE"
        const val FORGETTING_CURVE_PROBABILITY_VALUES_TABLE_ID_KEY: String =
            "FORGETTING_CURVE_PROBABILITY_VALUES_TABLE_ID"
        const val MAX_UNIGRAM_COUNT_KEY: String = "MAX_UNIGRAM_ENTRY_COUNT"
        const val MAX_BIGRAM_COUNT_KEY: String = "MAX_BIGRAM_ENTRY_COUNT"
        const val MAX_TRIGRAM_COUNT_KEY: String = "MAX_TRIGRAM_ENTRY_COUNT"
        const val ATTRIBUTE_VALUE_TRUE: String = "1"
        const val CODE_POINT_TABLE_KEY: String = "codePointTable"
    }
}