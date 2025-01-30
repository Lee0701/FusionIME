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

import jp.co.omronsoft.openwnn.CandidateFilter
import jp.co.omronsoft.openwnn.WnnClause
import jp.co.omronsoft.openwnn.WnnDictionary
import jp.co.omronsoft.openwnn.WnnPOS
import jp.co.omronsoft.openwnn.WnnSentence
import jp.co.omronsoft.openwnn.WnnWord
import java.util.LinkedList

/**
 * The penWnn Clause Converter class for Japanese IME.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
class OpenWnnClauseConverterJAJP {
    /** search cache for unique independent words (jiritsugo)  */
    private val mIndepWordBag =
        HashMap<String, ArrayList<WnnWord>>()

    /** search cache for all independent words (jiritsugo)  */
    private val mAllIndepWordBag =
        HashMap<String, ArrayList<WnnWord>>()

    /** search cache for ancillary words (fuzokugo)  */
    private val mFzkPatterns: HashMap<String?, ArrayList<WnnWord>?>

    /** connect matrix for generating a clause  */
    private var mConnectMatrix: Array<ByteArray?>?

    /** dictionaries  */
    private var mDictionary: WnnDictionary? = null

    /** candidates of conversion  */
    private val mConvertResult: LinkedList<*>

    /** work area for consecutive clause conversion  */
    private val mSentenceBuffer: Array<WnnSentence?>

    /** part of speech (default)  */
    private var mPosDefault: WnnPOS? = null

    /** part of speech (end of clause/not end of sentence)  */
    private var mPosEndOfClause1: WnnPOS? = null

    /** part of speech (end of clause/any place)  */
    private var mPosEndOfClause2: WnnPOS? = null

    /** part of speech (end of sentence)  */
    private var mPosEndOfClause3: WnnPOS? = null

    /** The candidate filter  */
    private var mFilter: CandidateFilter? = null

    /**
     * Constructor
     */
    init {
        mFzkPatterns = HashMap<Any?, Any?>()
        mConvertResult = LinkedList<Any?>()

        mSentenceBuffer = arrayOfNulls(MAX_INPUT_LENGTH)
    }

    /**
     * Set the dictionary
     *
     * @param dict  The dictionary for phrase conversion
     */
    fun setDictionary(dict: WnnDictionary) {
        /* get connect matrix */
        mConnectMatrix = dict.connectMatrix

        /* clear dictionary settings */
        mDictionary = dict
        dict.clearDictionary()
        dict.clearApproxPattern()


        /* clear work areas */
        mIndepWordBag.clear()
        mAllIndepWordBag.clear()
        mFzkPatterns.clear()


        /* get part of speech tags */
        mPosDefault = dict.getPOS(WnnDictionary.Companion.POS_TYPE_MEISI)
        mPosEndOfClause1 = dict.getPOS(WnnDictionary.Companion.POS_TYPE_V1)
        mPosEndOfClause2 = dict.getPOS(WnnDictionary.Companion.POS_TYPE_V2)
        mPosEndOfClause3 = dict.getPOS(WnnDictionary.Companion.POS_TYPE_V3)
    }

    /**
     * Set the candidate filter
     *
     * @param filter    The candidate filter
     */
    fun setFilter(filter: CandidateFilter?) {
        mFilter = filter
    }

    /**
     * Kana-to-Kanji conversion (single clause).
     * <br></br>
     * This method execute single clause conversion.
     *
     * @param input        The input string
     * @return            The candidates of conversion; `null` if an error occurs.
     */
    fun convert(input: String): Iterator<*>? {
        /* do nothing if no dictionary is specified */
        if (mConnectMatrix == null || mDictionary == null) {
            return null
        }
        /* do nothing if the length of input exceeds the limit */
        if (input.length > MAX_INPUT_LENGTH) {
            return null
        }

        /* clear the candidates list */
        mConvertResult.clear()

        /* try single clause conversion */
        if (!singleClauseConvert(mConvertResult, input, mPosEndOfClause2!!, true)) {
            return null
        }
        return mConvertResult.iterator()
    }

    /**
     * Consecutive clause conversion.
     *
     * @param input        The input string
     * @return            The result of consecutive clause conversion; `null` if fail.
     */
    fun consecutiveClauseConvert(input: String): WnnSentence? {
        val clauses: LinkedList<*> = LinkedList<Any?>()

        /* clear the cache which is not matched */
        for (i in 0 until input.length) {
            mSentenceBuffer[i] = null
        }
        val sentence = mSentenceBuffer

        /* consecutive clause conversion */
        for (start in 0 until input.length) {
            if (start != 0 && sentence[start - 1] == null) {
                continue
            }

            /* limit the length of a clause */
            var end = input.length
            if (end > start + 20) {
                end = start + 20
            }
            /* make clauses */
            while (end > start) {
                val idx = end - 1

                /* cutting a branch */
                if (sentence[idx] != null) {
                    if (start != 0) {
                        if (sentence[idx]!!.frequency > sentence[start - 1]!!.frequency + CLAUSE_COST + FREQ_LEARN) {
                            /* there may be no way to be the best sequence from the 'start' */
                            break
                        }
                    } else {
                        if (sentence[idx]!!.frequency > CLAUSE_COST + FREQ_LEARN) {
                            /* there may be no way to be the best sequence from the 'start' */
                            break
                        }
                    }
                }

                val key = input.substring(start, end)
                clauses.clear()
                var bestClause: WnnClause? = null
                if (end == input.length) {
                    /* get the clause which can be the end of the sentence */
                    singleClauseConvert(clauses, key, mPosEndOfClause1!!, false)
                } else {
                    /* get the clause which is not the end of the sentence */
                    singleClauseConvert(clauses, key, mPosEndOfClause3!!, false)
                }
                bestClause = if (clauses.isEmpty()) {
                    defaultClause(key)
                } else {
                    clauses[0] as WnnClause
                }

                /* make a sub-sentence */
                var ws = if (start == 0) {
                    WnnSentence(key, bestClause!!)
                } else {
                    WnnSentence(sentence[start - 1]!!, bestClause!!)
                }
                ws.frequency += CLAUSE_COST

                /* update the best sub-sentence on the cache buffer */
                if (sentence[idx] == null || (sentence[idx]!!.frequency < ws.frequency)) {
                    sentence[idx] = ws
                }
                end--
            }
        }

        /* return the result of the consecutive clause conversion */
        if (sentence[input.length - 1] != null) {
            return sentence[input.length - 1]
        }
        return null
    }

    /**
     * Consecutive clause conversion.
     *
     * @param resultList    Where to store the result
     * @param input            Input string
     * @return                `true` if success; `false` if fail.
     */
    private fun consecutiveClauseConvert(resultList: LinkedList<*>, input: String): Boolean {
        val sentence = consecutiveClauseConvert(input)

        /* set the result of the consecutive clause conversion on the top of the list */
        if (sentence != null) {
            resultList.add(0, sentence)
            return true
        }
        return false
    }

    /**
     * Single clause conversion.
     *
     * @param clauseList    Where to store the results
     * @param input            Input string
     * @param terminal        Part of speech tag at the terminal
     * @param all            Get all candidates or not
     * @return                `true` if success; `false` if fail.
     */
    private fun singleClauseConvert(
        clauseList: LinkedList<*>,
        input: String,
        terminal: WnnPOS,
        all: Boolean
    ): Boolean {
        var ret = false

        /* get clauses without ancillary word */
        var stems = getIndependentWords(input, all)
        if (stems != null && (!stems.isEmpty())) {
            val stemsi: Iterator<WnnWord> = stems.iterator()
            while (stemsi.hasNext()) {
                val stem = stemsi.next()
                if (addClause(clauseList, input, stem, null, terminal, all)) {
                    ret = true
                }
            }
        }

        /* get clauses with ancillary word */
        var max = CLAUSE_COST * 2
        for (split in 1 until input.length) {
            /* get ancillary patterns */
            var str = input.substring(split)
            val fzks = getAncillaryPattern(str)
            if (fzks == null || fzks.isEmpty()) {
                continue
            }


            /* get candidates of stem in a clause */
            str = input.substring(0, split)
            stems = getIndependentWords(str, all)
            if (stems == null || stems.isEmpty()) {
                if (mDictionary!!.searchWord(
                        WnnDictionary.Companion.SEARCH_PREFIX,
                        WnnDictionary.Companion.ORDER_BY_FREQUENCY,
                        str
                    ) <= 0
                ) {
                    break
                } else {
                    continue
                }
            }
            /* make clauses */
            val stemsi: Iterator<WnnWord> = stems.iterator()
            while (stemsi.hasNext()) {
                val stem = stemsi.next()
                if (all || stem.frequency > max) {
                    val fzksi: Iterator<WnnWord> = fzks.iterator()
                    while (fzksi.hasNext()) {
                        val fzk = fzksi.next()
                        if (addClause(clauseList, input, stem, fzk, terminal, all)) {
                            ret = true
                            max = stem.frequency
                        }
                    }
                }
            }
        }
        return ret
    }

    /**
     * Add valid clause to the candidates list.
     *
     * @param clauseList    Where to store the results
     * @param input            Input string
     * @param stem            Stem of the clause (a independent word)
     * @param fzk            Ancillary pattern
     * @param terminal        Part of speech tag at the terminal
     * @param all            Get all candidates or not
     * @return                `true` if add the clause to the list; `false` if not.
     */
    private fun addClause(
        clauseList: LinkedList<WnnClause>, input: String, stem: WnnWord, fzk: WnnWord?,
        terminal: WnnPOS, all: Boolean
    ): Boolean {
        var clause: WnnClause? = null
        /* check if the part of speech is valid */
        if (fzk == null) {
            if (connectible(stem.partOfSpeech!!.right, terminal.left)) {
                clause = WnnClause(input, stem)
            }
        } else {
            if (connectible(stem.partOfSpeech!!.right, fzk.partOfSpeech!!.left)
                && connectible(fzk.partOfSpeech!!.right, terminal.left)
            ) {
                clause = WnnClause(input, stem, fzk)
            }
        }
        if (clause == null) {
            return false
        }
        if (mFilter != null && !mFilter!!.isAllowed(clause)) {
            return false
        }

        /* store to the list */
        if (clauseList.isEmpty()) {
            /* add if the list is empty */
            clauseList.add(0, clause)
            return true
        } else {
            if (!all) {
                /* reserve only the best clause */
                if (clauseList[0].frequency < clause.frequency) {
                    clauseList[0] = clause
                    return true
                }
            } else {
                /* reserve all clauses */
                val clauseListi: Iterator<*> = clauseList.iterator()
                var index = 0
                while (clauseListi.hasNext()) {
                    val clausei = clauseListi.next() as WnnClause
                    if (clausei.frequency < clause.frequency) {
                        break
                    }
                    index++
                }
                clauseList.add(index, clause)
                return true
            }
        }

        return false
    }

    /**
     * Check the part-of-speeches are connectable.
     *
     * @param right        Right attribute of the preceding word/clause
     * @param left        Left attribute of the following word/clause
     * @return            `true` if there are connectable; `false` if otherwise
     */
    private fun connectible(right: Int, left: Int): Boolean {
        try {
            if (mConnectMatrix!![left]!![right].toInt() != 0) {
                return true
            }
        } catch (ex: Exception) {
        }
        return false
    }

    /**
     * Get all exact matched ancillary words(Fuzokugo) list.
     *
     * @param input        Search key
     * @return            List of ancillary words
     */
    private fun getAncillaryPattern(input: String): ArrayList<WnnWord>? {
        if (input.length == 0) {
            return null
        }

        val fzkPat = mFzkPatterns
        var fzks = fzkPat[input]
        if (fzks != null) {
            return fzks
        }

        /* set dictionaries */
        val dict = mDictionary
        dict!!.clearDictionary()
        dict.clearApproxPattern()
        dict.setDictionary(6, 400, 500)

        for (start in input.length - 1 downTo 0) {
            val key = input.substring(start)

            fzks = fzkPat[key]
            if (fzks != null) {
                continue
            }

            fzks = ArrayList()
            mFzkPatterns[key] = fzks

            /* search ancillary words */
            dict.searchWord(
                WnnDictionary.Companion.SEARCH_EXACT,
                WnnDictionary.Companion.ORDER_BY_FREQUENCY,
                key
            )
            var word: WnnWord
            while ((dict.nextWord.also { word = it!! }) != null) {
                fzks.add(word)
            }

            /* concatenate sequence of ancillary words */
            for (end in input.length - 1 downTo start + 1) {
                val followFzks = fzkPat[input.substring(end)]
                if (followFzks == null || followFzks.isEmpty()) {
                    continue
                }
                dict.searchWord(
                    WnnDictionary.Companion.SEARCH_EXACT,
                    WnnDictionary.Companion.ORDER_BY_FREQUENCY,
                    input.substring(start, end)
                )
                while ((dict.nextWord.also { word = it!! }) != null) {
                    val followFzksi: Iterator<WnnWord> = followFzks.iterator()
                    while (followFzksi.hasNext()) {
                        val follow = followFzksi.next()
                        if (connectible(word.partOfSpeech!!.right, follow.partOfSpeech!!.left)) {
                            fzks.add(
                                WnnWord(
                                    key,
                                    key,
                                    WnnPOS(word.partOfSpeech!!.left, follow.partOfSpeech!!.right)
                                )
                            )
                        }
                    }
                }
            }
        }
        return fzks
    }

    /**
     * Get all exact matched independent words(Jiritsugo) list.
     *
     * @param input    Search key
     * @param all      `true` if list all words; `false` if list words which has an unique part of speech tag.
     * @return            List of words; `null` if `input.length() == 0`.
     */
    private fun getIndependentWords(input: String, all: Boolean): ArrayList<WnnWord>? {
        if (input.length == 0) {
            return null
        }

        var words = if ((all)) mAllIndepWordBag[input] else mIndepWordBag[input]

        if (words == null) {
            /* set dictionaries */
            val dict = mDictionary
            dict!!.clearDictionary()
            dict.clearApproxPattern()
            dict.setDictionary(4, 0, 10)
            dict.setDictionary(5, 400, 500)
            dict.setDictionary(WnnDictionary.Companion.INDEX_USER_DICTIONARY, FREQ_USER, FREQ_USER)
            dict.setDictionary(
                WnnDictionary.Companion.INDEX_LEARN_DICTIONARY,
                FREQ_LEARN,
                FREQ_LEARN
            )

            words = ArrayList()
            var word: WnnWord
            if (all) {
                mAllIndepWordBag[input] = words
                dict.searchWord(
                    WnnDictionary.Companion.SEARCH_EXACT,
                    WnnDictionary.Companion.ORDER_BY_FREQUENCY,
                    input
                )
                /* store all words */
                while ((dict.nextWord.also { word = it!! }) != null) {
                    if (input == word.stroke) {
                        words.add(word)
                    }
                }
            } else {
                mIndepWordBag[input] = words
                dict.searchWord(
                    WnnDictionary.Companion.SEARCH_EXACT,
                    WnnDictionary.Companion.ORDER_BY_FREQUENCY,
                    input
                )
                /* store a word which has an unique part of speech tag */
                while ((dict.nextWord.also { word = it!! }) != null) {
                    if (input == word.stroke) {
                        val list: Iterator<WnnWord> = words.iterator()
                        var found = false
                        while (list.hasNext()) {
                            if (list.next().partOfSpeech!!.right == word.partOfSpeech!!.right) {
                                found = true
                                break
                            }
                        }
                        if (!found) {
                            words.add(word)
                        }
                        if (word.frequency < 400) {
                            break
                        }
                    }
                }
            }
            addAutoGeneratedCandidates(input, words, all)
        }
        return words
    }

    /**
     * Add some words not including in the dictionary.
     * <br></br>
     * This method adds some words which are not in the dictionary.
     *
     * @param input     Input string
     * @param wordList  List to store words
     * @param all       Get all candidates or not
     */
    private fun addAutoGeneratedCandidates(input: String, wordList: ArrayList<*>, all: Boolean) {
        wordList.add(WnnWord(input, input, mPosDefault, (CLAUSE_COST - 1) * input.length))
    }

    /**
     * Get a default clause.
     * <br></br>
     * This method generates a clause which has a string same as input
     * and the default part-of-speech tag.
     *
     * @param input    Input string
     * @return            Default clause
     */
    private fun defaultClause(input: String): WnnClause {
        return (WnnClause(input, input, mPosDefault, (CLAUSE_COST - 1) * input.length))
    }

    companion object {
        /** Score(frequency value) of word in the learning dictionary  */
        private const val FREQ_LEARN = 600

        /** Score(frequency value) of word in the user dictionary  */
        private const val FREQ_USER = 500

        /** Maximum limit length of input  */
        const val MAX_INPUT_LENGTH: Int = 50

        /** cost value of a clause  */
        private const val CLAUSE_COST = -1000
    }
}