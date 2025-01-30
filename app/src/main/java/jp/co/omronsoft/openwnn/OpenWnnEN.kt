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
import android.os.Handler
import android.os.Message
import android.preference.PreferenceManager
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.MetaKeyKeyListener
import android.text.style.BackgroundColorSpan
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import ee.oyatl.ime.fusion.R
import jp.co.omronsoft.openwnn.EN.DefaultSoftKeyboardEN
import jp.co.omronsoft.openwnn.EN.OpenWnnEngineEN
import jp.co.omronsoft.openwnn.EN.TutorialEN
import java.io.File

/**
 * The OpenWnn English IME class.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
class OpenWnnEN() : OpenWnn() {
    /** Spannable string for the composing text  */
    protected var mDisplayText: SpannableStringBuilder

    /** Characters treated as a separator  */
    private var mWordSeparators: String? = null

    /** Previous event's code  */
    private var mPreviousEventCode = 0

    /** Array of words from the user dictionary  */
    private var mUserDictionaryWords: Array<WnnWord?>? = null

    /** The converter for English prediction/spell correction  */
    private var mConverterEN: OpenWnnEngineEN? = null

    /** The symbol list generator  */
    private var mSymbolList: SymbolList?

    /** Whether it is displaying symbol list  */
    private var mSymbolMode: Boolean

    /** Whether prediction is enabled  */
    private var mOptPrediction: Boolean

    /** Whether spell correction is enabled  */
    private var mOptSpellCorrection: Boolean

    /** Whether learning is enabled  */
    private var mOptLearning: Boolean

    /** SHIFT key state  */
    private var mHardShift = 0

    /** SHIFT key state (pressing)  */
    private var mShiftPressing = false

    /** ALT key state  */
    private var mHardAlt = 0

    /** ALT key state (pressing)  */
    private var mAltPressing = false

    /** Auto caps mode  */
    private var mAutoCaps = false

    /** Whether dismissing the keyboard when the enter key is pressed  */
    private var mEnableAutoHideKeyboard = true

    /** Tutorial  */
    private var mTutorial: TutorialEN? = null

    /** Whether tutorial mode or not  */
    private var mEnableTutorial = false

    /** `Handler` for drawing candidates/displaying tutorial  */
    var mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_PREDICTION -> updatePrediction()
                MSG_START_TUTORIAL -> if (mTutorial == null) {
                    if (isInputViewShown) {
                        val inputManager = (mInputViewManager as DefaultSoftKeyboardEN)
                        val v = inputManager.keyboardView
                        mTutorial = TutorialEN(this@OpenWnnEN, v!!, inputManager)

                        mTutorial!!.start()
                    } else {
                        /* Try again soon if the view is not yet showing */
                        sendMessageDelayed(obtainMessage(MSG_START_TUTORIAL), 100)
                    }
                }

                MSG_CLOSE -> {
                    if (mConverterEN != null) mConverterEN!!.close()
                    if (mSymbolList != null) mSymbolList!!.close()
                }
            }
        }
    }

    /**
     * Constructor
     */
    init {
        instance = this

        /* used by OpenWnn */
        mComposingText = ComposingText()
        mCandidatesViewManager = TextCandidatesViewManager(-1)
        mInputViewManager = DefaultSoftKeyboardEN()

        if (OpenWnn.Companion.getCurrentIme() != null) {
            createConverters()
        }

        mConverter = mConverterEN
        mSymbolList = null

        /* etc */
        mDisplayText = SpannableStringBuilder()
        mAutoHideMode = false
        mSymbolMode = false
        mOptPrediction = true
        mOptSpellCorrection = true
        mOptLearning = true
    }

    /**
     * Constructor
     *
     * @param context       The context
     */
    constructor(context: Context?) : this() {
        attachBaseContext(context)
    }

    /**
     * Insert a character into the composing text.
     *
     * @param chars     A array of character
     */
    private fun insertCharToComposingText(chars: CharArray) {
        val seg = StrSegment(chars)

        if (chars[0] == SPACE[0] || chars[0] == '\u0009') {
            /* if the character is a space, commit the composing text */
            commitText(1)
            commitText(seg.string!!)
            mComposingText!!.clear()
        } else if (mWordSeparators!!.contains(seg.string!!)) {
            /* if the character is a separator, remove an auto-inserted space and commit the composing text. */
            if (mPreviousEventCode == OpenWnnEvent.Companion.SELECT_CANDIDATE) {
                mInputConnection!!.deleteSurroundingText(1, 0)
            }
            commitText(1)
            commitText(seg.string!!)
            mComposingText!!.clear()
        } else {
            mComposingText!!.insertStrSegment(0, 1, seg)
            updateComposingText(1)
        }
    }

    /**
     * Insert a character into the composing text.
     *
     * @param charCode      A character code
     * @return              `true` if success; `false` if an error occurs.
     */
    private fun insertCharToComposingText(charCode: Int): Boolean {
        if (charCode == 0) {
            return false
        }
        insertCharToComposingText(Character.toChars(charCode))
        return true
    }

    /**
     * Get the shift key state from the editor.
     *
     * @param editor    Editor
     *
     * @return          State ID of the shift key (0:off, 1:on)
     */
    protected fun getShiftKeyState(editor: EditorInfo): Int {
        return if ((currentInputConnection.getCursorCapsMode(editor.inputType) == 0)) 0 else 1
    }

    /**
     * Set the mode of the symbol list.
     *
     * @param mode      `SymbolList.SYMBOL_ENGLISH` or `null`.
     */
    private fun setSymbolMode(mode: String?) {
        if (mode != null) {
            mHandler.removeMessages(MSG_PREDICTION)
            mSymbolMode = true
            mSymbolList!!.setDictionary(mode)
            mConverter = mSymbolList
        } else {
            if (!mSymbolMode) {
                return
            }
            mHandler.removeMessages(MSG_PREDICTION)
            mSymbolMode = false
            mConverter = mConverterEN
        }
    }

    /***********************************************************************
     * InputMethodServer
     */
    /** @see jp.co.omronsoft.openwnn.OpenWnn.onCreate
     */
    override fun onCreate() {
        super.onCreate()
        mWordSeparators = resources.getString(R.string.en_word_separators)

        createConverters()

        if (mSymbolList == null) {
            mSymbolList = SymbolList(this, SymbolList.Companion.LANG_EN)
        }
    }

    private fun createConverters() {
        if (mConverterEN == null) {
            mConverterEN = OpenWnnEngineEN(
                File(applicationInfo.nativeLibraryDir, "libWnnEngDic.so").absolutePath,
                File(filesDir, "writableEN.dic").absolutePath
            )
        }
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn.onCreateInputView
     */
    override fun onCreateInputView(): View {
        val hiddenState = resources.configuration.hardKeyboardHidden
        val hidden = (hiddenState == Configuration.HARDKEYBOARDHIDDEN_YES)
        (mInputViewManager as DefaultSoftKeyboardEN).setHardKeyboardHidden(hidden)
        mEnableTutorial = hidden

        return super.onCreateInputView()
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn.onStartInputView
     */
    override fun onStartInputView(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInputView(attribute, restarting)

        /* initialize views */
        mCandidatesViewManager!!.clearCandidates()
        mCandidatesViewManager.viewType = CandidatesViewManager.Companion.VIEW_TYPE_CLOSE

        mHardShift = 0
        mHardAlt = 0
        updateMetaKeyStateDisplay()

        /* load preferences */
        val pref = PreferenceManager.getDefaultSharedPreferences(this)

        /* auto caps mode */
        mAutoCaps = pref.getBoolean("auto_caps", true)

        /* set TextCandidatesViewManager's option */
        (mCandidatesViewManager as TextCandidatesViewManager).setAutoHide(true)

        /* display status icon */
        showStatusIcon(R.drawable.immodeic_half_alphabet)

        if (mComposingText != null) {
            mComposingText!!.clear()
        }
        /* initialize the engine's state */
        fitInputType(pref, attribute)

        (mInputViewManager as DefaultSoftKeyboard).resetCurrentKeyboard()

        if (OpenWnn.Companion.isXLarge()) {
            mTextCandidatesViewManager!!.setPreferences(pref)
        }
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn.hideWindow
     */
    override fun hideWindow() {
        ((mInputViewManager as DefaultSoftKeyboard).currentView as BaseInputView).closeDialog()
        mComposingText!!.clear()
        mInputViewManager.onUpdateState(this)
        mHandler.removeMessages(MSG_START_TUTORIAL)
        mInputViewManager.closing()
        if (mTutorial != null) {
            mTutorial!!.close()
            mTutorial = null
        }

        super.hideWindow()
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn.onUpdateSelection
     */
    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int, candidatesStart: Int,
        candidatesEnd: Int
    ) {
        val isNotComposing = ((candidatesStart < 0) && (candidatesEnd < 0))
        if (isNotComposing) {
            mComposingText!!.clear()
            updateComposingText(1)
        } else {
            if (mComposingText!!.size(1) != 0) {
                updateComposingText(1)
            }
        }
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn.onConfigurationChanged
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        try {
            super.onConfigurationChanged(newConfig)
            if (mInputConnection != null) {
                updateComposingText(1)
            }
            /* Hardware keyboard */
            val hiddenState = newConfig.hardKeyboardHidden
            val hidden = (hiddenState == Configuration.HARDKEYBOARDHIDDEN_YES)
            mEnableTutorial = hidden
        } catch (ex: Exception) {
        }
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn.onEvaluateFullscreenMode
     */
    override fun onEvaluateFullscreenMode(): Boolean {
        return false
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn.onEvaluateInputViewShown
     */
    override fun onEvaluateInputViewShown(): Boolean {
        return true
    }

    /***********************************************************************
     * OpenWnn
     */
    /** @see jp.co.omronsoft.openwnn.OpenWnn.onEvent
     */
    @Synchronized
    override fun onEvent(ev: OpenWnnEvent): Boolean {
        /* handling events which are valid when InputConnection is not active. */
        when (ev.code) {
            OpenWnnEvent.Companion.KEYUP -> {
                onKeyUpEvent(ev.keyEvent!!)
                return true
            }

            OpenWnnEvent.Companion.INITIALIZE_LEARNING_DICTIONARY -> return mConverterEN!!.initializeDictionary(
                WnnEngine.Companion.DICTIONARY_TYPE_LEARN
            )

            OpenWnnEvent.Companion.INITIALIZE_USER_DICTIONARY -> return mConverterEN!!.initializeDictionary(
                WnnEngine.Companion.DICTIONARY_TYPE_USER
            )

            OpenWnnEvent.Companion.LIST_WORDS_IN_USER_DICTIONARY -> {
                mUserDictionaryWords = mConverterEN.getUserDictionaryWords()
                return true
            }

            OpenWnnEvent.Companion.GET_WORD -> if (mUserDictionaryWords != null) {
                ev.word = mUserDictionaryWords!![0]
                var i = 0
                while (i < mUserDictionaryWords!!.size - 1) {
                    mUserDictionaryWords!![i] = mUserDictionaryWords!![i + 1]
                    i++
                }
                mUserDictionaryWords!![mUserDictionaryWords!!.size - 1] = null
                if (mUserDictionaryWords!![0] == null) {
                    mUserDictionaryWords = null
                }
                return true
            }

            OpenWnnEvent.Companion.ADD_WORD -> {
                mConverterEN!!.addWord(ev.word!!)
                return true
            }

            OpenWnnEvent.Companion.DELETE_WORD -> {
                mConverterEN!!.deleteWord(ev.word)
                return true
            }

            OpenWnnEvent.Companion.CHANGE_MODE -> return false

            OpenWnnEvent.Companion.UPDATE_CANDIDATE -> {
                updateComposingText(ComposingText.Companion.LAYER1)
                return true
            }

            OpenWnnEvent.Companion.CHANGE_INPUT_VIEW -> {
                setInputView(onCreateInputView())
                return true
            }

            OpenWnnEvent.Companion.CANDIDATE_VIEW_TOUCH -> {
                val ret =
                    (mCandidatesViewManager as TextCandidatesViewManager).onTouchSync()
                return ret
            }

            else -> {}
        }

        dismissPopupKeyboard()
        val keyEvent = ev.keyEvent
        var keyCode = 0
        if (keyEvent != null) {
            keyCode = keyEvent.keyCode
        }
        if (mDirectInputMode) {
            if (ev.code == OpenWnnEvent.Companion.INPUT_SOFT_KEY && mInputConnection != null) {
                mInputConnection!!.sendKeyEvent(keyEvent)
                mInputConnection!!.sendKeyEvent(
                    KeyEvent(
                        KeyEvent.ACTION_UP,
                        keyEvent!!.keyCode
                    )
                )
            }
            return false
        }

        if (ev.code == OpenWnnEvent.Companion.LIST_CANDIDATES_FULL) {
            mCandidatesViewManager.viewType = CandidatesViewManager.Companion.VIEW_TYPE_FULL
            return true
        } else if (ev.code == OpenWnnEvent.Companion.LIST_CANDIDATES_NORMAL) {
            mCandidatesViewManager.viewType = CandidatesViewManager.Companion.VIEW_TYPE_NORMAL
            return true
        }

        var ret = false
        when (ev.code) {
            OpenWnnEvent.Companion.INPUT_CHAR -> {
                (mCandidatesViewManager as TextCandidatesViewManager).setAutoHide(false)
                val edit = currentInputEditorInfo
                if (edit.inputType == EditorInfo.TYPE_CLASS_PHONE) {
                    commitText(String(ev.chars!!))
                } else {
                    setSymbolMode(null)
                    insertCharToComposingText(ev.chars!!)
                    ret = true
                    mPreviousEventCode = ev.code
                }
            }

            OpenWnnEvent.Companion.INPUT_KEY -> {
                keyCode = ev.keyEvent!!.keyCode
                /* update shift/alt state */
                when (keyCode) {
                    KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> {
                        if (ev.keyEvent!!.repeatCount == 0) {
                            if (++mHardAlt > 2) {
                                mHardAlt = 0
                            }
                        }
                        mAltPressing = true
                        updateMetaKeyStateDisplay()
                        return true
                    }

                    KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                        if (ev.keyEvent!!.repeatCount == 0) {
                            if (++mHardShift > 2) {
                                mHardShift = 0
                            }
                        }
                        mShiftPressing = true
                        updateMetaKeyStateDisplay()
                        return true
                    }
                }
                setSymbolMode(null)
                updateComposingText(1)
                /* handle other key event */
                ret = processKeyEvent(ev.keyEvent!!)
                mPreviousEventCode = ev.code
            }

            OpenWnnEvent.Companion.INPUT_SOFT_KEY -> {
                setSymbolMode(null)
                updateComposingText(1)
                ret = processKeyEvent(ev.keyEvent!!)
                if (!ret) {
                    val code = keyEvent!!.keyCode
                    if (code == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                        sendKeyChar('\n')
                    } else {
                        mInputConnection!!.sendKeyEvent(keyEvent)
                        mInputConnection!!.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
                    }
                    ret = true
                }
                mPreviousEventCode = ev.code
            }

            OpenWnnEvent.Companion.SELECT_CANDIDATE -> {
                if (mSymbolMode) {
                    commitText(ev.word!!, false)
                } else {
                    if (mWordSeparators!!.contains(ev.word!!.candidate!!) &&
                        mPreviousEventCode == OpenWnnEvent.Companion.SELECT_CANDIDATE
                    ) {
                        mInputConnection!!.deleteSurroundingText(1, 0)
                    }
                    commitText(ev.word!!, true)
                }
                mComposingText!!.clear()
                mPreviousEventCode = ev.code
                updateComposingText(1)
            }

            OpenWnnEvent.Companion.LIST_SYMBOLS -> {
                commitText(1)
                mComposingText!!.clear()
                setSymbolMode(SymbolList.Companion.SYMBOL_ENGLISH)
                updateComposingText(1)
            }

            else -> {}
        }

        if (mCandidatesViewManager.viewType == CandidatesViewManager.Companion.VIEW_TYPE_FULL) {
            mCandidatesViewManager.viewType = CandidatesViewManager.Companion.VIEW_TYPE_NORMAL
        }

        return ret
    }

    /***********************************************************************
     * OpenWnnEN
     */
    /**
     * Handling KeyEvent
     * <br></br>
     * This method is called from [.onEvent].
     *
     * @param ev   A key event
     * @return      `true` if the event is processed in this method; `false` if the event is not processed in this method
     */
    private fun processKeyEvent(ev: KeyEvent): Boolean {
        val key = ev.keyCode
        val edit = currentInputEditorInfo
        /* keys which produce a glyph */
        if (ev.isPrintingKey) {
            /* do nothing if the character is not able to display or the character is dead key */
            if ((mHardShift > 0 && mHardAlt > 0) || (ev.isAltPressed && ev.isShiftPressed)) {
                val charCode =
                    ev.getUnicodeChar(MetaKeyKeyListener.META_SHIFT_ON or MetaKeyKeyListener.META_ALT_ON)
                if (charCode == 0 || (charCode and KeyCharacterMap.COMBINING_ACCENT) != 0 || charCode == PRIVATE_AREA_CODE) {
                    if (mHardShift == 1) {
                        mShiftPressing = false
                    }
                    if (mHardAlt == 1) {
                        mAltPressing = false
                    }
                    if (!ev.isAltPressed) {
                        if (mHardAlt == 1) {
                            mHardAlt = 0
                        }
                    }
                    if (!ev.isShiftPressed) {
                        if (mHardShift == 1) {
                            mHardShift = 0
                        }
                    }
                    if (!ev.isShiftPressed && !ev.isAltPressed) {
                        updateMetaKeyStateDisplay()
                    }
                    return true
                }
            }

            (mCandidatesViewManager as TextCandidatesViewManager).setAutoHide(false)

            /* get the key character */
            if (mHardShift == 0 && mHardAlt == 0) {
                /* no meta key is locked */
                val shift = if ((mAutoCaps)) getShiftKeyState(edit) else 0
                if (shift != mHardShift && (key >= KeyEvent.KEYCODE_A && key <= KeyEvent.KEYCODE_Z)) {
                    /* handling auto caps for a alphabet character */
                    insertCharToComposingText(ev.getUnicodeChar(MetaKeyKeyListener.META_SHIFT_ON))
                } else {
                    insertCharToComposingText(ev.unicodeChar)
                }
            } else {
                insertCharToComposingText(
                    ev.getUnicodeChar(
                        mShiftKeyToggle[mHardShift]
                                or mAltKeyToggle[mHardAlt]
                    )
                )
                if (mHardShift == 1) {
                    mShiftPressing = false
                }
                if (mHardAlt == 1) {
                    mAltPressing = false
                }
                /* back to 0 (off) if 1 (on/not locked) */
                if (!ev.isAltPressed) {
                    if (mHardAlt == 1) {
                        mHardAlt = 0
                    }
                }
                if (!ev.isShiftPressed) {
                    if (mHardShift == 1) {
                        mHardShift = 0
                    }
                }
                if (!ev.isShiftPressed && !ev.isAltPressed) {
                    updateMetaKeyStateDisplay()
                }
            }

            if (edit.inputType == EditorInfo.TYPE_CLASS_PHONE) {
                commitText(1)
                mComposingText!!.clear()
                return true
            }
            return true
        } else if (key == KeyEvent.KEYCODE_SPACE) {
            if (ev.isAltPressed) {
                /* display the symbol list (G1 specific. same as KEYCODE_SYM) */
                commitText(1)
                mComposingText!!.clear()
                setSymbolMode(SymbolList.Companion.SYMBOL_ENGLISH)
                updateComposingText(1)
                mHardAlt = 0
                updateMetaKeyStateDisplay()
            } else {
                insertCharToComposingText(SPACE)
            }
            return true
        } else if (key == KeyEvent.KEYCODE_SYM) {
            /* display the symbol list */
            commitText(1)
            mComposingText!!.clear()
            setSymbolMode(SymbolList.Companion.SYMBOL_ENGLISH)
            updateComposingText(1)
            mHardAlt = 0
            updateMetaKeyStateDisplay()
        }


        /* Functional key */
        if (mComposingText!!.size(1) > 0) {
            when (key) {
                KeyEvent.KEYCODE_DEL -> {
                    mComposingText!!.delete(1, false)
                    updateComposingText(1)
                    return true
                }

                KeyEvent.KEYCODE_BACK -> {
                    if (mCandidatesViewManager.viewType == CandidatesViewManager.Companion.VIEW_TYPE_FULL) {
                        mCandidatesViewManager.viewType =
                            CandidatesViewManager.Companion.VIEW_TYPE_NORMAL
                    } else {
                        mComposingText!!.clear()
                        updateComposingText(1)
                    }
                    return true
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    mComposingText!!.moveCursor(1, -1)
                    updateComposingText(1)
                    return true
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    mComposingText!!.moveCursor(1, 1)
                    updateComposingText(1)
                    return true
                }

                KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                    commitText(1)
                    mComposingText!!.clear()
                    if (mEnableAutoHideKeyboard) {
                        mInputViewManager!!.closing()
                        requestHideSelf(0)
                    }
                    return true
                }

                else -> return !isThroughKeyCode(key)
            }
        } else {
            /* if there is no composing string. */
            if (mCandidatesViewManager.currentView.isShown) {
                if (key == KeyEvent.KEYCODE_BACK) {
                    if (mCandidatesViewManager.viewType == CandidatesViewManager.Companion.VIEW_TYPE_FULL) {
                        mCandidatesViewManager.viewType =
                            CandidatesViewManager.Companion.VIEW_TYPE_NORMAL
                    } else {
                        mCandidatesViewManager.viewType =
                            CandidatesViewManager.Companion.VIEW_TYPE_CLOSE
                    }
                    return true
                }
            } else {
                when (key) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> if (mEnableAutoHideKeyboard) {
                        mInputViewManager!!.closing()
                        requestHideSelf(0)
                        return true
                    }

                    KeyEvent.KEYCODE_BACK ->                     /*
                     * If 'BACK' key is pressed when the SW-keyboard is shown
                     * and the candidates view is not shown, dismiss the SW-keyboard.
                     */
                        if (isInputViewShown) {
                            mInputViewManager!!.closing()
                            requestHideSelf(0)
                            return true
                        }

                    else -> {}
                }
            }
        }

        return false
    }

    /**
     * Thread for updating the candidates view
     */
    private fun updatePrediction() {
        var candidates = 0
        if (mConverter != null) {
            /* normal prediction */
            candidates = mConverter!!.predict(mComposingText, 0, -1)
        }
        /* update the candidates view */
        if (candidates > 0) {
            mCandidatesViewManager!!.displayCandidates(mConverter)
        } else {
            mCandidatesViewManager!!.clearCandidates()
        }
    }

    /**
     * Update the composing text.
     *
     * @param layer  [mComposingText]'s layer to display
     */
    private fun updateComposingText(layer: Int) {
        /* update the candidates view */
        if (!mOptPrediction) {
            commitText(1)
            mComposingText!!.clear()
            if (mSymbolMode) {
                mHandler.removeMessages(MSG_PREDICTION)
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PREDICTION), 0)
            }
        } else {
            if (mComposingText!!.size(1) != 0) {
                mHandler.removeMessages(MSG_PREDICTION)
                if (mCandidatesViewManager.currentView.isShown) {
                    mHandler.sendMessageDelayed(
                        mHandler.obtainMessage(MSG_PREDICTION),
                        PREDICTION_DELAY_MS_SHOWING_CANDIDATE.toLong()
                    )
                } else {
                    mHandler.sendMessageDelayed(
                        mHandler.obtainMessage(MSG_PREDICTION),
                        PREDICTION_DELAY_MS_1ST.toLong()
                    )
                }
            } else {
                mHandler.removeMessages(MSG_PREDICTION)
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PREDICTION), 0)
            }

            /* notice to the input view */
            mInputViewManager!!.onUpdateState(this)

            /* set the text for displaying as the composing text */
            val disp = mDisplayText
            disp.clear()
            disp.insert(0, mComposingText!!.toString(layer))

            /* add decoration to the text */
            val cursor = mComposingText!!.getCursor(layer)
            if (disp.length != 0) {
                if (cursor > 0 && cursor < disp.length) {
                    disp.setSpan(
                        SPAN_EXACT_BGCOLOR_HL, 0, cursor,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                if (cursor < disp.length) {
                    mDisplayText.setSpan(
                        SPAN_REMAIN_BGCOLOR_HL, cursor, disp.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    mDisplayText.setSpan(
                        SPAN_TEXTCOLOR, 0, disp.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                disp.setSpan(
                    SPAN_UNDERLINE, 0, disp.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            var displayCursor = cursor
            if (FIX_CURSOR_TEXT_END) {
                displayCursor = if ((cursor == 0)) 0 else 1
            }
            /* update the composing text on the EditView */
            mInputConnection!!.setComposingText(disp, displayCursor)
        }
    }

    /**
     * Commit the composing text.
     *
     * @param layer  [mComposingText]'s layer to commit.
     */
    private fun commitText(layer: Int) {
        val tmp = mComposingText!!.toString(layer)

        if (mOptLearning && mConverter != null && tmp!!.length > 0) {
            val word = WnnWord(tmp, tmp)
            mConverter!!.learn(word)
        }

        mInputConnection!!.commitText(tmp, (if (FIX_CURSOR_TEXT_END) 1 else tmp!!.length))
        mCandidatesViewManager!!.clearCandidates()
    }

    /**
     * Commit a word
     *
     * @param word          A word to commit
     * @param withSpace     Append a space after the word if `true`.
     */
    private fun commitText(word: WnnWord, withSpace: Boolean) {
        if (mOptLearning && mConverter != null) {
            mConverter!!.learn(word)
        }

        mInputConnection!!.commitText(
            word.candidate,
            (if (FIX_CURSOR_TEXT_END) 1 else word.candidate!!.length)
        )

        if (withSpace) {
            commitText(" ")
        }
    }

    /**
     * Commit a string
     * <br></br>
     * The string is not registered into the learning dictionary.
     *
     * @param str  A string to commit
     */
    private fun commitText(str: String) {
        mInputConnection!!.commitText(str, (if (FIX_CURSOR_TEXT_END) 1 else str.length))
        mCandidatesViewManager!!.clearCandidates()
    }

    /**
     * Dismiss the pop-up keyboard
     */
    protected fun dismissPopupKeyboard() {
        val kbd = mInputViewManager as DefaultSoftKeyboardEN
        kbd?.dismissPopupKeyboard()
    }

    /**
     * Display current meta-key state.
     */
    private fun updateMetaKeyStateDisplay() {
        var mode = 0
        mode = if (mHardShift == 0 && mHardAlt == 0) {
            DefaultSoftKeyboard.Companion.HARD_KEYMODE_SHIFT_OFF_ALT_OFF
        } else if (mHardShift == 1 && mHardAlt == 0) {
            DefaultSoftKeyboard.Companion.HARD_KEYMODE_SHIFT_ON_ALT_OFF
        } else if (mHardShift == 2 && mHardAlt == 0) {
            DefaultSoftKeyboard.Companion.HARD_KEYMODE_SHIFT_LOCK_ALT_OFF
        } else if (mHardShift == 0 && mHardAlt == 1) {
            DefaultSoftKeyboard.Companion.HARD_KEYMODE_SHIFT_OFF_ALT_ON
        } else if (mHardShift == 0 && mHardAlt == 2) {
            DefaultSoftKeyboard.Companion.HARD_KEYMODE_SHIFT_OFF_ALT_LOCK
        } else if (mHardShift == 1 && mHardAlt == 1) {
            DefaultSoftKeyboard.Companion.HARD_KEYMODE_SHIFT_ON_ALT_ON
        } else if (mHardShift == 1 && mHardAlt == 2) {
            DefaultSoftKeyboard.Companion.HARD_KEYMODE_SHIFT_ON_ALT_LOCK
        } else if (mHardShift == 2 && mHardAlt == 1) {
            DefaultSoftKeyboard.Companion.HARD_KEYMODE_SHIFT_LOCK_ALT_ON
        } else if (mHardShift == 2 && mHardAlt == 2) {
            DefaultSoftKeyboard.Companion.HARD_KEYMODE_SHIFT_LOCK_ALT_LOCK
        } else {
            DefaultSoftKeyboard.Companion.HARD_KEYMODE_SHIFT_OFF_ALT_OFF
        }

        (mInputViewManager as DefaultSoftKeyboard).updateIndicator(mode)
    }

    /**
     * Handling KeyEvent(KEYUP)
     * <br></br>
     * This method is called from [.onEvent].
     *
     * @param ev   An up key event
     */
    private fun onKeyUpEvent(ev: KeyEvent) {
        val key = ev.keyCode
        if (!mShiftPressing) {
            if (key == KeyEvent.KEYCODE_SHIFT_LEFT || key == KeyEvent.KEYCODE_SHIFT_RIGHT) {
                mHardShift = 0
                mShiftPressing = true
                updateMetaKeyStateDisplay()
            }
        }
        if (!mAltPressing) {
            if (key == KeyEvent.KEYCODE_ALT_LEFT || key == KeyEvent.KEYCODE_ALT_RIGHT) {
                mHardAlt = 0
                mAltPressing = true
                updateMetaKeyStateDisplay()
            }
        }
    }

    /**
     * Fits an editor info.
     *
     * @param preferences  The preference data.
     * @param info          The editor info.
     */
    private fun fitInputType(preference: SharedPreferences, info: EditorInfo) {
        if (info.inputType == EditorInfo.TYPE_NULL) {
            mDirectInputMode = true
            return
        }

        mEnableAutoHideKeyboard = false

        /* set prediction & spell correction mode */
        mOptPrediction = preference.getBoolean("opt_en_prediction", true)
        mOptSpellCorrection = preference.getBoolean("opt_en_spell_correction", true)
        mOptLearning = preference.getBoolean("opt_en_enable_learning", true)

        /* prediction on/off */
        when (info.inputType and EditorInfo.TYPE_MASK_CLASS) {
            EditorInfo.TYPE_CLASS_NUMBER, EditorInfo.TYPE_CLASS_DATETIME, EditorInfo.TYPE_CLASS_PHONE -> {
                mOptPrediction = false
                mOptLearning = false
            }

            EditorInfo.TYPE_CLASS_TEXT -> when (info.inputType and EditorInfo.TYPE_MASK_VARIATION) {
                EditorInfo.TYPE_TEXT_VARIATION_PASSWORD, EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> {
                    mOptLearning = false
                    mOptPrediction = false
                }

                EditorInfo.TYPE_TEXT_VARIATION_PHONETIC -> {
                    mOptLearning = false
                    mOptPrediction = false
                }

                else -> {}
            }
        }

        /* doesn't learn any word if it is not prediction mode */
        if (!mOptPrediction) {
            mOptLearning = false
        }

        /* set engine's mode */
        if (mOptSpellCorrection) {
            mConverterEN!!.setDictionary(OpenWnnEngineEN.Companion.DICT_FOR_CORRECT_MISTYPE)
        } else {
            mConverterEN!!.setDictionary(OpenWnnEngineEN.Companion.DICT_DEFAULT)
        }
        checkTutorial(info.privateImeOptions)
    }

    /**
     * Check and start the tutorial if it is the tutorial mode.
     *
     * @param privateImeOptions IME's options
     */
    private fun checkTutorial(privateImeOptions: String?) {
        if (privateImeOptions == null) return
        if (privateImeOptions == "com.google.android.setupwizard:ShowTutorial") {
            if ((mTutorial == null) && mEnableTutorial) startTutorial()
        } else if (privateImeOptions == "com.google.android.setupwizard:HideTutorial") {
            if (mTutorial != null) {
                if (mTutorial!!.close()) {
                    mTutorial = null
                }
            }
        }
    }

    /**
     * Start the tutorial
     */
    private fun startTutorial() {
        val inputManager = (mInputViewManager as DefaultSoftKeyboardEN)
        val v = inputManager.keyboardView
        v!!.setOnTouchListener { v, event -> true }
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_START_TUTORIAL), 500)
    }

    /**
     * Close the tutorial
     */
    fun tutorialDone() {
        mTutorial = null
    }

    /** @see OpenWnn.close
     */
    override fun close() {
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CLOSE), 0)
    }

    companion object {
        /** A space character  */
        private val SPACE = charArrayOf(' ')

        /** Character style of underline  */
        private val SPAN_UNDERLINE: CharacterStyle = UnderlineSpan()

        /** Highlight color style for the selected string   */
        private val SPAN_EXACT_BGCOLOR_HL: CharacterStyle = BackgroundColorSpan(-0x993256)

        /** Highlight color style for the composing text  */
        private val SPAN_REMAIN_BGCOLOR_HL: CharacterStyle = BackgroundColorSpan(-0xf0001)

        /** Highlight text color  */
        private val SPAN_TEXTCOLOR: CharacterStyle = ForegroundColorSpan(-0x1000000)

        /** A private area code(ALT+SHIFT+X) to be ignore (G1 specific).  */
        private const val PRIVATE_AREA_CODE = 61184

        /** Never move cursor in to the composing text (adapting to IMF's specification change)  */
        private const val FIX_CURSOR_TEXT_END = true

        /**
         * Get the instance of this service.
         * <br></br>
         * Before using this method, the constructor of this service must be invoked.
         *
         * @return      The instance of this object
         */
        /** Instance of this service  */
        var instance: OpenWnnEN? = null
            private set

        /** Shift lock toggle definition  */
        private val mShiftKeyToggle =
            intArrayOf(0, MetaKeyKeyListener.META_SHIFT_ON, MetaKeyKeyListener.META_CAP_LOCKED)

        /** ALT lock toggle definition  */
        private val mAltKeyToggle =
            intArrayOf(0, MetaKeyKeyListener.META_ALT_ON, MetaKeyKeyListener.META_ALT_LOCKED)

        /** Message for `mHandler` (execute prediction)  */
        private const val MSG_PREDICTION = 0

        /** Message for `mHandler` (execute tutorial)  */
        private const val MSG_START_TUTORIAL = 1

        /** Message for `mHandler` (close)  */
        private const val MSG_CLOSE = 2

        /** Delay time(msec.) to start prediction after key input when the candidates view is not shown.  */
        private const val PREDICTION_DELAY_MS_1ST = 200

        /** Delay time(msec.) to start prediction after key input when the candidates view is shown.  */
        private const val PREDICTION_DELAY_MS_SHOWING_CANDIDATE = 200
    }
}
