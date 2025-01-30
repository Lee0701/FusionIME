/*
 * Copyright (C) 2009 The Android Open Source Project
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
package com.android.inputmethod.pinyin

import android.graphics.drawable.Drawable
import java.util.Locale

/**
 * Class for soft keys which defined in the keyboard xml file. A soft key can be
 * a basic key or a toggling key.
 *
 * @see com.android.inputmethod.pinyin.SoftKey
 */
class SoftKeyToggle : SoftKey() {
    private var mToggleState: ToggleState? = null

    val toggleStateId: Int
        get() = (mKeyMask and KEYMASK_TOGGLE_STATE)

    // The state id should be valid, and less than 255.
    // If resetIfNotFound is true and there is no such toggle state with the
    // given id, the key state will be reset.
    // If the key state is newly changed (enabled to the given state, or
    // reseted) and needs re-draw, return true.
    fun enableToggleState(stateId: Int, resetIfNotFound: Boolean): Boolean {
        val oldStateId = (mKeyMask and KEYMASK_TOGGLE_STATE)
        if (oldStateId == stateId) return false

        mKeyMask = mKeyMask and (KEYMASK_TOGGLE_STATE.inv())
        if (stateId > 0) {
            mKeyMask = mKeyMask or (KEYMASK_TOGGLE_STATE and stateId)
            if (toggleState == null) {
                mKeyMask = mKeyMask and (KEYMASK_TOGGLE_STATE.inv())
                if (!resetIfNotFound && oldStateId > 0) {
                    mKeyMask = mKeyMask or (KEYMASK_TOGGLE_STATE and oldStateId)
                }
                return resetIfNotFound
            } else {
                return true
            }
        } else {
            return true
        }
    }

    // The state id should be valid, and less than 255.
    // If resetIfNotFound is true and there is no such toggle state with the
    // given id, the key state will be reset.
    // If the key state is newly changed and needs re-draw, return true.
    fun disableToggleState(stateId: Int, resetIfNotFound: Boolean): Boolean {
        val oldStateId = (mKeyMask and KEYMASK_TOGGLE_STATE)
        if (oldStateId == stateId) {
            mKeyMask = mKeyMask and (KEYMASK_TOGGLE_STATE.inv())
            return stateId != 0
        }

        if (resetIfNotFound) {
            mKeyMask = mKeyMask and (KEYMASK_TOGGLE_STATE.inv())
            return oldStateId != 0
        }
        return false
    }

    // Clear any toggle state. If the key needs re-draw, return true.
    fun disableAllToggleStates(): Boolean {
        val oldStateId = (mKeyMask and KEYMASK_TOGGLE_STATE)
        mKeyMask = mKeyMask and (KEYMASK_TOGGLE_STATE.inv())
        return oldStateId != 0
    }

    override var keyIcon: Drawable?
        get() {
            val state = toggleState
            if (null != state) return state.mKeyIcon
            return super.keyIcon
        }
        set(keyIcon) {
            super.keyIcon = keyIcon
        }

    override val keyIconPopup: Drawable?
        get() {
            val state = toggleState
            if (null != state) {
                return if (null != state.mKeyIconPopup) {
                    state.mKeyIconPopup
                } else {
                    state.mKeyIcon
                }
            }
            return super.keyIconPopup
        }

    override var keyCode: Int
        get() {
            val state = toggleState
            if (null != state) return state.mKeyCode
            return super.keyCode
        }
        set(keyCode) {
            super.keyCode = keyCode
        }

    override var keyLabel: String?
        get() {
            val state = toggleState
            if (null != state) return state.mKeyLabel
            return super.keyLabel
        }
        set(keyLabel) {
            super.keyLabel = keyLabel
        }

    override val keyBg: Drawable?
        get() {
            val state = toggleState
            if (state?.mKeyType != null) {
                return state.mKeyType!!.mKeyBg
            }
            return mKeyType!!.mKeyBg
        }

    override val keyHlBg: Drawable?
        get() {
            val state = toggleState
            if (state?.mKeyType != null) {
                return state.mKeyType!!.mKeyHlBg
            }
            return mKeyType!!.mKeyHlBg
        }

    override val color: Int
        get() {
            val state = toggleState
            if (state?.mKeyType != null) {
                return state.mKeyType!!.mColor
            }
            return mKeyType!!.mColor
        }

    override val colorHl: Int
        get() {
            val state = toggleState
            if (state?.mKeyType != null) {
                return state.mKeyType!!.mColorHl
            }
            return mKeyType!!.mColorHl
        }

    override val colorBalloon: Int
        get() {
            val state = toggleState
            if (state?.mKeyType != null) {
                return state.mKeyType!!.mColorBalloon
            }
            return mKeyType!!.mColorBalloon
        }

    override val isKeyCodeKey: Boolean
        get() {
            val state = toggleState
            if (null != state) {
                if (state.mKeyCode > 0) return true
                return false
            }
            return super.isKeyCodeKey
        }

    override val isUserDefKey: Boolean
        get() {
            val state = toggleState
            if (null != state) {
                if (state.mKeyCode < 0) return true
                return false
            }
            return super.isUserDefKey
        }

    override val isUniStrKey: Boolean
        get() {
            val state = toggleState
            if (null != state) {
                if (null != state.mKeyLabel && state.mKeyCode == 0) {
                    return true
                }
                return false
            }
            return super.isUniStrKey
        }

    override fun needBalloon(): Boolean {
        val state = toggleState
        if (null != state) {
            return (state.mIdAndFlags and KEYMASK_BALLOON) != 0
        }
        return super.needBalloon()
    }

    override fun repeatable(): Boolean {
        val state = toggleState
        if (null != state) {
            return (state.mIdAndFlags and SoftKey.KEYMASK_REPEAT) != 0
        }
        return super.repeatable()
    }

    override fun changeCase(lowerCase: Boolean) {
        val state = toggleState
        if (state?.mKeyLabel != null) {
            if (lowerCase) state.mKeyLabel = state.mKeyLabel!!.lowercase(Locale.getDefault())
            else state.mKeyLabel = state.mKeyLabel!!.uppercase(Locale.getDefault())
        }
    }

    fun createToggleState(): ToggleState {
        return ToggleState()
    }

    fun setToggleStates(rootState: ToggleState?): Boolean {
        if (null == rootState) return false
        mToggleState = rootState
        return true
    }

    private val toggleState: ToggleState?
        get() {
            val stateId = (mKeyMask and KEYMASK_TOGGLE_STATE)
            if (0 == stateId) return null

            var state = mToggleState
            while ((null != state)
                && (state.mIdAndFlags and KEYMASK_TOGGLE_STATE) != stateId
            ) {
                state = state.mNextState
            }
            return state
        }

    inner class ToggleState {
        // The id should be bigger than 0;
        var mIdAndFlags: Int = 0
        var mKeyType: SoftKeyType? = null
        var mKeyCode: Int = 0
        var mKeyIcon: Drawable? = null
        var mKeyIconPopup: Drawable? = null
        var mKeyLabel: String? = null
        var mNextState: ToggleState? = null

        fun setStateId(stateId: Int) {
            mIdAndFlags = mIdAndFlags or (stateId and KEYMASK_TOGGLE_STATE)
        }

        fun setStateFlags(repeat: Boolean, balloon: Boolean) {
            mIdAndFlags = if (repeat) {
                mIdAndFlags or SoftKey.KEYMASK_REPEAT
            } else {
                mIdAndFlags and (SoftKey.KEYMASK_REPEAT.inv())
            }

            mIdAndFlags = if (balloon) {
                mIdAndFlags or KEYMASK_BALLOON
            } else {
                mIdAndFlags and (KEYMASK_BALLOON.inv())
            }
        }
    }

    companion object {
        /**
         * The current state number is stored in the lowest 8 bits of mKeyMask, this
         * mask is used to get the state number. If the current state is 0, the
         * normal state is enabled; if the current state is more than 0, a toggle
         * state in the toggle state chain will be enabled.
         */
        private const val KEYMASK_TOGGLE_STATE = 0x000000ff
    }
}
