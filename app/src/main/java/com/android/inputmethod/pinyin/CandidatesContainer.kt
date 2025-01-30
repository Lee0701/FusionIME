/*
 * Copyright (C) 2009 The Android Open Source Project
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
package com.android.inputmethod.pinyin

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.ViewFlipper
import com.android.inputmethod.pinyin.PinyinIME.DecodingInfo
import ee.oyatl.ime.fusion.R

interface ArrowUpdater {
    fun updateArrowStatus()
}


/**
 * Container used to host the two candidate views. When user drags on candidate
 * view, animation is used to dismiss the current candidate view and show a new
 * one. These two candidate views and their parent are hosted by this container.
 *
 *
 * Besides the candidate views, there are two arrow views to show the page
 * forward/backward arrows.
 *
 */
class CandidatesContainer(context: Context?, attrs: AttributeSet?) :
    RelativeLayout(context, attrs), OnTouchListener, Animation.AnimationListener, ArrowUpdater {
    /**
     * Listener used to notify IME that user clicks a candidate, or navigate
     * between them.
     */
    private var mCvListener: CandidateViewListener? = null

    /**
     * The left arrow button used to show previous page.
     */
    private var mLeftArrowBtn: ImageButton? = null

    /**
     * The right arrow button used to show next page.
     */
    private var mRightArrowBtn: ImageButton? = null

    /**
     * Decoding result to show.
     */
    private var mDecInfo: DecodingInfo? = null

    /**
     * The animation view used to show candidates. It contains two views.
     * Normally, the candidates are shown one of them. When user navigates to
     * another page, animation effect will be performed.
     */
    private var mFlipper: ViewFlipper? = null

    /**
     * The x offset of the flipper in this container.
     */
    private var xOffsetForFlipper: Int = 0

    /**
     * Animation used by the incoming view when the user navigates to a left
     * page.
     */
    private var mInAnimPushLeft: Animation? = null

    /**
     * Animation used by the incoming view when the user navigates to a right
     * page.
     */
    private var mInAnimPushRight: Animation? = null

    /**
     * Animation used by the incoming view when the user navigates to a page
     * above. If the page navigation is triggered by DOWN key, this animation is
     * used.
     */
    private var mInAnimPushUp: Animation? = null

    /**
     * Animation used by the incoming view when the user navigates to a page
     * below. If the page navigation is triggered by UP key, this animation is
     * used.
     */
    private var mInAnimPushDown: Animation? = null

    /**
     * Animation used by the outgoing view when the user navigates to a left
     * page.
     */
    private var mOutAnimPushLeft: Animation? = null

    /**
     * Animation used by the outgoing view when the user navigates to a right
     * page.
     */
    private var mOutAnimPushRight: Animation? = null

    /**
     * Animation used by the outgoing view when the user navigates to a page
     * above. If the page navigation is triggered by DOWN key, this animation is
     * used.
     */
    private var mOutAnimPushUp: Animation? = null

    /**
     * Animation used by the incoming view when the user navigates to a page
     * below. If the page navigation is triggered by UP key, this animation is
     * used.
     */
    private var mOutAnimPushDown: Animation? = null

    /**
     * Animation object which is used for the incoming view currently.
     */
    private var mInAnimInUse: Animation? = null

    /**
     * Animation object which is used for the outgoing view currently.
     */
    private var mOutAnimInUse: Animation? = null

    /**
     * Current page number in display.
     */
    var currentPage: Int = -1
        private set

    fun initialize(
        cvListener: CandidateViewListener?,
        balloonHint: BalloonHint?, gestureDetector: GestureDetector?
    ) {
        mCvListener = cvListener

        mLeftArrowBtn = findViewById<View>(R.id.arrow_left_btn) as ImageButton?
        mRightArrowBtn = findViewById<View>(R.id.arrow_right_btn) as ImageButton?
        mLeftArrowBtn!!.setOnTouchListener(this)
        mRightArrowBtn!!.setOnTouchListener(this)

        mFlipper = findViewById<View>(R.id.candidate_flipper) as ViewFlipper?
        mFlipper!!.setMeasureAllChildren(true)

        invalidate()
        requestLayout()

        for (i in 0 until mFlipper!!.getChildCount()) {
            val cv: CandidateView = mFlipper!!.getChildAt(i) as CandidateView
            cv.initialize(this, balloonHint, gestureDetector, mCvListener)
        }
    }

    fun showCandidates(
        decInfo: DecodingInfo?,
        enableActiveHighlight: Boolean
    ) {
        if (null == decInfo) return
        mDecInfo = decInfo
        currentPage = 0

        if (decInfo.isCandidatesListEmpty) {
            showArrow(mLeftArrowBtn!!, false)
            showArrow(mRightArrowBtn!!, false)
        } else {
            showArrow(mLeftArrowBtn!!, true)
            showArrow(mRightArrowBtn!!, true)
        }

        for (i in 0 until mFlipper!!.getChildCount()) {
            val cv: CandidateView = mFlipper!!.getChildAt(i) as CandidateView
            cv.setDecodingInfo(mDecInfo)
        }
        stopAnimation()

        val cv: CandidateView = mFlipper!!.getCurrentView() as CandidateView
        cv.showPage(currentPage, 0, enableActiveHighlight)

        updateArrowStatus()
        invalidate()
    }

    fun enableActiveHighlight(enableActiveHighlight: Boolean) {
        val cv: CandidateView = mFlipper!!.getCurrentView() as CandidateView
        cv.enableActiveHighlight(enableActiveHighlight)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec: Int = widthMeasureSpec
        var heightMeasureSpec: Int = heightMeasureSpec
        val env: Environment = Environment.instance
        val measuredWidth: Int = env.screenWidth
        var measuredHeight: Int = getPaddingTop()
        measuredHeight += env.heightForCandidates
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(
            measuredWidth,
            MeasureSpec.EXACTLY
        )
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(
            measuredHeight,
            MeasureSpec.EXACTLY
        )
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (null != mLeftArrowBtn) {
            xOffsetForFlipper = mLeftArrowBtn!!.getMeasuredWidth()
        }
    }

    fun activeCurseBackward(): Boolean {
        if (mFlipper!!.isFlipping() || null == mDecInfo) {
            return false
        }

        val cv: CandidateView = mFlipper!!.getCurrentView() as CandidateView

        if (cv.activeCurseBackward()) {
            cv.invalidate()
            return true
        } else {
            return pageBackward(true, true)
        }
    }

    fun activeCurseForward(): Boolean {
        if (mFlipper!!.isFlipping() || null == mDecInfo) {
            return false
        }

        val cv: CandidateView = mFlipper!!.getCurrentView() as CandidateView

        if (cv.activeCursorForward()) {
            cv.invalidate()
            return true
        } else {
            return pageForward(true, true)
        }
    }

    fun pageBackward(
        animLeftRight: Boolean,
        enableActiveHighlight: Boolean
    ): Boolean {
        if (null == mDecInfo) return false

        if (mFlipper!!.isFlipping() || 0 == currentPage) return false

        val child: Int = mFlipper!!.getDisplayedChild()
        val childNext: Int = (child + 1) % 2
        val cv: CandidateView = mFlipper!!.getChildAt(child) as CandidateView
        val cvNext: CandidateView = mFlipper!!.getChildAt(childNext) as CandidateView

        currentPage--
        var activeCandInPage: Int = cv.activeCandiatePosInPage
        if (animLeftRight) activeCandInPage = (mDecInfo!!.mPageStart.elementAt(currentPage + 1)
                - mDecInfo!!.mPageStart.elementAt(currentPage) - 1)

        cvNext.showPage(currentPage, activeCandInPage, enableActiveHighlight)
        loadAnimation(animLeftRight, false)
        startAnimation()

        updateArrowStatus()
        return true
    }

    fun pageForward(
        animLeftRight: Boolean,
        enableActiveHighlight: Boolean
    ): Boolean {
        if (null == mDecInfo) return false

        if (mFlipper!!.isFlipping() || !mDecInfo!!.preparePage(currentPage + 1)) {
            return false
        }

        val child: Int = mFlipper!!.getDisplayedChild()
        val childNext: Int = (child + 1) % 2
        val cv: CandidateView = mFlipper!!.getChildAt(child) as CandidateView
        var activeCandInPage: Int = cv.activeCandiatePosInPage
        cv.enableActiveHighlight(enableActiveHighlight)

        val cvNext: CandidateView = mFlipper!!.getChildAt(childNext) as CandidateView
        currentPage++
        if (animLeftRight) activeCandInPage = 0

        cvNext.showPage(currentPage, activeCandInPage, enableActiveHighlight)
        loadAnimation(animLeftRight, true)
        startAnimation()

        updateArrowStatus()
        return true
    }

    val activeCandiatePos: Int
        get() {
            if (null == mDecInfo) return -1
            val cv: CandidateView = mFlipper!!.getCurrentView() as CandidateView
            return cv.activeCandiatePosGlobal
        }

    override fun updateArrowStatus() {
        if (currentPage < 0) return
        val forwardEnabled: Boolean = mDecInfo!!.pageForwardable(currentPage)
        val backwardEnabled: Boolean = mDecInfo!!.pageBackwardable(currentPage)

        if (backwardEnabled) {
            enableArrow(mLeftArrowBtn!!, true)
        } else {
            enableArrow(mLeftArrowBtn!!, false)
        }
        if (forwardEnabled) {
            enableArrow(mRightArrowBtn!!, true)
        } else {
            enableArrow(mRightArrowBtn!!, false)
        }
    }

    private fun enableArrow(arrowBtn: ImageButton, enabled: Boolean) {
        arrowBtn.setEnabled(enabled)
        if (enabled) arrowBtn.setAlpha(ARROW_ALPHA_ENABLED)
        else arrowBtn.setAlpha(ARROW_ALPHA_DISABLED)
    }

    private fun showArrow(arrowBtn: ImageButton, show: Boolean) {
        if (show) arrowBtn.setVisibility(VISIBLE)
        else arrowBtn.setVisibility(INVISIBLE)
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (v === mLeftArrowBtn) {
                mCvListener!!.onToRightGesture()
            } else if (v === mRightArrowBtn) {
                mCvListener!!.onToLeftGesture()
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            val cv: CandidateView = mFlipper!!.getCurrentView() as CandidateView
            cv.enableActiveHighlight(true)
        }

        return false
    }

    // The reason why we handle candiate view's touch events here is because
    // that the view under the focused view may get touch events instead of the
    // focused one.
    override fun onTouchEvent(event: MotionEvent): Boolean {
        event.offsetLocation(-xOffsetForFlipper.toFloat(), 0f)
        val cv: CandidateView = mFlipper!!.getCurrentView() as CandidateView
        cv.onTouchEventReal(event)
        return true
    }

    fun loadAnimation(animLeftRight: Boolean, forward: Boolean) {
        if (animLeftRight) {
            if (forward) {
                if (null == mInAnimPushLeft) {
                    mInAnimPushLeft = createAnimation(
                        1.0f, 0f, 0f, 0f, 0f, 1.0f,
                        ANIMATION_TIME.toLong()
                    )
                    mOutAnimPushLeft = createAnimation(
                        0f, -1.0f, 0f, 0f, 1.0f, 0f,
                        ANIMATION_TIME.toLong()
                    )
                }
                mInAnimInUse = mInAnimPushLeft
                mOutAnimInUse = mOutAnimPushLeft
            } else {
                if (null == mInAnimPushRight) {
                    mInAnimPushRight = createAnimation(
                        -1.0f, 0f, 0f, 0f, 0f, 1.0f,
                        ANIMATION_TIME.toLong()
                    )
                    mOutAnimPushRight = createAnimation(
                        0f, 1.0f, 0f, 0f, 1.0f, 0f,
                        ANIMATION_TIME.toLong()
                    )
                }
                mInAnimInUse = mInAnimPushRight
                mOutAnimInUse = mOutAnimPushRight
            }
        } else {
            if (forward) {
                if (null == mInAnimPushUp) {
                    mInAnimPushUp = createAnimation(
                        0f, 0f, 1.0f, 0f, 0f, 1.0f,
                        ANIMATION_TIME.toLong()
                    )
                    mOutAnimPushUp = createAnimation(
                        0f, 0f, 0f, -1.0f, 1.0f, 0f,
                        ANIMATION_TIME.toLong()
                    )
                }
                mInAnimInUse = mInAnimPushUp
                mOutAnimInUse = mOutAnimPushUp
            } else {
                if (null == mInAnimPushDown) {
                    mInAnimPushDown = createAnimation(
                        0f, 0f, -1.0f, 0f, 0f, 1.0f,
                        ANIMATION_TIME.toLong()
                    )
                    mOutAnimPushDown = createAnimation(
                        0f, 0f, 0f, 1.0f, 1.0f, 0f,
                        ANIMATION_TIME.toLong()
                    )
                }
                mInAnimInUse = mInAnimPushDown
                mOutAnimInUse = mOutAnimPushDown
            }
        }

        mInAnimInUse!!.setAnimationListener(this)

        mFlipper!!.setInAnimation(mInAnimInUse)
        mFlipper!!.setOutAnimation(mOutAnimInUse)
    }

    private fun createAnimation(
        xFrom: Float, xTo: Float, yFrom: Float,
        yTo: Float, alphaFrom: Float, alphaTo: Float, duration: Long
    ): Animation {
        val animSet: AnimationSet = AnimationSet(getContext(), null)
        val trans: Animation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF,
            xFrom, Animation.RELATIVE_TO_SELF, xTo,
            Animation.RELATIVE_TO_SELF, yFrom, Animation.RELATIVE_TO_SELF,
            yTo
        )
        val alpha: Animation = AlphaAnimation(alphaFrom, alphaTo)
        animSet.addAnimation(trans)
        animSet.addAnimation(alpha)
        animSet.setDuration(duration)
        return animSet
    }

    private fun startAnimation() {
        mFlipper!!.showNext()
    }

    private fun stopAnimation() {
        mFlipper!!.stopFlipping()
    }

    override fun onAnimationEnd(animation: Animation) {
        if (!mLeftArrowBtn!!.isPressed() && !mRightArrowBtn!!.isPressed()) {
            val cv: CandidateView = mFlipper!!.getCurrentView() as CandidateView
            cv.enableActiveHighlight(true)
        }
    }

    override fun onAnimationRepeat(animation: Animation) {
    }

    override fun onAnimationStart(animation: Animation) {
    }

    companion object {
        /**
         * Alpha value to show an enabled arrow.
         */
        private val ARROW_ALPHA_ENABLED: Int = 0xff

        /**
         * Alpha value to show an disabled arrow.
         */
        private val ARROW_ALPHA_DISABLED: Int = 0x40

        /**
         * Animation time to show a new candidate view and dismiss the old one.
         */
        private val ANIMATION_TIME: Int = 200
    }
}
