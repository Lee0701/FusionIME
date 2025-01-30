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
import android.graphics.drawable.Drawable
import android.os.Vibrator
import android.util.AttributeSet
import android.view.View
import com.android.inputmethod.pinyin.SkbContainer.LongPressTimer

/**
 * Class used to show a soft keyboard.
 *
 * A soft keyboard view should not handle touch event itself, because we do bias
 * correction, need a global strategy to map an event into a proper view to
 * achieve better user experience.
 */
class SoftKeyboardView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {
    /**
     * The definition of the soft keyboard for the current this soft keyboard
     * view.
     */
    var softKeyboard: SoftKeyboard? = null
        private set

    /**
     * The popup balloon hint for key press/release.
     */
    private var mBalloonPopup: BalloonHint? = null

    /**
     * The on-key balloon hint for key press/release. If it is null, on-key
     * highlight will be drawn on th soft keyboard view directly.
     */
    private var mBalloonOnKey: BalloonHint? = null

    /** Used to play key sounds.  */
    private val mSoundManager: SoundManager? = SoundManager.getInstance(getContext())

    /** The last key pressed.  */
    private var mSoftKeyDown: SoftKey? = null

    /** Used to indicate whether the user is holding on a key.  */
    private var mKeyPressed = false

    /**
     * The location offset of the view to the keyboard container.
     */
    private val mOffsetToSkbContainer = IntArray(2)

    /**
     * The location of the desired hint view to the keyboard container.
     */
    private val mHintLocationToSkbContainer = IntArray(2)

    /**
     * Text size for normal key.
     */
    private var mNormalKeyTextSize = 0

    /**
     * Text size for function key.
     */
    private var mFunctionKeyTextSize = 0

    /**
     * Long press timer used to response long-press.
     */
    private var mLongPressTimer: LongPressTimer? = null

    /**
     * Repeated events for long press
     */
    private val mRepeatForLongPress = false

    /**
     * If this parameter is true, the balloon will never be dismissed even if
     * user moves a lot from the pressed point.
     */
    private var mMovingNeverHidePopupBalloon = false

    /** Vibration for key press.  */
    private var mVibrator: Vibrator? = null

    /** Vibration pattern for key press.  */
    protected var mVibratePattern: LongArray = longArrayOf(1, 20)

    /**
     * The dirty rectangle used to mark the area to re-draw during key press and
     * release. Currently, whenever we can invalidate(Rect), view will call
     * onDraw() and we MUST draw the whole view. This dirty information is for
     * future use.
     */
    private val mDirtyRect = Rect()

    private val mPaint = Paint()
    private val mFmi: FontMetricsInt
    private var mDimSkb = false

    init {
        mPaint.isAntiAlias = true
        mFmi = mPaint.fontMetricsInt
    }

    fun setSoftKeyboard(softSkb: SoftKeyboard?): Boolean {
        if (null == softSkb) {
            return false
        }
        softKeyboard = softSkb
        val bg = softSkb.skbBackground
        if (null != bg) setBackgroundDrawable(bg)
        return true
    }

    fun resizeKeyboard(skbWidth: Int, skbHeight: Int) {
        softKeyboard!!.setSkbCoreSize(skbWidth, skbHeight)
    }

    fun setBalloonHint(
        balloonOnKey: BalloonHint?,
        balloonPopup: BalloonHint?, movingNeverHidePopup: Boolean
    ) {
        mBalloonOnKey = balloonOnKey
        mBalloonPopup = balloonPopup
        mMovingNeverHidePopupBalloon = movingNeverHidePopup
    }

    fun setOffsetToSkbContainer(offsetToSkbContainer: IntArray) {
        mOffsetToSkbContainer[0] = offsetToSkbContainer[0]
        mOffsetToSkbContainer[1] = offsetToSkbContainer[1]
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var measuredWidth = 0
        var measuredHeight = 0
        if (null != softKeyboard) {
            measuredWidth = softKeyboard!!.skbCoreWidth
            measuredHeight = softKeyboard!!.skbCoreHeight
            measuredWidth += paddingLeft + paddingRight
            measuredHeight += paddingTop + paddingBottom
        }
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private fun showBalloon(
        balloon: BalloonHint, balloonLocationToSkb: IntArray,
        movePress: Boolean
    ) {
        var delay: Long = BalloonHint.TIME_DELAY_SHOW.toLong()
        if (movePress) delay = 0
        if (balloon.needForceDismiss()) {
            balloon.delayedDismiss(0)
        }
        if (!balloon.isShowing) {
            balloon.delayedShow(delay, balloonLocationToSkb)
        } else {
            balloon.delayedUpdate(
                delay, balloonLocationToSkb, balloon
                    .width, balloon.height
            )
        }
        val b = System.currentTimeMillis()
    }

    fun resetKeyPress(balloonDelay: Long) {
        if (!mKeyPressed) return
        mKeyPressed = false
        if (null != mBalloonOnKey) {
            mBalloonOnKey!!.delayedDismiss(balloonDelay)
        } else {
            if (null != mSoftKeyDown) {
                if (mDirtyRect.isEmpty) {
                    mDirtyRect[mSoftKeyDown!!.mLeft, mSoftKeyDown!!.mTop, mSoftKeyDown!!.mRight] =
                        mSoftKeyDown!!.mBottom
                }
                invalidate(mDirtyRect)
            } else {
                invalidate()
            }
        }
        mBalloonPopup!!.delayedDismiss(balloonDelay)
    }

    // If movePress is true, means that this function is called because user
    // moves his finger to this button. If movePress is false, means that this
    // function is called when user just presses this key.
    fun onKeyPress(
        x: Int, y: Int,
        longPressTimer: LongPressTimer, movePress: Boolean
    ): SoftKey? {
        mKeyPressed = false
        var moveWithinPreviousKey = false
        if (movePress) {
            val newKey = softKeyboard!!.mapToKey(x, y)
            if (newKey === mSoftKeyDown) moveWithinPreviousKey = true
            mSoftKeyDown = newKey
        } else {
            mSoftKeyDown = softKeyboard!!.mapToKey(x, y)
        }
        if (moveWithinPreviousKey || null == mSoftKeyDown) return mSoftKeyDown
        mKeyPressed = true

        if (!movePress) {
            tryPlayKeyDown()
            tryVibrate()
        }

        mLongPressTimer = longPressTimer

        if (!movePress) {
            if (mSoftKeyDown!!.popupResId > 0 || mSoftKeyDown!!.repeatable()) {
                mLongPressTimer!!.startTimer()
            }
        } else {
            mLongPressTimer!!.removeTimer()
        }

        var desired_width: Int
        var desired_height: Int
        var textSize: Float
        val env: Environment = Environment.instance

        if (null != mBalloonOnKey) {
            val keyHlBg = mSoftKeyDown!!.keyHlBg
            mBalloonOnKey!!.setBalloonBackground(keyHlBg)

            // Prepare the on-key balloon
            val keyXMargin = softKeyboard!!.keyXMargin
            val keyYMargin = softKeyboard!!.keyYMargin
            desired_width = mSoftKeyDown!!.width() - 2 * keyXMargin
            desired_height = mSoftKeyDown!!.height() - 2 * keyYMargin
            textSize = env
                .getKeyTextSize(SoftKeyType.KEYTYPE_ID_NORMAL_KEY != mSoftKeyDown!!.mKeyType!!.mKeyTypeId)
                .toFloat()
            val icon = mSoftKeyDown!!.keyIcon
            if (null != icon) {
                mBalloonOnKey!!.setBalloonConfig(
                    icon, desired_width,
                    desired_height
                )
            } else {
                mBalloonOnKey!!.setBalloonConfig(
                    mSoftKeyDown!!.keyLabel,
                    textSize, true, mSoftKeyDown!!.colorHl,
                    desired_width, desired_height
                )
            }

            mHintLocationToSkbContainer[0] = (paddingLeft + mSoftKeyDown!!.mLeft
                    - (mBalloonOnKey!!.width - mSoftKeyDown!!.width()) / 2)
            mHintLocationToSkbContainer[0] += mOffsetToSkbContainer[0]
            mHintLocationToSkbContainer[1] = (paddingTop
                    + (mSoftKeyDown!!.mBottom - keyYMargin)
                    - mBalloonOnKey!!.height)
            mHintLocationToSkbContainer[1] += mOffsetToSkbContainer[1]
            showBalloon(mBalloonOnKey!!, mHintLocationToSkbContainer, movePress)
        } else {
            mDirtyRect.union(
                mSoftKeyDown!!.mLeft, mSoftKeyDown!!.mTop,
                mSoftKeyDown!!.mRight, mSoftKeyDown!!.mBottom
            )
            invalidate(mDirtyRect)
        }

        // Prepare the popup balloon
        if (mSoftKeyDown!!.needBalloon()) {
            val balloonBg = softKeyboard!!.balloonBackground
            mBalloonPopup!!.setBalloonBackground(balloonBg)

            desired_width = mSoftKeyDown!!.width() + env.keyBalloonWidthPlus
            desired_height = (mSoftKeyDown!!.height()
                    + env.keyBalloonHeightPlus)
            textSize = env
                .getBalloonTextSize(SoftKeyType.KEYTYPE_ID_NORMAL_KEY != mSoftKeyDown!!.mKeyType!!.mKeyTypeId)
                .toFloat()
            val iconPopup = mSoftKeyDown!!.keyIconPopup
            if (null != iconPopup) {
                mBalloonPopup!!.setBalloonConfig(
                    iconPopup, desired_width,
                    desired_height
                )
            } else {
                mBalloonPopup!!.setBalloonConfig(
                    mSoftKeyDown!!.keyLabel,
                    textSize, mSoftKeyDown!!.needBalloon(), mSoftKeyDown!!.colorBalloon,
                    desired_width, desired_height
                )
            }

            // The position to show.
            mHintLocationToSkbContainer[0] = (paddingLeft + mSoftKeyDown!!.mLeft
                    + -(mBalloonPopup!!.width - mSoftKeyDown!!.width()) / 2)
            mHintLocationToSkbContainer[0] += mOffsetToSkbContainer[0]
            mHintLocationToSkbContainer[1] = (paddingTop + mSoftKeyDown!!.mTop
                    - mBalloonPopup!!.height)
            mHintLocationToSkbContainer[1] += mOffsetToSkbContainer[1]
            showBalloon(mBalloonPopup!!, mHintLocationToSkbContainer, movePress)
        } else {
            mBalloonPopup!!.delayedDismiss(0)
        }

        if (mRepeatForLongPress) longPressTimer.startTimer()
        return mSoftKeyDown
    }

    fun onKeyRelease(x: Int, y: Int): SoftKey? {
        mKeyPressed = false
        if (null == mSoftKeyDown) return null

        mLongPressTimer!!.removeTimer()

        if (null != mBalloonOnKey) {
            mBalloonOnKey!!.delayedDismiss(BalloonHint.TIME_DELAY_DISMISS.toLong())
        } else {
            mDirtyRect.union(
                mSoftKeyDown!!.mLeft, mSoftKeyDown!!.mTop,
                mSoftKeyDown!!.mRight, mSoftKeyDown!!.mBottom
            )
            invalidate(mDirtyRect)
        }

        if (mSoftKeyDown!!.needBalloon()) {
            mBalloonPopup!!.delayedDismiss(BalloonHint.TIME_DELAY_DISMISS.toLong())
        }

        if (mSoftKeyDown!!.moveWithinKey(x - paddingLeft, y - paddingTop)) {
            return mSoftKeyDown
        }
        return null
    }

    fun onKeyMove(x: Int, y: Int): SoftKey? {
        if (null == mSoftKeyDown) return null

        if (mSoftKeyDown!!.moveWithinKey(x - paddingLeft, y - paddingTop)) {
            return mSoftKeyDown
        }

        // The current key needs to be updated.
        mDirtyRect.union(
            mSoftKeyDown!!.mLeft, mSoftKeyDown!!.mTop,
            mSoftKeyDown!!.mRight, mSoftKeyDown!!.mBottom
        )

        if (mRepeatForLongPress) {
            if (mMovingNeverHidePopupBalloon) {
                return onKeyPress(x, y, mLongPressTimer!!, true)
            }

            if (null != mBalloonOnKey) {
                mBalloonOnKey!!.delayedDismiss(0)
            } else {
                invalidate(mDirtyRect)
            }

            if (mSoftKeyDown!!.needBalloon()) {
                mBalloonPopup!!.delayedDismiss(0)
            }

            if (null != mLongPressTimer) {
                mLongPressTimer!!.removeTimer()
            }
            return onKeyPress(x, y, mLongPressTimer!!, true)
        } else {
            // When user moves between keys, repeated response is disabled.
            return onKeyPress(x, y, mLongPressTimer!!, true)
        }
    }

    private fun tryVibrate() {
        if (!Settings.vibrate) {
            return
        }
        if (mVibrator == null) {
            mVibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        mVibrator!!.vibrate(mVibratePattern, -1)
    }

    private fun tryPlayKeyDown() {
        if (Settings.keySound) {
            mSoundManager!!.playKeyDown()
        }
    }

    fun dimSoftKeyboard(dimSkb: Boolean) {
        mDimSkb = dimSkb
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (null == softKeyboard) return

        canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())

        val env: Environment = Environment.instance
        mNormalKeyTextSize = env.getKeyTextSize(false)
        mFunctionKeyTextSize = env.getKeyTextSize(true)
        // Draw the last soft keyboard
        val rowNum = softKeyboard!!.rowNum
        val keyXMargin = softKeyboard!!.keyXMargin
        val keyYMargin = softKeyboard!!.keyYMargin
        for (row in 0 until rowNum) {
            val keyRow = softKeyboard!!.getKeyRowForDisplay(row) ?: continue
            val softKeys = keyRow.mSoftKeys
            val keyNum = softKeys!!.size
            for (i in 0 until keyNum) {
                val softKey = softKeys[i]
                if (SoftKeyType.KEYTYPE_ID_NORMAL_KEY == softKey!!.mKeyType!!.mKeyTypeId) {
                    mPaint.textSize = mNormalKeyTextSize.toFloat()
                } else {
                    mPaint.textSize = mFunctionKeyTextSize.toFloat()
                }
                drawSoftKey(canvas, softKey, keyXMargin, keyYMargin)
            }
        }

        if (mDimSkb) {
            mPaint.color = -0x60000000
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), mPaint)
        }

        mDirtyRect.setEmpty()
    }

    private fun drawSoftKey(
        canvas: Canvas, softKey: SoftKey, keyXMargin: Int,
        keyYMargin: Int
    ) {
        val bg: Drawable?
        val textColor: Int
        if (mKeyPressed && softKey === mSoftKeyDown) {
            bg = softKey.keyHlBg
            textColor = softKey.colorHl
        } else {
            bg = softKey.keyBg
            textColor = softKey.color
        }

        if (null != bg) {
            bg.setBounds(
                softKey.mLeft + keyXMargin, softKey.mTop + keyYMargin,
                softKey.mRight - keyXMargin, softKey.mBottom - keyYMargin
            )
            bg.draw(canvas)
        }

        val keyLabel = softKey.keyLabel
        val keyIcon = softKey.keyIcon
        if (null != keyIcon) {
            val icon = keyIcon
            val marginLeft = (softKey.width() - icon.intrinsicWidth) / 2
            val marginRight = (softKey.width() - icon.intrinsicWidth
                    - marginLeft)
            val marginTop = (softKey.height() - icon.intrinsicHeight) / 2
            val marginBottom = (softKey.height() - icon.intrinsicHeight
                    - marginTop)
            icon.setBounds(
                softKey.mLeft + marginLeft,
                softKey.mTop + marginTop, softKey.mRight - marginRight,
                softKey.mBottom - marginBottom
            )
            icon.draw(canvas)
        } else if (null != keyLabel) {
            mPaint.color = textColor
            val x = (softKey.mLeft
                    + (softKey.width() - mPaint.measureText(keyLabel)) / 2.0f)
            val fontHeight = mFmi.bottom - mFmi.top
            val marginY = (softKey.height() - fontHeight) / 2.0f
            val y = softKey.mTop + marginY - mFmi.top + mFmi.bottom / 1.5f
            canvas.drawText(keyLabel, x, y + 1, mPaint)
        }
    }
}
