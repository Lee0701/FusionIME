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
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.SystemClock
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.ViewFlipper
import ee.oyatl.ime.fusion.R
import kotlin.concurrent.Volatile
import kotlin.math.abs

/**
 * The top container to host soft keyboard view(s).
 */
class SkbContainer(context: Context?, attrs: AttributeSet?) :
    RelativeLayout(context, attrs), OnTouchListener {
    /**
     * The current soft keyboard layout.
     *
     * @see com.android.inputmethod.pinyin.InputModeSwitcher for detailed layout
     * definitions.
     */
    private var mSkbLayout = 0

    /**
     * The input method service.
     */
    private var mService: InputMethodService? = null

    /**
     * Input mode switcher used to switch between different modes like Chinese,
     * English, etc.
     */
    private var mInputModeSwitcher: InputModeSwitcher? = null

    /**
     * The gesture detector.
     */
    private var mGestureDetector: GestureDetector? = null

    private val mEnvironment: Environment =
        Environment.instance

    private var mSkbFlipper: ViewFlipper? = null

    /**
     * The popup balloon hint for key press/release.
     */
    private val mBalloonPopup: BalloonHint

    /**
     * The on-key balloon hint for key press/release.
     */
    private var mBalloonOnKey: BalloonHint? = null

    /** The major sub soft keyboard.  */
    private var mMajorView: SoftKeyboardView? = null

    /**
     * The last parameter when function [.toggleCandidateMode]
     * was called.
     */
    private var mLastCandidatesShowing = false

    /** Used to indicate whether a popup soft keyboard is shown.  */
    private var mPopupSkbShow = false

    /**
     * Used to indicate whether a popup soft keyboard is just shown, and waits
     * for the touch event to release. After the release, the popup window can
     * response to touch events.
     */
    private var mPopupSkbNoResponse = false

    /** Popup sub keyboard.  */
    private val mPopupSkb: PopupWindow

    /** The view of the popup sub soft keyboard.  */
    private var mPopupSkbView: SoftKeyboardView? = null

    private var mPopupX = 0

    private var mPopupY = 0

    /**
     * When user presses a key, a timer is started, when it times out, it is
     * necessary to detect whether user still holds the key.
     */
    @Volatile
    private var mWaitForTouchUp = false

    /**
     * When user drags on the soft keyboard and the distance is enough, this
     * drag will be recognized as a gesture and a gesture-based action will be
     * taken, in this situation, ignore the consequent events.
     */
    @Volatile
    private var mDiscardEvent = false

    /**
     * For finger touch, user tends to press the bottom part of the target key,
     * or he/she even presses the area out of it, so it is necessary to make a
     * simple bias correction in Y.
     */
    private var mYBiasCorrection = 0

    /**
     * The x coordination of the last touch event.
     */
    private var mXLast = 0

    /**
     * The y coordination of the last touch event.
     */
    private var mYLast = 0

    /**
     * The soft keyboard view.
     */
    private var mSkv: SoftKeyboardView? = null

    /**
     * The position of the soft keyboard view in the container.
     */
    private val mSkvPosInContainer = IntArray(2)

    /**
     * The key pressed by user.
     */
    private var mSoftKeyDown: SoftKey? = null

    /**
     * Used to timeout a press if user holds the key for a long time.
     */
    private val mLongPressTimer = LongPressTimer(this)

    /**
     * For temporary use.
     */
    private val mXyPosTmp = IntArray(2)

    init {
        // If it runs on an emulator, no bias correction
        mYBiasCorrection =
            if ("1" == SystemProperties.get("ro.kernel.qemu")) {
                0
            } else {
                Y_BIAS_CORRECTION
            }
        mBalloonPopup = BalloonHint(context, this, MeasureSpec.AT_MOST)
        if (POPUPWINDOW_FOR_PRESSED_UI) {
            mBalloonOnKey = BalloonHint(context, this, MeasureSpec.AT_MOST)
        }

        mPopupSkb = PopupWindow(getContext())
        mPopupSkb.setBackgroundDrawable(null)
        mPopupSkb.isClippingEnabled = false
    }

    fun setService(service: InputMethodService?) {
        mService = service
    }

    fun setInputModeSwitcher(inputModeSwitcher: InputModeSwitcher?) {
        mInputModeSwitcher = inputModeSwitcher
    }

    fun setGestureDetector(gestureDetector: GestureDetector?) {
        mGestureDetector = gestureDetector
    }

    val isCurrentSkbSticky: Boolean
        get() {
            if (null == mMajorView) return true
            val skb = mMajorView!!.softKeyboard
            if (null != skb) {
                return skb.stickyFlag
            }
            return true
        }

    fun toggleCandidateMode(candidatesShowing: Boolean) {
        if (null == mMajorView || !mInputModeSwitcher!!.isChineseText
            || mLastCandidatesShowing == candidatesShowing
        ) return
        mLastCandidatesShowing = candidatesShowing

        val skb = mMajorView!!.softKeyboard ?: return

        val state = mInputModeSwitcher!!.toggleStateForCand
        if (!candidatesShowing) {
            skb.disableToggleState(state, false)
            skb.enableToggleStates(mInputModeSwitcher!!.toggleStates)
        } else {
            skb.enableToggleState(state, false)
        }

        mMajorView!!.invalidate()
    }

    fun updateInputMode() {
        val skbLayout = mInputModeSwitcher!!.skbLayout
        if (mSkbLayout != skbLayout) {
            mSkbLayout = skbLayout
            updateSkbLayout()
        }

        mLastCandidatesShowing = false

        if (null == mMajorView) return

        val skb = mMajorView!!.softKeyboard ?: return
        skb.enableToggleStates(mInputModeSwitcher!!.toggleStates)
        invalidate()
        return
    }

    private fun updateSkbLayout() {
        val screenWidth = mEnvironment.screenWidth
        val keyHeight = mEnvironment.keyHeight
        val skbHeight = mEnvironment.skbHeight

        val r = context.resources
        if (null == mSkbFlipper) {
            mSkbFlipper = findViewById<View>(R.id.alpha_floatable) as ViewFlipper
        }
        mMajorView = mSkbFlipper!!.getChildAt(0) as SoftKeyboardView

        var majorSkb: SoftKeyboard? = null
        val skbPool: SkbPool = SkbPool.instance

        when (mSkbLayout) {
            R.xml.skb_qwerty -> majorSkb = skbPool.getSoftKeyboard(
                R.xml.skb_qwerty,
                R.xml.skb_qwerty, screenWidth, skbHeight, context
            )

            R.xml.skb_sym1 -> majorSkb = skbPool.getSoftKeyboard(
                R.xml.skb_sym1, R.xml.skb_sym1,
                screenWidth, skbHeight, context
            )

            R.xml.skb_sym2 -> majorSkb = skbPool.getSoftKeyboard(
                R.xml.skb_sym2, R.xml.skb_sym2,
                screenWidth, skbHeight, context
            )

            R.xml.skb_smiley -> majorSkb = skbPool.getSoftKeyboard(
                R.xml.skb_smiley,
                R.xml.skb_smiley, screenWidth, skbHeight, context
            )

            R.xml.skb_phone -> majorSkb = skbPool.getSoftKeyboard(
                R.xml.skb_phone,
                R.xml.skb_phone, screenWidth, skbHeight, context
            )

            else -> {}
        }

        if (null == majorSkb || !mMajorView!!.setSoftKeyboard(majorSkb)) {
            return
        }
        mMajorView!!.setBalloonHint(mBalloonOnKey, mBalloonPopup, false)
        mMajorView!!.invalidate()
    }

    private fun responseKeyEvent(sKey: SoftKey?) {
        if (null == sKey) return
        (mService as PinyinIME).responseSoftKeyEvent(sKey)
        return
    }

    private fun inKeyboardView(
        x: Int, y: Int,
        positionInParent: IntArray
    ): SoftKeyboardView? {
        if (mPopupSkbShow) {
            if (mPopupX <= x && mPopupX + mPopupSkb.width > x && mPopupY <= y && mPopupY + mPopupSkb.height > y) {
                positionInParent[0] = mPopupX
                positionInParent[1] = mPopupY
                mPopupSkbView!!.setOffsetToSkbContainer(positionInParent)
                return mPopupSkbView
            }
            return null
        }

        return mMajorView
    }

    private fun popupSymbols() {
        val popupResId = mSoftKeyDown!!.popupResId
        if (popupResId > 0) {
            val skbContainerWidth = width
            val skbContainerHeight = height
            // The paddings of the background are not included.
            val miniSkbWidth = (skbContainerWidth * 0.8).toInt()
            val miniSkbHeight = (skbContainerHeight * 0.23).toInt()

            val skbPool: SkbPool = SkbPool.instance
            val skb = skbPool.getSoftKeyboard(
                popupResId, popupResId,
                miniSkbWidth, miniSkbHeight, context
            )
            if (null == skb) return

            mPopupX = (skbContainerWidth - skb.skbTotalWidth) / 2
            mPopupY = (skbContainerHeight - skb.skbTotalHeight) / 2

            if (null == mPopupSkbView) {
                mPopupSkbView = SoftKeyboardView(context, null)
                mPopupSkbView!!.measure(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                )
            }
            mPopupSkbView!!.setOnTouchListener(this)
            mPopupSkbView!!.setSoftKeyboard(skb)
            mPopupSkbView!!.setBalloonHint(mBalloonOnKey, mBalloonPopup, true)

            mPopupSkb.contentView = mPopupSkbView
            mPopupSkb.width = (skb.skbCoreWidth
                    + mPopupSkbView!!.paddingLeft
                    + mPopupSkbView!!.paddingRight)
            mPopupSkb.height = (skb.skbCoreHeight
                    + mPopupSkbView!!.paddingTop
                    + mPopupSkbView!!.paddingBottom)

            getLocationInWindow(mXyPosTmp)
            mPopupSkb.showAtLocation(
                this, Gravity.NO_GRAVITY, mPopupX, mPopupY
                        + mXyPosTmp[1]
            )
            mPopupSkbShow = true
            mPopupSkbNoResponse = true
            // Invalidate itself to dim the current soft keyboards.
            dimSoftKeyboard(true)
            resetKeyPress(0)
        }
    }

    private fun dimSoftKeyboard(dimSkb: Boolean) {
        mMajorView!!.dimSoftKeyboard(dimSkb)
    }

    private fun dismissPopupSkb() {
        mPopupSkb.dismiss()
        mPopupSkbShow = false
        dimSoftKeyboard(false)
        resetKeyPress(0)
    }

    private fun resetKeyPress(delay: Long) {
        mLongPressTimer.removeTimer()

        if (null != mSkv) {
            mSkv!!.resetKeyPress(delay)
        }
    }

    fun handleBack(realAction: Boolean): Boolean {
        if (mPopupSkbShow) {
            if (!realAction) return true

            dismissPopupSkb()
            mDiscardEvent = true
            return true
        }
        return false
    }

    fun dismissPopups() {
        handleBack(true)
        resetKeyPress(0)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        val env: Environment = Environment.instance
        val measuredWidth = env.screenWidth
        var measuredHeight = paddingTop
        measuredHeight += env.skbHeight
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(
            measuredWidth,
            MeasureSpec.EXACTLY
        )
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(
            measuredHeight,
            MeasureSpec.EXACTLY
        )
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)

        if (mSkbFlipper!!.isFlipping) {
            resetKeyPress(0)
            return true
        }

        val x = event.x.toInt()
        var y = event.y.toInt()
        // Bias correction
        y = y + mYBiasCorrection

        // Ignore short-distance movement event to get better performance.
        if (event.action == MotionEvent.ACTION_MOVE) {
            if (abs((x - mXLast).toDouble()) <= MOVE_TOLERANCE
                && abs((y - mYLast).toDouble()) <= MOVE_TOLERANCE
            ) {
                return true
            }
        }

        mXLast = x
        mYLast = y

        if (!mPopupSkbShow) {
            if (mGestureDetector!!.onTouchEvent(event)) {
                resetKeyPress(0)
                mDiscardEvent = true
                return true
            }
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                resetKeyPress(0)

                mWaitForTouchUp = true
                mDiscardEvent = false

                mSkv = null
                mSoftKeyDown = null
                mSkv = inKeyboardView(x, y, mSkvPosInContainer)
                if (null != mSkv) {
                    mSoftKeyDown = mSkv!!.onKeyPress(
                        x - mSkvPosInContainer[0], y
                                - mSkvPosInContainer[1], mLongPressTimer, false
                    )
                }
            }

            MotionEvent.ACTION_MOVE -> run {
                if (x < 0 || x >= width || y < 0 || y >= height) {
                    return@run
                }
                if (mDiscardEvent) {
                    resetKeyPress(0)
                    return@run
                }

                if (mPopupSkbShow && mPopupSkbNoResponse) {
                    return@run
                }

                val skv = inKeyboardView(x, y, mSkvPosInContainer)
                if (null != skv) {
                    if (skv !== mSkv) {
                        mSkv = skv
                        mSoftKeyDown = mSkv!!.onKeyPress(
                            x - mSkvPosInContainer[0], y
                                    - mSkvPosInContainer[1], mLongPressTimer, true
                        )
                    } else if (null != skv) {
                        if (null != mSkv) {
                            mSoftKeyDown = mSkv!!.onKeyMove(
                                x - mSkvPosInContainer[0], y
                                        - mSkvPosInContainer[1]
                            )
                            if (null == mSoftKeyDown) {
                                mDiscardEvent = true
                            }
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP -> run {
                if (mDiscardEvent) {
                    resetKeyPress(0)
                    return@run
                }

                mWaitForTouchUp = false

                // The view which got the {@link MotionEvent#ACTION_DOWN} event is
                // always used to handle this event.
                if (null != mSkv) {
                    mSkv!!.onKeyRelease(
                        x - mSkvPosInContainer[0], y
                                - mSkvPosInContainer[1]
                    )
                }

                if (!mPopupSkbShow || !mPopupSkbNoResponse) {
                    responseKeyEvent(mSoftKeyDown)
                }

                if (mSkv === mPopupSkbView && !mPopupSkbNoResponse) {
                    dismissPopupSkb()
                }
                mPopupSkbNoResponse = false
            }

            MotionEvent.ACTION_CANCEL -> {}
        }

        if (null == mSkv) {
            return false
        }

        return true
    }

    // Function for interface OnTouchListener, it is used to handle touch events
    // which will be delivered to the popup soft keyboard view.
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        // Translate the event to fit to the container.
        val newEv = MotionEvent.obtain(
            event.downTime, event
                .eventTime, event.action, event.x + mPopupX,
            event.y + mPopupY, event.pressure, event.size,
            event.metaState, event.xPrecision, event
                .yPrecision, event.deviceId, event
                .edgeFlags
        )
        val ret = onTouchEvent(newEv)
        return ret
    }

    inner class LongPressTimer(var mSkbContainer: SkbContainer) : Handler(), Runnable {
        private var mResponseTimes = 0

        fun startTimer() {
            mResponseTimes = 0
            postAtTime(this, SystemClock.uptimeMillis() + LONG_PRESS_TIMEOUT1)
        }

        fun removeTimer(): Boolean {
            removeCallbacks(this)
            return true
        }

        override fun run() {
            if (mWaitForTouchUp) {
                mResponseTimes++
                if (mSoftKeyDown!!.repeatable()) {
                    if (mSoftKeyDown!!.isUserDefKey) {
                        if (1 == mResponseTimes) {
                            if (mInputModeSwitcher!!
                                    .tryHandleLongPressSwitch(mSoftKeyDown!!.keyCode)
                            ) {
                                mDiscardEvent = true
                                resetKeyPress(0)
                            }
                        }
                    } else {
                        responseKeyEvent(mSoftKeyDown)
                        val timeout = if (mResponseTimes < LONG_PRESS_KEYNUM1) {
                            LONG_PRESS_TIMEOUT1.toLong()
                        } else if (mResponseTimes < LONG_PRESS_KEYNUM2) {
                            LONG_PRESS_TIMEOUT2.toLong()
                        } else {
                            LONG_PRESS_TIMEOUT3.toLong()
                        }
                        postAtTime(this, SystemClock.uptimeMillis() + timeout)
                    }
                } else {
                    if (1 == mResponseTimes) {
                        popupSymbols()
                    }
                }
            }
        }
    }

    companion object {
        /**
         * For finger touch, user tends to press the bottom part of the target key,
         * or he/she even presses the area out of it, so it is necessary to make a
         * simple bias correction. If the input method runs on emulator, no bias
         * correction will be used.
         */
        private const val Y_BIAS_CORRECTION = -10

        /**
         * Used to skip these move events whose position is too close to the
         * previous touch events.
         */
        private const val MOVE_TOLERANCE = 6

        /**
         * If this member is true, PopupWindow is used to show on-key highlight
         * effect.
         */
        private const val POPUPWINDOW_FOR_PRESSED_UI = false

        /**
         * When user presses a key for a long time, the timeout interval to
         * generate first [.LONG_PRESS_KEYNUM1] key events.
         */
        const val LONG_PRESS_TIMEOUT1: Int = 500

        /**
         * When user presses a key for a long time, after the first
         * [.LONG_PRESS_KEYNUM1] key events, this timeout interval will be
         * used.
         */
        private const val LONG_PRESS_TIMEOUT2 = 100

        /**
         * When user presses a key for a long time, after the first
         * [.LONG_PRESS_KEYNUM2] key events, this timeout interval will be
         * used.
         */
        private const val LONG_PRESS_TIMEOUT3 = 100

        /**
         * When user presses a key for a long time, after the first
         * [.LONG_PRESS_KEYNUM1] key events, timeout interval
         * [.LONG_PRESS_TIMEOUT2] will be used instead.
         */
        const val LONG_PRESS_KEYNUM1: Int = 1

        /**
         * When user presses a key for a long time, after the first
         * [.LONG_PRESS_KEYNUM2] key events, timeout interval
         * [.LONG_PRESS_TIMEOUT3] will be used instead.
         */
        const val LONG_PRESS_KEYNUM2: Int = 3
    }
}
