package ee.oyatl.ime.fusion

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
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
    context: Context,
    private val listener: IMEMode.Listener
): IMEMode, CandidateView.Listener, CommonKeyboardListener.Callback {
    private val keyCharacterMap: KeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

    /**
     * Connection used to bind the decoding service.
     */
    private var pinyinDecoderServiceConnection: PinyinDecoderServiceConnection? = null

    /**
     * The current IME status.
     *
     * @see ImeState
     */
    private var imeState = ImeState.STATE_IDLE

    /**
     * The decoding information, include spelling(Pinyin) string, decoding
     * result, etc.
     */
    private val decInfo: DecodingInfo = DecodingInfo()

    /**
     * The floating container which contains the composing view. If necessary,
     * some other view like candiates container can also be put here.
     */
    private lateinit var floatingContainer: LinearLayout

    /**
     * View to show the composing string.
     */
    private lateinit var composingView: ComposingView

    /**
     * View to show candidates list.
     */
    private lateinit var candidatesContainer: CandidatesContainer

    private var isEnterNormalState = true

    private val keyboardListener = KeyboardListener()
    private val keyboardSet: KeyboardSet = StackedKeyboardSet(
        DefaultKeyboardSet(keyboardListener, LayoutQwerty.ROWS_PINYIN_LOWER, LayoutQwerty.ROWS_PINYIN_UPPER),
        BottomRowKeyboardSet(keyboardListener)
    )
    private lateinit var candidateView: CandidateView
    private lateinit var imeView: ViewGroup

    private var util: KeyEventUtil? = null
    private val currentInputConnection: InputConnection? get() = util?.currentInputConnection
    private val currentInputEditorInfo: EditorInfo? get() = util?.currentInputEditorInfo

    init {
        startPinyinDecoderService(context)
    }

    override fun onStart(inputConnection: InputConnection, editorInfo: EditorInfo) {
        this.util = KeyEventUtil(inputConnection, editorInfo)
    }

    override fun onFinish(inputConnection: InputConnection, editorInfo: EditorInfo) {
        this.util = null
    }

    override fun initView(context: Context): View {
        val layoutInflater = LayoutInflater.from(context)

        // Inflate the floating container view
        floatingContainer = layoutInflater.inflate(
            com.android.inputmethod.pinyin.R.layout.floating_container, null)
                as LinearLayout

        // The first child is the composing view.
        composingView = floatingContainer.getChildAt(0) as ComposingView

        candidatesContainer = layoutInflater.inflate(
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
        updateComposingText(showComposingView)
        val candidates = decInfo.mCandidatesList.mapIndexed { i, s -> PinyinCandidate(i, s) }
        candidateView.submitList(candidates)
    }

    private fun resetCandidateWindow() {
        updateComposingText(false)
        decInfo.resetCandidates()
        candidateView.submitList(emptyList())
    }

    private fun onChoiceTouched(activeCandNo: Int) {
        if (imeState == ImeState.STATE_COMPOSING) {
            changeToStateInput(true)
        } else if (imeState == ImeState.STATE_INPUT
            || imeState == ImeState.STATE_PREDICT
        ) {
            chooseCandidate(activeCandNo)
        } else if (imeState == ImeState.STATE_APP_COMPLETION) {
            val appCompletions = decInfo.mAppCompletions
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
                Keyboard.SpecialKey.Language -> listener.onLanguageSwitch()
                else -> {}
            }
            super.onSpecial(type)
        }
    }

    private fun processKeyCode(keyCode: Int) {
        processKey(KeyEvent(KeyEvent.ACTION_DOWN, keyCode), true)
    }

    private fun processKey(event: KeyEvent, realAction: Boolean): Boolean {
        if (ImeState.STATE_BYPASS == imeState) return false

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

        if (imeState == ImeState.STATE_IDLE ||
            imeState == ImeState.STATE_APP_COMPLETION
        ) {
            imeState = ImeState.STATE_IDLE
            return processStateIdle(keyChar, keyCode, event, realAction)
        } else if (imeState == ImeState.STATE_INPUT) {
            return processStateInput(keyChar, keyCode, event, realAction)
        } else if (imeState == ImeState.STATE_PREDICT) {
            return processStatePredict(keyChar, keyCode, event, realAction)
        } else if (imeState == ImeState.STATE_COMPOSING) {
            return processStateEditComposing(
                keyChar, keyCode, event,
                realAction
            )
        }

        return false
    }

    // keyCode can be from both hard key or soft key.
    private fun processFunctionKeys(keyCode: Int, realAction: Boolean): Boolean {
        if (!decInfo.isCandidatesListEmpty) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                if (!realAction) return true

                chooseCandidate(-1)
                return true
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (!realAction) return true
                candidatesContainer.activeCurseBackward()
                return true
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (!realAction) return true
                candidatesContainer.activeCurseForward()
                return true
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                if (!realAction) return true
                candidatesContainer.pageBackward(false, true)
                return true
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (!realAction) return true
                candidatesContainer.pageForward(false, true)
                return true
            }

            if (keyCode == KeyEvent.KEYCODE_DEL &&
                ImeState.STATE_PREDICT == imeState
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
                util?.sendKeyChar('\n')
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_SPACE) {
                if (!realAction) return true
                util?.sendKeyChar(' ')
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
            decInfo.addSplChar(keyChar.toChar(), true)
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
            util?.sendKeyChar('\n')
            return true
        } else if (keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT || keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            return true
        } else if (event.isAltPressed) {
            val fullwidthChar = KeyMapDream.getChineseLabel(keyCode)
            if (0 != fullwidthChar.code) {
                if (realAction) {
                    val result = fullwidthChar.toString()
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
                    val result = keyChar.toChar().toString()
                    commitResultText(result)
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
                            decInfo
                                .getCurrentFullSent(
                                    candidatesContainer
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
            && !decInfo.charBeforeCursorIsSeparator() || keyCode == KeyEvent.KEYCODE_DEL
        ) {
            if (!realAction) return true
            return processSurfaceChange(keyChar, keyCode)
        } else if (keyChar == ','.code || keyChar == '.'.code) {
            if (!realAction) return true
            inputCommaPeriod(
                decInfo.getCurrentFullSent(
                    candidatesContainer
                        .getActiveCandiatePos()
                ), keyChar, true,
                ImeState.STATE_IDLE
            )
            return true
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (!realAction) return true

            when(keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    candidatesContainer.activeCurseBackward()
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    candidatesContainer.activeCurseForward()
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    // If it has been the first page, a up key will shift
                    // the state to edit composing string.
                    if (!candidatesContainer.pageBackward(false, true)) {
                        candidatesContainer.enableActiveHighlight(false)
                        changeToStateComposing(true)
                        updateComposingText(true)
                    }
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    candidatesContainer.pageForward(false, true)
                }
            }
            return true
        } else if (keyCode >= KeyEvent.KEYCODE_1
            && keyCode <= KeyEvent.KEYCODE_9
        ) {
            if (!realAction) return true

            var activePos = keyCode - KeyEvent.KEYCODE_1
            val currentPage: Int = candidatesContainer.getCurrentPage()
            if (activePos < decInfo.getCurrentPageSize(currentPage)) {
                activePos = (activePos
                        + decInfo.getCurrentPageStart(currentPage))
                if (activePos >= 0) {
                    chooseAndUpdate(activePos)
                }
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (!realAction) return true
            if (isEnterNormalState) {
                commitResultText(decInfo.origianlSplStr.toString())
                resetToIdleState(false)
            } else {
                commitResultText(
                    decInfo
                        .getCurrentFullSent(
                            candidatesContainer
                                .getActiveCandiatePos()
                        )
                )
                util?.sendKeyChar('\n')
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
            val fullwidthChar = KeyMapDream.getChineseLabel(keyCode)
            if (0 != fullwidthChar.code) {
                commitResultText(
                    decInfo.getCandidate(
                        candidatesContainer
                            .getActiveCandiatePos()
                    ) + fullwidthChar.toString()
                )
                resetToIdleState(false)
            }
            return true
        }

        // In this status, when user presses keys in [a..z], the status will
        // change to input state.
        if (keyChar >= 'a'.code && keyChar <= 'z'.code) {
            changeToStateInput(true)
            decInfo.addSplChar(keyChar.toChar(), true)
            chooseAndUpdate(-1)
        } else if (keyChar == ','.code || keyChar == '.'.code) {
            inputCommaPeriod("", keyChar, true, ImeState.STATE_IDLE)
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                candidatesContainer.activeCurseBackward()
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                candidatesContainer.activeCurseForward()
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                candidatesContainer.pageBackward(false, true)
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                candidatesContainer.pageForward(false, true)
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
            val currentPage: Int = candidatesContainer.getCurrentPage()
            if (activePos < decInfo.getCurrentPageSize(currentPage)) {
                activePos = (activePos
                        + decInfo.getCurrentPageStart(currentPage))
                if (activePos >= 0) {
                    chooseAndUpdate(activePos)
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            util?.sendKeyChar('\n')
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
            composingView.composingStatus

        // If ALT key is pressed, input alternative key. But if the
        // alternative key is quote key, it will be used for input a splitter
        // in Pinyin string.
        if (event.isAltPressed) {
            if ('\''.code != event.getUnicodeChar(event.metaState)) {
                val fullwidth_char = KeyMapDream.getChineseLabel(keyCode)
                if (0 != fullwidth_char.code) {
                    val retStr: String
                    if (ComposingStatus.SHOW_STRING_LOWERCASE == cmpsvStatus) {
                        retStr = decInfo.origianlSplStr.toString()
                    } else {
                        retStr = decInfo.composingStr
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
            if (!decInfo.selectionFinished()) {
                changeToStateInput(true)
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
            || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
        ) {
            composingView.moveCursor(keyCode)
        } else if ((keyCode == KeyEvent.KEYCODE_ENTER && isEnterNormalState)
            || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_SPACE
        ) {
            if (ComposingStatus.SHOW_STRING_LOWERCASE == cmpsvStatus) {
                val str: String = decInfo.origianlSplStr.toString()
                if (!tryInputRawUnicode(str)) {
                    commitResultText(str)
                }
            } else if (ComposingStatus.EDIT_PINYIN == cmpsvStatus) {
                val str: String = decInfo.composingStr
                if (!tryInputRawUnicode(str)) {
                    commitResultText(str)
                }
            } else {
                commitResultText(decInfo.composingStr)
            }
            resetToIdleState(false)
        } else if (keyCode == KeyEvent.KEYCODE_ENTER
            && !isEnterNormalState
        ) {
            val retStr: String
            if (!decInfo.isCandidatesListEmpty) {
                retStr = decInfo.getCurrentFullSent(
                    candidatesContainer
                        .getActiveCandiatePos()
                )
            } else {
                retStr = decInfo.composingStr
            }
            commitResultText(retStr)
            util?.sendKeyChar('\n')
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
        if (decInfo.isSplStrFull && KeyEvent.KEYCODE_DEL != keyCode) {
            return true
        }

        if ((keyChar >= 'a'.code && keyChar <= 'z'.code)
            || (keyChar == '\''.code && !decInfo.charBeforeCursorIsSeparator())
            || (((keyChar >= '0'.code && keyChar <= '9'.code) || keyChar == ' '.code) && ImeState.STATE_COMPOSING == imeState)
        ) {
            decInfo.addSplChar(keyChar.toChar(), false)
            chooseAndUpdate(-1)
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            decInfo.prepareDeleteBeforeCursor()
            chooseAndUpdate(-1)
        }
        return true
    }

    private fun changeToStateComposing(updateUi: Boolean) {
        imeState = ImeState.STATE_COMPOSING
        if (!updateUi) return
    }

    private fun changeToStateInput(updateUi: Boolean) {
        imeState = ImeState.STATE_INPUT
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
        composingView.visibility = View.INVISIBLE
        composingView.invalidate()
    }

    private fun updateComposingText(visible: Boolean) {
        if (!visible) {
            composingView.visibility = View.INVISIBLE
        } else {
//            mComposingView.setDecodingInfo(mDecInfo, mImeState)
            composingView.visibility = View.VISIBLE
        }
        composingView.invalidate()
        currentInputConnection?.setComposingText(decInfo.composingStrForDisplay, 1)
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
        imeState = nextState
    }

    private fun resetToIdleState(resetInlineText: Boolean) {
        if (ImeState.STATE_IDLE == imeState) return

        imeState = ImeState.STATE_IDLE
        decInfo.reset()

        composingView.reset()
        if (resetInlineText) commitResultText("")
        resetCandidateWindow()
    }

    private fun chooseAndUpdate(candId: Int) {
        if (ImeState.STATE_PREDICT != imeState) {
            // Get result candidate list, if choice_id < 0, do a new decoding.
            // If choice_id >=0, select the candidate, and get the new candidate
            // list.
            decInfo.chooseDecodingCandidate(candId)
        } else {
            // Choose a prediction item.
            decInfo.choosePredictChoice(candId)
        }

        if (decInfo.composingStr.isNotEmpty()) {
            val resultStr = decInfo.composingStrActivePart

            // choiceId >= 0 means user finishes a choice selection.
            if (candId >= 0 && decInfo.canDoPrediction()) {
                commitResultText(resultStr)
                imeState = ImeState.STATE_PREDICT
                // Try to get the prediction list.
                if (Settings.getPrediction()) {
                    val ic = currentInputConnection
                    if (null != ic) {
                        val cs = ic.getTextBeforeCursor(3, 0)
                        if (null != cs) {
                            decInfo.preparePredicts(cs)
                        }
                    }
                } else {
                    decInfo.resetCandidates()
                }

                if (decInfo.mCandidatesList.size > 0) {
                    showCandidateWindow(false)
                } else {
                    resetToIdleState(false)
                }
            } else {
                if (ImeState.STATE_IDLE == imeState) {
                    if (decInfo.splStrDecodedLen == 0) {
                        changeToStateComposing(true)
                    } else {
                        changeToStateInput(true)
                    }
                } else {
                    if (decInfo.selectionFinished()) {
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
            activeCandNo = candidatesContainer.getActiveCandiatePos()
        }
        if (activeCandNo >= 0) {
            chooseAndUpdate(activeCandNo)
        }
    }

    private fun startPinyinDecoderService(context: Context): Boolean {
        if (decInfo.mIPinyinDecoderService is Stub) {
            val serviceIntent = Intent()
            try {
                serviceIntent.setClass(
                    context,
                    Class.forName("com.android.inputmethod.pinyin.PinyinDecoderService")
                )
            } catch (e: ClassNotFoundException) {
                return false
            }

            val mPinyinDecoderServiceConnection = pinyinDecoderServiceConnection ?: PinyinDecoderServiceConnection()

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
                assert(activeCmpsLen <= composingStr.length)
                return composingStr.substring(0, activeCmpsLen)
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
            return ImeState.STATE_APP_COMPLETION == imeState
        }

        fun canDoPrediction(): Boolean {
            return composingStr.length == fixedLen
        }

        fun selectionFinished(): Boolean {
            return mFinishSelection
        }

        // After the user chooses a candidate, input method will do a
        // re-decoding and give the new candidate list.
        // If candidate id is less than 0, means user is inputting Pinyin,
        // not selecting any choice.
        internal fun chooseDecodingCandidate(candId: Int) {
            if (imeState != ImeState.STATE_PREDICT) {
                resetCandidates()
                val totalChoicesNum: Int
                if (candId < 0) {
                    if (length() == 0) {
                        totalChoicesNum = 0
                    } else {
                        val mPyBuf = mPyBuf ?: ByteArray(PY_STRING_MAX)
                        this.mPyBuf = mPyBuf
                        for (i in 0..<length()) mPyBuf[i] = charAt(i).code.toByte()
                        mPyBuf[length()] = 0

                        if (mPosDelSpl < 0) {
                            totalChoicesNum = mIPinyinDecoderService
                                .imSearch(mPyBuf, length())
                        } else {
                            var clearFixedThisStep = true
                            if (ImeState.STATE_COMPOSING == imeState) {
                                clearFixedThisStep = false
                            }
                            totalChoicesNum = mIPinyinDecoderService
                                .imDelSearch(
                                    mPosDelSpl, mIsPosInSpl,
                                    clearFixedThisStep
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

                activeCmpsLen = composingStr.length
                if (splStrDecodedLen > 0) {
                    activeCmpsLen = (activeCmpsLen
                            - (origianlSplStr.length - splStrDecodedLen))
                }

                // Prepare the display string.
                if (0 == splStrDecodedLen) {
                    composingStrForDisplay = composingStr
                    activeCmpsDisplayLen = composingStr.length
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
                    activeCmpsDisplayLen = composingStrForDisplay.length
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
            if (ImeState.STATE_PREDICT != imeState || choiceId < 0 || choiceId >= mTotalChoicesNum) {
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
                var newList: List<String> = listOf()
                if (ImeState.STATE_INPUT == imeState || ImeState.STATE_IDLE == imeState || ImeState.STATE_COMPOSING == imeState) {
                    newList = mIPinyinDecoderService.imGetChoiceList(
                        fetchStart, fetchSize, fixedLen
                    )
                } else if (ImeState.STATE_PREDICT == imeState) {
                    newList = mIPinyinDecoderService.imGetPredictList(
                        fetchStart, fetchSize
                    )
                } else if (ImeState.STATE_APP_COMPLETION == imeState) {
                    newList = mutableListOf()
                    val mAppCompletions = mAppCompletions
                    if (null != mAppCompletions) {
                        for (pos in fetchStart..<fetchSize) {
                            val ci = mAppCompletions[pos]
                            val s = ci.text
                            if (null != s) newList.add(s.toString())
                        }
                    }
                }
                mCandidatesList.addAll(newList)
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
                mTotalChoicesNum = mIPinyinDecoderService
                    .imGetPredictsNum(preEdit)
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
            decInfo.mIPinyinDecoderService = Stub
                .asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
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