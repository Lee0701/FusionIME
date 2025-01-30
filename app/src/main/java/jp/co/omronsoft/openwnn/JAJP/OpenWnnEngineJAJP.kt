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
package jp.co.omronsoft.openwnn.JAJP

import android.content.SharedPreferences
import jp.co.omronsoft.openwnn.CandidateFilter
import jp.co.omronsoft.openwnn.ComposingText
import jp.co.omronsoft.openwnn.OpenWnnDictionaryImpl
import jp.co.omronsoft.openwnn.StrSegmentClause
import jp.co.omronsoft.openwnn.WnnClause
import jp.co.omronsoft.openwnn.WnnDictionary
import jp.co.omronsoft.openwnn.WnnEngine
import jp.co.omronsoft.openwnn.WnnSentence
import jp.co.omronsoft.openwnn.WnnWord
import java.util.Arrays

/**
 * The OpenWnn engine class for Japanese IME.
 *
 * @author Copyright (C) 2009-2011 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
class OpenWnnEngineJAJP(dicLibPath: String?, writableDictionaryName: String?) :
    WnnEngine {
    /** Current dictionary type  */
    private var mDictType = DIC_LANG_INIT

    /** Type of the keyboard  */
    private var mKeyboardType = KEYBOARD_UNDEF

    /** OpenWnn dictionary  */
    private var mDictionaryJP: WnnDictionary

    /** Word list  */
    private val mConvResult: ArrayList<WnnWord>

    /** HashMap for checking duplicate word  */
    private val mCandTable: HashMap<String?, WnnWord>

    /** Input string (Hiragana)  */
    private var mInputHiragana: String? = null

    /** Input string (Romaji)  */
    private var mInputRomaji: String? = null

    /** Number of output candidates  */
    private var mOutputNum = 0

    /**
     * Where to get the next candidates from.<br></br>
     * (0:prefix search from the dictionary, 1:single clause converter, 2:Kana converter)
     */
    private var mGetCandidateFrom = 0

    /** Previously selected word  */
    private var mPreviousWord: WnnWord? = null

    /** Converter for single/consecutive clause conversion  */
    private val mClauseConverter: OpenWnnClauseConverterJAJP

    /** Kana converter (for EISU-KANA conversion)  */
    private val mKanaConverter: KanaConverter

    /** Whether exact match search or prefix match search  */
    private var mExactMatchMode = false

    /** Whether displaying single clause candidates or not  */
    private var mSingleClauseMode = false

    /** A result of consecutive clause conversion  */
    private var mConvertSentence: WnnSentence? = null

    /** The candidate filter  */
    private var mFilter: CandidateFilter? = null

    /**
     * Constructor
     *
     * @param writableDictionaryName    Writable dictionary file name(null if not use)
     */
    init {
        /* load Japanese dictionary library */
        mDictionaryJP = OpenWnnDictionaryImpl(
            dicLibPath,
            writableDictionaryName
        )
        if (!mDictionaryJP.isActive()) {
            mDictionaryJP = OpenWnnDictionaryImpl(
                "/system/lib/libWnnJpnDic.so",
                writableDictionaryName
            )
        }

        /* clear dictionary settings */
        mDictionaryJP.clearDictionary()
        mDictionaryJP.clearApproxPattern()
        mDictionaryJP.setInUseState(false)

        /* work buffers */
        mConvResult = ArrayList()
        mCandTable = HashMap()

        /* converters */
        mClauseConverter = OpenWnnClauseConverterJAJP()
        mKanaConverter = KanaConverter()
    }

    /**
     * Set dictionary for prediction.
     *
     * @param strlen        Length of input string
     */
    private fun setDictionaryForPrediction(strlen: Int) {
        val dict = mDictionaryJP

        dict.clearDictionary()

        if (mDictType != DIC_LANG_JP_EISUKANA) {
            dict.clearApproxPattern()
            if (strlen == 0) {
                dict.setDictionary(2, 245, 245)
                dict.setDictionary(3, 100, 244)

                dict.setDictionary(
                    WnnDictionary.Companion.INDEX_LEARN_DICTIONARY,
                    FREQ_LEARN,
                    FREQ_LEARN
                )
            } else {
                dict.setDictionary(0, 100, 400)
                if (strlen > 1) {
                    dict.setDictionary(1, 100, 400)
                }
                dict.setDictionary(2, 245, 245)
                dict.setDictionary(3, 100, 244)

                dict.setDictionary(
                    WnnDictionary.Companion.INDEX_USER_DICTIONARY,
                    FREQ_USER,
                    FREQ_USER
                )
                dict.setDictionary(
                    WnnDictionary.Companion.INDEX_LEARN_DICTIONARY,
                    FREQ_LEARN,
                    FREQ_LEARN
                )
                if (mKeyboardType != KEYBOARD_QWERTY) {
                    dict.setApproxPattern(WnnDictionary.Companion.APPROX_PATTERN_JAJP_12KEY_NORMAL)
                }
            }
        }
    }

    /**
     * Get a candidate.
     *
     * @param index     Index of a candidate.
     * @return          The candidate; `null` if there is no candidate.
     */
    private fun getCandidate(index: Int): WnnWord? {
        var word: WnnWord

        if (mGetCandidateFrom == 0) {
            if (mDictType == DIC_LANG_JP_EISUKANA) {
                /* skip to Kana conversion if EISU-KANA conversion mode */
                mGetCandidateFrom = 2
            } else if (mSingleClauseMode) {
                /* skip to single clause conversion if single clause conversion mode */
                mGetCandidateFrom = 1
            } else {
                if (mConvResult.size < PREDICT_LIMIT) {
                    /* get prefix matching words from the dictionaries */
                    while (index >= mConvResult.size) {
                        if ((mDictionaryJP.nextWord.also { word = it!! }) == null) {
                            mGetCandidateFrom = 1
                            break
                        }
                        if (!mExactMatchMode || mInputHiragana == word.stroke) {
                            addCandidate(word)
                            if (mConvResult.size >= PREDICT_LIMIT) {
                                mGetCandidateFrom = 1
                                break
                            }
                        }
                    }
                } else {
                    mGetCandidateFrom = 1
                }
            }
        }

        /* get candidates by single clause conversion */
        if (mGetCandidateFrom == 1) {
            val convResult = mClauseConverter.convert(
                mInputHiragana!!
            )
            if (convResult != null) {
                while (convResult.hasNext()) {
                    addCandidate(convResult.next() as WnnWord)
                }
            }
            /* end of candidates by single clause conversion */
            mGetCandidateFrom = 2
        }


        /* get candidates from Kana converter */
        if (mGetCandidateFrom == 2) {
            val addCandidateList = mKanaConverter.createPseudoCandidateList(
                mInputHiragana!!, mInputRomaji!!, mKeyboardType
            )

            val it: Iterator<WnnWord?> = addCandidateList.iterator()
            while (it.hasNext()) {
                addCandidate(it.next()!!)
            }

            mGetCandidateFrom = 3
        }

        if (index >= mConvResult.size) {
            return null
        }
        return mConvResult[index]
    }

    /**
     * Add a candidate to the conversion result buffer.
     * <br></br>
     * This method adds a word to the result buffer if there is not
     * the same one in the buffer and the length of the candidate
     * string is not longer than `MAX_OUTPUT_LENGTH`.
     *
     * @param word      A word to be add
     * @return          `true` if the word added; `false` if not.
     */
    private fun addCandidate(word: WnnWord): Boolean {
        if (word.candidate == null || mCandTable.containsKey(word.candidate)
            || word.candidate!!.length > MAX_OUTPUT_LENGTH
        ) {
            return false
        }
        if (mFilter != null && !mFilter!!.isAllowed(word)) {
            return false
        }
        mCandTable[word.candidate] = word
        mConvResult.add(word)
        return true
    }

    /**
     * Clear work area that hold candidates information.
     */
    private fun clearCandidates() {
        mConvResult.clear()
        mCandTable.clear()
        mOutputNum = 0
        mInputHiragana = null
        mInputRomaji = null
        mGetCandidateFrom = 0
        mSingleClauseMode = false
    }

    /**
     * Set dictionary type.
     *
     * @param type      Type of dictionary
     * @return          `true` if the dictionary is changed; `false` if not.
     */
    fun setDictionary(type: Int): Boolean {
        mDictType = type
        return true
    }

    /**
     * Set the search key and the search mode from [ComposingText].
     *
     * @param text      Input text
     * @param maxLen    Maximum length to convert
     * @return          Length of the search key
     */
    private fun setSearchKey(text: ComposingText, maxLen: Int): Int {
        var input = text.toString(ComposingText.Companion.LAYER1)
        if (0 <= maxLen && maxLen <= input!!.length) {
            input = input.substring(0, maxLen)
            mExactMatchMode = true
        } else {
            mExactMatchMode = false
        }

        if (input!!.length == 0) {
            mInputHiragana = ""
            mInputRomaji = ""
            return 0
        }

        mInputHiragana = input
        mInputRomaji = text.toString(ComposingText.Companion.LAYER0)

        return input.length
    }

    /**
     * Clear the previous word's information.
     */
    fun clearPreviousWord() {
        mPreviousWord = null
    }

    /**
     * Set keyboard type.
     *
     * @param keyboardType      Type of keyboard
     */
    fun setKeyboardType(keyboardType: Int) {
        mKeyboardType = keyboardType
    }

    /**
     * Set the candidate filter
     *
     * @param filter    The candidate filter
     */
    fun setFilter(filter: CandidateFilter?) {
        mFilter = filter
        mClauseConverter.setFilter(filter)
    }

    /***********************************************************************
     * WnnEngine's interface
     */
    /** @see jp.co.omronsoft.openwnn.WnnEngine.init
     */
    override fun init() {
        clearPreviousWord()
        mClauseConverter.setDictionary(mDictionaryJP)
        mKanaConverter.setDictionary(mDictionaryJP)
    }

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

        /* set mInputHiragana and mInputRomaji */
        val len = setSearchKey(text, maxLen)

        /* set dictionaries by the length of input */
        setDictionaryForPrediction(len)


        /* search dictionaries */
        mDictionaryJP.setInUseState(true)

        if (len == 0) {
            /* search by previously selected word */
            return mDictionaryJP.searchWord(
                WnnDictionary.Companion.SEARCH_LINK, WnnDictionary.Companion.ORDER_BY_FREQUENCY,
                mInputHiragana!!, mPreviousWord
            )
        } else {
            if (mExactMatchMode) {
                /* exact matching */
                mDictionaryJP.searchWord(
                    WnnDictionary.Companion.SEARCH_EXACT,
                    WnnDictionary.Companion.ORDER_BY_FREQUENCY,
                    mInputHiragana!!
                )
            } else {
                /* prefix matching */
                mDictionaryJP.searchWord(
                    WnnDictionary.Companion.SEARCH_PREFIX,
                    WnnDictionary.Companion.ORDER_BY_FREQUENCY,
                    mInputHiragana!!
                )
            }
            return 1
        }
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.convert
     */
    override fun convert(text: ComposingText?): Int {
        clearCandidates()

        if (text == null) {
            return 0
        }

        mDictionaryJP.setInUseState(true)

        val cursor = text.getCursor(ComposingText.Companion.LAYER1)
        var input: String?
        var head: WnnClause? = null
        if (cursor > 0) {
            /* convert previous part from cursor */
            input = text.toString(ComposingText.Companion.LAYER1, 0, cursor - 1)
            val headCandidates = mClauseConverter.convert(input!!)
            if ((headCandidates == null) || (!headCandidates.hasNext())) {
                return 0
            }
            head = WnnClause(input, headCandidates.next() as WnnWord)

            /* set the rest of input string */
            input = text.toString(
                ComposingText.Companion.LAYER1,
                cursor,
                text.size(ComposingText.Companion.LAYER1) - 1
            )
        } else {
            /* set whole of input string */
            input = text.toString(ComposingText.Companion.LAYER1)
        }

        var sentence: WnnSentence? = null
        if (input!!.length != 0) {
            sentence = mClauseConverter.consecutiveClauseConvert(input)
        }
        if (head != null) {
            sentence = WnnSentence(head, sentence)
        }
        if (sentence == null) {
            return 0
        }

        val ss = arrayOfNulls<StrSegmentClause>(sentence.elements!!.size)
        var pos = 0
        var idx = 0
        val it: Iterator<WnnClause> = sentence.elements!!.iterator()
        while (it.hasNext()) {
            val clause = it.next()
            val len = clause.stroke!!.length
            ss[idx] = StrSegmentClause(clause, pos, pos + len - 1)
            pos += len
            idx += 1
        }
        text.setCursor(ComposingText.Companion.LAYER2, text.size(ComposingText.Companion.LAYER2))
        text.replaceStrSegment(
            ComposingText.Companion.LAYER2, ss,
            text.getCursor(ComposingText.Companion.LAYER2)
        )
        mConvertSentence = sentence

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
            if (mInputHiragana == null) {
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
        var ret = -1
        if (word.partOfSpeech!!.right == 0) {
            word.partOfSpeech = mDictionaryJP.getPOS(WnnDictionary.Companion.POS_TYPE_MEISI)
        }

        val dict = mDictionaryJP
        if (word is WnnSentence) {
            val clauses: Iterator<WnnClause> = word.elements!!.iterator()
            while (clauses.hasNext()) {
                val wd: WnnWord = clauses.next()
                ret = if (mPreviousWord != null) {
                    dict.learnWord(wd, mPreviousWord)
                } else {
                    dict.learnWord(wd)
                }
                mPreviousWord = wd
                if (ret != 0) {
                    break
                }
            }
        } else {
            ret = if (mPreviousWord != null) {
                dict.learnWord(word, mPreviousWord)
            } else {
                dict.learnWord(word)
            }
            mPreviousWord = word
            mClauseConverter.setDictionary(dict)
        }

        return (ret == 0)
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.addWord
     */
    override fun addWord(word: WnnWord): Int {
        mDictionaryJP.setInUseState(true)
        if (word.partOfSpeech!!.right == 0) {
            word.partOfSpeech = mDictionaryJP.getPOS(WnnDictionary.Companion.POS_TYPE_MEISI)
        }
        mDictionaryJP.addWordToUserDictionary(word)
        mDictionaryJP.setInUseState(false)
        return 0
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.deleteWord
     */
    override fun deleteWord(word: WnnWord?): Boolean {
        mDictionaryJP.setInUseState(true)
        mDictionaryJP.removeWordFromUserDictionary(word)
        mDictionaryJP.setInUseState(false)
        return false
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.setPreferences
     */
    override fun setPreferences(pref: SharedPreferences?) {}

    /** @see jp.co.omronsoft.openwnn.WnnEngine.breakSequence
     */
    override fun breakSequence() {
        clearPreviousWord()
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.makeCandidateListOf
     */
    override fun makeCandidateListOf(clausePosition: Int): Int {
        clearCandidates()

        if ((mConvertSentence == null) || (mConvertSentence!!.elements!!.size <= clausePosition)) {
            return 0
        }
        mSingleClauseMode = true
        val clause = mConvertSentence!!.elements!![clausePosition]
        mInputHiragana = clause.stroke
        mInputRomaji = clause.candidate

        return 1
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine.initializeDictionary
     */
    override fun initializeDictionary(dictionary: Int): Boolean {
        when (dictionary) {
            WnnEngine.Companion.DICTIONARY_TYPE_LEARN -> {
                mDictionaryJP.setInUseState(true)
                mDictionaryJP.clearLearnDictionary()
                mDictionaryJP.setInUseState(false)
                return true
            }

            WnnEngine.Companion.DICTIONARY_TYPE_USER -> {
                mDictionaryJP.setInUseState(true)
                mDictionaryJP.clearUserDictionary()
                mDictionaryJP.setInUseState(false)
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
            /* get words in the user dictionary */
            mDictionaryJP.setInUseState(true)
            val result = mDictionaryJP.userDictionaryWords
            mDictionaryJP.setInUseState(false)

            /* sort the array of words */
            Arrays.sort(result, WnnWordComparator())

            return result!!
        }

    /* {@link WnnWord} comparator for listing up words in the user dictionary */
    private inner class WnnWordComparator : Comparator<Any?> {
        override fun compare(object1: Any?, object2: Any?): Int {
            val wnnWord1 = object1 as WnnWord?
            val wnnWord2 = object2 as WnnWord?
            return wnnWord1!!.stroke!!.compareTo(wnnWord2!!.stroke!!)
        }
    }

    companion object {
        /** Dictionary type (default)  */
        const val DIC_LANG_INIT: Int = 0

        /** Dictionary type (Japanese standard)  */
        const val DIC_LANG_JP: Int = 0

        /** Dictionary type (English standard)  */
        const val DIC_LANG_EN: Int = 1

        /** Dictionary type (Japanese person's name)  */
        const val DIC_LANG_JP_PERSON_NAME: Int = 2

        /** Dictionary type (User dictionary)  */
        const val DIC_USERDIC: Int = 3

        /** Dictionary type (Japanese EISU-KANA conversion)  */
        const val DIC_LANG_JP_EISUKANA: Int = 4

        /** Dictionary type (e-mail/URI)  */
        const val DIC_LANG_EN_EMAIL_ADDRESS: Int = 5

        /** Dictionary type (Japanese postal address)  */
        const val DIC_LANG_JP_POSTAL_ADDRESS: Int = 6

        /** Keyboard type (not defined)  */
        const val KEYBOARD_UNDEF: Int = 0

        /** Keyboard type (12-keys)  */
        const val KEYBOARD_KEYPAD12: Int = 1

        /** Keyboard type (Qwerty)  */
        const val KEYBOARD_QWERTY: Int = 2

        /** Score(frequency value) of word in the learning dictionary  */
        const val FREQ_LEARN: Int = 600

        /** Score(frequency value) of word in the user dictionary  */
        const val FREQ_USER: Int = 500

        /** Maximum limit length of output  */
        const val MAX_OUTPUT_LENGTH: Int = 50

        /** Limitation of predicted candidates  */
        const val PREDICT_LIMIT: Int = 100

        /** Limitation of candidates one-line  */
        const val LIMIT_OF_CANDIDATES_1LINE: Int = 500
    }
}
