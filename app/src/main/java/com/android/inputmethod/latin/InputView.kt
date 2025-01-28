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
package com.android.inputmethod.latin

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.android.inputmethod.accessibility.AccessibilityUtils
import com.android.inputmethod.keyboard.MainKeyboardView
import com.android.inputmethod.latin.suggestions.MoreSuggestionsView
import com.android.inputmethod.latin.suggestions.SuggestionStripView
import kotlin.math.min

class InputView(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs, 0) {
    private val mInputViewRect: Rect = Rect()
    private var mMainKeyboardView: MainKeyboardView? = null
    private var mKeyboardTopPaddingForwarder: KeyboardTopPaddingForwarder? = null
    private var mMoreSuggestionsViewCanceler: MoreSuggestionsViewCanceler? = null
    private var mActiveForwarder: MotionEventForwarder<*, *>? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        val suggestionStripView: SuggestionStripView =
            findViewById<View>(R.id.suggestion_strip_view) as SuggestionStripView
        mMainKeyboardView = findViewById<View>(R.id.keyboard_view) as MainKeyboardView?
        mKeyboardTopPaddingForwarder = KeyboardTopPaddingForwarder(
            mMainKeyboardView, suggestionStripView
        )
        mMoreSuggestionsViewCanceler = MoreSuggestionsViewCanceler(
            mMainKeyboardView, suggestionStripView
        )
    }

    fun setKeyboardTopPadding(keyboardTopPadding: Int) {
        mKeyboardTopPaddingForwarder?.setKeyboardTopPadding(keyboardTopPadding)
    }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        if (AccessibilityUtils.instance.isTouchExplorationEnabled()
            && mMainKeyboardView?.isShowingMoreKeysPanel() == true
        ) {
            // With accessibility mode on, discard hover events while a more keys keyboard is shown.
            // The {@link MoreKeysKeyboard} receives hover events directly from the platform.
            return true
        }
        return super.dispatchHoverEvent(event)
    }

    override fun onInterceptTouchEvent(me: MotionEvent): Boolean {
        val rect: Rect = mInputViewRect
        getGlobalVisibleRect(rect)
        val index: Int = me.getActionIndex()
        val x: Int = me.getX(index).toInt() + rect.left
        val y: Int = me.getY(index).toInt() + rect.top

        // The touch events that hit the top padding of keyboard should be forwarded to
        // {@link SuggestionStripView}.
        if (mKeyboardTopPaddingForwarder?.onInterceptTouchEvent(x, y, me) == true) {
            mActiveForwarder = mKeyboardTopPaddingForwarder
            return true
        }

        // To cancel {@link MoreSuggestionsView}, we should intercept a touch event to
        // {@link MainKeyboardView} and dismiss the {@link MoreSuggestionsView}.
        if (mMoreSuggestionsViewCanceler?.onInterceptTouchEvent(x, y, me) == true) {
            mActiveForwarder = mMoreSuggestionsViewCanceler
            return true
        }

        mActiveForwarder = null
        return false
    }

    override fun onTouchEvent(me: MotionEvent): Boolean {
        if (mActiveForwarder == null) {
            return super.onTouchEvent(me)
        }

        val rect: Rect = mInputViewRect
        getGlobalVisibleRect(rect)
        val index: Int = me.actionIndex
        val x: Int = me.getX(index).toInt() + rect.left
        val y: Int = me.getY(index).toInt() + rect.top
        return mActiveForwarder?.onTouchEvent(x, y, me) == true
    }

    /**
     * This class forwards series of [MotionEvent]s from `SenderView` to
     * `ReceiverView`.
     *
     * @param <SenderView> a [View] that may send a [MotionEvent] to <ReceiverView>.
     * @param <ReceiverView> a [View] that receives forwarded [MotionEvent] from
     * <SenderView>.
    </SenderView></ReceiverView></ReceiverView></SenderView> */
    private abstract class MotionEventForwarder<SenderView : View?, ReceiverView : View?>(
        senderView: SenderView?,
        receiverView: ReceiverView
    ) {
        protected val mSenderView: SenderView? = senderView
        protected val mReceiverView: ReceiverView = receiverView

        protected val mEventSendingRect: Rect = Rect()
        protected val mEventReceivingRect: Rect = Rect()

        // Return true if a touch event of global coordinate x, y needs to be forwarded.
        protected abstract fun needsToForward(x: Int, y: Int): Boolean

        // Translate global x-coordinate to <code>ReceiverView</code> local coordinate.
        protected fun translateX(x: Int): Int {
            return x - mEventReceivingRect.left
        }

        // Translate global y-coordinate to <code>ReceiverView</code> local coordinate.
        protected open fun translateY(y: Int): Int {
            return y - mEventReceivingRect.top
        }

        /**
         * Callback when a [MotionEvent] is forwarded.
         * @param me the motion event to be forwarded.
         */
        protected open fun onForwardingEvent(me: MotionEvent) {}

        // Returns true if a {@link MotionEvent} is needed to be forwarded to
        // <code>ReceiverView</code>. Otherwise returns false.
        fun onInterceptTouchEvent(x: Int, y: Int, me: MotionEvent): Boolean {
            // Forwards a {link MotionEvent} only if both <code>SenderView</code> and
            // <code>ReceiverView</code> are visible.
            if (mSenderView?.visibility != VISIBLE ||
                mReceiverView?.visibility != VISIBLE
            ) {
                return false
            }
            mSenderView.getGlobalVisibleRect(mEventSendingRect)
            if (!mEventSendingRect.contains(x, y)) {
                return false
            }

            if (me.actionMasked == MotionEvent.ACTION_DOWN) {
                // If the down event happens in the forwarding area, successive
                // {@link MotionEvent}s should be forwarded to <code>ReceiverView</code>.
                if (needsToForward(x, y)) {
                    return true
                }
            }

            return false
        }

        // Returns true if a {@link MotionEvent} is forwarded to <code>ReceiverView</code>.
        // Otherwise returns false.
        fun onTouchEvent(x: Int, y: Int, me: MotionEvent): Boolean {
            mReceiverView?.getGlobalVisibleRect(mEventReceivingRect)
            // Translate global coordinates to <code>ReceiverView</code> local coordinates.
            me.setLocation(translateX(x).toFloat(), translateY(y).toFloat())
            mReceiverView?.dispatchTouchEvent(me)
            onForwardingEvent(me)
            return true
        }
    }

    /**
     * This class forwards [MotionEvent]s happened in the top padding of
     * [MainKeyboardView] to [SuggestionStripView].
     */
    private class KeyboardTopPaddingForwarder
        (
        mainKeyboardView: MainKeyboardView?,
        suggestionStripView: SuggestionStripView
    ) : MotionEventForwarder<MainKeyboardView?, SuggestionStripView?>(
        mainKeyboardView,
        suggestionStripView
    ) {
        private var mKeyboardTopPadding: Int = 0

        fun setKeyboardTopPadding(keyboardTopPadding: Int) {
            mKeyboardTopPadding = keyboardTopPadding
        }

        fun isInKeyboardTopPadding(y: Int): Boolean {
            return y < mEventSendingRect.top + mKeyboardTopPadding
        }

        override fun needsToForward(x: Int, y: Int): Boolean {
            // Forwarding an event only when {@link MainKeyboardView} is visible.
            // Because the visibility of {@link MainKeyboardView} is controlled by its parent
            // view in {@link KeyboardSwitcher#setMainKeyboardFrame()}, we should check the
            // visibility of the parent view.
            val mainKeyboardFrame: View = mSenderView?.parent as View
            return mainKeyboardFrame.visibility == VISIBLE && isInKeyboardTopPadding(y)
        }

        override fun translateY(y: Int): Int {
            val translatedY: Int = super.translateY(y)
            if (isInKeyboardTopPadding(y)) {
                // The forwarded event should have coordinates that are inside of the target.
                return min(
                    translatedY.toDouble(),
                    (mEventReceivingRect.height() - 1).toDouble()
                ).toInt()
            }
            return translatedY
        }
    }

    /**
     * This class forwards [MotionEvent]s happened in the [MainKeyboardView] to
     * [SuggestionStripView] when the [MoreSuggestionsView] is showing.
     * [SuggestionStripView] dismisses [MoreSuggestionsView] when it receives any event
     * outside of it.
     */
    private class MoreSuggestionsViewCanceler
        (
        mainKeyboardView: MainKeyboardView?,
        suggestionStripView: SuggestionStripView
    ) : MotionEventForwarder<MainKeyboardView?, SuggestionStripView?>(
        mainKeyboardView,
        suggestionStripView
    ) {
        override fun needsToForward(x: Int, y: Int): Boolean {
            return mReceiverView?.isShowingMoreSuggestionPanel == true && mEventSendingRect.contains(
                x,
                y
            )
        }

        override fun onForwardingEvent(me: MotionEvent) {
            if (me.actionMasked == MotionEvent.ACTION_DOWN) {
                mReceiverView?.dismissMoreSuggestionsPanel()
            }
        }
    }
}
