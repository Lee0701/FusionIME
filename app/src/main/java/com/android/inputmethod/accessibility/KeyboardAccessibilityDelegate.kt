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
package com.android.inputmethod.accessibility

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewParent
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.android.inputmethod.keyboard.Key
import com.android.inputmethod.keyboard.KeyDetector
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.keyboard.KeyboardView

/**
 * This class represents a delegate that can be registered in a class that extends
 * [KeyboardView] to enhance accessibility support via composition rather via inheritance.
 *
 * To implement accessibility mode, the target keyboard view has to:
 *
 *
 * - Call [.setKeyboard] when a new keyboard is set to the keyboard view.
 * - Dispatch a hover event by calling [.onHoverEnter].
 *
 * @param <KV> The keyboard view class type.
</KV> */
open class KeyboardAccessibilityDelegate<KV : KeyboardView>
    (keyboardView: KV, keyDetector: KeyDetector) : AccessibilityDelegateCompat() {
    protected val mKeyboardView: KV
    protected val mKeyDetector: KeyDetector
    private var mKeyboard: Keyboard? = null
    private var mAccessibilityNodeProvider: KeyboardAccessibilityNodeProvider<KV>? = null
    private var mLastHoverKey: Key? = null

    init {
        mKeyboardView = keyboardView
        mKeyDetector = keyDetector

        // Ensure that the view has an accessibility delegate.
        ViewCompat.setAccessibilityDelegate(keyboardView, this)
    }

    /**
     * Called when the keyboard layout changes.
     *
     *
     * **Note:** This method will be called even if accessibility is not
     * enabled.
     * @param keyboard The keyboard that is being set to the wrapping view.
     */
    open fun setKeyboard(keyboard: Keyboard?) {
        if (keyboard == null) {
            return
        }
        if (mAccessibilityNodeProvider != null) {
            mAccessibilityNodeProvider!!.setKeyboard(keyboard)
        }
        mKeyboard = keyboard
    }

    protected fun getKeyboard(): Keyboard? {
        return mKeyboard
    }

    protected fun setLastHoverKey(key: Key?) {
        mLastHoverKey = key
    }

    protected fun getLastHoverKey(): Key? {
        return mLastHoverKey
    }

    /**
     * Sends a window state change event with the specified string resource id.
     *
     * @param resId The string resource id of the text to send with the event.
     */
    protected fun sendWindowStateChanged(resId: Int) {
        if (resId == 0) {
            return
        }
        val context: Context = mKeyboardView.context
        sendWindowStateChanged(context.getString(resId))
    }

    /**
     * Sends a window state change event with the specified text.
     *
     * @param text The text to send with the event.
     */
    protected fun sendWindowStateChanged(text: String?) {
        val stateChange: AccessibilityEvent = AccessibilityEvent.obtain(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        )
        mKeyboardView.onInitializeAccessibilityEvent(stateChange)
        stateChange.getText().add(text)
        stateChange.setContentDescription(null)

        val parent: ViewParent? = mKeyboardView.parent
        if (parent != null) {
            parent.requestSendAccessibilityEvent(mKeyboardView, stateChange)
        }
    }

    /**
     * Delegate method for View.getAccessibilityNodeProvider(). This method is called in SDK
     * version 15 (Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) and higher to obtain the virtual
     * node hierarchy provider.
     *
     * @param host The host view for the provider.
     * @return The accessibility node provider for the current keyboard.
     */
    override fun getAccessibilityNodeProvider(host: View): KeyboardAccessibilityNodeProvider<KV>? {
        return getAccessibilityNodeProvider()
    }

    /**
     * @return A lazily-instantiated node provider for this view delegate.
     */
    protected fun getAccessibilityNodeProvider(): KeyboardAccessibilityNodeProvider<KV> {
        // Instantiate the provide only when requested. Since the system
        // will call this method multiple times it is a good practice to
        // cache the provider instance.
        if (mAccessibilityNodeProvider == null) {
            mAccessibilityNodeProvider =
                KeyboardAccessibilityNodeProvider(mKeyboardView, this)
        }
        return mAccessibilityNodeProvider!!
    }

    /**
     * Get a key that a hover event is on.
     *
     * @param event The hover event.
     * @return key The key that the `event` is on.
     */
    protected fun getHoverKeyOf(event: MotionEvent): Key? {
        val actionIndex: Int = event.getActionIndex()
        val x: Int = event.getX(actionIndex).toInt()
        val y: Int = event.getY(actionIndex).toInt()
        return mKeyDetector.detectHitKey(x, y)
    }

    /**
     * Receives hover events when touch exploration is turned on in SDK versions ICS and higher.
     *
     * @param event The hover event.
     * @return `true` if the event is handled.
     */
    fun onHoverEvent(event: MotionEvent): Boolean {
        when (event.getActionMasked()) {
            MotionEvent.ACTION_HOVER_ENTER -> onHoverEnter(event)
            MotionEvent.ACTION_HOVER_MOVE -> onHoverMove(event)
            MotionEvent.ACTION_HOVER_EXIT -> onHoverExit(event)
            else -> Log.w(javaClass.getSimpleName(), "Unknown hover event: " + event)
        }
        return true
    }

    /**
     * Process [MotionEvent.ACTION_HOVER_ENTER] event.
     *
     * @param event A hover enter event.
     */
    protected open fun onHoverEnter(event: MotionEvent) {
        val key: Key? = getHoverKeyOf(event)
        if (DEBUG_HOVER) {
            Log.d(TAG, "onHoverEnter: key=" + key)
        }
        if (key != null) {
            onHoverEnterTo(key)
        }
        setLastHoverKey(key)
    }

    /**
     * Process [MotionEvent.ACTION_HOVER_MOVE] event.
     *
     * @param event A hover move event.
     */
    protected open fun onHoverMove(event: MotionEvent) {
        val lastKey: Key? = getLastHoverKey()
        val key: Key? = getHoverKeyOf(event)
        if (key !== lastKey) {
            if (lastKey != null) {
                onHoverExitFrom(lastKey)
            }
            if (key != null) {
                onHoverEnterTo(key)
            }
        }
        if (key != null) {
            onHoverMoveWithin(key)
        }
        setLastHoverKey(key)
    }

    /**
     * Process [MotionEvent.ACTION_HOVER_EXIT] event.
     *
     * @param event A hover exit event.
     */
    protected open fun onHoverExit(event: MotionEvent) {
        val lastKey: Key? = getLastHoverKey()
        if (DEBUG_HOVER) {
            Log.d(TAG, "onHoverExit: key=" + getHoverKeyOf(event) + " last=" + lastKey)
        }
        if (lastKey != null) {
            onHoverExitFrom(lastKey)
        }
        val key: Key? = getHoverKeyOf(event)
        // Make sure we're not getting an EXIT event because the user slid
        // off the keyboard area, then force a key press.
        if (key != null) {
            onHoverExitFrom(key)
        }
        setLastHoverKey(null)
    }

    /**
     * Perform click on a key.
     *
     * @param key A key to be registered.
     */
    open fun performClickOn(key: Key) {
        if (DEBUG_HOVER) {
            Log.d(TAG, "performClickOn: key=" + key)
        }
        simulateTouchEvent(MotionEvent.ACTION_DOWN, key)
        simulateTouchEvent(MotionEvent.ACTION_UP, key)
    }

    /**
     * Simulating a touch event by injecting a synthesized touch event into [KeyboardView].
     *
     * @param touchAction The action of the synthesizing touch event.
     * @param key The key that a synthesized touch event is on.
     */
    private fun simulateTouchEvent(touchAction: Int, key: Key) {
        val x: Int = key.hitBox.centerX()
        val y: Int = key.hitBox.centerY()
        val eventTime: Long = SystemClock.uptimeMillis()
        val touchEvent: MotionEvent = MotionEvent.obtain(
            eventTime, eventTime, touchAction, x.toFloat(), y.toFloat(), 0 /* metaState */
        )
        mKeyboardView.onTouchEvent(touchEvent)
        touchEvent.recycle()
    }

    /**
     * Handles a hover enter event on a key.
     *
     * @param key The currently hovered key.
     */
    protected open fun onHoverEnterTo(key: Key) {
        if (DEBUG_HOVER) {
            Log.d(TAG, "onHoverEnterTo: key=" + key)
        }
        key.onPressed()
        mKeyboardView.invalidateKey(key)
        val provider: KeyboardAccessibilityNodeProvider<KV> = getAccessibilityNodeProvider()
        provider.onHoverEnterTo(key)
        provider.performActionForKey(key, AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS)
    }

    /**
     * Handles a hover move event on a key.
     *
     * @param key The currently hovered key.
     */
    protected fun onHoverMoveWithin(key: Key?) {}

    /**
     * Handles a hover exit event on a key.
     *
     * @param key The currently hovered key.
     */
    protected open fun onHoverExitFrom(key: Key) {
        if (DEBUG_HOVER) {
            Log.d(TAG, "onHoverExitFrom: key=" + key)
        }
        key.onReleased()
        mKeyboardView!!.invalidateKey(key)
        val provider: KeyboardAccessibilityNodeProvider<KV> = getAccessibilityNodeProvider()
        provider.onHoverExitFrom(key)
    }

    /**
     * Perform long click on a key.
     *
     * @param key A key to be long pressed on.
     */
    open fun performLongClickOn(key: Key) {
        // A extended class should override this method to implement long press.
    }

    companion object {
        private val TAG: String = KeyboardAccessibilityDelegate::class.java.getSimpleName()
        const val DEBUG_HOVER: Boolean = false

        const val HOVER_EVENT_POINTER_ID: Int = 0
    }
}
