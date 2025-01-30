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

import android.view.KeyEvent

/**
 * The definition class of event message used by OpenWnn framework.
 *
 * @author Copyright (C) 2009-2011 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
class OpenWnnEvent {
    /**
     * The definition class of engine's mode.
     */
    object Mode {
        /** Default (use both of the letterConverter and the [WnnEngine])  */
        const val DEFAULT: Int = 0

        /** Direct input (not use the letterConverter and the [WnnEngine])  */
        const val DIRECT: Int = 1

        /** Do not use the [LetterConverter]  */
        const val NO_LV1_CONV: Int = 2

        /** Do not use the [WnnEngine]  */
        const val NO_LV2_CONV: Int = 3
    }

    /** Event code  */
    var code: Int = UNDEFINED

    /** Detail mode of the event  */
    var mode: Int = 0

    /** Type of dictionary  */
    var dictionaryType: Int = 0

    /** Input character(s)  */
    var chars: CharArray? = null

    /** Key event  */
    var keyEvent: KeyEvent? = null

    /** Mapping table for toggle input  */
    var toggleTable: Array<String>? = null

    /** Mapping table for toggle input  */
    var replaceTable: HashMap<*, *>? = null

    /** Word's information  */
    var word: WnnWord? = null

    /** Error code  */
    var errorCode: Int = 0

    /**
     * Generate [OpenWnnEvent]
     *
     * @param code      The code
     */
    constructor(code: Int) {
        this.code = code
    }

    /**
     * Generate [OpenWnnEvent] for changing the mode
     *
     * @param code      The code
     * @param mode      The mode
     */
    constructor(code: Int, mode: Int) {
        this.code = code
        this.mode = mode
    }

    /**
     * Generate [OpenWnnEvent] for a inputing character
     *
     * @param code      The code
     * @param c         The inputing character
     */
    constructor(code: Int, c: Char) {
        this.code = code
        this.chars = CharArray(1)
        chars!![0] = c
    }

    /**
     * Generate [OpenWnnEvent] for inputing characters
     *
     * @param code      The code
     * @param c         The array of inputing character
     */
    constructor(code: Int, c: CharArray?) {
        this.code = code
        this.chars = c
    }

    /**
     * Generate [OpenWnnEvent] for toggle inputing a character
     *
     * @param code          The code
     * @param toggleTable   The array of toggle inputing a character
     */
    constructor(code: Int, toggleTable: Array<String>?) {
        this.code = code
        this.toggleTable = toggleTable
    }

    /**
     * Generate [OpenWnnEvent] for replacing a character
     *
     * @param code          The code
     * @param replaceTable  The replace table
     */
    constructor(code: Int, replaceTable: HashMap<*, *>?) {
        this.code = code
        this.replaceTable = replaceTable
    }

    /**
     * Generate [OpenWnnEvent] from [KeyEvent]
     * <br></br>
     * This constructor is same as `OpenWnnEvent(INPUT_KEY, ev)`.
     *
     * @param ev    The key event
     */
    constructor(ev: KeyEvent) {
        if (ev.action != KeyEvent.ACTION_UP) {
            this.code = INPUT_KEY
        } else {
            this.code = KEYUP
        }
        this.keyEvent = ev
    }

    /**
     * Generate [OpenWnnEvent] from [KeyEvent]
     *
     * @param code      The code
     * @param ev        The key event
     */
    constructor(code: Int, ev: KeyEvent?) {
        this.code = code
        this.keyEvent = ev
    }

    /**
     * Generate [OpenWnnEvent] for selecting a candidate
     *
     * @param code      The code
     * @param word      The selected candidate
     */
    constructor(code: Int, word: WnnWord?) {
        this.code = code
        this.word = word
    }

    /**
     * Generate [OpenWnnEvent] for dictionary management
     *
     * @param code      The code
     * @param dict      The type of dictionary
     * @param word      The selected candidate
     */
    constructor(code: Int, dict: Int, word: WnnWord?) {
        this.code = code
        this.dictionaryType = dict
        this.word = word
    }

    companion object {
        /** Offset value for private events  */
        const val PRIVATE_EVENT_OFFSET: Int = -0x1000000

        /** Undefined  */
        const val UNDEFINED: Int = 0

        /**
         * Reverse key.
         * <br></br>
         * This is used for multi-tap keyboard like 12-key.
         */
        const val TOGGLE_REVERSE_CHAR: Int = -0xfffffff

        /**
         * Convert.
         * <br></br>
         * This event makes [OpenWnn] to display conversion candidates from [ComposingText].
         */
        const val CONVERT: Int = -0xffffffe

        /**
         * Predict.
         * <br></br>
         * This event makes [OpenWnn] to display prediction candidates from [ComposingText].
         */
        const val PREDICT: Int = -0xffffff8

        /**
         * List candidates (normal view).
         * <br></br>
         * This event changes the candidates view's size
         */
        const val LIST_CANDIDATES_NORMAL: Int = -0xffffffd

        /**
         * List candidates (wide view).
         * <br></br>
         * This event changes the candidates view's size
         */
        const val LIST_CANDIDATES_FULL: Int = -0xffffffc

        /**
         * Close view
         */
        const val CLOSE_VIEW: Int = -0xffffffb

        /**
         * Insert character(s).
         * <br></br>
         * This event input specified character(`chars`) into the cursor position.
         */
        const val INPUT_CHAR: Int = -0xffffffa

        /**
         * Toggle a character.
         * <br></br>
         * This event changes a character at cursor position with specified rule(`toggleMap`).
         * This is used for multi-tap keyboard.
         */
        const val TOGGLE_CHAR: Int = -0xffffff4

        /**
         * Replace a character at the cursor.
         */
        const val REPLACE_CHAR: Int = -0xffffff3

        /**
         * Input key.
         * <br></br>
         * This event processes a `keyEvent`.
         */
        const val INPUT_KEY: Int = -0xffffff9

        /**
         * Input Soft key.
         * <br></br>
         * This event processes a `keyEvent`.
         * If the event is not processed in [OpenWnn], the event is thrown to the IME's client.
         */
        const val INPUT_SOFT_KEY: Int = -0xffffff2

        /**
         * Focus to the candidates view.
         */
        const val FOCUS_TO_CANDIDATE_VIEW: Int = -0xffffff7

        /**
         * Focus out from the candidates view.
         */
        const val FOCUS_OUT_CANDIDATE_VIEW: Int = -0xffffff6

        /**
         * Select a candidate
         */
        const val SELECT_CANDIDATE: Int = -0xffffff5

        /**
         * Change Mode
         */
        const val CHANGE_MODE: Int = -0xffffff1

        /**
         * Key long press event.
         */
        const val KEYLONGPRESS: Int = -0xfffffdc

        /**
         * Commit the composing text
         */
        const val COMMIT_COMPOSING_TEXT: Int = -0xffffff0

        /**
         * List symbols
         */
        const val LIST_SYMBOLS: Int = -0xfffffef

        /**
         * Switch Language
         */
        const val SWITCH_LANGUAGE: Int = -0xfffffee

        /**
         * Initialize the user dictionary.
         */
        const val INITIALIZE_USER_DICTIONARY: Int = -0xfffffed

        /**
         * Initialize the learning dictionary.
         */
        const val INITIALIZE_LEARNING_DICTIONARY: Int = -0xfffffec

        /**
         * List words in the user dictionary.
         * <br></br>
         * To get words from the list, use `GET_WORD` event.
         */
        const val LIST_WORDS_IN_USER_DICTIONARY: Int = -0xfffffeb

        /**
         * Get a word from the user dictionary.
         * <br></br>
         * Get a word from top of the list made by `LIST_WORDS_IN_USER_DICTIONARY`.
         */
        const val GET_WORD: Int = -0xfffffe8

        /**
         * Add word to the user dictionary.
         */
        const val ADD_WORD: Int = -0xfffffea

        /**
         * Delete a word from the dictionary.
         */
        const val DELETE_WORD: Int = -0xfffffe9

        /**
         * Update the candidate view
         */
        const val UPDATE_CANDIDATE: Int = -0xfffffe7

        /**
         * Edit words in the user dictionary.
         */
        const val EDIT_WORDS_IN_USER_DICTIONARY: Int = -0xfffffe6

        /**
         * Undo
         */
        const val UNDO: Int = -0xfffffe5

        /**
         * Change input view
         */
        const val CHANGE_INPUT_VIEW: Int = -0xfffffe4

        /**
         * Touch the candidate view.
         */
        const val CANDIDATE_VIEW_TOUCH: Int = -0xfffffe3

        /**
         * Key up event.
         */
        const val KEYUP: Int = -0xfffffe1

        /**
         * Touch the other key.
         */
        const val TOUCH_OTHER_KEY: Int = -0xfffffe0

        /**
         * Start focus candidate.
         */
        const val FOCUS_CANDIDATE_START: Int = -0xfffe000

        /**
         * End focus candidate.
         */
        const val FOCUS_CANDIDATE_END: Int = -0xfffdfff

        /**
         * Scroll up for symbol keyboard.
         */
        const val CANDIDATE_VIEW_SCROLL_UP: Int = -0xffff000

        /**
         * Scroll down for symbol keyboard.
         */
        const val CANDIDATE_VIEW_SCROLL_DOWN: Int = -0xfffefff

        /**
         * Scroll full up for symbol keyboard.
         */
        const val CANDIDATE_VIEW_SCROLL_FULL_UP: Int = -0xfffeffe

        /**
         * Scroll full down for symbol keyboard.
         */
        const val CANDIDATE_VIEW_SCROLL_FULL_DOWN: Int = -0xfffeffd
    }
}

