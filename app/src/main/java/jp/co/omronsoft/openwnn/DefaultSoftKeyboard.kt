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

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Vibrator
import android.preference.PreferenceManager
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import ee.oyatl.ime.fusion.R

/**
 * The default software keyboard class.
 *
 * @author Copyright (C) 2009-2011 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
open class DefaultSoftKeyboard
/**
 * Constructor
 */
    : InputViewManager, KeyboardView.OnKeyboardActionListener {
    /** OpenWnn instance which hold this software keyboard */
    protected var mWnn: OpenWnn? = null

    /** Current keyboard view  */
    protected var mKeyboardView: KeyboardView? = null

    /** View objects (main side)  */
    protected var mMainView: BaseInputView? = null

    /** View objects (sub side)  */
    protected var mSubView: ViewGroup? = null

    /** Current keyboard definition  */
    protected var mCurrentKeyboard: Keyboard? = null

    /** Caps lock state  */
    protected var mCapsLock: Boolean = false

    /** Input restraint  */
    protected var mDisableKeyInput: Boolean = true

    /**
     * Keyboard surfaces
     * <br></br>
     * Keyboard[language][portrait/landscape][keyboard type][shift off/on][key-mode]
     */
    protected var mKeyboard: Array<Array<Array<Array<Array<Array<Keyboard?>>>>>> = emptyArray()

    /* languages */
    /** Current language  */
    protected var mCurrentLanguage: Int = 0
    /* portrait/landscape */
    /** State of the display  */
    protected var mDisplayMode: Int = 0
    /* keyboard type */
    /**
     * Get current keyboard type.
     *
     * @return Current keyboard type
     */
    /** Current keyboard type  */
    var keyboardType: Int = 0
        protected set

    /** State of the shift key  */
    protected var mShiftOn: Int = 0
    /* key-modes */
    /**
     * Get current key mode.
     *
     * @return Current key mode
     */
    /** Current key-mode  */
    var keyMode: Int = 0
        protected set

    /** Whether the H/W keyboard is hidden.  */
    protected var mHardKeyboardHidden: Boolean = true

    /** Whether the H/W 12key keyboard.  */
    protected var mEnableHardware12Keyboard: Boolean = false

    /** Symbol keyboard  */
    protected var mSymbolKeyboard: Keyboard? = null

    /** Symbol keyboard state  */
    protected var mIsSymbolKeyboard: Boolean = false

    /**
     * Status of the composing text
     * <br></br>
     * `true` if there is no composing text.
     */
    protected var mNoInput: Boolean = true

    /** Vibratior for key click vibration  */
    protected var mVibrator: Vibrator? = null

    /** MediaPlayer for key click sound  */
    protected var mSound: MediaPlayer? = null

    /** Key toggle cycle table currently using  */
    protected var mCurrentCycleTable: Array<String> = arrayOf()

    /** Event listener for symbol keyboard  */
    private val mSymbolOnKeyboardAction: KeyboardView.OnKeyboardActionListener =
        object : KeyboardView.OnKeyboardActionListener {
            override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
                when (primaryCode) {
                    KEYCODE_4KEY_MODE -> mWnn!!.onEvent(
                        OpenWnnEvent(
                            OpenWnnEvent.INPUT_KEY,
                            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)
                        )
                    )

                    KEYCODE_4KEY_UP -> mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.CANDIDATE_VIEW_SCROLL_UP))
                    KEYCODE_4KEY_DOWN -> mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.CANDIDATE_VIEW_SCROLL_DOWN))
                    KEYCODE_4KEY_CLEAR -> {
                        val connection = mWnn!!.currentInputConnection
                        connection?.sendKeyEvent(
                            KeyEvent(
                                KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_DEL
                            )
                        )
                        return
                    }

                    else -> {}
                }
            }

            override fun onPress(primaryCode: Int) {
                playSoundAndVibration()
            }

            override fun onText(text: CharSequence?) {}
            override fun swipeLeft() {}
            override fun swipeRight() {}
            override fun swipeUp() {}
            override fun swipeDown() {}
            override fun onRelease(primaryCode: Int) {}
            override fun onLongPress(key: Keyboard.Key): Boolean {
                when (key.codes!![0]) {
                    KEYCODE_4KEY_UP -> {
                        mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.CANDIDATE_VIEW_SCROLL_FULL_UP))
                        return true
                    }

                    KEYCODE_4KEY_DOWN -> {
                        mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.CANDIDATE_VIEW_SCROLL_FULL_DOWN))
                        return true
                    }

                    else -> {}
                }
                return false
            }
        }

    /**
     * Create keyboard views
     *
     * @param parent   OpenWnn using the keyboards.
     */
    protected open fun createKeyboards(parent: OpenWnn) {
        /*
         *  Keyboard[# of Languages][portrait/landscape][# of keyboard type]
         *          [shift off/on][max # of key-modes][non-input/input]
         */
        mKeyboard = Array(3) { Array(2) { Array(4) { Array(2) { Array(7) { arrayOfNulls(2) } } } } }
    }

    /**
     * Get the keyboard changed the specified shift state.
     *
     * @param shift     Shift state
     * @return          Keyboard view
     */
    protected fun getShiftChangeKeyboard(shift: Int): Keyboard? {
        try {
            val kbd = mKeyboard[mCurrentLanguage][mDisplayMode][keyboardType][shift][keyMode]

            if (!mNoInput && kbd[1] != null) {
                return kbd[1]
            }
            return kbd[0]
        } catch (ex: Exception) {
            return null
        }
    }

    /**
     * Get the keyboard changed the specified input mode.
     *
     * @param mode      Input mode
     * @return          Keyboard view
     */
    protected fun getModeChangeKeyboard(mode: Int): Keyboard? {
        try {
            val kbd = mKeyboard[mCurrentLanguage][mDisplayMode][keyboardType][mShiftOn][mode]

            if (!mNoInput && kbd[1] != null) {
                return kbd[1]
            }
            return kbd[0]
        } catch (ex: Exception) {
            return null
        }
    }

    /**
     * Get the keyboard changed the specified keyboard type
     *
     * @param type      Keyboard type
     * @return          Keyboard view
     */
    protected fun getTypeChangeKeyboard(type: Int): Keyboard? {
        try {
            val kbd = mKeyboard[mCurrentLanguage][mDisplayMode][type][mShiftOn][keyMode]

            if (!mNoInput && kbd[1] != null) {
                return kbd[1]
            }
            return kbd[0]
        } catch (ex: Exception) {
            return null
        }
    }

    /**
     * Get the keyboard when some characters are input or no character is input.
     *
     * @param inputed   `true` if some characters are inputed; `false` if no character is inputed.
     * @return          Keyboard view
     */
    protected fun getKeyboardInputed(inputed: Boolean): Keyboard? {
        try {
            val kbd = mKeyboard[mCurrentLanguage][mDisplayMode][keyboardType][mShiftOn][keyMode]

            if (inputed && kbd[1] != null) {
                return kbd[1]
            }
            return kbd[0]
        } catch (ex: Exception) {
            return null
        }
    }

    /**
     * Change the circulative key-mode.
     */
    protected fun toggleKeyMode() {
        /* unlock shift */
        mShiftOn = KEYBOARD_SHIFT_OFF

        /* search next defined key-mode */
        val keyboardList = mKeyboard[mCurrentLanguage][mDisplayMode][keyboardType][mShiftOn]
        do {
            if (++keyMode >= keyboardList.size) {
                keyMode = 0
            }
        } while (keyboardList[keyMode][0] == null)
        val kbd = if (!mNoInput && keyboardList[keyMode][1] != null) {
            keyboardList[keyMode][1]
        } else {
            keyboardList[keyMode][0]
        }
        changeKeyboard(kbd)

        mWnn!!.onEvent(
            OpenWnnEvent(
                OpenWnnEvent.CHANGE_MODE,
                OpenWnnEvent.Mode.DEFAULT
            )
        )
    }

    /**
     * Toggle change the shift lock state.
     */
    protected fun toggleShiftLock() {
        if (mShiftOn == 0) {
            /* turn shift on */
            val newKeyboard = getShiftChangeKeyboard(KEYBOARD_SHIFT_ON)
            if (newKeyboard != null) {
                mShiftOn = 1
                changeKeyboard(newKeyboard)
            }
            mCapsLock = true
        } else {
            /* turn shift off */
            val newKeyboard = getShiftChangeKeyboard(KEYBOARD_SHIFT_OFF)
            if (newKeyboard != null) {
                mShiftOn = 0
                changeKeyboard(newKeyboard)
            }
            mCapsLock = false
        }
    }

    /**
     * Handling Alt key event.
     */
    protected fun processAltKey() {
        /* invalid if it is not qwerty mode */
        if (keyboardType != KEYBOARD_QWERTY) {
            return
        }

        var mode = -1
        val keymode = keyMode
        when (mCurrentLanguage) {
            LANG_EN -> if (keymode == KEYMODE_EN_ALPHABET) {
                mode = KEYMODE_EN_NUMBER
            } else if (keymode == KEYMODE_EN_NUMBER) {
                mode = KEYMODE_EN_ALPHABET
            }

            LANG_JA -> if (keymode == KEYMODE_JA_HALF_ALPHABET) {
                mode = KEYMODE_JA_HALF_NUMBER
            } else if (keymode == KEYMODE_JA_HALF_NUMBER) {
                mode = KEYMODE_JA_HALF_ALPHABET
            } else if (keymode == KEYMODE_JA_FULL_ALPHABET) {
                mode = KEYMODE_JA_FULL_NUMBER
            } else if (keymode == KEYMODE_JA_FULL_NUMBER) {
                mode = KEYMODE_JA_FULL_ALPHABET
            }

            else -> {}
        }

        if (mode >= 0) {
            val kbd = getModeChangeKeyboard(mode)
            if (kbd != null) {
                keyMode = mode
                changeKeyboard(kbd)
            }
        }
    }

    /**
     * Change the keyboard type.
     *
     * @param type  Type of the keyboard
     * @see jp.co.omronsoft.openwnn.KEYBOARD_QWERTY
     *
     * @see jp.co.omronsoft.openwnn.KEYBOARD_12KEY
     */
    open fun changeKeyboardType(type: Int) {
        /* ignore invalid parameter */
        if (type != KEYBOARD_QWERTY && type != KEYBOARD_12KEY) {
            return
        }


        /* change keyboard view */
        val kbd = getTypeChangeKeyboard(type)
        if (kbd != null) {
            keyboardType = type
            changeKeyboard(kbd)
        }

        /* notice that the keyboard is changed */
        mWnn!!.onEvent(
            OpenWnnEvent(
                OpenWnnEvent.CHANGE_MODE,
                OpenWnnEvent.Mode.DEFAULT
            )
        )
    }

    /**
     * Change the keyboard.
     *
     * @param keyboard  The new keyboard
     * @return          `true` if the keyboard is changed; `false` if not changed.
     */
    protected open fun changeKeyboard(keyboard: Keyboard?): Boolean {
        if (keyboard == null) {
            return false
        }
        if (mCurrentKeyboard !== keyboard) {
            mKeyboardView?.keyboard = keyboard
            mKeyboardView!!.setShifted(if ((mShiftOn == 0)) false else true)
            mCurrentKeyboard = keyboard
            return true
        } else {
            mKeyboardView!!.setShifted(if ((mShiftOn == 0)) false else true)
            return false
        }
    }

    /** @see jp.co.omronsoft.openwnn.InputViewManager.initView
     */
    override fun initView(parent: OpenWnn, width: Int, height: Int): View? {
        mWnn = parent
        mDisplayMode =
            if ((parent.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE))
                LANDSCAPE
            else
                PORTRAIT

        /*
         * create keyboards & the view.
         * To re-display the input view when the display mode is changed portrait <-> landscape,
         * create keyboards every time.
         */
        createKeyboards(parent)

        /* create symbol keyboard */
        mSymbolKeyboard = Keyboard(parent, R.xml.keyboard_4key)

        val pref = PreferenceManager.getDefaultSharedPreferences(parent)
        val skin = pref.getString(
            "keyboard_skin",
            mWnn!!.resources.getString(R.string.keyboard_skin_id_default)
        )!!
        val id = parent.resources.getIdentifier(skin, "layout", "ee.oyatl.ime.fusion")

        mKeyboardView = mWnn!!.layoutInflater.inflate(id, null) as KeyboardView
        mKeyboardView?.onKeyboardActionListener = this
        mCurrentKeyboard = null

        mMainView =
            parent.layoutInflater.inflate(R.layout.keyboard_default_main, null) as BaseInputView
        mSubView = parent.layoutInflater.inflate(R.layout.keyboard_default_sub, null) as ViewGroup

        if (!mHardKeyboardHidden) {
            if (!mEnableHardware12Keyboard) {
                mMainView!!.addView(mSubView)
            }
        } else if (mKeyboardView != null) {
            mMainView!!.addView(mKeyboardView)
        }

        return mMainView
    }

    /**
     * Update the SHFIT/ALT keys indicator.
     *
     * @param mode  The state of SHIFT/ALT keys.
     */
    fun updateIndicator(mode: Int) {
        val res = mWnn!!.resources
        val text1 = mSubView!!.findViewById<View>(R.id.shift) as TextView
        val text2 = mSubView!!.findViewById<View>(R.id.alt) as TextView

        when (mode) {
            HARD_KEYMODE_SHIFT_OFF_ALT_OFF -> {
                text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_off))
                text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_off))
                text1.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default))
                text2.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default))
            }

            HARD_KEYMODE_SHIFT_ON_ALT_OFF -> {
                text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_on))
                text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_off))
                text1.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default))
                text2.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default))
            }

            HARD_KEYMODE_SHIFT_LOCK_ALT_OFF -> {
                text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_lock))
                text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_off))
                text1.setBackgroundColor(res.getColor(R.color.indicator_background_lock_caps))
                text2.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default))
            }

            HARD_KEYMODE_SHIFT_OFF_ALT_ON -> {
                text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_off))
                text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_on))
                text1.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default))
                text2.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default))
            }

            HARD_KEYMODE_SHIFT_OFF_ALT_LOCK -> {
                text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_off))
                text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_lock))
                text1.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default))
                text2.setBackgroundColor(res.getColor(R.color.indicator_background_lock_alt))
            }

            HARD_KEYMODE_SHIFT_ON_ALT_ON -> {
                text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_on))
                text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_on))
                text1.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default))
                text2.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default))
            }

            HARD_KEYMODE_SHIFT_ON_ALT_LOCK -> {
                text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_on))
                text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_lock))
                text1.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default))
                text2.setBackgroundColor(res.getColor(R.color.indicator_background_lock_alt))
            }

            HARD_KEYMODE_SHIFT_LOCK_ALT_ON -> {
                text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_lock))
                text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_on))
                text1.setBackgroundColor(res.getColor(R.color.indicator_background_lock_caps))
                text2.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default))
            }

            HARD_KEYMODE_SHIFT_LOCK_ALT_LOCK -> {
                text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_lock))
                text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_lock))
                text1.setBackgroundColor(res.getColor(R.color.indicator_background_lock_caps))
                text2.setBackgroundColor(res.getColor(R.color.indicator_background_lock_alt))
            }

            else -> {
                text1.setTextColor(res.getColor(R.color.indicator_textcolor_caps_off))
                text2.setTextColor(res.getColor(R.color.indicator_textcolor_alt_off))
                text1.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default))
                text2.setBackgroundColor(res.getColor(R.color.indicator_textbackground_default))
            }
        }
        return
    }

    override val currentView: View?
        /** @see jp.co.omronsoft.openwnn.InputViewManager.getCurrentView
         */
        get() = mMainView

    /** @see jp.co.omronsoft.openwnn.InputViewManager.onUpdateState
     */
    override fun onUpdateState(parent: OpenWnn) {
        try {
            if (parent.mComposingText!!.size(1) == 0) {
                if (!mNoInput) {
                    /* when the mode changed to "no input" */
                    mNoInput = true
                    val newKeyboard = getKeyboardInputed(false)
                    if (mCurrentKeyboard !== newKeyboard) {
                        changeKeyboard(newKeyboard)
                    }
                }
            } else {
                if (mNoInput) {
                    /* when the mode changed to "input some characters" */
                    mNoInput = false
                    val newKeyboard = getKeyboardInputed(true)
                    if (mCurrentKeyboard !== newKeyboard) {
                        changeKeyboard(newKeyboard)
                    }
                }
            }
        } catch (ex: Exception) {
        }
    }

    /** @see jp.co.omronsoft.openwnn.InputViewManager.setPreferences
     */
    override fun setPreferences(pref: SharedPreferences, editor: EditorInfo) {
        /* vibrator */

        try {
            mVibrator = if (pref.getBoolean("key_vibration", false)) {
                mWnn!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            } else {
                null
            }
        } catch (ex: Exception) {
            Log.d("OpenWnn", "NO VIBRATOR")
        }

        /* sound */
        try {
            mSound = if (pref.getBoolean("key_sound", false)) {
                MediaPlayer.create(mWnn, R.raw.type)
            } else {
                null
            }
        } catch (ex: Exception) {
            Log.d("OpenWnn", "NO SOUND")
        }

        /* pop-up preview */
        if (OpenWnn.isXLarge) {
            mKeyboardView?.isPreviewEnabled = false
        } else {
            mKeyboardView?.isPreviewEnabled = pref.getBoolean("popup_preview", true)
            mKeyboardView?.clearWindowInfo()
        }
    }

    /** @see jp.co.omronsoft.openwnn.InputViewManager.closing
     */
    override fun closing() {
        if (mKeyboardView != null) {
            mKeyboardView!!.closing()
        }
        mDisableKeyInput = true
    }

    /** @see jp.co.omronsoft.openwnn.InputViewManager.showInputView
     */
    override fun showInputView() {
        if (mKeyboardView != null) {
            mKeyboardView!!.visibility = View.VISIBLE
        }
    }

    /** @see jp.co.omronsoft.openwnn.InputViewManager.hideInputView
     */
    override fun hideInputView() {
        mKeyboardView!!.visibility = View.GONE
    }

    /***********************************************************************
     * onKeyboardActionListener
     */
    /** @see jp.co.omronsoft.openwnn.KeyboardView.OnKeyboardActionListener.onKey
     */
    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {}

    /** @see jp.co.omronsoft.openwnn.KeyboardView.OnKeyboardActionListener.swipeRight
     */
    override fun swipeRight() {}

    /** @see jp.co.omronsoft.openwnn.KeyboardView.OnKeyboardActionListener.swipeLeft
     */
    override fun swipeLeft() {}

    /** @see jp.co.omronsoft.openwnn.KeyboardView.OnKeyboardActionListener.swipeDown
     */
    override fun swipeDown() {}

    /** @see jp.co.omronsoft.openwnn.KeyboardView.OnKeyboardActionListener.swipeUp
     */
    override fun swipeUp() {}

    /** @see jp.co.omronsoft.openwnn.KeyboardView.OnKeyboardActionListener.onRelease
     */
    override fun onRelease(x: Int) {}

    /** @see jp.co.omronsoft.openwnn.KeyboardView.OnKeyboardActionListener.onPress
     */
    override fun onPress(x: Int) {
        playSoundAndVibration()
    }

    /** @see android.jp.co.omronsoft.openwnn.KeyboardView.OnKeyboardActionListener.onLongPress
     */
    override fun onLongPress(key: Keyboard.Key): Boolean {
        return false
    }

    /**
     * Play sound & vibration.
     */
    private fun playSoundAndVibration() {
        /* key click sound & vibration */
        if (mVibrator != null) {
            try {
                mVibrator!!.vibrate(5)
            } catch (ex: Exception) {
            }
        }
        if (mSound != null) {
            try {
                mSound!!.seekTo(0)
                mSound!!.start()
            } catch (ex: Exception) {
            }
        }
    }

    /** @see jp.co.omronsoft.openwnn.KeyboardView.OnKeyboardActionListener.onText
     */
    override fun onText(text: CharSequence?) {}

    /**
     * Set the H/W keyboard's state.
     *
     * @param hidden `true` if hidden.
     */
    open fun setHardKeyboardHidden(hidden: Boolean) {
        mHardKeyboardHidden = hidden
    }

    /**
     * Set the H/W keyboard's type.
     *
     * @param type12Key `true` if 12Key.
     */
    open fun setHardware12Keyboard(type12Key: Boolean) {
        mEnableHardware12Keyboard = type12Key
    }

    val keyboardView: View?
        /**
         * Get current keyboard view.
         */
        get() = mKeyboardView

    /**
     * Reset the current keyboard
     */
    fun resetCurrentKeyboard() {
        closing()
        val keyboard = mCurrentKeyboard
        mCurrentKeyboard = null
        changeKeyboard(keyboard)
    }

    /**
     * Set the normal keyboard.
     */
    fun setNormalKeyboard() {
        if (mCurrentKeyboard == null) {
            return
        }
        mKeyboardView?.keyboard = mCurrentKeyboard
        mKeyboardView?.onKeyboardActionListener = this
        mIsSymbolKeyboard = false
    }

    /**
     * Set the symbol keyboard.
     */
    fun setSymbolKeyboard() {
        mKeyboardView?.keyboard = mSymbolKeyboard
        mKeyboardView?.onKeyboardActionListener = mSymbolOnKeyboardAction
        mIsSymbolKeyboard = true
    }

    companion object {
        /*
     *----------------------------------------------------------------------
     * key codes for a software keyboard
     *----------------------------------------------------------------------
     */
        /** Change the keyboard language  */
        const val KEYCODE_CHANGE_LANG: Int = -500

        /* for Japanese 12-key keyboard */
        /** Japanese 12-key keyboard [1]  */
        const val KEYCODE_JP12_1: Int = -201

        /** Japanese 12-key keyboard [2]  */
        const val KEYCODE_JP12_2: Int = -202

        /** Japanese 12-key keyboard [3]  */
        const val KEYCODE_JP12_3: Int = -203

        /** Japanese 12-key keyboard [4]  */
        const val KEYCODE_JP12_4: Int = -204

        /** Japanese 12-key keyboard [5]  */
        const val KEYCODE_JP12_5: Int = -205

        /** Japanese 12-key keyboard [6]  */
        const val KEYCODE_JP12_6: Int = -206

        /** Japanese 12-key keyboard [7]  */
        const val KEYCODE_JP12_7: Int = -207

        /** Japanese 12-key keyboard [8]  */
        const val KEYCODE_JP12_8: Int = -208

        /** Japanese 12-key keyboard [9]  */
        const val KEYCODE_JP12_9: Int = -209

        /** Japanese 12-key keyboard [0]  */
        const val KEYCODE_JP12_0: Int = -210

        /** Japanese 12-key keyboard [#]  */
        const val KEYCODE_JP12_SHARP: Int = -211

        /** Japanese 12-key keyboard [*]  */
        const val KEYCODE_JP12_ASTER: Int = -213

        /** Japanese 12-key keyboard [DEL]  */
        const val KEYCODE_JP12_BACKSPACE: Int = -214

        /** Japanese 12-key keyboard [SPACE]  */
        const val KEYCODE_JP12_SPACE: Int = -215

        /** Japanese 12-key keyboard [ENTER]  */
        const val KEYCODE_JP12_ENTER: Int = -216

        /** Japanese 12-key keyboard [RIGHT ARROW]  */
        const val KEYCODE_JP12_RIGHT: Int = -217

        /** Japanese 12-key keyboard [LEFT ARROW]  */
        const val KEYCODE_JP12_LEFT: Int = -218

        /** Japanese 12-key keyboard [REVERSE TOGGLE]  */
        const val KEYCODE_JP12_REVERSE: Int = -219

        /** Japanese 12-key keyboard [CLOSE]  */
        const val KEYCODE_JP12_CLOSE: Int = -220

        /** Japanese 12-key keyboard [KEYBOARD TYPE CHANGE]  */
        const val KEYCODE_JP12_KBD: Int = -221

        /** Japanese 12-key keyboard [EMOJI]  */
        const val KEYCODE_JP12_EMOJI: Int = -222

        /** Japanese 12-key keyboard [FULL-WIDTH HIRAGANA MODE]  */
        const val KEYCODE_JP12_ZEN_HIRA: Int = -223

        /** Japanese 12-key keyboard [FULL-WIDTH NUMBER MODE]  */
        const val KEYCODE_JP12_ZEN_NUM: Int = -224

        /** Japanese 12-key keyboard [FULL-WIDTH ALPHABET MODE]  */
        const val KEYCODE_JP12_ZEN_ALPHA: Int = -225

        /** Japanese 12-key keyboard [FULL-WIDTH KATAKANA MODE]  */
        const val KEYCODE_JP12_ZEN_KATA: Int = -226

        /** Japanese 12-key keyboard [HALF-WIDTH KATAKANA MODE]  */
        const val KEYCODE_JP12_HAN_KATA: Int = -227

        /** Japanese 12-key keyboard [HALF-WIDTH NUMBER MODE]  */
        const val KEYCODE_JP12_HAN_NUM: Int = -228

        /** Japanese 12-key keyboard [HALF-WIDTH ALPHABET MODE]  */
        const val KEYCODE_JP12_HAN_ALPHA: Int = -229

        /** Japanese 12-key keyboard [MODE TOOGLE CHANGE]  */
        const val KEYCODE_JP12_TOGGLE_MODE: Int = -230

        /** Key code for symbol keyboard alt key  */
        const val KEYCODE_4KEY_MODE: Int = -300

        /** Key code for symbol keyboard up key  */
        const val KEYCODE_4KEY_UP: Int = -301

        /** Key code for symbol keyboard down key  */
        const val KEYCODE_4KEY_DOWN: Int = -302

        /** Key code for symbol keyboard del key  */
        const val KEYCODE_4KEY_CLEAR: Int = -303

        /* for Qwerty keyboard */
        /** Qwerty keyboard [DEL]  */
        const val KEYCODE_QWERTY_BACKSPACE: Int = -100

        /** Qwerty keyboard [ENTER]  */
        const val KEYCODE_QWERTY_ENTER: Int = -101

        /** Qwerty keyboard [SHIFT]  */
        val KEYCODE_QWERTY_SHIFT: Int = Keyboard.KEYCODE_SHIFT

        /** Qwerty keyboard [ALT]  */
        const val KEYCODE_QWERTY_ALT: Int = -103

        /** Qwerty keyboard [KEYBOARD TYPE CHANGE]  */
        const val KEYCODE_QWERTY_KBD: Int = -104

        /** Qwerty keyboard [CLOSE]  */
        const val KEYCODE_QWERTY_CLOSE: Int = -105

        /** Japanese Qwerty keyboard [EMOJI]  */
        const val KEYCODE_QWERTY_EMOJI: Int = -106

        /** Japanese Qwerty keyboard [FULL-WIDTH HIRAGANA MODE]  */
        const val KEYCODE_QWERTY_ZEN_HIRA: Int = -107

        /** Japanese Qwerty keyboard [FULL-WIDTH NUMBER MODE]  */
        const val KEYCODE_QWERTY_ZEN_NUM: Int = -108

        /** Japanese Qwerty keyboard [FULL-WIDTH ALPHABET MODE]  */
        const val KEYCODE_QWERTY_ZEN_ALPHA: Int = -109

        /** Japanese Qwerty keyboard [FULL-WIDTH KATAKANA MODE]  */
        const val KEYCODE_QWERTY_ZEN_KATA: Int = -110

        /** Japanese Qwerty keyboard [HALF-WIDTH KATAKANA MODE]  */
        const val KEYCODE_QWERTY_HAN_KATA: Int = -111

        /** Qwerty keyboard [NUMBER MODE]  */
        const val KEYCODE_QWERTY_HAN_NUM: Int = -112

        /** Qwerty keyboard [ALPHABET MODE]  */
        const val KEYCODE_QWERTY_HAN_ALPHA: Int = -113

        /** Qwerty keyboard [MODE TOOGLE CHANGE]  */
        const val KEYCODE_QWERTY_TOGGLE_MODE: Int = -114

        /** Qwerty keyboard [PINYIN MODE]  */
        const val KEYCODE_QWERTY_PINYIN: Int = -115

        /** Language (English)  */
        const val LANG_EN: Int = 0

        /** Language (Japanese)  */
        const val LANG_JA: Int = 1

        /** Language (Chinese)  */
        const val LANG_CN: Int = 2

        /** Display mode (Portrait)  */
        const val PORTRAIT: Int = 0

        /** Display mode (Landscape)  */
        const val LANDSCAPE: Int = 1

        /** Keyboard (QWERTY keyboard)  */
        const val KEYBOARD_QWERTY: Int = 0

        /** Keyboard (12-keys keyboard)  */
        const val KEYBOARD_12KEY: Int = 1

        /** Shift key off  */
        const val KEYBOARD_SHIFT_OFF: Int = 0

        /** Shift key on  */
        const val KEYBOARD_SHIFT_ON: Int = 1

        /* key-modes for English */
        /** English key-mode (alphabet)  */
        const val KEYMODE_EN_ALPHABET: Int = 0

        /** English key-mode (number)  */
        const val KEYMODE_EN_NUMBER: Int = 1

        /** English key-mode (phone number)  */
        const val KEYMODE_EN_PHONE: Int = 2

        /* key-modes for Japanese */
        /** Japanese key-mode (Full-width Hiragana)  */
        const val KEYMODE_JA_FULL_HIRAGANA: Int = 0

        /** Japanese key-mode (Full-width alphabet)  */
        const val KEYMODE_JA_FULL_ALPHABET: Int = 1

        /** Japanese key-mode (Full-width number)  */
        const val KEYMODE_JA_FULL_NUMBER: Int = 2

        /** Japanese key-mode (Full-width Katakana)  */
        const val KEYMODE_JA_FULL_KATAKANA: Int = 3

        /** Japanese key-mode (Half-width alphabet)  */
        const val KEYMODE_JA_HALF_ALPHABET: Int = 4

        /** Japanese key-mode (Half-width number)  */
        const val KEYMODE_JA_HALF_NUMBER: Int = 5

        /** Japanese key-mode (Half-width Katakana)  */
        const val KEYMODE_JA_HALF_KATAKANA: Int = 6

        /** Japanese key-mode (Half-width phone number)  */
        const val KEYMODE_JA_HALF_PHONE: Int = 7

        /* key-modes for Chinese */
        /** Chinese key-mode (pinyin)  */
        const val KEYMODE_CN_PINYIN: Int = 0

        /** Chinese key-mode (Full-width number)  */
        const val KEYMODE_CN_FULL_NUMBER: Int = 1

        /** Chinese key-mode (alphabet)  */
        const val KEYMODE_CN_ALPHABET: Int = 2

        /** Chinese key-mode (phone)  */
        const val KEYMODE_CN_PHONE: Int = 3

        /** Chinese key-mode (Half-width number)  */
        const val KEYMODE_CN_HALF_NUMBER: Int = 4

        /* key-modes for HARD */
        /** HARD key-mode (SHIFT_OFF_ALT_OFF)  */
        const val HARD_KEYMODE_SHIFT_OFF_ALT_OFF: Int = 2

        /** HARD key-mode (SHIFT_ON_ALT_OFF)  */
        const val HARD_KEYMODE_SHIFT_ON_ALT_OFF: Int = 3

        /** HARD key-mode (SHIFT_OFF_ALT_ON)  */
        const val HARD_KEYMODE_SHIFT_OFF_ALT_ON: Int = 4

        /** HARD key-mode (SHIFT_ON_ALT_ON)  */
        const val HARD_KEYMODE_SHIFT_ON_ALT_ON: Int = 5

        /** HARD key-mode (SHIFT_LOCK_ALT_OFF)  */
        const val HARD_KEYMODE_SHIFT_LOCK_ALT_OFF: Int = 6

        /** HARD key-mode (SHIFT_LOCK_ALT_ON)  */
        const val HARD_KEYMODE_SHIFT_LOCK_ALT_ON: Int = 7

        /** HARD key-mode (SHIFT_LOCK_ALT_LOCK)  */
        const val HARD_KEYMODE_SHIFT_LOCK_ALT_LOCK: Int = 8

        /** HARD key-mode (SHIFT_OFF_ALT_LOCK)  */
        const val HARD_KEYMODE_SHIFT_OFF_ALT_LOCK: Int = 9

        /** HARD key-mode (SHIFT_ON_ALT_LOCK)  */
        const val HARD_KEYMODE_SHIFT_ON_ALT_LOCK: Int = 10
    }
}
