/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.text.TextUtils
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.StringUtils
import com.android.inputmethod.latin.define.DebugFlags
import com.android.inputmethod.latin.define.DecoderSpecificConstants
import com.android.inputmethod.latin.settings.SettingsValuesForSuggestion
import com.android.inputmethod.latin.utils.AutoCorrectionUtils
import com.android.inputmethod.latin.utils.BinaryDictionaryUtils
import com.android.inputmethod.latin.utils.SuggestionResults
import java.util.Locale

/**
 * This class loads a dictionary and provides a list of suggestions for a given sequence of
 * characters. This includes corrections and completions.
 */
class Suggest(dictionaryFacilitator: DictionaryFacilitator) {
    private val mDictionaryFacilitator: DictionaryFacilitator

    private var mAutoCorrectionThreshold: Float = 0f
    private var mPlausibilityThreshold: Float = 0f

    init {
        mDictionaryFacilitator = dictionaryFacilitator
    }

    /**
     * Set the normalized-score threshold for a suggestion to be considered strong enough that we
     * will auto-correct to this.
     * @param threshold the threshold
     */
    fun setAutoCorrectionThreshold(threshold: Float) {
        mAutoCorrectionThreshold = threshold
    }

    /**
     * Set the normalized-score threshold for what we consider a "plausible" suggestion, in
     * the same dimension as the auto-correction threshold.
     * @param threshold the threshold
     */
    fun setPlausibilityThreshold(threshold: Float) {
        mPlausibilityThreshold = threshold
    }

    fun interface OnGetSuggestedWordsCallback {
        fun onGetSuggestedWords(suggestedWords: SuggestedWords)
    }

    fun getSuggestedWords(
        wordComposer: WordComposer,
        ngramContext: NgramContext, keyboard: Keyboard,
        settingsValuesForSuggestion: SettingsValuesForSuggestion,
        isCorrectionEnabled: Boolean, inputStyle: Int, sequenceNumber: Int,
        callback: OnGetSuggestedWordsCallback
    ) {
        if (wordComposer.isBatchMode) {
            getSuggestedWordsForBatchInput(
                wordComposer, ngramContext, keyboard,
                settingsValuesForSuggestion, inputStyle, sequenceNumber, callback
            )
        } else {
            getSuggestedWordsForNonBatchInput(
                wordComposer, ngramContext, keyboard,
                settingsValuesForSuggestion, inputStyle, isCorrectionEnabled,
                sequenceNumber, callback
            )
        }
    }

    // Retrieves suggestions for non-batch input (typing, recorrection, predictions...)
    // and calls the callback function with the suggestions.
    private fun getSuggestedWordsForNonBatchInput(
        wordComposer: WordComposer,
        ngramContext: NgramContext, keyboard: Keyboard,
        settingsValuesForSuggestion: SettingsValuesForSuggestion,
        inputStyleIfNotPrediction: Int, isCorrectionEnabled: Boolean,
        sequenceNumber: Int, callback: OnGetSuggestedWordsCallback
    ) {
        val typedWordString: String = wordComposer.typedWord
        val trailingSingleQuotesCount: Int =
            StringUtils.getTrailingSingleQuotesCount(typedWordString)
        val consideredWord: String = if (trailingSingleQuotesCount > 0)
            typedWordString.substring(0, typedWordString.length - trailingSingleQuotesCount)
        else
            typedWordString

        val suggestionResults: SuggestionResults = mDictionaryFacilitator.getSuggestionResults(
            wordComposer.composedDataSnapshot, ngramContext, keyboard,
            settingsValuesForSuggestion, SESSION_ID_TYPING, inputStyleIfNotPrediction
        )
        val locale: Locale? = mDictionaryFacilitator.locale
        val suggestionsContainer: ArrayList<SuggestedWordInfo> =
            getTransformedSuggestedWordInfoList(
                wordComposer, suggestionResults,
                trailingSingleQuotesCount, locale
            )

        var foundInDictionary: Boolean = false
        var sourceDictionaryOfRemovedWord: Dictionary? = null
        for (info: SuggestedWordInfo in suggestionsContainer) {
            // Search for the best dictionary, defined as the first one with the highest match
            // quality we can find.
            if (!foundInDictionary && typedWordString == info.word) {
                // Use this source if the old match had lower quality than this match
                sourceDictionaryOfRemovedWord = info.sourceDictionary
                foundInDictionary = true
                break
            }
        }

        val firstOcurrenceOfTypedWordInSuggestions: Int =
            SuggestedWordInfo.removeDups(typedWordString, suggestionsContainer)

        val whitelistedWordInfo: SuggestedWordInfo? =
            getWhitelistedWordInfoOrNull(suggestionsContainer)
        val whitelistedWord: String? = if (whitelistedWordInfo == null)
            null
        else
            whitelistedWordInfo.word
        val resultsArePredictions: Boolean = !wordComposer.isComposingWord

        // We allow auto-correction if whitelisting is not required or the word is whitelisted,
        // or if the word had more than one char and was not suggested.
        val allowsToBeAutoCorrected: Boolean =
            (DecoderSpecificConstants.SHOULD_AUTO_CORRECT_USING_NON_WHITE_LISTED_SUGGESTION || whitelistedWord != null)
                    || (consideredWord.length > 1 && (sourceDictionaryOfRemovedWord == null))

        val hasAutoCorrection: Boolean
        // If correction is not enabled, we never auto-correct. This is for example for when
        // the setting "Auto-correction" is "off": we still suggest, but we don't auto-correct.
        if (!isCorrectionEnabled // If the word does not allow to be auto-corrected, then we don't auto-correct.
            || !allowsToBeAutoCorrected // If we are doing prediction, then we never auto-correct of course
            || resultsArePredictions // If we don't have suggestion results, we can't evaluate the first suggestion
            // for auto-correction
            || suggestionResults.isEmpty() // If the word has digits, we never auto-correct because it's likely the word
            // was type with a lot of care
            || wordComposer.hasDigits() // If the word is mostly caps, we never auto-correct because this is almost
            // certainly intentional (and careful input)
            || wordComposer.isMostlyCaps // We never auto-correct when suggestions are resumed because it would be unexpected
            || wordComposer.isResumed // If we don't have a main dictionary, we never want to auto-correct. The reason
            // for this is, the user may have a contact whose name happens to match a valid
            // word in their language, and it will unexpectedly auto-correct. For example, if
            // the user types in English with no dictionary and has a "Will" in their contact
            // list, "will" would always auto-correct to "Will" which is unwanted. Hence, no
            // main dict => no auto-correct. Also, it would probably get obnoxious quickly.
            // TODO: now that we have personalization, we may want to re-evaluate this decision
            || !mDictionaryFacilitator.hasAtLeastOneInitializedMainDictionary() // If the first suggestion is a shortcut we never auto-correct to it, regardless
            // of how strong it is (allowlist entries are not KIND_SHORTCUT but KIND_WHITELIST).
            // TODO: we may want to have shortcut-only entries auto-correct in the future.
            || suggestionResults.first()!!.isKindOf(SuggestedWordInfo.KIND_SHORTCUT)
        ) {
            hasAutoCorrection = false
        } else {
            val firstSuggestion: SuggestedWordInfo? = suggestionResults.first()
            if (suggestionResults.mFirstSuggestionExceedsConfidenceThreshold
                && firstOcurrenceOfTypedWordInSuggestions != 0
            ) {
                hasAutoCorrection = true
            } else if (!AutoCorrectionUtils.suggestionExceedsThreshold(
                    firstSuggestion, consideredWord, mAutoCorrectionThreshold
                )
            ) {
                // Score is too low for autocorrect
                hasAutoCorrection = false
            } else {
                // We have a high score, so we need to check if this suggestion is in the correct
                // form to allow auto-correcting to it in this language. For details of how this
                // is determined, see #isAllowedByAutoCorrectionWithSpaceFilter.
                // TODO: this should not have its own logic here but be handled by the dictionary.
                hasAutoCorrection = isAllowedByAutoCorrectionWithSpaceFilter(
                    firstSuggestion!!
                )
            }
        }

        val typedWordInfo: SuggestedWordInfo = SuggestedWordInfo(
            typedWordString,
            "",  /* prevWordsContext */SuggestedWordInfo.MAX_SCORE,
            SuggestedWordInfo.KIND_TYPED,
            if (null == sourceDictionaryOfRemovedWord)
                Dictionary.DICTIONARY_USER_TYPED
            else
                sourceDictionaryOfRemovedWord,
            SuggestedWordInfo.NOT_AN_INDEX,  /* indexOfTouchPointOfSecondWord */
            SuggestedWordInfo.NOT_A_CONFIDENCE /* autoCommitFirstWordConfidence */
        )
        if (!TextUtils.isEmpty(typedWordString)) {
            suggestionsContainer.add(0, typedWordInfo)
        }

        val suggestionsList: ArrayList<SuggestedWordInfo>
        if (DBG && !suggestionsContainer.isEmpty()) {
            suggestionsList = getSuggestionsInfoListWithDebugInfo(
                typedWordString,
                suggestionsContainer
            )
        } else {
            suggestionsList = suggestionsContainer
        }

        val inputStyle: Int
        if (resultsArePredictions) {
            inputStyle = if (suggestionResults.mIsBeginningOfSentence)
                SuggestedWords.INPUT_STYLE_BEGINNING_OF_SENTENCE_PREDICTION
            else
                SuggestedWords.INPUT_STYLE_PREDICTION
        } else {
            inputStyle = inputStyleIfNotPrediction
        }

        val isTypedWordValid: Boolean = firstOcurrenceOfTypedWordInSuggestions > -1
                || (!resultsArePredictions && !allowsToBeAutoCorrected)
        callback.onGetSuggestedWords(
            SuggestedWords(
                suggestionsList,
                suggestionResults.mRawSuggestions, typedWordInfo,
                isTypedWordValid,
                hasAutoCorrection,  /* willAutoCorrect */
                false,  /* isObsoleteSuggestions */inputStyle, sequenceNumber
            )
        )
    }

    // Retrieves suggestions for the batch input
    // and calls the callback function with the suggestions.
    private fun getSuggestedWordsForBatchInput(
        wordComposer: WordComposer,
        ngramContext: NgramContext, keyboard: Keyboard,
        settingsValuesForSuggestion: SettingsValuesForSuggestion,
        inputStyle: Int, sequenceNumber: Int,
        callback: OnGetSuggestedWordsCallback
    ) {
        val suggestionResults: SuggestionResults = mDictionaryFacilitator.getSuggestionResults(
            wordComposer.composedDataSnapshot, ngramContext, keyboard,
            settingsValuesForSuggestion, SESSION_ID_GESTURE, inputStyle
        )
        // For transforming words that don't come from a dictionary, because it's our best bet
        val locale: Locale? = mDictionaryFacilitator.locale
        val suggestionsContainer: ArrayList<SuggestedWordInfo> =
            ArrayList(suggestionResults)
        val suggestionsCount: Int = suggestionsContainer.size
        val isFirstCharCapitalized: Boolean = wordComposer.wasShiftedNoLock()
        val isAllUpperCase: Boolean = wordComposer.isAllUpperCase
        if (isFirstCharCapitalized || isAllUpperCase) {
            for (i in 0 until suggestionsCount) {
                val wordInfo: SuggestedWordInfo = suggestionsContainer.get(i)
                val wordlocale: Locale? = wordInfo.sourceDictionary?.mLocale
                val transformedWordInfo: SuggestedWordInfo = getTransformedSuggestedWordInfo(
                    wordInfo, (if (null == wordlocale) locale else wordlocale)!!, isAllUpperCase,
                    isFirstCharCapitalized, 0 /* trailingSingleQuotesCount */
                )
                suggestionsContainer.set(i, transformedWordInfo)
            }
        }

        if (DecoderSpecificConstants.SHOULD_REMOVE_PREVIOUSLY_REJECTED_SUGGESTION
            && suggestionsContainer.size > 1 && TextUtils.equals(
                suggestionsContainer.get(0).word,
                wordComposer.rejectedBatchModeSuggestion
            )
        ) {
            val rejected: SuggestedWordInfo = suggestionsContainer.removeAt(0)
            suggestionsContainer.add(1, rejected)
        }
        SuggestedWordInfo.removeDups(null,  /* typedWord */suggestionsContainer)

        // For some reason some suggestions with MIN_VALUE are making their way here.
        // TODO: Find a more robust way to detect distracters.
        for (i in suggestionsContainer.indices.reversed()) {
            if (suggestionsContainer.get(i).mScore < SUPPRESS_SUGGEST_THRESHOLD) {
                suggestionsContainer.removeAt(i)
            }
        }

        // In the batch input mode, the most relevant suggested word should act as a "typed word"
        // (typedWordValid=true), not as an "auto correct word" (willAutoCorrect=false).
        // Note that because this method is never used to get predictions, there is no need to
        // modify inputType such in getSuggestedWordsForNonBatchInput.
        val pseudoTypedWordInfo: SuggestedWordInfo? = if (suggestionsContainer.isEmpty())
            null
        else
            suggestionsContainer.get(0)

        callback.onGetSuggestedWords(
            SuggestedWords(
                suggestionsContainer,
                suggestionResults.mRawSuggestions,
                pseudoTypedWordInfo,
                true,  /* typedWordValid */
                false,  /* willAutoCorrect */
                false,  /* isObsoleteSuggestions */
                inputStyle, sequenceNumber
            )
        )
    }

    companion object {
        val TAG: String = Suggest::class.java.getSimpleName()

        // Session id for
        // {@link #getSuggestedWords(WordComposer,String,ProximityInfo,boolean,int)}.
        // We are sharing the same ID between typing and gesture to save RAM footprint.
        const val SESSION_ID_TYPING: Int = 0
        const val SESSION_ID_GESTURE: Int = 0

        // Close to -2**31
        private val SUPPRESS_SUGGEST_THRESHOLD: Int = -2000000000

        private val DBG: Boolean = DebugFlags.DEBUG_ENABLED
        private const val MAXIMUM_AUTO_CORRECT_LENGTH_FOR_GERMAN: Int = 12
        private val sLanguageToMaximumAutoCorrectionWithSpaceLength: HashMap<String, Int> =
            HashMap()

        init {
            // TODO: should we add Finnish here?
            // TODO: This should not be hardcoded here but be written in the dictionary header
            sLanguageToMaximumAutoCorrectionWithSpaceLength.put(
                Locale.GERMAN.getLanguage(),
                MAXIMUM_AUTO_CORRECT_LENGTH_FOR_GERMAN
            )
        }

        private fun getTransformedSuggestedWordInfoList(
            wordComposer: WordComposer, results: SuggestionResults,
            trailingSingleQuotesCount: Int, defaultLocale: Locale?
        ): ArrayList<SuggestedWordInfo> {
            val shouldMakeSuggestionsAllUpperCase: Boolean = wordComposer.isAllUpperCase
                    && !wordComposer.isResumed
            val isOnlyFirstCharCapitalized: Boolean =
                wordComposer.isOrWillBeOnlyFirstCharCapitalized

            val suggestionsContainer: ArrayList<SuggestedWordInfo> = ArrayList(results)
            val suggestionsCount: Int = suggestionsContainer.size
            if (isOnlyFirstCharCapitalized || shouldMakeSuggestionsAllUpperCase
                || 0 != trailingSingleQuotesCount
            ) {
                for (i in 0 until suggestionsCount) {
                    val wordInfo: SuggestedWordInfo = suggestionsContainer.get(i)
                    val wordLocale: Locale? = wordInfo.sourceDictionary?.mLocale
                    val transformedWordInfo: SuggestedWordInfo = getTransformedSuggestedWordInfo(
                        wordInfo, (if (null == wordLocale) defaultLocale else wordLocale)!!,
                        shouldMakeSuggestionsAllUpperCase, isOnlyFirstCharCapitalized,
                        trailingSingleQuotesCount
                    )
                    suggestionsContainer.set(i, transformedWordInfo)
                }
            }
            return suggestionsContainer
        }

        private fun getWhitelistedWordInfoOrNull(
            suggestions: ArrayList<SuggestedWordInfo>
        ): SuggestedWordInfo? {
            if (suggestions.isEmpty()) {
                return null
            }
            val firstSuggestedWordInfo: SuggestedWordInfo = suggestions.get(0)
            if (!firstSuggestedWordInfo.isKindOf(SuggestedWordInfo.KIND_WHITELIST)) {
                return null
            }
            return firstSuggestedWordInfo
        }

        private fun getSuggestionsInfoListWithDebugInfo(
            typedWord: String, suggestions: ArrayList<SuggestedWordInfo>
        ): ArrayList<SuggestedWordInfo> {
            val typedWordInfo: SuggestedWordInfo = suggestions.get(0)
            typedWordInfo.debugString = "+"
            val suggestionsSize: Int = suggestions.size
            val suggestionsList: ArrayList<SuggestedWordInfo> = ArrayList(suggestionsSize)
            suggestionsList.add(typedWordInfo)
            // Note: i here is the index in mScores[], but the index in mSuggestions is one more
            // than i because we added the typed word to mSuggestions without touching mScores.
            for (i in 0 until suggestionsSize - 1) {
                val cur: SuggestedWordInfo = suggestions.get(i + 1)
                val normalizedScore: Float = BinaryDictionaryUtils.calcNormalizedScore(
                    typedWord, cur.toString(), cur.mScore
                )
                val scoreInfoString: String
                if (normalizedScore > 0) {
                    scoreInfoString = String.format(
                        Locale.ROOT, "%d (%4.2f), %s", cur.mScore, normalizedScore,
                        cur.sourceDictionary?.mDictType
                    )
                } else {
                    scoreInfoString = cur.mScore.toString()
                }
                cur.debugString = scoreInfoString
                suggestionsList.add(cur)
            }
            return suggestionsList
        }

        /**
         * Computes whether this suggestion should be blocked or not in this language
         *
         * This function implements a filter that avoids auto-correcting to suggestions that contain
         * spaces that are above a certain language-dependent character limit. In languages like German
         * where it's possible to concatenate many words, it often happens our dictionary does not
         * have the longer words. In this case, we offer a lot of unhelpful suggestions that contain
         * one or several spaces. Ideally we should understand what the user wants and display useful
         * suggestions by improving the dictionary and possibly having some specific logic. Until
         * that's possible we should avoid displaying unhelpful suggestions. But it's hard to tell
         * whether a suggestion is useful or not. So at least for the time being we block
         * auto-correction when the suggestion is long and contains a space, which should avoid the
         * worst damage.
         * This function is implementing that filter. If the language enforces no such limit, then it
         * always returns true. If the suggestion contains no space, it also returns true. Otherwise,
         * it checks the length against the language-specific limit.
         *
         * @param info the suggestion info
         * @return whether it's fine to auto-correct to this.
         */
        private fun isAllowedByAutoCorrectionWithSpaceFilter(info: SuggestedWordInfo): Boolean {
            val locale: Locale? = info.sourceDictionary?.mLocale
            if (null == locale) {
                return true
            }
            val maximumLengthForThisLanguage: Int? =
                sLanguageToMaximumAutoCorrectionWithSpaceLength.get(locale.getLanguage())
            if (null == maximumLengthForThisLanguage) {
                // This language does not enforce a maximum length to auto-correction
                return true
            }
            return info.word.length <= maximumLengthForThisLanguage
                    || -1 == info.word.indexOf(Constants.CODE_SPACE.toChar())
        }

        /* package for test */
        fun getTransformedSuggestedWordInfo(
            wordInfo: SuggestedWordInfo, locale: Locale, isAllUpperCase: Boolean,
            isOnlyFirstCharCapitalized: Boolean, trailingSingleQuotesCount: Int
        ): SuggestedWordInfo {
            val sb: StringBuilder = StringBuilder(wordInfo.word.length)
            if (isAllUpperCase) {
                sb.append(wordInfo.word.uppercase(locale))
            } else if (isOnlyFirstCharCapitalized) {
                sb.append(StringUtils.capitalizeFirstCodePoint(wordInfo.word, locale))
            } else {
                sb.append(wordInfo.word)
            }
            // Appending quotes is here to help people quote words. However, it's not helpful
            // when they type words with quotes toward the end like "it's" or "didn't", where
            // it's more likely the user missed the last character (or didn't type it yet).
            val quotesToAppend: Int = (trailingSingleQuotesCount
                    - (if (-1 == wordInfo.word.indexOf(Constants.CODE_SINGLE_QUOTE.toChar())) 0 else 1))
            for (i in quotesToAppend - 1 downTo 0) {
                sb.appendCodePoint(Constants.CODE_SINGLE_QUOTE)
            }
            return SuggestedWordInfo(
                sb.toString(), wordInfo.mPrevWordsContext,
                wordInfo.mScore, wordInfo.mKindAndFlags,
                wordInfo.sourceDictionary, wordInfo.mIndexOfTouchPointOfSecondWord,
                wordInfo.mAutoCommitFirstWordConfidence
            )
        }
    }
}
