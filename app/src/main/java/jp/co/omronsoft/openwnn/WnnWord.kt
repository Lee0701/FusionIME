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
 * The container class of a word.
 *
 * @author Copyright (C) 2008-2009, OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
open class WnnWord
/**
 * Constructor
 *
 * @param id            The ID of word
 * @param candidate     The string of word
 * @param stroke        The reading of word
 * @param posTag        The part of speech of word
 * @param frequency     The score of word
 */ @JvmOverloads constructor(
    /** The word's Id  */
    var id: Int = 0,
    /** The string of this word.  */
    var candidate: String? = "",
    /** The reading of this word.  */
    var stroke: String? = "",
    /** The part of speech this word.  */
    var partOfSpeech: WnnPOS? = WnnPOS(),
    /** The score of this word.  */
    var frequency: Int = 0,
    /** The attribute of this word when it is assumed a candidate.  */
    var attribute: Int = 0
) {
    /**
     * Constructor
     *
     * @param candidate     The string of word
     * @param stroke        The reading of word
     */
    constructor(candidate: String?, stroke: String?) : this(0, candidate, stroke, WnnPOS(), 0, 0)

    /**
     * Constructor
     *
     * @param candidate     The string of word
     * @param stroke        The reading of word
     * @param frequency     The score of word
     */
    constructor(candidate: String?, stroke: String?, frequency: Int) : this(
        0,
        candidate,
        stroke,
        WnnPOS(),
        frequency,
        0
    )

    /**
     * Constructor
     *
     * @param candidate     The string of word
     * @param stroke        The reading of word
     * @param posTag        The part of speech of word
     */
    constructor(candidate: String?, stroke: String?, posTag: WnnPOS?) : this(
        0,
        candidate,
        stroke,
        posTag,
        0,
        0
    )

    /**
     * Constructor
     *
     * @param candidate     The string of word
     * @param stroke        The reading of word
     * @param posTag        The part of speech of word
     * @param frequency     The score of word
     */
    constructor(candidate: String?, stroke: String?, posTag: WnnPOS?, frequency: Int) : this(
        0,
        candidate,
        stroke,
        posTag,
        frequency,
        0
    )

    /**
     * Constructor
     *
     * @param id            The ID of word
     * @param candidate     The string of word
     * @param stroke        The reading of word
     * @param partOfSpeech        The part of speech of word
     * @param frequency     The score of word
     * @param attribute     The attribute of word
     */
    /**
     * Constructor
     */
}

