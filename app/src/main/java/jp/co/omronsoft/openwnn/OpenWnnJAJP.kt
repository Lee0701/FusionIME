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
import android.view.inputmethod.InputConnection
import ee.oyatl.ime.fusion.R
import jp.co.omronsoft.openwnn.EN.OpenWnnEngineEN
import jp.co.omronsoft.openwnn.JAJP.DefaultSoftKeyboardJAJP
import jp.co.omronsoft.openwnn.JAJP.OpenWnnEngineJAJP
import jp.co.omronsoft.openwnn.JAJP.Romkan
import jp.co.omronsoft.openwnn.JAJP.RomkanFullKatakana
import jp.co.omronsoft.openwnn.JAJP.RomkanHalfKatakana
import jp.co.omronsoft.openwnn.JAJP.TutorialJAJP
import java.io.File
import java.util.regex.Pattern


/**
 * The OpenWnn Japanese IME class
 *
 * @author Copyright (C) 2009-2011 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
class OpenWnnJAJP() : OpenWnn() {
    /** Convert engine's state  */
    private inner class EngineState {
        /** Set of dictionaries  */
        var dictionarySet: Int = Companion.INVALID

        /** Type of conversion  */
        var convertType: Int = Companion.INVALID

        /** Temporary mode  */
        var temporaryMode: Int = Companion.INVALID

        /** Preference dictionary setting  */
        var preferenceDictionary: Int = Companion.INVALID

        /** keyboard  */
        var keyboard: Int = Companion.INVALID

        val isRenbun: Boolean
            /**
             * Returns whether current type of conversion is consecutive clause(RENBUNSETSU) conversion.
             *
             * @return `true` if current type of conversion is consecutive clause conversion.
             */
            get() = convertType == Companion.CONVERT_TYPE_RENBUN

        val isEisuKana: Boolean
            /**
             * Returns whether current type of conversion is EISU-KANA conversion.
             *
             * @return `true` if current type of conversion is EISU-KANA conversion.
             */
            get() = convertType == Companion.CONVERT_TYPE_EISU_KANA

        val isConvertState: Boolean
            /**
             * Returns whether current type of conversion is no conversion.
             *
             * @return `true` if no conversion is executed currently.
             */
            get() = convertType != Companion.CONVERT_TYPE_NONE

        val isSymbolList: Boolean
            /**
             * Check whether or not the mode is "symbol list".
             *
             * @return `true` if the mode is "symbol list".
             */
            get() = temporaryMode == Companion.TEMPORARY_DICTIONARY_MODE_SYMBOL

        val isEnglish: Boolean
            /**
             * Check whether or not the current language is English.
             *
             * @return `true` if the current language is English.
             */
            get() = dictionarySet == Companion.DICTIONARYSET_EN

        companion object {
            /** Definition for `EngineState.*` (invalid)  */
            const val INVALID: Int = -1

            /** Definition for `EngineState.dictionarySet` (Japanese)  */
            const val DICTIONARYSET_JP: Int = 0

            /** Definition for `EngineState.dictionarySet` (English)  */
            const val DICTIONARYSET_EN: Int = 1

            /** Definition for `EngineState.convertType` (prediction/no conversion)  */
            const val CONVERT_TYPE_NONE: Int = 0

            /** Definition for `EngineState.convertType` (consecutive clause conversion)  */
            const val CONVERT_TYPE_RENBUN: Int = 1

            /** Definition for `EngineState.convertType` (EISU-KANA conversion)  */
            const val CONVERT_TYPE_EISU_KANA: Int = 2

            /** Definition for `EngineState.temporaryMode` (change back to the normal dictionary)  */
            const val TEMPORARY_DICTIONARY_MODE_NONE: Int = 0

            /** Definition for `EngineState.temporaryMode` (change to the symbol dictionary)  */
            const val TEMPORARY_DICTIONARY_MODE_SYMBOL: Int = 1

            /** Definition for `EngineState.temporaryMode` (change to the user dictionary)  */
            const val TEMPORARY_DICTIONARY_MODE_USER: Int = 2

            /** Definition for `EngineState.preferenceDictionary` (no preference dictionary)  */
            const val PREFERENCE_DICTIONARY_NONE: Int = 0

            /** Definition for `EngineState.preferenceDictionary` (person's name)  */
            const val PREFERENCE_DICTIONARY_PERSON_NAME: Int = 1

            /** Definition for `EngineState.preferenceDictionary` (place name)  */
            const val PREFERENCE_DICTIONARY_POSTAL_ADDRESS: Int = 2

            /** Definition for `EngineState.preferenceDictionary` (email/URI)  */
            const val PREFERENCE_DICTIONARY_EMAIL_ADDRESS_URI: Int = 3

            /** Definition for `EngineState.keyboard` (undefined)  */
            const val KEYBOARD_UNDEF: Int = 0

            /** Definition for `EngineState.keyboard` (QWERTY)  */
            const val KEYBOARD_QWERTY: Int = 1

            /** Definition for `EngineState.keyboard` (12-keys)  */
            const val KEYBOARD_12KEY: Int = 2
        }
    }

    /** IME's status  */
    protected var mStatus: Int = STATUS_INIT

    /** Whether exact match searching or not  */
    protected var mExactMatchMode: Boolean = false

    /** Spannable string builder for displaying the composing text  */
    protected var mDisplayText: SpannableStringBuilder

    /** Backup for switching the converter  */
    private var mConverterBack: WnnEngine? = null

    /** Backup for switching the pre-converter  */
    private var mPreConverterBack: LetterConverter? = null

    /** OpenWnn conversion engine for Japanese  */
    private var mConverterJAJP: OpenWnnEngineJAJP? = null

    /** OpenWnn conversion engine for English  */
    private var mConverterEN: OpenWnnEngineEN? = null

    /** Conversion engine for listing symbols  */
    private var mConverterSymbolEngineBack: SymbolList? = null

    /** Current symbol list  */
    private var mCurrentSymbol = -1

    /** Romaji-to-Kana converter (HIRAGANA)  */
    private val mPreConverterHiragana: Romkan

    /** Romaji-to-Kana converter (full-width KATAKANA)  */
    private val mPreConverterFullKatakana: RomkanFullKatakana

    /** Romaji-to-Kana converter (half-width KATAKANA)  */
    private val mPreConverterHalfKatakana: RomkanHalfKatakana

    /** Conversion Engine's state  */
    private val mEngineState: EngineState = EngineState()

    /** Whether learning function is active of not.  */
    private var mEnableLearning = true

    /** Whether prediction is active or not.  */
    private var mEnablePrediction = true

    /** Whether using the converter  */
    private var mEnableConverter = true

    /** Whether displaying the symbol list  */
    private var mEnableSymbolList = true

    /** Whether non ASCII code is enabled  */
    private var mEnableSymbolListNonHalf = true

    /** Enable mistyping spell correction or not  */
    private var mEnableSpellCorrection = true

    /** Auto commit state (in English mode)  */
    private var mDisableAutoCommitEnglishMask = AUTO_COMMIT_ENGLISH_ON

    /** Whether removing a space before a separator or not. (in English mode)  */
    private var mEnableAutoDeleteSpace = false

    /** Whether auto-spacing is enabled or not.  */
    private var mEnableAutoInsertSpace = true

    /** Whether dismissing the keyboard when the enter key is pressed  */
    private var mEnableAutoHideKeyboard = true

    /** Number of committed clauses on consecutive clause conversion  */
    private var mCommitCount = 0

    /** Target layer of the [ComposingText]  */
    private var mTargetLayer = 1

    /** Current orientation of the display  */
    private var mOrientation = Configuration.ORIENTATION_UNDEFINED

    /** Current normal dictionary set  */
    private var mPrevDictionarySet: Int = OpenWnnEngineJAJP.Companion.DIC_LANG_INIT

    /** Regular expression pattern for English separators  */
    private var mEnglishAutoCommitDelimiter: Pattern? = null

    /** Cursor position in the composing text  */
    private var mComposingStartCursor = 0

    /** Cursor position before committing text  */
    private var mCommitStartCursor = 0

    /** Previous committed text  */
    private var mPrevCommitText: StringBuffer? = null

    /** Call count of `commitText`  */
    private var mPrevCommitCount = 0

    /** Shift lock status of the Hardware keyboard  */
    private var mHardShift = 0

    /** SHIFT key state (pressing)  */
    private var mShiftPressing = false

    /** ALT lock status of the Hardware keyboard  */
    private var mHardAlt = 0

    /** ALT key state (pressing)  */
    private var mAltPressing = false

    /** Auto caps mode  */
    private var mAutoCaps = false

    /** List of words in the user dictionary  */
    private var mUserDictionaryWords: Array<WnnWord?>? = null

    /** Tutorial  */
    private var mTutorial: TutorialJAJP? = null

    /** Whether tutorial mode or not  */
    private var mEnableTutorial = false

    /** Whether there is a continued predicted candidate  */
    private var mHasContinuedPrediction = false

    /** Whether text selection has started  */
    private var mHasStartedTextSelection = true

    /** Whether the H/W 12keyboard is active or not.  */
    private var mEnableHardware12Keyboard = false

    /** `Handler` for drawing candidates/displaying tutorial  */
    var mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_PREDICTION -> updatePrediction()
                MSG_START_TUTORIAL -> if (mTutorial == null) {
                    if (isInputViewShown) {
                        val inputManager = (mInputViewManager as DefaultSoftKeyboardJAJP)
                        val v = inputManager.keyboardView
                        mTutorial = TutorialJAJP(this@OpenWnnJAJP, v!!, inputManager)

                        mTutorial!!.start()
                    } else {
                        /* Try again soon if the view is not yet showing */
                        sendMessageDelayed(obtainMessage(MSG_START_TUTORIAL), 100)
                    }
                }

                MSG_CLOSE -> {
                    if (mConverterJAJP != null) mConverterJAJP!!.close()
                    if (mConverterEN != null) mConverterEN!!.close()
                    if (mConverterSymbolEngineBack != null) mConverterSymbolEngineBack!!.close()
                }
            }
        }
    }

    /** The candidate filter  */
    private val mFilter: CandidateFilter

    /**
     * Constructor
     */
    init {
        instance = this
        mComposingText = ComposingText()
        mCandidatesViewManager = TextCandidatesViewManager(-1)
        mInputViewManager = DefaultSoftKeyboardJAJP()

        if (OpenWnn.Companion.getCurrentIme() != null) {
            createConverters()
        }

        mPreConverterHiragana = Romkan()
        mPreConverter = mPreConverterHiragana
        mPreConverterFullKatakana = RomkanFullKatakana()
        mPreConverterHalfKatakana = RomkanHalfKatakana()
        mFilter = CandidateFilter()

        mDisplayText = SpannableStringBuilder()
        mAutoHideMode = false

        mPrevCommitText = StringBuffer()
    }

    /**
     * Constructor
     *
     * @param context       The context
     */
    constructor(context: Context?) : this() {
        attachBaseContext(context)
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn.onCreate
     */
    override fun onCreate() {
        updateXLargeMode()
        super.onCreate()

        createConverters()

        val delimiter = Pattern.quote(resources.getString(R.string.en_word_separators))
        mEnglishAutoCommitDelimiter = Pattern.compile(".*[$delimiter]$")
        if (mConverterSymbolEngineBack == null) {
            mConverterSymbolEngineBack = SymbolList(this, SymbolList.Companion.LANG_JA)
        }
    }

    private fun createConverters() {
        if (mConverter == null || mConverterJAJP == null) {
            mConverterJAJP = OpenWnnEngineJAJP(
                File(applicationInfo.nativeLibraryDir, "libWnnJpnDic.so").absolutePath,
                File(filesDir, "writableJAJP.dic").absolutePath
            )
            mConverter = mConverterJAJP
        }
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
        val type12Key = (resources.configuration.keyboard == Configuration.KEYBOARD_12KEY)
        (mInputViewManager as DefaultSoftKeyboardJAJP).setHardKeyboardHidden(hidden)
        (mInputViewManager as DefaultSoftKeyboard).setHardware12Keyboard(type12Key)
        mTextCandidatesViewManager!!.setHardKeyboardHidden(hidden)
        mEnableTutorial = hidden
        mEnableHardware12Keyboard = type12Key
        return super.onCreateInputView()
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn.onStartInputView
     */
    override fun onStartInputView(attribute: EditorInfo, restarting: Boolean) {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        if (restarting) {
            super.onStartInputView(attribute, restarting)
        } else {
            val state: EngineState = EngineState()
            state.temporaryMode = EngineState.Companion.TEMPORARY_DICTIONARY_MODE_NONE
            updateEngineState(state)

            mPrevCommitCount = 0
            clearCommitInfo()

            (mInputViewManager as DefaultSoftKeyboard).resetCurrentKeyboard()

            super.onStartInputView(attribute, restarting)

            if (OpenWnn.Companion.isXLarge()) {
                mTextCandidatesViewManager!!.setPreferences(pref)
            }

            mCandidatesViewManager!!.clearCandidates()
            mStatus = STATUS_INIT
            mExactMatchMode = false

            /* hardware keyboard support */
            mHardShift = 0
            mHardAlt = 0
            updateMetaKeyStateDisplay()
        }

        /* initialize the engine's state */
        fitInputType(pref, attribute)

        if (OpenWnn.Companion.isXLarge()) {
            mTextCandidates1LineViewManager!!.setAutoHide(true)
        } else {
            (mCandidatesViewManager as TextCandidatesViewManager).setAutoHide(true)
        }

        if (isEnableL2Converter) {
            breakSequence()
        }
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn.hideWindow
     */
    override fun hideWindow() {
        mCandidatesViewManager!!.setCandidateMsgRemove()

        val baseInputView =
            ((mInputViewManager as DefaultSoftKeyboard).currentView as BaseInputView)
        baseInputView?.closeDialog()
        mComposingText!!.clear()
        mInputViewManager.onUpdateState(this)
        clearCommitInfo()
        mHandler.removeMessages(MSG_START_TUTORIAL)
        mInputViewManager.closing()
        if (mTutorial != null) {
            mTutorial!!.close()
            mTutorial = null
        }

        if (OpenWnn.Companion.isXLarge()) {
            mTextCandidates1LineViewManager!!.closeDialog()
        } else {
            mTextCandidatesViewManager!!.closeDialog()
        }

        super.hideWindow()
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn.onUpdateSelection
     */
    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        mComposingStartCursor = if ((candidatesStart < 0)) newSelEnd else candidatesStart

        val prevSelection = mHasStartedTextSelection
        if (newSelStart != newSelEnd) {
            clearCommitInfo()
            mHasStartedTextSelection = true
        } else {
            mHasStartedTextSelection = false
        }

        if (mHasContinuedPrediction) {
            mHasContinuedPrediction = false
            if (0 < mPrevCommitCount) {
                mPrevCommitCount--
            }
            return
        }

        if (mEngineState.isSymbolList()) {
            return
        }

        val isNotComposing = ((candidatesStart < 0) && (candidatesEnd < 0))
        if ((mComposingText!!.size(ComposingText.Companion.LAYER1) != 0)
            && !isNotComposing
        ) {
            updateViewStatus(mTargetLayer, false, true)
        } else {
            if (0 < mPrevCommitCount) {
                mPrevCommitCount--
            } else {
                val commitEnd = mCommitStartCursor + mPrevCommitText!!.length
                if ((((newSelEnd < oldSelEnd) || (commitEnd < newSelEnd)) && clearCommitInfo())
                    || isNotComposing
                ) {
                    if (isEnableL2Converter) {
                        breakSequence()
                    }

                    if (mInputConnection != null) {
                        if (isNotComposing && (mComposingText!!.size(ComposingText.Companion.LAYER1) != 0)) {
                            mInputConnection!!.finishComposingText()
                        }
                    }
                    if ((prevSelection != mHasStartedTextSelection) || !mHasStartedTextSelection) {
                        initializeScreen()
                    }
                }
            }
        }
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn.onConfigurationChanged
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        try {
            super.onConfigurationChanged(newConfig)

            if (mInputConnection != null) {
                if (super.isInputViewShown()) {
                    updateViewStatus(mTargetLayer, true, true)
                }

                /* display orientation */
                if (mOrientation != newConfig.orientation) {
                    mOrientation = newConfig.orientation
                    commitConvertingText()
                    initializeScreen()
                }

                /* Hardware keyboard */
                val hiddenState = newConfig.hardKeyboardHidden
                val hidden = (hiddenState == Configuration.HARDKEYBOARDHIDDEN_YES)
                val type12Key = (newConfig.keyboard == Configuration.KEYBOARD_12KEY)
                (mInputViewManager as DefaultSoftKeyboardJAJP).setHardKeyboardHidden(hidden)
                (mInputViewManager as DefaultSoftKeyboard).setHardware12Keyboard(type12Key)
                mTextCandidatesViewManager!!.setHardKeyboardHidden(hidden)
                mEnableTutorial = hidden
                mEnableHardware12Keyboard = type12Key
            }
        } catch (ex: Exception) {
            /* do nothing if an error occurs. */
        }
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn.onEvent
     */
    @Synchronized
    override fun onEvent(ev: OpenWnnEvent): Boolean {
        var state: EngineState

        /* handling events which are valid when InputConnection is not active. */
        when (ev.code) {
            OpenWnnEvent.Companion.KEYUP -> {
                onKeyUpEvent(ev.keyEvent!!)
                return true
            }

            OpenWnnEvent.Companion.KEYLONGPRESS -> return onKeyLongPressEvent(ev.keyEvent)

            OpenWnnEvent.Companion.INITIALIZE_LEARNING_DICTIONARY -> {
                mConverterEN!!.initializeDictionary(WnnEngine.Companion.DICTIONARY_TYPE_LEARN)
                mConverterJAJP!!.initializeDictionary(WnnEngine.Companion.DICTIONARY_TYPE_LEARN)
                return true
            }

            OpenWnnEvent.Companion.INITIALIZE_USER_DICTIONARY -> return mConverterJAJP!!.initializeDictionary(
                WnnEngine.Companion.DICTIONARY_TYPE_USER
            )

            OpenWnnEvent.Companion.LIST_WORDS_IN_USER_DICTIONARY -> {
                mUserDictionaryWords = mConverterJAJP.getUserDictionaryWords()
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
                mConverterJAJP!!.addWord(ev.word!!)
                return true
            }

            OpenWnnEvent.Companion.DELETE_WORD -> {
                mConverterJAJP!!.deleteWord(ev.word)
                return true
            }

            OpenWnnEvent.Companion.CHANGE_MODE -> {
                changeEngineMode(ev.mode)
                if (!(ev.mode == ENGINE_MODE_SYMBOL || ev.mode == ENGINE_MODE_EISU_KANA)) {
                    initializeScreen()
                }
                return true
            }

            OpenWnnEvent.Companion.UPDATE_CANDIDATE -> {
                if (mEngineState.isRenbun()) {
                    mComposingText!!.setCursor(
                        ComposingText.Companion.LAYER1,
                        mComposingText!!.toString(ComposingText.Companion.LAYER1)!!.length
                    )
                    mExactMatchMode = false
                    updateViewStatusForPrediction(true, true)
                } else {
                    updateViewStatus(mTargetLayer, true, true)
                }
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

            OpenWnnEvent.Companion.TOUCH_OTHER_KEY -> {
                mStatus = mStatus or STATUS_INPUT_EDIT
                return true
            }

            OpenWnnEvent.Companion.CANDIDATE_VIEW_SCROLL_UP -> {
                if (mCandidatesViewManager is TextCandidatesViewManager) {
                    (mCandidatesViewManager as TextCandidatesViewManager).setScrollUp()
                }
                return true
            }

            OpenWnnEvent.Companion.CANDIDATE_VIEW_SCROLL_DOWN -> {
                if (mCandidatesViewManager is TextCandidatesViewManager) {
                    (mCandidatesViewManager as TextCandidatesViewManager).setScrollDown()
                }
                return true
            }

            OpenWnnEvent.Companion.CANDIDATE_VIEW_SCROLL_FULL_UP -> {
                if (mCandidatesViewManager is TextCandidatesViewManager) {
                    (mCandidatesViewManager as TextCandidatesViewManager).setScrollFullUp()
                }
                return true
            }

            OpenWnnEvent.Companion.CANDIDATE_VIEW_SCROLL_FULL_DOWN -> {
                if (mCandidatesViewManager is TextCandidatesViewManager) {
                    (mCandidatesViewManager as TextCandidatesViewManager).setScrollFullDown()
                }
                return true
            }

            OpenWnnEvent.Companion.FOCUS_CANDIDATE_START -> return true

            OpenWnnEvent.Companion.FOCUS_CANDIDATE_END -> {
                mInputViewManager!!.onUpdateState(this)
                return true
            }

            else -> {}
        }

        val keyEvent = ev.keyEvent
        var keyCode = 0
        if (keyEvent != null) {
            keyCode = keyEvent.keyCode
        }

        if (mDirectInputMode) {
            if (mInputConnection != null) {
                when (ev.code) {
                    OpenWnnEvent.Companion.INPUT_SOFT_KEY -> if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        sendKeyChar('\n')
                    } else {
                        mInputConnection!!.sendKeyEvent(keyEvent)
                        mInputConnection!!.sendKeyEvent(
                            KeyEvent(
                                KeyEvent.ACTION_UP,
                                keyEvent!!.keyCode
                            )
                        )
                    }

                    OpenWnnEvent.Companion.INPUT_CHAR -> sendKeyChar(ev.chars!![0])
                    else -> {}
                }
            }

            /* return if InputConnection is not active */
            return false
        }

        if (mEngineState.isSymbolList()) {
            if (keyEvent != null && keyEvent.isPrintingKey && isTenKeyCode(keyCode) && !keyEvent.isNumLockOn) {
                return false
            }
            when (keyCode) {
                KeyEvent.KEYCODE_DEL -> return false

                KeyEvent.KEYCODE_BACK -> {
                    initializeScreen()
                    return true
                }

                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    if (mCandidatesViewManager!!.isFocusCandidate) {
                        mCandidatesViewManager!!.selectFocusCandidate()
                        return true
                    }
                    return false
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (mCandidatesViewManager!!.isFocusCandidate) {
                        processLeftKeyEvent()
                        return true
                    }
                    return false
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (mCandidatesViewManager!!.isFocusCandidate) {
                        processRightKeyEvent()
                        return true
                    }
                    return false
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    processDownKeyEvent()
                    return true
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (mCandidatesViewManager!!.isFocusCandidate) {
                        processUpKeyEvent()
                        return true
                    }
                    return false
                }

                KeyEvent.KEYCODE_SPACE -> {
                    if (keyEvent != null) {
                        if (keyEvent.isShiftPressed) {
                            onEvent(OpenWnnEvent(OpenWnnEvent.Companion.CANDIDATE_VIEW_SCROLL_UP))
                        } else if (keyEvent.isAltPressed) {
                            if (keyEvent.repeatCount == 0) {
                                switchSymbolList()
                            }
                        } else {
                            onEvent(OpenWnnEvent(OpenWnnEvent.Companion.CANDIDATE_VIEW_SCROLL_DOWN))
                        }
                    }
                    return true
                }

                KeyEvent.KEYCODE_SYM -> {
                    switchSymbolList()
                    return true
                }

                KeyEvent.KEYCODE_PAGE_UP -> {
                    onEvent(OpenWnnEvent(OpenWnnEvent.Companion.CANDIDATE_VIEW_SCROLL_UP))
                    return true
                }

                KeyEvent.KEYCODE_PAGE_DOWN -> {
                    onEvent(OpenWnnEvent(OpenWnnEvent.Companion.CANDIDATE_VIEW_SCROLL_DOWN))
                    return true
                }

                KeyEvent.KEYCODE_PICTSYMBOLS -> {
                    if (keyEvent != null) {
                        if (keyEvent.repeatCount == 0) {
                            switchSymbolList()
                        }
                    }
                    return true
                }

                else -> {}
            }

            if ((ev.code == OpenWnnEvent.Companion.INPUT_KEY) &&
                (keyCode != KeyEvent.KEYCODE_SEARCH) &&
                (keyCode != KeyEvent.KEYCODE_ALT_LEFT) &&
                (keyCode != KeyEvent.KEYCODE_ALT_RIGHT) &&
                (keyCode != KeyEvent.KEYCODE_SHIFT_LEFT) &&
                (keyCode != KeyEvent.KEYCODE_SHIFT_RIGHT)
            ) {
                state = EngineState()
                state.temporaryMode = EngineState.Companion.TEMPORARY_DICTIONARY_MODE_NONE
                updateEngineState(state)
            }
        }

        if (!((ev.code == OpenWnnEvent.Companion.COMMIT_COMPOSING_TEXT)
                    || ((keyEvent != null)
                    && ((keyCode == KeyEvent.KEYCODE_SHIFT_LEFT)
                    || (keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT)
                    || (keyCode == KeyEvent.KEYCODE_ALT_LEFT)
                    || (keyCode == KeyEvent.KEYCODE_ALT_RIGHT)
                    || (keyEvent.isAltPressed && (keyCode == KeyEvent.KEYCODE_SPACE)))))
        ) {
            clearCommitInfo()
        }

        /* change back the dictionary if necessary */
        if (!((ev.code == OpenWnnEvent.Companion.SELECT_CANDIDATE)
                    || (ev.code == OpenWnnEvent.Companion.LIST_CANDIDATES_NORMAL)
                    || (ev.code == OpenWnnEvent.Companion.LIST_CANDIDATES_FULL)
                    || ((keyEvent != null)
                    && ((keyCode == KeyEvent.KEYCODE_SHIFT_LEFT)
                    || (keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT)
                    || (keyCode == KeyEvent.KEYCODE_ALT_LEFT)
                    || (keyCode == KeyEvent.KEYCODE_ALT_RIGHT)
                    || (keyCode == KeyEvent.KEYCODE_BACK && mCandidatesViewManager.viewType == CandidatesViewManager.Companion.VIEW_TYPE_FULL)
                    || (keyEvent.isAltPressed && (keyCode == KeyEvent.KEYCODE_SPACE)))))
        ) {
            state = EngineState()
            state.temporaryMode = EngineState.Companion.TEMPORARY_DICTIONARY_MODE_NONE
            updateEngineState(state)
        }

        if ((ev.code == OpenWnnEvent.Companion.INPUT_KEY) && processHardware12Keyboard(keyEvent)) {
            return true
        }

        if (ev.code == OpenWnnEvent.Companion.LIST_CANDIDATES_FULL) {
            mStatus = mStatus or STATUS_CANDIDATE_FULL
            mCandidatesViewManager.viewType = CandidatesViewManager.Companion.VIEW_TYPE_FULL
            if (!mEngineState.isSymbolList()) {
                mInputViewManager!!.hideInputView()
            }
            return true
        } else if (ev.code == OpenWnnEvent.Companion.LIST_CANDIDATES_NORMAL) {
            mStatus = mStatus and STATUS_CANDIDATE_FULL.inv()
            mCandidatesViewManager.viewType = CandidatesViewManager.Companion.VIEW_TYPE_NORMAL
            mInputViewManager!!.showInputView()
            return true
        }

        var ret = false
        when (ev.code) {
            OpenWnnEvent.Companion.INPUT_CHAR -> {
                if ((mPreConverter == null) && !isEnableL2Converter) {
                    /* direct input (= full-width alphabet/number input) */
                    commitText(false)
                    commitText(String(ev.chars!!))
                    mCandidatesViewManager!!.clearCandidates()
                } else if (!isEnableL2Converter) {
                    processSoftKeyboardCodeWithoutConversion(ev.chars)
                } else {
                    processSoftKeyboardCode(ev.chars)
                }
                ret = true
            }

            OpenWnnEvent.Companion.TOGGLE_CHAR -> {
                processSoftKeyboardToggleChar(ev.toggleTable)
                ret = true
            }

            OpenWnnEvent.Companion.TOGGLE_REVERSE_CHAR -> if (((mStatus and STATUS_CANDIDATE_FULL.inv()) == STATUS_INPUT)
                && !(mEngineState.isConvertState()) && (ev.toggleTable != null)
            ) {
                val cursor = mComposingText!!.getCursor(ComposingText.Companion.LAYER1)
                if (cursor > 0) {
                    val prevChar = mComposingText!!.getStrSegment(
                        ComposingText.Companion.LAYER1,
                        cursor - 1
                    )!!.string
                    val c = searchToggleCharacter(prevChar!!, ev.toggleTable!!, true)
                    if (c != null) {
                        mComposingText!!.delete(ComposingText.Companion.LAYER1, false)
                        appendStrSegment(StrSegment(c))
                        updateViewStatusForPrediction(true, true)
                        ret = true
                        break
                    }
                }
            }

            OpenWnnEvent.Companion.REPLACE_CHAR -> {
                val cursor = mComposingText!!.getCursor(ComposingText.Companion.LAYER1)
                if ((cursor > 0)
                    && !(mEngineState.isConvertState())
                ) {
                    val search = mComposingText!!.getStrSegment(
                        ComposingText.Companion.LAYER1,
                        cursor - 1
                    )!!.string
                    val c = ev.replaceTable!![search] as String?
                    if (c != null) {
                        mComposingText!!.delete(1, false)
                        appendStrSegment(StrSegment(c))
                        updateViewStatusForPrediction(true, true)
                        ret = true
                        mStatus = STATUS_INPUT_EDIT
                        break
                    }
                }
            }

            OpenWnnEvent.Companion.INPUT_KEY -> {
                /* update shift/alt state */
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP -> if (mTutorial != null) {
                        return true
                    }

                    KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> {
                        if (keyEvent!!.repeatCount == 0) {
                            if (++mHardAlt > 2) {
                                mHardAlt = 0
                            }
                        }
                        mAltPressing = true
                        updateMetaKeyStateDisplay()
                        return false
                    }

                    KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                        if (keyEvent!!.repeatCount == 0) {
                            if (++mHardShift > 2) {
                                mHardShift = 0
                            }
                        }
                        mShiftPressing = true
                        updateMetaKeyStateDisplay()
                        return false
                    }
                }

                /* handle other key event */
                ret = processKeyEvent(keyEvent!!)
            }

            OpenWnnEvent.Companion.INPUT_SOFT_KEY -> {
                ret = processKeyEvent(keyEvent!!)
                if (!ret) {
                    val code = keyEvent.keyCode
                    if (code == KeyEvent.KEYCODE_ENTER) {
                        sendKeyChar('\n')
                    } else {
                        mInputConnection!!.sendKeyEvent(keyEvent)
                        mInputConnection!!.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
                    }
                    ret = true
                }
            }

            OpenWnnEvent.Companion.SELECT_CANDIDATE -> {
                initCommitInfoForWatchCursor()
                if (isEnglishPrediction) {
                    mComposingText!!.clear()
                }
                mStatus = commitText(ev.word!!)
                if (isEnglishPrediction && !mEngineState.isSymbolList() && mEnableAutoInsertSpace) {
                    commitSpaceJustOne()
                }
                checkCommitInfo()

                if (mEngineState.isSymbolList()) {
                    mEnableAutoDeleteSpace = false
                }
            }

            OpenWnnEvent.Companion.CONVERT -> {
                if (mEngineState.isRenbun()) {
                    if (mCandidatesViewManager is TextCandidatesViewManager) {
                        if (!mCandidatesViewManager.isFocusCandidate) {
                            processDownKeyEvent()
                        }
                        processRightKeyEvent()
                    } else {
                        mCandidatesViewManager!!.processMoveKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT)
                    }
                    break
                }
                startConvert(EngineState.Companion.CONVERT_TYPE_RENBUN)
            }

            OpenWnnEvent.Companion.COMMIT_COMPOSING_TEXT -> commitAllText()
        }

        return ret
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn.onEvaluateFullscreenMode
     */
    override fun onEvaluateFullscreenMode(): Boolean {
        /* never use full-screen mode */
        return false
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn.onEvaluateInputViewShown
     */
    override fun onEvaluateInputViewShown(): Boolean {
        return true
    }

    /**
     * Create a [StrSegment] from a character code.
     * <br></br>
     * @param charCode           A character code
     * @return                  [StrSegment] created; `null` if an error occurs.
     */
    private fun createStrSegment(charCode: Int): StrSegment? {
        if (charCode == 0) {
            return null
        }
        return StrSegment(Character.toChars(charCode))
    }

    /**
     * Key event handler.
     *
     * @param ev        A key event
     * @return  `true` if the event is handled in this method.
     */
    private fun processKeyEvent(ev: KeyEvent): Boolean {
        val key = ev.keyCode

        /* keys which produce a glyph */
        if (ev.isPrintingKey) {
            if (isTenKeyCode(key) && !ev.isNumLockOn) {
                return false
            }
            if (ev.isCtrlPressed) {
                if (key == KeyEvent.KEYCODE_A || key == KeyEvent.KEYCODE_F || key == KeyEvent.KEYCODE_C || key == KeyEvent.KEYCODE_V || key == KeyEvent.KEYCODE_X || key == KeyEvent.KEYCODE_Z) {
                    return if (mComposingText!!.size(ComposingText.Companion.LAYER1) < 1) {
                        false
                    } else {
                        true
                    }
                }
            }

            /* do nothing if the character is not able to display or the character is dead key */
            if ((mHardShift > 0 && mHardAlt > 0) ||
                (ev.isAltPressed && ev.isShiftPressed)
            ) {
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

            commitConvertingText()

            val edit = currentInputEditorInfo
            val str: StrSegment?

            /* get the key character */
            if (mHardShift == 0 && mHardAlt == 0) {
                /* no meta key is locked */
                val shift = if ((mAutoCaps)) getShiftKeyState(edit) else 0
                str =
                    if (shift != mHardShift && (key >= KeyEvent.KEYCODE_A && key <= KeyEvent.KEYCODE_Z)) {
                        /* handling auto caps for a alphabet character */
                        createStrSegment(ev.getUnicodeChar(MetaKeyKeyListener.META_SHIFT_ON))
                    } else {
                        createStrSegment(ev.unicodeChar)
                    }
            } else {
                str = createStrSegment(
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
                if (!ev.isShiftPressed && !ev.isShiftPressed) {
                    updateMetaKeyStateDisplay()
                }
            }

            if (str == null) {
                return true
            }

            /* append the character to the composing text if the character is not TAB */
            if (str.string!![0] != '\u0009') {
                processHardwareKeyboardInputChar(str)
                return true
            } else {
                commitText(true)
                commitText(str.string!!)
                initializeScreen()
                return true
            }
        } else if (key == KeyEvent.KEYCODE_SPACE) {
            /* H/W space key */
            processHardwareKeyboardSpaceKey(ev)
            return true
        } else if (key == KeyEvent.KEYCODE_SYM) {
            /* display the symbol list */
            initCommitInfoForWatchCursor()
            mStatus = commitText(true)
            checkCommitInfo()
            changeEngineMode(ENGINE_MODE_SYMBOL)
            mHardAlt = 0
            updateMetaKeyStateDisplay()
            return true
        }

        /* Functional key */
        if (mComposingText!!.size(ComposingText.Companion.LAYER1) > 0) {
            when (key) {
                KeyEvent.KEYCODE_DEL -> {
                    mStatus = STATUS_INPUT_EDIT
                    if (mEngineState.isConvertState()) {
                        mComposingText!!.setCursor(
                            ComposingText.Companion.LAYER1,
                            mComposingText!!.toString(ComposingText.Companion.LAYER1)!!.length
                        )
                        mExactMatchMode = false
                    } else {
                        if ((mComposingText!!.size(ComposingText.Companion.LAYER1) == 1)
                            && mComposingText!!.getCursor(ComposingText.Companion.LAYER1) != 0
                        ) {
                            initializeScreen()
                            return true
                        } else {
                            mComposingText!!.delete(ComposingText.Companion.LAYER1, false)
                        }
                    }
                    updateViewStatusForPrediction(true, true)
                    return true
                }

                KeyEvent.KEYCODE_BACK -> {
                    if (mCandidatesViewManager.viewType == CandidatesViewManager.Companion.VIEW_TYPE_FULL) {
                        mStatus = mStatus and STATUS_CANDIDATE_FULL.inv()
                        mCandidatesViewManager.viewType =
                            CandidatesViewManager.Companion.VIEW_TYPE_NORMAL
                        mInputViewManager!!.showInputView()
                    } else {
                        if (!mEngineState.isConvertState()) {
                            initializeScreen()
                            if (mConverter != null) {
                                mConverter!!.init()
                            }
                        } else {
                            mCandidatesViewManager!!.clearCandidates()
                            mStatus = STATUS_INPUT_EDIT
                            mExactMatchMode = false
                            mComposingText!!.setCursor(
                                ComposingText.Companion.LAYER1,
                                mComposingText!!.toString(ComposingText.Companion.LAYER1)!!.length
                            )
                            updateViewStatusForPrediction(true, true)
                        }
                    }
                    return true
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> if (!isEnableL2Converter) {
                    commitText(false)
                    return false
                } else {
                    processLeftKeyEvent()
                    return true
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (!isEnableL2Converter) {
                        if (mEngineState.keyboard == EngineState.Companion.KEYBOARD_12KEY) {
                            commitText(false)
                        }
                    } else {
                        processRightKeyEvent()
                    }
                    return true
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    processDownKeyEvent()
                    return true
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (OpenWnn.Companion.isXLarge()) {
                        updateViewStatusForPrediction(true, true)
                    } else {
                        if (mCandidatesViewManager!!.isFocusCandidate) {
                            processUpKeyEvent()
                        }
                    }
                    return true
                }

                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    if (mCandidatesViewManager!!.isFocusCandidate) {
                        mCandidatesViewManager!!.selectFocusCandidate()
                        return true
                    }
                    if (!isEnglishPrediction) {
                        val cursor = mComposingText!!.getCursor(ComposingText.Companion.LAYER1)
                        if (cursor < 1) {
                            return true
                        }
                    }
                    initCommitInfoForWatchCursor()
                    mStatus = commitText(true)
                    checkCommitInfo()

                    if (isEnglishPrediction) {
                        initializeScreen()
                    }

                    if (mEnableAutoHideKeyboard) {
                        mInputViewManager!!.closing()
                        requestHideSelf(0)
                    }
                    return true
                }

                KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP -> return false

                else -> return !isThroughKeyCode(key)
            }
        } else {
            /* if there is no composing string. */
            if (mCandidatesViewManager.currentView.isShown) {
                /* displaying relational prediction candidates */
                when (key) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (mCandidatesViewManager!!.isFocusCandidate) {
                            mCandidatesViewManager!!.processMoveKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT)
                            return true
                        }
                        if (isEnableL2Converter) {
                            /* initialize the converter */
                            mConverter!!.init()
                        }
                        mStatus = STATUS_INPUT_EDIT
                        updateViewStatusForPrediction(true, true)
                        return false
                    }

                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (mCandidatesViewManager!!.isFocusCandidate) {
                            mCandidatesViewManager!!.processMoveKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT)
                            return true
                        }
                        if (isEnableL2Converter) {
                            /* initialize the converter */
                            mConverter!!.init()
                        }
                        mStatus = STATUS_INPUT_EDIT
                        updateViewStatusForPrediction(true, true)
                        return false
                    }

                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        processDownKeyEvent()
                        return true
                    }

                    KeyEvent.KEYCODE_DPAD_UP -> if (mCandidatesViewManager!!.isFocusCandidate) {
                        processUpKeyEvent()
                        return true
                    }

                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> if (mCandidatesViewManager!!.isFocusCandidate) {
                        mCandidatesViewManager!!.selectFocusCandidate()
                        return true
                    }

                    else -> return processKeyEventNoInputCandidateShown(ev)
                }
            } else {
                when (key) {
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
     * Handle the space key event from the Hardware keyboard.
     *
     * @param ev  The space key event
     */
    private fun processHardwareKeyboardSpaceKey(ev: KeyEvent) {
        /* H/W space key */
        if (ev.isShiftPressed) {
            /* change Japanese <-> English mode */
            mHardAlt = 0
            mHardShift = 0
            updateMetaKeyStateDisplay()

            val pref = PreferenceManager.getDefaultSharedPreferences(this)
            if (mEngineState.isEnglish()) {
                /* English mode to Japanese mode */
                (mInputViewManager as DefaultSoftKeyboardJAJP).changeKeyMode(DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA)
                mConverter = mConverterJAJP

                mEnableLearning = pref.getBoolean("opt_enable_learning_ja", true)
                mEnablePrediction = pref.getBoolean("opt_prediction_ja", true)
            } else {
                /* Japanese mode to English mode */
                (mInputViewManager as DefaultSoftKeyboardJAJP).changeKeyMode(DefaultSoftKeyboard.Companion.KEYMODE_JA_HALF_ALPHABET)
                mConverter = mConverterEN

                mEnableLearning = pref.getBoolean("opt_enable_learning_en", true)
                mEnablePrediction = pref.getBoolean("opt_prediction_en", false)
                mEnableSpellCorrection = if (OpenWnn.Companion.isXLarge()) {
                    pref.getBoolean("opt_spell_correction_en", false)
                } else {
                    pref.getBoolean("opt_spell_correction_en", true)
                }
            }
            mCandidatesViewManager!!.clearCandidates()
        } else if (ev.isAltPressed) {
            /* display the symbol list (G1 specific. same as KEYCODE_SYM) */
            if (!mEngineState.isSymbolList()) {
                commitAllText()
            }
            changeEngineMode(ENGINE_MODE_SYMBOL)
            mHardAlt = 0
            updateMetaKeyStateDisplay()
        } else if (isEnglishPrediction) {
            /* Auto commit if English mode */
            if (mComposingText!!.size(0) == 0) {
                commitText(" ")
                mCandidatesViewManager!!.clearCandidates()
                breakSequence()
            } else {
                initCommitInfoForWatchCursor()
                commitText(true)
                commitSpaceJustOne()
                checkCommitInfo()
            }
            mEnableAutoDeleteSpace = false
        } else if (mEngineState.isRenbun()) {
            if (mCandidatesViewManager is TextCandidatesViewManager) {
                if (!mCandidatesViewManager.isFocusCandidate) {
                    processDownKeyEvent()
                }
                processRightKeyEvent()
            } else {
                mCandidatesViewManager!!.processMoveKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT)
            }
        } else {
            /* start consecutive clause conversion if Japanese mode */
            if (mComposingText!!.size(0) == 0) {
                commitText(" ")
                mCandidatesViewManager!!.clearCandidates()
                breakSequence()
            } else {
                startConvert(EngineState.Companion.CONVERT_TYPE_RENBUN)
            }
        }
    }

    /**
     * Handle the character code from the hardware keyboard except the space key.
     *
     * @param str  The input character
     */
    private fun processHardwareKeyboardInputChar(str: StrSegment) {
        if (isEnableL2Converter) {
            var commit = false
            if (mPreConverter == null) {
                val m = mEnglishAutoCommitDelimiter!!.matcher(str.string)
                if (m.matches()) {
                    commitText(true)

                    commit = true
                }
                appendStrSegment(str)
            } else {
                appendStrSegment(str)
                mPreConverter!!.convert(mComposingText!!)
            }

            if (commit) {
                commitText(true)
            } else {
                mStatus = STATUS_INPUT
                updateViewStatusForPrediction(true, true)
            }
        } else {
            appendStrSegment(str)
            var completed = true
            if (mPreConverter != null) {
                completed = mPreConverter!!.convert(mComposingText!!)
            }

            if (completed) {
                if (!mEngineState.isEnglish()) {
                    commitTextWithoutLastAlphabet()
                } else {
                    commitText(false)
                }
            } else {
                updateViewStatus(ComposingText.Companion.LAYER1, false, true)
            }
        }
    }

    /** Thread for updating the candidates view  */
    private fun updatePrediction() {
        var candidates = 0
        val cursor = mComposingText!!.getCursor(ComposingText.Companion.LAYER1)
        if (isEnableL2Converter || mEngineState.isSymbolList()) {
            candidates = if (mExactMatchMode) {
                /* exact matching */
                mConverter!!.predict(mComposingText, 0, cursor)
            } else {
                /* normal prediction */
                mConverter!!.predict(mComposingText, 0, -1)
            }
        }

        /* update the candidates view */
        if (candidates > 0) {
            mHasContinuedPrediction = ((mComposingText!!.size(ComposingText.Companion.LAYER1) == 0)
                    && !mEngineState.isSymbolList())
            mCandidatesViewManager!!.displayCandidates(mConverter)
        } else {
            mCandidatesViewManager!!.clearCandidates()
        }
    }

    /**
     * Handle a left key event.
     */
    private fun processLeftKeyEvent() {
        if (mCandidatesViewManager!!.isFocusCandidate) {
            mCandidatesViewManager!!.processMoveKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT)
            return
        }

        if (mEngineState.isConvertState()) {
            if (mEngineState.isEisuKana()) {
                mExactMatchMode = true
            }

            if (1 < mComposingText!!.getCursor(ComposingText.Companion.LAYER1)) {
                mComposingText!!.moveCursor(ComposingText.Companion.LAYER1, -1)
            }
        } else if (mExactMatchMode) {
            mComposingText!!.moveCursor(ComposingText.Companion.LAYER1, -1)
        } else {
            if (isEnglishPrediction) {
                mComposingText!!.moveCursor(ComposingText.Companion.LAYER1, -1)
            } else {
                mExactMatchMode = true
            }
        }

        mCommitCount = 0 /* retry consecutive clause conversion if necessary. */
        mStatus = STATUS_INPUT_EDIT
        updateViewStatus(mTargetLayer, true, true)
    }

    /**
     * Handle a right key event.
     */
    private fun processRightKeyEvent() {
        if (mCandidatesViewManager!!.isFocusCandidate) {
            mCandidatesViewManager!!.processMoveKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT)
            return
        }

        var layer = mTargetLayer
        val composingText = mComposingText!!
        if (mExactMatchMode || (mEngineState.isConvertState())) {
            val textSize = composingText.size(ComposingText.Companion.LAYER1)
            if (composingText.getCursor(ComposingText.Companion.LAYER1) == textSize) {
                mExactMatchMode = false
                layer = ComposingText.Companion.LAYER1 /* convert -> prediction */
                val state: EngineState = EngineState()
                state.convertType = EngineState.Companion.CONVERT_TYPE_NONE
                updateEngineState(state)
            } else {
                if (mEngineState.isEisuKana()) {
                    mExactMatchMode = true
                }
                composingText.moveCursor(ComposingText.Companion.LAYER1, 1)
            }
        } else {
            if (composingText.getCursor(ComposingText.Companion.LAYER1)
                < composingText.size(ComposingText.Companion.LAYER1)
            ) {
                composingText.moveCursor(ComposingText.Companion.LAYER1, 1)
            }
        }

        mCommitCount = 0 /* retry consecutive clause conversion if necessary. */
        mStatus = STATUS_INPUT_EDIT

        updateViewStatus(layer, true, true)
    }

    /**
     * Handle a down key event.
     */
    private fun processDownKeyEvent() {
        mCandidatesViewManager!!.processMoveKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN)
    }

    /**
     * Handle a up key event.
     */
    private fun processUpKeyEvent() {
        mCandidatesViewManager!!.processMoveKeyEvent(KeyEvent.KEYCODE_DPAD_UP)
    }

    /**
     * Handle a key event which is not right or left key when the
     * composing text is empty and some candidates are shown.
     *
     * @param ev        A key event
     * @return          `true` if this consumes the event; `false` if not.
     */
    fun processKeyEventNoInputCandidateShown(ev: KeyEvent): Boolean {
        var ret = true
        val key = ev.keyCode

        when (key) {
            KeyEvent.KEYCODE_DEL -> ret = true
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_MENU -> ret =
                false

            KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP -> return false

            KeyEvent.KEYCODE_DPAD_CENTER -> ret = true
            KeyEvent.KEYCODE_BACK -> if (mCandidatesViewManager.viewType == CandidatesViewManager.Companion.VIEW_TYPE_FULL) {
                mStatus = mStatus and STATUS_CANDIDATE_FULL.inv()
                mCandidatesViewManager.viewType = CandidatesViewManager.Companion.VIEW_TYPE_NORMAL
                mInputViewManager!!.showInputView()
                return true
            } else {
                ret = true
            }

            else -> return !isThroughKeyCode(key)
        }

        if (mConverter != null) {
            /* initialize the converter */
            mConverter!!.init()
        }
        updateViewStatusForPrediction(true, true)
        return ret
    }

    /**
     * Update views and the display of the composing text for predict mode.
     *
     * @param updateCandidates  `true` to update the candidates view
     * @param updateEmptyText   `false` to update the composing text if it is not empty; `true` to update always.
     */
    private fun updateViewStatusForPrediction(updateCandidates: Boolean, updateEmptyText: Boolean) {
        val state: EngineState = EngineState()
        state.convertType = EngineState.Companion.CONVERT_TYPE_NONE
        updateEngineState(state)

        updateViewStatus(ComposingText.Companion.LAYER1, updateCandidates, updateEmptyText)
    }

    /**
     * Update views and the display of the composing text.
     *
     * @param layer                      Display layer of the composing text
     * @param updateCandidates  `true` to update the candidates view
     * @param updateEmptyText   `false` to update the composing text if it is not empty; `true` to update always.
     */
    private fun updateViewStatus(layer: Int, updateCandidates: Boolean, updateEmptyText: Boolean) {
        mTargetLayer = layer

        if (updateCandidates) {
            updateCandidateView()
        }
        /* notice to the input view */
        mInputViewManager!!.onUpdateState(this)

        /* set the text for displaying as the composing text */
        mDisplayText.clear()
        mDisplayText.insert(0, mComposingText!!.toString(layer))

        /* add decoration to the text */
        val cursor = mComposingText!!.getCursor(layer)
        if ((mInputConnection != null) && (mDisplayText.length != 0 || updateEmptyText)) {
            if (cursor != 0) {
                var highlightEnd = 0

                if ((mExactMatchMode && (!mEngineState.isEisuKana()))
                    || (FIX_CURSOR_TEXT_END && isEnglishPrediction
                            && (cursor < mComposingText!!.size(ComposingText.Companion.LAYER1)))
                ) {
                    mDisplayText.setSpan(
                        SPAN_EXACT_BGCOLOR_HL, 0, cursor,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    highlightEnd = cursor
                } else if (FIX_CURSOR_TEXT_END && mEngineState.isEisuKana()) {
                    mDisplayText.setSpan(
                        SPAN_EISUKANA_BGCOLOR_HL, 0, cursor,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    highlightEnd = cursor
                } else if (layer == ComposingText.Companion.LAYER2) {
                    highlightEnd = mComposingText!!.toString(layer, 0, 0)!!.length

                    /* highlights the first segment */
                    mDisplayText.setSpan(
                        SPAN_CONVERT_BGCOLOR_HL, 0,
                        highlightEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                if (FIX_CURSOR_TEXT_END && (highlightEnd != 0)) {
                    /* highlights remaining text */
                    mDisplayText.setSpan(
                        SPAN_REMAIN_BGCOLOR_HL, highlightEnd,
                        mComposingText!!.toString(layer)!!.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    /* text color in the highlight */
                    mDisplayText.setSpan(
                        SPAN_TEXTCOLOR, 0,
                        mComposingText!!.toString(layer)!!.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            mDisplayText.setSpan(
                SPAN_UNDERLINE, 0, mDisplayText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            var displayCursor = mComposingText!!.toString(layer, 0, cursor - 1)!!.length
            if (FIX_CURSOR_TEXT_END) {
                displayCursor = if ((cursor == 0)) 0 else 1
            }
            /* update the composing text on the EditView */
            if ((mDisplayText.length != 0) || !mHasStartedTextSelection) {
                mInputConnection!!.setComposingText(mDisplayText, displayCursor)
            }
        }
    }

    /**
     * Update the candidates view.
     */
    private fun updateCandidateView() {
        when (mTargetLayer) {
            ComposingText.Companion.LAYER0, ComposingText.Companion.LAYER1 -> if (mEnablePrediction || mEngineState.isSymbolList() || mEngineState.isEisuKana()) {
                /* update the candidates view */
                if ((mComposingText!!.size(ComposingText.Companion.LAYER1) != 0)
                    && !mEngineState.isConvertState()
                ) {
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
                    updatePrediction()
                }
            } else {
                mHandler.removeMessages(MSG_PREDICTION)
                mCandidatesViewManager!!.clearCandidates()
            }

            ComposingText.Companion.LAYER2 -> {
                if (mCommitCount == 0) {
                    mHandler.removeMessages(MSG_PREDICTION)
                    mConverter!!.convert(mComposingText)
                }

                val candidates = mConverter!!.makeCandidateListOf(mCommitCount)

                if (candidates != 0) {
                    mComposingText!!.setCursor(ComposingText.Companion.LAYER2, 1)
                    mCandidatesViewManager!!.displayCandidates(mConverter)
                } else {
                    mComposingText!!.setCursor(
                        ComposingText.Companion.LAYER1,
                        mComposingText!!.toString(ComposingText.Companion.LAYER1)!!.length
                    )
                    mCandidatesViewManager!!.clearCandidates()
                }
            }

            else -> {}
        }
    }

    /**
     * Commit the displaying composing text.
     *
     * @param learn  `true` to register the committed string to the learning dictionary.
     * @return          IME's status after commit
     */
    private fun commitText(learn: Boolean): Int {
        if (isEnglishPrediction) {
            mComposingText!!.setCursor(
                ComposingText.Companion.LAYER1,
                mComposingText!!.size(ComposingText.Companion.LAYER1)
            )
        }

        val layer = mTargetLayer
        val cursor = mComposingText!!.getCursor(layer)
        if (cursor == 0) {
            return mStatus
        }
        val tmp = mComposingText!!.toString(layer, 0, cursor - 1)

        if (mConverter != null) {
            if (learn) {
                if (mEngineState.isRenbun()) {
                    learnWord(0) /* select the top of the clauses */
                } else {
                    if (mComposingText!!.size(ComposingText.Companion.LAYER1) != 0) {
                        val stroke = mComposingText!!.toString(
                            ComposingText.Companion.LAYER1,
                            0,
                            mComposingText!!.getCursor(layer) - 1
                        )
                        val word = WnnWord(tmp, stroke)

                        learnWord(word)
                    }
                }
            } else {
                breakSequence()
            }
        }
        return commitTextThroughInputConnection(tmp!!)
    }

    /**
     * Commit the composing text except the alphabet character at the tail.
     */
    private fun commitTextWithoutLastAlphabet() {
        val layer = mTargetLayer
        val tmp = mComposingText!!.getStrSegment(layer, -1)!!.string

        if (isAlphabetLast(tmp!!)) {
            mComposingText!!.moveCursor(ComposingText.Companion.LAYER1, -1)
            commitText(false)
            mComposingText!!.moveCursor(ComposingText.Companion.LAYER1, 1)
        } else {
            commitText(false)
        }
    }

    /**
     * Commit all uncommitted words.
     */
    private fun commitAllText() {
        initCommitInfoForWatchCursor()
        if (mEngineState.isConvertState()) {
            commitConvertingText()
        } else {
            mComposingText!!.setCursor(
                ComposingText.Companion.LAYER1,
                mComposingText!!.size(ComposingText.Companion.LAYER1)
            )
            mStatus = commitText(true)
        }
        checkCommitInfo()
    }

    /**
     * Commit a word.
     *
     * @param word              A word to commit
     * @return                  IME's status after commit
     */
    private fun commitText(word: WnnWord): Int {
        if (mConverter != null) {
            learnWord(word)
        }
        return commitTextThroughInputConnection(word.candidate!!)
    }

    /**
     * Commit a string.
     *
     * @param str  A string to commit
     */
    private fun commitText(str: String) {
        mInputConnection!!.commitText(str, (if (FIX_CURSOR_TEXT_END) 1 else str.length))
        mPrevCommitText!!.append(str)
        mPrevCommitCount++
        mEnableAutoDeleteSpace = true
        updateViewStatusForPrediction(false, false)
    }

    /**
     * Commit a string through [InputConnection].
     *
     * @param string  A string to commit
     * @return                  IME's status after commit
     */
    private fun commitTextThroughInputConnection(string: String): Int {
        var layer = mTargetLayer

        mInputConnection!!.commitText(string, (if (FIX_CURSOR_TEXT_END) 1 else string.length))
        mPrevCommitText!!.append(string)
        mPrevCommitCount++

        val cursor = mComposingText!!.getCursor(layer)
        if (cursor > 0) {
            mComposingText!!.deleteStrSegment(layer, 0, mComposingText!!.getCursor(layer) - 1)
            mComposingText!!.setCursor(layer, mComposingText!!.size(layer))
        }
        mExactMatchMode = false
        mCommitCount++

        if ((layer == ComposingText.Companion.LAYER2) && (mComposingText!!.size(layer) == 0)) {
            layer = 1 /* for connected prediction */
        }

        val committed = autoCommitEnglish()
        mEnableAutoDeleteSpace = true

        if (layer == ComposingText.Companion.LAYER2) {
            val state: EngineState = EngineState()
            state.convertType = EngineState.Companion.CONVERT_TYPE_RENBUN
            updateEngineState(state)
            updateViewStatus(layer, !committed, false)
        } else {
            updateViewStatusForPrediction(!committed, false)
        }

        return if (mComposingText!!.size(ComposingText.Companion.LAYER0) == 0) {
            STATUS_INIT
        } else {
            STATUS_INPUT_EDIT
        }
    }

    private val isEnglishPrediction: Boolean
        /**
         * Returns whether it is English prediction mode or not.
         *
         * @return  `true` if it is English prediction mode; otherwise, `false`.
         */
        get() = (mEngineState.isEnglish() && isEnableL2Converter)

    /**
     * Change the conversion engine and the letter converter(Romaji-to-Kana converter).
     *
     * @param mode  Engine's mode to be changed
     * @see jp.co.omronsoft.openwnn.OpenWnnEvent.Mode
     *
     * @see jp.co.omronsoft.openwnn.JAJP.DefaultSoftKeyboardJAJP
     */
    private fun changeEngineMode(mode: Int) {
        var state: EngineState = EngineState()

        when (mode) {
            ENGINE_MODE_OPT_TYPE_QWERTY -> {
                state.keyboard = EngineState.Companion.KEYBOARD_QWERTY
                updateEngineState(state)
                clearCommitInfo()
                return
            }

            ENGINE_MODE_OPT_TYPE_12KEY -> {
                state.keyboard = EngineState.Companion.KEYBOARD_12KEY
                updateEngineState(state)
                clearCommitInfo()
                return
            }

            ENGINE_MODE_EISU_KANA -> {
                if (mEngineState.isEisuKana()) {
                    state.temporaryMode = EngineState.Companion.TEMPORARY_DICTIONARY_MODE_NONE
                    updateEngineState(state)
                    updateViewStatusForPrediction(true, true) /* prediction only */
                } else {
                    startConvert(EngineState.Companion.CONVERT_TYPE_EISU_KANA)
                }
                return
            }

            ENGINE_MODE_SYMBOL -> {
                if (mEnableSymbolList && !mDirectInputMode) {
                    state.temporaryMode = EngineState.Companion.TEMPORARY_DICTIONARY_MODE_SYMBOL
                    updateEngineState(state)
                    updateViewStatusForPrediction(true, true)
                }
                return
            }

            ENGINE_MODE_SYMBOL_KAO_MOJI -> {
                changeSymbolEngineState(state, ENGINE_MODE_SYMBOL_KAO_MOJI)
                return
            }

            else -> {}
        }

        state = EngineState()
        state.temporaryMode = EngineState.Companion.TEMPORARY_DICTIONARY_MODE_NONE
        updateEngineState(state)

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        state = EngineState()
        when (mode) {
            OpenWnnEvent.Mode.DIRECT -> {
                /* Full/Half-width number or Full-width alphabet */
                mConverter = null
                mPreConverter = null
            }

            OpenWnnEvent.Mode.NO_LV1_CONV -> {
                /* no Romaji-to-Kana conversion (=English prediction mode) */
                state.dictionarySet = EngineState.Companion.DICTIONARYSET_EN
                updateEngineState(state)
                mConverter = mConverterEN
                mPreConverter = null

                mEnableLearning = pref.getBoolean("opt_enable_learning_en", true)
                mEnablePrediction = pref.getBoolean("opt_prediction_en", false)
                mEnableSpellCorrection = if (OpenWnn.Companion.isXLarge()) {
                    pref.getBoolean("opt_spell_correction_en", false)
                } else {
                    pref.getBoolean("opt_spell_correction_en", true)
                }
            }

            OpenWnnEvent.Mode.NO_LV2_CONV -> {
                mConverter = null
                mPreConverter = mPreConverterHiragana
            }

            ENGINE_MODE_FULL_KATAKANA -> {
                mConverter = null
                mPreConverter = mPreConverterFullKatakana
            }

            ENGINE_MODE_HALF_KATAKANA -> {
                mConverter = null
                mPreConverter = mPreConverterHalfKatakana
            }

            else -> {
                /* HIRAGANA input mode */
                state.dictionarySet = EngineState.Companion.DICTIONARYSET_JP
                updateEngineState(state)
                mConverter = mConverterJAJP
                mPreConverter = mPreConverterHiragana

                mEnableLearning = pref.getBoolean("opt_enable_learning_ja", true)
                mEnablePrediction = pref.getBoolean("opt_prediction_ja", true)
            }
        }

        mPreConverterBack = mPreConverter
        mConverterBack = mConverter
    }

    /**
     * Update the conversion engine's state.
     *
     * @param state  Engine's state to be updated
     */
    private fun updateEngineState(state: EngineState) {
        val myState = mEngineState

        /* language */
        if ((state.dictionarySet != EngineState.Companion.INVALID)
            && (myState.dictionarySet != state.dictionarySet)
        ) {
            when (state.dictionarySet) {
                EngineState.Companion.DICTIONARYSET_EN -> setDictionary(OpenWnnEngineJAJP.Companion.DIC_LANG_EN)
                EngineState.Companion.DICTIONARYSET_JP -> setDictionary(OpenWnnEngineJAJP.Companion.DIC_LANG_JP)
                else -> setDictionary(OpenWnnEngineJAJP.Companion.DIC_LANG_JP)
            }
            myState.dictionarySet = state.dictionarySet
            breakSequence()

            /* update keyboard setting */
            if (state.keyboard == EngineState.Companion.INVALID) {
                state.keyboard = myState.keyboard
            }
        }

        /* type of conversion */
        if ((state.convertType != EngineState.Companion.INVALID)
            && (myState.convertType != state.convertType)
        ) {
            when (state.convertType) {
                EngineState.Companion.CONVERT_TYPE_NONE -> setDictionary(mPrevDictionarySet)
                EngineState.Companion.CONVERT_TYPE_EISU_KANA -> setDictionary(OpenWnnEngineJAJP.Companion.DIC_LANG_JP_EISUKANA)
                EngineState.Companion.CONVERT_TYPE_RENBUN -> setDictionary(OpenWnnEngineJAJP.Companion.DIC_LANG_JP)
                else -> setDictionary(OpenWnnEngineJAJP.Companion.DIC_LANG_JP)
            }
            myState.convertType = state.convertType
        }

        /* temporary dictionary */
        if (state.temporaryMode != EngineState.Companion.INVALID) {
            when (state.temporaryMode) {
                EngineState.Companion.TEMPORARY_DICTIONARY_MODE_NONE -> if (myState.temporaryMode != EngineState.Companion.TEMPORARY_DICTIONARY_MODE_NONE) {
                    setDictionary(mPrevDictionarySet)
                    mCurrentSymbol = -1
                    mPreConverter = mPreConverterBack
                    mConverter = mConverterBack
                    mDisableAutoCommitEnglishMask =
                        mDisableAutoCommitEnglishMask and AUTO_COMMIT_ENGLISH_SYMBOL.inv()
                    (mInputViewManager as DefaultSoftKeyboard).setNormalKeyboard()
                    mTextCandidatesViewManager!!.setSymbolMode(false, ENGINE_MODE_SYMBOL_NONE)
                    if (OpenWnn.Companion.isXLarge()) {
                        mCandidatesViewManager = mTextCandidates1LineViewManager
                        val view = mTextCandidates1LineViewManager.currentView
                        if (view != null) {
                            setCandidatesView(view)
                        }
                    }
                }

                EngineState.Companion.TEMPORARY_DICTIONARY_MODE_SYMBOL -> {
                    if (++mCurrentSymbol >= SYMBOL_LISTS.size) {
                        mCurrentSymbol = 0
                    }
                    if (mEnableSymbolListNonHalf) {
                        mConverterSymbolEngineBack!!.setDictionary(SYMBOL_LISTS[mCurrentSymbol])
                    } else {
                        mConverterSymbolEngineBack!!.setDictionary(SymbolList.Companion.SYMBOL_ENGLISH)
                    }
                    mConverter = mConverterSymbolEngineBack
                    mDisableAutoCommitEnglishMask =
                        mDisableAutoCommitEnglishMask or AUTO_COMMIT_ENGLISH_SYMBOL
                    var engineModeSymbol = 0

                    if (SYMBOL_LISTS[mCurrentSymbol] === SymbolList.Companion.SYMBOL_JAPANESE) {
                        engineModeSymbol = ENGINE_MODE_SYMBOL
                    } else if (SYMBOL_LISTS[mCurrentSymbol] === SymbolList.Companion.SYMBOL_JAPANESE_FACE) {
                        engineModeSymbol = ENGINE_MODE_SYMBOL_KAO_MOJI
                    } else {
                    }

                    mTextCandidatesViewManager!!.setSymbolMode(true, engineModeSymbol)
                    if (OpenWnn.Companion.isXLarge()) {
                        mCandidatesViewManager = mTextCandidatesViewManager
                        val view = mTextCandidatesViewManager.currentView
                        if (view != null) {
                            setCandidatesView(view)
                        }
                    }
                    breakSequence()
                    (mInputViewManager as DefaultSoftKeyboard).setSymbolKeyboard()
                }

                else -> {}
            }
            myState.temporaryMode = state.temporaryMode
        }

        /* preference dictionary */
        if ((state.preferenceDictionary != EngineState.Companion.INVALID)
            && (myState.preferenceDictionary != state.preferenceDictionary)
        ) {
            myState.preferenceDictionary = state.preferenceDictionary
            setDictionary(mPrevDictionarySet)
        }

        /* keyboard type */
        if (state.keyboard != EngineState.Companion.INVALID) {
            when (state.keyboard) {
                EngineState.Companion.KEYBOARD_12KEY -> {
                    mConverterJAJP!!.setKeyboardType(OpenWnnEngineJAJP.Companion.KEYBOARD_KEYPAD12)
                    mConverterEN!!.setDictionary(OpenWnnEngineEN.Companion.DICT_DEFAULT)
                }

                EngineState.Companion.KEYBOARD_QWERTY -> {
                    mConverterJAJP!!.setKeyboardType(OpenWnnEngineJAJP.Companion.KEYBOARD_QWERTY)
                    if (mEnableSpellCorrection) {
                        mConverterEN!!.setDictionary(OpenWnnEngineEN.Companion.DICT_FOR_CORRECT_MISTYPE)
                    } else {
                        mConverterEN!!.setDictionary(OpenWnnEngineEN.Companion.DICT_DEFAULT)
                    }
                }

                else -> {
                    mConverterJAJP!!.setKeyboardType(OpenWnnEngineJAJP.Companion.KEYBOARD_QWERTY)
                    if (mEnableSpellCorrection) {
                        mConverterEN!!.setDictionary(OpenWnnEngineEN.Companion.DICT_FOR_CORRECT_MISTYPE)
                    } else {
                        mConverterEN!!.setDictionary(OpenWnnEngineEN.Companion.DICT_DEFAULT)
                    }
                }
            }
            myState.keyboard = state.keyboard
        }
    }

    /**
     * Set dictionaries to be used.
     *
     * @param mode  Definition of dictionaries
     */
    private fun setDictionary(mode: Int) {
        var target = mode
        when (target) {
            OpenWnnEngineJAJP.Companion.DIC_LANG_JP -> when (mEngineState.preferenceDictionary) {
                EngineState.Companion.PREFERENCE_DICTIONARY_PERSON_NAME -> target =
                    OpenWnnEngineJAJP.Companion.DIC_LANG_JP_PERSON_NAME

                EngineState.Companion.PREFERENCE_DICTIONARY_POSTAL_ADDRESS -> target =
                    OpenWnnEngineJAJP.Companion.DIC_LANG_JP_POSTAL_ADDRESS

                else -> {}
            }

            OpenWnnEngineJAJP.Companion.DIC_LANG_EN -> when (mEngineState.preferenceDictionary) {
                EngineState.Companion.PREFERENCE_DICTIONARY_EMAIL_ADDRESS_URI -> target =
                    OpenWnnEngineJAJP.Companion.DIC_LANG_EN_EMAIL_ADDRESS

                else -> {}
            }

            else -> {}
        }

        when (mode) {
            OpenWnnEngineJAJP.Companion.DIC_LANG_JP, OpenWnnEngineJAJP.Companion.DIC_LANG_EN -> mPrevDictionarySet =
                mode

            else -> {}
        }

        mConverterJAJP!!.setDictionary(target)
    }

    /**
     * Handle a toggle key input event.
     *
     * @param table  Table of toggle characters
     */
    private fun processSoftKeyboardToggleChar(table: Array<String?>?) {
        if (table == null) {
            return
        }

        commitConvertingText()

        var toggled = false
        if ((mStatus and STATUS_CANDIDATE_FULL.inv()) == STATUS_INPUT) {
            val cursor = mComposingText!!.getCursor(ComposingText.Companion.LAYER1)
            if (cursor > 0) {
                val prevChar = mComposingText!!.getStrSegment(
                    ComposingText.Companion.LAYER1,
                    cursor - 1
                )!!.string
                val c = searchToggleCharacter(prevChar!!, table, false)
                if (c != null) {
                    mComposingText!!.delete(ComposingText.Companion.LAYER1, false)
                    appendStrSegment(StrSegment(c))
                    toggled = true
                }
            }
        }

        if (!toggled) {
            if (!isEnableL2Converter) {
                commitText(false)
            }

            var str = table[0]
            /* shift on */
            if (mAutoCaps && (getShiftKeyState(currentInputEditorInfo) == 1)) {
                val top = table[0]!![0]
                if (Character.isLowerCase(top)) {
                    str = top.uppercaseChar().toString()
                }
            }
            appendStrSegment(StrSegment(str))
        }

        mStatus = STATUS_INPUT

        updateViewStatusForPrediction(true, true)
    }

    /**
     * Handle character input from the software keyboard without listing candidates.
     *
     * @param chars  The input character(s)
     */
    private fun processSoftKeyboardCodeWithoutConversion(chars: CharArray?) {
        if (chars == null) {
            return
        }

        val text = mComposingText!!
        appendStrSegment(StrSegment(chars))

        if (!isAlphabetLast(text.toString(ComposingText.Companion.LAYER1)!!)) {
            /* commit if the input character is not alphabet */
            commitText(false)
        } else {
            val completed = mPreConverter!!.convert(text)
            if (completed) {
                commitTextWithoutLastAlphabet()
            } else {
                mStatus = STATUS_INPUT
                updateViewStatusForPrediction(true, true)
            }
        }
    }

    /**
     * Handle character input from the software keyboard.
     *
     * @param chars   The input character(s)
     */
    private fun processSoftKeyboardCode(chars: CharArray?) {
        if (chars == null) {
            return
        }

        if ((chars[0] == ' ') || (chars[0] == '\u3000' /* Full-width space */)) {
            if (mComposingText!!.size(0) == 0) {
                mCandidatesViewManager!!.clearCandidates()
                commitText(String(chars))
                breakSequence()
            } else {
                if (isEnglishPrediction) {
                    initCommitInfoForWatchCursor()
                    commitText(true)
                    commitSpaceJustOne()
                    checkCommitInfo()
                } else {
                    if (mEngineState.isRenbun()) {
                        if (mCandidatesViewManager is TextCandidatesViewManager) {
                            if (!mCandidatesViewManager.isFocusCandidate) {
                                processDownKeyEvent()
                            }
                            processRightKeyEvent()
                        } else {
                            mCandidatesViewManager!!.processMoveKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT)
                        }
                    } else {
                        startConvert(EngineState.Companion.CONVERT_TYPE_RENBUN)
                    }
                }
            }
            mEnableAutoDeleteSpace = false
        } else {
            commitConvertingText()

            /* Auto-commit a word if it is English and Qwerty mode */
            var commit = false
            if (isEnglishPrediction
                && (mEngineState.keyboard == EngineState.Companion.KEYBOARD_QWERTY)
            ) {
                val m = mEnglishAutoCommitDelimiter!!.matcher(String(chars))
                if (m.matches()) {
                    commit = true
                }
            }

            if (commit) {
                commitText(true)

                appendStrSegment(StrSegment(chars))
                commitText(true)
            } else {
                appendStrSegment(StrSegment(chars))
                if (mPreConverter != null) {
                    mPreConverter!!.convert(mComposingText!!)
                    mStatus = STATUS_INPUT
                }
                updateViewStatusForPrediction(true, true)
            }
        }
    }

    /**
     * Start consecutive clause conversion or EISU-KANA conversion mode.
     *
     * @param convertType               The conversion type(`EngineState.CONVERT_TYPE_*`)
     */
    private fun startConvert(convertType: Int) {
        if (!isEnableL2Converter) {
            return
        }

        if (mEngineState.convertType != convertType) {
            /* adjust the cursor position */
            if (!mExactMatchMode) {
                if (convertType == EngineState.Companion.CONVERT_TYPE_RENBUN) {
                    /* not specify */
                    mComposingText!!.setCursor(ComposingText.Companion.LAYER1, 0)
                } else {
                    if (mEngineState.isRenbun()) {
                        /* EISU-KANA conversion specifying the position of the segment if previous mode is conversion mode */
                        mExactMatchMode = true
                    } else {
                        /* specify all range */
                        mComposingText!!.setCursor(
                            ComposingText.Companion.LAYER1,
                            mComposingText!!.size(ComposingText.Companion.LAYER1)
                        )
                    }
                }
            }

            if (convertType == EngineState.Companion.CONVERT_TYPE_RENBUN) {
                /* clears variables for the prediction */
                mExactMatchMode = false
            }
            /* clears variables for the convert */
            mCommitCount = 0
            val layer: Int = if (convertType == EngineState.Companion.CONVERT_TYPE_EISU_KANA) {
                ComposingText.Companion.LAYER1
            } else {
                ComposingText.Companion.LAYER2
            }

            val state: EngineState = EngineState()
            state.convertType = convertType
            updateEngineState(state)

            updateViewStatus(layer, true, true)
        }
    }

    /**
     * Auto commit a word in English (on half-width alphabet mode).
     *
     * @return  `true` if auto-committed; otherwise, `false`.
     */
    private fun autoCommitEnglish(): Boolean {
        if (isEnglishPrediction && (mDisableAutoCommitEnglishMask == AUTO_COMMIT_ENGLISH_ON)) {
            val seq = mInputConnection!!.getTextBeforeCursor(2, 0)
            val m = mEnglishAutoCommitDelimiter!!.matcher(seq)
            if (m.matches()) {
                if ((seq!![0] == ' ') && mEnableAutoDeleteSpace) {
                    mInputConnection!!.deleteSurroundingText(2, 0)
                    val str = seq.subSequence(1, 2)
                    mInputConnection!!.commitText(str, 1)
                    mPrevCommitText!!.append(str)
                    mPrevCommitCount++
                }

                mHandler.removeMessages(MSG_PREDICTION)
                mCandidatesViewManager!!.clearCandidates()
                return true
            }
        }
        return false
    }

    /**
     * Insert a white space if the previous character is not a white space.
     */
    private fun commitSpaceJustOne() {
        val seq = mInputConnection!!.getTextBeforeCursor(1, 0)
        if (seq!![0] != ' ') {
            commitText(" ")
        }
    }

    /**
     * Get the shift key state from the editor.
     *
     * @param editor    The editor
     * @return          State ID of the shift key (0:off, 1:on)
     */
    protected fun getShiftKeyState(editor: EditorInfo): Int {
        return if ((currentInputConnection.getCursorCapsMode(editor.inputType) == 0)) 0 else 1
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
     * Memory a selected word.
     *
     * @param word  A selected word
     */
    private fun learnWord(word: WnnWord?) {
        if (mEnableLearning && word != null) {
            mConverter!!.learn(word)
        }
    }

    /**
     * Memory a clause which is generated by consecutive clause conversion.
     *
     * @param index  Index of a clause
     */
    private fun learnWord(index: Int) {
        val composingText = mComposingText!!

        if (mEnableLearning && composingText.size(ComposingText.Companion.LAYER2) > index) {
            val seg = composingText.getStrSegment(ComposingText.Companion.LAYER2, index)
            if (seg is StrSegmentClause) {
                mConverter!!.learn(seg.clause)
            } else {
                val stroke =
                    composingText.toString(ComposingText.Companion.LAYER1, seg!!.from, seg.to)
                mConverter!!.learn(WnnWord(seg.string, stroke))
            }
        }
    }

    /**
     * Fits an editor info.
     *
     * @param preferences  The preference data.
     * @param info              The editor info.
     */
    private fun fitInputType(preference: SharedPreferences, info: EditorInfo) {
        if (info.inputType == EditorInfo.TYPE_NULL) {
            mDirectInputMode = true
            return
        }

        if (mConverter === mConverterEN) {
            mEnableLearning = preference.getBoolean("opt_enable_learning_en", true)
            mEnablePrediction = preference.getBoolean("opt_prediction_en", false)
            mEnableSpellCorrection = if (OpenWnn.Companion.isXLarge()) {
                preference.getBoolean("opt_spell_correction_en", false)
            } else {
                preference.getBoolean("opt_spell_correction_en", true)
            }
        } else {
            mEnableLearning = preference.getBoolean("opt_enable_learning_ja", true)
            mEnablePrediction = preference.getBoolean("opt_prediction_ja", true)
        }
        mDisableAutoCommitEnglishMask =
            mDisableAutoCommitEnglishMask and AUTO_COMMIT_ENGLISH_OFF.inv()
        var preferenceDictionary = EngineState.Companion.PREFERENCE_DICTIONARY_NONE
        mEnableConverter = true
        mEnableSymbolList = true
        mEnableSymbolListNonHalf = true
        setEnabledTabs(true)
        mAutoCaps = preference.getBoolean("auto_caps", true)
        mFilter.filter = 0
        mEnableAutoInsertSpace = true
        mEnableAutoHideKeyboard = false

        when (info.inputType and EditorInfo.TYPE_MASK_CLASS) {
            EditorInfo.TYPE_CLASS_NUMBER, EditorInfo.TYPE_CLASS_DATETIME -> mEnableConverter = false
            EditorInfo.TYPE_CLASS_PHONE -> {
                mEnableSymbolList = false
                mEnableConverter = false
            }

            EditorInfo.TYPE_CLASS_TEXT -> when (info.inputType and EditorInfo.TYPE_MASK_VARIATION) {
                EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME -> preferenceDictionary =
                    EngineState.Companion.PREFERENCE_DICTIONARY_PERSON_NAME

                EditorInfo.TYPE_TEXT_VARIATION_PASSWORD, EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> {
                    mEnableLearning = false
                    mEnableConverter = false
                    mEnableSymbolListNonHalf = false
                    mFilter.filter = CandidateFilter.Companion.FILTER_NON_ASCII
                    mDisableAutoCommitEnglishMask =
                        mDisableAutoCommitEnglishMask or AUTO_COMMIT_ENGLISH_OFF
                    mTextCandidatesViewManager!!.setEnableEmoticon(false)
                }

                EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> {
                    mEnableAutoInsertSpace = false
                    mDisableAutoCommitEnglishMask =
                        mDisableAutoCommitEnglishMask or AUTO_COMMIT_ENGLISH_OFF
                    preferenceDictionary =
                        EngineState.Companion.PREFERENCE_DICTIONARY_EMAIL_ADDRESS_URI
                }

                EditorInfo.TYPE_TEXT_VARIATION_URI -> {
                    mEnableAutoInsertSpace = false
                    mDisableAutoCommitEnglishMask =
                        mDisableAutoCommitEnglishMask or AUTO_COMMIT_ENGLISH_OFF
                    preferenceDictionary =
                        EngineState.Companion.PREFERENCE_DICTIONARY_EMAIL_ADDRESS_URI
                }

                EditorInfo.TYPE_TEXT_VARIATION_POSTAL_ADDRESS -> preferenceDictionary =
                    EngineState.Companion.PREFERENCE_DICTIONARY_POSTAL_ADDRESS

                EditorInfo.TYPE_TEXT_VARIATION_PHONETIC -> {
                    mEnableLearning = false
                    mEnableConverter = false
                    mEnableSymbolList = false
                }

                else -> {}
            }

            else -> {}
        }

        if (mFilter.filter == 0) {
            mConverterEN!!.setFilter(null)
            mConverterJAJP!!.setFilter(null)
        } else {
            mConverterEN!!.setFilter(mFilter)
            mConverterJAJP!!.setFilter(mFilter)
        }

        val state: EngineState = EngineState()
        state.preferenceDictionary = preferenceDictionary
        state.convertType = EngineState.Companion.CONVERT_TYPE_NONE
        state.keyboard = mEngineState.keyboard
        updateEngineState(state)
        updateMetaKeyStateDisplay()

        if (!OpenWnn.Companion.isXLarge()) {
            checkTutorial(info.privateImeOptions)
        }
    }

    /**
     * Append a [StrSegment] to the composing text
     * <br></br>
     * If the length of the composing text exceeds
     * `LIMIT_INPUT_NUMBER`, the appending operation is ignored.
     *
     * @param  str  Input segment
     */
    private fun appendStrSegment(str: StrSegment) {
        val composingText = mComposingText!!

        if (composingText.size(ComposingText.Companion.LAYER1) >= LIMIT_INPUT_NUMBER) {
            return  /* do nothing */
        }
        composingText.insertStrSegment(
            ComposingText.Companion.LAYER0,
            ComposingText.Companion.LAYER1,
            str
        )
        return
    }

    /**
     * Commit the consecutive clause conversion.
     */
    private fun commitConvertingText() {
        if (mEngineState.isConvertState()) {
            val size = mComposingText!!.size(ComposingText.Companion.LAYER2)
            for (i in 0 until size) {
                learnWord(i)
            }

            val text = mComposingText!!.toString(ComposingText.Companion.LAYER2)
            mInputConnection!!.commitText(text, (if (FIX_CURSOR_TEXT_END) 1 else text!!.length))
            mPrevCommitText!!.append(text)
            mPrevCommitCount++
            initializeScreen()
        }
    }

    /**
     * Initialize the screen displayed by IME
     */
    private fun initializeScreen() {
        if (mComposingText!!.size(ComposingText.Companion.LAYER0) != 0) {
            mInputConnection!!.setComposingText("", 0)
        }
        mComposingText!!.clear()
        mExactMatchMode = false
        mStatus = STATUS_INIT
        mHandler.removeMessages(MSG_PREDICTION)
        val candidateView = mCandidatesViewManager.currentView
        if ((candidateView != null) && candidateView.isShown) {
            mCandidatesViewManager!!.clearCandidates()
        }
        mInputViewManager!!.onUpdateState(this)

        val state: EngineState = EngineState()
        state.temporaryMode = EngineState.Companion.TEMPORARY_DICTIONARY_MODE_NONE
        updateEngineState(state)
    }

    /**
     * Whether the tail of the string is alphabet or not.
     *
     * @param  str      The string
     * @return          `true` if the tail is alphabet; `false` if otherwise.
     */
    private fun isAlphabetLast(str: String): Boolean {
        val m = ENGLISH_CHARACTER_LAST.matcher(str)
        return m.matches()
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn.onFinishInput
     */
    override fun onFinishInput() {
        if (mInputConnection != null) {
            initializeScreen()
        }
        super.onFinishInput()
    }

    private val isEnableL2Converter: Boolean
        /**
         * Check whether or not the converter is active.
         *
         * @return `true` if the converter is active.
         */
        get() {
            if (mConverter == null || !mEnableConverter) {
                return false
            }

            if (mEngineState.isEnglish() && !mEnablePrediction) {
                return false
            }

            return true
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
        if (mEnableHardware12Keyboard && !mDirectInputMode) {
            if (isHardKeyboard12KeyLongPress(key)
                && ((ev.flags and KeyEvent.FLAG_CANCELED_LONG_PRESS) == 0)
            ) {
                when (key) {
                    KeyEvent.KEYCODE_SOFT_LEFT -> if (mEngineState.isSymbolList()) {
                        switchSymbolList()
                    } else if ((mComposingText!!.size(0) != 0) && !mEngineState.isRenbun()
                        && ((mInputViewManager as DefaultSoftKeyboardJAJP).keyMode
                                == DefaultSoftKeyboard.Companion.KEYMODE_JA_FULL_HIRAGANA)
                    ) {
                        startConvert(EngineState.Companion.CONVERT_TYPE_RENBUN)
                    } else {
                        (mInputViewManager as DefaultSoftKeyboard).onKey(
                            DefaultSoftKeyboard.Companion.KEYCODE_JP12_EMOJI, null
                        )
                    }

                    KeyEvent.KEYCODE_SOFT_RIGHT -> (mInputViewManager as DefaultSoftKeyboardJAJP).showInputModeSwitchDialog()
                    KeyEvent.KEYCODE_DEL -> {
                        var newKeyCode = KeyEvent.KEYCODE_FORWARD_DEL
                        val composingTextSize =
                            mComposingText!!.size(ComposingText.Companion.LAYER1)
                        if (composingTextSize > 0) {
                            if (mComposingText!!.getCursor(ComposingText.Companion.LAYER1) > (composingTextSize - 1)) {
                                newKeyCode = KeyEvent.KEYCODE_DEL
                            }
                            val keyEvent = KeyEvent(ev.action, newKeyCode)
                            if (!processKeyEvent(keyEvent)) {
                                sendDownUpKeyEvents(keyEvent.keyCode)
                            }
                        } else {
                            if (mInputConnection != null) {
                                val text = mInputConnection!!.getTextAfterCursor(1, 0)
                                if ((text == null) || (text.length == 0)) {
                                    newKeyCode = KeyEvent.KEYCODE_DEL
                                }
                            }
                            sendDownUpKeyEvents(newKeyCode)
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    /**
     * Handling KeyEvent(KEYLONGPRESS)
     * <br></br>
     * This method is called from [.handleEvent].
     *
     * @param ev   An long press key event
     * @return    `true` if the event is processed in this method; `false` if not.
     */
    private fun onKeyLongPressEvent(ev: KeyEvent?): Boolean {
        if (mEnableHardware12Keyboard) {
            var keyCode = 0
            if (ev != null) {
                keyCode = ev.keyCode
            }
            when (keyCode) {
                KeyEvent.KEYCODE_DEL -> {
                    initializeScreen()
                    if (mInputConnection != null) {
                        mInputConnection!!.deleteSurroundingText(Int.MAX_VALUE, Int.MAX_VALUE)
                    }
                    return true
                }

                else -> {}
            }
        }
        return false
    }

    /**
     * Initialize the committed text's information.
     */
    private fun initCommitInfoForWatchCursor() {
        if (!isEnableL2Converter) {
            return
        }

        mCommitStartCursor = mComposingStartCursor
        mPrevCommitText!!.delete(0, mPrevCommitText.length)
    }

    /**
     * Clear the commit text's info.
     * @return `true`:cleared, `false`:has already cleared.
     */
    private fun clearCommitInfo(): Boolean {
        if (mCommitStartCursor < 0) {
            return false
        }

        mCommitStartCursor = -1
        return true
    }

    /**
     * Verify the commit text.
     */
    private fun checkCommitInfo() {
        if (mCommitStartCursor < 0) {
            return
        }

        val composingLength = mComposingText!!.toString(mTargetLayer)!!.length
        var seq =
            mInputConnection!!.getTextBeforeCursor(mPrevCommitText!!.length + composingLength, 0)
        seq = seq!!.subSequence(0, seq.length - composingLength)
        if (seq != mPrevCommitText.toString()) {
            mPrevCommitCount = 0
            clearCommitInfo()
        }
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
        val manager = mInputViewManager as DefaultSoftKeyboardJAJP
        manager.setDefaultKeyboard()
        if (mEngineState.keyboard == EngineState.Companion.KEYBOARD_QWERTY) {
            manager.changeKeyboardType(DefaultSoftKeyboard.Companion.KEYBOARD_12KEY)
        }

        val inputManager = (mInputViewManager as DefaultSoftKeyboardJAJP)
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

    /**
     * Break the sequence of words.
     */
    private fun breakSequence() {
        mEnableAutoDeleteSpace = false
        mConverterJAJP!!.breakSequence()
        mConverterEN!!.breakSequence()
    }

    /**
     * Switch symbol list.
     */
    private fun switchSymbolList() {
        changeSymbolEngineState(EngineState(), ENGINE_MODE_SYMBOL)
        mHardAlt = 0
        updateMetaKeyStateDisplay()
    }

    /**
     * Change symbol engine state.
     *
     * @param  state  Engine state
     * @param  mode   Engine mode
     */
    private fun changeSymbolEngineState(state: EngineState, mode: Int) {
        state.temporaryMode = EngineState.Companion.TEMPORARY_DICTIONARY_MODE_SYMBOL
        updateEngineState(state)
    }

    /**
     * Set enable tabs.
     *
     * @param enableEmoticon `true`  - Emoticon is enabled.
     * `false` - Emoticon is disabled.
     */
    private fun setEnabledTabs(enableEmoticon: Boolean) {
        mTextCandidatesViewManager!!.setEnableEmoticon(enableEmoticon)
    }

    /**
     * Is enable hard keyboard 12Key long press keycode.
     *
     * @param  keyCode  keycode.
     * @return  `true` if enable long press keycode; `false` if not.
     */
    private fun isHardKeyboard12KeyLongPress(keyCode: Int): Boolean {
        var isLongPress = false
        when (keyCode) {
            KeyEvent.KEYCODE_SOFT_LEFT, KeyEvent.KEYCODE_SOFT_RIGHT, KeyEvent.KEYCODE_DEL -> isLongPress =
                true

            else -> {}
        }
        return isLongPress
    }

    /**
     * Key event handler for hardware 12Keyboard.
     *
     * @param keyEvent A key event
     * @return  `true` if the event is handled in this method.
     */
    private fun processHardware12Keyboard(keyEvent: KeyEvent?): Boolean {
        var ret = false
        if (mEnableHardware12Keyboard && (keyEvent != null)) {
            val keyCode = keyEvent.keyCode

            if (isHardKeyboard12KeyLongPress(keyCode)) {
                if (keyEvent.repeatCount == 0) {
                    keyEvent.startTracking()
                }
                ret = true
            } else {
                val code = HW12KEYBOARD_KEYCODE_REPLACE_TABLE[keyCode]
                if (code != null) {
                    if (keyEvent.repeatCount == 0) {
                        (mInputViewManager as DefaultSoftKeyboard).onKey(code, null)
                    }
                    ret = true
                }
            }
        }
        return ret
    }

    companion object {
        /**
         * Mode of the convert engine (Full-width KATAKANA).
         * Use with `OpenWnn.CHANGE_MODE` event.
         */
        const val ENGINE_MODE_FULL_KATAKANA: Int = 101

        /**
         * Mode of the convert engine (Half-width KATAKANA).
         * Use with `OpenWnn.CHANGE_MODE` event.
         */
        const val ENGINE_MODE_HALF_KATAKANA: Int = 102

        /**
         * Mode of the convert engine (EISU-KANA conversion).
         * Use with `OpenWnn.CHANGE_MODE` event.
         */
        const val ENGINE_MODE_EISU_KANA: Int = 103

        /**
         * Mode of the convert engine (Symbol list).
         * Use with `OpenWnn.CHANGE_MODE` event.
         */
        const val ENGINE_MODE_SYMBOL_NONE: Int = 1040
        const val ENGINE_MODE_SYMBOL: Int = 1041
        const val ENGINE_MODE_SYMBOL_KAO_MOJI: Int = 1042

        /**
         * Mode of the convert engine (Keyboard type is QWERTY).
         * Use with `OpenWnn.CHANGE_MODE` event to change ambiguous searching pattern.
         */
        const val ENGINE_MODE_OPT_TYPE_QWERTY: Int = 105

        /**
         * Mode of the convert engine (Keyboard type is 12-keys).
         * Use with `OpenWnn.CHANGE_MODE` event to change ambiguous searching pattern.
         */
        const val ENGINE_MODE_OPT_TYPE_12KEY: Int = 106

        /** Never move cursor in to the composing text (adapting to IMF's specification change)  */
        private const val FIX_CURSOR_TEXT_END = true

        /** Highlight color style for the converted clause  */
        private val SPAN_CONVERT_BGCOLOR_HL: CharacterStyle = BackgroundColorSpan(-0x777701)

        /** Highlight color style for the selected string   */
        private val SPAN_EXACT_BGCOLOR_HL: CharacterStyle = BackgroundColorSpan(-0x993256)

        /** Highlight color style for EISU-KANA conversion  */
        private val SPAN_EISUKANA_BGCOLOR_HL: CharacterStyle = BackgroundColorSpan(-0x604933)

        /** Highlight color style for the composing text  */
        private val SPAN_REMAIN_BGCOLOR_HL: CharacterStyle = BackgroundColorSpan(-0xf0001)

        /** Highlight text color  */
        private val SPAN_TEXTCOLOR: CharacterStyle = ForegroundColorSpan(-0x1000000)

        /** Underline style for the composing text  */
        private val SPAN_UNDERLINE: CharacterStyle = UnderlineSpan()

        /** IME's status for `mStatus` input/no candidates).  */
        private const val STATUS_INIT = 0x0000

        /** IME's status for `mStatus`(input characters).  */
        private const val STATUS_INPUT = 0x0001

        /** IME's status for `mStatus`(input functional keys).  */
        private const val STATUS_INPUT_EDIT = 0x0003

        /** IME's status for `mStatus`(all candidates are displayed).  */
        private const val STATUS_CANDIDATE_FULL = 0x0010

        /** Alphabet-last pattern  */
        private val ENGLISH_CHARACTER_LAST: Pattern = Pattern.compile(".*[a-zA-Z]$")

        /**
         * Private area character code got by [KeyEvent.getUnicodeChar].
         * (SHIFT+ALT+X G1 specific)
         */
        private const val PRIVATE_AREA_CODE = 61184

        /** Maximum length of input string  */
        private const val LIMIT_INPUT_NUMBER = 30

        /** Bit flag for English auto commit mode (ON)  */
        private const val AUTO_COMMIT_ENGLISH_ON = 0x0000

        /** Bit flag for English auto commit mode (OFF)  */
        private const val AUTO_COMMIT_ENGLISH_OFF = 0x0001

        /** Bit flag for English auto commit mode (symbol list)  */
        private const val AUTO_COMMIT_ENGLISH_SYMBOL = 0x0010

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

        /** H/W 12Keyboard keycode replace table  */
        private val HW12KEYBOARD_KEYCODE_REPLACE_TABLE
                : HashMap<Int, Int> = object : HashMap<Int?, Int?>() {
            init {
                put(KeyEvent.KEYCODE_0, DefaultSoftKeyboard.Companion.KEYCODE_JP12_0)
                put(KeyEvent.KEYCODE_1, DefaultSoftKeyboard.Companion.KEYCODE_JP12_1)
                put(KeyEvent.KEYCODE_2, DefaultSoftKeyboard.Companion.KEYCODE_JP12_2)
                put(KeyEvent.KEYCODE_3, DefaultSoftKeyboard.Companion.KEYCODE_JP12_3)
                put(KeyEvent.KEYCODE_4, DefaultSoftKeyboard.Companion.KEYCODE_JP12_4)
                put(KeyEvent.KEYCODE_5, DefaultSoftKeyboard.Companion.KEYCODE_JP12_5)
                put(KeyEvent.KEYCODE_6, DefaultSoftKeyboard.Companion.KEYCODE_JP12_6)
                put(KeyEvent.KEYCODE_7, DefaultSoftKeyboard.Companion.KEYCODE_JP12_7)
                put(KeyEvent.KEYCODE_8, DefaultSoftKeyboard.Companion.KEYCODE_JP12_8)
                put(KeyEvent.KEYCODE_9, DefaultSoftKeyboard.Companion.KEYCODE_JP12_9)
                put(KeyEvent.KEYCODE_POUND, DefaultSoftKeyboard.Companion.KEYCODE_JP12_SHARP)
                put(KeyEvent.KEYCODE_STAR, DefaultSoftKeyboard.Companion.KEYCODE_JP12_ASTER)
                put(KeyEvent.KEYCODE_CALL, DefaultSoftKeyboard.Companion.KEYCODE_JP12_REVERSE)
            }
        }


        /**
         * Get the instance of this service.
         * <br></br>
         * Before using this method, the constructor of this service must be invoked.
         *
         * @return      The instance of this service
         */
        /** Instance of this service  */
        var instance: OpenWnnJAJP? = null
            private set

        /** Symbol lists to display when the symbol key is pressed  */
        private val SYMBOL_LISTS = arrayOf<String>(
            SymbolList.Companion.SYMBOL_JAPANESE, SymbolList.Companion.SYMBOL_JAPANESE_FACE
        )

        /** Shift lock toggle definition  */
        private val mShiftKeyToggle =
            intArrayOf(0, MetaKeyKeyListener.META_SHIFT_ON, MetaKeyKeyListener.META_CAP_LOCKED)

        /** ALT lock toggle definition  */
        private val mAltKeyToggle =
            intArrayOf(0, MetaKeyKeyListener.META_ALT_ON, MetaKeyKeyListener.META_ALT_LOCKED)
    }
}
