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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import com.android.inputmethod.pinyin.PinyinIME.DecodingInfo
import com.android.inputmethod.pinyin.PinyinIME.ImeState
import ee.oyatl.ime.fusion.R

/**
 * View used to show composing string (The Pinyin string for the unselected
 * syllables and the Chinese string for the selected syllables.)
 */
class ComposingView(context: Context, attrs: AttributeSet?) :
    View(context, attrs) {
    /**
     *
     *
     * There are three statuses for the composing view.
     *
     *
     *
     *
     * [.SHOW_PINYIN] is used to show the current Pinyin string without
     * highlighted effect. When user inputs Pinyin characters one by one, the
     * Pinyin string will be shown in this mode.
     *
     *
     *
     * [.SHOW_STRING_LOWERCASE] is used to show the Pinyin string in
     * lowercase with highlighted effect. When user presses UP key and there is
     * no fixed Chinese characters, composing view will switch from
     * [.SHOW_PINYIN] to this mode, and in this mode, user can press
     * confirm key to input the lower-case string, so that user can input
     * English letter in Chinese mode.
     *
     *
     *
     * [.EDIT_PINYIN] is used to edit the Pinyin string (shown with
     * highlighted effect). When current status is [.SHOW_PINYIN] and user
     * presses UP key, if there are fixed Characters, the input method will
     * switch to [.EDIT_PINYIN] thus user can modify some characters in
     * the middle of the Pinyin string. If the current status is
     * [.SHOW_STRING_LOWERCASE] and user presses LEFT and RIGHT key, it
     * will also switch to [.EDIT_PINYIN].
     *
     *
     *
     * Whenever user presses down key, the status switches to
     * [.SHOW_PINYIN].
     *
     *
     *
     * When composing view's status is [.SHOW_PINYIN], the IME's status is
     * [PinyinIME.ImeState.STATE_INPUT], otherwise, the IME's status
     * should be [PinyinIME.ImeState.STATE_COMPOSING].
     *
     */
    enum class ComposingStatus {
        SHOW_PINYIN, SHOW_STRING_LOWERCASE, EDIT_PINYIN,
    }

    /**
     * Used to draw composing string. When drawing the active and idle part of
     * the spelling(Pinyin) string, the color may be changed.
     */
    private val mPaint: Paint

    /**
     * Drawable used to draw highlight effect.
     */
    private val mHlDrawable: Drawable

    /**
     * Drawable used to draw cursor for editing mode.
     */
    private val mCursor: Drawable

    /**
     * Used to estimate dimensions to show the string .
     */
    private val mFmi: FontMetricsInt

    private val mStrColor: Int
    private val mStrColorHl: Int
    private val mStrColorIdle: Int

    private val mFontSize: Int

    var composingStatus: ComposingStatus? = null
        private set

    var mDecInfo: DecodingInfo? = null

    init {
        val r = context.resources
        mHlDrawable = r.getDrawable(R.drawable.composing_hl_bg)
        mCursor = r.getDrawable(R.drawable.composing_area_cursor)

        mStrColor = r.getColor(R.color.composing_color)
        mStrColorHl = r.getColor(R.color.composing_color_hl)
        mStrColorIdle = r.getColor(R.color.composing_color_idle)

        mFontSize = r.getDimensionPixelSize(R.dimen.composing_height)

        mPaint = Paint()
        mPaint.color = mStrColor
        mPaint.isAntiAlias = true
        mPaint.textSize = mFontSize.toFloat()

        mFmi = mPaint.fontMetricsInt
    }

    fun reset() {
        composingStatus = ComposingStatus.SHOW_PINYIN
    }

    /**
     * Set the composing string to show. If the IME status is
     * [PinyinIME.ImeState.STATE_INPUT], the composing view's status will
     * be set to [ComposingStatus.SHOW_PINYIN], otherwise the composing
     * view will set its status to [ComposingStatus.SHOW_STRING_LOWERCASE]
     * or [ComposingStatus.EDIT_PINYIN] automatically.
     */
    fun setDecodingInfo(
        decInfo: DecodingInfo,
        imeStatus: ImeState
    ) {
        mDecInfo = decInfo

        if (ImeState.STATE_INPUT == imeStatus) {
            composingStatus = ComposingStatus.SHOW_PINYIN
            mDecInfo!!.moveCursorToEdge(false)
        } else {
            if (decInfo.fixedLen != 0
                || ComposingStatus.EDIT_PINYIN == composingStatus
            ) {
                composingStatus = ComposingStatus.EDIT_PINYIN
            } else {
                composingStatus = ComposingStatus.SHOW_STRING_LOWERCASE
            }
            mDecInfo!!.moveCursor(0)
        }

        measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        requestLayout()
        invalidate()
    }

    fun moveCursor(keyCode: Int): Boolean {
        if (keyCode != KeyEvent.KEYCODE_DPAD_LEFT
            && keyCode != KeyEvent.KEYCODE_DPAD_RIGHT
        ) return false

        if (ComposingStatus.EDIT_PINYIN == composingStatus) {
            var offset = 0
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) offset = -1
            else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) offset = 1
            mDecInfo!!.moveCursor(offset)
        } else if (ComposingStatus.SHOW_STRING_LOWERCASE == composingStatus) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
            ) {
                composingStatus = ComposingStatus.EDIT_PINYIN

                measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                requestLayout()
            }
        }
        invalidate()
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width: Float
        val height = mFmi.bottom - mFmi.top + paddingTop + paddingBottom

        if (null == mDecInfo) {
            width = 0f
        } else {
            width = (paddingLeft + paddingRight + LEFT_RIGHT_MARGIN * 2).toFloat()
            val str = if (ComposingStatus.SHOW_STRING_LOWERCASE == composingStatus) {
                mDecInfo!!.originalSplStr.toString()
            } else {
                mDecInfo!!.composingStrForDisplay
            }
            width += mPaint.measureText(str, 0, str!!.length)
        }
        setMeasuredDimension((width + 0.5f).toInt(), height)
    }

    override fun onDraw(canvas: Canvas) {
        if (ComposingStatus.EDIT_PINYIN == composingStatus
            || ComposingStatus.SHOW_PINYIN == composingStatus
        ) {
            drawForPinyin(canvas)
            return
        }
        val x = (paddingLeft + LEFT_RIGHT_MARGIN).toFloat()
        val y = (-mFmi.top + paddingTop).toFloat()

        mPaint.color = mStrColorHl
        mHlDrawable.setBounds(
            paddingLeft, paddingTop, width
                    - paddingRight, height - paddingBottom
        )
        mHlDrawable.draw(canvas)

        val splStr = mDecInfo!!.originalSplStr.toString()
        canvas.drawText(splStr, 0, splStr.length, x, y, mPaint)
    }

    private fun drawCursor(canvas: Canvas, x: Float) {
        mCursor.setBounds(
            x.toInt(),
            paddingTop,
            x.toInt() + mCursor.intrinsicWidth,
            height - paddingBottom
        )
        mCursor.draw(canvas)
    }

    private fun drawForPinyin(canvas: Canvas) {
        var x = (paddingLeft + LEFT_RIGHT_MARGIN).toFloat()
        val y = (-mFmi.top + paddingTop).toFloat()

        mPaint.color = mStrColor

        var cursorPos = mDecInfo!!.cursorPosInCmpsDisplay
        var cmpsPos = cursorPos
        val cmpsStr = mDecInfo!!.composingStrForDisplay
        val activeCmpsLen = mDecInfo!!.activeCmpsDisplayLen
        if (cursorPos > activeCmpsLen) cmpsPos = activeCmpsLen
        canvas.drawText(cmpsStr!!, 0, cmpsPos, x, y, mPaint)
        x += mPaint.measureText(cmpsStr, 0, cmpsPos)
        if (cursorPos <= activeCmpsLen) {
            if (ComposingStatus.EDIT_PINYIN == composingStatus) {
                drawCursor(canvas, x)
            }
            canvas.drawText(cmpsStr, cmpsPos, activeCmpsLen, x, y, mPaint)
        }

        x += mPaint.measureText(cmpsStr, cmpsPos, activeCmpsLen)

        if (cmpsStr.length > activeCmpsLen) {
            mPaint.color = mStrColorIdle
            var oriPos = activeCmpsLen
            if (cursorPos > activeCmpsLen) {
                if (cursorPos > cmpsStr.length) cursorPos = cmpsStr.length
                canvas.drawText(cmpsStr, oriPos, cursorPos, x, y, mPaint)
                x += mPaint.measureText(cmpsStr, oriPos, cursorPos)

                if (ComposingStatus.EDIT_PINYIN == composingStatus) {
                    drawCursor(canvas, x)
                }

                oriPos = cursorPos
            }
            canvas.drawText(cmpsStr, oriPos, cmpsStr.length, x, y, mPaint)
        }
    }

    companion object {
        private const val LEFT_RIGHT_MARGIN = 5
    }
}
