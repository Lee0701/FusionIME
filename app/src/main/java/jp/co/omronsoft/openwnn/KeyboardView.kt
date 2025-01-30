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
/* This file is porting from Android framework.
 *   frameworks/base/core/java/android/inputmethodservice/KeyboardView.java
 *
 *package android.inputmethodservice;
 */
package jp.co.omronsoft.openwnn

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Region
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.PopupWindow
import android.widget.TextView
import ee.oyatl.ime.fusion.R
import java.util.Arrays
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A view that renders a virtual [Keyboard]. It handles rendering of keys and
 * detecting key presses and touch movements.
 */
class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyle: Int = 0
) :
    View(context, attrs, defStyle), View.OnClickListener {
    /**
     * Listener for virtual keyboard events.
     */
    interface OnKeyboardActionListener {
        /**
         * Called when the user presses a key. This is sent before the [.onKey] is called.
         * For keys that repeat, this is only called once.
         * @param primaryCode the unicode of the key being pressed. If the touch is not on a valid
         * key, the value will be zero.
         */
        fun onPress(primaryCode: Int)

        /**
         * Called when the user releases a key. This is sent after the [.onKey] is called.
         * For keys that repeat, this is only called once.
         * @param primaryCode the code of the key that was released
         */
        fun onRelease(primaryCode: Int)

        /**
         * Send a key press to the listener.
         * @param primaryCode this is the key that was pressed
         * @param keyCodes the codes for all the possible alternative keys
         * with the primary code being the first. If the primary key code is
         * a single character such as an alphabet or number or symbol, the alternatives
         * will include other characters that may be on the same key or adjacent keys.
         * These codes are useful to correct for accidental presses of a key adjacent to
         * the intended key.
         */
        fun onKey(primaryCode: Int, keyCodes: IntArray?)

        /**
         * Sends a sequence of characters to the listener.
         * @param text the sequence of characters to be displayed.
         */
        fun onText(text: CharSequence?)

        /**
         * Called when the user quickly moves the finger from right to left.
         */
        fun swipeLeft()

        /**
         * Called when the user quickly moves the finger from left to right.
         */
        fun swipeRight()

        /**
         * Called when the user quickly moves the finger from up to down.
         */
        fun swipeDown()

        /**
         * Called when the user quickly moves the finger from down to up.
         */
        fun swipeUp()

        /**
         * Called when the user long presses a key.
         * @param key the key that was long pressed
         * @return true if the long press is handled, false otherwise.
         */
        fun onLongPress(key: Keyboard.Key): Boolean
    }

    private var mKeyboard: Keyboard? = null
    private var mCurrentKeyIndex = NOT_A_KEY
    private var mLabelTextSize = 0
    private var mKeyTextSize = 0
    private var mKeyTextColor = 0
    private val mKeyTextColor2nd: Int
    private var mShadowRadius = 0f
    private var mShadowColor = 0
    private val mBackgroundDimAmount: Float

    private var mPreviewText: TextView? = null
    private val mPreviewPopup: PopupWindow
    private var mPreviewTextSizeLarge = 0
    private var mPreviewOffset = 0
    private var mPreviewHeight = 0
    private var mOffsetInWindow: IntArray?

    private val mPopupKeyboard: PopupWindow
    private var mMiniKeyboardContainer: View? = null
    private var mMiniKeyboard: KeyboardView? = null
    private var mMiniKeyboardOnScreen = false
    private var mPopupParent: View?
    private var mMiniKeyboardOffsetX = 0
    private var mMiniKeyboardOffsetY = 0
    private val mMiniKeyboardCache: MutableMap<Keyboard.Key, View?>
    private var mWindowOffset: IntArray?
    private var mKeys: Array<Keyboard.Key>?

    /**
     * Returns the [OnKeyboardActionListener] object.
     * @return the listener attached to this keyboard
     */
    /**
     * Set the [OnKeyboardActionListener] object.
     * @param listener  The OnKeyboardActionListener to set.
     */
    /** Listener for [OnKeyboardActionListener].  */
    protected var onKeyboardActionListener: OnKeyboardActionListener? = null

    private var mVerticalCorrection = 0
    private var mProximityThreshold = 0

    private val mPreviewCentered = false
    /**
     * Returns the enabled state of the key feedback popup.
     * @return whether or not the key feedback popup is enabled
     * @see .setPreviewEnabled
     */
    /**
     * Enables or disables the key feedback popup. This is a popup that shows a magnified
     * version of the depressed key. By default the preview is enabled.
     * @param previewEnabled whether or not to enable the key feedback popup
     * @see .isPreviewEnabled
     */
    var isPreviewEnabled: Boolean = true
    private val mShowTouchPoints = true
    private var mPopupPreviewX = 0
    private var mPopupPreviewY = 0
    private var mWindowY = 0

    private var mLastX = 0
    private var mLastY = 0
    private var mStartX = 0
    private var mStartY = 0

    /**
     * Returns true if proximity correction is enabled.
     */
    /**
     * When enabled, calls to [OnKeyboardActionListener.onKey] will include key
     * codes for adjacent keys.  When disabled, only the primary key code will be
     * reported.
     * @param enabled whether or not the proximity correction is enabled
     */
    var isProximityCorrectionEnabled: Boolean = false

    private val mPaint: Paint
    private val mPadding: Rect

    private var mDownTime: Long = 0
    private var mLastMoveTime: Long = 0
    private var mLastKey = 0
    private var mLastCodeX = 0
    private var mLastCodeY = 0
    private var mCurrentKey = NOT_A_KEY
    private var mDownKey = NOT_A_KEY
    private var mLastKeyTime: Long = 0
    private var mCurrentKeyTime: Long = 0
    private val mKeyIndices = IntArray(12)
    private var mGestureDetector: GestureDetector? = null
    private var mPopupX = 0
    private var mPopupY = 0
    private var mRepeatKeyIndex = NOT_A_KEY
    private var mPopupLayout = 0
    private var mAbortKey = false
    private var mInvalidatedKey: Keyboard.Key? = null
    private val mClipRegion = Rect(0, 0, 0, 0)
    private var mPossiblePoly = false
    private val mSwipeTracker = SwipeTracker()
    private val mSwipeThreshold: Int
    private val mDisambiguateSwipe: Boolean

    private var mOldPointerCount = 1
    private var mOldPointerX = 0f
    private var mOldPointerY = 0f

    private var mKeyBackground: Drawable? = null
    private val mKeyBackground2nd: Drawable?

    private val mDistances = IntArray(MAX_NEARBY_KEYS)

    private var mLastSentIndex = 0
    private var mTapCount = 0
    private var mLastTapTime: Long = 0
    private var mInMultiTap = false
    private val mPreviewLabel = StringBuilder(1)

    /** Whether the keyboard bitmap needs to be redrawn before it's blitted.  */
    private var mDrawPending = false

    /** The dirty region in the keyboard bitmap  */
    private val mDirtyRect = Rect()

    /** The keyboard bitmap for faster updates  */
    private var mBuffer: Bitmap? = null

    /** Notes if the keyboard just changed, so that we could possibly reallocate the mBuffer.  */
    private var mKeyboardChanged = false

    /** The canvas for the above mutable keyboard bitmap  */
    private var mCanvas: Canvas? = null

    var mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_SHOW_PREVIEW -> showKey(msg.arg1)
                MSG_REMOVE_PREVIEW -> mPreviewText!!.visibility =
                    INVISIBLE

                MSG_REPEAT -> if (repeatKey()) {
                    val repeat = Message.obtain(this, MSG_REPEAT)
                    sendMessageDelayed(repeat, REPEAT_INTERVAL.toLong())
                }

                MSG_LONGPRESS -> openPopupIfRequired(msg.obj as MotionEvent)
            }
        }
    }

    private fun initGestureDetector() {
        mGestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
            override fun onFling(
                me1: MotionEvent?, me2: MotionEvent,
                velocityX: Float, velocityY: Float
            ): Boolean {
                if (mPossiblePoly) return false
                val absX = abs(velocityX.toDouble()).toFloat()
                val absY = abs(velocityY.toDouble()).toFloat()
                val deltaX = me2.x - me1!!.x
                val deltaY = me2.y - me1.y
                val travelX = width / 2
                val travelY = height / 2
                mSwipeTracker.computeCurrentVelocity(1000)
                val endingVelocityX: Float = mSwipeTracker.getXVelocity()
                val endingVelocityY: Float = mSwipeTracker.getYVelocity()
                var sendDownKey = false
                if (velocityX > mSwipeThreshold && absY < absX && deltaX > travelX) {
                    if (mDisambiguateSwipe && endingVelocityX < velocityX / 4) {
                        sendDownKey = true
                    } else {
                        swipeRight()
                        return true
                    }
                } else if (velocityX < -mSwipeThreshold && absY < absX && deltaX < -travelX) {
                    if (mDisambiguateSwipe && endingVelocityX > velocityX / 4) {
                        sendDownKey = true
                    } else {
                        swipeLeft()
                        return true
                    }
                } else if (velocityY < -mSwipeThreshold && absX < absY && deltaY < -travelY) {
                    if (mDisambiguateSwipe && endingVelocityY > velocityY / 4) {
                        sendDownKey = true
                    } else {
                        swipeUp()
                        return true
                    }
                } else if (velocityY > mSwipeThreshold && absX < absY / 2 && deltaY > travelY) {
                    if (mDisambiguateSwipe && endingVelocityY < velocityY / 4) {
                        sendDownKey = true
                    } else {
                        swipeDown()
                        return true
                    }
                }

                if (sendDownKey) {
                    detectAndSendKey(mDownKey, mStartX, mStartY, me1.eventTime)
                }
                return false
            }
        })

        mGestureDetector!!.setIsLongpressEnabled(false)
    }

    var keyboard: Keyboard?
        /**
         * Returns the current keyboard being displayed by this view.
         * @return the currently attached keyboard
         * @see .setKeyboard
         */
        get() = mKeyboard
        /**
         * Attaches a keyboard to this view. The keyboard can be switched at any time and the
         * view will re-layout itself to accommodate the keyboard.
         * @see Keyboard
         *
         * @see .getKeyboard
         * @param keyboard the keyboard to display in this view
         */
        set(keyboard) {
            if (keyboard != mKeyboard) {
                clearWindowInfo()
            }
            var oldRepeatKeyCode =
                NOT_A_KEY
            if (mKeyboard != null) {
                showPreview(NOT_A_KEY)
                if ((mRepeatKeyIndex != NOT_A_KEY) && (mRepeatKeyIndex < mKeys!!.size)) {
                    oldRepeatKeyCode = mKeys!![mRepeatKeyIndex].codes!![0]
                }
            }
            removeMessages()
            mKeyboard = keyboard
            val keys =
                mKeyboard.getKeys()
            mKeys = keys.toTypedArray<Keyboard.Key>()
            requestLayout()
            mKeyboardChanged = true
            invalidateAllKeys()
            computeProximityThreshold(keyboard)
            mMiniKeyboardCache.clear()
            var abort = true
            if (oldRepeatKeyCode != NOT_A_KEY) {
                val keyIndex = getKeyIndices(mStartX, mStartY, null)
                if ((keyIndex != NOT_A_KEY)
                    && (keyIndex < mKeys!!.size)
                    && (oldRepeatKeyCode == mKeys!![keyIndex].codes!![0])
                ) {
                    abort = false
                    mRepeatKeyIndex = keyIndex
                }
            }
            if (abort) {
                mHandler.removeMessages(MSG_REPEAT)
            }
            mAbortKey = abort
        }

    /**
     * Sets the state of the shift key of the keyboard, if any.
     * @param shifted whether or not to enable the state of the shift key
     * @return true if the shift key state changed, false if there was no change
     * @see KeyboardView.isShifted
     */
    fun setShifted(shifted: Boolean): Boolean {
        if (mKeyboard != null) {
            if (mKeyboard!!.setShifted(shifted)) {
                invalidateAllKeys()
                return true
            }
        }
        return false
    }

    val isShifted: Boolean
        /**
         * Returns the state of the shift key of the keyboard, if any.
         * @return true if the shift is in a pressed state, false otherwise. If there is
         * no shift key on the keyboard or there is no keyboard attached, it returns false.
         * @see KeyboardView.setShifted
         */
        get() {
            if (mKeyboard != null) {
                return mKeyboard!!.isShifted
            }
            return false
        }

    val isParentPreviewEnabled: Boolean
        /**
         * Returns the root parent has the enabled state of the key feedback popup.
         * @return whether or not the key feedback popup is enabled
         * @see .setPreviewEnabled
         */
        get() = if ((mPopupParent != null) && (mPopupParent !== this)
            && (mPopupParent is KeyboardView)
        ) {
            (mPopupParent as KeyboardView).isParentPreviewEnabled()
        } else {
            isPreviewEnabled
        }

    fun setVerticalCorrection(verticalOffset: Int) {
    }

    /**
     * Set View on the PopupParent.
     * @param v  The View to set.
     */
    fun setPopupParent(v: View?) {
        mPopupParent = v
    }

    /**
     * Set parameters on the KeyboardOffset.
     * @param x  The value of KeyboardOffset.
     * @param y  The value of KeyboardOffset.
     */
    fun setPopupOffset(x: Int, y: Int) {
        mMiniKeyboardOffsetX = x
        mMiniKeyboardOffsetY = y
        if (mPreviewPopup.isShowing) {
            mPreviewPopup.dismiss()
        }
    }

    /**
     * Popup keyboard close button clicked.
     * @hide
     */
    override fun onClick(v: View) {
        dismissPopupKeyboard()
    }

    private fun adjustCase(label: CharSequence?): CharSequence? {
        var label = label
        if (mKeyboard!!.isShifted && label != null && label.length < 3 && Character.isLowerCase(
                label[0]
            )
        ) {
            label = label.toString().uppercase(Locale.getDefault())
        }
        return label
    }

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (mKeyboard == null) {
            setMeasuredDimension(paddingLeft + paddingRight, paddingTop + paddingBottom)
        } else {
            var width = mKeyboard.getMinWidth() + paddingLeft + paddingRight
            if (MeasureSpec.getSize(widthMeasureSpec) < width + 10) {
                width = MeasureSpec.getSize(widthMeasureSpec)
            }
            setMeasuredDimension(width, mKeyboard.getHeight() + paddingTop + paddingBottom)
        }
    }

    /**
     * Compute the average distance between adjacent keys (horizontally and vertically)
     * and square it to get the proximity threshold. We use a square here and in computing
     * the touch distance from a key's center to avoid taking a square root.
     * @param keyboard
     */
    private fun computeProximityThreshold(keyboard: Keyboard?) {
        if (keyboard == null) return
        val keys = mKeys ?: return
        val length = keys.size
        var dimensionSum = 0
        for (i in 0 until length) {
            val key = keys[i]
            (dimensionSum += min(
                key.width.toDouble(),
                key.height.toDouble()
            ) + key.gap).toInt()
        }
        if (dimensionSum < 0 || length == 0) return
        mProximityThreshold = (dimensionSum * 1.4f / length).toInt()
        mProximityThreshold *= mProximityThreshold
    }

    public override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mBuffer = null
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mDrawPending || mBuffer == null || mKeyboardChanged) {
            onBufferDraw()
        }
        canvas.drawBitmap(mBuffer!!, 0f, 0f, null)
    }

    private fun onBufferDraw() {
        val isBufferNull = (mBuffer == null)
        if (isBufferNull || mKeyboardChanged) {
            if (isBufferNull || mKeyboardChanged &&
                (mBuffer!!.width != width || mBuffer!!.height != height)
            ) {
                val width = max(1.0, width.toDouble()).toInt()
                val height = max(1.0, height.toDouble()).toInt()
                mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                mCanvas = Canvas(mBuffer!!)
            }
            invalidateAllKeys()
            mKeyboardChanged = false
        }
        val canvas = mCanvas
        canvas!!.clipRect(mDirtyRect, Region.Op.INTERSECT)

        if (mKeyboard == null) return

        val paint = mPaint
        val clipRegion = mClipRegion
        val padding = mPadding
        val kbdPaddingLeft = paddingLeft
        val kbdPaddingTop = paddingTop
        val keys = mKeys
        val invalidKey = mInvalidatedKey

        paint.color = mKeyTextColor
        var drawSingleKey = false
        if (invalidKey != null && canvas.getClipBounds(clipRegion)) {
            if (invalidKey.x + kbdPaddingLeft - 1 <= clipRegion.left && invalidKey.y + kbdPaddingTop - 1 <= clipRegion.top && invalidKey.x + invalidKey.width + kbdPaddingLeft + 1 >= clipRegion.right && invalidKey.y + invalidKey.height + kbdPaddingTop + 1 >= clipRegion.bottom) {
                drawSingleKey = true
            }
        }
        canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR)
        val keyCount = keys!!.size
        for (i in 0 until keyCount) {
            val key = keys[i]
            if (drawSingleKey && invalidKey !== key) {
                continue
            }

            paint.color = if (key.isSecondKey) mKeyTextColor2nd else mKeyTextColor
            val keyBackground = if (key.isSecondKey) mKeyBackground2nd else mKeyBackground
            val drawableState = key.currentDrawableState
            keyBackground!!.setState(drawableState!!)

            val label = if (key.label == null) null else adjustCase(key.label).toString()

            val bounds = keyBackground.bounds
            if (key.width != bounds.right ||
                key.height != bounds.bottom
            ) {
                keyBackground.setBounds(0, 0, key.width, key.height)
            }
            canvas.translate((key.x + kbdPaddingLeft).toFloat(), (key.y + kbdPaddingTop).toFloat())
            keyBackground.draw(canvas)

            if (label != null) {
                if (OpenWnn.Companion.isXLarge()) {
                    if (label.length > 1 && key.codes!!.size < 2) {
                        paint.textSize = mLabelTextSize.toFloat()
                        paint.setTypeface(Typeface.DEFAULT)
                    } else {
                        paint.textSize = mKeyTextSize.toFloat()
                        paint.setTypeface(Typeface.DEFAULT_BOLD)
                    }
                } else {
                    if (label.length > 1 && key.codes!!.size < 2) {
                        paint.textSize = mLabelTextSize.toFloat()
                        paint.setTypeface(Typeface.DEFAULT_BOLD)
                    } else {
                        paint.textSize = mKeyTextSize.toFloat()
                        paint.setTypeface(Typeface.DEFAULT_BOLD)
                    }
                }
                paint.setShadowLayer(mShadowRadius, 0f, 0f, mShadowColor)
                if (OpenWnn.Companion.isXLarge()) {
                    canvas.drawText(
                        label,
                        ((key.width - padding.left + 7 - padding.right) / 2
                                + padding.left).toFloat(),
                        (key.height - padding.top + 7 - padding.bottom) / 2 + (paint.textSize - paint.descent()) / 2 + padding.top,
                        paint
                    )
                } else {
                    canvas.drawText(
                        label,
                        ((key.width - padding.left - padding.right) / 2
                                + padding.left).toFloat(),
                        (key.height - padding.top - padding.bottom) / 2 + (paint.textSize - paint.descent()) / 2 + padding.top,
                        paint
                    )
                }
                paint.setShadowLayer(0f, 0f, 0f, 0)
            } else if (key.icon != null) {
                var drawableX: Int
                var drawableY: Int
                if (OpenWnn.Companion.isXLarge()) {
                    drawableX = ((key.width - padding.left + 12 - padding.right
                            - key.icon!!.intrinsicWidth)) / 2 + padding.left
                    drawableY = ((key.height - padding.top + 9 - padding.bottom
                            - key.icon!!.intrinsicHeight)) / 2 + padding.top
                } else {
                    drawableX = ((key.width - padding.left - padding.right
                            - key.icon!!.intrinsicWidth)) / 2 + padding.left
                    drawableY = ((key.height - padding.top - padding.bottom
                            - key.icon!!.intrinsicHeight)) / 2 + padding.top
                }
                canvas.translate(drawableX.toFloat(), drawableY.toFloat())
                key.icon!!.setBounds(
                    0, 0,
                    key.icon!!.intrinsicWidth, key.icon!!.intrinsicHeight
                )
                key.icon!!.draw(canvas)
                canvas.translate(-drawableX.toFloat(), -drawableY.toFloat())
            }
            canvas.translate(
                (-key.x - kbdPaddingLeft).toFloat(),
                (-key.y - kbdPaddingTop).toFloat()
            )
        }
        mInvalidatedKey = null
        if (mMiniKeyboardOnScreen) {
            paint.color = (mBackgroundDimAmount * 0xFF).toInt() shl 24
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }

        mDrawPending = false
        mDirtyRect.setEmpty()
    }

    private fun getKeyIndices(x: Int, y: Int, allKeys: IntArray?): Int {
        val keys = mKeys
        var primaryIndex = NOT_A_KEY
        var closestKey = NOT_A_KEY
        var closestKeyDist = mProximityThreshold + 1
        Arrays.fill(mDistances, Int.MAX_VALUE)
        val nearestKeyIndices = mKeyboard!!.getNearestKeys(x, y)
        val keyCount = nearestKeyIndices!!.size
        for (i in 0 until keyCount) {
            val key = keys!![nearestKeyIndices[i]]
            var dist = 0
            val isInside = key.isInside(x, y)
            if (isInside) {
                primaryIndex = nearestKeyIndices[i]
            }

            if (((isProximityCorrectionEnabled
                        && (key.squaredDistanceFrom(x, y).also { dist = it }) < mProximityThreshold)
                        || isInside)
                && key.codes!![0] > 32
            ) {
                val nCodes = key.codes!!.size
                if (dist < closestKeyDist) {
                    closestKeyDist = dist
                    closestKey = nearestKeyIndices[i]
                }

                if (allKeys == null) continue

                for (j in mDistances.indices) {
                    if (mDistances[j] > dist) {
                        System.arraycopy(
                            mDistances, j, mDistances, j + nCodes,
                            mDistances.size - j - nCodes
                        )
                        System.arraycopy(
                            allKeys, j, allKeys, j + nCodes,
                            allKeys.size - j - nCodes
                        )
                        for (c in 0 until nCodes) {
                            allKeys[j + c] = key.codes!![c]
                            mDistances[j + c] = dist
                        }
                        break
                    }
                }
            }
        }
        if (primaryIndex == NOT_A_KEY) {
            primaryIndex = closestKey
        }
        return primaryIndex
    }

    private fun detectAndSendKey(index: Int, x: Int, y: Int, eventTime: Long) {
        if (index != NOT_A_KEY && index < mKeys!!.size) {
            val key = mKeys!![index]
            if (key.text != null) {
                onKeyboardActionListener!!.onText(key.text)
                onKeyboardActionListener!!.onRelease(NOT_A_KEY)
            } else {
                var code = key.codes!![0]
                val codes = IntArray(MAX_NEARBY_KEYS)
                Arrays.fill(codes, NOT_A_KEY)
                getKeyIndices(x, y, codes)
                if (mInMultiTap) {
                    if (mTapCount != -1) {
                        onKeyboardActionListener!!.onKey(
                            Keyboard.Companion.KEYCODE_DELETE,
                            KEY_DELETE
                        )
                    } else {
                        mTapCount = 0
                    }
                    code = key.codes!![mTapCount]
                }
                onKeyboardActionListener!!.onKey(code, codes)
                onKeyboardActionListener!!.onRelease(code)
            }
            mLastSentIndex = index
            mLastTapTime = eventTime
        }
    }

    /**
     * Handle multi-tap keys by producing the key label for the current multi-tap state.
     */
    private fun getPreviewText(key: Keyboard.Key): CharSequence? {
        if (mInMultiTap) {
            mPreviewLabel.setLength(0)
            mPreviewLabel.append(key.codes!![if (mTapCount < 0) 0 else mTapCount].toChar())
            return adjustCase(mPreviewLabel)
        } else {
            return adjustCase(key.label)
        }
    }

    private fun showPreview(keyIndex: Int) {
        val oldKeyIndex = mCurrentKeyIndex
        val previewPopup = mPreviewPopup

        mCurrentKeyIndex = keyIndex
        val keys = mKeys
        if (oldKeyIndex != mCurrentKeyIndex) {
            if (oldKeyIndex != NOT_A_KEY && keys!!.size > oldKeyIndex) {
                keys[oldKeyIndex].onReleased(mCurrentKeyIndex == NOT_A_KEY)
                invalidateKey(oldKeyIndex)
            }
            if (mCurrentKeyIndex != NOT_A_KEY && keys!!.size > mCurrentKeyIndex) {
                keys[mCurrentKeyIndex].onPressed()
                invalidateKey(mCurrentKeyIndex)
            }
        }
        if (oldKeyIndex != mCurrentKeyIndex && isPreviewEnabled && isParentPreviewEnabled) {
            mHandler.removeMessages(MSG_SHOW_PREVIEW)
            if (previewPopup.isShowing) {
                if (keyIndex == NOT_A_KEY) {
                    mHandler.sendMessageDelayed(
                        mHandler
                            .obtainMessage(MSG_REMOVE_PREVIEW),
                        DELAY_AFTER_PREVIEW.toLong()
                    )
                }
            }
            if (keyIndex != NOT_A_KEY) {
                if (previewPopup.isShowing && mPreviewText!!.visibility == VISIBLE) {
                    showKey(keyIndex)
                } else {
                    mHandler.sendMessageDelayed(
                        mHandler.obtainMessage(MSG_SHOW_PREVIEW, keyIndex, 0),
                        DELAY_BEFORE_PREVIEW.toLong()
                    )
                }
            }
        }
    }

    private fun showKey(keyIndex: Int) {
        val previewPopup = mPreviewPopup
        val keys = mKeys
        if (keyIndex < 0 || keyIndex >= mKeys!!.size) return
        val key = keys!![keyIndex]

        mPreviewText!!.setBackgroundDrawable(context.resources.getDrawable(R.drawable.keyboard_key_feedback))

        if (key.icon != null) {
            mPreviewText.setCompoundDrawables(
                null, null, null,
                if (key.iconPreview != null) key.iconPreview else key.icon
            )
            mPreviewText.text = null
            mPreviewText.setPadding(5, 0, 5, 20)
        } else {
            mPreviewText.setCompoundDrawables(null, null, null, null)
            mPreviewText.text = getPreviewText(key)
            if (key.label!!.length > 1 && key.codes!!.size < 2) {
                mPreviewText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mKeyTextSize.toFloat())
                mPreviewText.setTypeface(Typeface.DEFAULT_BOLD)
            } else {
                mPreviewText.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    mPreviewTextSizeLarge.toFloat()
                )
                mPreviewText.setTypeface(Typeface.DEFAULT)
            }
            mPreviewText.setPadding(0, 0, 0, 10)
        }
        mPreviewText.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        val popupWidth = max(
            mPreviewText.measuredWidth.toDouble(), (key.width
                    + mPreviewText.paddingLeft + mPreviewText.paddingRight).toDouble()
        ).toInt()
        val popupHeight = mPreviewHeight
        val lp = mPreviewText.layoutParams
        if (lp != null) {
            lp.width = popupWidth
            lp.height = popupHeight
        }
        if (!mPreviewCentered) {
            mPopupPreviewX = (key.x - (abs((popupWidth - key.width).toDouble()) / 2)).toInt()
            mPopupPreviewY = key.y - popupHeight + mPreviewOffset
        } else {
            mPopupPreviewX = 160 - mPreviewText.measuredWidth / 2
            mPopupPreviewY = -mPreviewText.measuredHeight
        }
        mPopupPreviewY = mPopupPreviewY + 20
        mHandler.removeMessages(MSG_REMOVE_PREVIEW)
        if (mOffsetInWindow == null) {
            mOffsetInWindow = IntArray(2)
            getLocationInWindow(mOffsetInWindow)
            mOffsetInWindow!![0] += mMiniKeyboardOffsetX
            mOffsetInWindow!![1] += mMiniKeyboardOffsetY
            val mWindowLocation = IntArray(2)
            getLocationOnScreen(mWindowLocation)
            mWindowY = mWindowLocation[1]
        }
        mPreviewText.background.setState(
            if (key.popupResId != 0) LONG_PRESSABLE_STATE_SET else EMPTY_STATE_SET
        )
        mPopupPreviewX += mOffsetInWindow!![0]
        mPopupPreviewY += mOffsetInWindow!![1]

        if (mPopupPreviewY + mWindowY < 0) {
            if (key.x + key.width <= width / 2) {
                mPopupPreviewX += (key.width * 2.5).toInt()
            } else {
                mPopupPreviewX -= (key.width * 2.5).toInt()
            }
            mPopupPreviewY += popupHeight
        }

        if (previewPopup.isShowing) {
            previewPopup.update(
                mPopupPreviewX, mPopupPreviewY,
                popupWidth, popupHeight
            )
        } else {
            previewPopup.width = popupWidth
            previewPopup.height = popupHeight
            previewPopup.showAtLocation(
                mPopupParent, Gravity.NO_GRAVITY,
                mPopupPreviewX, mPopupPreviewY
            )
        }
        mPreviewText.visibility = VISIBLE
    }

    /**
     * Requests a redraw of the entire keyboard. Calling [.invalidate] is not sufficient
     * because the keyboard renders the keys to an off-screen buffer and an invalidate() only
     * draws the cached buffer.
     * @see .invalidateKey
     */
    fun invalidateAllKeys() {
        mDirtyRect.union(0, 0, width, height)
        mDrawPending = true
        invalidate()
    }

    /**
     * Invalidates a key so that it will be redrawn on the next repaint. Use this method if only
     * one key is changing it's content. Any changes that affect the position or size of the key
     * may not be honored.
     * @param keyIndex the index of the key in the attached [Keyboard].
     * @see .invalidateAllKeys
     */
    fun invalidateKey(keyIndex: Int) {
        if (mKeys == null) return
        if (keyIndex < 0 || keyIndex >= mKeys!!.size) {
            return
        }
        val key = mKeys!![keyIndex]
        mInvalidatedKey = key
        mDirtyRect.union(
            key.x + paddingLeft, key.y + paddingTop,
            key.x + key.width + paddingLeft, key.y + key.height + paddingTop
        )
        onBufferDraw()
        invalidate(
            key.x + paddingLeft, key.y + paddingTop,
            key.x + key.width + paddingLeft, key.y + key.height + paddingTop
        )
    }

    private fun openPopupIfRequired(me: MotionEvent): Boolean {
        if (mPopupLayout == 0) {
            return false
        }
        if (mCurrentKey < 0 || mCurrentKey >= mKeys!!.size) {
            return false
        }

        val popupKey = mKeys!![mCurrentKey]
        val result = onLongPress(popupKey)
        if (result) {
            mAbortKey = true
            showPreview(NOT_A_KEY)
        }
        return result
    }

    /**
     * Called when a key is long pressed. By default this will open any popup keyboard associated
     * with this key through the attributes popupLayout and popupCharacters.
     * @param popupKey the key that was long pressed
     * @return true if the long press is handled, false otherwise. Subclasses should call the
     * method on the base class if the subclass doesn't wish to handle the call.
     */
    protected fun onLongPress(popupKey: Keyboard.Key): Boolean {
        if (onKeyboardActionListener!!.onLongPress(popupKey)) {
            return true
        }
        val popupKeyboardId = popupKey.popupResId
        if (popupKeyboardId != 0) {
            mMiniKeyboardContainer = mMiniKeyboardCache[popupKey]
            if (mMiniKeyboardContainer == null) {
                val inflater = context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE
                ) as LayoutInflater
                mMiniKeyboardContainer = inflater.inflate(mPopupLayout, null)
                mMiniKeyboard =
                    mMiniKeyboardContainer.findViewById<View>(R.id.keyboardView) as KeyboardView
                val closeButton = mMiniKeyboardContainer.findViewById<View>(R.id.closeButton)
                closeButton?.setOnClickListener(this)
                mMiniKeyboard!!.onKeyboardActionListener =
                    object : OnKeyboardActionListener {
                        override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
                            onKeyboardActionListener!!.onKey(primaryCode, keyCodes)
                            dismissPopupKeyboard()
                        }

                        override fun onText(text: CharSequence?) {
                            onKeyboardActionListener!!.onText(text)
                            dismissPopupKeyboard()
                        }

                        override fun swipeLeft() {}
                        override fun swipeRight() {}
                        override fun swipeUp() {}
                        override fun swipeDown() {}
                        override fun onPress(primaryCode: Int) {
                            onKeyboardActionListener!!.onPress(primaryCode)
                        }

                        override fun onRelease(primaryCode: Int) {
                            onKeyboardActionListener!!.onRelease(primaryCode)
                        }

                        override fun onLongPress(key: Keyboard.Key): Boolean {
                            return false
                        }
                    }
                val keyboard = if (popupKey.popupCharacters != null) {
                    Keyboard(
                        context, popupKeyboardId,
                        popupKey.popupCharacters!!, -1, paddingLeft + paddingRight
                    )
                } else {
                    Keyboard(context, popupKeyboardId)
                }
                mMiniKeyboard!!.keyboard = keyboard
                mMiniKeyboard!!.setPopupParent(this)
                mMiniKeyboardContainer.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
                )

                mMiniKeyboardCache[popupKey] = mMiniKeyboardContainer
            } else {
                mMiniKeyboard =
                    mMiniKeyboardContainer!!.findViewById<View>(R.id.keyboardView) as KeyboardView
            }
            if (mWindowOffset == null) {
                mWindowOffset = IntArray(2)
                getLocationInWindow(mWindowOffset)
            }
            mPopupX = popupKey.x + paddingLeft
            mPopupY = popupKey.y + paddingTop
            mPopupX = mPopupX + popupKey.width - mMiniKeyboardContainer!!.measuredWidth
            mPopupY = mPopupY - mMiniKeyboardContainer!!.measuredHeight
            val x = mPopupX + mMiniKeyboardContainer!!.paddingRight + mWindowOffset!![0]
            val y = mPopupY + mMiniKeyboardContainer!!.paddingBottom + mWindowOffset!![1]
            mMiniKeyboard!!.setPopupOffset(if (x < 0) 0 else x, y)
            mMiniKeyboard!!.setShifted(isShifted)
            mPopupKeyboard.contentView = mMiniKeyboardContainer
            mPopupKeyboard.width = mMiniKeyboardContainer!!.measuredWidth
            mPopupKeyboard.height = mMiniKeyboardContainer!!.measuredHeight
            mPopupKeyboard.showAtLocation(this, Gravity.NO_GRAVITY, x, y)
            mMiniKeyboardOnScreen = true
            invalidateAllKeys()
            return true
        }
        return false
    }

    private var mOldEventTime: Long = 0
    private val mUsedVelocity = false

    /** Constructor  */
    /** Constructor  */
    init {
        var a =
            context.obtainStyledAttributes(
                attrs, R.styleable.AospKeyboardView, defStyle, R.style.WnnKeyboardView
            )

        val inflate =
            context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        var previewLayout = 0
        val keyTextSize = 0

        val n = a.indexCount

        for (i in 0 until n) {
            val attr = a.getIndex(i)

            when (attr) {
                R.styleable.AospKeyboardView_keyBackground -> mKeyBackground = a.getDrawable(attr)
                R.styleable.AospKeyboardView_verticalCorrection -> mVerticalCorrection =
                    a.getDimensionPixelOffset(attr, 0)

                R.styleable.AospKeyboardView_keyPreviewLayout -> previewLayout =
                    a.getResourceId(attr, 0)

                R.styleable.AospKeyboardView_keyPreviewOffset -> mPreviewOffset =
                    a.getDimensionPixelOffset(attr, 0)

                R.styleable.AospKeyboardView_keyPreviewHeight -> mPreviewHeight =
                    a.getDimensionPixelSize(attr, 80)

                R.styleable.AospKeyboardView_keyTextSize -> mKeyTextSize =
                    a.getDimensionPixelSize(attr, 18)

                R.styleable.AospKeyboardView_keyTextColor -> mKeyTextColor =
                    a.getColor(attr, -0x1000000)

                R.styleable.AospKeyboardView_labelTextSize -> mLabelTextSize =
                    a.getDimensionPixelSize(attr, 14)

                R.styleable.AospKeyboardView_popupLayout -> mPopupLayout = a.getResourceId(attr, 0)
                R.styleable.AospKeyboardView_shadowColor -> mShadowColor = a.getColor(attr, 0)
                R.styleable.AospKeyboardView_shadowRadius -> mShadowRadius = a.getFloat(attr, 0f)
            }
        }

        a.recycle()
        a = context.obtainStyledAttributes(attrs, R.styleable.WnnKeyboardView, 0, 0)
        mKeyBackground2nd = a.getDrawable(R.styleable.WnnKeyboardView_keyBackground2nd)
        mKeyTextColor2nd = a.getColor(R.styleable.WnnKeyboardView_keyTextColor2nd, -0x1000000)

        a.recycle()
        a = context.obtainStyledAttributes(
            R.styleable.Theme
        )
        mBackgroundDimAmount = a.getFloat(R.styleable.Theme_backgroundDimAmount, 0.5f)

        mPreviewPopup = PopupWindow(context)
        if (previewLayout != 0) {
            mPreviewText = inflate.inflate(previewLayout, null) as TextView
            mPreviewTextSizeLarge = mPreviewText!!.textSize.toInt()
            mPreviewPopup.contentView = mPreviewText
            mPreviewPopup.setBackgroundDrawable(null)
        } else {
            isPreviewEnabled = false
        }

        mPreviewPopup.isTouchable = false

        mPopupKeyboard = PopupWindow(context)
        mPopupKeyboard.setBackgroundDrawable(null)

        mPopupParent = this

        mPaint = Paint()
        mPaint.isAntiAlias = true
        mPaint.textSize = keyTextSize.toFloat()
        mPaint.textAlign = Align.CENTER
        mPaint.alpha = 255

        mPadding = Rect(0, 0, 0, 0)
        mMiniKeyboardCache = HashMap()
        mKeyBackground!!.getPadding(mPadding)

        mSwipeThreshold = (500 * resources.displayMetrics.density).toInt()

        mDisambiguateSwipe = true

        resetMultiTap()
        initGestureDetector()
    }

    override fun onTouchEvent(me: MotionEvent): Boolean {
        val pointerCount = me.pointerCount
        val action = me.action
        var result = false
        val now = me.eventTime
        val isPointerCountOne = (pointerCount == 1)

        if (pointerCount != mOldPointerCount) {
            if (isPointerCountOne) {
                val down = MotionEvent.obtain(
                    now, now, MotionEvent.ACTION_DOWN,
                    me.x, me.y, me.metaState
                )
                result = onModifiedTouchEvent(down, false)
                down.recycle()
                if (action == MotionEvent.ACTION_UP) {
                    result = onModifiedTouchEvent(me, true)
                }
            } else {
                val up = MotionEvent.obtain(
                    now, now, MotionEvent.ACTION_UP,
                    mOldPointerX, mOldPointerY, me.metaState
                )
                result = onModifiedTouchEvent(up, true)
                up.recycle()
            }
        } else {
            if (isPointerCountOne) {
                result = onModifiedTouchEvent(me, false)
                mOldPointerX = me.x
                mOldPointerY = me.y
            } else {
                result = true
            }
        }
        mOldPointerCount = pointerCount

        return result
    }

    private fun onModifiedTouchEvent(me: MotionEvent, possiblePoly: Boolean): Boolean {
        var touchX = me.x.toInt() - paddingLeft
        var touchY = me.y.toInt() + mVerticalCorrection - paddingTop
        val action = me.action
        val eventTime = me.eventTime
        mOldEventTime = eventTime
        val keyIndex = getKeyIndices(touchX, touchY, null)
        mPossiblePoly = possiblePoly

        if (action == MotionEvent.ACTION_DOWN) mSwipeTracker.clear()
        mSwipeTracker.addMovement(me)

        if (mAbortKey
            && action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_CANCEL
        ) {
            return true
        }

        if (mGestureDetector!!.onTouchEvent(me)) {
            showPreview(NOT_A_KEY)
            mHandler.removeMessages(MSG_REPEAT)
            mHandler.removeMessages(MSG_LONGPRESS)
            return true
        }

        if (mMiniKeyboardOnScreen && action != MotionEvent.ACTION_CANCEL) {
            return true
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mAbortKey = false
                mStartX = touchX
                mStartY = touchY
                mLastCodeX = touchX
                mLastCodeY = touchY
                mLastKeyTime = 0
                mCurrentKeyTime = 0
                mLastKey = NOT_A_KEY
                mCurrentKey = keyIndex
                mDownKey = keyIndex
                mDownTime = me.eventTime
                mLastMoveTime = mDownTime
                checkMultiTap(eventTime, keyIndex)
                onKeyboardActionListener!!.onPress(if (keyIndex != NOT_A_KEY) mKeys!![keyIndex].codes!![0] else 0)
                if (mCurrentKey >= 0 && mKeys!![mCurrentKey].repeatable) {
                    mRepeatKeyIndex = mCurrentKey
                    val msg = mHandler.obtainMessage(MSG_REPEAT)
                    mHandler.sendMessageDelayed(msg, REPEAT_START_DELAY.toLong())
                    repeatKey()
                    if (mAbortKey) {
                        mRepeatKeyIndex = NOT_A_KEY
                        break
                    }
                }
                if (mCurrentKey != NOT_A_KEY) {
                    val msg = mHandler.obtainMessage(MSG_LONGPRESS, me)
                    mHandler.sendMessageDelayed(msg, LONGPRESS_TIMEOUT.toLong())
                }
                showPreview(keyIndex)
            }

            MotionEvent.ACTION_MOVE -> {
                var continueLongPress = false
                if (keyIndex != NOT_A_KEY) {
                    if (mCurrentKey == NOT_A_KEY) {
                        mCurrentKey = keyIndex
                        mCurrentKeyTime = eventTime - mDownTime
                    } else {
                        if (keyIndex == mCurrentKey) {
                            mCurrentKeyTime += eventTime - mLastMoveTime
                            continueLongPress = true
                        } else if (mRepeatKeyIndex == NOT_A_KEY) {
                            resetMultiTap()
                            mLastKey = mCurrentKey
                            mLastCodeX = mLastX
                            mLastCodeY = mLastY
                            mLastKeyTime =
                                mCurrentKeyTime + eventTime - mLastMoveTime
                            mCurrentKey = keyIndex
                            mCurrentKeyTime = 0
                        }
                    }
                }
                if (!continueLongPress) {
                    mHandler.removeMessages(MSG_LONGPRESS)
                    if (keyIndex != NOT_A_KEY) {
                        val msg = mHandler.obtainMessage(MSG_LONGPRESS, me)
                        mHandler.sendMessageDelayed(msg, LONGPRESS_TIMEOUT.toLong())
                    }
                }
                showPreview(mCurrentKey)
                mLastMoveTime = eventTime
            }

            MotionEvent.ACTION_UP -> {
                removeMessages()
                if (keyIndex == mCurrentKey) {
                    mCurrentKeyTime += eventTime - mLastMoveTime
                } else {
                    resetMultiTap()
                    mLastKey = mCurrentKey
                    mLastKeyTime = mCurrentKeyTime + eventTime - mLastMoveTime
                    mCurrentKey = keyIndex
                    mCurrentKeyTime = 0
                }
                if (mCurrentKeyTime < mLastKeyTime && mCurrentKeyTime < DEBOUNCE_TIME && mLastKey != NOT_A_KEY) {
                    mCurrentKey = mLastKey
                    touchX = mLastCodeX
                    touchY = mLastCodeY
                }
                showPreview(NOT_A_KEY)
                Arrays.fill(mKeyIndices, NOT_A_KEY)
                if (mRepeatKeyIndex == NOT_A_KEY && !mMiniKeyboardOnScreen && !mAbortKey) {
                    detectAndSendKey(mCurrentKey, touchX, touchY, eventTime)
                }
                invalidateKey(keyIndex)
                mRepeatKeyIndex = NOT_A_KEY
            }

            MotionEvent.ACTION_CANCEL -> {
                removeMessages()
                dismissPopupKeyboard()
                mAbortKey = true
                showPreview(NOT_A_KEY)
                invalidateKey(mCurrentKey)
            }
        }
        mLastX = touchX
        mLastY = touchY
        return true
    }

    private fun repeatKey(): Boolean {
        val key = mKeys!![mRepeatKeyIndex]
        detectAndSendKey(mCurrentKey, key.x, key.y, mLastTapTime)
        return true
    }

    protected fun swipeRight() {
        onKeyboardActionListener!!.swipeRight()
    }

    protected fun swipeLeft() {
        onKeyboardActionListener!!.swipeLeft()
    }

    protected fun swipeUp() {
        onKeyboardActionListener!!.swipeUp()
    }

    protected fun swipeDown() {
        onKeyboardActionListener!!.swipeDown()
    }

    fun closing() {
        if (mPreviewPopup.isShowing) {
            mPreviewPopup.dismiss()
        }
        removeMessages()

        dismissPopupKeyboard()
        mBuffer = null
        mCanvas = null
        mMiniKeyboardCache.clear()
    }

    private fun removeMessages() {
        mHandler.removeMessages(MSG_REPEAT)
        mHandler.removeMessages(MSG_LONGPRESS)
        mHandler.removeMessages(MSG_SHOW_PREVIEW)
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        closing()
    }

    private fun dismissPopupKeyboard() {
        if (mPopupKeyboard.isShowing) {
            mPopupKeyboard.dismiss()
            mMiniKeyboardOnScreen = false
            invalidateAllKeys()
        }
    }

    fun handleBack(): Boolean {
        if (mPopupKeyboard.isShowing) {
            dismissPopupKeyboard()
            return true
        }
        return false
    }

    private fun resetMultiTap() {
        mLastSentIndex = NOT_A_KEY
        mTapCount = 0
        mLastTapTime = -1
        mInMultiTap = false
    }

    private fun checkMultiTap(eventTime: Long, keyIndex: Int) {
        if (keyIndex == NOT_A_KEY) return
        val key = mKeys!![keyIndex]
        if (key.codes!!.size > 1) {
            mInMultiTap = true
            if (eventTime < mLastTapTime + MULTITAP_INTERVAL
                && keyIndex == mLastSentIndex
            ) {
                mTapCount = (mTapCount + 1) % key.codes!!.size
                return
            } else {
                mTapCount = -1
                return
            }
        }
        if (eventTime > mLastTapTime + MULTITAP_INTERVAL || keyIndex != mLastSentIndex) {
            resetMultiTap()
        }
    }

    private class SwipeTracker {
        val mPastX: FloatArray = FloatArray(NUM_PAST)
        val mPastY: FloatArray = FloatArray(NUM_PAST)
        val mPastTime: LongArray = LongArray(NUM_PAST)

        var yVelocity: Float = 0f
        var xVelocity: Float = 0f

        fun clear() {
            mPastTime[0] = 0
        }

        fun addMovement(ev: MotionEvent) {
            val time = ev.eventTime
            val N = ev.historySize
            for (i in 0 until N) {
                addPoint(
                    ev.getHistoricalX(i), ev.getHistoricalY(i),
                    ev.getHistoricalEventTime(i)
                )
            }
            addPoint(ev.x, ev.y, time)
        }

        fun addPoint(x: Float, y: Float, time: Long) {
            var drop = -1
            val pastTime = mPastTime
            var i = 0
            while (i < NUM_PAST) {
                if (pastTime[i] == 0L) {
                    break
                } else if (pastTime[i] < time - LONGEST_PAST_TIME) {
                    drop = i
                }
                i++
            }
            if (i == NUM_PAST && drop < 0) {
                drop = 0
            }
            if (drop == i) drop--
            val pastX = mPastX
            val pastY = mPastY
            if (drop >= 0) {
                val start = drop + 1
                val count = NUM_PAST - drop - 1
                System.arraycopy(pastX, start, pastX, 0, count)
                System.arraycopy(pastY, start, pastY, 0, count)
                System.arraycopy(pastTime, start, pastTime, 0, count)
                i -= (drop + 1)
            }
            pastX[i] = x
            pastY[i] = y
            pastTime[i] = time
            i++
            if (i < NUM_PAST) {
                pastTime[i] = 0
            }
        }

        @JvmOverloads
        fun computeCurrentVelocity(units: Int, maxVelocity: Float = Float.MAX_VALUE) {
            val pastX = mPastX
            val pastY = mPastY
            val pastTime = mPastTime

            val oldestX = pastX[0]
            val oldestY = pastY[0]
            val oldestTime = pastTime[0]
            var accumX = 0f
            var accumY = 0f
            var N = 0
            while (N < NUM_PAST) {
                if (pastTime[N] == 0L) {
                    break
                }
                N++
            }

            for (i in 1 until N) {
                val dur = (pastTime[i] - oldestTime).toInt()
                if (dur == 0) continue
                var dist = pastX[i] - oldestX
                var vel = (dist / dur) * units
                accumX = if (accumX == 0f) vel
                else (accumX + vel) * .5f

                dist = pastY[i] - oldestY
                vel = (dist / dur) * units
                accumY = if (accumY == 0f) vel
                else (accumY + vel) * .5f
            }
            xVelocity = if (accumX < 0.0f) max(
                accumX.toDouble(),
                -maxVelocity.toDouble()
            ).toFloat() else min(accumX.toDouble(), maxVelocity.toDouble()).toFloat()
            yVelocity = if (accumY < 0.0f) max(
                accumY.toDouble(),
                -maxVelocity.toDouble()
            ).toFloat() else min(accumY.toDouble(), maxVelocity.toDouble()).toFloat()
        }

        companion object {
            const val NUM_PAST: Int = 4
            const val LONGEST_PAST_TIME: Int = 200
        }
    }

    /**
     * Clear window info.
     */
    fun clearWindowInfo() {
        mOffsetInWindow = null
    }

    companion object {
        private const val NOT_A_KEY = -1
        private val KEY_DELETE = intArrayOf(Keyboard.Companion.KEYCODE_DELETE)
        private val LONG_PRESSABLE_STATE_SET = intArrayOf(
            android.R.attr.state_long_pressable
        )

        private const val MSG_SHOW_PREVIEW = 1
        private const val MSG_REMOVE_PREVIEW = 2
        private const val MSG_REPEAT = 3
        private const val MSG_LONGPRESS = 4

        private const val DELAY_BEFORE_PREVIEW = 0
        private const val DELAY_AFTER_PREVIEW = 70
        private const val DEBOUNCE_TIME = 70

        private const val REPEAT_INTERVAL = 50
        private const val REPEAT_START_DELAY = 400
        private val LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout()

        private const val MAX_NEARBY_KEYS = 12
        private const val MULTITAP_INTERVAL = 800
    }
}
