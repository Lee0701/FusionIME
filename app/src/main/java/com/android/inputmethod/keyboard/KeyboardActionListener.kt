/*
 * Copyright (C) 2010 The Android Open Source Project
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

import com.android.inputmethod.latin.common.InputPointers

interface KeyboardActionListener {
    /**
     * Called when the user presses a key. This is sent before the [.onCodeInput] is called.
     * For keys that repeat, this is only called once.
     *
     * @param primaryCode the unicode of the key being pressed. If the touch is not on a valid key,
     * the value will be zero.
     * @param repeatCount how many times the key was repeated. Zero if it is the first press.
     * @param isSinglePointer true if pressing has occurred while no other key is being pressed.
     */
    fun onPressKey(primaryCode: Int, repeatCount: Int, isSinglePointer: Boolean)

    /**
     * Called when the user releases a key. This is sent after the [.onCodeInput] is called.
     * For keys that repeat, this is only called once.
     *
     * @param primaryCode the code of the key that was released
     * @param withSliding true if releasing has occurred because the user slid finger from the key
     * to other key without releasing the finger.
     */
    fun onReleaseKey(primaryCode: Int, withSliding: Boolean)

    /**
     * Send a key code to the listener.
     *
     * @param primaryCode this is the code of the key that was pressed
     * @param x x-coordinate pixel of touched event. If [.onCodeInput] is not called by
     * [PointerTracker] or so, the value should be
     * [Constants.NOT_A_COORDINATE]. If it's called on insertion from the
     * suggestion strip, it should be [Constants.SUGGESTION_STRIP_COORDINATE].
     * @param y y-coordinate pixel of touched event. If [.onCodeInput] is not called by
     * [PointerTracker] or so, the value should be
     * [Constants.NOT_A_COORDINATE].If it's called on insertion from the
     * suggestion strip, it should be [Constants.SUGGESTION_STRIP_COORDINATE].
     * @param isKeyRepeat true if this is a key repeat, false otherwise
     */
    // TODO: change this to send an Event object instead
    fun onCodeInput(primaryCode: Int, x: Int, y: Int, isKeyRepeat: Boolean)

    /**
     * Sends a string of characters to the listener.
     *
     * @param text the string of characters to be registered.
     */
    fun onTextInput(text: String?)

    /**
     * Called when user started batch input.
     */
    fun onStartBatchInput()

    /**
     * Sends the ongoing batch input points data.
     * @param batchPointers the batch input points representing the user input
     */
    fun onUpdateBatchInput(batchPointers: InputPointers)

    /**
     * Sends the final batch input points data.
     *
     * @param batchPointers the batch input points representing the user input
     */
    fun onEndBatchInput(batchPointers: InputPointers)

    fun onCancelBatchInput()

    /**
     * Called when user released a finger outside any key.
     */
    fun onCancelInput()

    /**
     * Called when user finished sliding key input.
     */
    fun onFinishSlidingInput()

    /**
     * Send a non-"code input" custom request to the listener.
     * @return true if the request has been consumed, false otherwise.
     */
    fun onCustomRequest(requestCode: Int): Boolean

    class Adapter : KeyboardActionListener {
        override fun onPressKey(primaryCode: Int, repeatCount: Int, isSinglePointer: Boolean) {}
        override fun onReleaseKey(primaryCode: Int, withSliding: Boolean) {}
        override fun onCodeInput(primaryCode: Int, x: Int, y: Int, isKeyRepeat: Boolean) {}
        override fun onTextInput(text: String?) {}
        override fun onStartBatchInput() {}
        override fun onUpdateBatchInput(batchPointers: InputPointers) {}
        override fun onEndBatchInput(batchPointers: InputPointers) {}
        override fun onCancelBatchInput() {}
        override fun onCancelInput() {}
        override fun onFinishSlidingInput() {}
        override fun onCustomRequest(requestCode: Int): Boolean {
            return false
        }
    }

    companion object {
        val EMPTY_LISTENER: KeyboardActionListener = Adapter()
    }
}
