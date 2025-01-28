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
package com.android.inputmethod.latin

import com.android.inputmethod.keyboard.internal.KeySpecParser
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.StringUtils

/**
 * The extended [SuggestedWords] class to represent punctuation suggestions.
 *
 * Each punctuation specification string is the key specification that can be parsed by
 * [KeySpecParser].
 */
class PunctuationSuggestions private constructor(punctuationsList: ArrayList<SuggestedWordInfo>) :
    SuggestedWords(
        punctuationsList,
        null,  /* rawSuggestions */
        null,  /* typedWord */
        false,  /* typedWordValid */
        false,  /* hasAutoCorrectionCandidate */
        false,  /* isObsoleteSuggestions */
        SuggestedWords.INPUT_STYLE_NONE,  /* inputStyle */
        SuggestedWords.NOT_A_SEQUENCE_NUMBER
    ) {
    /**
     * {@inheritDoc}
     * Note that [SuggestedWords.getWord] returns a punctuation key specification text.
     * The suggested punctuation should be gotten by parsing the key specification.
     */
    override fun getWord(index: Int): String? {
        val keySpec: String? = super.getWord(index)
        val code: Int = KeySpecParser.getCode(keySpec)
        return if ((code == Constants.CODE_OUTPUT_TEXT))
            KeySpecParser.getOutputText(keySpec)
        else
            StringUtils.newSingleCodePointString(code)
    }

    /**
     * {@inheritDoc}
     * Note that [SuggestedWords.getWord] returns a punctuation key specification text.
     * The displayed text should be gotten by parsing the key specification.
     */
    override fun getLabel(index: Int): String? {
        val keySpec: String? = super.getWord(index)
        return KeySpecParser.getLabel(keySpec)
    }

    /**
     * {@inheritDoc}
     * Note that [.getWord] returns a suggested punctuation. We should create a
     * [SuggestedWordInfo] object that represents a hard coded word.
     */
    override fun getInfo(index: Int): SuggestedWordInfo {
        return newHardCodedWordInfo(getWord(index))
    }

    override val isPunctuationSuggestions: Boolean
        /**
         * The predicator to tell whether this object represents punctuation suggestions.
         * @return true if this object represents punctuation suggestions.
         */
        get() {
            return true
        }

    override fun toString(): String {
        return ("PunctuationSuggestions: "
                + " words=" + mSuggestedWordInfoList.toTypedArray().contentToString())
    }

    companion object {
        /**
         * Create new instance of [PunctuationSuggestions] from the array of punctuation key
         * specifications.
         *
         * @param punctuationSpecs The array of punctuation key specifications.
         * @return The [PunctuationSuggestions] object.
         */
        fun newPunctuationSuggestions(
            punctuationSpecs: Array<String?>?
        ): PunctuationSuggestions {
            if (punctuationSpecs == null || punctuationSpecs.size == 0) {
                return PunctuationSuggestions(ArrayList(0))
            }
            val punctuationList: ArrayList<SuggestedWordInfo> =
                ArrayList(punctuationSpecs.size)
            for (spec: String? in punctuationSpecs) {
                punctuationList.add(newHardCodedWordInfo(spec))
            }
            return PunctuationSuggestions(punctuationList)
        }

        private fun newHardCodedWordInfo(keySpec: String?): SuggestedWordInfo {
            return SuggestedWordInfo(
                keySpec!!, "",  /* prevWordsContext */
                SuggestedWordInfo.MAX_SCORE,
                SuggestedWordInfo.KIND_HARDCODED,
                Dictionary.DICTIONARY_HARDCODED,
                SuggestedWordInfo.NOT_AN_INDEX,  /* indexOfTouchPointOfSecondWord */
                SuggestedWordInfo.NOT_A_CONFIDENCE /* autoCommitFirstWordConfidence */
            )
        }
    }
}
