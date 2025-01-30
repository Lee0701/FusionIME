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


/**
 * The interface of dictionary searcher used by [OpenWnn].
 *
 * @author Copyright (C) 2008-2009, OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
interface WnnDictionary {
    /**
     * Whether this dictionary module is active.
     * @return `true` if this dictionary module is active; `false` if not.
     */
    val isActive: Boolean

    /**
     * Set "in use" state.
     *
     * When the flag set true, the user dictionary is locked.
     *
     * @param flag      `true` if the user dictionary is locked; `false` if the user dictionary is unlocked.
     */
    fun setInUseState(flag: Boolean)

    /**
     * Clear all dictionary settings.
     *
     * All the dictionaries are set to be unused.
     *
     * @return          0 if success; minus value(error code) if fail.
     */
    fun clearDictionary(): Int

    /**
     * Sets a dictionary information for using specified dictionary.
     *
     *
     *
     * A dictionary information contains parameters:<br></br>
     * `base` is the bias of frequency for the dictionary.<br></br>
     * `high` is the upper limit of frequency for the dictionary.
     *
     * Searched word's frequency in the dictionary is mapped to the range from `base` to `high`.
     * <br></br>
     * The maximum value of `base` and `high` is 1000.
     * To set a dictionary unused, specify -1 to `base` and `high`.
     *
     * @param index     A dictionary index
     * @param base      The base frequency for the dictionary
     * @param high      The maximum frequency for the dictionary
     * @return          0 if success; minus value(error code) if fail.
     */
    fun setDictionary(index: Int, base: Int, high: Int): Int

    /**
     * Clears approximate patterns.
     *
     * This clears all approximate search patterns in the search condition.
     */
    fun clearApproxPattern()

    /**
     * Sets a approximate pattern.
     *
     * This adds an approximate search pattern(replacement of character) to the search condition.
     * The pattern rule is defined as replacing a character(`src`) to characters(`dst`).
     * <br></br>
     * The length of `src` must be 1 and the length of `dst` must be lower than 4.<br></br>
     * The maximum count of approximate patterns is 255.
     *
     * @param src       A character replace from
     * @param dst       Characters replace to
     * @return          0 if success; minus value(error code) if fail.
     */
    fun setApproxPattern(src: String?, dst: String?): Int

    /**
     * Sets a predefined approximate pattern.
     *
     * The patterns included predefined approximate search pattern set specified by
     * `approxPattern` are added to the search condition.
     *
     * @param approxPattern     A predefined approximate pattern set
     * @see jp.co.omronsoft.openwnn.WnnDictionary.APPROX_PATTERN_EN_TOUPPER
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.APPROX_PATTERN_EN_TOLOWER
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.APPROX_PATTERN_EN_QWERTY_NEAR
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.APPROX_PATTERN_EN_QWERTY_NEAR_UPPER
     *
     *
     * @return                  0 if success; minus value(error code) if fail.
     */
    fun setApproxPattern(approxPattern: Int): Int

    /**
     * Search words from dictionaries with specified conditions.
     *
     *
     * To get the searched word's information, use [.getNextWord].<br></br>
     * If a same word existed in the set of dictionary, the search result may contain some same words.<br></br>
     * <br></br>
     * If approximate patterns were set, the first word in search
     * results is the highest approximation word which contains best
     * matched character in the key string. <br></br>
     * For example, If a key string is "bbc", a approximate pattern
     * "b" to "a" is specified and the dictionary includes "abc
     * (frequency 10)" "bbcd (frequency 1)" "aac (frequency 5)"; the
     * result of prefix search is output by following order: "bbcd",
     * "abc", "aac".
     *
     *
     *
     * The supported combination of parameters is:
     * <table>
     * <th></th><td>Search Mode</td><td>Sort Order</td><td>Ambiguous Search</td>
     * <tr><td>exact matching</td><td>frequency descending</td><td>no</td></tr>
     * <tr><td>prefix matching</td><td>frequency descending</td><td>no</td></tr>
     * <tr><td>prefix matching</td><td>frequency descending</td><td>yes</td></tr>
     * <tr><td>prefix matching</td><td>character code ascending</td><td>no</td></tr>
    </table> *
     *
     *
     * @param operation     The search operation
     * @see jp.co.omronsoft.openwnn.WnnDictionary.SEARCH_EXACT
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.SEARCH_PREFIX
     *
     * @param order         The sort order
     * @see jp.co.omronsoft.openwnn.WnnDictionary.ORDER_BY_FREQUENCY
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.ORDER_BY_KEY
     *
     * @param keyString     The key string
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.getNextWord
     *
     *
     * @return              0 if no word is found; 1 if some words found; minus value if a error occurs.
     */
    fun searchWord(operation: Int, order: Int, keyString: String): Int

    /**
     * Search words from dictionaries with specified conditions and previous word.
     *
     *
     * For using link search function, specify the `wnnWord` as previous word and
     * set `SEARCH_LINK` mode to `operation`. The other arguments are
     * the same as [.searchWord].
     *
     *
     * If the prediction dictionary for reading is set to use, the previous word must contain
     * the `stroke` and the `candidate` information. If the prediction dictionary
     * for part of speech is set to use, the previous word must contain the `partOfSpeech` information.
     *
     * @param wnnWord       The previous word
     * @see jp.co.omronsoft.openwnn.WnnDictionary.searchWord
     *
     *
     * @return              0 if no word is found; 1 if some words found; minus value if a error occurs.
     */
    fun searchWord(operation: Int, order: Int, keyString: String, wnnWord: WnnWord?): Int

    /**
     * Retrieve a searched word information.
     *
     * It returns a word information from top of the `searchWord()`'s result.
     * To get all word's information of the result, call this method repeatedly until it returns null.
     *
     * @return              An instance of WnnWord; null if no result or an error occurs.
     */
    val nextWord: WnnWord?

    /**
     * Retrieve a searched word information with condition of length.
     *
     * It returns a word information from top of the `searchWord()`'s result.
     * To get all word's information of the result, call this method repeatedly until it returns null.
     *
     * @param length    >0 if only the result of specified length is retrieved; 0 if no condition exist
     * @return          An instance of WnnWord; null if no result or an error occurs.
     */
    fun getNextWord(length: Int): WnnWord?

    /**
     * Retrieve all word in the user dictionary.
     *
     * @return          The array of WnnWord objects.
     */
    val userDictionaryWords: Array<WnnWord?>?

    /**
     * Retrieve the connect matrix.
     *
     * @return          The array of the connect matrix; null if an error occurs.
     */
    val connectMatrix: Array<ByteArray?>?

    /**
     * Retrieve the part of speech information specified POS type.
     *
     * @param type      The type of a part of speech
     * @return          The part of speech information; null if invalid type is specified or  an error occurs.
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_V1
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_V2
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_V3
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_BUNTOU
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_TANKANJI
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_SUUJI
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_MEISI
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_JINMEI
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_CHIMEI
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_KIGOU
     */
    fun getPOS(type: Int): WnnPOS?

    /**
     * Clear the user dictionary.
     *
     * @return      0 if no error occur; <0 if an error occur
     */
    fun clearUserDictionary(): Int

    /**
     * Clear the learn dictionary.
     *
     * @return      0 if no error occur; <0 if an error occur
     */
    fun clearLearnDictionary(): Int

    /**
     * Add the words to user dictionary.
     *
     * @param word      The array of word
     * @return          0 if no error occur; <0 if an error occur
     */
    fun addWordToUserDictionary(word: Array<WnnWord?>): Int

    /**
     * Add the word to user dictionary.
     *
     * @param word      The word
     * @return          0 if no error occur; <0 if an error occur
     */
    fun addWordToUserDictionary(word: WnnWord?): Int

    /**
     * Remove the words from user dictionary.
     *
     * @param word      The array of word
     * @return          0 if no error occur; <0 if an error occur
     */
    fun removeWordFromUserDictionary(word: Array<WnnWord?>): Int

    /**
     * Remove the word from user dictionary.
     *
     * @param word      The word
     * @return          0 if no error occur; <0 if an error occur
     */
    fun removeWordFromUserDictionary(word: WnnWord?): Int

    /**
     * Learn the word.
     *
     * @param word      The word for learning
     * @return          0 if no error occur; <0 if an error occur
     */
    fun learnWord(word: WnnWord): Int

    /**
     * Learn the word with connection.
     *
     * @param word              The word for learning
     * @param previousWord      The word for link learning
     * @return                  0 if no error occur; <0 if an error occur
     */
    fun learnWord(word: WnnWord, previousWord: WnnWord?): Int

    companion object {
        /*
     * DEFINITION OF CONSTANTS
     */
        /**
         * Predefined approximate pattern set (capital letters from small letters).
         *
         * This pattern includes the rules for ambiguous searching capital letters from small letters.<br></br>
         * ex. "a" to "A", "b" to "B", ... , "z" to "Z"
         */
        const val APPROX_PATTERN_EN_TOUPPER: Int = 0

        /**
         * Predefined approximate pattern set (small letters from capital letters).
         *
         * This pattern includes the rules for ambiguous searching small letters from capital letters.<br></br>
         * ex. "A" to "a", "B" to "b", ... , "Z" to "z"
         */
        const val APPROX_PATTERN_EN_TOLOWER: Int = 1

        /**
         * Predefined approximate pattern set (QWERTY neighbor keys).
         *
         * This pattern includes the rules for ambiguous searching neighbor keys on QWERTY keyboard.
         * Only alphabet letters are defined; numerical or symbol letters are not defined as the rules.<br></br>
         * ex. "a" to "q"/"w"/"s"/"z", "b" to "v"/"g"/"h"/"n", ... ,"z" to "a"/"s"/"x"
         */
        const val APPROX_PATTERN_EN_QWERTY_NEAR: Int = 2

        /**
         * Predefined approximate pattern set (QWERTY neighbor keys/capital letters).
         *
         * This pattern includes the rules for ambiguous searching capital letters of neighbor keys on QWERTY keyboard.
         * Only alphabet letters are defined; numerical or symbol letters are not defined as the rules.<br></br>
         * ex. "a" to "Q"/"W"/"S"/"Z", "b" to "V"/"G"/"H"/"N", ... ,"z" to "A"/"S"/"X"
         */
        const val APPROX_PATTERN_EN_QWERTY_NEAR_UPPER: Int = 3

        /**
         * Predefined approximate pattern set (for Japanese 12-key keyboard).
         *
         * This pattern includes the standard rules for Japanese multi-tap 12-key keyboard.
         * ex. "&#x306F;" to "&#x3070;"/"&#x3071;", "&#x3064;" to "&#x3063;"/"&#x3065;"
         */
        const val APPROX_PATTERN_JAJP_12KEY_NORMAL: Int = 4

        /** Search operation mode (exact matching).  */
        const val SEARCH_EXACT: Int = 0

        /** Search operation mode (prefix matching).  */
        const val SEARCH_PREFIX: Int = 1

        /** Search operation mode (link search).  */
        const val SEARCH_LINK: Int = 2

        /** Sort order (frequency in descending).  */
        const val ORDER_BY_FREQUENCY: Int = 0

        /** Sort order (character code of key string in ascending).  */
        const val ORDER_BY_KEY: Int = 1

        /** Type of a part of speech (V1)  */
        const val POS_TYPE_V1: Int = 0

        /** Type of a part of speech (V2)  */
        const val POS_TYPE_V2: Int = 1

        /** Type of a part of speech (V3)  */
        const val POS_TYPE_V3: Int = 2

        /** Type of a part of speech (Top of sentence)  */
        const val POS_TYPE_BUNTOU: Int = 3

        /** Type of a part of speech (Single Chinese character)  */
        const val POS_TYPE_TANKANJI: Int = 4

        /** Type of a part of speech (Numeric)  */
        const val POS_TYPE_SUUJI: Int = 5

        /** Type of a part of speech (Noun)  */
        const val POS_TYPE_MEISI: Int = 6

        /** Type of a part of speech (Person's name)  */
        const val POS_TYPE_JINMEI: Int = 7

        /** Type of a part of speech (Place name)  */
        const val POS_TYPE_CHIMEI: Int = 8

        /** Type of a part of speech (Symbol)  */
        const val POS_TYPE_KIGOU: Int = 9

        /** Index of the user dictionary for [.setDictionary]  */
        const val INDEX_USER_DICTIONARY: Int = -1

        /** Index of the learn dictionary for [.setDictionary]  */
        const val INDEX_LEARN_DICTIONARY: Int = -2
    }
}

