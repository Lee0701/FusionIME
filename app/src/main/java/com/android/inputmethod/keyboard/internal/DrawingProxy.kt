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
package com.android.inputmethod.keyboard.internal

import com.android.inputmethod.keyboard.Key
import com.android.inputmethod.keyboard.MoreKeysPanel
import com.android.inputmethod.keyboard.PointerTracker
import javax.annotation.Nonnull

interface DrawingProxy {
    /**
     * Called when a key is being pressed.
     * @param key the [Key] that is being pressed.
     * @param withPreview true if key popup preview should be displayed.
     */
    fun onKeyPressed(@Nonnull key: Key, withPreview: Boolean)

    /**
     * Called when a key is being released.
     * @param key the [Key] that is being released.
     * @param withAnimation when true, key popup preview should be dismissed with animation.
     */
    fun onKeyReleased(@Nonnull key: Key, withAnimation: Boolean)

    /**
     * Start showing more keys keyboard of a key that is being long pressed.
     * @param key the [Key] that is being long pressed and showing more keys keyboard.
     * @param tracker the [PointerTracker] that detects this long pressing.
     * @return [MoreKeysPanel] that is being shown. null if there is no need to show more keys
     * keyboard.
     */
    fun showMoreKeysKeyboard(@Nonnull key: Key, @Nonnull tracker: PointerTracker): MoreKeysPanel?

    /**
     * Start a while-typing-animation.
     * @param fadeInOrOut [.FADE_IN] starts while-typing-fade-in animation.
     * [.FADE_OUT] starts while-typing-fade-out animation.
     */
    fun startWhileTypingAnimation(fadeInOrOut: Int)

    /**
     * Show sliding-key input preview.
     * @param tracker the [PointerTracker] that is currently doing the sliding-key input.
     * null to dismiss the sliding-key input preview.
     */
    fun showSlidingKeyInputPreview(tracker: PointerTracker?)

    /**
     * Show gesture trails.
     * @param tracker the [PointerTracker] whose gesture trail will be shown.
     * @param showsFloatingPreviewText when true, a gesture floating preview text will be shown
     * with this `tracker`'s trail.
     */
    fun showGestureTrail(@Nonnull tracker: PointerTracker?, showsFloatingPreviewText: Boolean)

    /**
     * Dismiss a gesture floating preview text without delay.
     */
    fun dismissGestureFloatingPreviewTextWithoutDelay()

    companion object {
        const val FADE_IN: Int = 0
        const val FADE_OUT: Int = 1
    }
}
