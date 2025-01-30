/*
 * Copyright (C) 2008-2012  OMRON SOFTWARE Co., Ltd.
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
package jp.co.omronsoft.openwnn

import android.content.SharedPreferences

/**
 * The interface of the text converter accessed from OpenWnn.
 * <br></br>
 * The realization class of this interface should be an singleton class.
 *
 * @author Copyright (C) 2009, OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
interface WnnEngine {
    /*
     * DEFINITION OF METHODS
     */
    /**
     * Initialize parameters.
     */
    fun init()

    /**
     * Close the converter.
     * <br></br>
     *
     * OpenWnn calls this method when it is destroyed.
     */
    fun close()

    /**
     * Predict words/phrases.
     * <br></br>
     * @param text      The input string
     * @param minLen    The minimum length of a word to predict (0  : no limit)
     * @param maxLen    The maximum length of a word to predict (-1 : no limit)
     * @return          Plus value if there are candidates; 0 if there is no candidate; minus value if a error occurs.
     */
    fun predict(text: ComposingText?, minLen: Int, maxLen: Int): Int

    /**
     * Convert a string.
     * <br></br>
     * This method is used to consecutive/single clause convert in
     * Japanese, Pinyin to Kanji convert in Chinese, Hangul to Hanja
     * convert in Korean, etc.
     *
     * The result of conversion is set into the layer 2 in the [ComposingText].
     * To get other candidates of each clause, call [.makeCandidateListOf].
     *
     * @param text      The input string
     * @return      Plus value if there are candidates; 0 if there is no candidate; minus value if a error occurs.
     */
    fun convert(text: ComposingText?): Int

    /**
     * Search words from the dictionaries.
     * <br></br>
     * @param key       The search key (stroke)
     * @return      Plus value if there are candidates; 0 if there is no candidate; minus value if a error occurs.
     */
    fun searchWords(key: String?): Int

    /**
     * Search words from the dictionaries.
     * <br></br>
     * @param word      A word to search
     * @return          Plus value if there are candidates; 0 if there is no candidate; minus value if a error occurs.
     */
    fun searchWords(word: WnnWord?): Int

    /**
     * Get a candidate.
     * <br></br>
     * After [.predict] or [.makeCandidateListOf] or
     * `searchWords()`, call this method to get the
     * results.  This method will return a candidate in decreasing
     * frequency order for [.predict] and
     * [.makeCandidateListOf], in increasing character code order for
     * `searchWords()`.
     *
     * @return          The candidate; `null` if there is no more candidate.
     */
    val nextCandidate: WnnWord?

    /**
     * Retrieve the list of registered words.
     * <br></br>
     * @return          `null` if no word is registered; the array of [WnnWord] if some words is registered.
     */
    val userDictionaryWords: Array<WnnWord?>?

    /**
     * Learn a word.
     * <br></br>
     * This method is used to register the word selected from
     * candidates to the learning dictionary or update the frequency
     * of the word.
     *
     * @param word      The selected word
     * @return          `true` if success; `false` if fail or not supported.
     */
    fun learn(word: WnnWord): Boolean

    /**
     * Register a word to the user's dictionary.
     * <br></br>
     * @param word      A word to register
     * @return          Number of registered words in the user's dictionary after the operation; minus value if a error occurs.
     */
    fun addWord(word: WnnWord): Int

    /**
     * Delete a word from the user's dictionary.
     * <br></br>
     * @param word      A word to delete
     * @return          `true` if success; `false` if fail or not supported.
     */
    fun deleteWord(word: WnnWord?): Boolean

    /**
     * Delete all words from the user's dictionary.
     * <br></br>
     * @param dictionary    `DICTIONARY_TYPE_LEARN` or `DICTIONARY_TYPE_USER`
     * @return              `true` if success; `false` if fail or not supported.
     */
    fun initializeDictionary(dictionary: Int): Boolean

    /**
     * Delete all words from the user's dictionary of the specified language.
     * <br></br>
     * @param dictionary        `DICTIONARY_TYPE_LEARN` or `DICTIONARY_TYPE_USER`
     * @param type              Dictionary type (language, etc...)
     * @return                  `true` if success; `false` if fail or not supported.
     */
    fun initializeDictionary(dictionary: Int, type: Int): Boolean

    /**
     * Reflect the preferences in the converter.
     *
     * @param pref  The preferences
     */
    fun setPreferences(pref: SharedPreferences?)

    /**
     * Break the sequence of words.
     * <br></br>
     * This method is used to notice breaking the sequence of input
     * words to the converter.  The converter will stop learning
     * collocation between previous input words and words which will
     * input after this break.
     */
    fun breakSequence()

    /**
     * Makes the candidate list.
     * <br></br>
     * This method is used when to make a list of other candidates of
     * the clause which is in the result of consecutive clause
     * conversion([.convert]).
     * To get the elements of the list, call [.getNextCandidate].
     *
     * @param clausePosition  The position of a clause
     * @return                  Plus value if there are candidates; 0 if there is no candidate; minus value if a error occurs.
     */
    fun makeCandidateListOf(clausePosition: Int): Int

    companion object {
        /*
     * DEFINITION OF CONSTANTS
     */
        /** The identifier of the learning dictionary  */
        const val DICTIONARY_TYPE_LEARN: Int = 1

        /** The identifier of the user dictionary  */
        const val DICTIONARY_TYPE_USER: Int = 2
    }
}
