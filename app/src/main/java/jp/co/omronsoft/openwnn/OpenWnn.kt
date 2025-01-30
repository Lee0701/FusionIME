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

import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.preference.PreferenceManager
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import jp.co.omronsoft.openwnn.JAJP.OpenWnnEngineJAJP

/**
 * The OpenWnn IME's base class.
 *
 * @author Copyright (C) 2009-2011 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
open class OpenWnn
/**
 * Constructor
 */
    : InputMethodService() {
    /** Candidate view  */
    protected var mCandidatesViewManager: CandidatesViewManager? = null

    /** Input view (software keyboard)  */
    protected var mInputViewManager: InputViewManager? = null

    /** Conversion engine  */
    protected var mConverter: WnnEngine? = null

    /** Pre-converter (for Romaji-to-Kana input, Hangul input, etc.)  */
    protected var mPreConverter: LetterConverter? = null

    /** The inputing/editing string  */
    var mComposingText: ComposingText? = null

    /** The input connection  */
    protected var mInputConnection: InputConnection? = null

    /** Auto hide candidate view  */
    protected var mAutoHideMode: Boolean = true

    /** Direct input mode  */
    protected var mDirectInputMode: Boolean = true

    /** Flag for checking if the previous down key event is consumed by OpenWnn   */
    private var mConsumeDownEvent = false

    /** TextCandidatesViewManager  */
    protected var mTextCandidatesViewManager: TextCandidatesViewManager? = null

    /** TextCandidates1LineViewManager  */
    protected var mTextCandidates1LineViewManager: TextCandidates1LineViewManager? = null

    /** KeyAction list  */
    private val KeyActionList: MutableList<KeyAction> = ArrayList()

    /***********************************************************************
     * InputMethodService
     */
    /** @see android.inputmethodservice.InputMethodService.onCreate
     */
    override fun onCreate() {
        updateXLargeMode()
        super.onCreate()

        val pref = PreferenceManager.getDefaultSharedPreferences(this)

        currentIme = this


        mTextCandidatesViewManager = TextCandidatesViewManager(-1)
        if (isXLarge) {
            mTextCandidates1LineViewManager =
                TextCandidates1LineViewManager(OpenWnnEngineJAJP.LIMIT_OF_CANDIDATES_1LINE)
            mCandidatesViewManager = mTextCandidates1LineViewManager
        } else {
            mCandidatesViewManager = mTextCandidatesViewManager
        }

        if (mConverter != null) {
            mConverter!!.init()
        }
        if (mComposingText != null) {
            mComposingText!!.clear()
        }
    }

    /** @see android.inputmethodservice.InputMethodService.onCreateCandidatesView
     */
    override fun onCreateCandidatesView(): View {
        if (mCandidatesViewManager != null) {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            if (isXLarge) {
                mCandidatesViewManager = mTextCandidates1LineViewManager
                mTextCandidatesViewManager!!.initView(
                    this,
                    wm.defaultDisplay.width,
                    wm.defaultDisplay.height
                )
            } else {
                mCandidatesViewManager = mTextCandidatesViewManager
            }
            val view = mCandidatesViewManager!!.initView(
                this,
                wm.defaultDisplay.width,
                wm.defaultDisplay.height
            )
            mCandidatesViewManager?.viewType = CandidatesViewManager.VIEW_TYPE_NORMAL
            return view!!
        } else {
            return super.onCreateCandidatesView()
        }
    }

    /** @see android.inputmethodservice.InputMethodService.onCreateInputView
     */
    override fun onCreateInputView(): View {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)


        if (mInputViewManager != null) {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            return mInputViewManager!!.initView(
                this,
                wm.defaultDisplay.width,
                wm.defaultDisplay.height
            )!!
        } else {
            return super.onCreateInputView()
        }
    }

    /** @see android.inputmethodservice.InputMethodService.onDestroy
     */
    override fun onDestroy() {
        super.onDestroy()
        currentIme = null
        close()
    }

    /** @see android.inputmethodservice.InputMethodService.onKeyDown
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        mConsumeDownEvent = onEvent(OpenWnnEvent(event))

        val Keycodeinfo = KeyAction()
        Keycodeinfo.mConsumeDownEvent = mConsumeDownEvent
        Keycodeinfo.mKeyCode = keyCode

        val cnt = KeyActionList.size
        if (cnt != 0) {
            for (i in 0 until cnt) {
                if (KeyActionList[i].mKeyCode == keyCode) {
                    KeyActionList.removeAt(i)
                    break
                }
            }
        }
        KeyActionList.add(Keycodeinfo)
        if (!mConsumeDownEvent) {
            return super.onKeyDown(keyCode, event)
        }
        return mConsumeDownEvent
    }

    /** @see android.inputmethodservice.InputMethodService.onKeyUp
     */
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        var ret = mConsumeDownEvent
        val cnt = KeyActionList.size
        for (i in 0 until cnt) {
            val Keycodeinfo = KeyActionList[i]
            if (Keycodeinfo.mKeyCode == keyCode) {
                ret = Keycodeinfo.mConsumeDownEvent
                KeyActionList.removeAt(i)
                break
            }
        }
        ret = if (!ret) {
            super.onKeyUp(keyCode, event)
        } else {
            onEvent(OpenWnnEvent(event))
        }
        return ret
    }

    /**
     * Called when the key long press event occurred.
     *
     * @see android.inputmethodservice.InputMethodService.onKeyLongPress
     */
    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        if (currentIme == null) {
            Log.e("iWnn", "OpenWnn::onKeyLongPress()  Unprocessing onCreate() ")
            return super.onKeyLongPress(keyCode, event)
        }

        val wnnEvent = OpenWnnEvent(event)
        wnnEvent.code = OpenWnnEvent.KEYLONGPRESS
        return onEvent(wnnEvent)
    }

    /** @see android.inputmethodservice.InputMethodService.onStartInput
     */
    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        mInputConnection = currentInputConnection
        if (!restarting && mComposingText != null) {
            mComposingText!!.clear()
        }
    }

    /** @see android.inputmethodservice.InputMethodService.onStartInputView
     */
    override fun onStartInputView(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInputView(attribute, restarting)
        mInputConnection = currentInputConnection

        setCandidatesViewShown(false)
        if (mInputConnection != null) {
            mDirectInputMode = false
            if (mConverter != null) {
                mConverter!!.init()
            }
        } else {
            mDirectInputMode = true
        }
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        if (mCandidatesViewManager != null) {
            mCandidatesViewManager!!.setPreferences(pref)
        }
        if (mInputViewManager != null) {
            mInputViewManager!!.setPreferences(pref, attribute)
        }
        if (mPreConverter != null) {
            mPreConverter!!.setPreferences(pref)
        }
        if (mConverter != null) {
            mConverter!!.setPreferences(pref)
        }
    }

    /** @see android.inputmethodservice.InputMethodService.requestHideSelf
     */
    override fun requestHideSelf(flag: Int) {
        super.requestHideSelf(flag)
        if (mInputViewManager == null) {
            hideWindow()
        }
    }

    /** @see android.inputmethodservice.InputMethodService.setCandidatesViewShown
     */
    override fun setCandidatesViewShown(shown: Boolean) {
        super.setCandidatesViewShown(shown)
        if (shown) {
            showWindow(true)
        } else {
            if (mAutoHideMode && mInputViewManager == null) {
                hideWindow()
            }
        }
    }

    /** @see android.inputmethodservice.InputMethodService.hideWindow
     */
    override fun hideWindow() {
        super.hideWindow()
        mDirectInputMode = true
        hideStatusIcon()
    }

    /** @see android.inputmethodservice.InputMethodService.onComputeInsets
     */
    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        outInsets.contentTopInsets = outInsets.visibleTopInsets
    }


    /**********************************************************************
     * OpenWnn
     */
    /**
     * Process an event.
     *
     * @param  ev  An event
     * @return  `true` if the event is processed in this method; `false` if not.
     */
    open fun onEvent(ev: OpenWnnEvent): Boolean {
        return false
    }

    /**
     * Search a character for toggle input.
     *
     * @param prevChar     The character input previous
     * @param toggleTable  Toggle table
     * @param reverse      `false` if toggle direction is forward, `true` if toggle direction is backward
     * @return          A character (`null` if no character is found)
     */
    protected fun searchToggleCharacter(
        prevChar: String,
        toggleTable: Array<String>,
        reverse: Boolean
    ): String? {
        var i = 0
        while (i < toggleTable.size) {
            if (prevChar == toggleTable[i]) {
                if (reverse) {
                    i--
                    return if (i < 0) {
                        toggleTable[toggleTable.size - 1]
                    } else {
                        toggleTable[i]
                    }
                } else {
                    i++
                    return if (i == toggleTable.size) {
                        toggleTable[0]
                    } else {
                        toggleTable[i]
                    }
                }
            }
            i++
        }
        return null
    }

    /**
     * Processing of resource open when IME ends.
     */
    protected open fun close() {
        if (mConverter != null) {
            mConverter!!.close()
        }
    }

    /**
     * Update the x large mode.
     */
    fun updateXLargeMode() {
        isXLarge = ((resources.configuration.screenLayout and
                Configuration.SCREENLAYOUT_SIZE_MASK)
                == Configuration.SCREENLAYOUT_SIZE_XLARGE)
    }

    /**
     * Check through key code in IME.
     *
     * @param keyCode  check key code.
     * @return `true` if through key code; `false` otherwise.
     */
    protected fun isThroughKeyCode(keyCode: Int): Boolean {
        val result = when (keyCode) {
            KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MUTE, KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_VOLUME_MUTE, KeyEvent.KEYCODE_MEDIA_CLOSE, KeyEvent.KEYCODE_MEDIA_EJECT, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_RECORD, KeyEvent.KEYCODE_MANNER_MODE -> true
            else -> false
        }
        return result
    }

    /**
     * Check ten-key code.
     *
     * @param keyCode  check key code.
     * @return `true` if ten-key code; `false` not ten-key code.
     */
    protected fun isTenKeyCode(keyCode: Int): Boolean {
        var result = false
        when (keyCode) {
            KeyEvent.KEYCODE_NUMPAD_0, KeyEvent.KEYCODE_NUMPAD_1, KeyEvent.KEYCODE_NUMPAD_2, KeyEvent.KEYCODE_NUMPAD_3, KeyEvent.KEYCODE_NUMPAD_4, KeyEvent.KEYCODE_NUMPAD_5, KeyEvent.KEYCODE_NUMPAD_6, KeyEvent.KEYCODE_NUMPAD_7, KeyEvent.KEYCODE_NUMPAD_8, KeyEvent.KEYCODE_NUMPAD_9, KeyEvent.KEYCODE_NUMPAD_DOT -> result =
                true

            else -> {}
        }
        return result
    }

    companion object {
        /**
         * Whether the x large mode.
         *
         * @return      `true` if x large; `false` if not x large.
         */
        /** for isXLarge  */
        var isXLarge: Boolean = false
            private set

        /**
         * Get the instance of current IME.
         *
         * @return the instance of current IME, See [jp.co.omronsoft.openwnn.OpenWnn]
         */
        /** The instance of current IME  */
        var currentIme: OpenWnn? = null
            private set
    }
}
