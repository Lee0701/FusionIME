/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.view.inputmethod.CompletionInfo
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.latin.common.StringUtils
import com.android.inputmethod.latin.define.DebugFlags

open class SuggestedWords(
    suggestedWordInfoList: ArrayList<SuggestedWordInfo>,
    rawSuggestions: ArrayList<SuggestedWordInfo>?,
    typedWordInfo: SuggestedWordInfo?,
    typedWordValid: Boolean,
    willAutoCorrect: Boolean,
    isObsoleteSuggestions: Boolean,
    inputStyle: Int,
    sequenceNumber: Int
) {
    /**
     * Get [SuggestedWordInfo] object for the typed word.
     * @return The [SuggestedWordInfo] object for the typed word.
     */
    val typedWordInfo: SuggestedWordInfo?
    val mTypedWordValid: Boolean

    // Note: this INCLUDES cases where the word will auto-correct to itself. A good definition
    // of what this flag means would be "the top suggestion is strong enough to auto-correct",
    // whether this exactly matches the user entry or not.
    val mWillAutoCorrect: Boolean
    val mIsObsoleteSuggestions: Boolean

    // How the input for these suggested words was done by the user. Must be one of the
    // INPUT_STYLE_* constants above.
    val mInputStyle: Int
    val mSequenceNumber: Int // Sequence number for auto-commit.

    protected val mSuggestedWordInfoList: ArrayList<SuggestedWordInfo>
    val mRawSuggestions: ArrayList<SuggestedWordInfo>?

    init {
        mSuggestedWordInfoList = suggestedWordInfoList
        mRawSuggestions = rawSuggestions
        mTypedWordValid = typedWordValid
        mWillAutoCorrect = willAutoCorrect
        mIsObsoleteSuggestions = isObsoleteSuggestions
        mInputStyle = inputStyle
        mSequenceNumber = sequenceNumber
        this.typedWordInfo = typedWordInfo
    }

    val isEmpty: Boolean
        get() {
            return mSuggestedWordInfoList.isEmpty()
        }

    fun size(): Int {
        return mSuggestedWordInfoList.size
    }

    /**
     * Get suggested word to show as suggestions to UI.
     *
     * @param shouldShowLxxSuggestionUi true if showing suggestion UI introduced in LXX and later.
     * @return the count of suggested word to show as suggestions to UI.
     */
    fun getWordCountToShow(shouldShowLxxSuggestionUi: Boolean): Int {
        if (isPrediction || !shouldShowLxxSuggestionUi) {
            return size()
        }
        return size() -  /* typed word */1
    }

    /**
     * Get suggested word at `index`.
     * @param index The index of the suggested word.
     * @return The suggested word.
     */
    open fun getWord(index: Int): String? {
        return mSuggestedWordInfoList.get(index).word
    }

    /**
     * Get displayed text at `index`.
     * In RTL languages, the displayed text on the suggestion strip may be different from the
     * suggested word that is returned from [.getWord]. For example the displayed text
     * of punctuation suggestion "(" should be ")".
     * @param index The index of the text to display.
     * @return The text to be displayed.
     */
    open fun getLabel(index: Int): String? {
        return mSuggestedWordInfoList.get(index).word
    }

    /**
     * Get [SuggestedWordInfo] object at `index`.
     * @param index The index of the [SuggestedWordInfo].
     * @return The [SuggestedWordInfo] object.
     */
    open fun getInfo(index: Int): SuggestedWordInfo {
        return mSuggestedWordInfoList.get(index)
    }

    /**
     * Gets the suggestion index from the suggestions list.
     * @param suggestedWordInfo The [SuggestedWordInfo] to find the index.
     * @return The position of the suggestion in the suggestion list.
     */
    fun indexOf(suggestedWordInfo: SuggestedWordInfo): Int {
        return mSuggestedWordInfoList.indexOf(suggestedWordInfo)
    }

    fun getDebugString(pos: Int): String? {
        if (!DebugFlags.DEBUG_ENABLED) {
            return null
        }
        val wordInfo: SuggestedWordInfo? = getInfo(pos)
        if (wordInfo == null) {
            return null
        }
        val debugString: String = wordInfo.debugString
        if (TextUtils.isEmpty(debugString)) {
            return null
        }
        return debugString
    }

    open val isPunctuationSuggestions: Boolean
        /**
         * The predicator to tell whether this object represents punctuation suggestions.
         * @return false if this object desn't represent punctuation suggestions.
         */
        get() {
            return false
        }

    override fun toString(): String {
        // Pretty-print method to help debug
        return ("SuggestedWords:"
                + " mTypedWordValid=" + mTypedWordValid
                + " mWillAutoCorrect=" + mWillAutoCorrect
                + " mInputStyle=" + mInputStyle
                + " words=" + mSuggestedWordInfoList.toTypedArray().contentToString())
    }

    val autoCommitCandidate: SuggestedWordInfo?
        get() {
            if (mSuggestedWordInfoList.size <= 0) return null
            val candidate: SuggestedWordInfo = mSuggestedWordInfoList.get(0)
            return if (candidate.isEligibleForAutoCommit) candidate else null
        }

    // non-final for testability.
    class SuggestedWordInfo {
        val word: String
        val mPrevWordsContext: String?

        // The completion info from the application. Null for suggestions that don't come from
        // the application (including keyboard-computed ones, so this is almost always null)
        val mApplicationSpecifiedCompletionInfo: CompletionInfo?
        val mScore: Int
        val mKindAndFlags: Int
        val mCodePointCount: Int

        @get:Deprecated("")
        @Deprecated("")
        val sourceDictionary: Dictionary?

        // For auto-commit. This keeps track of the index inside the touch coordinates array
        // passed to native code to get suggestions for a gesture that corresponds to the first
        // letter of the second word.
        val mIndexOfTouchPointOfSecondWord: Int

        // For auto-commit. This is a measure of how confident we are that we can commit the
        // first word of this suggestion.
        val mAutoCommitFirstWordConfidence: Int
        private var mDebugString: String = ""

        /**
         * Create a new suggested word info.
         * @param word The string to suggest.
         * @param prevWordsContext previous words context.
         * @param score A measure of how likely this suggestion is.
         * @param kindAndFlags The kind of suggestion, as one of the above KIND_* constants with
         * flags.
         * @param sourceDict What instance of Dictionary produced this suggestion.
         * @param indexOfTouchPointOfSecondWord See mIndexOfTouchPointOfSecondWord.
         * @param autoCommitFirstWordConfidence See mAutoCommitFirstWordConfidence.
         */
        constructor(
            word: String, prevWordsContext: String?,
            score: Int, kindAndFlags: Int,
            sourceDict: Dictionary?, indexOfTouchPointOfSecondWord: Int,
            autoCommitFirstWordConfidence: Int
        ) {
            this.word = word
            mPrevWordsContext = prevWordsContext
            mApplicationSpecifiedCompletionInfo = null
            mScore = score
            mKindAndFlags = kindAndFlags
            sourceDictionary = sourceDict
            mCodePointCount = StringUtils.codePointCount(this.word)
            mIndexOfTouchPointOfSecondWord = indexOfTouchPointOfSecondWord
            mAutoCommitFirstWordConfidence = autoCommitFirstWordConfidence
        }

        /**
         * Create a new suggested word info from an application-specified completion.
         * If the passed argument or its contained text is null, this throws a NPE.
         * @param applicationSpecifiedCompletion The application-specified completion info.
         */
        constructor(applicationSpecifiedCompletion: CompletionInfo) {
            word = applicationSpecifiedCompletion.getText().toString()
            mPrevWordsContext = ""
            mApplicationSpecifiedCompletionInfo = applicationSpecifiedCompletion
            mScore = MAX_SCORE
            mKindAndFlags = KIND_APP_DEFINED
            sourceDictionary = Dictionary.DICTIONARY_APPLICATION_DEFINED
            mCodePointCount = StringUtils.codePointCount(word)
            mIndexOfTouchPointOfSecondWord = NOT_AN_INDEX
            mAutoCommitFirstWordConfidence = NOT_A_CONFIDENCE
        }

        val isEligibleForAutoCommit: Boolean
            get() {
                return (isKindOf(KIND_CORRECTION) && NOT_AN_INDEX != mIndexOfTouchPointOfSecondWord)
            }

        val kind: Int
            get() {
                return (mKindAndFlags and KIND_MASK_KIND)
            }

        fun isKindOf(kind: Int): Boolean {
            return this.kind == kind
        }

        val isPossiblyOffensive: Boolean
            get() {
                return (mKindAndFlags and KIND_FLAG_POSSIBLY_OFFENSIVE) != 0
            }

        val isExactMatch: Boolean
            get() {
                return (mKindAndFlags and KIND_FLAG_EXACT_MATCH) != 0
            }

        val isExactMatchWithIntentionalOmission: Boolean
            get() {
                return (mKindAndFlags and KIND_FLAG_EXACT_MATCH_WITH_INTENTIONAL_OMISSION) != 0
            }

        val isAprapreateForAutoCorrection: Boolean
            get() {
                return (mKindAndFlags and KIND_FLAG_APPROPRIATE_FOR_AUTO_CORRECTION) != 0
            }

        var debugString: String
            get() {
                return mDebugString
            }
            set(str) {
                if (null == str) throw NullPointerException("Debug info is null")
                mDebugString = str
            }

        fun codePointAt(i: Int): Int {
            return word.codePointAt(i)
        }

        override fun toString(): String {
            if (TextUtils.isEmpty(mDebugString)) {
                return word
            }
            return word + " (" + mDebugString + ")"
        }

        companion object {
            val NOT_AN_INDEX: Int = -1
            val NOT_A_CONFIDENCE: Int = -1
            val MAX_SCORE: Int = Int.MAX_VALUE

            private const val KIND_MASK_KIND: Int = 0xFF // Mask to get only the kind
            const val KIND_TYPED: Int = 0 // What user typed
            const val KIND_CORRECTION: Int = 1 // Simple correction/suggestion
            const val KIND_COMPLETION: Int = 2 // Completion (suggestion with appended chars)
            const val KIND_WHITELIST: Int = 3 // Whitelisted word
            const val KIND_BLACKLIST: Int = 4 // Blacklisted word
            const val KIND_HARDCODED: Int = 5 // Hardcoded suggestion, e.g. punctuation
            const val KIND_APP_DEFINED: Int = 6 // Suggested by the application
            const val KIND_SHORTCUT: Int = 7 // A shortcut
            const val KIND_PREDICTION: Int = 8 // A prediction (== a suggestion with no input)

            // KIND_RESUMED: A resumed suggestion (comes from a span, currently this type is used only
            // in java for re-correction)
            const val KIND_RESUMED: Int = 9
            const val KIND_OOV_CORRECTION: Int = 10 // Most probable string correction

            const val KIND_FLAG_POSSIBLY_OFFENSIVE: Int = -0x80000000
            const val KIND_FLAG_EXACT_MATCH: Int = 0x40000000
            const val KIND_FLAG_EXACT_MATCH_WITH_INTENTIONAL_OMISSION: Int = 0x20000000
            const val KIND_FLAG_APPROPRIATE_FOR_AUTO_CORRECTION: Int = 0x10000000

            /**
             * This will always remove the higher index if a duplicate is found.
             *
             * @return position of typed word in the candidate list
             */
            fun removeDups(
                typedWord: String?,
                candidates: ArrayList<SuggestedWordInfo>
            ): Int {
                if (candidates.isEmpty()) {
                    return -1
                }
                var firstOccurrenceOfWord: Int = -1
                if (typedWord != null && !TextUtils.isEmpty(typedWord)) {
                    firstOccurrenceOfWord = removeSuggestedWordInfoFromList(
                        typedWord, candidates, -1 /* startIndexExclusive */
                    )
                }
                candidates.indices.reversed().forEach { i ->
                    removeSuggestedWordInfoFromList(
                        candidates[i].word, candidates, i /* startIndexExclusive */
                    )
                }
                return firstOccurrenceOfWord
            }

            private fun removeSuggestedWordInfoFromList(
                word: String,
                candidates: ArrayList<SuggestedWordInfo>,
                startIndexExclusive: Int
            ): Int {
                var firstOccurrenceOfWord: Int = -1
                var i: Int = startIndexExclusive + 1
                while (i < candidates.size) {
                    val previous: SuggestedWordInfo = candidates.get(i)
                    if (word == previous.word) {
                        if (firstOccurrenceOfWord == -1) {
                            firstOccurrenceOfWord = i
                        }
                        candidates.removeAt(i)
                        --i
                    }
                    ++i
                }
                return firstOccurrenceOfWord
            }
        }
    }

    val isPrediction: Boolean
        get() {
            return isPrediction(mInputStyle)
        }

    @get:UsedForTesting
    val typedWordInfoOrNull: SuggestedWordInfo?
        /**
         * @return the [SuggestedWordInfo] which corresponds to the word that is originally
         * typed by the user. Otherwise returns `null`. Note that gesture input is not
         * considered to be a typed word.
         */
        get() {
            if (INDEX_OF_TYPED_WORD >= size()) {
                return null
            }
            val info: SuggestedWordInfo = getInfo(INDEX_OF_TYPED_WORD)
            return if ((info.kind == SuggestedWordInfo.KIND_TYPED)) info else null
        }

    companion object {
        const val INDEX_OF_TYPED_WORD: Int = 0
        const val INDEX_OF_AUTO_CORRECTION: Int = 1
        val NOT_A_SEQUENCE_NUMBER: Int = -1

        const val INPUT_STYLE_NONE: Int = 0
        const val INPUT_STYLE_TYPING: Int = 1
        const val INPUT_STYLE_UPDATE_BATCH: Int = 2
        const val INPUT_STYLE_TAIL_BATCH: Int = 3
        const val INPUT_STYLE_APPLICATION_SPECIFIED: Int = 4
        const val INPUT_STYLE_RECORRECTION: Int = 5
        const val INPUT_STYLE_PREDICTION: Int = 6
        const val INPUT_STYLE_BEGINNING_OF_SENTENCE_PREDICTION: Int = 7

        // The maximum number of suggestions available.
        const val MAX_SUGGESTIONS: Int = 18

        private val EMPTY_WORD_INFO_LIST: ArrayList<SuggestedWordInfo> = ArrayList(0)

        val emptyInstance: SuggestedWords = SuggestedWords(
            EMPTY_WORD_INFO_LIST, null,  /* rawSuggestions */null,  /* typedWord */
            false,  /* typedWordValid */false,  /* willAutoCorrect */
            false,  /* isObsoleteSuggestions */INPUT_STYLE_NONE, NOT_A_SEQUENCE_NUMBER
        )

        fun getFromApplicationSpecifiedCompletions(
            infos: Array<CompletionInfo>
        ): ArrayList<SuggestedWordInfo> {
            val result: ArrayList<SuggestedWordInfo> = ArrayList()
            for (info: CompletionInfo? in infos) {
                if (null == info || null == info.getText()) {
                    continue
                }
                result.add(SuggestedWordInfo(info))
            }
            return result
        }

        // Should get rid of the first one (what the user typed previously) from suggestions
        // and replace it with what the user currently typed.
        fun getTypedWordAndPreviousSuggestions(
            typedWordInfo: SuggestedWordInfo,
            previousSuggestions: SuggestedWords
        ): ArrayList<SuggestedWordInfo> {
            val suggestionsList: ArrayList<SuggestedWordInfo> = ArrayList()
            val alreadySeen: HashSet<String> = HashSet()
            suggestionsList.add(typedWordInfo)
            alreadySeen.add(typedWordInfo.word)
            val previousSize: Int = previousSuggestions.size()
            for (index in 1 until previousSize) {
                val prevWordInfo: SuggestedWordInfo = previousSuggestions.getInfo(index)
                val prevWord: String = prevWordInfo.word
                // Filter out duplicate suggestions.
                if (!alreadySeen.contains(prevWord)) {
                    suggestionsList.add(prevWordInfo)
                    alreadySeen.add(prevWord)
                }
            }
            return suggestionsList
        }

        private fun isPrediction(inputStyle: Int): Boolean {
            return INPUT_STYLE_PREDICTION == inputStyle
                    || INPUT_STYLE_BEGINNING_OF_SENTENCE_PREDICTION == inputStyle
        }
    }
}
