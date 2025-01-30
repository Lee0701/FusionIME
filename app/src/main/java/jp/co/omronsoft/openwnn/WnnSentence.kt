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
 * The container class of a sentence.
 *
 * @author Copyright (C) 2009, OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
class WnnSentence : WnnWord {
    /** The array of clauses  */
    var elements: ArrayList<WnnClause>? = null

    /**
     * Constructor
     *
     * @param input     The string of reading
     * @param clauses   The array of clauses of this sentence
     */
    constructor(input: String?, clauses: ArrayList<WnnClause>?) {
        if (clauses == null || clauses.isEmpty()) {
            this.id = 0
            this.candidate = ""
            this.stroke = ""
            this.frequency = 0
            this.partOfSpeech = WnnPOS()
            this.attribute = 0
        } else {
            this.elements = clauses
            val headClause = clauses[0]

            if (clauses.size == 1) {
                this.id = headClause.id
                this.candidate = headClause.candidate
                this.stroke = input
                this.frequency = headClause.frequency
                this.partOfSpeech = headClause.partOfSpeech
                this.attribute = headClause.attribute
            } else {
                val candidate = StringBuffer()
                val ci: Iterator<WnnClause> = clauses.iterator()
                while (ci.hasNext()) {
                    val clause = ci.next()
                    candidate.append(clause.candidate)
                }

                this.id = headClause.id
                this.candidate = candidate.toString()
                this.stroke = input
                this.frequency = headClause.frequency
                this.partOfSpeech = WnnPOS(
                    headClause.partOfSpeech!!.left,
                    clauses[clauses.size - 1].partOfSpeech!!.right
                )
                this.attribute = 2
            }
        }
    }

    /**
     * Constructor
     *
     * @param input     The string of reading
     * @param clause    The clauses of this sentence
     */
    constructor(input: String?, clause: WnnClause) {
        this.id = clause.id
        this.candidate = clause.candidate
        this.stroke = input
        this.frequency = clause.frequency
        this.partOfSpeech = clause.partOfSpeech
        this.attribute = clause.attribute

        this.elements = ArrayList()
        elements!!.add(clause)
    }

    /**
     * Constructor
     *
     * @param prev      The previous clauses
     * @param clause    The clauses of this sentence
     */
    constructor(prev: WnnSentence, clause: WnnClause) {
        this.id = prev.id
        this.candidate = prev.candidate + clause.candidate
        this.stroke = prev.stroke + clause.stroke
        this.frequency = prev.frequency + clause.frequency
        this.partOfSpeech = WnnPOS(prev.partOfSpeech!!.left, clause.partOfSpeech!!.right)
        this.attribute = prev.attribute

        this.elements = ArrayList()
        elements!!.addAll(prev.elements!!)
        elements!!.add(clause)
    }

    /**
     * Constructor
     *
     * @param head      The top clause of this sentence
     * @param tail      The following sentence
     */
    constructor(head: WnnClause, tail: WnnSentence?) {
        if (tail == null) {
            /* single clause */
            this.id = head.id
            this.candidate = head.candidate
            this.stroke = head.stroke
            this.frequency = head.frequency
            this.partOfSpeech = head.partOfSpeech
            this.attribute = head.attribute
            this.elements = ArrayList()
            elements!!.add(head)
        } else {
            /* consecutive clauses */
            this.id = head.id
            this.candidate = head.candidate + tail.candidate
            this.stroke = head.stroke + tail.stroke
            this.frequency = head.frequency + tail.frequency
            this.partOfSpeech = WnnPOS(head.partOfSpeech!!.left, tail.partOfSpeech!!.right)
            this.attribute = 2

            this.elements = ArrayList()
            elements!!.add(head)
            elements!!.addAll(tail.elements!!)
        }
    }
}
