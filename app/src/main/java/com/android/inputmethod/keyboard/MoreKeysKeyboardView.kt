/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.inputmethod.keyboard

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.android.inputmethod.accessibility.AccessibilityUtils
import com.android.inputmethod.accessibility.MoreKeysKeyboardAccessibilityDelegate
import com.android.inputmethod.keyboard.MoreKeysKeyboard.MoreKeyDivider
import com.android.inputmethod.keyboard.internal.KeyDrawParams
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.CoordinateUtils
import kotlin.math.max
import kotlin.math.min

/**
 * A view that renders a virtual [MoreKeysKeyboard]. It handles rendering of keys and
 * detecting key presses and touch movements.
 */
open class MoreKeysKeyboardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet?,
    defStyle: Int = R.attr.moreKeysKeyboardViewStyle
) :
    KeyboardView(context, attrs, defStyle), MoreKeysPanel {
    private val mCoordinates: IntArray = CoordinateUtils.newInstance()

    private val mDivider: Drawable?
    protected val mKeyDetector: KeyDetector
    private var mController: MoreKeysPanel.Controller = MoreKeysPanel.EMPTY_CONTROLLER
    protected var mListener: KeyboardActionListener? = null
    private var mOriginX: Int = 0
    private var mOriginY: Int = 0
    private var mCurrentKey: Key? = null

    private var mActivePointerId: Int = 0

    protected var mAccessibilityDelegate: MoreKeysKeyboardAccessibilityDelegate? = null

    init {
        val moreKeysKeyboardViewAttr: TypedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.MoreKeysKeyboardView, defStyle, R.style.MoreKeysKeyboardView
        )
        mDivider = moreKeysKeyboardViewAttr.getDrawable(R.styleable.MoreKeysKeyboardView_divider)
        if (mDivider != null) {
            // TODO: Drawable itself should have an alpha value.
            mDivider.setAlpha(128)
        }
        moreKeysKeyboardViewAttr.recycle()
        mKeyDetector = MoreKeysDetector(
            getResources().getDimension(
                R.dimen.config_more_keys_keyboard_slide_allowance
            )
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val keyboard: Keyboard? = this.keyboard
        if (keyboard != null) {
            val width: Int = keyboard.mOccupiedWidth + getPaddingLeft() + getPaddingRight()
            val height: Int = keyboard.mOccupiedHeight + getPaddingTop() + getPaddingBottom()
            setMeasuredDimension(width, height)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onDrawKeyTopVisuals(
        key: Key, canvas: Canvas, paint: Paint,
        params: KeyDrawParams
    ) {
        if (!key.isSpacer || key !is MoreKeyDivider || mDivider == null) {
            super.onDrawKeyTopVisuals(key, canvas, paint, params)
            return
        }
        val keyWidth: Int = key.drawWidth
        val keyHeight: Int = key.height
        val iconWidth: Int =
            min(mDivider.getIntrinsicWidth().toDouble(), keyWidth.toDouble()).toInt()
        val iconHeight: Int = mDivider.getIntrinsicHeight()
        val iconX: Int = (keyWidth - iconWidth) / 2 // Align horizontally center
        val iconY: Int = (keyHeight - iconHeight) / 2 // Align vertically center
        drawIcon(canvas, mDivider, iconX, iconY, iconWidth, iconHeight)
    }

    override var keyboard: Keyboard?
        get() = super.keyboard
        set(keyboard) {
            super.keyboard = keyboard
            mKeyDetector.setKeyboard(
                keyboard!!, -getPaddingLeft().toFloat(), -paddingTop + verticalCorrection
            )
            if (AccessibilityUtils.instance.isAccessibilityEnabled()) {
                if (mAccessibilityDelegate == null) {
                    mAccessibilityDelegate = MoreKeysKeyboardAccessibilityDelegate(
                        this, mKeyDetector
                    )
                    mAccessibilityDelegate!!.setOpenAnnounce(R.string.spoken_open_more_keys_keyboard)
                    mAccessibilityDelegate!!.setCloseAnnounce(R.string.spoken_close_more_keys_keyboard)
                }
                mAccessibilityDelegate!!.setKeyboard(keyboard)
            } else {
                mAccessibilityDelegate = null
            }
        }

    override fun showMoreKeysPanel(
        parentView: View, controller: MoreKeysPanel.Controller,
        pointX: Int, pointY: Int, listener: KeyboardActionListener?
    ) {
        mController = controller
        mListener = listener
        val container: View = containerView
        // The coordinates of panel's left-top corner in parentView's coordinate system.
        // We need to consider background drawable paddings.
        val x: Int = pointX - defaultCoordX - container.getPaddingLeft() - getPaddingLeft()
        val y: Int = (pointY - container.getMeasuredHeight() + container.getPaddingBottom()
                + getPaddingBottom())

        parentView.getLocationInWindow(mCoordinates)
        // Ensure the horizontal position of the panel does not extend past the parentView edges.
        val maxX: Int = parentView.getMeasuredWidth() - container.getMeasuredWidth()
        val panelX: Int = (max(
            0.0,
            min(maxX.toDouble(), x.toDouble())
        ) + CoordinateUtils.x(
            mCoordinates
        )).toInt()
        val panelY: Int = y + CoordinateUtils.y(mCoordinates)
        container.setX(panelX.toFloat())
        container.setY(panelY.toFloat())

        mOriginX = x + container.getPaddingLeft()
        mOriginY = y + container.getPaddingTop()
        controller.onShowMoreKeysPanel(this)
        val accessibilityDelegate: MoreKeysKeyboardAccessibilityDelegate? = mAccessibilityDelegate
        if (accessibilityDelegate != null
            && AccessibilityUtils.instance.isAccessibilityEnabled()
        ) {
            accessibilityDelegate.onShowMoreKeysKeyboard()
        }
    }

    protected open val defaultCoordX: Int
        /**
         * Returns the default x coordinate for showing this panel.
         */
        get() {
            return (keyboard as MoreKeysKeyboard).getDefaultCoordX()
        }

    override fun onDownEvent(x: Int, y: Int, pointerId: Int, eventTime: Long) {
        mActivePointerId = pointerId
        mCurrentKey = detectKey(x, y)
    }

    override fun onMoveEvent(x: Int, y: Int, pointerId: Int, eventTime: Long) {
        if (mActivePointerId != pointerId) {
            return
        }
        val hasOldKey: Boolean = (mCurrentKey != null)
        mCurrentKey = detectKey(x, y)
        if (hasOldKey && mCurrentKey == null) {
            // A more keys keyboard is canceled when detecting no key.
            mController.onCancelMoreKeysPanel()
        }
    }

    override fun onUpEvent(x: Int, y: Int, pointerId: Int, eventTime: Long) {
        if (mActivePointerId != pointerId) {
            return
        }
        // Calling {@link #detectKey(int,int,int)} here is harmless because the last move event and
        // the following up event share the same coordinates.
        mCurrentKey = detectKey(x, y)
        if (mCurrentKey != null) {
            updateReleaseKeyGraphics(mCurrentKey!!)
            onKeyInput(mCurrentKey!!, x, y)
            mCurrentKey = null
        }
    }

    /**
     * Performs the specific action for this panel when the user presses a key on the panel.
     */
    protected open fun onKeyInput(key: Key, x: Int, y: Int) {
        val code: Int = key.code
        if (code == Constants.CODE_OUTPUT_TEXT) {
            mListener!!.onTextInput(mCurrentKey?.outputText)
        } else if (code != Constants.CODE_UNSPECIFIED) {
            if (keyboard?.hasProximityCharsCorrection(code) == true) {
                mListener!!.onCodeInput(code, x, y, false /* isKeyRepeat */)
            } else {
                mListener!!.onCodeInput(
                    code, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE,
                    false /* isKeyRepeat */
                )
            }
        }
    }

    private fun detectKey(x: Int, y: Int): Key? {
        val oldKey: Key? = mCurrentKey
        val newKey: Key? = mKeyDetector.detectHitKey(x, y)
        if (newKey === oldKey) {
            return newKey
        }
        // A new key is detected.
        if (oldKey != null) {
            updateReleaseKeyGraphics(oldKey)
            invalidateKey(oldKey)
        }
        if (newKey != null) {
            updatePressKeyGraphics(newKey)
            invalidateKey(newKey)
        }
        return newKey
    }

    private fun updateReleaseKeyGraphics(key: Key) {
        key.onReleased()
        invalidateKey(key)
    }

    private fun updatePressKeyGraphics(key: Key) {
        key.onPressed()
        invalidateKey(key)
    }

    override fun dismissMoreKeysPanel() {
        if (!isShowingInParent) {
            return
        }
        val accessibilityDelegate: MoreKeysKeyboardAccessibilityDelegate? = mAccessibilityDelegate
        if (accessibilityDelegate != null
            && AccessibilityUtils.instance.isAccessibilityEnabled()
        ) {
            accessibilityDelegate.onDismissMoreKeysKeyboard()
        }
        mController.onDismissMoreKeysPanel()
    }

    override fun translateX(x: Int): Int {
        return x - mOriginX
    }

    override fun translateY(y: Int): Int {
        return y - mOriginY
    }

    override fun onTouchEvent(me: MotionEvent): Boolean {
        val action: Int = me.getActionMasked()
        val eventTime: Long = me.getEventTime()
        val index: Int = me.getActionIndex()
        val x: Int = me.getX(index).toInt()
        val y: Int = me.getY(index).toInt()
        val pointerId: Int = me.getPointerId(index)
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> onDownEvent(
                x,
                y,
                pointerId,
                eventTime
            )

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> onUpEvent(
                x,
                y,
                pointerId,
                eventTime
            )

            MotionEvent.ACTION_MOVE -> onMoveEvent(x, y, pointerId, eventTime)
        }
        return true
    }

    /**
     * {@inheritDoc}
     */
    override fun onHoverEvent(event: MotionEvent): Boolean {
        val accessibilityDelegate: MoreKeysKeyboardAccessibilityDelegate? = mAccessibilityDelegate
        if (accessibilityDelegate != null
            && AccessibilityUtils.instance.isTouchExplorationEnabled()
        ) {
            return accessibilityDelegate.onHoverEvent(event)
        }
        return super.onHoverEvent(event)
    }

    private val containerView: View
        get() {
            return getParent() as View
        }

    override fun showInParent(parentView: ViewGroup) {
        removeFromParent()
        parentView.addView(containerView)
    }

    override fun removeFromParent() {
        val containerView: View = containerView
        val currentParent: ViewGroup? = containerView.getParent() as ViewGroup?
        if (currentParent != null) {
            currentParent.removeView(containerView)
        }
    }

    override val isShowingInParent: Boolean
        get() {
            return (containerView.getParent() != null)
        }
}
