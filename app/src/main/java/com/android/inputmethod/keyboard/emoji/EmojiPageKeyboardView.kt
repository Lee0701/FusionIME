/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.inputmethod.keyboard.emoji

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import com.android.inputmethod.accessibility.AccessibilityUtils
import com.android.inputmethod.accessibility.KeyboardAccessibilityDelegate
import com.android.inputmethod.keyboard.Key
import com.android.inputmethod.keyboard.KeyDetector
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.keyboard.KeyboardView
import com.android.inputmethod.latin.R

/**
 * This is an extended [KeyboardView] class that hosts an emoji page keyboard.
 * Multi-touch unsupported. No gesture support.
 */
// TODO: Implement key popup preview.
internal class EmojiPageKeyboardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet?,
    defStyle: Int = R.attr.keyboardViewStyle
) :
    KeyboardView(context, attrs, defStyle), GestureDetector.OnGestureListener {
    interface OnKeyEventListener {
        fun onPressKey(key: Key)
        fun onReleaseKey(key: Key)
    }

    private var mListener: OnKeyEventListener = EMPTY_LISTENER
    private val mKeyDetector: KeyDetector = KeyDetector()
    private val mGestureDetector: GestureDetector
    private var mAccessibilityDelegate: KeyboardAccessibilityDelegate<EmojiPageKeyboardView>? = null

    override var keyboard: Keyboard?
        get() = super.keyboard
        set(value) {
            super.keyboard = value
            mKeyDetector.setKeyboard(value!!, 0f,  /* correctionX */0f /* correctionY */)
            if (AccessibilityUtils.instance.isAccessibilityEnabled()) {
                if (mAccessibilityDelegate == null) {
                    mAccessibilityDelegate = KeyboardAccessibilityDelegate(this, mKeyDetector)
                }
                mAccessibilityDelegate!!.setKeyboard(keyboard)
            } else {
                mAccessibilityDelegate = null
            }
        }

    fun setOnKeyEventListener(listener: OnKeyEventListener) {
        mListener = listener
    }

    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent): Boolean {
        // Don't populate accessibility event with all Emoji keys.
        return true
    }

    /**
     * {@inheritDoc}
     */
    override fun onHoverEvent(event: MotionEvent): Boolean {
        val accessibilityDelegate: KeyboardAccessibilityDelegate<EmojiPageKeyboardView>? =
            mAccessibilityDelegate
        if (accessibilityDelegate != null
            && AccessibilityUtils.instance.isTouchExplorationEnabled()
        ) {
            return accessibilityDelegate.onHoverEvent(event)
        }
        return super.onHoverEvent(event)
    }

    /**
     * {@inheritDoc}
     */
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (mGestureDetector.onTouchEvent(e)) {
            return true
        }
        val key: Key? = getKey(e)
        if (key != null && key !== mCurrentKey) {
            releaseCurrentKey(false /* withKeyRegistering */)
        }
        return true
    }

    // {@link GestureEnabler#OnGestureListener} methods.
    private var mCurrentKey: Key? = null
    private var mPendingKeyDown: Runnable? = null
    private val mHandler: Handler

    init {
        mGestureDetector = GestureDetector(context, this)
        mGestureDetector.setIsLongpressEnabled(false /* isLongpressEnabled */)
        mHandler = Handler()
    }

    private fun getKey(e: MotionEvent): Key? {
        val index: Int = e.getActionIndex()
        val x: Int = e.getX(index).toInt()
        val y: Int = e.getY(index).toInt()
        return mKeyDetector.detectHitKey(x, y)
    }

    fun callListenerOnReleaseKey(releasedKey: Key, withKeyRegistering: Boolean) {
        releasedKey.onReleased()
        invalidateKey(releasedKey)
        if (withKeyRegistering) {
            mListener.onReleaseKey(releasedKey)
        }
    }

    fun callListenerOnPressKey(pressedKey: Key) {
        mPendingKeyDown = null
        pressedKey.onPressed()
        invalidateKey(pressedKey)
        mListener.onPressKey(pressedKey)
    }

    fun releaseCurrentKey(withKeyRegistering: Boolean) {
        mHandler.removeCallbacks(mPendingKeyDown!!)
        mPendingKeyDown = null
        val currentKey: Key? = mCurrentKey
        if (currentKey == null) {
            return
        }
        callListenerOnReleaseKey(currentKey, withKeyRegistering)
        mCurrentKey = null
    }

    override fun onDown(e: MotionEvent): Boolean {
        val key: Key? = getKey(e)
        releaseCurrentKey(false /* withKeyRegistering */)
        mCurrentKey = key
        if (key == null) {
            return false
        }
        // Do not trigger key-down effect right now in case this is actually a fling action.
        mPendingKeyDown = Runnable { callListenerOnPressKey(key) }
        mHandler.postDelayed(mPendingKeyDown!!, KEY_PRESS_DELAY_TIME)
        return false
    }

    override fun onShowPress(e: MotionEvent) {
        // User feedback is done at {@link #onDown(MotionEvent)}.
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        val key: Key? = getKey(e)
        val pendingKeyDown: Runnable? = mPendingKeyDown
        val currentKey: Key? = mCurrentKey
        releaseCurrentKey(false /* withKeyRegistering */)
        if (key == null) {
            return false
        }
        if (key === currentKey && pendingKeyDown != null) {
            pendingKeyDown.run()
            // Trigger key-release event a little later so that a user can see visual feedback.
            mHandler.postDelayed(object : Runnable {
                override fun run() {
                    callListenerOnReleaseKey(key, true /* withRegistering */)
                }
            }, KEY_RELEASE_DELAY_TIME)
        } else {
            callListenerOnReleaseKey(key, true /* withRegistering */)
        }
        return true
    }

    override fun onScroll(
        e1: MotionEvent?, e2: MotionEvent, distanceX: Float,
        distanceY: Float
    ): Boolean {
        releaseCurrentKey(false /* withKeyRegistering */)
        return false
    }

    override fun onFling(
        e1: MotionEvent?, e2: MotionEvent, velocityX: Float,
        velocityY: Float
    ): Boolean {
        releaseCurrentKey(false /* withKeyRegistering */)
        return false
    }

    override fun onLongPress(e: MotionEvent) {
        // Long press detection of {@link #mGestureDetector} is disabled and not used.
    }

    companion object {
        private const val KEY_PRESS_DELAY_TIME: Long = 250 // msec
        private const val KEY_RELEASE_DELAY_TIME: Long = 30 // msec

        private val EMPTY_LISTENER: OnKeyEventListener = object : OnKeyEventListener {
            override fun onPressKey(key: Key) {}
            override fun onReleaseKey(key: Key) {}
        }
    }
}
