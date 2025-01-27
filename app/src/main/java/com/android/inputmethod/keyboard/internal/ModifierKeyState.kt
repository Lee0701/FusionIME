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
package com.android.inputmethod.keyboard.internal

import android.util.Log

/* package */
internal open class ModifierKeyState(name: String) {
    protected val mName: String
    protected var mState: Int = RELEASING

    init {
        mName = name
    }

    fun onPress() {
        val oldState: Int = mState
        mState = PRESSING
        if (DEBUG) Log.d(TAG, mName + ".onPress: " + toString(oldState) + " > " + this)
    }

    fun onRelease() {
        val oldState: Int = mState
        mState = RELEASING
        if (DEBUG) Log.d(TAG, mName + ".onRelease: " + toString(oldState) + " > " + this)
    }

    open fun onOtherKeyPressed() {
        val oldState: Int = mState
        if (oldState == PRESSING) mState = CHORDING
        if (DEBUG) Log.d(TAG, mName + ".onOtherKeyPressed: " + toString(oldState) + " > " + this)
    }

    fun isPressing(): Boolean {
        return mState == PRESSING
    }

    fun isReleasing(): Boolean {
        return mState == RELEASING
    }

    fun isChording(): Boolean {
        return mState == CHORDING
    }

    override fun toString(): String {
        return toString(mState)
    }

    protected open fun toString(state: Int): String {
        when (state) {
            RELEASING -> return "RELEASING"
            PRESSING -> return "PRESSING"
            CHORDING -> return "CHORDING"
            else -> return "UNKNOWN"
        }
    }

    companion object {
        protected val TAG: String = ModifierKeyState::class.java.getSimpleName()
        protected const val DEBUG: Boolean = false

        protected const val RELEASING: Int = 0
        protected const val PRESSING: Int = 1
        protected const val CHORDING: Int = 2
    }
}
