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
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.view.View.MeasureSpec
import android.widget.PopupWindow

/**
 * Subclass of PopupWindow used as the feedback when user presses on a soft key
 * or a candidate.
 */
class BalloonHint(
    context: Context?,
    /**
     * Parent used to show the balloon window.
     */
    private val mParent: View?,
    /**
     * The measuring specification used to determine its size. Key-press
     * balloons and candidates balloons have different measuring specifications.
     */
    private val mMeasureSpecMode: Int
) :
    PopupWindow(context) {
    /**
     * The padding information of the balloon. Because PopupWindow's background
     * can not be changed unless it is dismissed and shown again, we set the
     * real background drawable to the content view, and make the PopupWindow's
     * background transparent. So actually this padding information is for the
     * content view.
     */
    val padding: Rect = Rect()

    /**
     * The context used to create this balloon hint object.
     */
    val context: Context? = null

    /**
     * The content view of the balloon.
     */
    var mBalloonView: BalloonView

    /**
     * Used to indicate whether the balloon needs to be dismissed forcibly.
     */
    private var mForceDismiss = false

    /**
     * Timer used to show/dismiss the balloon window with some time delay.
     */
    private val mBalloonTimer: BalloonTimer

    private val mParentLocationInWindow = IntArray(2)

    init {
        inputMethodMode = INPUT_METHOD_NOT_NEEDED
        isTouchable = false
        setBackgroundDrawable(ColorDrawable(0))

        mBalloonView = BalloonView(context)
        mBalloonView.isClickable = false
        contentView = mBalloonView

        mBalloonTimer = BalloonTimer()
    }

    fun setBalloonBackground(drawable: Drawable?) {
        // We usually pick up a background from a soft keyboard template,
        // and the object may has been set to this balloon before.
        if (mBalloonView.background === drawable) return
        mBalloonView.setBackgroundDrawable(drawable)

        drawable?.getPadding(padding) ?: padding.set(0, 0, 0, 0)
    }

    /**
     * Set configurations to show text label in this balloon.
     *
     * @param label The text label to show in the balloon.
     * @param textSize The text size used to show label.
     * @param textBold Used to indicate whether the label should be bold.
     * @param textColor The text color used to show label.
     * @param width The desired width of the balloon. The real width is
     * determined by the desired width and balloon's measuring
     * specification.
     * @param height The desired width of the balloon. The real width is
     * determined by the desired width and balloon's measuring
     * specification.
     */
    fun setBalloonConfig(
        label: String?, textSize: Float,
        textBold: Boolean, textColor: Int, width: Int, height: Int
    ) {
        mBalloonView.setTextConfig(label, textSize, textBold, textColor)
        setBalloonSize(width, height)
    }

    /**
     * Set configurations to show text label in this balloon.
     *
     * @param icon The icon used to shown in this balloon.
     * @param width The desired width of the balloon. The real width is
     * determined by the desired width and balloon's measuring
     * specification.
     * @param height The desired width of the balloon. The real width is
     * determined by the desired width and balloon's measuring
     * specification.
     */
    fun setBalloonConfig(icon: Drawable?, width: Int, height: Int) {
        mBalloonView.setIcon(icon)
        setBalloonSize(width, height)
    }


    fun needForceDismiss(): Boolean {
        return mForceDismiss
    }

    val paddingLeft: Int
        get() = padding.left

    val paddingTop: Int
        get() = padding.top

    val paddingRight: Int
        get() = padding.right

    val paddingBottom: Int
        get() = padding.bottom

    fun delayedShow(delay: Long, locationInParent: IntArray) {
        if (mBalloonTimer.isPending) {
            mBalloonTimer.removeTimer()
        }
        if (delay <= 0) {
            mParent!!.getLocationInWindow(mParentLocationInWindow)
            showAtLocation(
                mParent, Gravity.LEFT or Gravity.TOP,
                locationInParent[0], locationInParent[1]
                        + mParentLocationInWindow[1]
            )
        } else {
            mBalloonTimer.startTimer(
                delay, TimerAction.SHOW,
                locationInParent, -1, -1
            )
        }
    }

    fun delayedUpdate(
        delay: Long, locationInParent: IntArray,
        width: Int, height: Int
    ) {
        mBalloonView.invalidate()
        if (mBalloonTimer.isPending) {
            mBalloonTimer.removeTimer()
        }
        if (delay <= 0) {
            mParent!!.getLocationInWindow(mParentLocationInWindow)
            update(
                locationInParent[0], locationInParent[1]
                        + mParentLocationInWindow[1], width, height
            )
        } else {
            mBalloonTimer.startTimer(
                delay, TimerAction.UPDATE,
                locationInParent, width, height
            )
        }
    }

    fun delayedDismiss(delay: Long) {
        if (mBalloonTimer.isPending) {
            mBalloonTimer.removeTimer()
            val pendingAction: Int = mBalloonTimer.action
            if (0L != delay && TimerAction.HIDE != pendingAction) {
                mBalloonTimer.run()
            }
        }
        if (delay <= 0) {
            dismiss()
        } else {
            mBalloonTimer.startTimer(
                delay, TimerAction.HIDE, null, -1,
                -1
            )
        }
    }

    fun removeTimer() {
        if (mBalloonTimer.isPending) {
            mBalloonTimer.removeTimer()
        }
    }

    private fun setBalloonSize(width: Int, height: Int) {
        val widthMeasureSpec = MeasureSpec.makeMeasureSpec(
            width,
            mMeasureSpecMode
        )
        val heightMeasureSpec = MeasureSpec.makeMeasureSpec(
            height,
            mMeasureSpecMode
        )
        mBalloonView.measure(widthMeasureSpec, heightMeasureSpec)

        val oldWidth = getWidth()
        val oldHeight = getHeight()
        val newWidth = (mBalloonView.measuredWidth + paddingLeft
                + paddingRight)
        val newHeight = (mBalloonView.measuredHeight + paddingTop
                + paddingBottom)
        setWidth(newWidth)
        setHeight(newHeight)

        // If update() is called to update both size and position, the system
        // will first MOVE the PopupWindow to the new position, and then
        // perform a size-updating operation, so there will be a flash in
        // PopupWindow if user presses a key and moves finger to next one whose
        // size is different.
        // PopupWindow will handle the updating issue in one go in the future,
        // but before that, if we find the size is changed, a mandatory dismiss
        // operation is required. In our UI design, normal QWERTY keys' width
        // can be different in 1-pixel, and we do not dismiss the balloon when
        // user move between QWERTY keys.
        mForceDismiss = false
        if (isShowing) {
            mForceDismiss = oldWidth - newWidth > 1 || newWidth - oldWidth > 1
        }
    }


    private inner class BalloonTimer : Handler(), Runnable {
        /**
         * The pending action.
         */
        var action: Int = 0
            private set

        private val mPositionInParent = IntArray(2)
        private var mWidth = 0
        private var mHeight = 0

        var isPending: Boolean = false
            private set

        fun startTimer(
            time: Long, action: Int, positionInParent: IntArray?,
            width: Int, height: Int
        ) {
            this.action = action
            if (TimerAction.HIDE != action) {
                mPositionInParent[0] = positionInParent!![0]
                mPositionInParent[1] = positionInParent[1]
            }
            mWidth = width
            mHeight = height
            postDelayed(this, time)
            isPending = true
        }

        fun removeTimer(): Boolean {
            if (isPending) {
                isPending = false
                removeCallbacks(this)
                return true
            }

            return false
        }

        override fun run() {
            when (action) {
                TimerAction.SHOW -> {
                    mParent!!.getLocationInWindow(mParentLocationInWindow)
                    showAtLocation(
                        mParent, Gravity.LEFT or Gravity.TOP,
                        mPositionInParent[0], mPositionInParent[1]
                                + mParentLocationInWindow[1]
                    )
                }

                TimerAction.HIDE -> dismiss()
                TimerAction.UPDATE -> {
                    mParent!!.getLocationInWindow(mParentLocationInWindow)
                    update(
                        mPositionInParent[0], mPositionInParent[1]
                                + mParentLocationInWindow[1], mWidth, mHeight
                    )
                }
            }
            isPending = false
        }

    }

    object TimerAction {
        const val SHOW: Int = 1
        const val HIDE: Int = 2
        const val UPDATE: Int = 3
    }

    inner class BalloonView(context: Context?) : View(context) {
        /**
         * The icon to be shown. If it is not null, [.mLabel] will be
         * ignored.
         */
        private var mIcon: Drawable? = null

        /**
         * The label to be shown. It is enabled only if [.mIcon] is null.
         */
        private var mLabel: String? = null

        private val mLabeColor = -0x1000000
        private val mPaintLabel = Paint()
        private var mFmi: FontMetricsInt

        /**
         * The width to show suspension points.
         */
        private var mSuspensionPointsWidth = 0f


        init {
            mPaintLabel.color = mLabeColor
            mPaintLabel.isAntiAlias = true
            mPaintLabel.isFakeBoldText = true
            mFmi = mPaintLabel.fontMetricsInt
        }

        fun setIcon(icon: Drawable?) {
            mIcon = icon
        }

        fun setTextConfig(
            label: String?, fontSize: Float,
            textBold: Boolean, textColor: Int
        ) {
            // Icon should be cleared so that the label will be enabled.
            mIcon = null
            mLabel = label
            mPaintLabel.textSize = fontSize
            mPaintLabel.isFakeBoldText = textBold
            mPaintLabel.color = textColor
            mFmi = mPaintLabel.fontMetricsInt
            mSuspensionPointsWidth = mPaintLabel.measureText(SUSPENSION_POINTS)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val widthMode = MeasureSpec.getMode(widthMeasureSpec)
            val heightMode = MeasureSpec.getMode(heightMeasureSpec)
            val widthSize = MeasureSpec.getSize(widthMeasureSpec)
            val heightSize = MeasureSpec.getSize(heightMeasureSpec)

            if (widthMode == MeasureSpec.EXACTLY) {
                setMeasuredDimension(widthSize, heightSize)
                return
            }

            var measuredWidth = paddingLeft + paddingRight
            var measuredHeight = paddingTop + paddingBottom
            if (null != mIcon) {
                measuredWidth += mIcon!!.intrinsicWidth
                measuredHeight += mIcon!!.intrinsicHeight
            } else if (null != mLabel) {
                measuredWidth += (mPaintLabel.measureText(mLabel)).toInt()
                measuredHeight += mFmi.bottom - mFmi.top
            }
            if (widthSize > measuredWidth || widthMode == MeasureSpec.AT_MOST) {
                measuredWidth = widthSize
            }

            if (heightSize > measuredHeight
                || heightMode == MeasureSpec.AT_MOST
            ) {
                measuredHeight = heightSize
            }

            val maxWidth: Int = Environment.instance.screenWidth -
                    paddingLeft - paddingRight
            if (measuredWidth > maxWidth) {
                measuredWidth = maxWidth
            }
            setMeasuredDimension(measuredWidth, measuredHeight)
        }

        override fun onDraw(canvas: Canvas) {
            val width = width
            val height = height
            if (null != mIcon) {
                val marginLeft = (width - mIcon!!.intrinsicWidth) / 2
                val marginRight = (width - mIcon!!.intrinsicWidth
                        - marginLeft)
                val marginTop = (height - mIcon!!.intrinsicHeight) / 2
                val marginBottom = (height - mIcon!!.intrinsicHeight
                        - marginTop)
                mIcon!!.setBounds(
                    marginLeft, marginTop, width - marginRight,
                    height - marginBottom
                )
                mIcon!!.draw(canvas)
            } else if (null != mLabel) {
                val labelMeasuredWidth = mPaintLabel.measureText(mLabel)
                var x = paddingLeft.toFloat()
                x += (width - labelMeasuredWidth - paddingLeft - paddingRight) / 2.0f
                var labelToDraw = mLabel
                if (x < paddingLeft) {
                    x = paddingLeft.toFloat()
                    labelToDraw = getLimitedLabelForDrawing(
                        mLabel!!,
                        (width - paddingLeft - paddingRight).toFloat()
                    )
                }

                val fontHeight = mFmi.bottom - mFmi.top
                val marginY = (height - fontHeight) / 2.0f
                val y = marginY - mFmi.top
                canvas.drawText(labelToDraw!!, x, y, mPaintLabel)
            }
        }

        fun getLimitedLabelForDrawing(
            rawLabel: String,
            widthToDraw: Float
        ): String {
            var subLen = rawLabel.length
            if (subLen <= 1) return rawLabel
            do {
                subLen--
                val width = mPaintLabel.measureText(rawLabel, 0, subLen)
                if (width + mSuspensionPointsWidth <= widthToDraw || 1 >= subLen) {
                    return rawLabel.substring(0, subLen) +
                            SUSPENSION_POINTS
                }
            } while (true)
        }
    }

    companion object {
        /**
         * Delayed time to show the balloon hint.
         */
        const val TIME_DELAY_SHOW: Int = 0

        /**
         * Delayed time to dismiss the balloon hint.
         */
        const val TIME_DELAY_DISMISS: Int = 200

        private const val SUSPENSION_POINTS = "..."
    }
}
