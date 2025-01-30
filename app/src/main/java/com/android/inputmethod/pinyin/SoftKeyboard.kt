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

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.KeyEvent
import com.android.inputmethod.pinyin.InputModeSwitcher.ToggleStates

/**
 * Class used to represent a soft keyboard definition, including the height, the
 * background image, the image for high light, the keys, etc.
 */
class SoftKeyboard(
    /** The XML resource id for this soft keyboard.  */
    val skbXmlId: Int,
    /** The soft keyboard template for this soft keyboard.  */
    private val mSkbTemplate: SkbTemplate,
    /** The width of the soft keyboard.  */
    var skbCoreWidth: Int,
    /** The height of the soft keyboard.  */
    var skbCoreHeight: Int
) {
    /** Do we need to cache this soft keyboard?  */
    var cacheFlag: Boolean = false
        private set

    /**
     * After user switches to this soft keyboard, if this flag is true, this
     * soft keyboard will be kept unless explicit switching operation is
     * performed, otherwise IME will switch back to the previous keyboard layout
     * whenever user clicks on any none-function key.
     */
    var stickyFlag: Boolean = false
        private set

    /**
     * The cache id for this soft keyboard. It is used to identify it in the
     * soft keyboard pool.
     */
    var cacheId: Int = 0

    /**
     * Used to indicate whether this soft keyboard is newly loaded from an XML
     * file or is just gotten from the soft keyboard pool.
     */
    var newlyLoadedFlag: Boolean = true

    /** Used to indicate whether this soft keyboard is a QWERTY keyboard.  */
    private var mIsQwerty = false

    /**
     * When [.mIsQwerty] is true, this member is Used to indicate that the
     * soft keyboard should be displayed in uppercase.
     */
    private var mIsQwertyUpperCase = false

    /**
     * The id of the rows which are enabled. Rows with id
     * [KeyRow.ALWAYS_SHOW_ROW_ID] are always enabled.
     */
    private var mEnabledRowId = 0

    /**
     * Rows in this soft keyboard. Each row has a id. Only matched rows will be
     * enabled.
     */
    private var mKeyRows: MutableList<KeyRow>? = null

    /**
     * Background of the soft keyboard. If it is null, the one in the soft
     * keyboard template will be used.
     */
    var mSkbBg: Drawable? = null

    /**
     * Background for key balloon. If it is null, the one in the soft keyboard
     * template will be used.
     */
    private var mBalloonBg: Drawable? = null

    /**
     * Background for popup mini soft keyboard. If it is null, the one in the
     * soft keyboard template will be used.
     */
    private var mPopupBg: Drawable? = null

    /** The left and right margin of a key.  */
    private var mKeyXMargin = 0f

    /** The top and bottom margin of a key.  */
    private var mKeyYMargin = 0f

    private val mTmpRect = Rect()

    fun setFlags(
        cacheFlag: Boolean, stickyFlag: Boolean,
        isQwerty: Boolean, isQwertyUpperCase: Boolean
    ) {
        this.cacheFlag = cacheFlag
        this.stickyFlag = stickyFlag
        mIsQwerty = isQwerty
        mIsQwertyUpperCase = isQwertyUpperCase
    }

    fun setKeyBalloonBackground(balloonBg: Drawable?) {
        mBalloonBg = balloonBg
    }

    fun setKeyMargins(xMargin: Float, yMargin: Float) {
        mKeyXMargin = xMargin
        mKeyYMargin = yMargin
    }

    fun reset() {
        if (null != mKeyRows) mKeyRows!!.clear()
    }

    fun beginNewRow(rowId: Int, yStartingPos: Float) {
        if (null == mKeyRows) mKeyRows = ArrayList()
        val keyRow = KeyRow()
        keyRow.mRowId = rowId
        keyRow.mTopF = yStartingPos
        keyRow.mBottomF = yStartingPos
        keyRow.mSoftKeys = ArrayList()
        mKeyRows!!.add(keyRow)
    }

    fun addSoftKey(softKey: SoftKey): Boolean {
        if (mKeyRows!!.size == 0) return false
        val keyRow = mKeyRows!![mKeyRows!!.size - 1] ?: return false
        val softKeys = keyRow.mSoftKeys

        softKey.setSkbCoreSize(skbCoreWidth, skbCoreHeight)
        softKeys!!.add(softKey)
        if (softKey.mTopF < keyRow.mTopF) {
            keyRow.mTopF = softKey.mTopF
        }
        if (softKey.mBottomF > keyRow.mBottomF) {
            keyRow.mBottomF = softKey.mBottomF
        }
        return true
    }

    // Set the size of the soft keyboard core. In other words, the background's
    // padding are not counted.
    fun setSkbCoreSize(skbCoreWidth: Int, skbCoreHeight: Int) {
        if (null == mKeyRows
            || (skbCoreWidth == this.skbCoreWidth && skbCoreHeight == this.skbCoreHeight)
        ) {
            return
        }
        for (row in mKeyRows!!.indices) {
            val keyRow = mKeyRows!![row]
            keyRow.mBottom = (skbCoreHeight * keyRow.mBottomF).toInt()
            keyRow.mTop = (skbCoreHeight * keyRow.mTopF).toInt()

            val softKeys: List<SoftKey>? = keyRow.mSoftKeys
            for (i in softKeys!!.indices) {
                val softKey = softKeys[i]
                softKey.setSkbCoreSize(skbCoreWidth, skbCoreHeight)
            }
        }
        this.skbCoreWidth = skbCoreWidth
        this.skbCoreHeight = skbCoreHeight
    }

    val skbTotalWidth: Int
        get() {
            val padding = padding
            return skbCoreWidth + padding.left + padding.right
        }

    val skbTotalHeight: Int
        get() {
            val padding = padding
            return skbCoreHeight + padding.top + padding.bottom
        }

    val keyXMargin: Int
        get() {
            val env: Environment =
                Environment.instance
            return (mKeyXMargin * skbCoreWidth * env.keyXMarginFactor).toInt()
        }

    val keyYMargin: Int
        get() {
            val env: Environment =
                Environment.instance
            return (mKeyYMargin * skbCoreHeight * env.keyYMarginFactor).toInt()
        }

    var skbBackground: Drawable?
        get() {
            if (null != mSkbBg) return mSkbBg
            return mSkbTemplate.skbBackground
        }
        set(skbBg) {
            mSkbBg = skbBg
        }

    val balloonBackground: Drawable?
        get() {
            if (null != mBalloonBg) return mBalloonBg
            return mSkbTemplate.balloonBackground
        }

    var popupBackground: Drawable?
        get() {
            if (null != mPopupBg) return mPopupBg
            return mSkbTemplate.popupBackground
        }
        set(popupBg) {
            mPopupBg = popupBg
        }

    val rowNum: Int
        get() {
            if (null != mKeyRows) {
                return mKeyRows!!.size
            }
            return 0
        }

    fun getKeyRowForDisplay(row: Int): KeyRow? {
        if (null != mKeyRows && mKeyRows!!.size > row) {
            val keyRow = mKeyRows!![row]
            if (KeyRow.ALWAYS_SHOW_ROW_ID == keyRow.mRowId
                || keyRow.mRowId == mEnabledRowId
            ) {
                return keyRow
            }
        }
        return null
    }

    fun getKey(row: Int, location: Int): SoftKey? {
        if (null != mKeyRows && mKeyRows!!.size > row) {
            val softKeys: List<SoftKey>? = mKeyRows!![row].mSoftKeys
            if (softKeys!!.size > location) {
                return softKeys[location]
            }
        }
        return null
    }

    fun mapToKey(x: Int, y: Int): SoftKey? {
        if (null == mKeyRows) {
            return null
        }
        // If the position is inside the rectangle of a certain key, return that
        // key.
        val rowNum = mKeyRows!!.size
        for (row in 0 until rowNum) {
            val keyRow = mKeyRows!![row]
            if (KeyRow.ALWAYS_SHOW_ROW_ID != keyRow.mRowId
                && keyRow.mRowId != mEnabledRowId
            ) continue
            if (keyRow.mTop > y && keyRow.mBottom <= y) continue

            val softKeys: List<SoftKey>? = keyRow.mSoftKeys
            val keyNum = softKeys!!.size
            for (i in 0 until keyNum) {
                val sKey = softKeys[i]
                if (sKey.mLeft <= x && sKey.mTop <= y && sKey.mRight > x && sKey.mBottom > y) {
                    return sKey
                }
            }
        }

        // If the position is outside the rectangles of all keys, find the
        // nearest one.
        var nearestKey: SoftKey? = null
        var nearestDis = Float.MAX_VALUE
        for (row in 0 until rowNum) {
            val keyRow = mKeyRows!![row]
            if (KeyRow.ALWAYS_SHOW_ROW_ID != keyRow.mRowId
                && keyRow.mRowId != mEnabledRowId
            ) continue
            if (keyRow.mTop > y && keyRow.mBottom <= y) continue

            val softKeys: List<SoftKey>? = keyRow.mSoftKeys
            val keyNum = softKeys!!.size
            for (i in 0 until keyNum) {
                val sKey = softKeys[i]
                val disx = (sKey.mLeft + sKey.mRight) / 2 - x
                val disy = (sKey.mTop + sKey.mBottom) / 2 - y
                val dis = (disx * disx + disy * disy).toFloat()
                if (dis < nearestDis) {
                    nearestDis = dis
                    nearestKey = sKey
                }
            }
        }
        return nearestKey
    }

    fun switchQwertyMode(toggle_state_id: Int, upperCase: Boolean) {
        if (!mIsQwerty) return

        val rowNum = mKeyRows!!.size
        for (row in 0 until rowNum) {
            val keyRow = mKeyRows!![row]
            val softKeys: List<SoftKey>? = keyRow.mSoftKeys
            val keyNum = softKeys!!.size
            for (i in 0 until keyNum) {
                val sKey = softKeys[i]
                if (sKey is SoftKeyToggle) {
                    sKey.enableToggleState(
                        toggle_state_id,
                        true
                    )
                }
                if (sKey.keyCode >= KeyEvent.KEYCODE_A
                    && sKey.keyCode <= KeyEvent.KEYCODE_Z
                ) {
                    sKey.changeCase(upperCase)
                }
            }
        }
    }

    fun enableToggleState(toggleStateId: Int, resetIfNotFound: Boolean) {
        val rowNum = mKeyRows!!.size
        for (row in 0 until rowNum) {
            val keyRow = mKeyRows!![row]
            val softKeys: List<SoftKey>? = keyRow.mSoftKeys
            val keyNum = softKeys!!.size
            for (i in 0 until keyNum) {
                val sKey = softKeys[i]
                if (sKey is SoftKeyToggle) {
                    sKey.enableToggleState(
                        toggleStateId,
                        resetIfNotFound
                    )
                }
            }
        }
    }

    fun disableToggleState(toggleStateId: Int, resetIfNotFound: Boolean) {
        val rowNum = mKeyRows!!.size
        for (row in 0 until rowNum) {
            val keyRow = mKeyRows!![row]
            val softKeys: List<SoftKey>? = keyRow.mSoftKeys
            val keyNum = softKeys!!.size
            for (i in 0 until keyNum) {
                val sKey = softKeys[i]
                if (sKey is SoftKeyToggle) {
                    sKey.disableToggleState(
                        toggleStateId,
                        resetIfNotFound
                    )
                }
            }
        }
    }

    fun enableToggleStates(toggleStates: ToggleStates?) {
        if (null == toggleStates) return

        enableRow(toggleStates.mRowIdToEnable)

        val isQwerty = toggleStates.mQwerty
        val isQwertyUpperCase = toggleStates.mQwertyUpperCase
        val needUpdateQwerty = (isQwerty && mIsQwerty && (mIsQwertyUpperCase != isQwertyUpperCase))
        val states = toggleStates.mKeyStates
        val statesNum = toggleStates.mKeyStatesNum

        val rowNum = mKeyRows!!.size
        for (row in 0 until rowNum) {
            val keyRow = mKeyRows!![row]
            if (KeyRow.ALWAYS_SHOW_ROW_ID != keyRow.mRowId
                && keyRow.mRowId != mEnabledRowId
            ) {
                continue
            }
            val softKeys: List<SoftKey>? = keyRow.mSoftKeys
            val keyNum = softKeys!!.size
            for (keyPos in 0 until keyNum) {
                val sKey = softKeys[keyPos]
                if (sKey is SoftKeyToggle) {
                    for (statePos in 0 until statesNum) {
                        sKey.enableToggleState(
                            states!![statePos], statePos == 0
                        )
                    }
                    if (0 == statesNum) {
                        sKey.disableAllToggleStates()
                    }
                }
                if (needUpdateQwerty) {
                    if (sKey.keyCode >= KeyEvent.KEYCODE_A
                        && sKey.keyCode <= KeyEvent.KEYCODE_Z
                    ) {
                        sKey.changeCase(isQwertyUpperCase)
                    }
                }
            }
        }
        mIsQwertyUpperCase = isQwertyUpperCase
    }

    private val padding: Rect
        get() {
            mTmpRect[0, 0, 0] = 0
            val skbBg = skbBackground ?: return mTmpRect
            skbBg.getPadding(mTmpRect)
            return mTmpRect
        }

    /**
     * Enable a row with the give toggle Id. Rows with other toggle ids (except
     * the id [KeyRow.ALWAYS_SHOW_ROW_ID]) will be disabled.
     *
     * @param rowId The row id to enable.
     * @return True if the soft keyboard requires redrawing.
     */
    private fun enableRow(rowId: Int): Boolean {
        if (KeyRow.ALWAYS_SHOW_ROW_ID == rowId) return false

        var enabled = false
        val rowNum = mKeyRows!!.size
        for (row in rowNum - 1 downTo 0) {
            if (mKeyRows!![row].mRowId == rowId) {
                enabled = true
                break
            }
        }
        if (enabled) {
            mEnabledRowId = rowId
        }
        return enabled
    }

    override fun toString(): String {
        var str = "------------------SkbInfo----------------------\n"
        val endStr = "-----------------------------------------------\n"
        str += "Width: $skbCoreWidth\n"
        str += "Height: $skbCoreHeight\n"
        str += "KeyRowNum: " + (if (mKeyRows == null) "0" else (mKeyRows!!.size.toString() + "\n"))
        if (null == mKeyRows) return str + endStr
        val rowNum = mKeyRows!!.size
        for (row in 0 until rowNum) {
            val keyRow = mKeyRows!![row]
            val softKeys: List<SoftKey>? = keyRow.mSoftKeys
            val keyNum = softKeys!!.size
            for (i in softKeys.indices) {
                str += ("-key " + i.toString() + ":"
                        + softKeys[i].toString())
            }
        }
        return str + endStr
    }

    fun toShortString(): String {
        return super.toString()
    }

    class KeyRow {
        var mSoftKeys: MutableList<SoftKey>? = null

        /**
         * If the row id is [.ALWAYS_SHOW_ROW_ID], this row will always be
         * enabled.
         */
        var mRowId: Int = 0
        var mTopF: Float = 0f
        var mBottomF: Float = 0f
        var mTop: Int = 0
        var mBottom: Int = 0

        companion object {
            const val ALWAYS_SHOW_ROW_ID: Int = -1
            const val DEFAULT_ROW_ID: Int = 0
        }
    }
}
