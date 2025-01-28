/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.inputmethod.accessibility

import android.graphics.Rect
import android.util.Log
import android.view.MotionEvent
import com.android.inputmethod.keyboard.Key
import com.android.inputmethod.keyboard.KeyDetector
import com.android.inputmethod.keyboard.MoreKeysKeyboardView
import com.android.inputmethod.keyboard.PointerTracker

/**
 * This class represents a delegate that can be registered in [MoreKeysKeyboardView] to
 * enhance accessibility support via composition rather via inheritance.
 */
class MoreKeysKeyboardAccessibilityDelegate
    (
    moreKeysKeyboardView: MoreKeysKeyboardView,
    keyDetector: KeyDetector
) : KeyboardAccessibilityDelegate<MoreKeysKeyboardView>(moreKeysKeyboardView, keyDetector) {
    private val mMoreKeysKeyboardValidBounds: Rect = Rect()
    private var mOpenAnnounceResId: Int = 0
    private var mCloseAnnounceResId: Int = 0

    fun setOpenAnnounce(resId: Int) {
        mOpenAnnounceResId = resId
    }

    fun setCloseAnnounce(resId: Int) {
        mCloseAnnounceResId = resId
    }

    fun onShowMoreKeysKeyboard() {
        sendWindowStateChanged(mOpenAnnounceResId)
    }

    fun onDismissMoreKeysKeyboard() {
        sendWindowStateChanged(mCloseAnnounceResId)
    }

    override fun onHoverEnter(event: MotionEvent) {
        if (KeyboardAccessibilityDelegate.DEBUG_HOVER) {
            Log.d(TAG, "onHoverEnter: key=" + getHoverKeyOf(event))
        }
        super.onHoverEnter(event)
        val actionIndex: Int = event.getActionIndex()
        val x: Int = event.getX(actionIndex).toInt()
        val y: Int = event.getY(actionIndex).toInt()
        val pointerId: Int = event.getPointerId(actionIndex)
        val eventTime: Long = event.getEventTime()
        mKeyboardView!!.onDownEvent(x, y, pointerId, eventTime)
    }

    override fun onHoverMove(event: MotionEvent) {
        super.onHoverMove(event)
        val actionIndex: Int = event.getActionIndex()
        val x: Int = event.getX(actionIndex).toInt()
        val y: Int = event.getY(actionIndex).toInt()
        val pointerId: Int = event.getPointerId(actionIndex)
        val eventTime: Long = event.getEventTime()
        mKeyboardView!!.onMoveEvent(x, y, pointerId, eventTime)
    }

    override fun onHoverExit(event: MotionEvent) {
        val lastKey: Key? = getLastHoverKey()
        if (KeyboardAccessibilityDelegate.DEBUG_HOVER) {
            Log.d(TAG, "onHoverExit: key=" + getHoverKeyOf(event) + " last=" + lastKey)
        }
        if (lastKey != null) {
            super.onHoverExitFrom(lastKey)
        }
        setLastHoverKey(null)
        val actionIndex: Int = event.getActionIndex()
        val x: Int = event.getX(actionIndex).toInt()
        val y: Int = event.getY(actionIndex).toInt()
        val pointerId: Int = event.getPointerId(actionIndex)
        val eventTime: Long = event.getEventTime()
        // A hover exit event at one pixel width or height area on the edges of more keys keyboard
        // are treated as closing.
        mMoreKeysKeyboardValidBounds.set(
            0,
            0,
            mKeyboardView!!.getWidth(),
            mKeyboardView.getHeight()
        )
        mMoreKeysKeyboardValidBounds.inset(CLOSING_INSET_IN_PIXEL, CLOSING_INSET_IN_PIXEL)
        if (mMoreKeysKeyboardValidBounds.contains(x, y)) {
            // Invoke {@link MoreKeysKeyboardView#onUpEvent(int,int,int,long)} as if this hover
            // exit event selects a key.
            mKeyboardView.onUpEvent(x, y, pointerId, eventTime)
            // TODO: Should fix this reference. This is a hack to clear the state of
            // {@link PointerTracker}.
            PointerTracker.dismissAllMoreKeysPanels()
            return
        }
        // Close the more keys keyboard.
        // TODO: Should fix this reference. This is a hack to clear the state of
        // {@link PointerTracker}.
        PointerTracker.dismissAllMoreKeysPanels()
    }

    companion object {
        private val TAG: String = MoreKeysKeyboardAccessibilityDelegate::class.java.getSimpleName()

        private const val CLOSING_INSET_IN_PIXEL: Int = 1
    }
}
