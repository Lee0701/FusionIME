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
package com.android.inputmethod.latin.spellcheck

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Binder
import android.provider.UserDictionary.Words
import android.service.textservice.SpellCheckerService
import android.text.TextUtils
import android.util.Log
import android.util.LruCache
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import com.android.inputmethod.compat.SuggestionsInfoCompatUtils
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.latin.NgramContext
import com.android.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import com.android.inputmethod.latin.WordComposer
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.LocaleUtils
import com.android.inputmethod.latin.common.StringUtils
import com.android.inputmethod.latin.define.DebugFlags
import com.android.inputmethod.latin.utils.BinaryDictionaryUtils
import com.android.inputmethod.latin.utils.ScriptUtils
import com.android.inputmethod.latin.utils.StatsUtils
import com.android.inputmethod.latin.utils.SuggestionResults
import java.util.Locale
import kotlin.math.min

abstract class AndroidWordLevelSpellCheckerSession internal constructor(service: AndroidSpellCheckerService) :
    SpellCheckerService.Session() {
    // Immutable, but not available in the constructor.
    private var mLocale: Locale? = null

    // Cache this for performance
    private var mScript: Int = 0 // One of SCRIPT_LATIN or SCRIPT_CYRILLIC for now.
    private val mService: AndroidSpellCheckerService
    protected val mSuggestionsCache: SuggestionsCache = SuggestionsCache()
    private val mObserver: ContentObserver

    class SuggestionsParams(suggestions: Array<String?>, flags: Int) {
        val mSuggestions: Array<String?>
        val mFlags: Int

        init {
            mSuggestions = suggestions
            mFlags = flags
        }
    }

    protected class SuggestionsCache {
        private val mUnigramSuggestionsInfoCache: LruCache<String, SuggestionsParams> = LruCache(
            MAX_CACHE_SIZE
        )

        fun getSuggestionsFromCache(query: String): SuggestionsParams? {
            return mUnigramSuggestionsInfoCache.get(query)
        }

        fun putSuggestionsToCache(
            query: String, suggestions: Array<String?>?, flags: Int
        ) {
            if (suggestions == null || TextUtils.isEmpty(query)) {
                return
            }
            mUnigramSuggestionsInfoCache.put(
                generateKey(query),
                SuggestionsParams(suggestions, flags)
            )
        }

        fun clearCache() {
            mUnigramSuggestionsInfoCache.evictAll()
        }

        companion object {
            private const val MAX_CACHE_SIZE: Int = 50
            private fun generateKey(query: String): String {
                return query + ""
            }
        }
    }

    override fun onCreate() {
        val localeString: String? = getLocale()
        mLocale = if ((null == localeString))
            null
        else
            LocaleUtils.constructLocaleFromString(localeString)
        mScript = ScriptUtils.getScriptFromSpellCheckerLocale(mLocale!!)
    }

    override fun onClose() {
        val cres: ContentResolver = mService.getContentResolver()
        cres.unregisterContentObserver(mObserver)
    }

    init {
        mService = service
        val cres: ContentResolver = service.getContentResolver()

        mObserver = object : ContentObserver(null) {
            override fun onChange(self: Boolean) {
                mSuggestionsCache.clearCache()
            }
        }
        cres.registerContentObserver(Words.CONTENT_URI, true, mObserver)
    }

    /**
     * Helper method to test valid capitalizations of a word.
     *
     * If the "text" is lower-case, we test only the exact string.
     * If the "Text" is capitalized, we test the exact string "Text" and the lower-cased
     * version of it "text".
     * If the "TEXT" is fully upper case, we test the exact string "TEXT", the lower-cased
     * version of it "text" and the capitalized version of it "Text".
     */
    private fun isInDictForAnyCapitalization(text: String, capitalizeType: Int): Boolean {
        // If the word is in there as is, then it's in the dictionary. If not, we'll test lower
        // case versions, but only if the word is not already all-lower case or mixed case.
        if (mService.isValidWord(mLocale, text)) return true
        if (StringUtils.CAPITALIZE_NONE == capitalizeType) return false

        // If we come here, we have a capitalized word (either First- or All-).
        // Downcase the word and look it up again. If the word is only capitalized, we
        // tested all possibilities, so if it's still negative we can return false.
        val lowerCaseText: String = text.lowercase(mLocale!!)
        if (mService.isValidWord(mLocale, lowerCaseText)) return true
        if (StringUtils.CAPITALIZE_FIRST == capitalizeType) return false

        // If the lower case version is not in the dictionary, it's still possible
        // that we have an all-caps version of a word that needs to be capitalized
        // according to the dictionary. E.g. "GERMANS" only exists in the dictionary as "Germans".
        return mService.isValidWord(
            mLocale,
            StringUtils.capitalizeFirstAndDowncaseRest(lowerCaseText, mLocale!!)
        )
    }

    // Note : this must be reentrant
    /**
     * Gets a list of suggestions for a specific string. This returns a list of possible
     * corrections for the text passed as an argument. It may split or group words, and
     * even perform grammatical analysis.
     */
    private fun onGetSuggestionsInternal(
        textInfo: TextInfo,
        suggestionsLimit: Int
    ): SuggestionsInfo {
        return onGetSuggestionsInternal(textInfo, null, suggestionsLimit)
    }

    protected fun onGetSuggestionsInternal(
        textInfo: TextInfo, ngramContext: NgramContext?, suggestionsLimit: Int
    ): SuggestionsInfo {
        try {
            val text: String = textInfo.getText()
                .replace(AndroidSpellCheckerService.APOSTROPHE.toRegex(), AndroidSpellCheckerService.SINGLE_QUOTE)
                .replace(("^" + quotesRegexp).toRegex(), "")
                .replace((quotesRegexp + "$").toRegex(), "")

            if (!mService.hasMainDictionaryForLocale(mLocale)) {
                return AndroidSpellCheckerService.getNotInDictEmptySuggestions(
                    false /* reportAsTypo */
                )
            }

            // Handle special patterns like email, URI, telephone number.
            val checkability: Int = getCheckabilityInScript(text, mScript)
            if (CHECKABILITY_CHECKABLE != checkability) {
                if (CHECKABILITY_CONTAINS_PERIOD == checkability) {
                    val splitText: Array<String> =
                        text.split(Constants.REGEXP_PERIOD.toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    var allWordsAreValid = true
                    for (word: String in splitText) {
                        if (!mService.isValidWord(mLocale, word)) {
                            allWordsAreValid = false
                            break
                        }
                    }
                    if (allWordsAreValid) {
                        return SuggestionsInfo(
                            SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
                                    or SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS,
                            arrayOf(TextUtils.join(Constants.STRING_SPACE, splitText))
                        )
                    }
                }
                return if (mService.isValidWord(
                        mLocale,
                        text
                    )
                ) AndroidSpellCheckerService.inDictEmptySuggestions else AndroidSpellCheckerService.getNotInDictEmptySuggestions(
                    CHECKABILITY_CONTAINS_PERIOD == checkability /* reportAsTypo */
                )
            }

            // Handle normal words.
            val capitalizeType: Int = StringUtils.getCapitalizationType(text)

            if (isInDictForAnyCapitalization(text, capitalizeType)) {
                if (DebugFlags.DEBUG_ENABLED) {
                    Log.i(TAG, "onGetSuggestionsInternal() : [" + text + "] is a valid word")
                }
                return AndroidSpellCheckerService.inDictEmptySuggestions
            }
            if (DebugFlags.DEBUG_ENABLED) {
                Log.i(TAG, "onGetSuggestionsInternal() : [" + text + "] is NOT a valid word")
            }

            val keyboard: Keyboard = mService.getKeyboardForLocale(
                mLocale!!
            )
            if (null == keyboard) {
                Log.w(TAG, "onGetSuggestionsInternal() : No keyboard for locale: " + mLocale)
                // If there is no keyboard for this locale, don't do any spell-checking.
                return AndroidSpellCheckerService.getNotInDictEmptySuggestions(
                    false /* reportAsTypo */
                )
            }

            val composer: WordComposer = WordComposer()
            val codePoints: IntArray = StringUtils.toCodePointArray(text)
            val coordinates: IntArray
            coordinates = keyboard.getCoordinates(codePoints)
            composer.setComposingWord(codePoints, coordinates)
            // TODO: Don't gather suggestions if the limit is <= 0 unless necessary
            val suggestionResults: SuggestionResults = mService.getSuggestionResults(
                mLocale, composer.composedDataSnapshot, ngramContext!!, keyboard
            )
            val result: Result = getResult(
                capitalizeType,
                mLocale!!, suggestionsLimit,
                mService.recommendedThreshold, text, suggestionResults
            )
            if (DebugFlags.DEBUG_ENABLED) {
                if (result.mSuggestions != null && result.mSuggestions.size > 0) {
                    val builder: StringBuilder = StringBuilder()
                    for (suggestion: String? in result.mSuggestions) {
                        builder.append(" [")
                        builder.append(suggestion)
                        builder.append("]")
                    }
                    Log.i(TAG, "onGetSuggestionsInternal() : Suggestions =" + builder)
                }
            }
            // Handle word not in dictionary.
            // This is called only once per unique word, so entering multiple
            // instances of the same word does not result in more than one call
            // to this method.
            // Also, upon changing the orientation of the device, this is called
            // again for every unique invalid word in the text box.
            StatsUtils.onInvalidWordIdentification(text)

            val flags: Int =
                (SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
                        or (if (result.mHasRecommendedSuggestions)
                    SuggestionsInfoCompatUtils.getValueOf_RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS()
                else
                    0))
            val retval: SuggestionsInfo = SuggestionsInfo(flags, result.mSuggestions)
            mSuggestionsCache.putSuggestionsToCache(text, result.mSuggestions, flags)
            return retval
        } catch (e: RuntimeException) {
            // Don't kill the keyboard if there is a bug in the spell checker
            Log.e(TAG, "Exception while spellchecking", e)
            return AndroidSpellCheckerService.getNotInDictEmptySuggestions(
                false /* reportAsTypo */
            )
        }
    }

    private class Result(gatheredSuggestions: Array<String?>?, hasRecommendedSuggestions: Boolean) {
        val mSuggestions: Array<String?>?
        val mHasRecommendedSuggestions: Boolean

        init {
            mSuggestions = gatheredSuggestions
            mHasRecommendedSuggestions = hasRecommendedSuggestions
        }
    }

    /*
     * The spell checker acts on its own behalf. That is needed, in particular, to be able to
     * access the dictionary files, which the provider restricts to the identity of Latin IME.
     * Since it's called externally by the application, the spell checker is using the identity
     * of the application by default unless we clearCallingIdentity.
     * That's what the following method does.
     */
    override fun onGetSuggestions(textInfo: TextInfo, suggestionsLimit: Int): SuggestionsInfo {
        val ident: Long = Binder.clearCallingIdentity()
        try {
            return onGetSuggestionsInternal(textInfo, suggestionsLimit)
        } finally {
            Binder.restoreCallingIdentity(ident)
        }
    }

    companion object {
        private val TAG: String = AndroidWordLevelSpellCheckerSession::class.java.getSimpleName()

        val EMPTY_STRING_ARRAY: Array<String?> = arrayOfNulls(0)

        private const val quotesRegexp: String =
            "(\\u0022|\\u0027|\\u0060|\\u00B4|\\u2018|\\u2018|\\u201C|\\u201D)"

        private const val CHECKABILITY_CHECKABLE: Int = 0
        private const val CHECKABILITY_TOO_MANY_NON_LETTERS: Int = 1
        private const val CHECKABILITY_CONTAINS_PERIOD: Int = 2
        private const val CHECKABILITY_EMAIL_OR_URL: Int = 3
        private const val CHECKABILITY_FIRST_LETTER_UNCHECKABLE: Int = 4
        private const val CHECKABILITY_TOO_SHORT: Int = 5

        /**
         * Finds out whether a particular string should be filtered out of spell checking.
         *
         * This will loosely match URLs, numbers, symbols. To avoid always underlining words that
         * we know we will never recognize, this accepts a script identifier that should be one
         * of the SCRIPT_* constants defined above, to rule out quickly characters from very
         * different languages.
         *
         * @param text the string to evaluate.
         * @param script the identifier for the script this spell checker recognizes
         * @return one of the FILTER_OUT_* constants above.
         */
        private fun getCheckabilityInScript(text: String, script: Int): Int {
            if (TextUtils.isEmpty(text) || text.length <= 1) return CHECKABILITY_TOO_SHORT

            // TODO: check if an equivalent processing can't be done more quickly with a
            // compiled regexp.
            // Filter by first letter
            val firstCodePoint: Int = text.codePointAt(0)
            // Filter out words that don't start with a letter or an apostrophe
            if (!ScriptUtils.isLetterPartOfScript(firstCodePoint, script)
                && '\''.code != firstCodePoint
            ) return CHECKABILITY_FIRST_LETTER_UNCHECKABLE

            // Filter contents
            val length: Int = text.length
            var letterCount: Int = 0
            var i: Int = 0
            while (i < length) {
                val codePoint: Int = text.codePointAt(i)
                // Any word containing a COMMERCIAL_AT is probably an e-mail address
                // Any word containing a SLASH is probably either an ad-hoc combination of two
                // words or a URI - in either case we don't want to spell check that
                if (Constants.CODE_COMMERCIAL_AT == codePoint || Constants.CODE_SLASH == codePoint) {
                    return CHECKABILITY_EMAIL_OR_URL
                }
                // If the string contains a period, native returns strange suggestions (it seems
                // to return suggestions for everything up to the period only and to ignore the
                // rest), so we suppress lookup if there is a period.
                // TODO: investigate why native returns these suggestions and remove this code.
                if (Constants.CODE_PERIOD == codePoint) {
                    return CHECKABILITY_CONTAINS_PERIOD
                }
                if (ScriptUtils.isLetterPartOfScript(codePoint, script)) ++letterCount
                i = text.offsetByCodePoints(i, 1)
            }
            // Guestimate heuristic: perform spell checking if at least 3/4 of the characters
            // in this word are letters
            return if ((letterCount * 4 < length * 3))
                CHECKABILITY_TOO_MANY_NON_LETTERS
            else
                CHECKABILITY_CHECKABLE
        }

        private fun getResult(
            capitalizeType: Int, locale: Locale,
            suggestionsLimit: Int, recommendedThreshold: Float, originalText: String,
            suggestionResults: SuggestionResults
        ): Result {
            if (suggestionResults.isEmpty() || suggestionsLimit <= 0) {
                return Result(
                    null,  /* gatheredSuggestions */
                    false /* hasRecommendedSuggestions */
                )
            }
            val suggestions: ArrayList<String?> = ArrayList()
            for (suggestedWordInfo: SuggestedWordInfo in suggestionResults) {
                val suggestion: String?
                if (StringUtils.CAPITALIZE_ALL == capitalizeType) {
                    suggestion = suggestedWordInfo.word.uppercase(locale)
                } else if (StringUtils.CAPITALIZE_FIRST == capitalizeType) {
                    suggestion = StringUtils.capitalizeFirstCodePoint(
                        suggestedWordInfo.word, locale
                    )
                } else {
                    suggestion = suggestedWordInfo.word
                }
                suggestions.add(suggestion)
            }
            StringUtils.removeDupes(suggestions)
            // This returns a String[], while toArray() returns an Object[] which cannot be cast
            // into a String[].
            val gatheredSuggestionsList: List<String?> =
                suggestions.subList(
                    0,
                    min(suggestions.size.toDouble(), suggestionsLimit.toDouble()).toInt()
                )
            val gatheredSuggestions: Array<String?> =
                gatheredSuggestionsList.toTypedArray<String?>()

            val bestScore: Int = suggestionResults.first()!!.mScore
            val bestSuggestion: String = suggestions.get(0)!!
            val normalizedScore: Float = BinaryDictionaryUtils.calcNormalizedScore(
                originalText, bestSuggestion, bestScore
            )
            val hasRecommendedSuggestions: Boolean = (normalizedScore > recommendedThreshold)
            return Result(gatheredSuggestions, hasRecommendedSuggestions)
        }
    }
}
