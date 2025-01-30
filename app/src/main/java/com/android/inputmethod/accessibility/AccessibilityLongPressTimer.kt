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

import android.content.Context
import android.os.Handler
import android.os.Message
import com.android.inputmethod.keyboard.Key
import ee.oyatl.ime.fusion.R

// Handling long press timer to show a more keys keyboard.
internal class AccessibilityLongPressTimer(
    callback: LongPressTimerCallback,
    context: Context
) : Handler() {
    interface LongPressTimerCallback {
        fun performLongClickOn(key: Key)
    }

    private val mCallback: LongPressTimerCallback = callback
    private val mConfigAccessibilityLongPressTimeout: Long = context.resources.getInteger(
        R.integer.config_accessibility_long_press_key_timeout
    ).toLong()

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            MSG_LONG_PRESS -> {
                cancelLongPress()
                mCallback.performLongClickOn(msg.obj as Key)
                return
            }

            else -> {
                super.handleMessage(msg)
                return
            }
        }
    }

    fun startLongPress(key: Key?) {
        cancelLongPress()
        val longPressMessage: Message = obtainMessage(MSG_LONG_PRESS, key)
        sendMessageDelayed(longPressMessage, mConfigAccessibilityLongPressTimeout)
    }

    fun cancelLongPress() {
        removeMessages(MSG_LONG_PRESS)
    }

    companion object {
        private const val MSG_LONG_PRESS: Int = 1
    }
}
