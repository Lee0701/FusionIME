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
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Handler
import android.os.Message
import android.os.Vibrator
import android.preference.PreferenceManager
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.AbsoluteLayout
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import ee.oyatl.ime.fusion.R
import java.util.LinkedList
import kotlin.math.max
import kotlin.math.min

/**
 * The default candidates view manager class using [EditText].
 *
 * @author Copyright (C) 2009-2011 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
class TextCandidatesViewManager
/**
 * Constructor
 */ @JvmOverloads constructor(
    /** Limitation of displaying candidates  */
    private val mDisplayLimit: Int = -1
) :
    CandidatesViewManager(), GestureDetector.OnGestureListener {
    /** Body view of the candidates list  */
    private var mViewBody: ViewGroup? = null

    /** The view of the Symbol Tab  */
    private var mViewTabSymbol: TextView? = null

    /** The view of the Emoticon Tab  */
    private var mViewTabEmoticon: TextView? = null

    /** Scroller of `mViewBodyText`  */
    private var mViewBodyScroll: ScrollView? = null

    /** Base of `mViewCandidateList1st`, `mViewCandidateList2nd`  */
    private var mViewCandidateBase: ViewGroup? = null

    /** Button displayed bottom of the view when there are more candidates.  */
    private var mReadMoreButton: ImageView? = null

    /** Layout for the candidates list on normal view  */
    private var mViewCandidateList1st: LinearLayout? = null

    /** Layout for the candidates list on full view  */
    private var mViewCandidateList2nd: AbsoluteLayout? = null

    /** View for symbol tab  */
    private var mViewCandidateListTab: LinearLayout? = null

    /** [OpenWnn] instance using this manager  */
    private var mWnn: OpenWnn? = null

    /** View type (VIEW_TYPE_NORMAL or VIEW_TYPE_FULL or VIEW_TYPE_CLOSE)  */
    private var mViewType = 0

    /** Portrait display(`true`) or landscape(`false`)  */
    private var mPortrait = false

    /** Width of the view  */
    private var mViewWidth = 0
    /**
     * Get a minimum width of a candidate view.
     *
     * @return the minimum width of a candidate view.
     */
    /** Minimum width of a candidate (density support)  */
    private var candidateMinimumWidth = 0
    /**
     * @return the minimum height of a candidate view.
     */
    /** Maximum width of a candidate (density support)  */
    private var candidateMinimumHeight = 0

    /** Minimum height of the category candidate view  */
    private var mCandidateCategoryMinimumHeight = 0

    /** Left align threshold of the candidate view  */
    private var mCandidateLeftAlignThreshold = 0

    /** Height of keyboard  */
    private var mKeyboardHeight = 0

    /** Height of symbol keyboard  */
    private var mSymbolKeyboardHeight = 0

    /** Height of symbol keyboard tab  */
    private var mSymbolKeyboardTabHeight = 0

    /** Whether being able to use Emoticon  */
    private var mEnableEmoticon = false

    /** Whether hide the view if there is no candidate  */
    private var mAutoHideMode = true

    /** The converter to get candidates from and notice the selected candidate to.  */
    private var mConverter: WnnEngine? = null

    /** Vibrator for touch vibration  */
    private var mVibrator: Vibrator? = null

    /** AudioManager for click sound  */
    private var mSound: AudioManager? = null

    /** Number of candidates displaying for 1st  */
    private var mWordCount1st = 0

    /** Number of candidates displaying for 2nd  */
    private var mWordCount2nd = 0

    /** List of candidates for 1st  */
    private val mWnnWordArray1st = ArrayList<WnnWord>()

    /** List of candidates for 2nd  */
    private val mWnnWordArray2nd = ArrayList<WnnWord>()

    /** List of select candidates  */
    private val mWnnWordSelectedList = LinkedList<WnnWord?>()

    /** Gesture detector  */
    private var mGestureDetector: GestureDetector? = null

    /** Character width of the candidate area  */
    private var mLineLength = 0

    /** Number of lines displayed  */
    private var mLineCount = 1

    /** `true` if the full screen mode is selected  */
    private var mIsFullView = false

    /** The event object for "touch"  */
    private val mMotionEvent: MotionEvent? = null

    /** The offset when the candidates is flowed out the candidate window  */
    private var mDisplayEndOffset = 0

    /** `true` if there are more candidates to display.  */
    private var mCanReadMore = false

    /** Color of the candidates  */
    private var mTextColor = 0

    /** Template object for each candidate and normal/full view change button  */
    private var mViewCandidateTemplate: TextView? = null

    /** Number of candidates in full view  */
    private var mFullViewWordCount = 0

    /** Number of candidates in the current line (in full view)  */
    private var mFullViewOccupyCount = 0

    /** View of the previous candidate (in full view)  */
    private var mFullViewPrevView: TextView? = null

    /** Id of the top line view (in full view)  */
    private var mFullViewPrevLineTopId = 0

    /** Layout of the previous candidate (in full view)  */
    private var mFullViewPrevParams: ViewGroup.LayoutParams? = null

    /** Whether all candidates are displayed  */
    private var mCreateCandidateDone = false

    /** Number of lines in normal view  */
    private var mNormalViewWordCountOfLine = 0

    /** List of textView for CandiData List 1st for Symbol mode  */
    private val mTextViewArray1st = ArrayList<TextView?>()

    /** List of textView for CandiData List 2st for Symbol mode  */
    private val mTextViewArray2nd = ArrayList<TextView?>()

    /** Now focus textView index  */
    private var mCurrentFocusIndex = FOCUS_NONE

    /** Focused View  */
    private var mFocusedView: View? = null

    /** Focused View Background  */
    private var mFocusedViewBackground: Drawable? = null

    /** Axis to find next TextView for Up/Down  */
    private var mFocusAxisX = 0

    /** Now focused TextView in mTextViewArray1st  */
    private var mHasFocusedArray1st = true

    /** Portrait Number of Lines from Preference  */
    private var mPortraitNumberOfLine = LINE_NUM_PORTRAIT

    /** Landscape Number of Lines from Preference  */
    private var mLandscapeNumberOfLine = LINE_NUM_LANDSCAPE

    /** Coordinates of line  */
    private var mLineY = 0

    /** `true` if the candidate is selected  */
    private var mIsSymbolSelected = false

    /** Whether candidates is symbol  */
    private var mIsSymbolMode = false

    /** Symbol mode  */
    private var mSymbolMode: Int = OpenWnnJAJP.Companion.ENGINE_MODE_SYMBOL

    /** Text size of candidates  */
    private var mCandNormalTextSize = 0f

    /** Text size of category  */
    private var mCandCategoryTextSize = 0f

    /** HardKeyboard hidden(`true`) or disp(`false`)  */
    private var mHardKeyboardHidden = true

    /** Minimum height of the candidate 1line view  */
    private var mCandidateOneLineMinimumHeight = 0

    /** Whether candidates long click enable  */
    private val mEnableCandidateLongClick = true

    /** `Handler` Handler for focus Candidate wait delay  */
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_MOVE_FOCUS -> moveFocus(msg.arg1, msg.arg2 == 1)
                MSG_SET_CANDIDATES -> if (mViewType == CandidatesViewManager.Companion.VIEW_TYPE_FULL && mIsSymbolMode) {
                    displayCandidates(mConverter, false, SET_CANDIDATE_DELAY_LINE_COUNT)
                }

                MSG_SELECT_CANDIDATES -> {
                    var word: WnnWord? = null
                    while ((mWnnWordSelectedList.poll().also { word = it }) != null) {
                        selectCandidate(word)
                    }
                }

                else -> {}
            }
        }
    }

    /** Event listener for touching a candidate for 1st  */
    private val mCandidateOnClick1st =
        View.OnClickListener { v -> onClickCandidate(v, mWnnWordArray1st) }

    /** Event listener for touching a candidate for 2nd  */
    private val mCandidateOnClick2nd =
        View.OnClickListener { v -> onClickCandidate(v, mWnnWordArray2nd) }

    /** Event listener for long-clicking a candidate for 1st  */
    private val mCandidateOnLongClick1st =
        OnLongClickListener { v -> onLongClickCandidate(v, mWnnWordArray1st) }

    /** Event listener for long-clicking a candidate for for 2nd  */
    private val mCandidateOnLongClick2nd =
        OnLongClickListener { v -> onLongClickCandidate(v, mWnnWordArray2nd) }

    /** Event listener for click a symbol tab  */
    private val mTabOnClick =
        View.OnClickListener { v ->
            if (!v.isShown) {
                return@OnClickListener
            }
            playSoundAndVibration()
            if (v is TextView) {
                when (v.id) {
                    R.id.candview_symbol -> if (mSymbolMode != OpenWnnJAJP.Companion.ENGINE_MODE_SYMBOL) {
                        mWnn!!.onEvent(
                            OpenWnnEvent(
                                OpenWnnEvent.Companion.CHANGE_MODE,
                                OpenWnnJAJP.Companion.ENGINE_MODE_SYMBOL
                            )
                        )
                    }

                    R.id.candview_emoticon -> if (mSymbolMode != OpenWnnJAJP.Companion.ENGINE_MODE_SYMBOL_KAO_MOJI) {
                        mWnn!!.onEvent(
                            OpenWnnEvent(
                                OpenWnnEvent.Companion.CHANGE_MODE,
                                OpenWnnJAJP.Companion.ENGINE_MODE_SYMBOL
                            )
                        )
                    }

                    else -> {}
                }
            }
        }

    /**
     * Constructor
     *
     * @param mDisplayLimit      The limit of display
     */

    /**
     * Handle a click event on the candidate.
     * @param v  View
     * @param list  List of candidates
     */
    private fun onClickCandidate(v: View, list: ArrayList<WnnWord>) {
        if (!v.isShown) {
            return
        }
        playSoundAndVibration()

        if (v is TextView) {
            val wordcount = v.id
            val word = list[wordcount]

            if (mHandler.hasMessages(MSG_SET_CANDIDATES)) {
                mWnnWordSelectedList.add(word)
                return
            }
            clearFocusCandidate()
            selectCandidate(word)
        }
    }

    /**
     * Handle a long click event on the candidate.
     * @param v  View
     * @param list  List of candidates
     */
    fun onLongClickCandidate(v: View, list: ArrayList<WnnWord>): Boolean {
        if (mViewLongPressDialog == null) {
            return false
        }

        if (mIsSymbolMode) {
            return false
        }

        if (!mEnableCandidateLongClick) {
            return false
        }

        if (!v.isShown) {
            return true
        }

        val d = v.background
        if (d != null) {
            if (d.state.size == 0) {
                return true
            }
        }

        val wordcount = (v as TextView).id
        mWord = list[wordcount]
        clearFocusCandidate()
        displayDialog(v, mWord!!)

        return true
    }

    /**
     * Set auto-hide mode.
     * @param hide      `true` if the view will hidden when no candidate exists;
     * `false` if the view is always shown.
     */
    fun setAutoHide(hide: Boolean) {
        mAutoHideMode = hide
    }

    /** @see CandidatesViewManager.initView
     */
    override fun initView(parent: OpenWnn, width: Int, height: Int): View? {
        mWnn = parent
        mViewWidth = width
        val r = mWnn!!.resources
        candidateMinimumWidth = r.getDimensionPixelSize(R.dimen.cand_minimum_width)
        candidateMinimumHeight = r.getDimensionPixelSize(R.dimen.cand_minimum_height)
        if (OpenWnn.Companion.isXLarge()) {
            mCandidateOneLineMinimumHeight =
                r.getDimensionPixelSize(R.dimen.candidate_layout_height)
        }
        mCandidateCategoryMinimumHeight =
            r.getDimensionPixelSize(R.dimen.cand_category_minimum_height)
        mCandidateLeftAlignThreshold = r.getDimensionPixelSize(R.dimen.cand_left_align_threshold)
        mKeyboardHeight = r.getDimensionPixelSize(R.dimen.keyboard_height)
        if (OpenWnn.Companion.isXLarge()) {
            mKeyboardHeight += (Math.round(height * KEYBOARD_VERTICAL_GAP)
                    * KEYBOARD_VERTICAL_GAP_COUNT)
        }
        mSymbolKeyboardHeight = r.getDimensionPixelSize(R.dimen.symbol_keyboard_height)
        val d = r.getDrawable(R.drawable.tab_no_select)
        mSymbolKeyboardTabHeight = d.minimumHeight

        mPortrait =
            (r.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE)

        mCandNormalTextSize = r.getDimensionPixelSize(R.dimen.cand_normal_text_size).toFloat()
        mCandCategoryTextSize = r.getDimensionPixelSize(R.dimen.cand_category_text_size).toFloat()

        val inflater = parent.layoutInflater
        mViewBody = inflater.inflate(R.layout.candidates, null) as ViewGroup

        mViewTabSymbol = mViewBody!!.findViewById<View>(R.id.candview_symbol) as TextView
        mViewTabEmoticon = mViewBody!!.findViewById<View>(R.id.candview_emoticon) as TextView

        mViewBodyScroll = mViewBody!!.findViewById<View>(R.id.candview_scroll) as ScrollView

        mViewCandidateBase = mViewBody!!.findViewById<View>(R.id.candview_base) as ViewGroup

        setNumeberOfDisplayLines()
        createNormalCandidateView()
        mViewCandidateList2nd =
            mViewBody!!.findViewById<View>(R.id.candidates_2nd_view) as AbsoluteLayout

        mTextColor = r.getColor(R.color.candidate_text)

        mReadMoreButton = mViewBody!!.findViewById<View>(R.id.read_more_button) as ImageView
        mReadMoreButton!!.setOnTouchListener { v, event ->
            var resid = 0
            when (event.action) {
                MotionEvent.ACTION_DOWN -> resid = if (mIsFullView) {
                    R.drawable.cand_up_press
                } else {
                    R.drawable.cand_down_press
                }

                MotionEvent.ACTION_UP -> resid = if (mIsFullView) {
                    R.drawable.cand_up
                } else {
                    R.drawable.cand_down
                }

                else -> {}
            }

            if (resid != 0) {
                mReadMoreButton!!.setImageResource(resid)
            }
            false
        }
        mReadMoreButton!!.setOnClickListener(View.OnClickListener { v ->
            if (!v.isShown) {
                return@OnClickListener
            }
            playSoundAndVibration()
            if (mIsFullView) {
                mIsFullView = false
                mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.LIST_CANDIDATES_NORMAL))
            } else {
                mIsFullView = true
                mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.LIST_CANDIDATES_FULL))
            }
        })

        viewType = CandidatesViewManager.Companion.VIEW_TYPE_CLOSE

        mGestureDetector = GestureDetector(this)

        mViewLongPressDialog = inflater.inflate(R.layout.candidate_longpress_dialog, null) as View

        /* select button */
        var longPressDialogButton =
            mViewLongPressDialog!!.findViewById<View>(R.id.candidate_longpress_dialog_select) as Button
        longPressDialogButton.setOnClickListener {
            playSoundAndVibration()
            clearFocusCandidate()
            selectCandidate(mWord)
            closeDialog()
        }

        /* cancel button */
        longPressDialogButton =
            mViewLongPressDialog!!.findViewById<View>(R.id.candidate_longpress_dialog_cancel) as Button
        longPressDialogButton.setOnClickListener {
            playSoundAndVibration()
            mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.LIST_CANDIDATES_NORMAL))
            mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.UPDATE_CANDIDATE))
            closeDialog()
        }

        return mViewBody
    }

    /**
     * Create the normal candidate view
     */
    private fun createNormalCandidateView() {
        mViewCandidateList1st =
            mViewBody!!.findViewById<View>(R.id.candidates_1st_view) as LinearLayout
        mViewCandidateList1st!!.setOnClickListener(mCandidateOnClick1st)

        mViewCandidateListTab = mViewBody!!.findViewById<View>(R.id.candview_tab) as LinearLayout
        val tSymbol = mViewTabSymbol
        tSymbol!!.setOnClickListener(mTabOnClick)
        val tEmoticon = mViewTabEmoticon
        tEmoticon!!.setOnClickListener(mTabOnClick)

        val line = SETTING_NUMBER_OF_LINEMAX
        val width = mViewWidth
        for (i in 0 until line) {
            val lineView = LinearLayout(mViewBodyScroll!!.context)
            lineView.orientation = LinearLayout.HORIZONTAL
            var layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            lineView.layoutParams = layoutParams
            for (j in 0 until (width / candidateMinimumWidth)) {
                val tv = createCandidateView()
                lineView.addView(tv)
            }

            if (i == 0) {
                val tv = createCandidateView()
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                layoutParams.weight = 0f
                layoutParams.gravity = Gravity.RIGHT
                tv.layoutParams = layoutParams

                lineView.addView(tv)
                mViewCandidateTemplate = tv
            }
            mViewCandidateList1st!!.addView(lineView)
        }
    }

    override val currentView: View?
        /** @see CandidatesViewManager.getCurrentView
         */
        get() = mViewBody

    /**
     * Set the view layout
     *
     * @param type      View type
     * @return          `true` if display is updated; `false` if otherwise
     */
    private fun setViewLayout(type: Int): Boolean {
        val params: ViewGroup.LayoutParams
        val line = if ((mPortrait)) mPortraitNumberOfLine else mLandscapeNumberOfLine

        if ((mViewType == CandidatesViewManager.Companion.VIEW_TYPE_FULL)
            && (type == CandidatesViewManager.Companion.VIEW_TYPE_NORMAL)
        ) {
            clearFocusCandidate()
        }

        mViewType = type

        when (type) {
            CandidatesViewManager.Companion.VIEW_TYPE_CLOSE -> {
                params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    candidateMinimumHeight * line
                )
                mViewBodyScroll!!.layoutParams = params
                mViewCandidateListTab!!.visibility = View.GONE
                mViewCandidateBase!!.minimumHeight = -1
                mHandler.removeMessages(MSG_SET_CANDIDATES)
                return false
            }

            CandidatesViewManager.Companion.VIEW_TYPE_NORMAL -> {
                params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    candidateMinimumHeight * line
                )
                mViewBodyScroll!!.layoutParams = params
                mViewBodyScroll!!.scrollTo(0, 0)
                mViewCandidateListTab!!.visibility = View.GONE
                mViewCandidateList1st!!.visibility = View.VISIBLE
                mViewCandidateList2nd!!.visibility = View.GONE
                mViewCandidateBase!!.minimumHeight = -1
                return false
            }

            CandidatesViewManager.Companion.VIEW_TYPE_FULL -> {
                params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    candidateViewHeight
                )
                mViewBodyScroll!!.layoutParams = params
                if (mIsSymbolMode) {
                    updateSymbolType()
                    mViewCandidateListTab!!.visibility = View.VISIBLE
                } else {
                    mViewCandidateListTab!!.visibility = View.GONE
                }
                mViewCandidateList2nd!!.visibility = View.VISIBLE
                mViewCandidateBase!!.minimumHeight = -1
                return true
            }

            else -> {
                params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    candidateViewHeight
                )
                mViewBodyScroll!!.layoutParams = params
                if (mIsSymbolMode) {
                    updateSymbolType()
                    mViewCandidateListTab!!.visibility = View.VISIBLE
                } else {
                    mViewCandidateListTab!!.visibility = View.GONE
                }
                mViewCandidateList2nd!!.visibility = View.VISIBLE
                mViewCandidateBase!!.minimumHeight = -1
                return true
            }
        }
    }

    override var viewType: Int
        /** @see CandidatesViewManager.getViewType
         */
        get() = mViewType
        /** @see CandidatesViewManager.setViewType
         */
        set(type) {
            val readMore = setViewLayout(type)

            if (readMore) {
                displayCandidates(this.mConverter, false, -1)
            } else {
                if (type == CandidatesViewManager.Companion.VIEW_TYPE_NORMAL) {
                    mIsFullView = false
                    if (mDisplayEndOffset > 0) {
                        val maxLine = maxLine
                        displayCandidates(this.mConverter, false, maxLine)
                    } else {
                        setReadMore()
                    }
                } else {
                    if (mViewBody!!.isShown) {
                        mWnn!!.setCandidatesViewShown(false)
                    }
                }
            }
        }

    /** @see CandidatesViewManager.displayCandidates
     */
    override fun displayCandidates(converter: WnnEngine?) {
        mHandler.removeMessages(MSG_SET_CANDIDATES)

        if (mIsSymbolSelected) {
            mIsSymbolSelected = false
            if (mSymbolMode == OpenWnnJAJP.Companion.ENGINE_MODE_SYMBOL_KAO_MOJI) {
                return
            }

            val prevLineCount = mLineCount
            val prevWordCount1st = mWordCount1st
            clearNormalViewCandidate()
            mWordCount1st = 0
            mLineCount = 1
            mLineLength = 0
            mNormalViewWordCountOfLine = 0
            mWnnWordArray1st.clear()
            mTextViewArray1st.clear()
            if (((prevWordCount1st == 0) && (mWordCount1st == 1)) ||
                (prevLineCount < mLineCount)
            ) {
                mViewBodyScroll!!.scrollTo(0, mViewBodyScroll!!.scrollY + candidateMinimumHeight)
            }
            if (isFocusCandidate && mHasFocusedArray1st) {
                mCurrentFocusIndex = 0
                val m = mHandler.obtainMessage(MSG_MOVE_FOCUS, 0, 0)
                mHandler.sendMessage(m)
            }
            return
        }

        mCanReadMore = false
        mDisplayEndOffset = 0
        mIsFullView = false
        mFullViewWordCount = 0
        mFullViewOccupyCount = 0
        mFullViewPrevLineTopId = 0
        mFullViewPrevView = null
        mCreateCandidateDone = false
        mNormalViewWordCountOfLine = 0

        clearCandidates()
        mConverter = converter
        mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.LIST_CANDIDATES_NORMAL))

        mViewCandidateTemplate!!.visibility = View.VISIBLE
        mViewCandidateTemplate!!.setBackgroundResource(R.drawable.cand_back)

        displayCandidates(converter, true, maxLine)

        if (mIsSymbolMode) {
            mIsFullView = true
            mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.LIST_CANDIDATES_FULL))
        }
    }

    private val maxLine: Int
        /** @see CandidatesViewManager.getMaxLine
         */
        get() {
            val maxLine =
                if ((mPortrait)) mPortraitNumberOfLine else mLandscapeNumberOfLine
            return maxLine
        }

    /**
     * Get categories text.
     * @param String Source string replacement
     * @return String Categories text
     */
    private fun getCategoriesText(categoriesString: String): String {
        var ret: String? = null

        val r = mWnn!!.resources
        ret = if (categoriesString == r.getString(R.string.half_symbol_categories_txt)) {
            r.getString(R.string.half_symbol_txt)
        } else if (categoriesString == r.getString(R.string.full_symbol_categories_txt)) {
            r.getString(R.string.full_symbol_txt)
        } else {
            ""
        }

        return ret
    }

    /**
     * Display the candidates.
     *
     * @param converter  [WnnEngine] which holds candidates.
     * @param dispFirst  Whether it is the first time displaying the candidates
     * @param maxLine    The maximum number of displaying lines
     */
    private fun displayCandidates(converter: WnnEngine?, dispFirst: Boolean, maxLine: Int) {
        var maxLine = maxLine
        if (converter == null) {
            return
        }

        /* Concatenate the candidates already got and the last one in dispFirst mode */
        var displayLimit = mDisplayLimit

        var isDelay = false
        var isBreak = false

        if (converter is SymbolList) {
            if (!dispFirst) {
                if (maxLine == -1) {
                    isDelay = true
                    maxLine = mLineCount + SET_CANDIDATE_FIRST_LINE_COUNT

                    mHandler.sendEmptyMessageDelayed(
                        MSG_SET_CANDIDATES,
                        SET_CANDIDATE_DELAY.toLong()
                    )
                } else if (maxLine == SET_CANDIDATE_DELAY_LINE_COUNT) {
                    isDelay = true
                    maxLine = mLineCount + SET_CANDIDATE_DELAY_LINE_COUNT

                    mHandler.sendEmptyMessageDelayed(
                        MSG_SET_CANDIDATES,
                        SET_CANDIDATE_DELAY.toLong()
                    )
                }
            }
            if (mSymbolMode != OpenWnnJAJP.Companion.ENGINE_MODE_SYMBOL_KAO_MOJI) {
                displayLimit = -1
            }
        }

        /* Get candidates */
        var result: WnnWord? = null
        var prevCandidate: String? = null
        while ((displayLimit == -1 || wordCount < displayLimit)) {
            for (i in 0 until DISPLAY_LINE_MAX_COUNT) {
                result = converter.nextCandidate
                if (result == null) {
                    break
                }

                if (converter is SymbolList) {
                    break
                }

                if ((prevCandidate == null) || prevCandidate != result.candidate) {
                    break
                }
            }

            if (result == null) {
                break
            } else {
                prevCandidate = result.candidate
            }

            if (converter is SymbolList) {
                if (isCategory(result)) {
                    if (wordCount != 0) {
                        createNextLine(false)
                    }
                    result.candidate = getCategoriesText(result.candidate!!)
                    setCandidate(true, result)
                    createNextLine(true)
                    continue
                }
            }

            setCandidate(false, result)

            if ((dispFirst || isDelay) && (maxLine < mLineCount)) {
                mCanReadMore = true
                isBreak = true
                break
            }
        }

        if (!isBreak && !mCreateCandidateDone) {
            /* align left if necessary */
            createNextLine(false)
            mCreateCandidateDone = true
            mHandler.removeMessages(MSG_SET_CANDIDATES)
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SELECT_CANDIDATES))
        }

        if (wordCount < 1) { /* no candidates */
            if (mAutoHideMode) {
                mWnn!!.setCandidatesViewShown(false)
                return
            } else {
                mCanReadMore = false
                mIsFullView = false
                setViewLayout(CandidatesViewManager.Companion.VIEW_TYPE_NORMAL)
            }
        }

        setReadMore()

        if (!(mViewBody!!.isShown)) {
            mWnn!!.setCandidatesViewShown(true)
        }
        return
    }

    /**
     * Add a candidate into the list.
     * @param isCategory  `true`:caption of category, `false`:normal word
     * @param word        A candidate word
     */
    private fun setCandidate(isCategory: Boolean, word: WnnWord) {
        var textLength = measureText(word.candidate, 0, word.candidate!!.length)
        val template = mViewCandidateTemplate
        textLength += template!!.paddingLeft + template.paddingRight
        var maxWidth = mViewWidth
        var isEmojiSymbol = false
        if (mIsSymbolMode && (word.candidate!!.length < 3)) {
            isEmojiSymbol = true
        }
        var textView: TextView

        val is2nd = isFirstListOver(mIsFullView, mLineCount, word)
        if (is2nd) {
            /* Full view */
            val viewDivison = candidateViewDivison
            val indentWidth = mViewWidth / viewDivison
            var occupyCount = min(
                ((textLength + indentWidth + getCandidateSpaceWidth(isEmojiSymbol)) / indentWidth).toDouble(),
                viewDivison.toDouble()
            ).toInt()
            if (isCategory) {
                occupyCount = viewDivison
            }

            if (viewDivison < (mFullViewOccupyCount + occupyCount)) {
                if (viewDivison != mFullViewOccupyCount) {
                    mFullViewPrevParams!!.width += (viewDivison - mFullViewOccupyCount) * indentWidth
                    if (mFullViewPrevView != null) {
                        mViewCandidateList2nd!!.updateViewLayout(
                            mFullViewPrevView,
                            mFullViewPrevParams
                        )
                    }
                }
                mFullViewOccupyCount = 0
                if (mFullViewPrevView != null) {
                    mFullViewPrevLineTopId = mFullViewPrevView!!.id
                }
                mLineCount++
                mLineY += if (isCategory) {
                    mCandidateCategoryMinimumHeight
                } else {
                    candidateMinimumHeight
                }
            }
            if (mFullViewWordCount == 0) {
                mLineY = 0
            }

            val layout: ViewGroup? = mViewCandidateList2nd

            val width = indentWidth * occupyCount
            var height = candidateMinimumHeight
            if (isCategory) {
                height = mCandidateCategoryMinimumHeight
            }

            val params = buildLayoutParams(mViewCandidateList2nd, width, height)

            textView = layout!!.getChildAt(mFullViewWordCount) as TextView
            if (textView == null) {
                textView = createCandidateView()
                textView.layoutParams = params

                mViewCandidateList2nd!!.addView(textView)
            } else {
                mViewCandidateList2nd!!.updateViewLayout(textView, params)
            }

            mFullViewOccupyCount += occupyCount
            mFullViewWordCount++
            mFullViewPrevView = textView
            mFullViewPrevParams = params
        } else {
            val viewDivison = candidateViewDivison
            val indentWidth = mViewWidth / viewDivison
            textLength = max(textLength.toDouble(), indentWidth.toDouble()).toInt()

            /* Normal view */
            var nextEnd = mLineLength + textLength
            nextEnd += getCandidateSpaceWidth(isEmojiSymbol)

            if (mLineCount == 1 && !mIsSymbolMode) {
                maxWidth -= candidateMinimumWidth
            }

            if ((maxWidth < nextEnd) && (wordCount != 0)) {
                createNextLineFor1st()
                if (maxLine < mLineCount) {
                    mLineLength = 0
                    /* Call this method again to add the candidate in the full view */
                    if (!mIsSymbolSelected) {
                        setCandidate(isCategory, word)
                    }
                    return
                }

                mLineLength = textLength
                mLineLength += getCandidateSpaceWidth(isEmojiSymbol)
            } else {
                mLineLength = nextEnd
            }

            val lineView = mViewCandidateList1st!!.getChildAt(mLineCount - 1) as LinearLayout
            textView = lineView.getChildAt(mNormalViewWordCountOfLine) as TextView

            if (isCategory) {
                if (mLineCount == 1) {
                    mViewCandidateTemplate!!.setBackgroundDrawable(null)
                }
                mLineLength += mCandidateLeftAlignThreshold
            } else {
                var CompareLength = textLength
                CompareLength += getCandidateSpaceWidth(isEmojiSymbol)
            }

            mNormalViewWordCountOfLine++
        }

        textView.text = word.candidate
        if (is2nd) {
            textView.id = mWordCount2nd
        } else {
            textView.id = mWordCount1st
        }
        textView.visibility = View.VISIBLE
        textView.isPressed = false
        textView.isFocusable = false

        if (isCategory) {
            textView.text = "      " + word.candidate

            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mCandCategoryTextSize)
            textView.setBackgroundDrawable(null)
            textView.gravity = Gravity.CENTER_VERTICAL
            textView.minHeight = mCandidateCategoryMinimumHeight
            textView.height = mCandidateCategoryMinimumHeight

            textView.setOnClickListener(null)
            textView.setOnLongClickListener(null)
            textView.setTextColor(mTextColor)
        } else {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mCandNormalTextSize)
            textView.gravity = Gravity.CENTER
            textView.minHeight = candidateMinimumHeight
            textView.height = candidateMinimumHeight

            if (is2nd) {
                textView.setOnClickListener(mCandidateOnClick2nd)
                textView.setOnLongClickListener(mCandidateOnLongClick2nd)
            } else {
                textView.setOnClickListener(mCandidateOnClick1st)
                textView.setOnLongClickListener(mCandidateOnLongClick1st)
            }

            textView.setBackgroundResource(R.drawable.cand_back)

            textView.setTextColor(mTextColor)
        }

        if (maxWidth < textLength) {
            textView.ellipsize = TextUtils.TruncateAt.END
        } else {
            textView.ellipsize = null
        }

        var span: ImageSpan? = null
        if (word.candidate == " ") {
            span = ImageSpan(
                mWnn!!, R.drawable.word_half_space,
                DynamicDrawableSpan.ALIGN_BASELINE
            )
        } else if (word.candidate == "\u3000") {
            span = ImageSpan(
                mWnn!!, R.drawable.word_full_space,
                DynamicDrawableSpan.ALIGN_BASELINE
            )
        }

        if (span != null) {
            val spannable = SpannableString("   ")
            spannable.setSpan(span, 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            textView.text = spannable
        }
        textView.setPadding(0, 0, 0, 0)

        if (is2nd) {
            mWnnWordArray2nd.add(mWordCount2nd, word)
            mWordCount2nd++
            mTextViewArray2nd.add(textView)
        } else {
            mWnnWordArray1st.add(mWordCount1st, word)
            mWordCount1st++
            mTextViewArray1st.add(textView)
        }
    }

    /**
     * Create AbsoluteLayout.LayoutParams
     * @param layout AbsoluteLayout
     * @param width  The width of the display
     * @param height The height of the display
     * @return Layout parameter
     */
    private fun buildLayoutParams(
        layout: AbsoluteLayout?,
        width: Int,
        height: Int
    ): ViewGroup.LayoutParams {
        val viewDivison = candidateViewDivison
        val indentWidth = mViewWidth / viewDivison
        val x = indentWidth * mFullViewOccupyCount
        val y = mLineY
        val params
                : ViewGroup.LayoutParams = AbsoluteLayout.LayoutParams(width, height, x, y)

        return params
    }

    /**
     * Create a view for a candidate.
     * @return the view
     */
    private fun createCandidateView(): TextView {
        val text: TextView = CandidateTextView(mViewBodyScroll!!.context)
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, mCandNormalTextSize)
        text.setBackgroundResource(R.drawable.cand_back)
        text.compoundDrawablePadding = 0
        text.gravity = Gravity.CENTER
        text.setSingleLine()
        text.setPadding(0, 0, 0, 0)
        text.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        text.minHeight = candidateMinimumHeight
        text.minimumWidth = candidateMinimumWidth
        text.isSoundEffectsEnabled = false
        return text
    }

    /**
     * Display `mReadMoreText` if there are more candidates.
     */
    private fun setReadMore() {
        if (mIsSymbolMode) {
            mReadMoreButton!!.visibility = View.GONE
            mViewCandidateTemplate!!.visibility = View.GONE
            return
        }

        var resid = 0

        if (mIsFullView) {
            mReadMoreButton!!.visibility = View.VISIBLE
            resid = R.drawable.cand_up
        } else {
            if (mCanReadMore) {
                mReadMoreButton!!.visibility = View.VISIBLE
                resid = R.drawable.cand_down
            } else {
                mReadMoreButton!!.visibility = View.GONE
                mViewCandidateTemplate!!.visibility = View.GONE
            }
        }

        if (resid != 0) {
            mReadMoreButton!!.setImageResource(resid)
        }
    }

    /**
     * Clear the list of the normal candidate view.
     */
    private fun clearNormalViewCandidate() {
        val candidateList = mViewCandidateList1st
        val lineNum = candidateList!!.childCount
        for (i in 0 until lineNum) {
            val lineView = candidateList.getChildAt(i) as LinearLayout
            val size = lineView.childCount
            for (j in 0 until size) {
                val v = lineView.getChildAt(j)
                v.visibility = View.GONE
            }
        }
    }

    /** @see CandidatesViewManager.clearCandidates
     */
    override fun clearCandidates() {
        closeDialog()
        clearFocusCandidate()
        clearNormalViewCandidate()

        val layout: ViewGroup? = mViewCandidateList2nd
        val size = layout!!.childCount
        for (i in 0 until size) {
            val v = layout.getChildAt(i)
            v.visibility = View.GONE
        }

        mLineCount = 1
        mWordCount1st = 0
        mWordCount2nd = 0
        mWnnWordArray1st.clear()
        mWnnWordArray2nd.clear()
        mTextViewArray1st.clear()
        mTextViewArray2nd.clear()
        mLineLength = 0

        mLineY = 0

        mIsFullView = false
        mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.LIST_CANDIDATES_NORMAL))
        if (mAutoHideMode) {
            setViewLayout(CandidatesViewManager.Companion.VIEW_TYPE_CLOSE)
        }

        if (mAutoHideMode && mViewBody!!.isShown) {
            mWnn!!.setCandidatesViewShown(false)
        }
        mCanReadMore = false
        setReadMore()
    }

    /** @see CandidatesViewManager.setPreferences
     */
    override fun setPreferences(pref: SharedPreferences) {
        try {
            mVibrator = if (pref.getBoolean("key_vibration", false)) {
                mWnn!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            } else {
                null
            }
            mSound = if (pref.getBoolean("key_sound", false)) {
                mWnn!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            } else {
                null
            }
            setNumeberOfDisplayLines()
        } catch (ex: Exception) {
            Log.e("OpenWnn", "NO VIBRATOR")
        }
    }

    /**
     * Set normal mode.
     */
    fun setNormalMode() {
        setReadMore()
        mIsFullView = false
        mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.LIST_CANDIDATES_NORMAL))
    }

    /**
     * Set full mode.
     */
    fun setFullMode() {
        mIsFullView = true
        mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.LIST_CANDIDATES_FULL))
    }

    /**
     * Set symbol mode.
     */
    fun setSymbolMode(enable: Boolean, mode: Int) {
        if (mIsSymbolMode && !enable) {
            viewType = CandidatesViewManager.Companion.VIEW_TYPE_CLOSE
        }
        mSymbolMode = mode
        mIsSymbolMode = enable
    }

    /**
     * Set scroll up.
     */
    fun setScrollUp() {
        if (!mViewBodyScroll!!.pageScroll(ScrollView.FOCUS_UP)) {
            mViewBodyScroll!!.scrollTo(0, mViewBodyScroll!!.getChildAt(0).height)
        }
    }

    /**
     * Set scroll down.
     */
    fun setScrollDown() {
        if (!mViewBodyScroll!!.pageScroll(ScrollView.FOCUS_DOWN)) {
            mViewBodyScroll!!.scrollTo(0, 0)
        }
    }

    /**
     * Set scroll full up.
     */
    fun setScrollFullUp() {
        if (!mViewBodyScroll!!.fullScroll(ScrollView.FOCUS_UP)) {
            mViewBodyScroll!!.scrollTo(0, mViewBodyScroll!!.getChildAt(0).height)
        }
    }

    /**
     * Set scroll full down.
     */
    fun setScrollFullDown() {
        if (!mViewBodyScroll!!.fullScroll(ScrollView.FOCUS_DOWN)) {
            mViewBodyScroll!!.scrollTo(0, 0)
        }
    }

    /**
     * Process [OpenWnnEvent.CANDIDATE_VIEW_TOUCH] event.
     *
     * @return      `true` if event is processed; `false` if otherwise
     */
    fun onTouchSync(): Boolean {
        return mGestureDetector!!.onTouchEvent(mMotionEvent!!)
    }

    /**
     * Select a candidate.
     * <br></br>
     * This method notices the selected word to [OpenWnn].
     *
     * @param word  The selected word
     */
    private fun selectCandidate(word: WnnWord?) {
        if (!mIsSymbolMode) {
            mIsFullView = false
            mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.LIST_CANDIDATES_NORMAL))
        }
        mIsSymbolSelected = mIsSymbolMode
        mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.SELECT_CANDIDATE, word))
    }

    private fun playSoundAndVibration() {
        if (mVibrator != null) {
            try {
                mVibrator!!.vibrate(5)
            } catch (ex: Exception) {
                Log.e(
                    "OpenWnn",
                    "TextCandidatesViewManager::selectCandidate Vibrator $ex"
                )
            }
        }
        if (mSound != null) {
            try {
                mSound!!.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1f)
            } catch (ex: Exception) {
                Log.e(
                    "OpenWnn",
                    "TextCandidatesViewManager::selectCandidate Sound $ex"
                )
            }
        }
    }

    /** @see android.view.GestureDetector.OnGestureListener.onDown
     */
    override fun onDown(arg0: MotionEvent): Boolean {
        return false
    }

    /** @see android.view.GestureDetector.OnGestureListener.onFling
     */
    override fun onFling(
        arg0: MotionEvent?,
        arg1: MotionEvent?,
        arg2: Float,
        arg3: Float
    ): Boolean {
        var consumed = false
        if (arg1 != null && arg0 != null && arg1.y < arg0.y) {
            if ((mViewType == CandidatesViewManager.Companion.VIEW_TYPE_NORMAL) && mCanReadMore) {
                if (mVibrator != null) {
                    try {
                        mVibrator!!.vibrate(5)
                    } catch (ex: Exception) {
                        Log.e(
                            "iwnn",
                            "TextCandidatesViewManager::onFling Vibrator $ex"
                        )
                    }
                }
                mIsFullView = true
                mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.LIST_CANDIDATES_FULL))
                consumed = true
            }
        } else {
            if (mViewBodyScroll!!.scrollY == 0) {
                if (mVibrator != null) {
                    try {
                        mVibrator!!.vibrate(5)
                    } catch (ex: Exception) {
                        Log.e("iwnn", "TextCandidatesViewManager::onFling Sound $ex")
                    }
                }
                mIsFullView = false
                mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.LIST_CANDIDATES_NORMAL))
                consumed = true
            }
        }

        return consumed
    }

    /** @see android.view.GestureDetector.OnGestureListener.onLongPress
     */
    override fun onLongPress(arg0: MotionEvent) {
        return
    }

    /** @see android.view.GestureDetector.OnGestureListener.onScroll
     */
    override fun onScroll(
        arg0: MotionEvent?,
        arg1: MotionEvent,
        arg2: Float,
        arg3: Float
    ): Boolean {
        return false
    }

    /** @see android.view.GestureDetector.OnGestureListener.onShowPress
     */
    override fun onShowPress(arg0: MotionEvent) {
    }

    /** @see android.view.GestureDetector.OnGestureListener.onSingleTapUp
     */
    override fun onSingleTapUp(arg0: MotionEvent): Boolean {
        return false
    }

    /**
     * Retrieve the width of string to draw.
     *
     * @param text          The string
     * @param start         The start position (specified by the number of character)
     * @param end           The end position (specified by the number of character)
     * @return          The width of string to draw
     */
    fun measureText(text: CharSequence?, start: Int, end: Int): Int {
        if (end - start < 3) {
            return candidateMinimumWidth
        }

        val paint = mViewCandidateTemplate!!.paint
        return paint.measureText(text, start, end).toInt()
    }

    /**
     * Create a layout for the next line.
     */
    private fun createNextLine(isCategory: Boolean) {
        if (isFirstListOver(mIsFullView, mLineCount, null)) {
            /* Full view */
            mFullViewOccupyCount = 0
            if (mFullViewPrevView != null) {
                mFullViewPrevLineTopId = mFullViewPrevView!!.id
            }
            mLineY += if (isCategory) {
                mCandidateCategoryMinimumHeight
            } else {
                candidateMinimumHeight
            }
            mLineCount++
        } else {
            createNextLineFor1st()
        }
    }

    /**
     * Create a layout for the next line.
     */
    private fun createNextLineFor1st() {
        val lineView = mViewCandidateList1st!!.getChildAt(mLineCount - 1) as LinearLayout
        var weight = 0f
        if (mLineLength < mCandidateLeftAlignThreshold) {
            if (mLineCount == 1) {
                mViewCandidateTemplate!!.visibility = View.GONE
            }
        } else {
            weight = 1.0f
        }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            weight
        )

        val child = lineView.childCount
        for (i in 0 until child) {
            val view = lineView.getChildAt(i)

            if (view !== mViewCandidateTemplate) {
                view.layoutParams = params
                view.setPadding(0, 0, 0, 0)
            }
        }

        mLineLength = 0
        mNormalViewWordCountOfLine = 0
        mLineCount++
    }

    /**
     * Judge if it's a category.
     *
     * @return `true` if category
     */
    fun isCategory(word: WnnWord): Boolean {
        val length = word.candidate!!.length
        return ((length > 3) && (word.candidate!![0] == '['))
    }

    private val candidateViewHeight: Int
        /**
         * Get a height of a candidate view.
         *
         * @return the height of a candidate view.
         */
        get() {
            if (OpenWnn.Companion.isXLarge()) {
                return (mKeyboardHeight + mCandidateOneLineMinimumHeight - mSymbolKeyboardHeight
                        - mSymbolKeyboardTabHeight)
            } else {
                val numberOfLine =
                    if ((mPortrait)) mPortraitNumberOfLine else mLandscapeNumberOfLine
                val resource = mWnn!!.resources
                val keyboardBackground =
                    resource.getDrawable(R.drawable.keyboard_background)
                val keyboardPadding = Rect(0, 0, 0, 0)
                keyboardBackground.getPadding(keyboardPadding)
                val keyboardTotalPadding = keyboardPadding.top + keyboardPadding.bottom
                return if (mIsSymbolMode) {
                    (mKeyboardHeight + numberOfLine * candidateMinimumHeight - mSymbolKeyboardHeight - mSymbolKeyboardTabHeight)
                } else if (!mHardKeyboardHidden) {
                    (mKeyboardHeight + numberOfLine * candidateMinimumHeight
                            - mSymbolKeyboardHeight)
                } else {
                    (mKeyboardHeight + keyboardTotalPadding
                            + numberOfLine * candidateMinimumHeight)
                }
            }
        }

    /**
     * Update symbol type.
     */
    private fun updateSymbolType() {
        when (mSymbolMode) {
            OpenWnnJAJP.Companion.ENGINE_MODE_SYMBOL -> {
                updateTabStatus(mViewTabSymbol!!, true, true)
                updateTabStatus(mViewTabEmoticon!!, mEnableEmoticon, false)
            }

            OpenWnnJAJP.Companion.ENGINE_MODE_SYMBOL_KAO_MOJI -> {
                updateTabStatus(mViewTabSymbol!!, true, false)
                updateTabStatus(mViewTabEmoticon!!, mEnableEmoticon, true)
            }

            else -> {
                updateTabStatus(mViewTabSymbol!!, true, false)
                updateTabStatus(mViewTabEmoticon!!, mEnableEmoticon, false)
            }
        }
    }

    /**
     * Update tab status.
     *
     * @param tab           The tab view.
     * @param enabled       The tab is enabled.
     * @param selected      The tab is selected.
     */
    private fun updateTabStatus(tab: TextView, enabled: Boolean, selected: Boolean) {
        tab.visibility = View.VISIBLE
        tab.isEnabled = enabled
        var backgroundId = 0
        var colorId = 0
        if (enabled) {
            if (selected) {
                backgroundId = R.drawable.cand_tab
                colorId = R.color.tab_textcolor_select
            } else {
                backgroundId = R.drawable.cand_tab_noselect
                colorId = R.color.tab_textcolor_no_select
            }
        } else {
            backgroundId = R.drawable.cand_tab_noselect
            colorId = R.color.tab_textcolor_disable
        }
        tab.setBackgroundResource(backgroundId)
        tab.setTextColor(mWnn!!.resources.getColor(colorId))
    }

    private val candidateViewDivison: Int
        /**
         * Get candidate number of division.
         * @return Number of division
         */
        get() {
            val viewDivison: Int

            if (mIsSymbolMode) {
                val mode = mSymbolMode
                viewDivison = when (mode) {
                    OpenWnnJAJP.Companion.ENGINE_MODE_SYMBOL -> if ((mPortrait)) FULL_VIEW_SYMBOL_DIV_PORT else FULL_VIEW_SYMBOL_DIV_LAND

                    OpenWnnJAJP.Companion.ENGINE_MODE_SYMBOL_KAO_MOJI -> FULL_VIEW_DIV

                    else -> FULL_VIEW_DIV
                }
            } else {
                viewDivison = FULL_VIEW_DIV
            }
            return viewDivison
        }

    private val wordCount: Int
        /**
         * @return Word count
         */
        get() = mWordCount1st + mWordCount2nd

    /**
     * @return Add second
     */
    private fun isFirstListOver(isFullView: Boolean, lineCount: Int, word: WnnWord?): Boolean {
        return if (mIsSymbolMode) {
            when (mSymbolMode) {
                OpenWnnJAJP.Companion.ENGINE_MODE_SYMBOL_KAO_MOJI -> true
                OpenWnnJAJP.Companion.ENGINE_MODE_SYMBOL -> true
                else -> (isFullView || maxLine < lineCount)
            }
        } else {
            (isFullView || maxLine < lineCount)
        }
    }

    /**
     * @return Candidate space width
     */
    private fun getCandidateSpaceWidth(isEmojiSymbol: Boolean): Int {
        val r = mWnn!!.resources
        return if (mPortrait) {
            if (isEmojiSymbol) {
                0
            } else {
                r.getDimensionPixelSize(R.dimen.cand_space_width)
            }
        } else {
            if (isEmojiSymbol) {
                r.getDimensionPixelSize(R.dimen.cand_space_width_emoji_symbol)
            } else {
                r.getDimensionPixelSize(R.dimen.cand_space_width)
            }
        }
    }

    /**
     * KeyEvent action for the candidate view.
     *
     * @param key    Key event
     */
    override fun processMoveKeyEvent(key: Int) {
        if (!mViewBody!!.isShown) {
            return
        }

        when (key) {
            KeyEvent.KEYCODE_DPAD_UP -> moveFocus(-1, true)
            KeyEvent.KEYCODE_DPAD_DOWN -> moveFocus(1, true)
            KeyEvent.KEYCODE_DPAD_LEFT -> moveFocus(-1, false)
            KeyEvent.KEYCODE_DPAD_RIGHT -> moveFocus(1, false)
            else -> {}
        }
    }

    override val isFocusCandidate: Boolean
        /**
         * Get a flag candidate is focused now.
         *
         * @return the Candidate is focused of a flag.
         */
        get() {
            if (mCurrentFocusIndex != FOCUS_NONE) {
                return true
            }
            return false
        }

    /**
     * Give focus to View of candidate.
     */
    fun setViewStatusOfFocusedCandidate() {
        val view = mFocusedView
        if (view != null) {
            view.setBackgroundDrawable(mFocusedViewBackground)
            view.setPadding(0, 0, 0, 0)
        }

        val v = focusedView
        mFocusedView = v
        if (v != null) {
            mFocusedViewBackground = v.background
            v.setBackgroundResource(R.drawable.cand_back_focuse)
            v.setPadding(0, 0, 0, 0)

            val viewBodyTop = getViewTopOnScreen(mViewBodyScroll!!)
            val viewBodyBottom = viewBodyTop + mViewBodyScroll!!.height
            val focusedViewTop = getViewTopOnScreen(v)
            val focusedViewBottom = focusedViewTop + v.height

            if (focusedViewBottom > viewBodyBottom) {
                mViewBodyScroll!!.scrollBy(0, (focusedViewBottom - viewBodyBottom))
            } else if (focusedViewTop < viewBodyTop) {
                mViewBodyScroll!!.scrollBy(0, (focusedViewTop - viewBodyTop))
            }
        }
    }

    /**
     * Clear focus to selected candidate.
     */
    fun clearFocusCandidate() {
        val view = mFocusedView
        if (view != null) {
            view.setBackgroundDrawable(mFocusedViewBackground)
            view.setPadding(0, 0, 0, 0)
            mFocusedView = null
        }

        mFocusAxisX = 0
        mHasFocusedArray1st = true
        mCurrentFocusIndex = FOCUS_NONE
        mHandler.removeMessages(MSG_MOVE_FOCUS)
        mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.FOCUS_CANDIDATE_END))
    }

    /**
     * @see CandidatesViewManager.selectFocusCandidate
     */
    override fun selectFocusCandidate() {
        if (mCurrentFocusIndex != FOCUS_NONE) {
            val word = focusedWnnWord

            if (mHandler.hasMessages(MSG_SET_CANDIDATES)) {
                mWnnWordSelectedList.add(word)
            } else {
                selectCandidate(word)
            }
        }
    }

    val focusedWnnWord: WnnWord?
        /** @see CandidatesViewManager.getFocusedWnnWord
         */
        get() = getWnnWord(mCurrentFocusIndex)

    /**
     * Get WnnWord.
     *
     * @return WnnWord word
     */
    fun getWnnWord(index: Int): WnnWord? {
        var index = index
        var word: WnnWord? = null
        if (index < 0) {
            index = 0
            mHandler.removeMessages(MSG_MOVE_FOCUS)
            Log.i("iwnn", "TextCandidatesViewManager::getWnnWord  index < 0 ")
        } else {
            val size = if (mHasFocusedArray1st) mWnnWordArray1st.size else mWnnWordArray2nd.size
            if (index >= size) {
                index = size - 1
                mHandler.removeMessages(MSG_MOVE_FOCUS)
                Log.i("iwnn", "TextCandidatesViewManager::getWnnWord  index > candidate max ")
            }
        }

        word = if (mHasFocusedArray1st) {
            mWnnWordArray1st[index]
        } else {
            mWnnWordArray2nd[index]
        }
        return word
    }

    /**
     * Set display candidate line from SharedPreferences.
     */
    private fun setNumeberOfDisplayLines() {
        val pref = PreferenceManager.getDefaultSharedPreferences(mWnn)
        mPortraitNumberOfLine = pref.getString("setting_portrait", "2")!!.toInt()
        mLandscapeNumberOfLine = pref.getString("setting_landscape", "1")!!.toInt()
    }

    /**
     * Set emoticon enabled.
     */
    fun setEnableEmoticon(enableEmoticon: Boolean) {
        mEnableEmoticon = enableEmoticon
    }

    val focusedView: TextView?
        /**
         * Get View of focus candidate.
         */
        get() {
            if (mCurrentFocusIndex == FOCUS_NONE) {
                return null
            }
            val t = if (mHasFocusedArray1st) {
                mTextViewArray1st[mCurrentFocusIndex]
            } else {
                mTextViewArray2nd[mCurrentFocusIndex]
            }
            return t
        }

    /**
     * Move the focus to next candidate.
     *
     * @param direction  The direction of increment or decrement.
     * @param updown     `true` if move is up or down.
     */
    fun moveFocus(direction: Int, updown: Boolean) {
        var updown = updown
        val isStart = (mCurrentFocusIndex == FOCUS_NONE)
        if (direction == 0) {
            setViewStatusOfFocusedCandidate()
        }

        val size1st = mTextViewArray1st.size
        if (mHasFocusedArray1st && (size1st == 0)) {
            mHasFocusedArray1st = false
        }
        val list = if (mHasFocusedArray1st) mTextViewArray1st else mTextViewArray2nd
        val size = list.size
        val start = if ((mCurrentFocusIndex == FOCUS_NONE)) 0 else (mCurrentFocusIndex + direction)

        var index = -1
        var hasChangedLine = false
        var i = start
        while ((0 <= i) && (i < size)) {
            val view = list[i]
            if (!view!!.isShown) {
                break
            }

            if (mIsSymbolMode && (view.background == null)) {
                i += direction
                continue
            }

            if (updown) {
                val left = view.left
                if ((left <= mFocusAxisX)
                    && (mFocusAxisX < view.right)
                ) {
                    index = i
                    break
                }

                if (left == 0) {
                    hasChangedLine = true
                }
            } else {
                index = i
                break
            }
            i += direction
        }

        if ((index < 0) && hasChangedLine && !mHasFocusedArray1st && (0 < direction)) {
            index = mTextViewArray2nd.size - 1
        }

        if (0 <= index) {
            mCurrentFocusIndex = index
            setViewStatusOfFocusedCandidate()
            if (!updown) {
                mFocusAxisX = focusedView!!.left
            }
        } else {
            if (mCanReadMore && (0 < size1st)) {
                if ((mHasFocusedArray1st && (direction < 0))
                    || (!mHasFocusedArray1st && (0 < direction))
                ) {
                    updown = false
                }

                mHasFocusedArray1st = !mHasFocusedArray1st

                if (!mHasFocusedArray1st && !mIsFullView) {
                    setFullMode()
                }
            }

            if (size1st == 0) {
                updown = false
            }

            mCurrentFocusIndex = if (0 < direction) {
                -1
            } else {
                (if (mHasFocusedArray1st) size1st else mTextViewArray2nd.size)
            }
            val m = mHandler.obtainMessage(MSG_MOVE_FOCUS, direction, if (updown) 1 else 0)
            mHandler.sendMessage(m)
        }

        if (isStart) {
            mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.FOCUS_CANDIDATE_START))
        }
    }

    /**
     * Set hardkeyboard hidden.
     *
     * @param hardKeyboardHidden hardkeyaboard hidden.
     */
    fun setHardKeyboardHidden(hardKeyboardHidden: Boolean) {
        mHardKeyboardHidden = hardKeyboardHidden
    }

    /**
     * Get view top position on screen.
     *
     * @param view target view.
     * @return int view top position on screen
     */
    fun getViewTopOnScreen(view: View): Int {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return location[1]
    }


    /** @see CandidatesViewManager.setCandidateMsgRemove
     */
    override fun setCandidateMsgRemove() {
        mHandler.removeMessages(MSG_SET_CANDIDATES)
    }

    companion object {
        /** Number of lines to display (Portrait)  */
        const val LINE_NUM_PORTRAIT: Int = 2

        /** Number of lines to display (Landscape)  */
        const val LINE_NUM_LANDSCAPE: Int = 1

        /** Maximum lines  */
        private const val DISPLAY_LINE_MAX_COUNT = 1000

        /** Maximum number of displaying candidates par one line (full view mode)  */
        private const val FULL_VIEW_DIV = 4

        /** Maximum number of displaying candidates par one line (full view mode)(symbol)(portrait)  */
        private const val FULL_VIEW_SYMBOL_DIV_PORT = 6

        /** Maximum number of displaying candidates par one line (full view mode)(symbol)(landscape)  */
        private const val FULL_VIEW_SYMBOL_DIV_LAND = 10

        /** Delay of set candidate  */
        private const val SET_CANDIDATE_DELAY = 50

        /** First line count  */
        private const val SET_CANDIDATE_FIRST_LINE_COUNT = 7

        /** Delay line count  */
        private const val SET_CANDIDATE_DELAY_LINE_COUNT = 1

        /** Focus is none now  */
        private const val FOCUS_NONE = -1

        /** Handler for focus Candidate  */
        private const val MSG_MOVE_FOCUS = 0

        /** Handler for set  Candidate  */
        private const val MSG_SET_CANDIDATES = 1

        /** Handler for select Candidate  */
        private const val MSG_SELECT_CANDIDATES = 2

        /** NUmber of Candidate display lines  */
        private const val SETTING_NUMBER_OF_LINEMAX = 5

        /** Keyboard vertical gap value  */
        private const val KEYBOARD_VERTICAL_GAP = 0.009f

        /** Keyboard vertical gap count  */
        private const val KEYBOARD_VERTICAL_GAP_COUNT = 3
    }
}
