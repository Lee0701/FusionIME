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
 * The implementation class of JNI wrapper for dictionary.
 *
 * @author Copyright (C) 2008, 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
object OpenWnnDictionaryImplJni {
    /*
     * DEFINITION OF CONSTANTS
     */
    /**
     * Constant about the approximate pattern (for JNI native library)
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.APPROX_PATTERN_EN_TOUPPER
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.setApproxPattern
     */
    val APPROX_PATTERN_EN_TOUPPER: Int = WnnDictionary.APPROX_PATTERN_EN_TOUPPER

    /**
     * Constant about the approximate pattern (for JNI native library)
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.APPROX_PATTERN_EN_TOLOWER
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.setApproxPattern
     */
    val APPROX_PATTERN_EN_TOLOWER: Int = WnnDictionary.APPROX_PATTERN_EN_TOLOWER

    /**
     * Constant about the approximate pattern (for JNI native library)
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.APPROX_PATTERN_EN_QWERTY_NEAR
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.setApproxPattern
     */
    val APPROX_PATTERN_EN_QWERTY_NEAR: Int = WnnDictionary.APPROX_PATTERN_EN_QWERTY_NEAR

    /**
     * Constant about the approximate pattern (for JNI native library)
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.APPROX_PATTERN_EN_QWERTY_NEAR_UPPER
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.setApproxPattern
     */
    val APPROX_PATTERN_EN_QWERTY_NEAR_UPPER: Int =
        WnnDictionary.APPROX_PATTERN_EN_QWERTY_NEAR_UPPER

    /**
     * Constant about the approximate pattern (for JNI native library)
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.APPROX_PATTERN_JAJP_12KEY_NORMAL
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.setApproxPattern
     */
    val APPROX_PATTERN_JAJP_12KEY_NORMAL: Int =
        WnnDictionary.APPROX_PATTERN_JAJP_12KEY_NORMAL

    /**
     * Constant about the search operation (for JNI native library)
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.SEARCH_EXACT
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.searchWord
     */
    val SEARCH_EXACT: Int = WnnDictionary.SEARCH_EXACT

    /**
     * Constant about the search operation (for JNI native library)
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.SEARCH_PREFIX
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.searchWord
     */
    val SEARCH_PREFIX: Int = WnnDictionary.SEARCH_PREFIX

    /**
     * Constant about the search operation (for JNI native library)
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.SEARCH_LINK
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.searchWord
     */
    val SEARCH_LINK: Int = WnnDictionary.SEARCH_LINK

    /**
     * Constant about the sort order (for JNI native library)
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.ORDER_BY_FREQUENCY
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.searchWord
     */
    val ORDER_BY_FREQUENCY: Int = WnnDictionary.ORDER_BY_FREQUENCY

    /**
     * Constant about the sort order (for JNI native library)
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.ORDER_BY_KEY
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.searchWord
     */
    val ORDER_BY_KEY: Int = WnnDictionary.ORDER_BY_KEY

    /**
     * Type of a part of speech (for JNI native library)
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_V1
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getLeftPartOfSpeechSpecifiedType
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getRightPartOfSpeechSpecifiedType
     */
    val POS_TYPE_V1: Int = WnnDictionary.POS_TYPE_V1

    /**
     * Type of a part of speech (for JNI native library)
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_V2
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getLeftPartOfSpeechSpecifiedType
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getRightPartOfSpeechSpecifiedType
     */
    val POS_TYPE_V2: Int = WnnDictionary.POS_TYPE_V2

    /**
     * Type of a part of speech (for JNI native library)
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_V3
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getLeftPartOfSpeechSpecifiedType
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getRightPartOfSpeechSpecifiedType
     */
    val POS_TYPE_V3: Int = WnnDictionary.POS_TYPE_V3

    /**
     * Type of a part of speech (for JNI native library)
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_BUNTOU
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getLeftPartOfSpeechSpecifiedType
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getRightPartOfSpeechSpecifiedType
     */
    val POS_TYPE_BUNTOU: Int = WnnDictionary.POS_TYPE_BUNTOU

    /**
     * Type of a part of speech (for JNI native library)
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_TANKANJI
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getLeftPartOfSpeechSpecifiedType
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getRightPartOfSpeechSpecifiedType
     */
    val POS_TYPE_TANKANJI: Int = WnnDictionary.POS_TYPE_TANKANJI

    /**
     * Type of a part of speech (for JNI native library)
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_SUUJI
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getLeftPartOfSpeechSpecifiedType
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getRightPartOfSpeechSpecifiedType
     */
    val POS_TYPE_SUUJI: Int = WnnDictionary.POS_TYPE_SUUJI

    /**
     * Type of a part of speech (for JNI native library)
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_MEISI
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getLeftPartOfSpeechSpecifiedType
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getRightPartOfSpeechSpecifiedType
     */
    val POS_TYPE_MEISI: Int = WnnDictionary.POS_TYPE_MEISI

    /**
     * Type of a part of speech (for JNI native library)
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_JINMEI
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getLeftPartOfSpeechSpecifiedType
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getRightPartOfSpeechSpecifiedType
     */
    val POS_TYPE_JINMEI: Int = WnnDictionary.POS_TYPE_JINMEI

    /**
     * Type of a part of speech (for JNI native library)
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_CHIMEI
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getLeftPartOfSpeechSpecifiedType
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getRightPartOfSpeechSpecifiedType
     */
    val POS_TYPE_CHIMEI: Int = WnnDictionary.POS_TYPE_CHIMEI

    /**
     * Type of a part of speech (for JNI native library)
     * @see jp.co.omronsoft.openwnn.WnnDictionary.POS_TYPE_KIGOU
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getLeftPartOfSpeechSpecifiedType
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getRightPartOfSpeechSpecifiedType
     */
    val POS_TYPE_KIGOU: Int = WnnDictionary.POS_TYPE_KIGOU

    /*
     * METHODS
     */
    /**
     * Create a internal work area.
     * A internal work area is allocated dynamically, and the specified dictionary library is loaded.
     *
     * @param dicLibPath    The path of the dictionary library file
     * @return              The internal work area or null
     */
    external fun createWnnWork(dicLibPath: String?): Long

    /**
     * Free the internal work area.
     * The specified work area and the loaded dictionary library is free.
     *
     * @param work      The internal work area
     * @return          0 if processing is successful; <0 if an error occur
     */
    external fun freeWnnWork(work: Long): Int

    /**
     * Clear all dictionary information.
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.clearDictionary
     *
     * @param work      The internal work area
     * @return          0 if processing is successful; <0 if an error occur
     */
    external fun clearDictionaryParameters(work: Long): Int

    /**
     * Set a dictionary information.
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.setDictionary
     *
     * @param work      The internal work area
     * @param index     The index of dictionary
     * @param base      The base frequency or -1
     * @param high      The maximum frequency or -1
     * @return           0 if processing is successful; <0 otherwise
     */
    external fun setDictionaryParameter(work: Long, index: Int, base: Int, high: Int): Int

    /**
     * Search a word from dictionaries.
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.searchWord
     *
     * @param work          The internal work area
     * @param operation     The search operation (see "Constant about the search operation")
     * @see jp.co.omronsoft.openwnn.WnnDictionary.SEARCH_EXACT
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.SEARCH_PREFIX
     *
     * @param order         The sort order (see "Constant about the sort order")
     * @see jp.co.omronsoft.openwnn.WnnDictionary.ORDER_BY_FREQUENCY
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.ORDER_BY_KEY
     *
     * @param keyString     The key string
     * @return              0 if no result is found; 1 if a result is found; <0 if an error occur
     */
    external fun searchWord(work: Long, operation: Int, order: Int, keyString: String?): Int

    /**
     * Retrieve a word information.
     * A word information is stored to the internal work area. To retrieve a detail information,
     * use `getStroke()`, `getCandidate()`, `getFreqeuency(),` or other `get...()` method.
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.getNextWord
     *
     * @param work      The internal work area
     * @param length    >0 if only the result of specified length is retrieved; 0 if no condition exist
     * @return          0 if no result is retrieved; >0 if a result is retrieved; <0 if an error occur
     */
    external fun getNextWord(work: Long, length: Int): Int

    /**
     * Retrieve the key string from the current word information.
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getNextWord
     *
     * @param work      The internal work area
     * @return          The Key string
     */
    external fun getStroke(work: Long): String?

    /**
     * Retrieve the candidate string from the current word information.
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getNextWord
     *
     * @param work      The internal work area
     * @return          The candidate string
     */
    external fun getCandidate(work: Long): String?

    /**
     * Retrieve the frequency from the current word information.
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.getNextWord
     *
     * @param work      The internal work area
     * @return          The frequency
     */
    external fun getFrequency(work: Long): Int

    /**
     * Retrieve the part of speech at left side from the current word information.
     *
     * @param work      The internal work area
     * @return          The part of speech
     */
    external fun getLeftPartOfSpeech(work: Long): Int

    /**
     * Retrieve the part of speech at right side from the current word information.
     *
     * @param work      The internal work area
     * @return          The part of speech
     */
    external fun getRightPartOfSpeech(work: Long): Int

    /**
     * Clear approximate patterns.
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.clearApproxPattern
     *
     * @param work      The internal work area.
     */
    external fun clearApproxPatterns(work: Long)

    /**
     * Set a approximate pattern.
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.setApproxPattern
     *
     * @param work      The internal work area
     * @param src       The string (before)
     * @param dst       The string (after)
     * @return          0 if processing is successful; <0 if an error occur
     */
    external fun setApproxPattern(work: Long, src: String?, dst: String?): Int

    /**
     * Set a predefined approximate pattern.
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.setApproxPattern
     *
     * @param work              The internal work area
     * @param approxPattern     The index of predefined approximate pattern (See "Constant about the approximate pattern")
     * @see jp.co.omronsoft.openwnn.WnnDictionary.APPROX_PATTERN_EN_TOUPPER
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.APPROX_PATTERN_EN_TOLOWER
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.APPROX_PATTERN_EN_QWERTY_NEAR
     *
     * @see jp.co.omronsoft.openwnn.WnnDictionary.APPROX_PATTERN_EN_QWERTY_NEAR_UPPER
     *
     * @return                  0 if processing is successful; <0 if an error occur
     */
    external fun setApproxPattern(work: Long, approxPattern: Int): Int

    /**
     * Get the specified approximate pattern.
     * @param work      The internal work area
     * @param src       The string (before)
     * @return          The string array (after)
     */
    external fun getApproxPattern(work: Long, src: String?): Array<String?>?

    /**
     * Clear the current word information.
     *
     * @param work      The internal work area
     */
    external fun clearResult(work: Long)

    /**
     * Set the part of speech at left side to the current word information.
     *
     * @param work          The internal work area
     * @param partOfSpeech  The part of speech
     * @return              0 if processing is successful; <0 if an error occur
     */
    external fun setLeftPartOfSpeech(work: Long, partOfSpeech: Int): Int

    /**
     * Set the part of speech at right side to the current word information.
     *
     * @param work          The internal work area
     * @param partOfSpeech  The part of speech
     * @return              0 if processing is successful; <0 if an error occur
     */
    external fun setRightPartOfSpeech(work: Long, partOfSpeech: Int): Int

    /**
     * Set the key string to the current word information.
     *
     * @param work          The internal work area
     * @param stroke        The key string
     * @return              0 if processing is successful; <0 if an error occur
     */
    external fun setStroke(work: Long, stroke: String?): Int

    /**
     * Set the candidate string to the current word information.
     *
     * @param work          The internal work area
     * @param candidate     The candidate string
     * @return              0 if processing is successful; <0 if an error occur
     */
    external fun setCandidate(work: Long, candidate: String?): Int

    /**
     * Set the previous word information from the current word information.
     *
     * @param work          The internal work area
     * @return              0 if processing is successful; <0 if an error occur
     */
    external fun selectWord(work: Long): Int

    /**
     * Retrieve the connect array
     *
     * @param work                  The internal work area
     * @param leftPartOfSpeech      The part of speech at left side
     * @return                      The connect array
     */
    external fun getConnectArray(work: Long, leftPartOfSpeech: Int): ByteArray?

    /**
     * Retrieve the number of the part of speeches at left side.
     *
     * @return              The number
     */
    external fun getNumberOfLeftPOS(work: Long): Int

    /**
     * Retrieve the number of the part of speeches at right side.
     *
     * @return              The number
     */
    external fun getNumberOfRightPOS(work: Long): Int

    /**
     * Retrieve the specified part of speech at left side.
     *
     * @param work          The internal work area
     * @param type          The type of a part of speech
     * @return              0 if type is not found; <0 if an error occur; >0 The part of speech
     */
    external fun getLeftPartOfSpeechSpecifiedType(work: Long, type: Int): Int

    /**
     * Retrieve the specified part of speech at right side.
     *
     * @param work          The internal work area
     * @param type          The type of a part of speech
     * @return              0 if type is not found; <0 if an error occur; >0 The part of speech
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.POS_TYPE_V1
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.POS_TYPE_V2
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.POS_TYPE_V3
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.POS_TYPE_BUNTOU
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.POS_TYPE_TANKANJI
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.POS_TYPE_SUUJI
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.POS_TYPE_MEISI
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.POS_TYPE_JINMEI
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.POS_TYPE_CHIMEI
     *
     * @see jp.co.omronsoft.openwnn.OpenWnnDictionaryImplJni.POS_TYPE_KIGOU
     */
    external fun getRightPartOfSpeechSpecifiedType(work: Long, type: Int): Int

    /**
     * Create the string array that is used by operation of query
     *
     * @param work                  The internal work area
     * @param keyString             The key string
     * @param maxBindsOfQuery       The maximum number of binds of query
     * @param maxPatternOfApprox    The maximum number of approximate patterns per character
     * @return                     The string array for binding
     */
    external fun createBindArray(
        work: Long,
        keyString: String?,
        maxBindsOfQuery: Int,
        maxPatternOfApprox: Int
    ): Array<String?>?

    /**
     * Create the string which used query parameter
     *
     * @param work                  The internal work area
     * @param maxBindsOfQuery       The maximum number of binds of query
     * @param maxPatternOfApprox    The maximum number of approximate patterns per character
     * @param keyColumnName        The name of the key column
     * @return                     The string for querying
     */
    external fun createQueryStringBase(
        work: Long,
        maxBindsOfQuery: Int,
        maxPatternOfApprox: Int,
        keyColumnName: String?
    ): String?
}
