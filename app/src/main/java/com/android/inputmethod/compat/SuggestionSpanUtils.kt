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
package com.android.inputmethod.compat

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.SuggestionSpan
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.latin.SuggestedWords
import com.android.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import com.android.inputmethod.latin.common.LocaleUtils
import com.android.inputmethod.latin.define.DebugFlags
import java.lang.reflect.Field
import java.util.Locale
import javax.annotation.Nonnull

object SuggestionSpanUtils {
    // Note that SuggestionSpan.FLAG_AUTO_CORRECTION has been introduced
    // in API level 15 (Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1).
    private val FIELD_FLAG_AUTO_CORRECTION: Field? = CompatUtils.getField(
        SuggestionSpan::class.java, "FLAG_AUTO_CORRECTION"
    )
    private val OBJ_FLAG_AUTO_CORRECTION: Int? = CompatUtils.getFieldValue(
        null,  /* receiver */null,  /* defaultValue */FIELD_FLAG_AUTO_CORRECTION
    ) as Int?

    init {
        if (DebugFlags.DEBUG_ENABLED) {
            if (OBJ_FLAG_AUTO_CORRECTION == null) {
                throw RuntimeException("Field is accidentially null.")
            }
        }
    }

    @UsedForTesting
    fun getTextWithAutoCorrectionIndicatorUnderline(
        context: Context?, text: String, @Nonnull locale: Locale?
    ): CharSequence? {
        if (TextUtils.isEmpty(text) || OBJ_FLAG_AUTO_CORRECTION == null) {
            return text
        }
        val spannable: Spannable = SpannableString(text)
        val suggestionSpan: SuggestionSpan = SuggestionSpan(
            context, locale,
            arrayOf(),  /* suggestions */OBJ_FLAG_AUTO_CORRECTION, null
        )
        spannable.setSpan(
            suggestionSpan, 0, text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE or Spanned.SPAN_COMPOSING
        )
        return spannable
    }

    @UsedForTesting
    fun getTextWithSuggestionSpan(
        context: Context?,
        pickedWord: String, suggestedWords: SuggestedWords, locale: Locale?
    ): CharSequence? {
        if (TextUtils.isEmpty(pickedWord) || suggestedWords.isEmpty()
            || suggestedWords.isPrediction() || suggestedWords.isPunctuationSuggestions()
        ) {
            return pickedWord
        }

        val suggestionsList: ArrayList<String> = ArrayList()
        for (i in 0 until suggestedWords.size()) {
            if (suggestionsList.size >= SuggestionSpan.SUGGESTIONS_MAX_SIZE) {
                break
            }
            val info: SuggestedWordInfo? = suggestedWords.getInfo(i)
            if (info!!.isKindOf(SuggestedWordInfo.Companion.KIND_PREDICTION)) {
                continue
            }
            val word: String? = suggestedWords.getWord(i)
            if (!TextUtils.equals(pickedWord, word)) {
                suggestionsList.add(word.toString())
            }
        }
        val suggestionSpan: SuggestionSpan = SuggestionSpan(
            context, locale,
            suggestionsList.toTypedArray<String>(), 0,  /* flags */null
        )
        val spannable: Spannable = SpannableString(pickedWord)
        spannable.setSpan(suggestionSpan, 0, pickedWord.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannable
    }

    /**
     * Returns first [Locale] found in the given array of [SuggestionSpan].
     * @param suggestionSpans the array of [SuggestionSpan] to be examined.
     * @return the first [Locale] found in `suggestionSpans`. `null` when not
     * found.
     */
    @UsedForTesting
    fun findFirstLocaleFromSuggestionSpans(
        suggestionSpans: Array<SuggestionSpan>
    ): Locale? {
        for (suggestionSpan: SuggestionSpan in suggestionSpans) {
            val localeString: String = suggestionSpan.getLocale()
            if (TextUtils.isEmpty(localeString)) {
                continue
            }
            return LocaleUtils.constructLocaleFromString(localeString)
        }
        return null
    }
}
