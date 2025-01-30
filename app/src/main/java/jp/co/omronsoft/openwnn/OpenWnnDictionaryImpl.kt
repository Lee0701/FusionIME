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

import android.content.ContentValues
import android.database.DatabaseUtils
import android.database.SQLException
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteDatabase

/**
 * The implementation class of WnnDictionary interface (JNI wrapper class).
 *
 * @author Copyright (C) 2008, 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
class OpenWnnDictionaryImpl @JvmOverloads constructor(
    dicLibPath: String?,
    dicFilePath: String? = null
) :
    WnnDictionary {
    /*
     * DEFINITION OF PRIVATE FIELD
     */
    /** Internal work area for the dictionary search library  */
    protected var mWnnWork: Long = 0

    /** The file path of the writable dictionary  */
    protected var mDicFilePath: String = ""

    /** The writable dictionary object  */
    protected var mDbDic: SQLiteDatabase? = null

    /** The search cursor of the writable dictionary  */
    protected var mDbCursor: SQLiteCursor? = null

    /** The writable dictionary object Access helper  */
    protected var mDbOpenHelper: OpenWnnSQLiteOpenHelper? = null

    /** The number of queried items  */
    protected var mCountCursor: Int = 0

    /** The type of the search cursor object  */
    protected var mTypeOfQuery: Int = -1

    /** The query base strings for query operation  */
    protected var mExactQuerySqlOrderByFreq: String? = null

    /** The query base strings for query operation  */
    protected var mExactQuerySqlOrderByKey: String? = null

    /** The query base strings for query operation  */
    protected var mFullPrefixQuerySqlOrderByFreq: String? = null

    /** The query base strings for query operation  */
    protected var mFastPrefixQuerySqlOrderByFreq: String? = null

    /** The query base strings for query operation  */
    protected var mFullPrefixQuerySqlOrderByKey: String? = null

    /** The query base strings for query operation  */
    protected var mFastPrefixQuerySqlOrderByKey: String? = null

    /** The query base strings for query operation  */
    protected var mFullLinkQuerySqlOrderByFreq: String? = null

    /** The query base strings for query operation  */
    protected var mFastLinkQuerySqlOrderByFreq: String? = null

    /** The query base strings for query operation  */
    protected var mFullLinkQuerySqlOrderByKey: String? = null

    /** The query base strings for query operation  */
    protected var mFastLinkQuerySqlOrderByKey: String? = null

    /** The string array used by query operation (for "selection")  */
    protected var mExactQueryArgs: Array<String?> = arrayOfNulls(1)

    /** The string array used by query operation (for "selection")  */
    protected var mFullQueryArgs: Array<String?> =
        arrayOfNulls(MAX_LENGTH_OF_QUERY * (MAX_PATTERN_OF_APPROX + 1))

    /** The string array used by query operation (for "selection")  */
    protected var mFastQueryArgs: Array<String?> =
        arrayOfNulls(FAST_QUERY_LENGTH * (MAX_PATTERN_OF_APPROX + 1))

    /** The Frequency offset of user dictionary  */
    protected var mFrequencyOffsetOfUserDictionary: Int = -1

    /** The Frequency offset of learn dictionary  */
    protected var mFrequencyOffsetOfLearnDictionary: Int = -1

    /**
     * The constructor of this class with writable dictionary.
     *
     * Create a internal work area and the writable dictionary for the search engine. It is allocated for each object.
     *
     * @param dicLibPath    The dictionary library file path
     * @param dicFilePath   The path name of writable dictionary
     */
    /*
     * DEFINITION OF METHODS
     */
    /**
     * The constructor of this class without writable dictionary.
     *
     * Create a internal work area for the search engine. It is allocated for each object.
     *
     * @param dicLibPath    The dictionary library file path
     */
    init {
        /* Create the internal work area */
        this.mWnnWork = OpenWnnDictionaryImplJni.createWnnWork(dicLibPath)

        if (this.mWnnWork != 0L && dicFilePath != null) {
            /* Create query base strings */
            val queryFullBaseString =
                OpenWnnDictionaryImplJni.createQueryStringBase(
                    this.mWnnWork,
                    MAX_LENGTH_OF_QUERY,
                    MAX_PATTERN_OF_APPROX,
                    COLUMN_NAME_STROKE
                )

            val queryFastBaseString =
                OpenWnnDictionaryImplJni.createQueryStringBase(
                    this.mWnnWork,
                    FAST_QUERY_LENGTH,
                    MAX_PATTERN_OF_APPROX,
                    COLUMN_NAME_STROKE
                )


            mExactQuerySqlOrderByFreq = String.format(
                NORMAL_QUERY,
                String.format("%s=?", COLUMN_NAME_STROKE), String.format("%s DESC", COLUMN_NAME_ID)
            )

            mExactQuerySqlOrderByKey = String.format(
                NORMAL_QUERY,
                String.format("%s=?", COLUMN_NAME_STROKE), COLUMN_NAME_STROKE
            )


            mFullPrefixQuerySqlOrderByFreq = String.format(
                NORMAL_QUERY,
                queryFullBaseString, String.format("%s DESC", COLUMN_NAME_ID)
            )

            mFastPrefixQuerySqlOrderByFreq = String.format(
                NORMAL_QUERY,
                queryFastBaseString, String.format("%s DESC", COLUMN_NAME_ID)
            )

            mFullPrefixQuerySqlOrderByKey = String.format(
                NORMAL_QUERY,
                queryFullBaseString, COLUMN_NAME_STROKE
            )

            mFastPrefixQuerySqlOrderByKey = String.format(
                NORMAL_QUERY,
                queryFastBaseString, COLUMN_NAME_STROKE
            )


            mFullLinkQuerySqlOrderByFreq = String.format(
                LINK_QUERY, COLUMN_NAME_PREVIOUS_STROKE, COLUMN_NAME_PREVIOUS_CANDIDATE,
                queryFullBaseString, String.format("%s DESC", COLUMN_NAME_ID)
            )

            mFastLinkQuerySqlOrderByFreq = String.format(
                LINK_QUERY, COLUMN_NAME_PREVIOUS_STROKE, COLUMN_NAME_PREVIOUS_CANDIDATE,
                queryFastBaseString, String.format("%s DESC", COLUMN_NAME_ID)
            )

            mFullLinkQuerySqlOrderByKey = String.format(
                LINK_QUERY, COLUMN_NAME_PREVIOUS_STROKE, COLUMN_NAME_PREVIOUS_CANDIDATE,
                queryFullBaseString, COLUMN_NAME_STROKE
            )

            mFastLinkQuerySqlOrderByKey = String.format(
                LINK_QUERY, COLUMN_NAME_PREVIOUS_STROKE, COLUMN_NAME_PREVIOUS_CANDIDATE,
                queryFastBaseString, COLUMN_NAME_STROKE
            )


            try {
                /* Create the database object */
                mDicFilePath = dicFilePath
                setInUseState(true)

                /* Create the table if not exist */
                createDictionaryTable(TABLE_NAME_DIC)
            } catch (e: SQLException) {
            }
        }
    }

    /**
     * The finalizer of this class.
     * Destroy the internal work area for the search engine.
     */
    protected fun finalize() {
        /* Free the internal work area */
        if (this.mWnnWork != 0L) {
            OpenWnnDictionaryImplJni.freeWnnWork(this.mWnnWork)
            this.mWnnWork = 0

            freeDatabase()
        }
    }

    /**
     * Create the table of writable dictionary.
     *
     * @param tableName     The name of table
     */
    protected fun createDictionaryTable(tableName: String) {
        val sqlStr = "create table if not exists " + tableName +
                " (" + COLUMN_NAME_ID + " integer primary key autoincrement, " +
                COLUMN_NAME_TYPE + " integer, " +
                COLUMN_NAME_STROKE + " text, " +
                COLUMN_NAME_CANDIDATE + " text, " +
                COLUMN_NAME_POS_LEFT + " integer, " +
                COLUMN_NAME_POS_RIGHT + " integer, " +
                COLUMN_NAME_PREVIOUS_STROKE + " text, " +
                COLUMN_NAME_PREVIOUS_CANDIDATE + " text, " +
                COLUMN_NAME_PREVIOUS_POS_LEFT + " integer, " +
                COLUMN_NAME_PREVIOUS_POS_RIGHT + " integer)"

        if (mDbDic != null) {
            mDbDic!!.execSQL(sqlStr)
        }
    }

    /**
     * Free the [SQLiteDatabase] of writable dictionary.
     */
    protected fun freeDatabase() {
        freeCursor()

        if (mDbDic != null) {
            /* The SQLiteDataBase object must close() before releasing. */
            mDbDic!!.close()
            mDbDic = null
            mDbOpenHelper = null
        }
    }

    /**
     * Free the [SQLiteCursor] of writable dictionary.
     */
    protected fun freeCursor() {
        if (mDbCursor != null) {
            /* The SQLiteCursor object must close() before releasing. */
            mDbCursor!!.close()
            mDbCursor = null

            mTypeOfQuery = -1
        }
    }


    override val isActive: Boolean
        /**
         * @see jp.co.omronsoft.openwnn.WnnDictionary.setInUseState
         */
        get() = (this.mWnnWork != 0L)

    /**
     * @see jp.co.omronsoft.openwnn.WnnDictionary.setInUseState
     */
    override fun setInUseState(flag: Boolean) {
        if (flag) {
            if (mDbDic == null) {
                mDbOpenHelper =
                    OpenWnnSQLiteOpenHelper(OpenWnn.currentIme, mDicFilePath)
                mDbDic = mDbOpenHelper!!.writableDatabase
            }
        } else {
            freeDatabase()
        }
    }

    /**
     * @see jp.co.omronsoft.openwnn.WnnDictionary.clearDictionary
     */
    override fun clearDictionary(): Int {
        if (this.mWnnWork != 0L) {
            mFrequencyOffsetOfUserDictionary = -1
            mFrequencyOffsetOfLearnDictionary = -1

            return OpenWnnDictionaryImplJni.clearDictionaryParameters(this.mWnnWork)
        } else {
            return -1
        }
    }

    /**
     * @see jp.co.omronsoft.openwnn.WnnDictionary.setDictionary
     */
    override fun setDictionary(index: Int, base: Int, high: Int): Int {
        if (this.mWnnWork != 0L) {
            when (index) {
                WnnDictionary.INDEX_USER_DICTIONARY -> {
                    mFrequencyOffsetOfUserDictionary =
                        if (base < 0 || high < 0 || base > high /* || base < OFFSET_FREQUENCY_OF_USER_DICTIONARY || high >= OFFSET_FREQUENCY_OF_LEARN_DICTIONARY */
                        ) {
                            -1
                        } else {
                            high
                        }
                    return 0
                }

                WnnDictionary.INDEX_LEARN_DICTIONARY -> {
                    mFrequencyOffsetOfLearnDictionary =
                        if (base < 0 || high < 0 || base > high /* || base < OFFSET_FREQUENCY_OF_LEARN_DICTIONARY */
                        ) {
                            -1
                        } else {
                            high
                        }
                    return 0
                }

                else -> return OpenWnnDictionaryImplJni.setDictionaryParameter(
                    this.mWnnWork,
                    index,
                    base,
                    high
                )
            }
        } else {
            return -1
        }
    }

    /**
     * Query to the database
     *
     * @param keyString     The key string
     * @param wnnWord      The previous word for link search
     * @param operation    The search operation
     * @param order         The type of sort order
     */
    protected fun createQuery(keyString: String, wnnWord: WnnWord?, operation: Int, order: Int) {
        var wnnWord = wnnWord
        val newTypeOfQuery: Int
        val maxBindsOfQuery: Int
        val querySqlOrderByFreq: String?
        val querySqlOrderByKey: String?
        var queryArgs: Array<String?>?

        if (operation != WnnDictionary.SEARCH_LINK) {
            wnnWord = null
        }

        when (operation) {
            WnnDictionary.SEARCH_EXACT -> {
                querySqlOrderByFreq = mExactQuerySqlOrderByFreq
                querySqlOrderByKey = mExactQuerySqlOrderByKey
                newTypeOfQuery = 0
                queryArgs = mExactQueryArgs

                queryArgs[0] = keyString
            }

            WnnDictionary.SEARCH_PREFIX, WnnDictionary.SEARCH_LINK -> {
                /* Select the suitable parameters for the query */
                if (keyString.length <= FAST_QUERY_LENGTH) {
                    if (wnnWord != null) {
                        querySqlOrderByFreq = mFastLinkQuerySqlOrderByFreq
                        querySqlOrderByKey = mFastLinkQuerySqlOrderByKey
                        newTypeOfQuery = 1
                    } else {
                        querySqlOrderByFreq = mFastPrefixQuerySqlOrderByFreq
                        querySqlOrderByKey = mFastPrefixQuerySqlOrderByKey
                        newTypeOfQuery = 2
                    }
                    maxBindsOfQuery = FAST_QUERY_LENGTH
                    queryArgs = mFastQueryArgs
                } else {
                    if (wnnWord != null) {
                        querySqlOrderByFreq = mFullLinkQuerySqlOrderByFreq
                        querySqlOrderByKey = mFullLinkQuerySqlOrderByKey
                        newTypeOfQuery = 3
                    } else {
                        querySqlOrderByFreq = mFullPrefixQuerySqlOrderByFreq
                        querySqlOrderByKey = mFullPrefixQuerySqlOrderByKey
                        newTypeOfQuery = 4
                    }
                    maxBindsOfQuery = MAX_LENGTH_OF_QUERY
                    queryArgs = mFullQueryArgs
                }

                if (wnnWord != null) {
                    /* If link search is enabled, insert information of the previous word */
                    val queryArgsTemp = OpenWnnDictionaryImplJni.createBindArray(
                        this.mWnnWork,
                        keyString,
                        maxBindsOfQuery,
                        MAX_PATTERN_OF_APPROX
                    )

                    queryArgs = arrayOfNulls(queryArgsTemp!!.size + 2)
                    var i = 0
                    while (i < queryArgsTemp.size) {
                        queryArgs[i + 2] = queryArgsTemp[i]
                        i++
                    }

                    queryArgs[0] = wnnWord.stroke
                    queryArgs[1] = wnnWord.candidate
                } else {
                    queryArgs = OpenWnnDictionaryImplJni.createBindArray(
                        this.mWnnWork,
                        keyString,
                        maxBindsOfQuery,
                        MAX_PATTERN_OF_APPROX
                    )
                }
            }

            else -> {
                mCountCursor = 0
                freeCursor()
                return
            }
        }

        /* Create the cursor and set arguments */
        mCountCursor = 0

        if (mDbCursor == null || mTypeOfQuery != newTypeOfQuery) {
            /* If the cursor is not exist or the type of query is changed, compile the query string and query words */
            freeCursor()

            try {
                mDbCursor = when (order) {
                    WnnDictionary.ORDER_BY_FREQUENCY -> mDbDic!!.rawQuery(
                        querySqlOrderByFreq!!, queryArgs
                    ) as SQLiteCursor

                    WnnDictionary.ORDER_BY_KEY -> mDbDic!!.rawQuery(
                        querySqlOrderByKey!!, queryArgs
                    ) as SQLiteCursor

                    else -> return
                }
            } catch (e: SQLException) {
                return
            }

            mTypeOfQuery = newTypeOfQuery
        } else {
            /* If the cursor is exist, bind new arguments and re-query words (DO NOT recompile the query string) */
            try {
                mDbCursor!!.setSelectionArguments(queryArgs)
                mDbCursor!!.requery()
            } catch (e: SQLException) {
                return
            }
        }

        if (mDbCursor != null) {
            /* If querying is succeed, count the number of words */
            mCountCursor = mDbCursor!!.count
            if (mCountCursor == 0) {
                /* If no word is retrieved, deactivate the cursor for reduce the resource */
                mDbCursor!!.deactivate()
            }
        }

        return
    }

    /**
     * @see jp.co.omronsoft.openwnn.WnnDictionary.searchWord
     */
    override fun searchWord(operation: Int, order: Int, keyString: String): Int {
        /* Unset the previous word information */
        OpenWnnDictionaryImplJni.clearResult(this.mWnnWork)

        /* Search to user/learn dictionary */
        if (mDbDic != null && (mFrequencyOffsetOfUserDictionary >= 0 ||
                    mFrequencyOffsetOfLearnDictionary >= 0)
        ) {
            try {
                if (keyString.length > 0) {
                    createQuery(keyString, null, operation, order)
                    if (mDbCursor != null) {
                        mDbCursor!!.moveToFirst()
                    }
                } else {
                    /* If the key string is "", no word is retrieved */
                    if (mDbCursor != null) {
                        mDbCursor!!.deactivate()
                    }
                    mCountCursor = 0
                }
            } catch (e: SQLException) {
                if (mDbCursor != null) {
                    mDbCursor!!.deactivate()
                }
                mCountCursor = 0
            }
        } else {
            mCountCursor = 0
        }

        /* Search to fixed dictionary */
        if (this.mWnnWork != 0L) {
            var ret =
                OpenWnnDictionaryImplJni.searchWord(this.mWnnWork, operation, order, keyString)
            if (mCountCursor > 0) {
                ret = 1
            }
            return ret
        } else {
            return -1
        }
    }

    /**
     * @see jp.co.omronsoft.openwnn.WnnDictionary.searchWord
     */
    override fun searchWord(operation: Int, order: Int, keyString: String, wnnWord: WnnWord?): Int {
        if (wnnWord?.partOfSpeech == null) {
            return -1
        }

        /* Search to user/learn dictionary with link information */
        if (mDbDic != null && (mFrequencyOffsetOfUserDictionary >= 0 ||
                    mFrequencyOffsetOfLearnDictionary >= 0)
        ) {
            try {
                createQuery(keyString, wnnWord, operation, order)
                if (mDbCursor != null) {
                    mDbCursor!!.moveToFirst()
                }
            } catch (e: SQLException) {
                if (mDbCursor != null) {
                    mDbCursor!!.deactivate()
                }
                mCountCursor = 0
            }
        } else {
            mCountCursor = 0
        }

        /* Search to fixed dictionary with link information */
        OpenWnnDictionaryImplJni.clearResult(this.mWnnWork)
        OpenWnnDictionaryImplJni.setStroke(this.mWnnWork, wnnWord.stroke)
        OpenWnnDictionaryImplJni.setCandidate(this.mWnnWork, wnnWord.candidate)
        OpenWnnDictionaryImplJni.setLeftPartOfSpeech(this.mWnnWork, wnnWord.partOfSpeech!!.left)
        OpenWnnDictionaryImplJni.setRightPartOfSpeech(this.mWnnWork, wnnWord.partOfSpeech!!.right)
        OpenWnnDictionaryImplJni.selectWord(this.mWnnWork)

        if (this.mWnnWork != 0L) {
            var ret =
                OpenWnnDictionaryImplJni.searchWord(this.mWnnWork, operation, order, keyString)
            if (mCountCursor > 0) {
                ret = 1
            }
            return ret
        } else {
            return -1
        }
    }

    override val nextWord: WnnWord?
        /**
         * @see jp.co.omronsoft.openwnn.WnnDictionary.getNextWord
         */
        get() = getNextWord(0)

    /**
     * @see jp.co.omronsoft.openwnn.WnnDictionary.getNextWord
     */
    override fun getNextWord(length: Int): WnnWord? {
        if (this.mWnnWork != 0L) {
            if (mDbDic != null && mDbCursor != null && mCountCursor > 0) {
                /* If the user/learn dictionary is queried, get the result from the user/learn dictionary */
                var result: WnnWord? = WnnWord()
                try {
                    /* Skip results if that is not contained the type of search or length of stroke is not equal specified length */
                    while (mCountCursor > 0 &&
                        ((mFrequencyOffsetOfUserDictionary < 0 && mDbCursor!!.getInt(4) == TYPE_NAME_USER) ||
                                (mFrequencyOffsetOfLearnDictionary < 0 && mDbCursor!!.getInt(4) == TYPE_NAME_LEARN) ||
                                (length > 0 && mDbCursor!!.getString(0).length != length))
                    ) {
                        mDbCursor!!.moveToNext()
                        mCountCursor--
                    }

                    if (mCountCursor > 0) {
                        /* Get the information of word */
                        result!!.stroke = mDbCursor!!.getString(0)
                        result.candidate = mDbCursor!!.getString(1)
                        result.partOfSpeech!!.left = mDbCursor!!.getInt(2)
                        result.partOfSpeech!!.right = mDbCursor!!.getInt(3)

                        if (mDbCursor!!.getInt(4) == TYPE_NAME_USER) {
                            result.frequency = mFrequencyOffsetOfUserDictionary
                        } else {
                            result.frequency = mFrequencyOffsetOfLearnDictionary
                        }

                        /* Move cursor to next result. If the next result is not exist, deactivate the cursor */
                        mDbCursor!!.moveToNext()
                        if (--mCountCursor <= 0) {
                            mDbCursor!!.deactivate()
                        }

                        return result
                    } else {
                        /* if no result is found, terminate the searching of user/learn dictionary */
                        mDbCursor!!.deactivate()
                        result = null
                    }
                } catch (e: SQLException) {
                    mDbCursor!!.deactivate()
                    mCountCursor = 0
                    result = null
                }
            }

            /* Get the result from fixed dictionary */
            val res = OpenWnnDictionaryImplJni.getNextWord(this.mWnnWork, length)
            if (res > 0) {
                val result = WnnWord()
                if (result != null) {
                    result.stroke = OpenWnnDictionaryImplJni.getStroke(this.mWnnWork)
                    result.candidate = OpenWnnDictionaryImplJni.getCandidate(this.mWnnWork)
                    result.frequency = OpenWnnDictionaryImplJni.getFrequency(this.mWnnWork)
                    result.partOfSpeech!!.left =
                        OpenWnnDictionaryImplJni.getLeftPartOfSpeech(this.mWnnWork)
                    result.partOfSpeech!!.right =
                        OpenWnnDictionaryImplJni.getRightPartOfSpeech(this.mWnnWork)
                }
                return result
            } else if (res == 0) {
                /* No result is found. */
                return null
            } else {
                /* An error occur (It is regarded as "No result is found".) */
                return null
            }
        } else {
            return null
        }
    }

    override val userDictionaryWords: Array<WnnWord?>?
        /**
         * @see jp.co.omronsoft.openwnn.WnnDictionary.getUserDictionaryWords
         */
        get() {
            if (this.mWnnWork != 0L && mDbDic != null) {
                val numOfWords: Int
                var i: Int
                var cursor: SQLiteCursor? = null

                try {
                    /* Count all words in the user dictionary */
                    cursor = mDbDic!!.query(
                        TABLE_NAME_DIC,
                        arrayOf(
                            COLUMN_NAME_STROKE,
                            COLUMN_NAME_CANDIDATE
                        ),
                        String.format(
                            "%s=%d",
                            COLUMN_NAME_TYPE,
                            TYPE_NAME_USER
                        ),
                        null, null, null, null
                    ) as SQLiteCursor
                    numOfWords = cursor.count

                    if (numOfWords > 0) {
                        /* Retrieve all words in the user dictionary */
                        val words = mutableListOf<WnnWord>()

                        cursor.moveToFirst()
                        i = 0
                        while (i < numOfWords) {
                            val word = WnnWord()
                            word.stroke = cursor.getString(0)
                            word.candidate = cursor.getString(1)
                            words += word
                            cursor.moveToNext()
                            i++
                        }

                        return words.toTypedArray()
                    }
                } catch (e: SQLException) {
                    /* An error occurs */
                    return null
                } finally {
                    cursor?.close()
                }
            }
            return null
        }

    /**
     * @see jp.co.omronsoft.openwnn.WnnDictionary.clearApproxPattern
     */
    override fun clearApproxPattern() {
        if (this.mWnnWork != 0L) {
            OpenWnnDictionaryImplJni.clearApproxPatterns(this.mWnnWork)
        }
    }

    /**
     * @see jp.co.omronsoft.openwnn.WnnDictionary.setApproxPattern
     */
    override fun setApproxPattern(src: String?, dst: String?): Int {
        return if (this.mWnnWork != 0L) {
            OpenWnnDictionaryImplJni.setApproxPattern(this.mWnnWork, src, dst)
        } else {
            -1
        }
    }

    /**
     * @see jp.co.omronsoft.openwnn.WnnDictionary.setApproxPattern
     */
    override fun setApproxPattern(approxPattern: Int): Int {
        return if (this.mWnnWork != 0L) {
            OpenWnnDictionaryImplJni.setApproxPattern(this.mWnnWork, approxPattern)
        } else {
            -1
        }
    }

    override val connectMatrix: Array<ByteArray?>?
        /**
         * @see jp.co.omronsoft.openwnn.WnnDictionary.getConnectMatrix
         */
        get() {
            val result: Array<ByteArray?>
            val lcount: Int
            var i: Int

            if (this.mWnnWork != 0L) {
                /* 1-origin */
                lcount = OpenWnnDictionaryImplJni.getNumberOfLeftPOS(this.mWnnWork)
                result = arrayOfNulls(lcount + 1)

                if (result != null) {
                    i = 0
                    while (i < lcount + 1) {
                        result[i] =
                            OpenWnnDictionaryImplJni.getConnectArray(
                                this.mWnnWork,
                                i
                            )

                        if (result[i] == null) {
                            return null
                        }
                        i++
                    }
                }
            } else {
                result = Array(1) { ByteArray(1) }
            }
            return result
        }

    /**
     * @see jp.co.omronsoft.openwnn.WnnDictionary.getPOS
     */
    override fun getPOS(type: Int): WnnPOS? {
        val result = WnnPOS()

        if (this.mWnnWork != 0L && result != null) {
            result.left =
                OpenWnnDictionaryImplJni.getLeftPartOfSpeechSpecifiedType(this.mWnnWork, type)
            result.right =
                OpenWnnDictionaryImplJni.getRightPartOfSpeechSpecifiedType(this.mWnnWork, type)

            if (result.left < 0 || result.right < 0) {
                return null
            }
        }
        return result
    }

    /**
     * @see jp.co.omronsoft.openwnn.WnnDictionary.clearUserDictionary
     */
    override fun clearUserDictionary(): Int {
        if (mDbDic != null) {
            mDbDic!!.execSQL(
                String.format(
                    "delete from %s where %s=%d",
                    TABLE_NAME_DIC,
                    COLUMN_NAME_TYPE,
                    TYPE_NAME_USER
                )
            )
        }

        /* If no writable dictionary exists, no error occurs. */
        return 0
    }

    /**
     * @see jp.co.omronsoft.openwnn.WnnDictionary.clearLearnDictionary
     */
    override fun clearLearnDictionary(): Int {
        if (mDbDic != null) {
            mDbDic!!.execSQL(
                String.format(
                    "delete from %s where %s=%d",
                    TABLE_NAME_DIC,
                    COLUMN_NAME_TYPE,
                    TYPE_NAME_LEARN
                )
            )
        }


        /* If no writable dictionary exists, no error occurs. */
        return 0
    }

    /**
     * @see jp.co.omronsoft.openwnn.WnnDictionary.addWordToUserDictionary
     */
    override fun addWordToUserDictionary(word: Array<WnnWord?>): Int {
        var result = 0

        if (mDbDic != null) {
            var cursor: SQLiteCursor?

            /* Count all words in the user dictionary */
            cursor = mDbDic!!.query(
                TABLE_NAME_DIC,
                arrayOf(COLUMN_NAME_ID),
                String.format("%s=%d", COLUMN_NAME_TYPE, TYPE_NAME_USER),
                null, null, null, null
            ) as SQLiteCursor

            val count = cursor.count
            cursor.close()

            if (count + word.size > MAX_WORDS_IN_USER_DICTIONARY) {
                /* If user dictionary is full, an error occurs. */
                return -1
            } else {
                mDbDic!!.beginTransaction()
                try {
                    val strokeSQL = StringBuilder()
                    val candidateSQL = StringBuilder()

                    for (index in word.indices) {
                        if (word[index]!!.stroke!!.length > 0 && word[index]!!.stroke!!.length <= MAX_STROKE_LENGTH && word[index]!!.candidate!!.length > 0 && word[index]!!.candidate!!.length <= MAX_CANDIDATE_LENGTH) {
                            strokeSQL.setLength(0)
                            candidateSQL.setLength(0)
                            DatabaseUtils.appendEscapedSQLString(strokeSQL, word[index]!!.stroke)
                            DatabaseUtils.appendEscapedSQLString(
                                candidateSQL,
                                word[index]!!.candidate
                            )

                            cursor = mDbDic!!.query(
                                TABLE_NAME_DIC,
                                arrayOf(COLUMN_NAME_ID),
                                String.format(
                                    "%s=%d and %s=%s and %s=%s",
                                    COLUMN_NAME_TYPE, TYPE_NAME_USER,
                                    COLUMN_NAME_STROKE, strokeSQL.toString(),
                                    COLUMN_NAME_CANDIDATE, candidateSQL.toString()
                                ),
                                null, null, null, null
                            ) as SQLiteCursor

                            if (cursor.count > 0) {
                                /* if the specified word is exist, an error reported and skipped that word. */
                                result = -2
                            } else {
                                val content = ContentValues()

                                content.clear()
                                content.put(COLUMN_NAME_TYPE, TYPE_NAME_USER)
                                content.put(COLUMN_NAME_STROKE, word[index]!!.stroke)
                                content.put(COLUMN_NAME_CANDIDATE, word[index]!!.candidate)
                                content.put(COLUMN_NAME_POS_LEFT, word[index]!!.partOfSpeech!!.left)
                                content.put(
                                    COLUMN_NAME_POS_RIGHT,
                                    word[index]!!.partOfSpeech!!.right
                                )

                                mDbDic!!.insert(TABLE_NAME_DIC, null, content)
                            }

                            cursor.close()
                            cursor = null
                        }
                    }
                    mDbDic!!.setTransactionSuccessful()
                } catch (e: SQLException) {
                    /* An error occurs */
                    return -1
                } finally {
                    mDbDic!!.endTransaction()
                    if (cursor != null) {
                        cursor.close()
                    }
                }
            }
        }

        /* If no writable dictionary exists, no error occurs. */
        return result
    }

    /**
     * @see jp.co.omronsoft.openwnn.WnnDictionary.addWordToUserDictionary
     */
    override fun addWordToUserDictionary(word: WnnWord?): Int {
        val words = arrayOfNulls<WnnWord>(1)
        words[0] = word

        return addWordToUserDictionary(words)
    }

    /**
     * @see jp.co.omronsoft.openwnn.WnnDictionary.removeWordFromUserDictionary
     */
    override fun removeWordFromUserDictionary(word: Array<WnnWord?>): Int {
        if (mDbDic != null) {
            /* Remove the specified word */
            mDbDic!!.beginTransaction()
            try {
                val strokeSQL = StringBuilder()
                val candidateSQL = StringBuilder()

                for (index in word.indices) {
                    if (word[index]!!.stroke!!.length > 0 && word[index]!!.stroke!!.length <= MAX_STROKE_LENGTH && word[index]!!.candidate!!.length > 0 && word[index]!!.candidate!!.length <= MAX_CANDIDATE_LENGTH) {
                        strokeSQL.setLength(0)
                        candidateSQL.setLength(0)
                        DatabaseUtils.appendEscapedSQLString(strokeSQL, word[index]!!.stroke)
                        DatabaseUtils.appendEscapedSQLString(candidateSQL, word[index]!!.candidate)

                        mDbDic!!.delete(
                            TABLE_NAME_DIC,
                            String.format(
                                "%s=%d and %s=%s and %s=%s",
                                COLUMN_NAME_TYPE, TYPE_NAME_USER,
                                COLUMN_NAME_STROKE, strokeSQL,
                                COLUMN_NAME_CANDIDATE, candidateSQL
                            ),
                            null
                        )
                    }
                }
                mDbDic!!.setTransactionSuccessful()
            } catch (e: SQLException) {
                /* An error occurs */
                return -1
            } finally {
                mDbDic!!.endTransaction()
            }
        }

        /* If no writable dictionary exists, no error occurs. */
        return 0
    }

    /**
     * @see jp.co.omronsoft.openwnn.WnnDictionary.removeWordFromUserDictionary
     */
    override fun removeWordFromUserDictionary(word: WnnWord?): Int {
        val words = arrayOfNulls<WnnWord>(1)
        words[0] = word

        return removeWordFromUserDictionary(words)
    }

    /**
     * @see jp.co.omronsoft.openwnn.WnnDictionary.learnWord
     */
    override fun learnWord(word: WnnWord): Int {
        return learnWord(word, null)
    }

    /**
     * Learn the word with connection.
     *
     * @param word              The word to learn
     * @param previousWord      The word which is selected previously.
     * @return                  0 if success; minus value if fail.
     */
    override fun learnWord(word: WnnWord, previousWord: WnnWord?): Int {
        if (mDbDic != null) {
            val previousStrokeSQL = StringBuilder()
            val previousCandidateSQL = StringBuilder()

            if (previousWord != null && previousWord.stroke!!.length > 0 && previousWord.stroke!!.length <= MAX_STROKE_LENGTH && previousWord.candidate!!.length > 0 && previousWord.candidate!!.length <= MAX_CANDIDATE_LENGTH) {
                DatabaseUtils.appendEscapedSQLString(previousStrokeSQL, previousWord.stroke)
                DatabaseUtils.appendEscapedSQLString(previousCandidateSQL, previousWord.candidate)
                /* If the information of previous word is set, perform the link learning */
            }

            if (word.stroke!!.length > 0 && word.stroke!!.length <= MAX_STROKE_LENGTH && word.candidate!!.length > 0 && word.candidate!!.length <= MAX_CANDIDATE_LENGTH) {
                val strokeSQL = StringBuilder()
                val candidateSQL = StringBuilder()
                DatabaseUtils.appendEscapedSQLString(strokeSQL, word.stroke)
                DatabaseUtils.appendEscapedSQLString(candidateSQL, word.candidate)

                /* Count the number of registered words and retrieve that words ascending by the ID */
                val cursor = mDbDic!!.query(
                    TABLE_NAME_DIC,
                    arrayOf(
                        COLUMN_NAME_STROKE,
                        COLUMN_NAME_CANDIDATE
                    ),
                    String.format(
                        "%s=%d",
                        COLUMN_NAME_TYPE,
                        TYPE_NAME_LEARN
                    ),
                    null, null, null,
                    String.format("%s ASC", COLUMN_NAME_ID)
                ) as SQLiteCursor

                if (cursor.count >= MAX_WORDS_IN_LEARN_DICTIONARY) {
                    /* If a registering space is short, delete the words that contain same stroke and candidate to the oldest word */
                    mDbDic!!.beginTransaction()
                    try {
                        cursor.moveToFirst()

                        val oldestStrokeSQL = StringBuilder()
                        val oldestCandidateSQL = StringBuilder()
                        DatabaseUtils.appendEscapedSQLString(oldestStrokeSQL, cursor.getString(0))
                        DatabaseUtils.appendEscapedSQLString(
                            oldestCandidateSQL,
                            cursor.getString(1)
                        )

                        mDbDic!!.delete(
                            TABLE_NAME_DIC,
                            String.format(
                                "%s=%d and %s=%s and %s=%s",
                                COLUMN_NAME_TYPE, TYPE_NAME_LEARN,
                                COLUMN_NAME_STROKE, oldestStrokeSQL.toString(),
                                COLUMN_NAME_CANDIDATE, oldestCandidateSQL.toString()
                            ),
                            null
                        )

                        mDbDic!!.setTransactionSuccessful()
                    } catch (e: SQLException) {
                        return -1
                    } finally {
                        mDbDic!!.endTransaction()
                        cursor.close()
                    }
                } else {
                    cursor.close()
                }


                /* learning the word */
                val content = ContentValues()

                content.clear()
                content.put(COLUMN_NAME_TYPE, TYPE_NAME_LEARN)
                content.put(COLUMN_NAME_STROKE, word.stroke)
                content.put(COLUMN_NAME_CANDIDATE, word.candidate)
                content.put(COLUMN_NAME_POS_LEFT, word.partOfSpeech!!.left)
                content.put(COLUMN_NAME_POS_RIGHT, word.partOfSpeech!!.right)
                if (previousWord != null) {
                    content.put(COLUMN_NAME_PREVIOUS_STROKE, previousWord.stroke)
                    content.put(COLUMN_NAME_PREVIOUS_CANDIDATE, previousWord.candidate)
                    content.put(COLUMN_NAME_PREVIOUS_POS_LEFT, previousWord.partOfSpeech!!.left)
                    content.put(COLUMN_NAME_PREVIOUS_POS_RIGHT, previousWord.partOfSpeech!!.right)
                }

                mDbDic!!.beginTransaction()
                try {
                    mDbDic!!.insert(TABLE_NAME_DIC, null, content)
                    mDbDic!!.setTransactionSuccessful()
                } catch (e: SQLException) {
                    mDbDic!!.endTransaction()
                    return -1
                } finally {
                    mDbDic!!.endTransaction()
                }
            }
        }

        /* If no writable dictionary exists, no error occurs. */
        return 0
    }

    companion object {
        /*
     * DEFINITION FOR JNI
     */
        init {
            /* Load the dictionary search library */
            System.loadLibrary("wnndict")
        }

        /*
     * DEFINITION OF CONSTANTS
     */
        /** The maximum length of stroke  */
        const val MAX_STROKE_LENGTH: Int = 50

        /** The maximum length of candidate  */
        const val MAX_CANDIDATE_LENGTH: Int = 50

        /** The table name of writable dictionary on the database  */
        protected const val TABLE_NAME_DIC: String = "dic"

        /** The type name of user word  */
        protected const val TYPE_NAME_USER: Int = 0

        /** The type name of learn word  */
        protected const val TYPE_NAME_LEARN: Int = 1

        /** The column name of database  */
        protected const val COLUMN_NAME_ID: String = "rowid"

        /** The column name of database   */
        protected const val COLUMN_NAME_TYPE: String = "type"

        /** The column name of database   */
        protected const val COLUMN_NAME_STROKE: String = "stroke"

        /** The column name of database   */
        protected const val COLUMN_NAME_CANDIDATE: String = "candidate"

        /** The column name of database   */
        protected const val COLUMN_NAME_POS_LEFT: String = "posLeft"

        /** The column name of database   */
        protected const val COLUMN_NAME_POS_RIGHT: String = "posRight"

        /** The column name of database   */
        protected const val COLUMN_NAME_PREVIOUS_STROKE: String = "prevStroke"

        /** The column name of database   */
        protected const val COLUMN_NAME_PREVIOUS_CANDIDATE: String = "prevCandidate"

        /** The column name of database   */
        protected const val COLUMN_NAME_PREVIOUS_POS_LEFT: String = "prevPosLeft"

        /** The column name of database   */
        protected const val COLUMN_NAME_PREVIOUS_POS_RIGHT: String = "prevPosRight"

        /** Query for normal search  */
        protected const val NORMAL_QUERY: String = "select distinct " + COLUMN_NAME_STROKE + "," +
                COLUMN_NAME_CANDIDATE + "," +
                COLUMN_NAME_POS_LEFT + "," +
                COLUMN_NAME_POS_RIGHT + "," +
                COLUMN_NAME_TYPE +
                " from " + TABLE_NAME_DIC + " where %s order by " +
                COLUMN_NAME_TYPE + " DESC, %s"

        /** Query for link search  */
        protected const val LINK_QUERY: String = "select distinct " + COLUMN_NAME_STROKE + "," +
                COLUMN_NAME_CANDIDATE + "," +
                COLUMN_NAME_POS_LEFT + "," +
                COLUMN_NAME_POS_RIGHT + "," +
                COLUMN_NAME_TYPE +
                " from " + TABLE_NAME_DIC + " where %s = ? and %s = ? and %s order by " +
                COLUMN_NAME_TYPE + " DESC, %s"

        /** The max words of user dictionary  */
        protected const val MAX_WORDS_IN_USER_DICTIONARY: Int = 100

        /** The max words of learning dictionary  */
        protected const val MAX_WORDS_IN_LEARN_DICTIONARY: Int = 2000

        /** The base frequency of user dictionary  */
        protected const val OFFSET_FREQUENCY_OF_USER_DICTIONARY: Int = 1000

        /** The base frequency of learning dictionary  */
        protected const val OFFSET_FREQUENCY_OF_LEARN_DICTIONARY: Int = 2000

        /*
     * Constants to define the upper limit of query.
     *
     * That is used to fix the size of query expression.
     * If the number of approximate patterns for a character is exceeded MAX_PATTERN_OF_APPROX,
     * increase that constant to the maximum number of patterns.
     */
        /** Constants to define the upper limit of approximate patterns  */
        protected const val MAX_PATTERN_OF_APPROX: Int = 6

        /** Constants to define the upper limit of length of a query  */
        protected const val MAX_LENGTH_OF_QUERY: Int = 50

        /**
         * Constants to define the turn around time of query.
         * <br></br>
         * It can be set between 1 to `MAX_LENGTH_OF_QUERY`. If the length of query
         * string is shorter than `FAST_QUERY_LENGTH`, the simple search logic is applied.
         * Therefore, the turn around time for short query string is fast so that it is short.
         * However, the difference of turn around time at the border length grows big.
         * the value should be fixed carefully.
         */
        protected const val FAST_QUERY_LENGTH: Int = 20
    }
}
