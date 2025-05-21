package ee.oyatl.ime.fusion.pinyin

import android.view.inputmethod.CompletionInfo
import com.android.inputmethod.pinyin.IPinyinDecoderService
import com.android.inputmethod.pinyin.IPinyinDecoderService.Stub
import com.android.inputmethod.pinyin.PinyinIME.ImeState
import com.android.inputmethod.pinyin.Settings
import java.util.Vector


class DecodingInfo(
    private val getImeState: () -> ImeState
) {
    private val imeState: ImeState get() = getImeState()

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
    @JvmField var mCandidatesList: MutableList<String> = Vector()

    /**
     * Element i stores the starting position of page i.
     */
    @JvmField var mPageStart: Vector<Int> = Vector()

    /**
     * Element i stores the number of characters to page i.
     */
    @JvmField var mCnToPage: Vector<Int> = Vector()

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
    fun chooseDecodingCandidate(candId: Int) {
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

    companion object {
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