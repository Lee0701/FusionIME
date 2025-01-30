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
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import ee.oyatl.ime.fusion.R
import jp.co.omronsoft.openwnn.DefaultSoftKeyboard
import jp.co.omronsoft.openwnn.Keyboard
import jp.co.omronsoft.openwnn.OpenWnn
import jp.co.omronsoft.openwnn.OpenWnnEvent

/**
 * The default Software Keyboard class for English IME.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
class DefaultSoftKeyboardEN
/**
 * Default constructor
 */
    : DefaultSoftKeyboard() {
    /** Auto caps mode  */
    private var mAutoCaps = false

    /**
     * Dismiss the pop-up keyboard.
     * <br></br>
     * Nothing will be done if no pop-up keyboard is displaying.
     */
    fun dismissPopupKeyboard() {
        try {
            if (mKeyboardView != null) {
                mKeyboardView!!.handleBack()
            }
        } catch (ex: Exception) {
            /* ignore */
        }
    }

    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard.createKeyboards
     */
    override fun createKeyboards(parent: OpenWnn) {
        mKeyboard = Array(3) { Array(2) { Array(4) { Array(2) { Array(7) { arrayOfNulls(2) } } } } }
        /***********************************************************************
         * English
         */
        /* qwerty shift_off */
        var keyList =
            mKeyboard[DefaultSoftKeyboard.Companion.LANG_EN][DefaultSoftKeyboard.Companion.PORTRAIT][DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF]
        keyList[DefaultSoftKeyboard.Companion.KEYMODE_EN_ALPHABET][0] =
            Keyboard(parent, R.xml.default_en_qwerty)
        keyList[DefaultSoftKeyboard.Companion.KEYMODE_EN_NUMBER][0] =
            Keyboard(parent, R.xml.default_en_symbols)
        keyList[DefaultSoftKeyboard.Companion.KEYMODE_EN_PHONE][0] =
            Keyboard(parent, R.xml.keyboard_12key_phone)


        /* qwerty shift_on */
        keyList =
            mKeyboard[DefaultSoftKeyboard.Companion.LANG_EN][DefaultSoftKeyboard.Companion.PORTRAIT][DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_ON]
        keyList[DefaultSoftKeyboard.Companion.KEYMODE_EN_ALPHABET][0] =
            mKeyboard[DefaultSoftKeyboard.Companion.LANG_EN][DefaultSoftKeyboard.Companion.PORTRAIT][DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY][DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF][DefaultSoftKeyboard.Companion.KEYMODE_EN_ALPHABET][0]
        keyList[DefaultSoftKeyboard.Companion.KEYMODE_EN_NUMBER][0] =
            Keyboard(parent, R.xml.default_en_symbols_shift)
        keyList[DefaultSoftKeyboard.Companion.KEYMODE_EN_PHONE][0] =
            Keyboard(parent, R.xml.keyboard_12key_phone)
    }

    /**
     * Get the shift key state from the editor.
     *
     * @param editor    The information of editor
     * @return        state ID of the shift key (0:off, 1:on)
     */
    private fun getShiftKeyState(editor: EditorInfo): Int {
        val connection = mWnn!!.currentInputConnection
        if (connection != null) {
            val caps = connection.getCursorCapsMode(editor.inputType)
            return if ((caps == 0)) 0 else 1
        } else {
            return 0
        }
    }

    /**
     * Switch the keymode
     *
     * @param keyMode        Keymode
     */
    private fun changeKeyMode(keyMode: Int) {
        val keyboard = super.getModeChangeKeyboard(keyMode)
        if (keyboard != null) {
            mCurrentKeyMode = keyMode
            super.changeKeyboard(keyboard)
        }
    }

    /***********************************************************************
     * from DefaultSoftKeyboard
     */
    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard.initView
     */
    override fun initView(parent: OpenWnn, width: Int, height: Int): View? {
        val view = super.initView(parent, width, height)


        /* default setting */
        mCurrentLanguage = DefaultSoftKeyboard.Companion.LANG_EN
        mCurrentKeyboardType = DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY
        mShiftOn = DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF
        mCurrentKeyMode = DefaultSoftKeyboard.Companion.KEYMODE_EN_ALPHABET

        val kbd =
            mKeyboard[mCurrentLanguage][mDisplayMode][mCurrentKeyboardType][mShiftOn][mCurrentKeyMode][0]
        if (kbd == null) {
            if (mDisplayMode == DefaultSoftKeyboard.Companion.LANDSCAPE) {
                return view
            }
            return null
        }
        mCurrentKeyboard = null
        changeKeyboard(kbd)
        return view
    }

    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard.setPreferences
     */
    override fun setPreferences(pref: SharedPreferences, editor: EditorInfo) {
        super.setPreferences(pref, editor)

        /* auto caps mode */
        mAutoCaps = pref.getBoolean("auto_caps", true)

        when (editor.inputType and EditorInfo.TYPE_MASK_CLASS) {
            EditorInfo.TYPE_CLASS_NUMBER, EditorInfo.TYPE_CLASS_DATETIME -> {
                mCurrentLanguage = DefaultSoftKeyboard.Companion.LANG_EN
                mCurrentKeyboardType = DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY
                mShiftOn = DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF
                mCurrentKeyMode = DefaultSoftKeyboard.Companion.KEYMODE_EN_NUMBER

                val kbdn =
                    mKeyboard[mCurrentLanguage][mDisplayMode][mCurrentKeyboardType][mShiftOn][mCurrentKeyMode][0]!!

                changeKeyboard(kbdn)
            }

            EditorInfo.TYPE_CLASS_PHONE -> {
                mCurrentLanguage = DefaultSoftKeyboard.Companion.LANG_EN
                mCurrentKeyboardType = DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY
                mShiftOn = DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF
                mCurrentKeyMode = DefaultSoftKeyboard.Companion.KEYMODE_EN_PHONE

                val kbdp =
                    mKeyboard[mCurrentLanguage][mDisplayMode][mCurrentKeyboardType][mShiftOn][mCurrentKeyMode][0]!!

                changeKeyboard(kbdp)
            }

            else -> {
                mCurrentLanguage = DefaultSoftKeyboard.Companion.LANG_EN
                mCurrentKeyboardType = DefaultSoftKeyboard.Companion.KEYBOARD_QWERTY
                mShiftOn = DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF
                mCurrentKeyMode = DefaultSoftKeyboard.Companion.KEYMODE_EN_ALPHABET

                val kbdq =
                    mKeyboard[mCurrentLanguage][mDisplayMode][mCurrentKeyboardType][mShiftOn][mCurrentKeyMode][0]!!

                changeKeyboard(kbdq)
            }
        }

        val shift = if ((mAutoCaps)) getShiftKeyState(mWnn!!.currentInputEditorInfo) else 0
        if (shift != mShiftOn) {
            val kbd = getShiftChangeKeyboard(shift)
            mShiftOn = shift
            changeKeyboard(kbd)
        }
    }

    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard.onKey
     */
    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        var primaryCode = primaryCode
        when (primaryCode) {
            DefaultSoftKeyboard.Companion.KEYCODE_QWERTY_HAN_ALPHA -> this.changeKeyMode(
                DefaultSoftKeyboard.Companion.KEYMODE_EN_ALPHABET
            )

            DefaultSoftKeyboard.Companion.KEYCODE_QWERTY_HAN_NUM -> this.changeKeyMode(
                DefaultSoftKeyboard.Companion.KEYMODE_EN_NUMBER
            )

            KEYCODE_PHONE -> this.changeKeyMode(DefaultSoftKeyboard.Companion.KEYMODE_EN_PHONE)
            DefaultSoftKeyboard.Companion.KEYCODE_QWERTY_EMOJI -> mWnn!!.onEvent(
                OpenWnnEvent(
                    OpenWnnEvent.Companion.LIST_SYMBOLS
                )
            )

            DefaultSoftKeyboard.Companion.KEYCODE_QWERTY_TOGGLE_MODE -> {
                when (mCurrentKeyMode) {
                    DefaultSoftKeyboard.Companion.KEYMODE_EN_ALPHABET -> if (TOGGLE_KEYBOARD[DefaultSoftKeyboard.Companion.KEYMODE_EN_NUMBER]) {
                        mCurrentKeyMode = DefaultSoftKeyboard.Companion.KEYMODE_EN_NUMBER
                    } else if (TOGGLE_KEYBOARD[DefaultSoftKeyboard.Companion.KEYMODE_EN_PHONE]) {
                        mCurrentKeyMode = DefaultSoftKeyboard.Companion.KEYMODE_EN_PHONE
                    }

                    DefaultSoftKeyboard.Companion.KEYMODE_EN_NUMBER -> if (TOGGLE_KEYBOARD[DefaultSoftKeyboard.Companion.KEYMODE_EN_PHONE]) {
                        mCurrentKeyMode = DefaultSoftKeyboard.Companion.KEYMODE_EN_PHONE
                    } else if (TOGGLE_KEYBOARD[DefaultSoftKeyboard.Companion.KEYMODE_EN_ALPHABET]) {
                        mCurrentKeyMode = DefaultSoftKeyboard.Companion.KEYMODE_EN_ALPHABET
                    }

                    DefaultSoftKeyboard.Companion.KEYMODE_EN_PHONE -> if (TOGGLE_KEYBOARD[DefaultSoftKeyboard.Companion.KEYMODE_EN_ALPHABET]) {
                        mCurrentKeyMode = DefaultSoftKeyboard.Companion.KEYMODE_EN_ALPHABET
                    } else if (TOGGLE_KEYBOARD[DefaultSoftKeyboard.Companion.KEYMODE_EN_NUMBER]) {
                        mCurrentKeyMode = DefaultSoftKeyboard.Companion.KEYMODE_EN_NUMBER
                    }
                }
                val kbdp =
                    mKeyboard[mCurrentLanguage][mDisplayMode][mCurrentKeyboardType][mShiftOn][mCurrentKeyMode][0]!!
                super.changeKeyboard(kbdp)
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

            DefaultSoftKeyboard.Companion.KEYCODE_JP12_LEFT -> mWnn!!.onEvent(
                OpenWnnEvent(
                    OpenWnnEvent.Companion.INPUT_SOFT_KEY,
                    KeyEvent(
                        KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DPAD_LEFT
                    )
                )
            )

            DefaultSoftKeyboard.Companion.KEYCODE_JP12_RIGHT -> {
                mWnn!!.onEvent(
                    OpenWnnEvent(
                        OpenWnnEvent.Companion.INPUT_SOFT_KEY,
                        KeyEvent(
                            KeyEvent.ACTION_DOWN,
                            KeyEvent.KEYCODE_DPAD_RIGHT
                        )
                    )
                )
                if (primaryCode >= 0) {
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
        if (!mCapsLock && primaryCode != DefaultSoftKeyboard.Companion.KEYCODE_QWERTY_SHIFT) {
            if (mCurrentKeyMode != DefaultSoftKeyboard.Companion.KEYMODE_EN_NUMBER) {
                val shift = if ((mAutoCaps)) getShiftKeyState(mWnn!!.currentInputEditorInfo) else 0
                if (shift != mShiftOn) {
                    val kbd = getShiftChangeKeyboard(shift)
                    mShiftOn = shift
                    changeKeyboard(kbd)
                }
            } else {
                mShiftOn = DefaultSoftKeyboard.Companion.KEYBOARD_SHIFT_OFF
                val kbd = getShiftChangeKeyboard(mShiftOn)
                changeKeyboard(kbd)
            }
        }
    }

    companion object {
        /** 12-key keyboard [PHONE MODE]  */
        const val KEYCODE_PHONE: Int = -116

        /**
         * Keyboards toggled by ALT key.
         * <br></br>
         * The normal keyboard(KEYMODE_EN_ALPHABET) and the number/symbol
         * keyboard(KEYMODE_EN_NUMBER) is active.  The phone number
         * keyboard(KEYMODE_EN_PHONE) is disabled.
         */
        private val TOGGLE_KEYBOARD = booleanArrayOf(true, true, false)
    }
}



