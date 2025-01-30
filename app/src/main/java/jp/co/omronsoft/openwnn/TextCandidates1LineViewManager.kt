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
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Handler
import android.os.Message
import android.os.Vibrator
import android.text.Layout
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import ee.oyatl.ime.fusion.R

/**
 * The default candidates view manager using [android.widget.EditText].
 *
 * @author Copyright (C) 2011 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
class TextCandidates1LineViewManager @JvmOverloads constructor(
    /** Limitation of displaying candidates  */
    private val mDisplayLimit: Int = 300
) :
    CandidatesViewManager() {
    /** Body view of the candidates list  */
    private var mViewBody: ViewGroup? = null

    /** Scroller  */
    private var mViewBodyScroll: HorizontalScrollView? = null

    /** Left more button  */
    private var mLeftMoreButton: ImageView? = null

    /** Right more button  */
    private var mRightMoreButton: ImageView? = null

    /** Candidate view  */
    private var mViewCandidateList: LinearLayout? = null

    /** [OpenWnn] instance using this manager  */
    private var mWnn: OpenWnn? = null

    /** View type (VIEW_TYPE_NORMAL or VIEW_TYPE_FULL or VIEW_TYPE_CLOSE)  */
    private var mViewType = 0

    /** view width  */
    private var mViewWidth = 0

    /** Minimum width of candidate view  */
    private var mCandidateMinimumWidth = 0

    /** Minimum height of candidate view  */
    private var mCandidateMinimumHeight = 0

    /** Whether hide the view if there is no candidate  */
    private var mAutoHideMode = true

    /** The converter to get candidates from and notice the selected candidate to.  */
    private var mConverter: WnnEngine? = null

    /** Vibrator for touch vibration  */
    private var mVibrator: Vibrator? = null

    /** AudioManager for click sound  */
    private var mSound: AudioManager? = null

    /** Number of candidates displaying  */
    private var mWordCount = 0

    /** List of candidates  */
    private val mWnnWordArray = ArrayList<WnnWord>()

    /** Character width of the candidate area  */
    private var mLineLength = 0

    /** Maximum width of candidate view  */
    private var mCandidateMaxWidth = 0

    /** general information about a display  */
    private val mMetrics = DisplayMetrics()

    /** List of textView for CandiData List  */
    private val mTextViewArray = ArrayList<TextView>()

    /** Now focus textView index  */
    private var mCurrentFocusIndex = FOCUS_NONE

    /** Focused View  */
    private var mFocusedView: View? = null

    /** Focused View Background  */
    private var mFocusedViewBackground: Drawable? = null

    /** Scale up text size  */
    private var mSizeSpan: AbsoluteSizeSpan? = null

    /** Scale up text alignment  */
    private var mCenterSpan: AlignmentSpan.Standard? = null

    /** Whether candidates long click enable  */
    private val mEnableCandidateLongClick = true

    /** `Handler` Handler for focus Candidate wait delay  */
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_SET_CANDIDATES -> displayCandidatesDelay(mConverter)
                else -> {}
            }
        }
    }

    /** Event listener for touching a candidate  */
    private val mCandidateOnClick =
        View.OnClickListener { v ->
            if (!v.isShown) {
                return@OnClickListener
            }
            playSoundAndVibration()
            if (v is CandidateTextView) {
                val wordcount = v.id
                val word = getWnnWord(wordcount)
                clearFocusCandidate()
                selectCandidate(word)
            }
        }

    /** Event listener for long-clicking a candidate  */
    private val mCandidateOnLongClick =
        OnLongClickListener { v ->
            if (!v.isShown) {
                return@OnLongClickListener true
            }
            if (!mEnableCandidateLongClick) {
                return@OnLongClickListener false
            }

            clearFocusCandidate()

            val wordcount = (v as TextView).id
            mWord = mWnnWordArray[wordcount]

            displayDialog(v, mWord!!)
            true
        }

    /**
     * Constructor
     *
     * @param mDisplayLimit      The limit of display
     */
    /**
     * Constructor
     */
    init {
        mMetrics.setToDefaults()
    }

    /**
     * Set auto-hide mode.
     * @param hide      `true` if the view will hidden when no candidate exists;
     * `false` if the view is always shown.
     */
    fun setAutoHide(hide: Boolean) {
        mAutoHideMode = hide
    }

    /** @see CandidatesViewManager
     */
    override fun initView(parent: OpenWnn, width: Int, height: Int): View? {
        mWnn = parent
        mViewWidth = width

        val r = mWnn!!.resources

        mCandidateMinimumWidth = (CANDIDATE_MINIMUM_WIDTH * mMetrics.density).toInt()
        mCandidateMinimumHeight = r.getDimensionPixelSize(R.dimen.candidate_layout_height)

        val inflater = parent.layoutInflater
        mViewBody = inflater.inflate(R.layout.candidates_1line, null) as ViewGroup
        mViewBodyScroll =
            mViewBody!!.findViewById<View>(R.id.candview_scroll_1line) as HorizontalScrollView
        mViewBodyScroll!!.overScrollMode = View.OVER_SCROLL_NEVER
        mViewBodyScroll!!.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> if (mHandler.hasMessages(
                        MSG_SET_CANDIDATES
                    )
                ) {
                    mHandler.removeMessages(MSG_SET_CANDIDATES)
                    mHandler.sendEmptyMessageDelayed(
                        MSG_SET_CANDIDATES,
                        CANDIDATE_DELAY_MILLIS.toLong()
                    )
                }

                else -> {}
            }
            false
        }

        mLeftMoreButton = mViewBody!!.findViewById<View>(R.id.left_more_imageview) as ImageView
        mLeftMoreButton!!.setOnClickListener(View.OnClickListener { v ->
            if (!v.isShown) {
                return@OnClickListener
            }
            playSoundAndVibration()
            if (mViewBodyScroll!!.scrollX > 0) {
                mViewBodyScroll!!.smoothScrollBy(
                    (mViewBodyScroll!!.width * -SCROLL_DISTANCE).toInt(), 0
                )
            }
        })
        mLeftMoreButton!!.setOnLongClickListener(OnLongClickListener { v ->
            if (!v.isShown) {
                return@OnLongClickListener false
            }
            if (!mViewBodyScroll!!.fullScroll(View.FOCUS_LEFT)) {
                mViewBodyScroll!!.scrollTo(mViewBodyScroll!!.getChildAt(0).width, 0)
            }
            true
        })

        mRightMoreButton = mViewBody!!.findViewById<View>(R.id.right_more_imageview) as ImageView
        mRightMoreButton!!.setOnClickListener(View.OnClickListener { v ->
            if (!v.isShown) {
                return@OnClickListener
            }
            val width = mViewBodyScroll!!.width
            val scrollMax = mViewBodyScroll!!.getChildAt(0).right
            if ((mViewBodyScroll!!.scrollX + width) < scrollMax) {
                mViewBodyScroll!!.smoothScrollBy((width * SCROLL_DISTANCE).toInt(), 0)
            }
        })
        mRightMoreButton!!.setOnLongClickListener(OnLongClickListener { v ->
            if (!v.isShown) {
                return@OnLongClickListener false
            }
            if (!mViewBodyScroll!!.fullScroll(View.FOCUS_RIGHT)) {
                mViewBodyScroll!!.scrollTo(0, 0)
            }
            true
        })

        mViewLongPressDialog = inflater.inflate(R.layout.candidate_longpress_dialog, null) as View

        /* select button */
        var longPressDialogButton =
            mViewLongPressDialog!!.findViewById<View>(R.id.candidate_longpress_dialog_select) as Button
        longPressDialogButton.setOnClickListener {
            playSoundAndVibration()
            clearFocusCandidate()
            selectCandidate(mWord!!)
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

        val buttonWidth = r.getDimensionPixelSize(R.dimen.candidate_layout_width)
        mCandidateMaxWidth = (mViewWidth - buttonWidth * 2) / 2

        mSizeSpan = AbsoluteSizeSpan(r.getDimensionPixelSize(R.dimen.candidate_delete_word_size))
        mCenterSpan = AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER)

        createNormalCandidateView()

        viewType = CandidatesViewManager.Companion.VIEW_TYPE_CLOSE

        return mViewBody
    }

    /**
     * Create normal candidate view
     */
    private fun createNormalCandidateView() {
        mViewCandidateList =
            mViewBody!!.findViewById<View>(R.id.candidates_view_1line) as LinearLayout

        val context = mViewBodyScroll!!.context
        for (i in 0 until mDisplayLimit) {
            mViewCandidateList!!.addView(
                CandidateTextView(
                    context,
                    mCandidateMinimumHeight,
                    mCandidateMinimumWidth,
                    mCandidateMaxWidth
                )
            )
        }
    }

    override val currentView: View?
        /** @see CandidatesViewManager.getCurrentView
         */
        get() = mViewBody

    override var viewType: Int
        /** @see CandidatesViewManager.getViewType
         */
        get() = mViewType
        /** @see CandidatesViewManager.setViewType
         */
        set(type) {
            mViewType = type

            if (type == CandidatesViewManager.Companion.VIEW_TYPE_NORMAL) {
                mViewCandidateList!!.minimumHeight = mCandidateMinimumHeight
            } else {
                mViewCandidateList!!.minimumHeight = -1
                mHandler.removeMessages(MSG_SET_CANDIDATES)

                if (mViewBody!!.isShown) {
                    mWnn!!.setCandidatesViewShown(false)
                }
            }
        }

    /** @see CandidatesViewManager.displayCandidates
     */
    override fun displayCandidates(converter: WnnEngine?) {
        mHandler.removeMessages(MSG_SET_CANDIDATES)

        closeDialog()
        clearCandidates()
        mConverter = converter

        var isNextCandidate = IS_NEXTCANDIDATE_NORMAL
        while (isNextCandidate == IS_NEXTCANDIDATE_NORMAL) {
            isNextCandidate = displayCandidatesNormal(converter)
        }

        if (isNextCandidate == IS_NEXTCANDIDATE_DELAY) {
            isNextCandidate = displayCandidatesDelay(converter)
        }

        mViewBodyScroll!!.scrollTo(0, 0)
    }


    /**
     * Display the candidates.
     * @param converter  [WnnEngine] which holds candidates.
     */
    private fun displayCandidatesNormal(converter: WnnEngine?): Int {
        var isNextCandidate = IS_NEXTCANDIDATE_NORMAL

        if (converter == null) {
            return IS_NEXTCANDIDATE_END
        }

        /* Get candidates */
        val result = converter.nextCandidate
            ?: return IS_NEXTCANDIDATE_END

        mLineLength += setCandidate(result)
        if (mLineLength >= mViewWidth) {
            isNextCandidate = IS_NEXTCANDIDATE_DELAY
        }

        if (mWordCount < 1) { /* no candidates */
            if (mAutoHideMode) {
                mWnn!!.setCandidatesViewShown(false)
                return IS_NEXTCANDIDATE_END
            }
        }

        if (mWordCount > mDisplayLimit) {
            return IS_NEXTCANDIDATE_END
        }

        if (!(mViewBody!!.isShown)) {
            mWnn!!.setCandidatesViewShown(true)
        }
        return isNextCandidate
    }

    /**
     * Display the candidates.
     * @param converter  [WnnEngine] which holds candidates.
     */
    private fun displayCandidatesDelay(converter: WnnEngine?): Int {
        val isNextCandidate = IS_NEXTCANDIDATE_DELAY

        if (converter == null) {
            return IS_NEXTCANDIDATE_END
        }

        /* Get candidates */
        val result = converter.nextCandidate
            ?: return IS_NEXTCANDIDATE_END

        setCandidate(result)

        if (mWordCount > mDisplayLimit) {
            return IS_NEXTCANDIDATE_END
        }

        mHandler.sendEmptyMessageDelayed(MSG_SET_CANDIDATES, SET_CANDIDATE_DELAY.toLong())

        return isNextCandidate
    }

    /**
     * Set the candidate for candidate view
     * @param word set word
     * @return int Set width
     */
    private fun setCandidate(word: WnnWord): Int {
        val candidateTextView =
            mViewCandidateList!!.getChildAt(mWordCount) as CandidateTextView
        candidateTextView.setCandidateTextView(
            word, mWordCount, mCandidateOnClick,
            mCandidateOnLongClick
        )
        mWnnWordArray.add(mWordCount, word)
        mWordCount++
        mTextViewArray.add(candidateTextView)

        return candidateTextView.width
    }

    /**
     * Clear the candidate view
     */
    private fun clearNormalViewCandidate() {
        val candidateNum = mViewCandidateList!!.childCount
        for (i in 0 until candidateNum) {
            val v = mViewCandidateList!!.getChildAt(i)
            v.visibility = View.GONE
        }
    }

    /** @see CandidatesViewManager.clearCandidates
     */
    override fun clearCandidates() {
        clearFocusCandidate()
        clearNormalViewCandidate()

        mLineLength = 0

        mWordCount = 0
        mWnnWordArray.clear()
        mTextViewArray.clear()

        if (mAutoHideMode && mViewBody!!.isShown) {
            mWnn!!.setCandidatesViewShown(false)
        }
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
        } catch (ex: Exception) {
            Log.d("OpenWnn", "NO VIBRATOR")
        }
    }

    /**
     * Select a candidate.
     * <br></br>
     * This method notices the selected word to [OpenWnn].
     *
     * @param word  The selected word
     */
    private fun selectCandidate(word: WnnWord) {
        mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.SELECT_CANDIDATE, word))
    }

    private fun playSoundAndVibration() {
        if (mVibrator != null) {
            try {
                mVibrator!!.vibrate(5)
            } catch (ex: Exception) {
                Log.e(
                    "OpenWnn",
                    "TextCandidates1LineViewManager::selectCandidate Vibrator $ex"
                )
            }
        }
        if (mSound != null) {
            try {
                mSound!!.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 1.0f)
            } catch (ex: Exception) {
                Log.e(
                    "OpenWnn",
                    "TextCandidates1LineViewManager::selectCandidate Sound $ex"
                )
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
            KeyEvent.KEYCODE_DPAD_LEFT -> moveFocus(-1)
            KeyEvent.KEYCODE_DPAD_RIGHT -> moveFocus(1)
            KeyEvent.KEYCODE_DPAD_UP -> moveFocus(-1)
            KeyEvent.KEYCODE_DPAD_DOWN -> moveFocus(1)
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
    private fun setViewStatusOfFocusedCandidate() {
        val view = mFocusedView
        view?.setBackgroundDrawable(mFocusedViewBackground)

        val v = focusedView
        mFocusedView = v
        if (v != null) {
            mFocusedViewBackground = v.background
            v.setBackgroundResource(R.drawable.cand_back_focuse)

            val viewBodyLeft = getViewLeftOnScreen(mViewBodyScroll!!)
            val viewBodyRight = viewBodyLeft + mViewBodyScroll!!.width
            val focusedViewLeft = getViewLeftOnScreen(v)
            val focusedViewRight = focusedViewLeft + v.width

            if (focusedViewRight > viewBodyRight) {
                mViewBodyScroll!!.scrollBy((focusedViewRight - viewBodyRight), 0)
            } else if (focusedViewLeft < viewBodyLeft) {
                mViewBodyScroll!!.scrollBy((focusedViewLeft - viewBodyLeft), 0)
            }
        }
    }

    /**
     * Clear focus to selected candidate.
     */
    private fun clearFocusCandidate() {
        val view = mFocusedView
        if (view != null) {
            view.setBackgroundDrawable(mFocusedViewBackground)
            mFocusedView = null
        }

        mCurrentFocusIndex = FOCUS_NONE

        mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.FOCUS_CANDIDATE_END))
    }

    /**
     * Select candidate that has focus.
     */
    override fun selectFocusCandidate() {
        if (mCurrentFocusIndex != FOCUS_NONE) {
            selectCandidate(focusedWnnWord)
        }
    }

    private val focusedView: TextView?
        /**
         * Get View of focus candidate.
         */
        get() {
            if (mCurrentFocusIndex == FOCUS_NONE) {
                return null
            }
            return mTextViewArray[mCurrentFocusIndex]
        }

    /**
     * Move the focus to next candidate.
     *
     * @param direction  The direction of increment or decrement.
     */
    private fun moveFocus(direction: Int) {
        val isStart = (mCurrentFocusIndex == FOCUS_NONE)
        val size = mTextViewArray.size
        var index = if (isStart) 0 else (mCurrentFocusIndex + direction)

        if (index < 0) {
            index = size - 1
        } else {
            if (index >= size) {
                index = 0
            }
        }

        mCurrentFocusIndex = index
        setViewStatusOfFocusedCandidate()

        if (isStart) {
            mWnn!!.onEvent(OpenWnnEvent(OpenWnnEvent.Companion.FOCUS_CANDIDATE_START))
        }
    }

    /**
     * Get view top position on screen.
     *
     * @param view target view.
     * @return int view top position on screen
     */
    private fun getViewLeftOnScreen(view: View): Int {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return location[0]
    }

    val focusedWnnWord: WnnWord
        /** @see CandidatesViewManager.getFocusedWnnWord
         */
        get() = getWnnWord(mCurrentFocusIndex)

    /**
     * Get WnnWord.
     *
     * @return WnnWord word
     */
    fun getWnnWord(index: Int): WnnWord {
        return mWnnWordArray[index]
    }

    /** @see CandidatesViewManager.setCandidateMsgRemove
     */
    override fun setCandidateMsgRemove() {
        mHandler.removeMessages(MSG_SET_CANDIDATES)
    }

    companion object {
        /** displayCandidates() normal display  */
        private const val IS_NEXTCANDIDATE_NORMAL = 1

        /** displayCandidates() delay display  */
        private const val IS_NEXTCANDIDATE_DELAY = 2

        /** displayCandidates() display end  */
        private const val IS_NEXTCANDIDATE_END = 3

        /** Delay of set candidate  */
        private const val SET_CANDIDATE_DELAY = 50

        /** Delay Millis  */
        private const val CANDIDATE_DELAY_MILLIS = 500

        /** Scroll distance  */
        private const val SCROLL_DISTANCE = 0.9f

        /** Minimum width of candidate view  */
        private const val CANDIDATE_MINIMUM_WIDTH = 48

        /** Focus is none now  */
        private const val FOCUS_NONE = -1

        /** Handler for set  Candidate  */
        private const val MSG_SET_CANDIDATES = 1
    }
}
