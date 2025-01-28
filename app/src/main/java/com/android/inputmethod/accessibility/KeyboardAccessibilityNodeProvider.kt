/*
 * Copyright (C) 2012 The Android Open Source Project
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
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.EditorInfo
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityEventCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat
import androidx.core.view.accessibility.AccessibilityRecordCompat
import com.android.inputmethod.keyboard.Key
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.keyboard.KeyboardView
import com.android.inputmethod.latin.common.CoordinateUtils
import com.android.inputmethod.latin.settings.Settings
import com.android.inputmethod.latin.settings.SettingsValues

/**
 * Exposes a virtual view sub-tree for [KeyboardView] and generates
 * [AccessibilityEvent]s for individual [Key]s.
 *
 *
 * A virtual sub-tree is composed of imaginary [View]s that are reported
 * as a part of the view hierarchy for accessibility purposes. This enables
 * custom views that draw complex content to report them selves as a tree of
 * virtual views, thus conveying their logical structure.
 *
 */
class KeyboardAccessibilityNodeProvider<KV : KeyboardView>
    (
    keyboardView: KV,
    delegate: KeyboardAccessibilityDelegate<KV>
) : AccessibilityNodeProviderCompat() {
    private val mKeyCodeDescriptionMapper: KeyCodeDescriptionMapper
    private val mAccessibilityUtils: AccessibilityUtils

    /** Temporary rect used to calculate in-screen bounds.  */
    private val mTempBoundsInScreen: Rect = Rect()

    /** The parent view's cached on-screen location.  */
    private val mParentLocation: IntArray = CoordinateUtils.newInstance()

    /** The virtual view identifier for the focused node.  */
    private var mAccessibilityFocusedView: Int = UNDEFINED

    /** The virtual view identifier for the hovering node.  */
    private var mHoveringNodeId: Int = UNDEFINED

    /** The keyboard view to provide an accessibility node info.  */
    private val mKeyboardView: KV

    /** The accessibility delegate.  */
    private val mDelegate: KeyboardAccessibilityDelegate<KV>

    /** The current keyboard.  */
    private var mKeyboard: Keyboard? = null

    init {
        mKeyCodeDescriptionMapper = KeyCodeDescriptionMapper.getInstance()
        mAccessibilityUtils = AccessibilityUtils.instance
        mKeyboardView = keyboardView
        mDelegate = delegate

        // Since this class is constructed lazily, we might not get a subsequent
        // call to setKeyboard() and therefore need to call it now.
        setKeyboard(keyboardView.keyboard)
    }

    /**
     * Sets the keyboard represented by this node provider.
     *
     * @param keyboard The keyboard that is being set to the keyboard view.
     */
    fun setKeyboard(keyboard: Keyboard?) {
        mKeyboard = keyboard
    }

    private fun getKeyOf(virtualViewId: Int): Key? {
        if (mKeyboard == null) {
            return null
        }
        val sortedKeys: List<Key?> = mKeyboard?.sortedKeys ?: emptyList()
        // Use a virtual view id as an index of the sorted keys list.
        if (virtualViewId >= 0 && virtualViewId < sortedKeys.size) {
            return sortedKeys.get(virtualViewId)
        }
        return null
    }

    private fun getVirtualViewIdOf(key: Key): Int {
        if (mKeyboard == null) {
            return View.NO_ID
        }
        val sortedKeys: List<Key?> = mKeyboard?.sortedKeys ?: emptyList()
        val size: Int = sortedKeys.size
        for (index in 0 until size) {
            if (sortedKeys.get(index) === key) {
                // Use an index of the sorted keys list as a virtual view id.
                return index
            }
        }
        return View.NO_ID
    }

    /**
     * Creates and populates an [AccessibilityEvent] for the specified key
     * and event type.
     *
     * @param key A key on the host keyboard view.
     * @param eventType The event type to create.
     * @return A populated [AccessibilityEvent] for the key.
     * @see AccessibilityEvent
     */
    fun createAccessibilityEvent(key: Key, eventType: Int): AccessibilityEvent {
        val virtualViewId: Int = getVirtualViewIdOf(key)
        val keyDescription: String? = getKeyDescription(key)
        val event: AccessibilityEvent = AccessibilityEvent.obtain(eventType)
        event.setPackageName(mKeyboardView!!.getContext().getPackageName())
        event.setClassName(key.javaClass.getName())
        event.setContentDescription(keyDescription)
        event.setEnabled(true)
        val record: AccessibilityRecordCompat = AccessibilityEventCompat.asRecord(event)
        record.setSource(mKeyboardView, virtualViewId)
        return event
    }

    fun onHoverEnterTo(key: Key) {
        val id: Int = getVirtualViewIdOf(key)
        if (id == View.NO_ID) {
            return
        }
        // Start hovering on the key. Because our accessibility model is lift-to-type, we should
        // report the node info without click and long click actions to avoid unnecessary
        // announcements.
        mHoveringNodeId = id
        // Invalidate the node info of the key.
        sendAccessibilityEventForKey(key, AccessibilityEventCompat.TYPE_WINDOW_CONTENT_CHANGED)
        sendAccessibilityEventForKey(key, AccessibilityEventCompat.TYPE_VIEW_HOVER_ENTER)
    }

    fun onHoverExitFrom(key: Key) {
        mHoveringNodeId = UNDEFINED
        // Invalidate the node info of the key to be able to revert the change we have done
        // in {@link #onHoverEnterTo(Key)}.
        sendAccessibilityEventForKey(key, AccessibilityEventCompat.TYPE_WINDOW_CONTENT_CHANGED)
        sendAccessibilityEventForKey(key, AccessibilityEventCompat.TYPE_VIEW_HOVER_EXIT)
    }

    /**
     * Returns an [AccessibilityNodeInfoCompat] representing a virtual
     * view, i.e. a descendant of the host View, with the given `virtualViewId` or
     * the host View itself if `virtualViewId` equals to [View.NO_ID].
     *
     *
     * A virtual descendant is an imaginary View that is reported as a part of
     * the view hierarchy for accessibility purposes. This enables custom views
     * that draw complex content to report them selves as a tree of virtual
     * views, thus conveying their logical structure.
     *
     *
     *
     * The implementer is responsible for obtaining an accessibility node info
     * from the pool of reusable instances and setting the desired properties of
     * the node info before returning it.
     *
     *
     * @param virtualViewId A client defined virtual view id.
     * @return A populated [AccessibilityNodeInfoCompat] for a virtual descendant or the host
     * View.
     * @see AccessibilityNodeInfoCompat
     */
    override fun createAccessibilityNodeInfo(virtualViewId: Int): AccessibilityNodeInfoCompat? {
        if (virtualViewId == UNDEFINED) {
            return null
        }
        if (virtualViewId == View.NO_ID) {
            // We are requested to create an AccessibilityNodeInfo describing
            // this View, i.e. the root of the virtual sub-tree.
            val rootInfo: AccessibilityNodeInfoCompat =
                AccessibilityNodeInfoCompat.obtain(mKeyboardView)
            ViewCompat.onInitializeAccessibilityNodeInfo(mKeyboardView, rootInfo)
            updateParentLocation()

            // Add the virtual children of the root View.
            val sortedKeys: List<Key?> = mKeyboard?.sortedKeys ?: listOf()
            val size: Int = sortedKeys.size
            for (index in 0 until size) {
                val key: Key? = sortedKeys[index]
                if (key!!.isSpacer) {
                    continue
                }
                // Use an index of the sorted keys list as a virtual view id.
                rootInfo.addChild(mKeyboardView, index)
            }
            return rootInfo
        }

        // Find the key that corresponds to the given virtual view id.
        val key: Key? = getKeyOf(virtualViewId)
        if (key == null) {
            Log.e(TAG, "Invalid virtual view ID: " + virtualViewId)
            return null
        }
        val keyDescription: String? = getKeyDescription(key)
        val boundsInParent: Rect = key.hitBox

        // Calculate the key's in-screen bounds.
        mTempBoundsInScreen.set(boundsInParent)
        mTempBoundsInScreen.offset(
            CoordinateUtils.x(mParentLocation), CoordinateUtils.y(mParentLocation)
        )
        val boundsInScreen: Rect = mTempBoundsInScreen

        // Obtain and initialize an AccessibilityNodeInfo with information about the virtual view.
        val info: AccessibilityNodeInfoCompat = AccessibilityNodeInfoCompat.obtain()
        info.setPackageName(mKeyboardView.getContext().getPackageName())
        info.setTextEntryKey(true)
        info.setClassName(key.javaClass.getName())
        info.setContentDescription(keyDescription)
        info.setBoundsInParent(boundsInParent)
        info.setBoundsInScreen(boundsInScreen)
        info.setParent(mKeyboardView)
        info.setSource(mKeyboardView, virtualViewId)
        info.setEnabled(key.isEnabled)
        info.setVisibleToUser(true)
        info.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
        if (key.isLongPressEnabled) {
            info.addAction(AccessibilityNodeInfoCompat.ACTION_LONG_CLICK)
        }

        if (mAccessibilityFocusedView == virtualViewId) {
            info.addAction(AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS)
        } else {
            info.addAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS)
        }
        return info
    }

    override fun performAction(
        virtualViewId: Int, action: Int,
        arguments: Bundle?
    ): Boolean {
        val key: Key? = getKeyOf(virtualViewId)
        if (key == null) {
            return false
        }
        return performActionForKey(key, action)
    }

    /**
     * Performs the specified accessibility action for the given key.
     *
     * @param key The on which to perform the action.
     * @param action The action to perform.
     * @return The result of performing the action, or false if the action is not supported.
     */
    fun performActionForKey(key: Key, action: Int): Boolean {
        when (action) {
            AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS -> {
                mAccessibilityFocusedView = getVirtualViewIdOf(key)
                sendAccessibilityEventForKey(
                    key, AccessibilityEventCompat.TYPE_VIEW_ACCESSIBILITY_FOCUSED
                )
                return true
            }

            AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS -> {
                mAccessibilityFocusedView = UNDEFINED
                sendAccessibilityEventForKey(
                    key, AccessibilityEventCompat.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED
                )
                return true
            }

            AccessibilityNodeInfoCompat.ACTION_CLICK -> {
                sendAccessibilityEventForKey(key, AccessibilityEvent.TYPE_VIEW_CLICKED)
                mDelegate.performClickOn(key)
                return true
            }

            AccessibilityNodeInfoCompat.ACTION_LONG_CLICK -> {
                sendAccessibilityEventForKey(key, AccessibilityEvent.TYPE_VIEW_LONG_CLICKED)
                mDelegate.performLongClickOn(key)
                return true
            }

            else -> return false
        }
    }

    /**
     * Sends an accessibility event for the given [Key].
     *
     * @param key The key that's sending the event.
     * @param eventType The type of event to send.
     */
    fun sendAccessibilityEventForKey(key: Key, eventType: Int) {
        val event: AccessibilityEvent = createAccessibilityEvent(key, eventType)
        mAccessibilityUtils.requestSendAccessibilityEvent(event)
    }

    /**
     * Returns the context-specific description for a [Key].
     *
     * @param key The key to describe.
     * @return The context-specific description of the key.
     */
    private fun getKeyDescription(key: Key): String? {
        val editorInfo: EditorInfo? = mKeyboard!!.mId!!.mEditorInfo
        val shouldObscure: Boolean = mAccessibilityUtils.shouldObscureInput(editorInfo)
        val currentSettings: SettingsValues? = Settings.instance.current
        val keyCodeDescription: String? = mKeyCodeDescriptionMapper.getDescriptionForKey(
            mKeyboardView.context, mKeyboard!!, key, shouldObscure
        )
        if (currentSettings!!.isWordSeparator(key.code)) {
            return mAccessibilityUtils.getAutoCorrectionDescription(
                keyCodeDescription, shouldObscure
            )
        }
        return keyCodeDescription
    }

    /**
     * Updates the parent's on-screen location.
     */
    private fun updateParentLocation() {
        mKeyboardView.getLocationOnScreen(mParentLocation)
    }

    companion object {
        private val TAG: String = KeyboardAccessibilityNodeProvider::class.java.getSimpleName()

        // From {@link android.view.accessibility.AccessibilityNodeInfo#UNDEFINED_ITEM_ID}.
        private val UNDEFINED: Int = Int.MAX_VALUE
    }
}
