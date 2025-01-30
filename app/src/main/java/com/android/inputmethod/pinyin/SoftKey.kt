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
 * @see com.android.inputmethod.pinyin.SoftKeyToggle
 */
open class SoftKey {
    /**
     * Used to indicate the type and attributes of this key. the lowest 8 bits
     * should be reserved for SoftkeyToggle.
     */
    protected var mKeyMask: Int = 0

    var mKeyType: SoftKeyType? = null

    open var keyIcon: Drawable? = null
        protected set

    protected var mKeyIconPopup: Drawable? = null

    open var keyLabel: String? = null
        protected set

    open var keyCode: Int = 0

    /**
     * If this value is not 0, this key can be used to popup a sub soft keyboard
     * when user presses it for some time.
     */
    var popupResId: Int = 0

    var mLeftF: Float = 0f
    var mRightF: Float = 0f
    var mTopF: Float = 0f
    var mBottomF: Float = 0f
    var mLeft: Int = 0
    var mRight: Int = 0
    var mTop: Int = 0
    var mBottom: Int = 0

    fun setKeyType(
        keyType: SoftKeyType?, keyIcon: Drawable?,
        keyIconPopup: Drawable?
    ) {
        mKeyType = keyType
        this.keyIcon = keyIcon
        mKeyIconPopup = keyIconPopup
    }

    // The caller guarantees that all parameters are in [0, 1]
    fun setKeyDimensions(
        left: Float, top: Float, right: Float,
        bottom: Float
    ) {
        mLeftF = left
        mTopF = top
        mRightF = right
        mBottomF = bottom
    }

    fun setKeyAttribute(
        keyCode: Int, label: String?, repeat: Boolean,
        balloon: Boolean
    ) {
        this.keyCode = keyCode
        keyLabel = label

        mKeyMask = if (repeat) {
            mKeyMask or KEYMASK_REPEAT
        } else {
            mKeyMask and (KEYMASK_REPEAT.inv())
        }

        mKeyMask = if (balloon) {
            mKeyMask or KEYMASK_BALLOON
        } else {
            mKeyMask and (KEYMASK_BALLOON.inv())
        }
    }

    fun setPopupSkbId(popupSkbId: Int) {
        popupResId = popupSkbId
    }

    // Call after setKeyDimensions(). The caller guarantees that the
    // keyboard with and height are valid.
    fun setSkbCoreSize(skbWidth: Int, skbHeight: Int) {
        mLeft = (mLeftF * skbWidth).toInt()
        mRight = (mRightF * skbWidth).toInt()
        mTop = (mTopF * skbHeight).toInt()
        mBottom = (mBottomF * skbHeight).toInt()
    }

    open val keyIconPopup: Drawable?
        get() {
            if (null != mKeyIconPopup) {
                return mKeyIconPopup
            }
            return keyIcon
        }

    open fun changeCase(upperCase: Boolean) {
        if (null != keyLabel) {
            if (upperCase) keyLabel = keyLabel!!.uppercase(Locale.getDefault())
            else keyLabel = keyLabel!!.lowercase(Locale.getDefault())
        }
    }

    open val keyBg: Drawable?
        get() = mKeyType!!.mKeyBg

    open val keyHlBg: Drawable?
        get() = mKeyType!!.mKeyHlBg

    open val color: Int
        get() = mKeyType!!.mColor

    open val colorHl: Int
        get() = mKeyType!!.mColorHl

    open val colorBalloon: Int
        get() = mKeyType!!.mColorBalloon

    open val isKeyCodeKey: Boolean
        get() {
            if (keyCode > 0) return true
            return false
        }

    open val isUserDefKey: Boolean
        get() {
            if (keyCode < 0) return true
            return false
        }

    open val isUniStrKey: Boolean
        get() {
            if (null != keyLabel && keyCode == 0) return true
            return false
        }

    open fun needBalloon(): Boolean {
        return (mKeyMask and KEYMASK_BALLOON) != 0
    }

    open fun repeatable(): Boolean {
        return (mKeyMask and KEYMASK_REPEAT) != 0
    }

    fun width(): Int {
        return mRight - mLeft
    }

    fun height(): Int {
        return mBottom - mTop
    }

    fun moveWithinKey(x: Int, y: Int): Boolean {
        if (mLeft - MAX_MOVE_TOLERANCE_X <= x && mTop - MAX_MOVE_TOLERANCE_Y <= y && mRight + MAX_MOVE_TOLERANCE_X > x && mBottom + MAX_MOVE_TOLERANCE_Y > y) {
            return true
        }
        return false
    }

    override fun toString(): String {
        var str = "\n"
        str += "  keyCode: $keyCode\n"
        str += "  keyMask: $mKeyMask\n"
        str += "  keyLabel: " + (if (keyLabel == null) "null" else keyLabel) + "\n"
        str += "  popupResId: $popupResId\n"
        str += ("  Position: " + mLeftF.toString() + ", "
                + mTopF.toString() + ", " + mRightF.toString() + ", "
                + mBottomF.toString() + "\n")
        return str
    }

    companion object {
        const val KEYMASK_REPEAT: Int = 0x10000000
        const val KEYMASK_BALLOON: Int = 0x20000000

        /**
         * For a finger touch device, after user presses a key, there will be some
         * consequent moving events because of the changing in touching pressure. If
         * the moving distance in x is within this threshold, the moving events will
         * be ignored.
         */
        const val MAX_MOVE_TOLERANCE_X: Int = 0

        /**
         * For a finger touch device, after user presses a key, there will be some
         * consequent moving events because of the changing in touching pressure. If
         * the moving distance in y is within this threshold, the moving events will
         * be ignored.
         */
        const val MAX_MOVE_TOLERANCE_Y: Int = 0
    }
}
