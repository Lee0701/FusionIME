/*
 * Copyright (C) 2009 The Android Open Source Project
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
package com.android.inputmethod.pinyin

import android.content.res.Resources
import android.view.inputmethod.EditorInfo
import com.android.inputmethod.pinyin.SoftKeyboard.KeyRow
import ee.oyatl.ime.fusion.R

/**
 * Switcher used to switching input mode between Chinese, English, symbol,etc.
 */
class InputModeSwitcher(
    /**
     * IME service.
     */
    private val mImeService: PinyinIME
) {
    /**
     * The input mode for the current edit box.
     */
    var inputMode: Int = MODE_UNSET
        private set

    /**
     * Used to remember previous input mode. When user enters an edit field, the
     * previous input mode will be tried. If the previous mode can not be used
     * for the current situation (For example, previous mode is a soft keyboard
     * mode to input symbols, and we have a hardware keyboard for the current
     * situation), [.mRecentLauageInputMode] will be tried.
     */
    private var mPreviousInputMode: Int = MODE_SKB_CHINESE

    /**
     * Used to remember recent mode to input language.
     */
    private var mRecentLauageInputMode: Int = MODE_SKB_CHINESE

    /**
     * Editor information of the current edit box.
     */
    private var mEditorInfo: EditorInfo? = null

    /**
     * Used to indicate required toggling operations.
     */
    val toggleStates: ToggleStates = ToggleStates()

    /**
     * The current field is a short message field?
     */
    private var mShortMessageField: Boolean = false

    /**
     * Is return key in normal state?
     */
    var isEnterNoramlState: Boolean = true
        private set

    /**
     * Current icon. 0 for none icon.
     */
    var mInputIcon: Int = R.drawable.ime_pinyin

    /**
     * Key toggling state for Chinese mode.
     */
    private val mToggleStateCn: Int

    /**
     * Key toggling state for Chinese mode with candidates.
     */
    val toggleStateForCand: Int

    /**
     * Key toggling state for English lowwercase mode.
     */
    private val mToggleStateEnLower: Int

    /**
     * Key toggling state for English upppercase mode.
     */
    private val mToggleStateEnUpper: Int

    /**
     * Key toggling state for English symbol mode for the first page.
     */
    private val mToggleStateEnSym1: Int

    /**
     * Key toggling state for English symbol mode for the second page.
     */
    private val mToggleStateEnSym2: Int

    /**
     * Key toggling state for smiley mode.
     */
    private val mToggleStateSmiley: Int

    /**
     * Key toggling state for phone symbol mode.
     */
    private val mToggleStatePhoneSym: Int

    /**
     * Key toggling state for GO action of ENTER key.
     */
    private val mToggleStateGo: Int

    /**
     * Key toggling state for SEARCH action of ENTER key.
     */
    private val mToggleStateSearch: Int

    /**
     * Key toggling state for SEND action of ENTER key.
     */
    private val mToggleStateSend: Int

    /**
     * Key toggling state for NEXT action of ENTER key.
     */
    private val mToggleStateNext: Int

    /**
     * Key toggling state for SEND action of ENTER key.
     */
    private val mToggleStateDone: Int

    /**
     * QWERTY row toggling state for Chinese input.
     */
    private val mToggleRowCn: Int

    /**
     * QWERTY row toggling state for English input.
     */
    private val mToggleRowEn: Int

    /**
     * QWERTY row toggling state for URI input.
     */
    private val mToggleRowUri: Int

    /**
     * QWERTY row toggling state for email address input.
     */
    private val mToggleRowEmailAddress: Int

    inner class ToggleStates {
        /**
         * If it is true, this soft keyboard is a QWERTY one.
         */
        var mQwerty: Boolean = false

        /**
         * If [.mQwerty] is true, this variable is used to decide the
         * letter case of the QWERTY keyboard.
         */
        var mQwertyUpperCase: Boolean = false

        /**
         * The id of enabled row in the soft keyboard. Refer to
         * [com.android.inputmethod.pinyin.SoftKeyboard.KeyRow] for
         * details.
         */
        var mRowIdToEnable: Int = 0

        /**
         * Used to store all other toggle states for the current input mode.
         */
        var mKeyStates: IntArray = IntArray(MAX_TOGGLE_STATES)

        /**
         * Number of states to toggle.
         */
        var mKeyStatesNum: Int = 0
    }

    init {
        val r: Resources = mImeService.resources
        mToggleStateCn = r.getString(R.string.toggle_cn).toInt()
        toggleStateForCand = r
            .getString(R.string.toggle_cn_cand).toInt()
        mToggleStateEnLower = r
            .getString(R.string.toggle_en_lower).toInt()
        mToggleStateEnUpper = r
            .getString(R.string.toggle_en_upper).toInt()
        mToggleStateEnSym1 = r
            .getString(R.string.toggle_en_sym1).toInt()
        mToggleStateEnSym2 = r
            .getString(R.string.toggle_en_sym2).toInt()
        mToggleStateSmiley = r
            .getString(R.string.toggle_smiley).toInt()
        mToggleStatePhoneSym = r
            .getString(R.string.toggle_phone_sym).toInt()

        mToggleStateGo = r.getString(R.string.toggle_enter_go).toInt()
        mToggleStateSearch = r
            .getString(R.string.toggle_enter_search).toInt()
        mToggleStateSend = r
            .getString(R.string.toggle_enter_send).toInt()
        mToggleStateNext = r
            .getString(R.string.toggle_enter_next).toInt()
        mToggleStateDone = r
            .getString(R.string.toggle_enter_done).toInt()

        mToggleRowCn = r.getString(R.string.toggle_row_cn).toInt()
        mToggleRowEn = r.getString(R.string.toggle_row_en).toInt()
        mToggleRowUri = r.getString(R.string.toggle_row_uri).toInt()
        mToggleRowEmailAddress = r
            .getString(R.string.toggle_row_emailaddress).toInt()
    }

    val skbLayout: Int
        get() {
            val layout: Int =
                (inputMode and MASK_SKB_LAYOUT)

            when (layout) {
                MASK_SKB_LAYOUT_QWERTY -> return R.xml.skb_qwerty
                MASK_SKB_LAYOUT_SYMBOL1 -> return R.xml.skb_sym1
                MASK_SKB_LAYOUT_SYMBOL2 -> return R.xml.skb_sym2
                MASK_SKB_LAYOUT_SMILEY -> return R.xml.skb_smiley
                MASK_SKB_LAYOUT_PHONE -> return R.xml.skb_phone
            }
            return 0
        }

    // Return the icon to update.
    fun switchLanguageWithHkb(): Int {
        var newInputMode: Int = MODE_HKB_CHINESE
        mInputIcon = R.drawable.ime_pinyin

        if (MODE_HKB_CHINESE == inputMode) {
            newInputMode = MODE_HKB_ENGLISH
            mInputIcon = R.drawable.ime_en
        }

        saveInputMode(newInputMode)
        return mInputIcon
    }

    // Return the icon to update.
    fun switchModeForUserKey(userKey: Int): Int {
        var newInputMode: Int = MODE_UNSET

        if (USERDEF_KEYCODE_LANG_2 == userKey) {
            if (MODE_SKB_CHINESE == inputMode) {
                newInputMode = MODE_SKB_ENGLISH_LOWER
            } else if (MODE_SKB_ENGLISH_LOWER == inputMode
                || MODE_SKB_ENGLISH_UPPER == inputMode
            ) {
                newInputMode = MODE_SKB_CHINESE
            } else if (MODE_SKB_SYMBOL1_CN == inputMode) {
                newInputMode = MODE_SKB_SYMBOL1_EN
            } else if (MODE_SKB_SYMBOL1_EN == inputMode) {
                newInputMode = MODE_SKB_SYMBOL1_CN
            } else if (MODE_SKB_SYMBOL2_CN == inputMode) {
                newInputMode = MODE_SKB_SYMBOL2_EN
            } else if (MODE_SKB_SYMBOL2_EN == inputMode) {
                newInputMode = MODE_SKB_SYMBOL2_CN
            } else if (MODE_SKB_SMILEY == inputMode) {
                newInputMode = MODE_SKB_CHINESE
            }
        } else if (USERDEF_KEYCODE_SYM_3 == userKey) {
            if (MODE_SKB_CHINESE == inputMode) {
                newInputMode = MODE_SKB_SYMBOL1_CN
            } else if (MODE_SKB_ENGLISH_UPPER == inputMode
                || MODE_SKB_ENGLISH_LOWER == inputMode
            ) {
                newInputMode = MODE_SKB_SYMBOL1_EN
            } else if (MODE_SKB_SYMBOL1_EN == inputMode
                || MODE_SKB_SYMBOL2_EN == inputMode
            ) {
                newInputMode = MODE_SKB_ENGLISH_LOWER
            } else if (MODE_SKB_SYMBOL1_CN == inputMode
                || MODE_SKB_SYMBOL2_CN == inputMode
            ) {
                newInputMode = MODE_SKB_CHINESE
            } else if (MODE_SKB_SMILEY == inputMode) {
                newInputMode = MODE_SKB_SYMBOL1_CN
            }
        } else if (USERDEF_KEYCODE_SHIFT_1 == userKey) {
            if (MODE_SKB_ENGLISH_LOWER == inputMode) {
                newInputMode = MODE_SKB_ENGLISH_UPPER
            } else if (MODE_SKB_ENGLISH_UPPER == inputMode) {
                newInputMode = MODE_SKB_ENGLISH_LOWER
            }
        } else if (USERDEF_KEYCODE_MORE_SYM_5 == userKey) {
            var sym: Int = (MASK_SKB_LAYOUT and inputMode)
            if (MASK_SKB_LAYOUT_SYMBOL1 == sym) {
                sym = MASK_SKB_LAYOUT_SYMBOL2
            } else {
                sym = MASK_SKB_LAYOUT_SYMBOL1
            }
            newInputMode = ((inputMode and (MASK_SKB_LAYOUT.inv())) or sym)
        } else if (USERDEF_KEYCODE_SMILEY_6 == userKey) {
            if (MODE_SKB_CHINESE == inputMode) {
                newInputMode = MODE_SKB_SMILEY
            } else {
                newInputMode = MODE_SKB_CHINESE
            }
        } else if (USERDEF_KEYCODE_PHONE_SYM_4 == userKey) {
            if (MODE_SKB_PHONE_NUM == inputMode) {
                newInputMode = MODE_SKB_PHONE_SYM
            } else {
                newInputMode = MODE_SKB_PHONE_NUM
            }
        }

        if (newInputMode == inputMode || MODE_UNSET == newInputMode) {
            return mInputIcon
        }

        saveInputMode(newInputMode)
        prepareToggleStates(true)
        return mInputIcon
    }

    // Return the icon to update.
    fun requestInputWithHkb(editorInfo: EditorInfo): Int {
        mShortMessageField = false
        var english: Boolean = false
        var newInputMode: Int = MODE_HKB_CHINESE

        when (editorInfo.inputType and EditorInfo.TYPE_MASK_CLASS) {
            EditorInfo.TYPE_CLASS_NUMBER, EditorInfo.TYPE_CLASS_PHONE, EditorInfo.TYPE_CLASS_DATETIME -> english =
                true

            EditorInfo.TYPE_CLASS_TEXT -> {
                val v: Int = editorInfo.inputType and EditorInfo.TYPE_MASK_VARIATION
                if (v == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS || v == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD || v == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD || v == EditorInfo.TYPE_TEXT_VARIATION_URI) {
                    english = true
                } else if (v == EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE) {
                    mShortMessageField = true
                }
            }

            else -> {}
        }

        if (english) {
            // If the application request English mode, we switch to it.
            newInputMode = MODE_HKB_ENGLISH
        } else {
            // If the application do not request English mode, we will
            // try to keep the previous mode to input language text.
            // Because there is not soft keyboard, we need discard all
            // soft keyboard related information from the previous language
            // mode.
            if ((mRecentLauageInputMode and MASK_LANGUAGE) == MASK_LANGUAGE_CN) {
                newInputMode = MODE_HKB_CHINESE
            } else {
                newInputMode = MODE_HKB_ENGLISH
            }
        }
        mEditorInfo = editorInfo
        saveInputMode(newInputMode)
        prepareToggleStates(false)
        return mInputIcon
    }

    // Return the icon to update.
    fun requestInputWithSkb(editorInfo: EditorInfo): Int {
        mShortMessageField = false

        var newInputMode: Int = MODE_SKB_CHINESE

        when (editorInfo.inputType and EditorInfo.TYPE_MASK_CLASS) {
            EditorInfo.TYPE_CLASS_NUMBER, EditorInfo.TYPE_CLASS_DATETIME -> newInputMode =
                MODE_SKB_SYMBOL1_EN

            EditorInfo.TYPE_CLASS_PHONE -> newInputMode = MODE_SKB_PHONE_NUM
            EditorInfo.TYPE_CLASS_TEXT -> {
                val v: Int = editorInfo.inputType and EditorInfo.TYPE_MASK_VARIATION
                if (v == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS || v == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD || v == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD || v == EditorInfo.TYPE_TEXT_VARIATION_URI) {
                    // If the application request English mode, we switch to it.
                    newInputMode = MODE_SKB_ENGLISH_LOWER
                } else {
                    if (v == EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE) {
                        mShortMessageField = true
                    }
                    // If the application do not request English mode, we will
                    // try to keep the previous mode.
                    val skbLayout: Int = (inputMode and MASK_SKB_LAYOUT)
                    newInputMode = inputMode
                    if (0 == skbLayout) {
                        if ((inputMode and MASK_LANGUAGE) == MASK_LANGUAGE_CN) {
                            newInputMode = MODE_SKB_CHINESE
                        } else {
                            newInputMode = MODE_SKB_ENGLISH_LOWER
                        }
                    }
                }
            }

            else -> {
                // Try to keep the previous mode.
                val skbLayout: Int = (inputMode and MASK_SKB_LAYOUT)
                newInputMode = inputMode
                if (0 == skbLayout) {
                    if ((inputMode and MASK_LANGUAGE) == MASK_LANGUAGE_CN) {
                        newInputMode = MODE_SKB_CHINESE
                    } else {
                        newInputMode = MODE_SKB_ENGLISH_LOWER
                    }
                }
            }
        }

        mEditorInfo = editorInfo
        saveInputMode(newInputMode)
        prepareToggleStates(true)
        return mInputIcon
    }

    // Return the icon to update.
    fun requestBackToPreviousSkb(): Int {
        val layout: Int = (inputMode and MASK_SKB_LAYOUT)
        val lastLayout: Int = (mPreviousInputMode and MASK_SKB_LAYOUT)
        if (0 != layout && 0 != lastLayout) {
            inputMode = mPreviousInputMode
            saveInputMode(inputMode)
            prepareToggleStates(true)
            return mInputIcon
        }
        return 0
    }

    val isEnglishWithHkb: Boolean
        get() = MODE_HKB_ENGLISH == inputMode

    val isEnglishWithSkb: Boolean
        get() = MODE_SKB_ENGLISH_LOWER == inputMode
                || MODE_SKB_ENGLISH_UPPER == inputMode

    val isEnglishUpperCaseWithSkb: Boolean
        get() {
            return MODE_SKB_ENGLISH_UPPER == inputMode
        }

    val isChineseText: Boolean
        get() {
            val skbLayout: Int =
                (inputMode and MASK_SKB_LAYOUT)
            if (MASK_SKB_LAYOUT_QWERTY == skbLayout || 0 == skbLayout) {
                val language: Int =
                    (inputMode and MASK_LANGUAGE)
                if (MASK_LANGUAGE_CN == language) return true
            }
            return false
        }

    val isChineseTextWithHkb: Boolean
        get() {
            val skbLayout: Int =
                (inputMode and MASK_SKB_LAYOUT)
            if (0 == skbLayout) {
                val language: Int =
                    (inputMode and MASK_LANGUAGE)
                if (MASK_LANGUAGE_CN == language) return true
            }
            return false
        }

    val isChineseTextWithSkb: Boolean
        get() {
            val skbLayout: Int =
                (inputMode and MASK_SKB_LAYOUT)
            if (MASK_SKB_LAYOUT_QWERTY == skbLayout) {
                val language: Int =
                    (inputMode and MASK_LANGUAGE)
                if (MASK_LANGUAGE_CN == language) return true
            }
            return false
        }

    val isSymbolWithSkb: Boolean
        get() {
            val skbLayout: Int =
                (inputMode and MASK_SKB_LAYOUT)
            if (MASK_SKB_LAYOUT_SYMBOL1 == skbLayout
                || MASK_SKB_LAYOUT_SYMBOL2 == skbLayout
            ) {
                return true
            }
            return false
        }

    fun tryHandleLongPressSwitch(keyCode: Int): Boolean {
        if (USERDEF_KEYCODE_LANG_2 == keyCode
            || USERDEF_KEYCODE_PHONE_SYM_4 == keyCode
        ) {
            mImeService.showOptionsMenu()
            return true
        }
        return false
    }

    private fun saveInputMode(newInputMode: Int) {
        mPreviousInputMode = inputMode
        inputMode = newInputMode

        val skbLayout: Int = (inputMode and MASK_SKB_LAYOUT)
        if (MASK_SKB_LAYOUT_QWERTY == skbLayout || 0 == skbLayout) {
            mRecentLauageInputMode = inputMode
        }

        mInputIcon = R.drawable.ime_pinyin
        if (isEnglishWithHkb) {
            mInputIcon = R.drawable.ime_en
        } else if (isChineseTextWithHkb) {
            mInputIcon = R.drawable.ime_pinyin
        }

        if (!Environment.instance.hasHardKeyboard()) {
            mInputIcon = 0
        }
    }

    private fun prepareToggleStates(needSkb: Boolean) {
        isEnterNoramlState = true
        if (!needSkb) return

        toggleStates.mQwerty = false
        toggleStates.mKeyStatesNum = 0

        val states: IntArray = toggleStates.mKeyStates
        var statesNum = 0
        // Toggle state for language.
        val language: Int = (inputMode and MASK_LANGUAGE)
        val layout: Int = (inputMode and MASK_SKB_LAYOUT)
        val charcase: Int = (inputMode and MASK_CASE)
        val variation: Int = mEditorInfo!!.inputType and EditorInfo.TYPE_MASK_VARIATION

        if (MASK_SKB_LAYOUT_PHONE != layout) {
            if (MASK_LANGUAGE_CN == language) {
                // Chinese and Chinese symbol are always the default states,
                // do not add a toggling operation.
                if (MASK_SKB_LAYOUT_QWERTY == layout) {
                    toggleStates.mQwerty = true
                    toggleStates.mQwertyUpperCase = true
                    if (mShortMessageField) {
                        states[statesNum] = mToggleStateSmiley
                        statesNum++
                    }
                }
            } else if (MASK_LANGUAGE_EN == language) {
                if (MASK_SKB_LAYOUT_QWERTY == layout) {
                    toggleStates.mQwerty = true
                    toggleStates.mQwertyUpperCase = false
                    states[statesNum] = mToggleStateEnLower
                    if (MASK_CASE_UPPER == charcase) {
                        toggleStates.mQwertyUpperCase = true
                        states[statesNum] = mToggleStateEnUpper
                    }
                    statesNum++
                } else if (MASK_SKB_LAYOUT_SYMBOL1 == layout) {
                    states[statesNum] = mToggleStateEnSym1
                    statesNum++
                } else if (MASK_SKB_LAYOUT_SYMBOL2 == layout) {
                    states[statesNum] = mToggleStateEnSym2
                    statesNum++
                }
            }

            // Toggle rows for QWERTY.
            toggleStates.mRowIdToEnable = KeyRow.DEFAULT_ROW_ID
            if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) {
                toggleStates.mRowIdToEnable = mToggleRowEmailAddress
            } else if (variation == EditorInfo.TYPE_TEXT_VARIATION_URI) {
                toggleStates.mRowIdToEnable = mToggleRowUri
            } else if (MASK_LANGUAGE_CN == language) {
                toggleStates.mRowIdToEnable = mToggleRowCn
            } else if (MASK_LANGUAGE_EN == language) {
                toggleStates.mRowIdToEnable = mToggleRowEn
            }
        } else {
            if (MASK_CASE_UPPER == charcase) {
                states[statesNum] = mToggleStatePhoneSym
                statesNum++
            }
        }

        // Toggle state for enter key.
        val action: Int = (mEditorInfo!!.imeOptions
                and (EditorInfo.IME_MASK_ACTION or EditorInfo.IME_FLAG_NO_ENTER_ACTION))

        if (action == EditorInfo.IME_ACTION_GO) {
            states[statesNum] = mToggleStateGo
            statesNum++
            isEnterNoramlState = false
        } else if (action == EditorInfo.IME_ACTION_SEARCH) {
            states[statesNum] = mToggleStateSearch
            statesNum++
            isEnterNoramlState = false
        } else if (action == EditorInfo.IME_ACTION_SEND) {
            states[statesNum] = mToggleStateSend
            statesNum++
            isEnterNoramlState = false
        } else if (action == EditorInfo.IME_ACTION_NEXT) {
            val f: Int = mEditorInfo!!.inputType and EditorInfo.TYPE_MASK_FLAGS
            if (f != EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) {
                states[statesNum] = mToggleStateNext
                statesNum++
                isEnterNoramlState = false
            }
        } else if (action == EditorInfo.IME_ACTION_DONE) {
            states[statesNum] = mToggleStateDone
            statesNum++
            isEnterNoramlState = false
        }
        toggleStates.mKeyStatesNum = statesNum
    }

    companion object {
        /**
         * User defined key code, used by soft keyboard.
         */
        private val USERDEF_KEYCODE_SHIFT_1: Int = -1

        /**
         * User defined key code, used by soft keyboard.
         */
        private val USERDEF_KEYCODE_LANG_2: Int = -2

        /**
         * User defined key code, used by soft keyboard.
         */
        private val USERDEF_KEYCODE_SYM_3: Int = -3

        /**
         * User defined key code, used by soft keyboard.
         */
        val USERDEF_KEYCODE_PHONE_SYM_4: Int = -4

        /**
         * User defined key code, used by soft keyboard.
         */
        private val USERDEF_KEYCODE_MORE_SYM_5: Int = -5

        /**
         * User defined key code, used by soft keyboard.
         */
        private val USERDEF_KEYCODE_SMILEY_6: Int = -6


        /**
         * Bits used to indicate soft keyboard layout. If none bit is set, the
         * current input mode does not require a soft keyboard.
         */
        private const val MASK_SKB_LAYOUT: Int = -0x10000000

        /**
         * A kind of soft keyboard layout. An input mode should be anded with
         * [.MASK_SKB_LAYOUT] to get its soft keyboard layout.
         */
        private const val MASK_SKB_LAYOUT_QWERTY: Int = 0x10000000

        /**
         * A kind of soft keyboard layout. An input mode should be anded with
         * [.MASK_SKB_LAYOUT] to get its soft keyboard layout.
         */
        private const val MASK_SKB_LAYOUT_SYMBOL1: Int = 0x20000000

        /**
         * A kind of soft keyboard layout. An input mode should be anded with
         * [.MASK_SKB_LAYOUT] to get its soft keyboard layout.
         */
        private const val MASK_SKB_LAYOUT_SYMBOL2: Int = 0x30000000

        /**
         * A kind of soft keyboard layout. An input mode should be anded with
         * [.MASK_SKB_LAYOUT] to get its soft keyboard layout.
         */
        private const val MASK_SKB_LAYOUT_SMILEY: Int = 0x40000000

        /**
         * A kind of soft keyboard layout. An input mode should be anded with
         * [.MASK_SKB_LAYOUT] to get its soft keyboard layout.
         */
        private const val MASK_SKB_LAYOUT_PHONE: Int = 0x50000000

        /**
         * Used to indicate which language the current input mode is in. If the
         * current input mode works with a none-QWERTY soft keyboard, these bits are
         * also used to get language information. For example, a Chinese symbol soft
         * keyboard and an English one are different in an icon which is used to
         * tell user the language information. BTW, the smiley soft keyboard mode
         * should be set with [.MASK_LANGUAGE_CN] because it can only be
         * launched from Chinese QWERTY soft keyboard, and it has Chinese icon on
         * soft keyboard.
         */
        private const val MASK_LANGUAGE: Int = 0x0f000000

        /**
         * Used to indicate the current language. An input mode should be anded with
         * [.MASK_LANGUAGE] to get this information.
         */
        private const val MASK_LANGUAGE_CN: Int = 0x01000000

        /**
         * Used to indicate the current language. An input mode should be anded with
         * [.MASK_LANGUAGE] to get this information.
         */
        private const val MASK_LANGUAGE_EN: Int = 0x02000000

        /**
         * Used to indicate which case the current input mode is in. For example,
         * English QWERTY has lowercase and uppercase. For the Chinese QWERTY, these
         * bits are ignored. For phone keyboard layout, these bits can be
         * [.MASK_CASE_UPPER] to request symbol page for phone soft keyboard.
         */
        private const val MASK_CASE: Int = 0x00f00000

        /**
         * Used to indicate the current case information. An input mode should be
         * anded with [.MASK_CASE] to get this information.
         */
        private const val MASK_CASE_LOWER: Int = 0x00100000

        /**
         * Used to indicate the current case information. An input mode should be
         * anded with [.MASK_CASE] to get this information.
         */
        private const val MASK_CASE_UPPER: Int = 0x00200000

        /**
         * Mode for inputing Chinese with soft keyboard.
         */
        val MODE_SKB_CHINESE: Int = (MASK_SKB_LAYOUT_QWERTY or MASK_LANGUAGE_CN)

        /**
         * Mode for inputing basic symbols for Chinese mode with soft keyboard.
         */
        val MODE_SKB_SYMBOL1_CN: Int = (MASK_SKB_LAYOUT_SYMBOL1 or MASK_LANGUAGE_CN)

        /**
         * Mode for inputing more symbols for Chinese mode with soft keyboard.
         */
        val MODE_SKB_SYMBOL2_CN: Int = (MASK_SKB_LAYOUT_SYMBOL2 or MASK_LANGUAGE_CN)

        /**
         * Mode for inputing English lower characters with soft keyboard.
         */
        val MODE_SKB_ENGLISH_LOWER: Int = ((MASK_SKB_LAYOUT_QWERTY
                or MASK_LANGUAGE_EN or MASK_CASE_LOWER))

        /**
         * Mode for inputing English upper characters with soft keyboard.
         */
        val MODE_SKB_ENGLISH_UPPER: Int = ((MASK_SKB_LAYOUT_QWERTY
                or MASK_LANGUAGE_EN or MASK_CASE_UPPER))

        /**
         * Mode for inputing basic symbols for English mode with soft keyboard.
         */
        val MODE_SKB_SYMBOL1_EN: Int = (MASK_SKB_LAYOUT_SYMBOL1 or MASK_LANGUAGE_EN)

        /**
         * Mode for inputing more symbols for English mode with soft keyboard.
         */
        val MODE_SKB_SYMBOL2_EN: Int = (MASK_SKB_LAYOUT_SYMBOL2 or MASK_LANGUAGE_EN)

        /**
         * Mode for inputing smileys with soft keyboard.
         */
        val MODE_SKB_SMILEY: Int = (MASK_SKB_LAYOUT_SMILEY or MASK_LANGUAGE_CN)

        /**
         * Mode for inputing phone numbers.
         */
        val MODE_SKB_PHONE_NUM: Int = (MASK_SKB_LAYOUT_PHONE)

        /**
         * Mode for inputing phone numbers.
         */
        val MODE_SKB_PHONE_SYM: Int = (MASK_SKB_LAYOUT_PHONE or MASK_CASE_UPPER)

        /**
         * Mode for inputing Chinese with a hardware keyboard.
         */
        val MODE_HKB_CHINESE: Int = (MASK_LANGUAGE_CN)

        /**
         * Mode for inputing English with a hardware keyboard
         */
        val MODE_HKB_ENGLISH: Int = (MASK_LANGUAGE_EN)

        /**
         * Unset mode.
         */
        const val MODE_UNSET: Int = 0

        /**
         * Maximum toggle states for a soft keyboard.
         */
        const val MAX_TOGGLE_STATES: Int = 4
    }
}
