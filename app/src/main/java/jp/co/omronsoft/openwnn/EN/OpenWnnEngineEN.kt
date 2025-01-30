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
package jp.co.omronsoft.openwnn.EN

import android.content.SharedPreferences
import jp.co.omronsoft.openwnn.CandidateFilter
import jp.co.omronsoft.openwnn.ComposingText
import jp.co.omronsoft.openwnn.OpenWnnDictionaryImpl
import jp.co.omronsoft.openwnn.WnnDictionary
import jp.co.omronsoft.openwnn.WnnEngine
import jp.co.omronsoft.openwnn.WnnWord
import java.util.Locale

/**
 * The OpenWnn engine class for English IME.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
class OpenWnnEngineEN(dicLibPath: String?, writableDictionaryName: String?) :
    WnnEngine {
    /** OpenWnn dictionary  */
    private var mDictionary: WnnDictionary

    /** Word list  */
    private val mConvResult = ArrayList<WnnWord>()

    /** HashMap for checking duplicate word  */
    private val mCandTable = HashMap<String?, WnnWord>()

    /** Input string  */
    private var mInputString: String? = null

    /** Searching string  */
    private var mSearchKey: String? = null

    /** Number of output candidates  */
    private var mOutputNum = 0

    /** The candidate filter  */
    private var mFilter: CandidateFilter? = null

    /**
     * Candidate's case
     * <br></br>
     * CASE_LOWER: all letters are lower.<br></br>
     * CASE_HEAD_UPPER: the first letter is upper; others are lower.<br></br>
     * CASE_UPPER: all letters are upper.<br></br>
     */
    private var mCandidateCase = 0

    /**
     * Constructor
     *
     * @param writableDictionaryName        Writable dictionary file name(null if not use)
     */
    init {
        mDictionary = OpenWnnDictionaryImpl(
            dicLibPath,
            writableDictionaryName
        )
        if (!mDictionary.isActive) {
            mDictionary = OpenWnnDictionaryImpl(
                "/system/lib/libWnnEngDic.so",
                writableDictionaryName
            )
        }
        mDictionary.clearDictionary()

        mDictionary.setDictionary(0, 400, 550)
        mDictionary.setDictionary(1, 400, 550)
        mDictionary.setDictionary(2, 400, 550)
        mDictionary.setDictionary(
            WnnDictionary.Companion.INDEX_USER_DICTIONARY,
            FREQ_USER,
            FREQ_USER
        )
        mDictionary.setDictionary(
            WnnDictionary.Companion.INDEX_LEARN_DICTIONARY,
            FREQ_LEARN,
            FREQ_LEARN
        )

        mDictionary.setApproxPattern(WnnDictionary.Companion.APPROX_PATTERN_EN_QWERTY_NEAR)

        mDictionary.setInUseState(false)
    }

    /**
     * Get a candidate.
     *
     * @param index        Index of candidate
     * @return            A candidate; `null` if no candidate for the index.
     */
    private fun getCandidate(index: Int): WnnWord? {
        var word: WnnWord?
        /* search the candidate from the dictionaries */
        while (mConvResult.size < PREDICT_LIMIT && index >= mConvResult.size) {
            while ((mDictionary.nextWord.also { word = it }) != null) {
                /* adjust the case of letter */
                val c = word!!.candidate!![0]
                if (mCandidateCase == CASE_LOWER) {
                    if (Character.isLowerCase(c)) {
                        break
                    }
                } else if (mCandidateCase == CASE_HEAD_UPPER) {
                    if (Character.isLowerCase(c)) {
                        word!!.candidate =
                            c.uppercaseChar().toString() + word!!.candidate!!.substring(1)
                    }
                    break
                } else {
                    word!!.candidate = word!!.candidate!!.uppercase(Locale.getDefault())
                    break
                }
            }
            if (word == null) {
                break
            }
            /* check duplication */
            addCandidate(word!!)
        }

        /* get the default candidates */
        if (index >= mConvResult.size) {
            /* input string itself */
            addCandidate(WnnWord(mInputString, mSearchKey))

            /* Capitalize the head of input */
            if (mSearchKey!!.length > 1) {
                addCandidate(
                    WnnWord(
                        mSearchKey!!.substring(0, 1)
                            .uppercase(Locale.getDefault()) + mSearchKey!!.substring(1),
                        mSearchKey
                    )
                )
            }

            /* Capitalize all */
            addCandidate(WnnWord(mSearchKey!!.uppercase(Locale.getDefault()), mSearchKey))
        }

        if (index >= mConvResult.size) {
            return null
        }
        return mConvResult[index]
    }

    /**
     * Add a word to the candidates list if there is no duplication.
     *
     * @param word        A word
     * @return            `true` if the word is added to the list; `false` if not.
     */
    private fun addCandidate(word: WnnWord): Boolean {
        if (word.candidate == null || mCandTable.containsKey(word.candidate)) {
            return false
        }
        if (mFilter != null && !mFilter!!.isAllowed(word)) {
            return false
        }
        mCandTable[word.candidate] = word
        mConvResult.add(word)
        return true
    }

    private fun clearCandidates() {
        mConvResult.clear()
        mCandTable.clear()
        mOutputNum = 0
        mSearchKey = null
    }

    /**
     * Set dictionary.
     *
     * @param type        Type of dictionary (DIC_DEFAULT or DIC_FOR_CORRECT_MISTYPE)
     * @return            `true` if the dictionary is changed; `false` if not.
     */
    fun setDictionary(type: Int): Boolean {
        if (type == DICT_FOR_CORRECT_MISTYPE) {
            mDictionary.clearApproxPattern()
            mDictionary.setApproxPattern(WnnDictionary.Companion.APPROX_PATTERN_EN_QWERTY_NEAR)
        } else {
            mDictionary.clearApproxPattern()
        }
        return true
    }

    /**
     * Set search key for the dictionary.
     * <br></br>
     * To search the dictionary, this method set the lower case of
     * input string to the search key. And hold the input string's
     * capitalization information to adjust the candidates
     * capitalization later.
     *
     * @param input        Input string
     * @return            `true` if the search key is set; `false` if not.
     */
    private fun setSearchKey(input: String): Boolean {
        if (input.length == 0) {
            return false
        }

        /* set mInputString */
        mInputString = input

        /* set mSearchKey */
        mSearchKey = input.lowercase(Locale.getDefault())

        /* set mCandidateCase */
        mCandidateCase = if (Character.isUpperCase(input[0])) {
            if (input.length > 1 && Character.isUpperCase(input[1])) {
                CASE_UPPER
            } else {
                CASE_HEAD_UPPER
            }
        } else {
            CASE_LOWER
        }

        return true
    }

    /**
     * Set the candidate filter
     *
     * @param filter    The candidate filter
     */
    fun setFilter(filter: CandidateFilter?) {
        mFilter = filter
    }

    /***********************************************************************
     * WnnEngine's interface
     */
    /** @see jp.co.omronsoft.openwnn.WnnEngine.init
     */
    override fun init() {}

    /** @see jp.co.omronsoft.openwnn.WnnEngine.close
     */
    override fun close() {}

    /** @see jp.co.omronsoft.openwnn.WnnEngine.predict
     */
    override fun predict(text: ComposingText?, minLen: Int, maxLen: Int): Int {
        clearCandidates()

        if (text == null) {
            return 0
        }

        val input = text.toString(2)
        if (!setSearchKey(input!!)) {
            return 0
        }

        /* set dictionaries by the length of input */
        val dict = mDictionary
        dict.setInUseState(true)

        dict.clearDictionary()
        dict.setDictionary(0, 400, 550)
        if (input.length > 1) {
            dict.setDictionary(1, 400, 550)
        }
        if (input.length > 2) {
            dict.setDictionary(2, 400, 550)
        }
        dict.setDictionary(WnnDictionary.Companion.INDEX_USER_DICTIONARY, FREQ_USER, FREQ_USER)
        dict.setDictionary(WnnDictionary.Companion.INDEX_LEARN_DICTIONARY, FREQ_LEARN, FREQ_LEARN)


        /* search dictionaries */
        dict.searchWord(
            WnnDictionary.Companion.SEARCH_PREFIX, WnnDictionary.Companion.ORDER_BY_FREQUENCY,
            mSearchKey!!
        )
        return 1
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.convert
     */
    override fun convert(text: ComposingText?): Int {
        clearCandidates()
        return 0
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.searchWords
     */
    override fun searchWords(key: String?): Int {
        clearCandidates()
        return 0
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.searchWords
     */
    override fun searchWords(word: WnnWord?): Int {
        clearCandidates()
        return 0
    }

    override val nextCandidate: WnnWord?
        /** @see jp.co.omronsoft.openwnn.WnnEngine.getNextCandidate
         */
        get() {
            if (mSearchKey == null) {
                return null
            }
            val word = getCandidate(mOutputNum)
            if (word != null) {
                mOutputNum++
            }
            return word
        }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.learn
     */
    override fun learn(word: WnnWord): Boolean {
        return (mDictionary.learnWord(word) == 0)
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.addWord
     */
    override fun addWord(word: WnnWord): Int {
        val dict = mDictionary
        dict.setInUseState(true)
        dict.addWordToUserDictionary(word)
        dict.setInUseState(false)
        return 0
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.deleteWord
     */
    override fun deleteWord(word: WnnWord?): Boolean {
        val dict = mDictionary
        dict.setInUseState(true)
        dict.removeWordFromUserDictionary(word)
        dict.setInUseState(false)
        return false
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.setPreferences
     */
    override fun setPreferences(pref: SharedPreferences?) {}

    /** @see jp.co.omronsoft.openwnn.WnnEngine.breakSequence
     */
    override fun breakSequence() {}

    /** @see jp.co.omronsoft.openwnn.WnnEngine.makeCandidateListOf
     */
    override fun makeCandidateListOf(clausePosition: Int): Int {
        return 0
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.initializeDictionary
     */
    override fun initializeDictionary(dictionary: Int): Boolean {
        val dict = mDictionary

        when (dictionary) {
            WnnEngine.Companion.DICTIONARY_TYPE_LEARN -> {
                dict.setInUseState(true)
                dict.clearLearnDictionary()
                dict.setInUseState(false)
                return true
            }

            WnnEngine.Companion.DICTIONARY_TYPE_USER -> {
                dict.setInUseState(true)
                dict.clearUserDictionary()
                dict.setInUseState(false)
                return true
            }
        }
        return false
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.initializeDictionary
     */
    override fun initializeDictionary(dictionary: Int, type: Int): Boolean {
        return initializeDictionary(dictionary)
    }

    override val userDictionaryWords: Array<WnnWord?>
        /** @see jp.co.omronsoft.openwnn.WnnEngine.getUserDictionaryWords
         */
        get() {
            val dict = mDictionary
            dict.setInUseState(true)
            val result = dict.userDictionaryWords
            dict.setInUseState(false)
            return result!!
        }

    companion object {
        /** Normal dictionary  */
        const val DICT_DEFAULT: Int = 0

        /** Dictionary for mistype correction  */
        const val DICT_FOR_CORRECT_MISTYPE: Int = 1

        /** Score(frequency value) of word in the learning dictionary  */
        const val FREQ_LEARN: Int = 600

        /** Score(frequency value) of word in the user dictionary  */
        const val FREQ_USER: Int = 500

        /** Limitation of predicted candidates  */
        const val PREDICT_LIMIT: Int = 300

        private const val CASE_LOWER = 0
        private const val CASE_UPPER = 1
        private const val CASE_HEAD_UPPER = 3
    }
}
