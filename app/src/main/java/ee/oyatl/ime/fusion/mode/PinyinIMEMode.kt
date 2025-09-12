package ee.oyatl.ime.fusion.mode

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.widget.LinearLayout
import com.android.inputmethod.pinyin.BalloonHint
import com.android.inputmethod.pinyin.CandidateViewListener
import com.android.inputmethod.pinyin.IPinyinDecoderService.Stub
import com.android.inputmethod.pinyin.KeyMapDream
import com.android.inputmethod.pinyin.PinyinIME.ImeState
import com.android.inputmethod.pinyin.R
import com.android.inputmethod.pinyin.Settings
import ee.oyatl.ime.candidate.CandidateView
import ee.oyatl.ime.fusion.pinyin.CandidatesContainer
import ee.oyatl.ime.fusion.pinyin.ComposingView
import ee.oyatl.ime.fusion.pinyin.ComposingView.ComposingStatus
import ee.oyatl.ime.fusion.pinyin.DecodingInfo
import ee.oyatl.ime.fusion.pinyin.OnGestureListener
import ee.oyatl.ime.keyboard.DefaultBottomRowKeyboard
import ee.oyatl.ime.keyboard.DefaultMobileKeyboard
import ee.oyatl.ime.keyboard.Keyboard
import ee.oyatl.ime.keyboard.KeyboardInflater
import ee.oyatl.ime.keyboard.ShiftStateKeyboard
import ee.oyatl.ime.keyboard.StackedKeyboard
import ee.oyatl.ime.keyboard.layout.LayoutPinyin
import java.util.Locale

class PinyinIMEMode(
    listener: IMEMode.Listener
): CommonIMEMode(listener) {
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
    private val decInfo: DecodingInfo = DecodingInfo { this.imeState }

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

    override val textKeyboard: Keyboard = StackedKeyboard(
        ShiftStateKeyboard(
            DefaultMobileKeyboard(KeyboardInflater.inflate(LayoutPinyin.ROWS_LOWER)[0]),
            DefaultMobileKeyboard(KeyboardInflater.inflate(LayoutPinyin.ROWS_UPPER)[0])
        ),
        DefaultBottomRowKeyboard()
    )

    override suspend fun onLoad(context: Context) {
        startPinyinDecoderService(context)
    }

    override fun onReset() {
        super.onReset()
        resetToIdleState(true)
    }

    override fun createCandidateView(context: Context): View {
        val layoutInflater = LayoutInflater.from(context)

        // Inflate the floating container view
        floatingContainer = layoutInflater.inflate(
            R.layout.floating_container, null)
                as LinearLayout

        // The first child is the composing view.
        composingView = floatingContainer.getChildAt(0) as ComposingView

        candidatesContainer = layoutInflater.inflate(
            R.layout.candidates_container, null)
                as CandidatesContainer
        val balloonHint = BalloonHint(context, candidatesContainer, MeasureSpec.UNSPECIFIED)
        val gestureDetector = GestureDetector(context,
            OnGestureListener(
                candidatesContainer,
                true
            )
        )
        candidatesContainer.initialize(CandidateListener(), balloonHint, gestureDetector)

        return super.createCandidateView(context)
    }

    private fun showCandidateWindow(showComposingView: Boolean) {
        updateComposingText(showComposingView)
        val candidates = decInfo.mCandidatesList.mapIndexed { i, s -> PinyinCandidate(i, s) }
        submitCandidates(candidates)
        candidatesContainer.showCandidates(decInfo, ImeState.STATE_COMPOSING != imeState)
    }

    private fun resetCandidateWindow() {
        updateComposingText(false)
        decInfo.resetCandidates()
        submitCandidates(emptyList())
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

    override fun onCandidateSelected(candidate: CandidateView.Candidate) {
        if(candidate is PinyinCandidate) {
            onChoiceTouched(candidate.index)
        }
    }

    override fun onChar(code: Int) {
        if(code in 'a'.code .. 'z'.code) {
            processKeyCode(code - 'a'.code + KeyEvent.KEYCODE_A)
        } else if(code == '\''.code) {
            processKeyCode(KeyEvent.KEYCODE_APOSTROPHE)
        } else {
            onReset()
            util?.sendKeyChar(code.toChar())
        }
    }

    override fun onSpecial(type: Keyboard.SpecialKey) {
        when(type) {
            Keyboard.SpecialKey.Delete -> processKeyCode(KeyEvent.KEYCODE_DEL)
            Keyboard.SpecialKey.Space -> processKeyCode(KeyEvent.KEYCODE_SPACE)
            Keyboard.SpecialKey.Return -> processKeyCode(KeyEvent.KEYCODE_ENTER)
            else -> {}
        }
    }

    private fun processKeyCode(keyCode: Int): Boolean {
        return processKey(KeyEvent(KeyEvent.ACTION_DOWN, keyCode), true)
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
            val currentPage: Int = candidatesContainer.currentPage
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
            composingView.setDecodingInfo(decInfo, imeState)
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

            val pinyinDecoderServiceConnection1 = pinyinDecoderServiceConnection ?: PinyinDecoderServiceConnection()

            // Bind service
            return context.bindService(
                    serviceIntent, pinyinDecoderServiceConnection1,
                    Context.BIND_AUTO_CREATE
                )
        }
        return true
    }

    fun stopPinyinDecoderService(context: Context) {
        val pinyinDecoderServiceConnection = pinyinDecoderServiceConnection ?: return
        context.unbindService(pinyinDecoderServiceConnection)
    }

    inner class CandidateListener: CandidateViewListener {
        override fun onClickChoice(choiceId: Int) {
            onChoiceTouched(choiceId)
        }

        override fun onToLeftGesture() {
        }

        override fun onToRightGesture() {
        }

        override fun onToTopGesture() {
        }

        override fun onToBottomGesture() {
        }
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

    class Params: IMEMode.Params {
        override val type: String = TYPE

        override fun create(listener: IMEMode.Listener): IMEMode {
            return PinyinIMEMode(listener)
        }

        override fun getLabel(context: Context): String {
            val localeName = Locale.SIMPLIFIED_CHINESE.displayName
            return "$localeName"
        }

        override fun getShortLabel(context: Context): String {
            return "拼音"
        }

        companion object {
            fun parse(map: Map<String, String>): Params {
                return Params()
            }
        }
    }

    companion object {
        const val TYPE: String = "pinyin"
        private const val SIMULATE_KEY_DELETE = true
    }
}