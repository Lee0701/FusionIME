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

import android.app.AlertDialog
import android.content.SharedPreferences
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import ee.oyatl.ime.fusion.R
import jp.co.omronsoft.openwnn.BaseInputView
import jp.co.omronsoft.openwnn.DefaultSoftKeyboard
import jp.co.omronsoft.openwnn.Keyboard
import jp.co.omronsoft.openwnn.OpenWnn
import jp.co.omronsoft.openwnn.OpenWnnEvent
import jp.co.omronsoft.openwnn.OpenWnnJAJP
import java.util.Locale

/**
 * The default Software Keyboard class for Japanese IME.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
class DefaultSoftKeyboardJAJP : DefaultSoftKeyboard() {
    /** Type of input mode  */
    private var mInputType = INPUT_TYPE_TOGGLE

    /** Previous input character code  */
    private var mPrevInputKeyCode = 0

    /**
     * Character table to input when mInputType becomes INPUT_TYPE_INSTANT.
     * (Either INSTANT_CHAR_CODE_FULL_NUMBER or INSTANT_CHAR_CODE_HALF_NUMBER)
     */
    private var mCurrentInstantTable: CharArray? = null

    /** Input mode that is not able to be changed. If ENABLE_CHANGE_KEYMODE is set, input mode can change.  */
    private var mLimitedKeyMode: IntArray? = null

    /** Input mode that is given the first priority. If ENABLE_CHANGE_KEYMODE is set, input mode can change.  */
    private var mPreferenceKeyMode = INVALID_KEYMODE

    /** The last input type  */
    private var mLastInputType = 0

    /** Auto caps mode  */
    private var mEnableAutoCaps = true

    /** PopupResId of "Moji" key (this is used for canceling long-press)  */
    private var mPopupResId = 0

    /** Whether the InputType is null  */
    private var mIsInputTypeNull = false

    /** `SharedPreferences` for save the keyboard type  */
    private var mPrefEditor: SharedPreferences.Editor? = null

    /** "Moji" key (this is used for canceling long-press)  */
    private var mChangeModeKey: Keyboard.Key? = null


    /** Default constructor  */
    init {
        mCurrentLanguage = DefaultSoftKeyboard.Companion.LANG_JA
        mCurrentKeyboardType = if (OpenWnn.Companion.isXLarge()) {
            DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY
        } else {
            DefaultSoftKeyboard.Companion.KEYBOARD_12KEY
        }
        mShiftOn = DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF
        mCurrentKeyMode = DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA
    }

    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard.createKeyboards
     */
    override fun createKeyboards(parent: OpenWnn) {
        /* Keyboard[# of Languages][portrait/landscape][# of keyboard type][shift off/on][max # of key-modes][noinput/input] */

        mKeyboard = Array(3) { Array(2) { Array(4) { Array(2) { Array(8) { arrayOfNulls(2) } } } } }

        if (mHardKeyboardHidden) {
            /* Create the suitable keyboard object */
            if (mDisplayMode == DefaultSoftKeyboard.Companion.PORTRAIT) {
                createKeyboardsPortrait(parent)
            } else {
                createKeyboardsLandscape(parent)
            }

            if (mCurrentKeyboardType == DefaultSoftKeyboard.Companion.KEYBOARD_12KEY) {
                mWnn!!.onEvent(
                    OpenWnnEvent(
                        OpenWnnEvent.Companion.CHANGE_MODE,
                        OpenWnnJAJP.Companion.ENGINE_MODE_OPT_TYPE_12KEY
                    )
                )
            } else {
                mWnn!!.onEvent(
                    OpenWnnEvent(
                        OpenWnnEvent.Companion.CHANGE_MODE,
                        OpenWnnJAJP.Companion.ENGINE_MODE_OPT_TYPE_QWERTY
                    )
                )
            }
        } else if (mEnableHardware12Keyboard) {
            mWnn!!.onEvent(
                OpenWnnEvent(
                    OpenWnnEvent.Companion.CHANGE_MODE,
                    OpenWnnJAJP.Companion.ENGINE_MODE_OPT_TYPE_12KEY
                )
            )
        } else {
            mWnn!!.onEvent(
                OpenWnnEvent(
                    OpenWnnEvent.Companion.CHANGE_MODE,
                    OpenWnnJAJP.Companion.ENGINE_MODE_OPT_TYPE_QWERTY
                )
            )
        }
    }

    /**
     * Commit the pre-edit string for committing operation that is not explicit
     * (ex. when a candidate is selected)
     */
    private fun commitText() {
        if (!mNoInput) {
            mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.COMMIT_COMPOSING_TEXT))
        }
    }

    /**
     * Change input mode
     * <br></br>
     * @param keyMode   The type of input mode
     */
    fun changeKeyMode(keyMode: Int) {
        val targetMode = filterKeyMode(keyMode)
        if (targetMode == INVALID_KEYMODE) {
            return
        }

        commitText()

        if (mCapsLock) {
            mWnn!!.onEvent(
                OpenWnnEvent(
                    OpenWnnEvent.Companion.INPUT_SOFT_KEY,
                    KeyEvent(
                        KeyEvent.ACTION_UP,
                        KeyEvent.KEYCODE_SHIFT_LEFT
                    )
                )
            )
            mCapsLock = false
        }
        mShiftOn = DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF
        val kbd = getModeChangeKeyboard(targetMode)
        mCurrentKeyMode = targetMode
        mPrevInputKeyCode = 0

        var mode = OpenWnnEvent.Mode.DIRECT

        when (targetMode) {
            DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA -> {
                mInputType = INPUT_TYPE_TOGGLE
                mode = OpenWnnEvent.Mode.DEFAULT
            }

            DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET -> if (USE_ENGLISH_PREDICT) {
                mInputType = INPUT_TYPE_TOGGLE
                mode = OpenWnnEvent.Mode.NO_LV1_CONV
            } else {
                mInputType = INPUT_TYPE_TOGGLE
                mode = OpenWnnEvent.Mode.DIRECT
            }

            DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER -> {
                mInputType = INPUT_TYPE_INSTANT
                mode = OpenWnnEvent.Mode.DIRECT
                mCurrentInstantTable = INSTANT_CHAR_CODE_FULL_NUMBER
            }

            DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER -> {
                mInputType = INPUT_TYPE_INSTANT
                mode = OpenWnnEvent.Mode.DIRECT
                mCurrentInstantTable = INSTANT_CHAR_CODE_HALF_NUMBER
            }

            DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_KATAKANA -> {
                mInputType = INPUT_TYPE_TOGGLE
                mode = OpenWnnJAJP.Companion.ENGINE_MODE_FULL_KATAKANA
            }

            DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_ALPHABET -> {
                mInputType = INPUT_TYPE_TOGGLE
                mode = OpenWnnEvent.Mode.DIRECT
            }

            DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_KATAKANA -> {
                mInputType = INPUT_TYPE_TOGGLE
                mode = OpenWnnJAJP.Companion.ENGINE_MODE_HALF_KATAKANA
            }

            else -> {}
        }

        setStatusIcon()
        changeKeyboard(kbd)
        mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.CHANGE_MODE, mode))
    }

    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard.initView
     */
    override fun initView(parent: OpenWnn, width: Int, height: Int): View? {
        val view = super.initView(parent, width, height)
        changeKeyboard(mKeyboard[mCurrentLanguage][mDisplayMode][mCurrentKeyboardType][mShiftOn][mCurrentKeyMode][0])

        return view
    }

    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard.changeKeyboard
     */
    override fun changeKeyboard(keyboard: Keyboard?): Boolean {
        if (keyboard != null) {
            if (mIsInputTypeNull && mChangeModeKey != null) {
                mChangeModeKey!!.popupResId = mPopupResId
            }

            val keys = keyboard.keys
            val keyIndex = if ((KEY_NUMBER_12KEY < keys!!.size))
                KEY_INDEX_CHANGE_MODE_QWERTY
            else
                KEY_INDEX_CHANGE_MODE_12KEY
            mChangeModeKey = keys[keyIndex]

            if (mIsInputTypeNull && mChangeModeKey != null) {
                mPopupResId = mChangeModeKey!!.popupResId
                mChangeModeKey!!.popupResId = 0
            }
        }

        return super.changeKeyboard(keyboard)
    }

    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard.changeKeyboardType
     */
    override fun changeKeyboardType(type: Int) {
        commitText()
        val kbd = getTypeChangeKeyboard(type)
        if (kbd != null) {
            mCurrentKeyboardType = type
            mPrefEditor!!.putBoolean(
                "opt_enable_qwerty",
                type == DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY
            )
            mPrefEditor!!.commit()
            changeKeyboard(kbd)
        }
        if (type == DefaultSoftKeyboard.Companion.KEYBOARD_12KEY) {
            mWnn!!.onEvent(
                OpenWnnEvent(
                    OpenWnnEvent.Companion.CHANGE_MODE,
                    OpenWnnJAJP.Companion.ENGINE_MODE_OPT_TYPE_12KEY
                )
            )
        } else {
            mWnn!!.onEvent(
                OpenWnnEvent(
                    OpenWnnEvent.Companion.CHANGE_MODE,
                    OpenWnnJAJP.Companion.ENGINE_MODE_OPT_TYPE_QWERTY
                )
            )
        }
    }

    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard.onKey
     */
    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        var primaryCode = primaryCode
        if (mDisableKeyInput) {
            return
        }

        when (primaryCode) {
            DefaultSoftKeyboard.Companion.KEYCODE_JP12_TOGGLE_MODE, DefaultSoftKeyboard.Companion.KEYCODE_QWERTY_TOGGLE_MODE -> if (!mIsInputTypeNull) {
                nextKeyMode()
            }

            DefaultSoftKeyboard.Companion.KEYCODE_QWERTY_BACKSPACE, DefaultSoftKeyboard.Companion.KEYCODE_JP12_BACKSPACE -> mWnn!!.onEvent(
                OpenWnnEvent(
                    OpenWnnEvent.Companion.INPUT_SOFT_KEY,
                    KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
                )
            )

            DefaultSoftKeyboard.Companion.KEYCODE_QWERTY_SHIFT -> toggleShiftLock()
            DefaultSoftKeyboard.Companion.KEYCODE_QWERTY_ALT -> processAltKey()
            DefaultSoftKeyboard.Companion.KEYCODE_QWERTY_ENTER, DefaultSoftKeyboard.Companion.KEYCODE_JP12_ENTER -> mWnn!!.onEvent(
                OpenWnnEvent(
                    OpenWnnEvent.Companion.INPUT_SOFT_KEY,
                    KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
                )
            )

            DefaultSoftKeyboard.Companion.KEYCODE_JP12_REVERSE -> if (!mNoInput && !mEnableHardware12Keyboard) {
                mWnn!!.onEvent(
                    OpenWnnEvent(
                        OpenWnnEvent.Companion.TOGGLE_REVERSE_CHAR,
                        mCurrentCycleTable
                    )
                )
            }

            DefaultSoftKeyboard.Companion.KEYCODE_QWERTY_KBD -> changeKeyboardType(
                DefaultSoftKeyboard.Companion.KEYBOARD_12KEY
            )

            DefaultSoftKeyboard.Companion.KEYCODE_JP12_KBD -> changeKeyboardType(DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY)
            DefaultSoftKeyboard.Companion.KEYCODE_JP12_EMOJI, DefaultSoftKeyboard.Companion.KEYCODE_QWERTY_EMOJI -> {
                commitText()
                mWnn!!.onEvent(
                    OpenWnnEvent(
                        OpenWnnEvent.Companion.CHANGE_MODE,
                        OpenWnnJAJP.Companion.ENGINE_MODE_SYMBOL
                    )
                )
            }

            DefaultSoftKeyboard.Companion.KEYCODE_JP12_1, DefaultSoftKeyboard.Companion.KEYCODE_JP12_2, DefaultSoftKeyboard.Companion.KEYCODE_JP12_3, DefaultSoftKeyboard.Companion.KEYCODE_JP12_4, DefaultSoftKeyboard.Companion.KEYCODE_JP12_5, DefaultSoftKeyboard.Companion.KEYCODE_JP12_6, DefaultSoftKeyboard.Companion.KEYCODE_JP12_7, DefaultSoftKeyboard.Companion.KEYCODE_JP12_8, DefaultSoftKeyboard.Companion.KEYCODE_JP12_9, DefaultSoftKeyboard.Companion.KEYCODE_JP12_0, DefaultSoftKeyboard.Companion.KEYCODE_JP12_SHARP ->             /* Processing to input by ten key */
                if (mInputType == INPUT_TYPE_INSTANT) {
                    /* Send a input character directly if instant input type is selected */
                    commitText()
                    mWnn!!.onEvent(
                        OpenWnnEvent(
                            OpenWnnEvent.Companion.INPUT_CHAR,
                            mCurrentInstantTable!![getTableIndex(primaryCode)]
                        )
                    )
                } else {
                    if ((mPrevInputKeyCode != primaryCode)) {
                        mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.TOUCH_OTHER_KEY))
                        if ((mCurrentKeyMode == DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET)
                            && (primaryCode == DefaultSoftKeyboard.Companion.KEYCODE_JP12_SHARP)
                        ) {
                            /* Commit text by symbol character (',' '.') when alphabet input mode is selected */
                            commitText()
                        }
                    }

                    /* Convert the key code to the table index and send the toggle event with the table index */
                    val cycleTable = cycleTable
                    if (cycleTable == null) {
                        Log.e("OpenWnn", "not founds cycle table")
                    } else {
                        val index = getTableIndex(primaryCode)
                        mWnn!!.onEvent(
                            OpenWnnEvent(
                                OpenWnnEvent.Companion.TOGGLE_CHAR,
                                cycleTable[index]
                            )
                        )
                        mCurrentCycleTable = cycleTable[index]
                    }
                    mPrevInputKeyCode = primaryCode
                }

            DefaultSoftKeyboard.Companion.KEYCODE_JP12_ASTER -> if (mInputType == INPUT_TYPE_INSTANT) {
                commitText()
                mWnn!!.onEvent(
                    OpenWnnEvent(
                        OpenWnnEvent.Companion.INPUT_CHAR,
                        mCurrentInstantTable!![getTableIndex(primaryCode)]
                    )
                )
            } else {
                if (!mNoInput) {
                    /* Processing to toggle Dakuten, Handakuten, and capital */
                    val replaceTable = replaceTable
                    if (replaceTable == null) {
                        Log.e("OpenWnn", "not founds replace table")
                    } else {
                        mWnn!!.onEvent(
                            OpenWnnEvent(
                                OpenWnnEvent.Companion.REPLACE_CHAR,
                                replaceTable
                            )
                        )
                        mPrevInputKeyCode = primaryCode
                    }
                }
            }

            KEYCODE_SWITCH_FULL_HIRAGANA ->             /* Change mode to Full width hiragana */
                changeKeyMode(DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA)

            KEYCODE_SWITCH_FULL_KATAKANA ->             /* Change mode to Full width katakana */
                changeKeyMode(DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_KATAKANA)

            KEYCODE_SWITCH_FULL_ALPHABET ->             /* Change mode to Full width alphabet */
                changeKeyMode(DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_ALPHABET)

            KEYCODE_SWITCH_FULL_NUMBER ->             /* Change mode to Full width numeric */
                changeKeyMode(DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER)

            KEYCODE_SWITCH_HALF_KATAKANA ->             /* Change mode to Half width katakana */
                changeKeyMode(DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_KATAKANA)

            KEYCODE_SWITCH_HALF_ALPHABET ->
                /* Change mode to Half width alphabet */
                changeKeyMode(DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET)

            KEYCODE_SWITCH_HALF_NUMBER ->             /* Change mode to Half width numeric */
                changeKeyMode(DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER)

            KEYCODE_SELECT_CASE -> {
                val shifted = if ((mShiftOn == 0)) 1 else 0
                val newKeyboard = getShiftChangeKeyboard(shifted)
                if (newKeyboard != null) {
                    mShiftOn = shifted
                    changeKeyboard(newKeyboard)
                }
            }

            DefaultSoftKeyboard.Companion.KEYCODE_JP12_SPACE -> if ((mCurrentKeyMode == DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA) && !mNoInput) {
                mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.CONVERT))
            } else {
                mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.INPUT_CHAR, ' '))
            }

            KEYCODE_EISU_KANA -> mWnn!!.onEvent(
                OpenWnnEvent(
                    OpenWnnEvent.Companion.CHANGE_MODE,
                    OpenWnnJAJP.Companion.ENGINE_MODE_EISU_KANA
                )
            )

            DefaultSoftKeyboard.Companion.KEYCODE_JP12_CLOSE -> mWnn!!.onEvent(
                OpenWnnEvent(
                    OpenWnnEvent.Companion.INPUT_KEY,
                    KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)
                )
            )

            DefaultSoftKeyboard.Companion.KEYCODE_JP12_LEFT -> mWnn!!.onEvent(
                OpenWnnEvent(
                    OpenWnnEvent.Companion.INPUT_SOFT_KEY,
                    KeyEvent(
                        KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_LEFT
                    )
                )
            )

            DefaultSoftKeyboard.Companion.KEYCODE_JP12_RIGHT -> mWnn!!.onEvent(
                OpenWnnEvent(
                    OpenWnnEvent.Companion.INPUT_SOFT_KEY,
                    KeyEvent(
                        KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_RIGHT
                    )
                )
            )

            KEYCODE_NOP -> {}
            else -> if (primaryCode >= 0) {
                if (mKeyboardView!!.isShifted) {
                    primaryCode = primaryCode.uppercaseChar()
                }
                mWnn!!.onEvent(
                    OpenWnnEvent(
                        OpenWnnEvent.Companion.INPUT_CHAR,
                        primaryCode.toChar()
                    )
                )
            }
        }

        /* update shift key's state */
        if (!mCapsLock && (primaryCode != DefaultSoftKeyboard.Companion.KEYCODE_QWERTY_SHIFT)) {
            setShiftByEditorInfo()
        }
    }

    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard.setPreferences
     */
    override fun setPreferences(pref: SharedPreferences, editor: EditorInfo) {
        mPrefEditor = pref.edit()
        val isQwerty = if (OpenWnn.Companion.isXLarge()) {
            pref.getBoolean("opt_enable_qwerty", true)
        } else {
            pref.getBoolean("opt_enable_qwerty", false)
        }

        if (isQwerty && (mCurrentKeyboardType == DefaultSoftKeyboard.Companion.KEYBOARD_12KEY)) {
            changeKeyboardType(DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY)
        }

        super.setPreferences(pref, editor)

        val inputType = editor.inputType
        if (mHardKeyboardHidden) {
            if (inputType == EditorInfo.TYPE_NULL) {
                if (!mIsInputTypeNull) {
                    mIsInputTypeNull = true
                    if (mChangeModeKey != null) {
                        mPopupResId = mChangeModeKey!!.popupResId
                        mChangeModeKey!!.popupResId = 0
                    }
                }
                return
            }

            if (mIsInputTypeNull) {
                mIsInputTypeNull = false
                if (mChangeModeKey != null) {
                    mChangeModeKey!!.popupResId = mPopupResId
                }
            }
        }

        mEnableAutoCaps = pref.getBoolean("auto_caps", true)
        mLimitedKeyMode = null
        mPreferenceKeyMode = INVALID_KEYMODE
        mNoInput = true
        mDisableKeyInput = false
        mCapsLock = false

        when (inputType and EditorInfo.TYPE_MASK_CLASS) {
            EditorInfo.TYPE_CLASS_NUMBER, EditorInfo.TYPE_CLASS_DATETIME -> mPreferenceKeyMode =
                DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER

            EditorInfo.TYPE_CLASS_PHONE -> mLimitedKeyMode = if (mHardKeyboardHidden) {
                intArrayOf(DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE)
            } else if (mEnableHardware12Keyboard) {
                intArrayOf(DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER)
            } else {
                intArrayOf(DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET)
            }

            EditorInfo.TYPE_CLASS_TEXT -> when (inputType and EditorInfo.TYPE_MASK_VARIATION) {
                EditorInfo.TYPE_TEXT_VARIATION_PASSWORD, EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> mLimitedKeyMode =
                    intArrayOf(
                        DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET,
                        DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER
                    )

                EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, EditorInfo.TYPE_TEXT_VARIATION_URI -> mPreferenceKeyMode =
                    DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET

                else -> {}
            }

            else -> {}
        }

        if (inputType != mLastInputType) {
            setDefaultKeyboard()
            mLastInputType = inputType
        }

        setStatusIcon()
        setShiftByEditorInfo()
    }

    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard.onUpdateState
     */
    override fun onUpdateState(parent: OpenWnn) {
        super.onUpdateState(parent)
        if (!mCapsLock) {
            setShiftByEditorInfo()
        }
    }

    /**
     * Change the keyboard to default
     */
    fun setDefaultKeyboard() {
        val locale = Locale.getDefault()
        var keymode: Int = DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA

        if (mPreferenceKeyMode != INVALID_KEYMODE) {
            keymode = mPreferenceKeyMode
        } else if (mLimitedKeyMode != null) {
            keymode = mLimitedKeyMode!![0]
        } else {
            if (locale.language != Locale.JAPANESE.language) {
                keymode = DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET
            }
        }
        changeKeyMode(keymode)
    }


    /**
     * Change to the next input mode
     */
    fun nextKeyMode() {
        /* Search the current mode in the toggle table */
        var found = false
        var index: Int
        index = 0
        while (index < JP_MODE_CYCLE_TABLE.size) {
            if (JP_MODE_CYCLE_TABLE[index] == mCurrentKeyMode) {
                found = true
                break
            }
            index++
        }

        if (!found) {
            /* If the current mode not exists, set the default mode */
            setDefaultKeyboard()
        } else {
            /* If the current mode exists, set the next input mode */
            val size = JP_MODE_CYCLE_TABLE.size
            var keyMode = INVALID_KEYMODE
            for (i in 0 until size) {
                index = (++index) % size

                keyMode = filterKeyMode(JP_MODE_CYCLE_TABLE[index])
                if (keyMode != INVALID_KEYMODE) {
                    break
                }
            }

            if (keyMode != INVALID_KEYMODE) {
                changeKeyMode(keyMode)
            }
        }
    }

    /**
     * Create the keyboard for portrait mode
     * <br></br>
     * @param parent  The context
     */
    private fun createKeyboardsPortrait(parent: OpenWnn) {
        var keyList: Array<Array<Keyboard?>>
        if (OpenWnn.Companion.isXLarge()) {
            /* qwerty shift_off (portrait) */
            keyList =
                mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.PORTRAIT][DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_full_symbols)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_alphabet)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_symbols)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE][0] =
                Keyboard(parent, R.xml.keyboard_12key_phone)

            /* qwerty shift_on (portrait) */
            keyList =
                mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.PORTRAIT][DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_ON]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_full_symbols_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_alphabet_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_symbols_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE][0] =
                mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.PORTRAIT][DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE][0]
        } else {
            /* qwerty shift_off (portrait) */
            keyList =
                mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.PORTRAIT][DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_ALPHABET][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_full_alphabet)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_full_symbols)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_KATAKANA][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_full_katakana)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_alphabet)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_symbols)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_KATAKANA][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_katakana)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE][0] =
                Keyboard(parent, R.xml.keyboard_12key_phone)

            /* qwerty shift_on (portrait) */
            keyList =
                mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.PORTRAIT][DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_ON]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_ALPHABET][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_full_alphabet_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_full_symbols_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_KATAKANA][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_full_katakana_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_alphabet_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_symbols_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_KATAKANA][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_katakana_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE][0] =
                mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.PORTRAIT][DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE][0]


            /* 12-keys shift_off (portrait) */
            keyList =
                mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.PORTRAIT][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA][0] =
                Keyboard(parent, R.xml.keyboard_12keyjp)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA][1] =
                Keyboard(parent, R.xml.keyboard_12keyjp_input)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_ALPHABET][0] =
                Keyboard(parent, R.xml.keyboard_12key_full_alphabet)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_ALPHABET][1] =
                Keyboard(parent, R.xml.keyboard_12key_full_alphabet_input)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_12key_full_num)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_KATAKANA][0] =
                Keyboard(parent, R.xml.keyboard_12key_full_katakana)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_KATAKANA][1] =
                Keyboard(parent, R.xml.keyboard_12key_full_katakana_input)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET][0] =
                Keyboard(parent, R.xml.keyboard_12key_half_alphabet)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET][1] =
                Keyboard(parent, R.xml.keyboard_12key_half_alphabet_input)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_12key_half_num)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_KATAKANA][0] =
                Keyboard(parent, R.xml.keyboard_12key_half_katakana)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_KATAKANA][1] =
                Keyboard(parent, R.xml.keyboard_12key_half_katakana_input)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE][0] =
                Keyboard(parent, R.xml.keyboard_12key_phone)

            /* 12-keys shift_on (portrait) */
            keyList =
                mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.PORTRAIT][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_ON]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA]
            = mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.PORTRAIT][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_ALPHABET]
            = mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.PORTRAIT][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_ALPHABET]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER]
            = mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.PORTRAIT][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_KATAKANA]
            = mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.PORTRAIT][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_KATAKANA]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET]
            = mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.PORTRAIT][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET]

            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER]
            = mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.PORTRAIT][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_KATAKANA]
            = mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.PORTRAIT][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_KATAKANA]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE]
            = mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.PORTRAIT][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE]
        }
    }

    /**
     * Create the keyboard for landscape mode
     * <br></br>
     * @param parent  The context
     */
    private fun createKeyboardsLandscape(parent: OpenWnn) {
        var keyList: Array<Array<Keyboard?>>
        if (OpenWnn.Companion.isXLarge()) {
            /* qwerty shift_off (landscape) */
            keyList =
                mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.LANDSCAPE][DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_full_symbols)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_alphabet)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_symbols)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE][0] =
                Keyboard(parent, R.xml.keyboard_12key_phone)

            /* qwerty shift_on (landscape) */
            keyList =
                mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.LANDSCAPE][DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_ON]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_full_symbols_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_alphabet_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_symbols_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE][0] =
                mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.LANDSCAPE][DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE][0]
        } else {
            /* qwerty shift_off (landscape) */
            keyList =
                mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.LANDSCAPE][DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_ALPHABET][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_full_alphabet)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_full_symbols)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_KATAKANA][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_full_katakana)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_alphabet)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_symbols)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_KATAKANA][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_katakana)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE][0] =
                Keyboard(parent, R.xml.keyboard_12key_phone)

            /* qwerty shift_on (landscape) */
            keyList =
                mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.LANDSCAPE][DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_ON]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_ALPHABET][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_full_alphabet_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_full_symbols_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_KATAKANA][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_full_katakana_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_alphabet_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_symbols_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_KATAKANA][0] =
                Keyboard(parent, R.xml.keyboard_qwerty_jp_half_katakana_shift)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE][0] =
                mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.LANDSCAPE][DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE][0]


            /* 12-keys shift_off (landscape) */
            keyList =
                mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.LANDSCAPE][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA][0] =
                Keyboard(parent, R.xml.keyboard_12keyjp)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA][1] =
                Keyboard(parent, R.xml.keyboard_12keyjp_input)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_ALPHABET][0] =
                Keyboard(parent, R.xml.keyboard_12key_full_alphabet)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_ALPHABET][1] =
                Keyboard(parent, R.xml.keyboard_12key_full_alphabet_input)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_12key_full_num)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_KATAKANA][0] =
                Keyboard(parent, R.xml.keyboard_12key_full_katakana)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_KATAKANA][1] =
                Keyboard(parent, R.xml.keyboard_12key_full_katakana_input)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET][0] =
                Keyboard(parent, R.xml.keyboard_12key_half_alphabet)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET][1] =
                Keyboard(parent, R.xml.keyboard_12key_half_alphabet_input)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER][0] =
                Keyboard(parent, R.xml.keyboard_12key_half_num)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_KATAKANA][0] =
                Keyboard(parent, R.xml.keyboard_12key_half_katakana)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_KATAKANA][1] =
                Keyboard(parent, R.xml.keyboard_12key_half_katakana_input)
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE][0] =
                Keyboard(parent, R.xml.keyboard_12key_phone)

            /* 12-keys shift_on (landscape) */
            keyList =
                mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.LANDSCAPE][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_ON]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA]
            = mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.LANDSCAPE][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_ALPHABET]
            = mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.LANDSCAPE][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_ALPHABET]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER]
            = mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.LANDSCAPE][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_KATAKANA]
            = mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.LANDSCAPE][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_KATAKANA]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET]
            = mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.LANDSCAPE][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET]

            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER]
            = mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.LANDSCAPE][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_KATAKANA]
            = mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.LANDSCAPE][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_KATAKANA]
            keyList[DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE]
            = mKeyboard[DefaultSoftKeyboard.Companion.LANG_JA][DefaultSoftKeyboard.Companion.LANDSCAPE][DefaultSoftKeyboard.Companion.KEYBOARD_12KEY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE]
        }
    }

    /**
     * Convert the key code to the index of table
     * <br></br>
     * @param index     The key code
     * @return          The index of the toggle table for input
     */
    private fun getTableIndex(keyCode: Int): Int {
        val index =
            if ((keyCode == DefaultSoftKeyboard.Companion.KEYCODE_JP12_1)) 0 else if ((keyCode == DefaultSoftKeyboard.Companion.KEYCODE_JP12_2)) 1 else if ((keyCode == DefaultSoftKeyboard.Companion.KEYCODE_JP12_3)) 2 else if ((keyCode == DefaultSoftKeyboard.Companion.KEYCODE_JP12_4)) 3 else if ((keyCode == DefaultSoftKeyboard.Companion.KEYCODE_JP12_5)) 4 else if ((keyCode == DefaultSoftKeyboard.Companion.KEYCODE_JP12_6)) 5 else if ((keyCode == DefaultSoftKeyboard.Companion.KEYCODE_JP12_7)) 6 else if ((keyCode == DefaultSoftKeyboard.Companion.KEYCODE_JP12_8)) 7 else if ((keyCode == DefaultSoftKeyboard.Companion.KEYCODE_JP12_9)) 8 else if ((keyCode == DefaultSoftKeyboard.Companion.KEYCODE_JP12_0)) 9 else if ((keyCode == DefaultSoftKeyboard.Companion.KEYCODE_JP12_SHARP)) 10 else if ((keyCode == DefaultSoftKeyboard.Companion.KEYCODE_JP12_ASTER)) 11 else 0

        return index
    }

    private val cycleTable: Array<Array<String>>?
        /**
         * Get the toggle table for input that is appropriate in current mode.
         *
         * @return      The toggle table for input
         */
        get() {
            var cycleTable: Array<Array<String>>? = null
            when (mCurrentKeyMode) {
                DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA -> cycleTable =
                    JP_FULL_HIRAGANA_CYCLE_TABLE

                DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_KATAKANA -> cycleTable =
                    JP_FULL_KATAKANA_CYCLE_TABLE

                DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_ALPHABET -> cycleTable =
                    JP_FULL_ALPHABET_CYCLE_TABLE

                DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER, DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER -> {}
                DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET -> cycleTable =
                    JP_HALF_ALPHABET_CYCLE_TABLE

                DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_KATAKANA -> cycleTable =
                    JP_HALF_KATAKANA_CYCLE_TABLE

                else -> {}
            }
            return cycleTable
        }

    private val replaceTable: HashMap<*, *>?
        /**
         * Get the replace table that is appropriate in current mode.
         *
         * @return      The replace table
         */
        get() {
            var hashTable: HashMap<*, *>? = null
            when (mCurrentKeyMode) {
                DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA -> hashTable =
                    JP_FULL_HIRAGANA_REPLACE_TABLE

                DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_KATAKANA -> hashTable =
                    JP_FULL_KATAKANA_REPLACE_TABLE

                DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_ALPHABET -> hashTable =
                    JP_FULL_ALPHABET_REPLACE_TABLE

                DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER, DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER -> {}
                DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET -> hashTable =
                    JP_HALF_ALPHABET_REPLACE_TABLE

                DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_KATAKANA -> hashTable =
                    JP_HALF_KATAKANA_REPLACE_TABLE

                else -> {}
            }
            return hashTable
        }

    /**
     * Set the status icon that is appropriate in current mode
     */
    private fun setStatusIcon() {
        var icon = 0

        when (mCurrentKeyMode) {
            DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA -> icon =
                R.drawable.immodeic_hiragana

            DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_KATAKANA -> icon =
                R.drawable.immodeic_full_kana

            DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_ALPHABET -> icon =
                R.drawable.immodeic_full_alphabet

            DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER -> icon =
                R.drawable.immodeic_full_number

            DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_KATAKANA -> icon =
                R.drawable.immodeic_half_kana

            DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET -> icon =
                R.drawable.immodeic_half_alphabet

            DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER, DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_PHONE -> icon =
                R.drawable.immodeic_half_number

            else -> {}
        }

        mWnn!!.showStatusIcon(icon)
    }

    /**
     * Get the shift key state from the editor.
     * <br></br>
     * @param editor    The editor information
     * @return          The state id of the shift key (0:off, 1:on)
     */
    protected fun getShiftKeyState(editor: EditorInfo): Int {
        val connection = mWnn!!.currentInputConnection
        if (connection != null) {
            val caps = connection.getCursorCapsMode(editor.inputType)
            return if ((caps == 0)) 0 else 1
        } else {
            return 0
        }
    }

    /**
     * Set the shift key state from [EditorInfo].
     */
    private fun setShiftByEditorInfo() {
        if (mEnableAutoCaps && (mCurrentKeyMode == DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET)) {
            val shift = getShiftKeyState(mWnn!!.currentInputEditorInfo)

            mShiftOn = shift
            changeKeyboard(getShiftChangeKeyboard(shift))
        }
    }

    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard.setHardKeyboardHidden
     */
    override fun setHardKeyboardHidden(hidden: Boolean) {
        if (mWnn != null) {
            if (!hidden) {
                if (mEnableHardware12Keyboard) {
                    mWnn!!.onEvent(
                        OpenWnnEvent(
                            OpenWnnEvent.Companion.CHANGE_MODE,
                            OpenWnnJAJP.Companion.ENGINE_MODE_OPT_TYPE_12KEY
                        )
                    )
                } else {
                    mWnn!!.onEvent(
                        OpenWnnEvent(
                            OpenWnnEvent.Companion.CHANGE_MODE,
                            OpenWnnJAJP.Companion.ENGINE_MODE_OPT_TYPE_QWERTY
                        )
                    )
                }
            }

            if (mHardKeyboardHidden != hidden) {
                if ((mLimitedKeyMode != null)
                    || (!mEnableHardware12Keyboard
                            && (mCurrentKeyMode != DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA)
                            && (mCurrentKeyMode != DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET))
                ) {
                    mLastInputType = EditorInfo.TYPE_NULL
                    if (mWnn!!.isInputViewShown) {
                        setDefaultKeyboard()
                    }
                }
            }
        }
        super.setHardKeyboardHidden(hidden)
    }

    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard.setHardware12Keyboard
     */
    override fun setHardware12Keyboard(type12Key: Boolean) {
        if (mWnn != null) {
            if (mEnableHardware12Keyboard != type12Key) {
                if (type12Key) {
                    mWnn!!.onEvent(
                        OpenWnnEvent(
                            OpenWnnEvent.Companion.CHANGE_MODE,
                            OpenWnnJAJP.Companion.ENGINE_MODE_OPT_TYPE_12KEY
                        )
                    )
                } else {
                    mWnn!!.onEvent(
                        OpenWnnEvent(
                            OpenWnnEvent.Companion.CHANGE_MODE,
                            OpenWnnJAJP.Companion.ENGINE_MODE_OPT_TYPE_QWERTY
                        )
                    )
                }
            }
        }
        super.setHardware12Keyboard(type12Key)
    }

    /**
     * Change the key-mode to the allowed one which is restricted
     * by the text input field or the type of the keyboard.
     * @param keyMode The key-mode
     * @return the key-mode allowed
     */
    private fun filterKeyMode(keyMode: Int): Int {
        var targetMode = keyMode
        val limits = mLimitedKeyMode

        if (!mHardKeyboardHidden) { /* for hardware keyboard */
            if (!mEnableHardware12Keyboard && (targetMode != DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA)
                && (targetMode != DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET)
            ) {
                val locale = Locale.getDefault()
                var keymode: Int = DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET
                if (locale.language == Locale.JAPANESE.language) {
                    when (targetMode) {
                        DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA, DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_KATAKANA, DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_KATAKANA -> keymode =
                            DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA

                        else -> {}
                    }
                }
                targetMode = keymode
            }
        }


        /* restrict by the type of the text field */
        if (limits != null) {
            var hasAccepted = false
            var hasRequiredChange = true
            val size = limits.size
            val nowMode = mCurrentKeyMode

            for (i in 0 until size) {
                if (targetMode == limits[i]) {
                    hasAccepted = true
                    break
                }
                if (nowMode == limits[i]) {
                    hasRequiredChange = false
                }
            }

            if (!hasAccepted) {
                targetMode = if (hasRequiredChange) {
                    mLimitedKeyMode!![0]
                } else {
                    INVALID_KEYMODE
                }
            }
        }

        return targetMode
    }

    /**
     * Shows input mode choosing dialog.
     *
     * @return boolean
     */
    fun showInputModeSwitchDialog(): Boolean {
        val baseInputView = currentView as BaseInputView
        val builder = AlertDialog.Builder(baseInputView.context)
        builder.setCancelable(true)
        builder.setNegativeButton(R.string.dialog_button_cancel, null)

        val r = baseInputView.resources
        val itemFullHirakana: CharSequence =
            r.getString(R.string.ti_input_mode_full_hirakana_title_txt)
        val itemFullKatakana: CharSequence =
            r.getString(R.string.ti_input_mode_full_katakana_title_txt)
        val itemHalfKatakana: CharSequence =
            r.getString(R.string.ti_input_mode_half_katakana_title_txt)
        val itemFullAlphabet: CharSequence =
            r.getString(R.string.ti_input_mode_full_alphabet_title_txt)
        val itemHalfAlphabet: CharSequence =
            r.getString(R.string.ti_input_mode_half_alphabet_title_txt)
        val itemFullNumber: CharSequence = r.getString(R.string.ti_input_mode_full_number_title_txt)
        val itemHalfNumber: CharSequence = r.getString(R.string.ti_input_mode_half_number_title_txt)
        val itemTitles: Array<CharSequence>
        val itemValues: IntArray
        if (OpenWnn.Companion.isXLarge()) {
            itemTitles = arrayOf(
                itemFullHirakana, itemHalfAlphabet,
                itemFullNumber, itemHalfNumber
            )
            itemValues = intArrayOf(
                DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA,
                DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET,
                DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER,
                DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER
            )
        } else {
            itemTitles = arrayOf(
                itemFullHirakana, itemFullKatakana, itemHalfKatakana,
                itemFullAlphabet, itemHalfAlphabet, itemFullNumber,
                itemHalfNumber
            )
            itemValues = intArrayOf(
                DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA,
                DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_KATAKANA,
                DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_KATAKANA,
                DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_ALPHABET,
                DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET,
                DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_NUMBER,
                DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER
            )
        }

        builder.setSingleChoiceItems(
            itemTitles, findIndexOfValue(
                itemValues,
                mCurrentKeyMode
            )
        ) { inputModeSwitchDialog, position ->
            when (position) {
                0 -> changeKeyMode(itemValues[0])
                1 -> changeKeyMode(itemValues[1])
                2 -> changeKeyMode(itemValues[2])
                3 -> changeKeyMode(itemValues[3])
                4 -> changeKeyMode(itemValues[4])
                5 -> changeKeyMode(itemValues[5])
                6 -> changeKeyMode(itemValues[6])
                else -> {}
            }
            inputModeSwitchDialog.dismiss()
        }

        builder.setTitle(r.getString(R.string.ti_long_press_dialog_input_mode_txt))
        baseInputView.showDialog(builder)
        return true
    }

    /**
     * Finds the index of a value in a int[].
     *
     * @param value   the int[] to search in,
     * @param mode    the value need to find index,
     * @return the index of the value.
     */
    private fun findIndexOfValue(value: IntArray, mode: Int): Int {
        for (i in value.indices) {
            if (value[i] == mode) {
                return i
            }
        }
        return -1
    }

    companion object {
        /** Enable English word prediction on half-width alphabet mode  */
        private const val USE_ENGLISH_PREDICT = true

        /** Key code for switching to full-width HIRAGANA mode  */
        private const val KEYCODE_SWITCH_FULL_HIRAGANA = -301

        /** Key code for switching to full-width KATAKANA mode  */
        private const val KEYCODE_SWITCH_FULL_KATAKANA = -302

        /** Key code for switching to full-width alphabet mode  */
        private const val KEYCODE_SWITCH_FULL_ALPHABET = -303

        /** Key code for switching to full-width number mode  */
        private const val KEYCODE_SWITCH_FULL_NUMBER = -304

        /** Key code for switching to half-width KATAKANA mode  */
        private const val KEYCODE_SWITCH_HALF_KATAKANA = -306

        /** Key code for switching to half-width alphabet mode  */
        private const val KEYCODE_SWITCH_HALF_ALPHABET = -307

        /** Key code for switching to half-width number mode  */
        private const val KEYCODE_SWITCH_HALF_NUMBER = -308

        /** Key code for case toggle key  */
        private const val KEYCODE_SELECT_CASE = -309

        /** Key code for EISU-KANA conversion  */
        private const val KEYCODE_EISU_KANA = -305

        /** Key code for NOP (no-operation)  */
        private const val KEYCODE_NOP = -310


        /** Input mode toggle cycle table  */
        private val JP_MODE_CYCLE_TABLE = intArrayOf(
            DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA,
            DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET,
            DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_NUMBER
        )

        /** Definition for `mInputType` (toggle)  */
        private const val INPUT_TYPE_TOGGLE = 1

        /** Definition for `mInputType` (commit instantly)  */
        private const val INPUT_TYPE_INSTANT = 2

        /** Max key number of the 12 key keyboard (depends on the definition of keyboards)  */
        private const val KEY_NUMBER_12KEY = 20

        /** Toggle cycle table for full-width HIRAGANA  */
        private val JP_FULL_HIRAGANA_CYCLE_TABLE = arrayOf(
            arrayOf(
                "\u3042",
                "\u3044",
                "\u3046",
                "\u3048",
                "\u304a",
                "\u3041",
                "\u3043",
                "\u3045",
                "\u3047",
                "\u3049"
            ),
            arrayOf("\u304b", "\u304d", "\u304f", "\u3051", "\u3053"),
            arrayOf("\u3055", "\u3057", "\u3059", "\u305b", "\u305d"),
            arrayOf("\u305f", "\u3061", "\u3064", "\u3066", "\u3068", "\u3063"),
            arrayOf("\u306a", "\u306b", "\u306c", "\u306d", "\u306e"),
            arrayOf("\u306f", "\u3072", "\u3075", "\u3078", "\u307b"),
            arrayOf("\u307e", "\u307f", "\u3080", "\u3081", "\u3082"),
            arrayOf("\u3084", "\u3086", "\u3088", "\u3083", "\u3085", "\u3087"),
            arrayOf("\u3089", "\u308a", "\u308b", "\u308c", "\u308d"),
            arrayOf("\u308f", "\u3092", "\u3093", "\u308e", "\u30fc"),
            arrayOf("\u3001", "\u3002", "\uff1f", "\uff01", "\u30fb", "\u3000"),
        )

        /** Replace table for full-width HIRAGANA  */
        private val JP_FULL_HIRAGANA_REPLACE_TABLE: HashMap<String, String> =
            object : HashMap<String?, String?>() {
                init {
                    put("\u3042", "\u3041")
                    put("\u3044", "\u3043")
                    put("\u3046", "\u3045")
                    put("\u3048", "\u3047")
                    put("\u304a", "\u3049")
                    put("\u3041", "\u3042")
                    put("\u3043", "\u3044")
                    put("\u3045", "\u30f4")
                    put("\u3047", "\u3048")
                    put("\u3049", "\u304a")
                    put("\u304b", "\u304c")
                    put("\u304d", "\u304e")
                    put("\u304f", "\u3050")
                    put("\u3051", "\u3052")
                    put("\u3053", "\u3054")
                    put("\u304c", "\u304b")
                    put("\u304e", "\u304d")
                    put("\u3050", "\u304f")
                    put("\u3052", "\u3051")
                    put("\u3054", "\u3053")
                    put("\u3055", "\u3056")
                    put("\u3057", "\u3058")
                    put("\u3059", "\u305a")
                    put("\u305b", "\u305c")
                    put("\u305d", "\u305e")
                    put("\u3056", "\u3055")
                    put("\u3058", "\u3057")
                    put("\u305a", "\u3059")
                    put("\u305c", "\u305b")
                    put("\u305e", "\u305d")
                    put("\u305f", "\u3060")
                    put("\u3061", "\u3062")
                    put("\u3064", "\u3063")
                    put("\u3066", "\u3067")
                    put("\u3068", "\u3069")
                    put("\u3060", "\u305f")
                    put("\u3062", "\u3061")
                    put("\u3063", "\u3065")
                    put("\u3067", "\u3066")
                    put("\u3069", "\u3068")
                    put("\u3065", "\u3064")
                    put("\u30f4", "\u3046")
                    put("\u306f", "\u3070")
                    put("\u3072", "\u3073")
                    put("\u3075", "\u3076")
                    put("\u3078", "\u3079")
                    put("\u307b", "\u307c")
                    put("\u3070", "\u3071")
                    put("\u3073", "\u3074")
                    put("\u3076", "\u3077")
                    put("\u3079", "\u307a")
                    put("\u307c", "\u307d")
                    put("\u3071", "\u306f")
                    put("\u3074", "\u3072")
                    put("\u3077", "\u3075")
                    put("\u307a", "\u3078")
                    put("\u307d", "\u307b")
                    put("\u3084", "\u3083")
                    put("\u3086", "\u3085")
                    put("\u3088", "\u3087")
                    put("\u3083", "\u3084")
                    put("\u3085", "\u3086")
                    put("\u3087", "\u3088")
                    put("\u308f", "\u308e")
                    put("\u308e", "\u308f")
                    put("\u309b", "\u309c")
                    put("\u309c", "\u309b")
                }
            }

        /** Toggle cycle table for full-width KATAKANA  */
        private val JP_FULL_KATAKANA_CYCLE_TABLE = arrayOf(
            arrayOf(
                "\u30a2", "\u30a4", "\u30a6", "\u30a8", "\u30aa", "\u30a1", "\u30a3",
                "\u30a5", "\u30a7", "\u30a9"
            ),
            arrayOf("\u30ab", "\u30ad", "\u30af", "\u30b1", "\u30b3"),
            arrayOf("\u30b5", "\u30b7", "\u30b9", "\u30bb", "\u30bd"),
            arrayOf("\u30bf", "\u30c1", "\u30c4", "\u30c6", "\u30c8", "\u30c3"),
            arrayOf("\u30ca", "\u30cb", "\u30cc", "\u30cd", "\u30ce"),
            arrayOf("\u30cf", "\u30d2", "\u30d5", "\u30d8", "\u30db"),
            arrayOf("\u30de", "\u30df", "\u30e0", "\u30e1", "\u30e2"),
            arrayOf("\u30e4", "\u30e6", "\u30e8", "\u30e3", "\u30e5", "\u30e7"),
            arrayOf("\u30e9", "\u30ea", "\u30eb", "\u30ec", "\u30ed"),
            arrayOf("\u30ef", "\u30f2", "\u30f3", "\u30ee", "\u30fc"),
            arrayOf("\u3001", "\u3002", "\uff1f", "\uff01", "\u30fb", "\u3000")
        )

        /** Replace table for full-width KATAKANA  */
        private val JP_FULL_KATAKANA_REPLACE_TABLE: HashMap<String, String> =
            object : HashMap<String?, String?>() {
                init {
                    put("\u30a2", "\u30a1")
                    put("\u30a4", "\u30a3")
                    put("\u30a6", "\u30a5")
                    put("\u30a8", "\u30a7")
                    put("\u30aa", "\u30a9")
                    put("\u30a1", "\u30a2")
                    put("\u30a3", "\u30a4")
                    put("\u30a5", "\u30f4")
                    put("\u30a7", "\u30a8")
                    put("\u30a9", "\u30aa")
                    put("\u30ab", "\u30ac")
                    put("\u30ad", "\u30ae")
                    put("\u30af", "\u30b0")
                    put("\u30b1", "\u30b2")
                    put("\u30b3", "\u30b4")
                    put("\u30ac", "\u30ab")
                    put("\u30ae", "\u30ad")
                    put("\u30b0", "\u30af")
                    put("\u30b2", "\u30b1")
                    put("\u30b4", "\u30b3")
                    put("\u30b5", "\u30b6")
                    put("\u30b7", "\u30b8")
                    put("\u30b9", "\u30ba")
                    put("\u30bb", "\u30bc")
                    put("\u30bd", "\u30be")
                    put("\u30b6", "\u30b5")
                    put("\u30b8", "\u30b7")
                    put("\u30ba", "\u30b9")
                    put("\u30bc", "\u30bb")
                    put("\u30be", "\u30bd")
                    put("\u30bf", "\u30c0")
                    put("\u30c1", "\u30c2")
                    put("\u30c4", "\u30c3")
                    put("\u30c6", "\u30c7")
                    put("\u30c8", "\u30c9")
                    put("\u30c0", "\u30bf")
                    put("\u30c2", "\u30c1")
                    put("\u30c3", "\u30c5")
                    put("\u30c7", "\u30c6")
                    put("\u30c9", "\u30c8")
                    put("\u30c5", "\u30c4")
                    put("\u30f4", "\u30a6")
                    put("\u30cf", "\u30d0")
                    put("\u30d2", "\u30d3")
                    put("\u30d5", "\u30d6")
                    put("\u30d8", "\u30d9")
                    put("\u30db", "\u30dc")
                    put("\u30d0", "\u30d1")
                    put("\u30d3", "\u30d4")
                    put("\u30d6", "\u30d7")
                    put("\u30d9", "\u30da")
                    put("\u30dc", "\u30dd")
                    put("\u30d1", "\u30cf")
                    put("\u30d4", "\u30d2")
                    put("\u30d7", "\u30d5")
                    put("\u30da", "\u30d8")
                    put("\u30dd", "\u30db")
                    put("\u30e4", "\u30e3")
                    put("\u30e6", "\u30e5")
                    put("\u30e8", "\u30e7")
                    put("\u30e3", "\u30e4")
                    put("\u30e5", "\u30e6")
                    put("\u30e7", "\u30e8")
                    put("\u30ef", "\u30ee")
                    put("\u30ee", "\u30ef")
                }
            }

        /** Toggle cycle table for half-width KATAKANA  */
        private val JP_HALF_KATAKANA_CYCLE_TABLE = arrayOf(
            arrayOf(
                "\uff71",
                "\uff72",
                "\uff73",
                "\uff74",
                "\uff75",
                "\uff67",
                "\uff68",
                "\uff69",
                "\uff6a",
                "\uff6b"
            ),
            arrayOf("\uff76", "\uff77", "\uff78", "\uff79", "\uff7a"),
            arrayOf("\uff7b", "\uff7c", "\uff7d", "\uff7e", "\uff7f"),
            arrayOf("\uff80", "\uff81", "\uff82", "\uff83", "\uff84", "\uff6f"),
            arrayOf("\uff85", "\uff86", "\uff87", "\uff88", "\uff89"),
            arrayOf("\uff8a", "\uff8b", "\uff8c", "\uff8d", "\uff8e"),
            arrayOf("\uff8f", "\uff90", "\uff91", "\uff92", "\uff93"),
            arrayOf("\uff94", "\uff95", "\uff96", "\uff6c", "\uff6d", "\uff6e"),
            arrayOf("\uff97", "\uff98", "\uff99", "\uff9a", "\uff9b"),
            arrayOf("\uff9c", "\uff66", "\uff9d", "\uff70"),
            arrayOf("\uff64", "\uff61", "?", "!", "\uff65", " "),
        )

        /** Replace table for half-width KATAKANA  */
        private val JP_HALF_KATAKANA_REPLACE_TABLE: HashMap<String, String> =
            object : HashMap<String?, String?>() {
                init {
                    put("\uff71", "\uff67")
                    put("\uff72", "\uff68")
                    put("\uff73", "\uff69")
                    put("\uff74", "\uff6a")
                    put("\uff75", "\uff6b")
                    put("\uff67", "\uff71")
                    put("\uff68", "\uff72")
                    put("\uff69", "\uff73\uff9e")
                    put("\uff6a", "\uff74")
                    put("\uff6b", "\uff75")
                    put("\uff76", "\uff76\uff9e")
                    put("\uff77", "\uff77\uff9e")
                    put("\uff78", "\uff78\uff9e")
                    put("\uff79", "\uff79\uff9e")
                    put("\uff7a", "\uff7a\uff9e")
                    put("\uff76\uff9e", "\uff76")
                    put("\uff77\uff9e", "\uff77")
                    put("\uff78\uff9e", "\uff78")
                    put("\uff79\uff9e", "\uff79")
                    put("\uff7a\uff9e", "\uff7a")
                    put("\uff7b", "\uff7b\uff9e")
                    put("\uff7c", "\uff7c\uff9e")
                    put("\uff7d", "\uff7d\uff9e")
                    put("\uff7e", "\uff7e\uff9e")
                    put("\uff7f", "\uff7f\uff9e")
                    put("\uff7b\uff9e", "\uff7b")
                    put("\uff7c\uff9e", "\uff7c")
                    put("\uff7d\uff9e", "\uff7d")
                    put("\uff7e\uff9e", "\uff7e")
                    put("\uff7f\uff9e", "\uff7f")
                    put("\uff80", "\uff80\uff9e")
                    put("\uff81", "\uff81\uff9e")
                    put("\uff82", "\uff6f")
                    put("\uff83", "\uff83\uff9e")
                    put("\uff84", "\uff84\uff9e")
                    put("\uff80\uff9e", "\uff80")
                    put("\uff81\uff9e", "\uff81")
                    put("\uff6f", "\uff82\uff9e")
                    put("\uff83\uff9e", "\uff83")
                    put("\uff84\uff9e", "\uff84")
                    put("\uff82\uff9e", "\uff82")
                    put("\uff8a", "\uff8a\uff9e")
                    put("\uff8b", "\uff8b\uff9e")
                    put("\uff8c", "\uff8c\uff9e")
                    put("\uff8d", "\uff8d\uff9e")
                    put("\uff8e", "\uff8e\uff9e")
                    put("\uff8a\uff9e", "\uff8a\uff9f")
                    put("\uff8b\uff9e", "\uff8b\uff9f")
                    put("\uff8c\uff9e", "\uff8c\uff9f")
                    put("\uff8d\uff9e", "\uff8d\uff9f")
                    put("\uff8e\uff9e", "\uff8e\uff9f")
                    put("\uff8a\uff9f", "\uff8a")
                    put("\uff8b\uff9f", "\uff8b")
                    put("\uff8c\uff9f", "\uff8c")
                    put("\uff8d\uff9f", "\uff8d")
                    put("\uff8e\uff9f", "\uff8e")
                    put("\uff94", "\uff6c")
                    put("\uff95", "\uff6d")
                    put("\uff96", "\uff6e")
                    put("\uff6c", "\uff94")
                    put("\uff6d", "\uff95")
                    put("\uff6e", "\uff96")
                    put("\uff9c", "\uff9c")
                    put("\uff73\uff9e", "\uff73")
                }
            }

        /** Toggle cycle table for full-width alphabet  */
        private val JP_FULL_ALPHABET_CYCLE_TABLE = arrayOf(
            arrayOf("\uff0e", "\uff20", "\uff0d", "\uff3f", "\uff0f", "\uff1a", "\uff5e", "\uff11"),
            arrayOf("\uff41", "\uff42", "\uff43", "\uff21", "\uff22", "\uff23", "\uff12"),
            arrayOf("\uff44", "\uff45", "\uff46", "\uff24", "\uff25", "\uff26", "\uff13"),
            arrayOf("\uff47", "\uff48", "\uff49", "\uff27", "\uff28", "\uff29", "\uff14"),
            arrayOf("\uff4a", "\uff4b", "\uff4c", "\uff2a", "\uff2b", "\uff2c", "\uff15"),
            arrayOf("\uff4d", "\uff4e", "\uff4f", "\uff2d", "\uff2e", "\uff2f", "\uff16"),
            arrayOf(
                "\uff50",
                "\uff51",
                "\uff52",
                "\uff53",
                "\uff30",
                "\uff31",
                "\uff32",
                "\uff33",
                "\uff17"
            ),
            arrayOf("\uff54", "\uff55", "\uff56", "\uff34", "\uff35", "\uff36", "\uff18"),
            arrayOf(
                "\uff57",
                "\uff58",
                "\uff59",
                "\uff5a",
                "\uff37",
                "\uff38",
                "\uff39",
                "\uff3a",
                "\uff19"
            ),
            arrayOf("\uff0d", "\uff10"),
            arrayOf("\uff0c", "\uff0e", "\uff1f", "\uff01", "\u30fb", "\u3000")
        )

        /** Replace table for full-width alphabet  */
        private val JP_FULL_ALPHABET_REPLACE_TABLE: HashMap<String, String> =
            object : HashMap<String?, String?>() {
                init {
                    put("\uff21", "\uff41")
                    put("\uff22", "\uff42")
                    put("\uff23", "\uff43")
                    put("\uff24", "\uff44")
                    put("\uff25", "\uff45")
                    put("\uff41", "\uff21")
                    put("\uff42", "\uff22")
                    put("\uff43", "\uff23")
                    put("\uff44", "\uff24")
                    put("\uff45", "\uff25")
                    put("\uff26", "\uff46")
                    put("\uff27", "\uff47")
                    put("\uff28", "\uff48")
                    put("\uff29", "\uff49")
                    put("\uff2a", "\uff4a")
                    put("\uff46", "\uff26")
                    put("\uff47", "\uff27")
                    put("\uff48", "\uff28")
                    put("\uff49", "\uff29")
                    put("\uff4a", "\uff2a")
                    put("\uff2b", "\uff4b")
                    put("\uff2c", "\uff4c")
                    put("\uff2d", "\uff4d")
                    put("\uff2e", "\uff4e")
                    put("\uff2f", "\uff4f")
                    put("\uff4b", "\uff2b")
                    put("\uff4c", "\uff2c")
                    put("\uff4d", "\uff2d")
                    put("\uff4e", "\uff2e")
                    put("\uff4f", "\uff2f")
                    put("\uff30", "\uff50")
                    put("\uff31", "\uff51")
                    put("\uff32", "\uff52")
                    put("\uff33", "\uff53")
                    put("\uff34", "\uff54")
                    put("\uff50", "\uff30")
                    put("\uff51", "\uff31")
                    put("\uff52", "\uff32")
                    put("\uff53", "\uff33")
                    put("\uff54", "\uff34")
                    put("\uff35", "\uff55")
                    put("\uff36", "\uff56")
                    put("\uff37", "\uff57")
                    put("\uff38", "\uff58")
                    put("\uff39", "\uff59")
                    put("\uff55", "\uff35")
                    put("\uff56", "\uff36")
                    put("\uff57", "\uff37")
                    put("\uff58", "\uff38")
                    put("\uff59", "\uff39")
                    put("\uff3a", "\uff5a")
                    put("\uff5a", "\uff3a")
                }
            }

        /** Toggle cycle table for half-width alphabet  */
        private val JP_HALF_ALPHABET_CYCLE_TABLE = arrayOf(
            arrayOf(".", "@", "-", "_", "/", ":", "~", "1"),
            arrayOf("a", "b", "c", "A", "B", "C", "2"),
            arrayOf("d", "e", "f", "D", "E", "F", "3"),
            arrayOf("g", "h", "i", "G", "H", "I", "4"),
            arrayOf("j", "k", "l", "J", "K", "L", "5"),
            arrayOf("m", "n", "o", "M", "N", "O", "6"),
            arrayOf("p", "q", "r", "s", "P", "Q", "R", "S", "7"),
            arrayOf("t", "u", "v", "T", "U", "V", "8"),
            arrayOf("w", "x", "y", "z", "W", "X", "Y", "Z", "9"),
            arrayOf("-", "0"),
            arrayOf(",", ".", "?", "!", ";", " ")
        )

        /** Replace table for half-width alphabet  */
        private val JP_HALF_ALPHABET_REPLACE_TABLE: HashMap<String, String> =
            object : HashMap<String?, String?>() {
                init {
                    put("A", "a")
                    put("B", "b")
                    put("C", "c")
                    put("D", "d")
                    put("E", "e")
                    put("a", "A")
                    put("b", "B")
                    put("c", "C")
                    put("d", "D")
                    put("e", "E")
                    put("F", "f")
                    put("G", "g")
                    put("H", "h")
                    put("I", "i")
                    put("J", "j")
                    put("f", "F")
                    put("g", "G")
                    put("h", "H")
                    put("i", "I")
                    put("j", "J")
                    put("K", "k")
                    put("L", "l")
                    put("M", "m")
                    put("N", "n")
                    put("O", "o")
                    put("k", "K")
                    put("l", "L")
                    put("m", "M")
                    put("n", "N")
                    put("o", "O")
                    put("P", "p")
                    put("Q", "q")
                    put("R", "r")
                    put("S", "s")
                    put("T", "t")
                    put("p", "P")
                    put("q", "Q")
                    put("r", "R")
                    put("s", "S")
                    put("t", "T")
                    put("U", "u")
                    put("V", "v")
                    put("W", "w")
                    put("X", "x")
                    put("Y", "y")
                    put("u", "U")
                    put("v", "V")
                    put("w", "W")
                    put("x", "X")
                    put("y", "Y")
                    put("Z", "z")
                    put("z", "Z")
                }
            }

        /** Character table for full-width number  */
        private val INSTANT_CHAR_CODE_FULL_NUMBER =
            "\uff11\uff12\uff13\uff14\uff15\uff16\uff17\uff18\uff19\uff10\uff03\uff0a".toCharArray()

        /** Character table for half-width number  */
        private val INSTANT_CHAR_CODE_HALF_NUMBER = "1234567890#*".toCharArray()

        /** The constant for mFixedKeyMode. It means that input mode is not fixed.  */
        private const val INVALID_KEYMODE = -1

        /** KeyIndex of "Moji" key on 12 keyboard (depends on the definition of keyboards)  */
        private const val KEY_INDEX_CHANGE_MODE_12KEY = 15

        /** KeyIndex of "Moji" key on QWERTY keyboard (depends on the definition of keyboards)  */
        private const val KEY_INDEX_CHANGE_MODE_QWERTY = 29
    }
}
