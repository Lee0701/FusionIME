package ee.oyatl.ime.fusion

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import com.android.inputmethod.pinyin.CandidatesContainer
import com.android.inputmethod.pinyin.ComposingView
import com.android.inputmethod.pinyin.ComposingView.ComposingStatus
import com.android.inputmethod.pinyin.IPinyinDecoderService
import com.android.inputmethod.pinyin.IPinyinDecoderService.Stub
import com.android.inputmethod.pinyin.KeyMapDream
import com.android.inputmethod.pinyin.PinyinIME.ImeState
import com.android.inputmethod.pinyin.Settings
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.candidate.ScrollingCandidateView
import ee.oyatl.ime.keyboard.CommonKeyboardListener
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.keyboardset.BottomRowKeyboardSet
import ee.oyatl.ime.keyboard.keyboardset.DefaultKeyboardSet
import ee.oyatl.ime.keyboard.keyboardset.KeyboardSet
import ee.oyatl.ime.keyboard.keyboardset.StackedKeyboardSet
import ee.oyatl.ime.keyboard.layout.LayoutQwerty
import java.util.Vector

class PinyinIMEMode(
    context: Context
): IMEMode, CandidateView.Listener, CommonKeyboardListener.Callback {
    private val keyCharacterMap: KeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

    /**
     * Connection used to bind the decoding service.
     */
    private var mPinyinDecoderServiceConnection: PinyinDecoderServiceConnection? = null

    /**
     * The current IME status.
     *
     * @see ImeState
     */
    private var mImeState = ImeState.STATE_IDLE

    /**
     * The decoding information, include spelling(Pinyin) string, decoding
     * result, etc.
     */
    private val mDecInfo: DecodingInfo = DecodingInfo()

    /**
     * The floating container which contains the composing view. If necessary,
     * some other view like candiates container can also be put here.
     */
    private lateinit var mFloatingContainer: LinearLayout

    /**
     * View to show the composing string.
     */
    private lateinit var mComposingView: ComposingView

    /**
     * View to show candidates list.
     */
    private lateinit var mCandidatesContainer: CandidatesContainer

    private var isEnterNormalState = true

    private val keyboardListener = KeyboardListener()
    private val keyboardSet: KeyboardSet = StackedKeyboardSet(
        DefaultKeyboardSet(keyboardListener, LayoutQwerty.ROWS_ROMAJI_LOWER, LayoutQwerty.ROWS_ROMAJI_UPPER),
        BottomRowKeyboardSet(keyboardListener)
    )
    private lateinit var candidateView: CandidateView
    private lateinit var imeView: ViewGroup

    private var currentInputConnection: InputConnection? = null
    private var currentInputEditorInfo: EditorInfo? = null

    init {
        startPinyinDecoderService(context)
    }

    override fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo) {
        this.currentInputConnection = inputConnection
        this.currentInputEditorInfo = editorInfo
    }

    override fun onFinish(inputConnection: InputConnection, editorInfo: EditorInfo) {
        this.currentInputConnection = null
        this.currentInputEditorInfo = null
    }

    private fun resetCandidateWindow() {
        mDecInfo.resetCandidates()
        candidateView.submitList(emptyList())
    }

    override fun initView(context: Context): View {
        val layoutInflater = LayoutInflater.from(context)

        // Inflate the floating container view
        mFloatingContainer = layoutInflater.inflate(
            com.android.inputmethod.pinyin.R.layout.floating_container, null
        ) as LinearLayout

        // The first child is the composing view.
        mComposingView = mFloatingContainer.getChildAt(0) as ComposingView

        mCandidatesContainer = layoutInflater.inflate(
            com.android.inputmethod.pinyin.R.layout.candidates_container, null)
                as CandidatesContainer

        keyboardSet.initView(context)
        imeView = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val inputView = keyboardSet.getView(keyboardListener.shiftState, false)
        candidateView = ScrollingCandidateView(context, null).apply {
            listener = this@PinyinIMEMode
        }
        imeView.addView(candidateView as View)
        imeView.addView(inputView)
        return imeView
    }

    override fun getView(): View {
        updateInputView()
        return imeView
    }

    private fun showCandidateWindow(showComposingView: Boolean) {
        val candidates = mDecInfo.mCandidatesList.mapIndexed { i, s -> PinyinCandidate(i, s) }
        candidateView.submitList(candidates)
    }

    private fun onChoiceTouched(activeCandNo: Int) {
        if (mImeState == ImeState.STATE_COMPOSING) {
            changeToStateInput(true)
        } else if (mImeState == ImeState.STATE_INPUT
            || mImeState == ImeState.STATE_PREDICT
        ) {
            chooseCandidate(activeCandNo)
        } else if (mImeState == ImeState.STATE_APP_COMPLETION) {
            val appCompletions = mDecInfo.mAppCompletions
            if (null != appCompletions && activeCandNo >= 0 && activeCandNo < appCompletions.size) {
                val ci = appCompletions[activeCandNo]
                currentInputConnection?.commitCompletion(ci)
            }
            resetToIdleState(false)
        }
    }

    private fun requestHideSelf(flags: Int) {
    }

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        if(candidate is PinyinCandidate) {
            onChoiceTouched(candidate.index)
        }
    }

    override fun updateInputView() {
        keyboardSet.getView(keyboardListener.shiftState, false)
    }

    private inner class KeyboardListener: CommonKeyboardListener(this) {
        override fun onChar(code: Int) {
            val keyCode = keyCharacterMap.getEvents(charArrayOf(code.toChar())).firstOrNull()?.keyCode
            if(keyCode != null) processKeyCode(keyCode)
            super.onChar(code)
        }

        override fun onSpecial(type: Keyboard.SpecialKey) {
            when(type) {
                Keyboard.SpecialKey.Delete -> processKeyCode(KeyEvent.KEYCODE_DEL)
                Keyboard.SpecialKey.Space -> processKeyCode(KeyEvent.KEYCODE_SPACE)
                Keyboard.SpecialKey.Return -> processKeyCode(KeyEvent.KEYCODE_ENTER)
                else -> {}
            }
            super.onSpecial(type)
        }
    }

    private fun processKeyCode(keyCode: Int) {
        processKey(KeyEvent(KeyEvent.ACTION_DOWN, keyCode), true)
    }

    private fun processKey(event: KeyEvent, realAction: Boolean): Boolean {
        if (ImeState.STATE_BYPASS == mImeState) return false

        val keyCode = event.keyCode
        // SHIFT-SPACE is used to switch between Chinese and English
        // when HKB is on.
        if (KeyEvent.KEYCODE_SPACE == keyCode && event.isShiftPressed) {
            if (!realAction) return true

            resetToIdleState(false)

            val allMetaState = (KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
                    or KeyEvent.META_ALT_RIGHT_ON or KeyEvent.META_SHIFT_ON
                    or KeyEvent.META_SHIFT_LEFT_ON
                    or KeyEvent.META_SHIFT_RIGHT_ON or KeyEvent.META_SYM_ON)
            currentInputConnection?.clearMetaKeyStates(allMetaState)
            return true
        }

        if (processFunctionKeys(keyCode, realAction)) {
            return true
        }

        var keyChar = 0
        if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
            keyChar = keyCode - KeyEvent.KEYCODE_A + 'a'.code
        } else if (keyCode >= KeyEvent.KEYCODE_0
            && keyCode <= KeyEvent.KEYCODE_9
        ) {
            keyChar = keyCode - KeyEvent.KEYCODE_0 + '0'.code
        } else if (keyCode == KeyEvent.KEYCODE_COMMA) {
            keyChar = ','.code
        } else if (keyCode == KeyEvent.KEYCODE_PERIOD) {
            keyChar = '.'.code
        } else if (keyCode == KeyEvent.KEYCODE_SPACE) {
            keyChar = ' '.code
        } else if (keyCode == KeyEvent.KEYCODE_APOSTROPHE) {
            keyChar = '\''.code
        }

        if (mImeState == ImeState.STATE_IDLE ||
            mImeState == ImeState.STATE_APP_COMPLETION
        ) {
            mImeState = ImeState.STATE_IDLE
            return processStateIdle(keyChar, keyCode, event, realAction)
        } else if (mImeState == ImeState.STATE_INPUT) {
            return processStateInput(keyChar, keyCode, event, realAction)
        } else if (mImeState == ImeState.STATE_PREDICT) {
            return processStatePredict(keyChar, keyCode, event, realAction)
        } else if (mImeState == ImeState.STATE_COMPOSING) {
            return processStateEditComposing(
                keyChar, keyCode, event,
                realAction
            )
        }

        return false
    }

    // keyCode can be from both hard key or soft key.
    private fun processFunctionKeys(keyCode: Int, realAction: Boolean): Boolean {
        if (mCandidatesContainer.isShown() && !mDecInfo.isCandidatesListEmpty) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                if (!realAction) return true

                chooseCandidate(-1)
                return true
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (!realAction) return true
                mCandidatesContainer.activeCurseBackward()
                return true
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (!realAction) return true
                mCandidatesContainer.activeCurseForward()
                return true
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                if (!realAction) return true
                mCandidatesContainer.pageBackward(false, true)
                return true
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (!realAction) return true
                mCandidatesContainer.pageForward(false, true)
                return true
            }

            if (keyCode == KeyEvent.KEYCODE_DEL &&
                ImeState.STATE_PREDICT == mImeState
            ) {
                if (!realAction) return true
                resetToIdleState(false)
                return true
            }
        } else {
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                if (!realAction) return true
                if (SIMULATE_KEY_DELETE) {
                    simulateKeyEventDownUp(keyCode)
                } else {
                    currentInputConnection?.deleteSurroundingText(1, 0)
                }
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (!realAction) return true
                sendKeyChar('\n')
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_SPACE) {
                if (!realAction) return true
                sendKeyChar(' ')
                return true
            }
        }

        return false
    }

    private fun processStateIdle(
        keyChar: Int, keyCode: Int, event: KeyEvent,
        realAction: Boolean
    ): Boolean {
        // In this status, when user presses keys in [a..z], the status will
        // change to input state.
        if (keyChar >= 'a'.code && keyChar <= 'z'.code && !event.isAltPressed) {
            if (!realAction) return true
            mDecInfo.addSplChar(keyChar.toChar(), true)
            chooseAndUpdate(-1)
            return true
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            if (!realAction) return true
            if (SIMULATE_KEY_DELETE) {
                simulateKeyEventDownUp(keyCode)
            } else {
                currentInputConnection?.deleteSurroundingText(1, 0)
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (!realAction) return true
            sendKeyChar('\n')
            return true
        } else if (keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT || keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            return true
        } else if (event.isAltPressed) {
            val fullwidth_char = KeyMapDream.getChineseLabel(keyCode)
            if (0 != fullwidth_char.code) {
                if (realAction) {
                    val result = fullwidth_char.toString()
                    commitResultText(result)
                }
                return true
            } else {
                if (keyCode >= KeyEvent.KEYCODE_A
                    && keyCode <= KeyEvent.KEYCODE_Z
                ) {
                    return true
                }
            }
        } else if (keyChar != 0 && keyChar != '\t'.code) {
            if (realAction) {
                if (keyChar == ','.code || keyChar == '.'.code) {
                    inputCommaPeriod("", keyChar, false, ImeState.STATE_IDLE)
                } else {
                    if (0 != keyChar) {
                        val result = keyChar.toChar().toString()
                        commitResultText(result)
                    }
                }
            }
            return true
        }
        return false
    }

    private fun processStateInput(
        keyChar: Int, keyCode: Int, event: KeyEvent,
        realAction: Boolean
    ): Boolean {
        // If ALT key is pressed, input alternative key. But if the
        // alternative key is quote key, it will be used for input a splitter
        // in Pinyin string.
        var keyChar = keyChar
        if (event.isAltPressed) {
            if ('\''.code != event.getUnicodeChar(event.metaState)) {
                if (realAction) {
                    val fullwidth_char = KeyMapDream.getChineseLabel(keyCode)
                    if (0 != fullwidth_char.code) {
                        commitResultText(
                            mDecInfo
                                .getCurrentFullSent(
                                    mCandidatesContainer
                                        .getActiveCandiatePos()
                                ) + fullwidth_char.toString()
                        )
                        resetToIdleState(false)
                    }
                }
                return true
            } else {
                keyChar = '\''.code
            }
        }

        if (keyChar >= 'a'.code && keyChar <= 'z'.code || keyChar == '\''.code
            && !mDecInfo.charBeforeCursorIsSeparator() || keyCode == KeyEvent.KEYCODE_DEL
        ) {
            if (!realAction) return true
            return processSurfaceChange(keyChar, keyCode)
        } else if (keyChar == ','.code || keyChar == '.'.code) {
            if (!realAction) return true
            inputCommaPeriod(
                mDecInfo.getCurrentFullSent(
                    mCandidatesContainer
                        .getActiveCandiatePos()
                ), keyChar, true,
                ImeState.STATE_IDLE
            )
            return true
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (!realAction) return true

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                mCandidatesContainer.activeCurseBackward()
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                mCandidatesContainer.activeCurseForward()
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                // If it has been the first page, a up key will shift
                // the state to edit composing string.
                if (!mCandidatesContainer.pageBackward(false, true)) {
                    mCandidatesContainer.enableActiveHighlight(false)
                    changeToStateComposing(true)
                    updateComposingText(true)
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                mCandidatesContainer.pageForward(false, true)
            }
            return true
        } else if (keyCode >= KeyEvent.KEYCODE_1
            && keyCode <= KeyEvent.KEYCODE_9
        ) {
            if (!realAction) return true

            var activePos = keyCode - KeyEvent.KEYCODE_1
            val currentPage: Int = mCandidatesContainer.getCurrentPage()
            if (activePos < mDecInfo.getCurrentPageSize(currentPage)) {
                activePos = (activePos
                        + mDecInfo.getCurrentPageStart(currentPage))
                if (activePos >= 0) {
                    chooseAndUpdate(activePos)
                }
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (!realAction) return true
            if (isEnterNormalState) {
                commitResultText(mDecInfo.origianlSplStr.toString())
                resetToIdleState(false)
            } else {
                commitResultText(
                    mDecInfo
                        .getCurrentFullSent(
                            mCandidatesContainer
                                .getActiveCandiatePos()
                        )
                )
                sendKeyChar('\n')
                resetToIdleState(false)
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
            || keyCode == KeyEvent.KEYCODE_SPACE
        ) {
            if (!realAction) return true
            chooseCandidate(-1)
            return true
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!realAction) return true
            resetToIdleState(false)
            requestHideSelf(0)
            return true
        }
        return false
    }

    private fun processStatePredict(
        keyChar: Int, keyCode: Int,
        event: KeyEvent, realAction: Boolean
    ): Boolean {
        if (!realAction) return true

        // If ALT key is pressed, input alternative key.
        if (event.isAltPressed) {
            val fullwidth_char = KeyMapDream.getChineseLabel(keyCode)
            if (0 != fullwidth_char.code) {
                commitResultText(
                    mDecInfo.getCandidate(
                        mCandidatesContainer
                            .getActiveCandiatePos()
                    ) + fullwidth_char.toString()
                )
                resetToIdleState(false)
            }
            return true
        }

        // In this status, when user presses keys in [a..z], the status will
        // change to input state.
        if (keyChar >= 'a'.code && keyChar <= 'z'.code) {
            changeToStateInput(true)
            mDecInfo.addSplChar(keyChar.toChar(), true)
            chooseAndUpdate(-1)
        } else if (keyChar == ','.code || keyChar == '.'.code) {
            inputCommaPeriod("", keyChar, true, ImeState.STATE_IDLE)
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                mCandidatesContainer.activeCurseBackward()
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                mCandidatesContainer.activeCurseForward()
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                mCandidatesContainer.pageBackward(false, true)
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                mCandidatesContainer.pageForward(false, true)
            }
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            resetToIdleState(false)
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            resetToIdleState(false)
            requestHideSelf(0)
        } else if (keyCode >= KeyEvent.KEYCODE_1
            && keyCode <= KeyEvent.KEYCODE_9
        ) {
            var activePos = keyCode - KeyEvent.KEYCODE_1
            val currentPage: Int = mCandidatesContainer.getCurrentPage()
            if (activePos < mDecInfo.getCurrentPageSize(currentPage)) {
                activePos = (activePos
                        + mDecInfo.getCurrentPageStart(currentPage))
                if (activePos >= 0) {
                    chooseAndUpdate(activePos)
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            sendKeyChar('\n')
            resetToIdleState(false)
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
            || keyCode == KeyEvent.KEYCODE_SPACE
        ) {
            chooseCandidate(-1)
        }

        return true
    }

    private fun processStateEditComposing(
        keyChar: Int, keyCode: Int,
        event: KeyEvent, realAction: Boolean
    ): Boolean {
        var keyChar = keyChar
        if (!realAction) return true

        val cmpsvStatus: ComposingStatus =
            mComposingView.composingStatus

        // If ALT key is pressed, input alternative key. But if the
        // alternative key is quote key, it will be used for input a splitter
        // in Pinyin string.
        if (event.isAltPressed) {
            if ('\''.code != event.getUnicodeChar(event.metaState)) {
                val fullwidth_char = KeyMapDream.getChineseLabel(keyCode)
                if (0 != fullwidth_char.code) {
                    val retStr: String
                    if (ComposingStatus.SHOW_STRING_LOWERCASE == cmpsvStatus) {
                        retStr = mDecInfo.origianlSplStr.toString()
                    } else {
                        retStr = mDecInfo.composingStr
                    }
                    commitResultText(retStr + fullwidth_char.toString())
                    resetToIdleState(false)
                }
                return true
            } else {
                keyChar = '\''.code
            }
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (!mDecInfo.selectionFinished()) {
                changeToStateInput(true)
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
            || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
        ) {
            mComposingView.moveCursor(keyCode)
        } else if ((keyCode == KeyEvent.KEYCODE_ENTER && isEnterNormalState)
            || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_SPACE
        ) {
            if (ComposingStatus.SHOW_STRING_LOWERCASE == cmpsvStatus) {
                val str: String = mDecInfo.origianlSplStr.toString()
                if (!tryInputRawUnicode(str)) {
                    commitResultText(str)
                }
            } else if (ComposingStatus.EDIT_PINYIN == cmpsvStatus) {
                val str: String = mDecInfo.composingStr
                if (!tryInputRawUnicode(str)) {
                    commitResultText(str)
                }
            } else {
                commitResultText(mDecInfo.composingStr)
            }
            resetToIdleState(false)
        } else if (keyCode == KeyEvent.KEYCODE_ENTER
            && !isEnterNormalState
        ) {
            val retStr: String
            if (!mDecInfo.isCandidatesListEmpty) {
                retStr = mDecInfo.getCurrentFullSent(
                    mCandidatesContainer
                        .getActiveCandiatePos()
                )
            } else {
                retStr = mDecInfo.composingStr
            }
            commitResultText(retStr)
            sendKeyChar('\n')
            resetToIdleState(false)
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            resetToIdleState(false)
            requestHideSelf(0)
            return true
        } else {
            return processSurfaceChange(keyChar, keyCode)
        }
        return true
    }

    private fun tryInputRawUnicode(str: String): Boolean {
        if (str.length > 7) {
            if (str.substring(0, 7).compareTo("unicode") == 0) {
                try {
                    var digitStr = str.substring(7)
                    var startPos = 0
                    var radix = 10
                    if (digitStr.length > 2 && digitStr[0] == '0' && digitStr[1] == 'x') {
                        startPos = 2
                        radix = 16
                    }
                    digitStr = digitStr.substring(startPos)
                    val unicode = digitStr.toInt(radix)
                    if (unicode > 0) {
                        val low = (unicode and 0x0000ffff).toChar()
                        val high = ((unicode and -0x10000) shr 16).toChar()
                        commitResultText(low.toString())
                        if (0 != high.code) {
                            commitResultText(high.toString())
                        }
                    }
                    return true
                } catch (e: NumberFormatException) {
                    return false
                }
            } else if (str.substring(str.length - 7, str.length).compareTo(
                    "unicode"
                ) == 0
            ) {
                var resultStr = ""
                for (pos in 0..<str.length - 7) {
                    if (pos > 0) {
                        resultStr += " "
                    }

                    resultStr += "0x" + Integer.toHexString(str[pos].code)
                }
                commitResultText(resultStr.toString())
                return true
            }
        }
        return false
    }

    private fun processSurfaceChange(keyChar: Int, keyCode: Int): Boolean {
        if (mDecInfo.isSplStrFull && KeyEvent.KEYCODE_DEL != keyCode) {
            return true
        }

        if ((keyChar >= 'a'.code && keyChar <= 'z'.code)
            || (keyChar == '\''.code && !mDecInfo.charBeforeCursorIsSeparator())
            || (((keyChar >= '0'.code && keyChar <= '9'.code) || keyChar == ' '.code) && ImeState.STATE_COMPOSING == mImeState)
        ) {
            mDecInfo.addSplChar(keyChar.toChar(), false)
            chooseAndUpdate(-1)
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            mDecInfo.prepareDeleteBeforeCursor()
            chooseAndUpdate(-1)
        }
        return true
    }

    private fun changeToStateComposing(updateUi: Boolean) {
        mImeState = ImeState.STATE_COMPOSING
        if (!updateUi) return
    }

    private fun changeToStateInput(updateUi: Boolean) {
        mImeState = ImeState.STATE_INPUT
        if (!updateUi) return
        showCandidateWindow(true)
    }

    private fun simulateKeyEventDownUp(keyCode: Int) {
        val ic = currentInputConnection ?: return

        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun commitResultText(resultText: String) {
        currentInputConnection?.commitText(resultText, 1)
        if (null != mComposingView) {
            mComposingView.visibility = View.INVISIBLE
            mComposingView.invalidate()
        }
    }

    private fun updateComposingText(visible: Boolean) {
        if (!visible) {
            mComposingView.visibility = View.INVISIBLE
        } else {
            mComposingView.setDecodingInfo(mDecInfo, mImeState)
            mComposingView.visibility = View.VISIBLE
        }
        mComposingView.invalidate()
    }

    private fun inputCommaPeriod(
        preEdit: String, keyChar: Int,
        dismissCandWindow: Boolean, nextState: ImeState
    ) {
        var preEdit = preEdit
        preEdit += if (keyChar == ','.code) '\uff0c'
        else if (keyChar == '.'.code) '\u3002'
        else return
        commitResultText(preEdit)
        if (dismissCandWindow) resetCandidateWindow()
        mImeState = nextState
    }

    private fun resetToIdleState(resetInlineText: Boolean) {
        if (ImeState.STATE_IDLE == mImeState) return

        mImeState = ImeState.STATE_IDLE
        mDecInfo.reset()

        if (null != mComposingView) mComposingView.reset()
        if (resetInlineText) commitResultText("")
        resetCandidateWindow()
    }

    private fun chooseAndUpdate(candId: Int) {
        if (ImeState.STATE_PREDICT != mImeState) {
            // Get result candidate list, if choice_id < 0, do a new decoding.
            // If choice_id >=0, select the candidate, and get the new candidate
            // list.
            mDecInfo.chooseDecodingCandidate(candId)
        } else {
            // Choose a prediction item.
            mDecInfo.choosePredictChoice(candId)
        }

        if (mDecInfo.composingStr.isNotEmpty()) {
            val resultStr = mDecInfo.composingStrActivePart

            // choiceId >= 0 means user finishes a choice selection.
            if (candId >= 0 && mDecInfo.canDoPrediction()) {
                commitResultText(resultStr)
                mImeState = ImeState.STATE_PREDICT
                // Try to get the prediction list.
                if (Settings.getPrediction()) {
                    val ic = currentInputConnection
                    if (null != ic) {
                        val cs = ic.getTextBeforeCursor(3, 0)
                        if (null != cs) {
                            mDecInfo.preparePredicts(cs)
                        }
                    }
                } else {
                    mDecInfo.resetCandidates()
                }

                if (mDecInfo.mCandidatesList.size > 0) {
                    showCandidateWindow(false)
                } else {
                    resetToIdleState(false)
                }
            } else {
                if (ImeState.STATE_IDLE == mImeState) {
                    if (mDecInfo.splStrDecodedLen == 0) {
                        changeToStateComposing(true)
                    } else {
                        changeToStateInput(true)
                    }
                } else {
                    if (mDecInfo.selectionFinished()) {
                        changeToStateComposing(true)
                    }
                }
                showCandidateWindow(true)
            }
        } else {
            resetToIdleState(false)
        }
    }

    // If activeCandNo is less than 0, get the current active candidate number
    // from candidate view, otherwise use activeCandNo.
    private fun chooseCandidate(activeCandNo: Int) {
        var activeCandNo = activeCandNo
        if (activeCandNo < 0) {
            activeCandNo = mCandidatesContainer.getActiveCandiatePos()
        }
        if (activeCandNo >= 0) {
            chooseAndUpdate(activeCandNo)
        }
    }

    private fun startPinyinDecoderService(context: Context): Boolean {
        if (mDecInfo.mIPinyinDecoderService is Stub) {
            val serviceIntent = Intent()
            try {
                serviceIntent.setClass(
                    context,
                    Class.forName("com.android.inputmethod.pinyin.PinyinDecoderService")
                )
            } catch (e: ClassNotFoundException) {
                return false
            }

            val mPinyinDecoderServiceConnection = mPinyinDecoderServiceConnection ?: PinyinDecoderServiceConnection()

            // Bind service
            return context.bindService(
                    serviceIntent, mPinyinDecoderServiceConnection,
                    Context.BIND_AUTO_CREATE
                )
        }
        return true
    }

    inner class DecodingInfo {
        /**
         * Spelling (Pinyin) string.
         */
        val origianlSplStr: StringBuffer = StringBuffer()

        /**
         * Byte buffer used as the Pinyin string parameter for native function
         * call.
         */
        private var mPyBuf: ByteArray? = null

        /**
         * The length of surface string successfully decoded by engine.
         */
        var splStrDecodedLen: Int = 0
            private set

        /**
         * Composing string.
         */
        var composingStr: String = ""
            private set

        /**
         * Length of the active composing string.
         */
        var activeCmpsLen: Int = 0
            private set

        /**
         * Composing string for display, it is copied from mComposingStr, and
         * add spaces between spellings.
         */
        var composingStrForDisplay: String = ""
            private set

        /**
         * Length of the active composing string for display.
         */
        var activeCmpsDisplayLen: Int = 0
            private set

        /**
         * The first full sentence choice.
         */
        var fullSent: String = ""
            private set

        /**
         * Number of characters which have been fixed.
         */
        var fixedLen: Int = 0
            private set

        /**
         * If this flag is true, selection is finished.
         */
        private var mFinishSelection = false

        /**
         * The starting position for each spelling. The first one is the number
         * of the real starting position elements.
         */
        var splStart: IntArray = intArrayOf()
            private set

        /**
         * Editing cursor in mSurface.
         */
        var cursorPos: Int = 0
            private set

        /**
         * Remote Pinyin-to-Hanzi decoding engine service.
         */
        var mIPinyinDecoderService: IPinyinDecoderService = Stub()

        /**
         * The complication information suggested by application.
         */
        var mAppCompletions: Array<CompletionInfo>? = null

        /**
         * The total number of choices for display. The list may only contains
         * the first part. If user tries to navigate to next page which is not
         * in the result list, we need to get these items.
         */
        var mTotalChoicesNum: Int = 0

        /**
         * Candidate list. The first one is the full-sentence candidate.
         */
        var mCandidatesList: MutableList<String> = Vector()

        /**
         * Element i stores the starting position of page i.
         */
        var mPageStart: Vector<Int> = Vector()

        /**
         * Element i stores the number of characters to page i.
         */
        var mCnToPage: Vector<Int> = Vector()

        /**
         * The position to delete in Pinyin string. If it is less than 0, IME
         * will do an incremental search, otherwise IME will do a deletion
         * operation. if [.mIsPosInSpl] is true, IME will delete the whole
         * string for mPosDelSpl-th spelling, otherwise it will only delete
         * mPosDelSpl-th character in the Pinyin string.
         */
        var mPosDelSpl: Int = -1

        /**
         * If [.mPosDelSpl] is big than or equal to 0, this member is used
         * to indicate that whether the postion is counted in spelling id or
         * character.
         */
        var mIsPosInSpl: Boolean = false

        fun reset() {
            origianlSplStr.delete(0, origianlSplStr.length)
            splStrDecodedLen = 0
            cursorPos = 0
            fullSent = ""
            fixedLen = 0
            mFinishSelection = false
            composingStr = ""
            composingStrForDisplay = ""
            activeCmpsLen = 0
            activeCmpsDisplayLen = 0

            resetCandidates()
        }

        val isCandidatesListEmpty: Boolean
            get() = mCandidatesList.size == 0

        val isSplStrFull: Boolean
            get() {
                if (origianlSplStr.length >= PY_STRING_MAX - 1) return true
                return false
            }

        fun addSplChar(ch: Char, reset: Boolean) {
            if (reset) {
                origianlSplStr.delete(0, origianlSplStr.length)
                splStrDecodedLen = 0
                cursorPos = 0
                mIPinyinDecoderService.imResetSearch()
            }
            origianlSplStr.insert(cursorPos, ch)
            cursorPos++
        }

        // Prepare to delete before cursor. We may delete a spelling char if
        // the cursor is in the range of unfixed part, delete a whole spelling
        // if the cursor in inside the range of the fixed part.
        // This function only marks the position used to delete.
        fun prepareDeleteBeforeCursor() {
            if (cursorPos > 0) {
                var pos = 0
                while (pos < fixedLen) {
                    if (splStart[pos + 2] >= cursorPos
                        && splStart[pos + 1] < cursorPos
                    ) {
                        mPosDelSpl = pos
                        cursorPos = splStart[pos + 1]
                        mIsPosInSpl = true
                        break
                    }
                    pos++
                }
                if (mPosDelSpl < 0) {
                    mPosDelSpl = cursorPos - 1
                    cursorPos--
                    mIsPosInSpl = false
                }
            }
        }

        fun length(): Int {
            return origianlSplStr.length
        }

        fun charAt(index: Int): Char {
            return origianlSplStr[index]
        }

        val composingStrActivePart: String
            get() {
                assert(activeCmpsLen <= composingStr!!.length)
                return composingStr!!.substring(0, activeCmpsLen)
            }

        fun getCurrentFullSent(activeCandPos: Int): String {
            try {
                var retStr = fullSent.substring(0, fixedLen)
                retStr += mCandidatesList[activeCandPos]
                return retStr
            } catch (e: Exception) {
                return ""
            }
        }

        fun resetCandidates() {
            mCandidatesList.clear()
            mTotalChoicesNum = 0

            mPageStart.clear()
            mPageStart.add(0)
            mCnToPage.clear()
            mCnToPage.add(0)
        }

        fun candidatesFromApp(): Boolean {
            return ImeState.STATE_APP_COMPLETION == mImeState
        }

        fun canDoPrediction(): Boolean {
            return composingStr!!.length == fixedLen
        }

        fun selectionFinished(): Boolean {
            return mFinishSelection
        }

        // After the user chooses a candidate, input method will do a
        // re-decoding and give the new candidate list.
        // If candidate id is less than 0, means user is inputting Pinyin,
        // not selecting any choice.
        internal fun chooseDecodingCandidate(candId: Int) {
            if (mImeState != ImeState.STATE_PREDICT) {
                resetCandidates()
                var totalChoicesNum = 0
                if (candId < 0) {
                    if (length() == 0) {
                        totalChoicesNum = 0
                    } else {
                        if (mPyBuf == null) mPyBuf = ByteArray(PY_STRING_MAX)
                        for (i in 0..<length()) mPyBuf!![i] = charAt(i).code.toByte()
                        mPyBuf!![length()] = 0

                        if (mPosDelSpl < 0) {
                            totalChoicesNum = mIPinyinDecoderService
                                .imSearch(mPyBuf, length())
                        } else {
                            var clear_fixed_this_step = true
                            if (ImeState.STATE_COMPOSING == mImeState) {
                                clear_fixed_this_step = false
                            }
                            totalChoicesNum = mIPinyinDecoderService
                                .imDelSearch(
                                    mPosDelSpl, mIsPosInSpl,
                                    clear_fixed_this_step
                                )
                            mPosDelSpl = -1
                        }
                    }
                } else {
                    totalChoicesNum = mIPinyinDecoderService
                        .imChoose(candId)
                }
                updateDecInfoForSearch(totalChoicesNum)
            }
        }

        private fun updateDecInfoForSearch(totalChoicesNum: Int) {
            mTotalChoicesNum = totalChoicesNum
            if (mTotalChoicesNum < 0) {
                mTotalChoicesNum = 0
                return
            }

            try {
                splStart = mIPinyinDecoderService.imGetSplStart()
                val pyStr = mIPinyinDecoderService.imGetPyStr(false)
                splStrDecodedLen = mIPinyinDecoderService.imGetPyStrLen(true)
                assert(splStrDecodedLen <= pyStr.length)

                fullSent = mIPinyinDecoderService.imGetChoice(0)
                fixedLen = mIPinyinDecoderService.imGetFixedLen()

                // Update the surface string to the one kept by engine.
                origianlSplStr.replace(0, origianlSplStr.length, pyStr)

                if (cursorPos > origianlSplStr.length) cursorPos = origianlSplStr.length
                composingStr = (fullSent.substring(0, fixedLen)
                        + origianlSplStr.substring(splStart[fixedLen + 1]))

                activeCmpsLen = composingStr!!.length
                if (splStrDecodedLen > 0) {
                    activeCmpsLen = (activeCmpsLen
                            - (origianlSplStr.length - splStrDecodedLen))
                }

                // Prepare the display string.
                if (0 == splStrDecodedLen) {
                    composingStrForDisplay = composingStr
                    activeCmpsDisplayLen = composingStr!!.length
                } else {
                    composingStrForDisplay = fullSent.substring(0, fixedLen)
                    for (pos in fixedLen + 1..<splStart.size - 1) {
                        composingStrForDisplay += origianlSplStr.substring(
                            splStart[pos], splStart[pos + 1]
                        )
                        if (splStart[pos + 1] < splStrDecodedLen) {
                            composingStrForDisplay += " "
                        }
                    }
                    activeCmpsDisplayLen = composingStrForDisplay!!.length
                    if (splStrDecodedLen < origianlSplStr.length) {
                        composingStrForDisplay += origianlSplStr
                            .substring(splStrDecodedLen)
                    }
                }

                mFinishSelection = if (splStart.size == fixedLen + 2) {
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                mTotalChoicesNum = 0
                composingStr = ""
            }
            // Prepare page 0.
            if (!mFinishSelection) {
                preparePage(0)
            }
        }

        fun choosePredictChoice(choiceId: Int) {
            if (ImeState.STATE_PREDICT != mImeState || choiceId < 0 || choiceId >= mTotalChoicesNum) {
                return
            }

            val tmp = mCandidatesList[choiceId]

            resetCandidates()

            mCandidatesList.add(tmp)
            mTotalChoicesNum = 1

            origianlSplStr.replace(0, origianlSplStr.length, "")
            cursorPos = 0
            fullSent = tmp
            fixedLen = tmp.length
            composingStr = fullSent
            activeCmpsLen = fixedLen

            mFinishSelection = true
        }

        fun getCandidate(candId: Int): String? {
            // Only loaded items can be gotten, so we use mCandidatesList.size()
            // instead mTotalChoiceNum.
            if (candId < 0 || candId > mCandidatesList.size) {
                return null
            }
            return mCandidatesList[candId]
        }

        private val candiagtesForCache: Unit
            get() {
                val fetchStart = mCandidatesList.size
                var fetchSize = mTotalChoicesNum - fetchStart
                if (fetchSize > MAX_PAGE_SIZE_DISPLAY) {
                    fetchSize = MAX_PAGE_SIZE_DISPLAY
                }
                var newList: MutableList<String>? = null
                if (ImeState.STATE_INPUT == mImeState || ImeState.STATE_IDLE == mImeState || ImeState.STATE_COMPOSING == mImeState) {
                    newList = mIPinyinDecoderService.imGetChoiceList(
                        fetchStart, fetchSize, fixedLen
                    )
                } else if (ImeState.STATE_PREDICT == mImeState) {
                    newList = mIPinyinDecoderService.imGetPredictList(
                        fetchStart, fetchSize
                    )
                } else if (ImeState.STATE_APP_COMPLETION == mImeState) {
                    newList = ArrayList()
                    if (null != mAppCompletions) {
                        for (pos in fetchStart..<fetchSize) {
                            val ci = mAppCompletions!![pos]
                            if (null != ci) {
                                val s = ci.text
                                if (null != s) newList.add(s.toString())
                            }
                        }
                    }
                }
                mCandidatesList.addAll(newList!!)
            }

        fun pageReady(pageNo: Int): Boolean {
            // If the page number is less than 0, return false
            if (pageNo < 0) return false

            // Page pageNo's ending information is not ready.
            if (mPageStart.size <= pageNo + 1) {
                return false
            }

            return true
        }

        fun preparePage(pageNo: Int): Boolean {
            // If the page number is less than 0, return false
            if (pageNo < 0) return false

            // Make sure the starting information for page pageNo is ready.
            if (mPageStart.size <= pageNo) {
                return false
            }

            // Page pageNo's ending information is also ready.
            if (mPageStart.size > pageNo + 1) {
                return true
            }

            // If cached items is enough for page pageNo.
            if (mCandidatesList.size - mPageStart.elementAt(pageNo) >= MAX_PAGE_SIZE_DISPLAY) {
                return true
            }

            // Try to get more items from engine
            candiagtesForCache

            // Try to find if there are available new items to display.
            // If no new item, return false;
            if (mPageStart.elementAt(pageNo) >= mCandidatesList.size) {
                return false
            }

            // If there are new items, return true;
            return true
        }

        fun preparePredicts(history: CharSequence?) {
            if (null == history) return

            resetCandidates()

            if (Settings.getPrediction()) {
                val preEdit = history.toString()
                val predictNum = 0
                if (null != preEdit) {
                    mTotalChoicesNum = mIPinyinDecoderService
                        .imGetPredictsNum(preEdit)
                }
            }

            preparePage(0)
            mFinishSelection = false
        }

        private fun prepareAppCompletions(completions: Array<CompletionInfo>) {
            resetCandidates()
            mAppCompletions = completions
            mTotalChoicesNum = completions.size
            preparePage(0)
            mFinishSelection = false
            return
        }

        fun getCurrentPageSize(currentPage: Int): Int {
            if (mPageStart.size <= currentPage + 1) return 0
            return (mPageStart.elementAt(currentPage + 1)
                    - mPageStart.elementAt(currentPage))
        }

        fun getCurrentPageStart(currentPage: Int): Int {
            if (mPageStart.size < currentPage + 1) return mTotalChoicesNum
            return mPageStart.elementAt(currentPage)
        }

        fun pageForwardable(currentPage: Int): Boolean {
            if (mPageStart.size <= currentPage + 1) return false
            if (mPageStart.elementAt(currentPage + 1) >= mTotalChoicesNum) {
                return false
            }
            return true
        }

        fun pageBackwardable(currentPage: Int): Boolean {
            if (currentPage > 0) return true
            return false
        }

        fun charBeforeCursorIsSeparator(): Boolean {
            val len = origianlSplStr.length
            if (cursorPos > len) return false
            if (cursorPos > 0 && origianlSplStr[cursorPos - 1] == '\'') {
                return true
            }
            return false
        }

        val cursorPosInCmps: Int
            get() {
                var cursorPos = cursorPos
                val fixedLen = 0

                for (hzPos in 0..<this.fixedLen) {
                    if (this.cursorPos >= splStart[hzPos + 2]) {
                        cursorPos -= splStart[hzPos + 2] - splStart[hzPos + 1]
                        cursorPos += 1
                    }
                }
                return cursorPos
            }

        val cursorPosInCmpsDisplay: Int
            get() {
                var cursorPos = cursorPosInCmps
                // +2 is because: one for mSplStart[0], which is used for other
                // purpose(The length of the segmentation string), and another
                // for the first spelling which does not need a space before it.
                for (pos in fixedLen + 2..<splStart.size - 1) {
                    if (this.cursorPos <= splStart[pos]) {
                        break
                    } else {
                        cursorPos++
                    }
                }
                return cursorPos
            }

        fun moveCursorToEdge(left: Boolean) {
            if (left) cursorPos = 0
            else cursorPos = origianlSplStr.length
        }

        // Move cursor. If offset is 0, this function can be used to adjust
        // the cursor into the bounds of the string.
        fun moveCursor(offset: Int) {
            var offset = offset
            if (offset > 1 || offset < -1) return

            if (offset != 0) {
                var hzPos = 0
                hzPos = 0
                while (hzPos <= fixedLen) {
                    if (cursorPos == splStart[hzPos + 1]) {
                        if (offset < 0) {
                            if (hzPos > 0) {
                                offset = (splStart[hzPos]
                                        - splStart[hzPos + 1])
                            }
                        } else {
                            if (hzPos < fixedLen) {
                                offset = (splStart[hzPos + 2]
                                        - splStart[hzPos + 1])
                            }
                        }
                        break
                    }
                    hzPos++
                }
            }
            cursorPos += offset
            if (cursorPos < 0) {
                cursorPos = 0
            } else if (cursorPos > origianlSplStr.length) {
                cursorPos = origianlSplStr.length
            }
        }

        val splNum: Int
            get() = splStart[0]

    }

    /**
     * Connection used for binding to the Pinyin decoding service.
     */
    inner class PinyinDecoderServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mDecInfo.mIPinyinDecoderService = Stub
                .asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
        }
    }

    /**
     * Send the given key event code (as defined by [KeyEvent]) to the
     * current input connection is a key down + key up event pair.  The sent
     * events have [KeyEvent.FLAG_SOFT_KEYBOARD]
     * set, so that the recipient can identify them as coming from a software
     * input method, and
     * [KeyEvent.FLAG_KEEP_TOUCH_MODE], so
     * that they don't impact the current touch mode of the UI.
     *
     *
     * Note that it's discouraged to send such key events in normal operation;
     * this is mainly for use with [android.text.InputType.TYPE_NULL] type
     * text fields, or for non-rich input methods. A reasonably capable software
     * input method should use the
     * [android.view.inputmethod.InputConnection.commitText] family of methods
     * to send text to an application, rather than sending key events.
     *
     * @param keyEventCode The raw key code to send, as defined by
     * [KeyEvent].
     */
    private fun sendDownUpKeyEvents(keyEventCode: Int) {
        val ic = currentInputConnection ?: return
        val eventTime = SystemClock.uptimeMillis()
        ic.sendKeyEvent(
            KeyEvent(
                eventTime, eventTime,
                KeyEvent.ACTION_DOWN, keyEventCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
            )
        )
        ic.sendKeyEvent(
            KeyEvent(
                eventTime, SystemClock.uptimeMillis(),
                KeyEvent.ACTION_UP, keyEventCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
            )
        )
    }

    /**
     * Ask the input target to execute its default action via
     * [ InputConnection.performEditorAction()][InputConnection.performEditorAction].
     *
     *
     * For compatibility, this method does not execute a custom action even if [ ][EditorInfo.actionLabel] is set. The implementor should directly call
     * [InputConnection.performEditorAction()][InputConnection.performEditorAction] with
     * [EditorInfo.actionId] if they want to execute a custom action.
     *
     * @param fromEnterKey If true, this will be executed as if the user had
     * pressed an enter key on the keyboard, that is it will *not*
     * be done if the editor has set [ EditorInfo.IME_FLAG_NO_ENTER_ACTION][EditorInfo.IME_FLAG_NO_ENTER_ACTION].  If false, the action will be
     * sent regardless of how the editor has set that flag.
     *
     * @return Returns a boolean indicating whether an action has been sent.
     * If false, either the editor did not specify a default action or it
     * does not want an action from the enter key.  If true, the action was
     * sent (or there was no input connection at all).
     */
    private fun sendDefaultEditorAction(fromEnterKey: Boolean): Boolean {
        val ei: EditorInfo = currentInputEditorInfo ?: return false
        if ((!fromEnterKey || (ei.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0)
            && (ei.imeOptions and EditorInfo.IME_MASK_ACTION) != EditorInfo.IME_ACTION_NONE
        ) {
            // If the enter key was pressed, and the editor has a default
            // action associated with pressing enter, then send it that
            // explicit action instead of the key event.
            val ic = currentInputConnection ?: return true
            ic.performEditorAction(ei.imeOptions and EditorInfo.IME_MASK_ACTION)
            return true
        }

        return false
    }

    /**
     * Send the given UTF-16 character to the current input connection.  Most
     * characters will be delivered simply by calling
     * [InputConnection.commitText()][InputConnection.commitText] with
     * the character; some, however, may be handled different.  In particular,
     * the enter character ('\n') will either be delivered as an action code
     * or a raw key event, as appropriate.  Consider this as a convenience
     * method for IMEs that do not have a full implementation of actions; a
     * fully complying IME will decide of the right action for each event and
     * will likely never call this method except maybe to handle events coming
     * from an actual hardware keyboard.
     *
     * @param charCode The UTF-16 character code to send.
     */
    private fun sendKeyChar(charCode: Char) {
        when (charCode) {
            '\n' -> if (!sendDefaultEditorAction(true)) {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            }

            else ->                 // Make sure that digits go through any text watcher on the client side.
                if (charCode >= '0' && charCode <= '9') {
                    sendDownUpKeyEvents(charCode.code - '0'.code + KeyEvent.KEYCODE_0)
                } else {
                    val ic = currentInputConnection ?: return
                    ic.commitText(charCode.toString(), 1)
                }
        }
    }

    data class PinyinCandidate(
        val index: Int,
        override val text: CharSequence
    ): CandidateView.Candidate

    companion object {
        private const val SIMULATE_KEY_DELETE = true

        /**
         * Maximum length of the Pinyin string
         */
        private const val PY_STRING_MAX = 28

        /**
         * Maximum number of candidates to display in one page.
         */
        private const val MAX_PAGE_SIZE_DISPLAY = 10
    }
}