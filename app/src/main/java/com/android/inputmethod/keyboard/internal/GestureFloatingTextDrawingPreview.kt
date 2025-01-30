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
package com.android.inputmethod.keyboard.internal

import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextUtils
import com.android.inputmethod.keyboard.PointerTracker
import ee.oyatl.ime.fusion.R
import com.android.inputmethod.latin.SuggestedWords
import com.android.inputmethod.latin.common.CoordinateUtils
import kotlin.math.max
import kotlin.math.min

/**
 * The class for single gesture preview text. The class for multiple gesture preview text will be
 * derived from it.
 *
 * @attr ref android.R.styleable#KeyboardView_gestureFloatingPreviewTextSize
 * @attr ref android.R.styleable#KeyboardView_gestureFloatingPreviewTextColor
 * @attr ref android.R.styleable#KeyboardView_gestureFloatingPreviewTextOffset
 * @attr ref android.R.styleable#KeyboardView_gestureFloatingPreviewColor
 * @attr ref android.R.styleable#KeyboardView_gestureFloatingPreviewHorizontalPadding
 * @attr ref android.R.styleable#KeyboardView_gestureFloatingPreviewVerticalPadding
 * @attr ref android.R.styleable#KeyboardView_gestureFloatingPreviewRoundRadius
 */
class GestureFloatingTextDrawingPreview(mainKeyboardViewAttr: TypedArray) :
    AbstractDrawingPreview() {
    protected class GesturePreviewTextParams(mainKeyboardViewAttr: TypedArray) {
        val mGesturePreviewTextOffset: Int
        val mGesturePreviewTextHeight: Int
        val mGesturePreviewHorizontalPadding: Float
        val mGesturePreviewVerticalPadding: Float
        val mGesturePreviewRoundRadius: Float
        val mDisplayWidth: Int

        private val mGesturePreviewTextSize: Int
        private val mGesturePreviewTextColor: Int
        private val mGesturePreviewColor: Int
        private val mPaint: Paint = Paint()

        init {
            mGesturePreviewTextSize = mainKeyboardViewAttr.getDimensionPixelSize(
                R.styleable.MainKeyboardView_gestureFloatingPreviewTextSize, 0
            )
            mGesturePreviewTextColor = mainKeyboardViewAttr.getColor(
                R.styleable.MainKeyboardView_gestureFloatingPreviewTextColor, 0
            )
            mGesturePreviewTextOffset = mainKeyboardViewAttr.getDimensionPixelOffset(
                R.styleable.MainKeyboardView_gestureFloatingPreviewTextOffset, 0
            )
            mGesturePreviewColor = mainKeyboardViewAttr.getColor(
                R.styleable.MainKeyboardView_gestureFloatingPreviewColor, 0
            )
            mGesturePreviewHorizontalPadding = mainKeyboardViewAttr.getDimension(
                R.styleable.MainKeyboardView_gestureFloatingPreviewHorizontalPadding, 0.0f
            )
            mGesturePreviewVerticalPadding = mainKeyboardViewAttr.getDimension(
                R.styleable.MainKeyboardView_gestureFloatingPreviewVerticalPadding, 0.0f
            )
            mGesturePreviewRoundRadius = mainKeyboardViewAttr.getDimension(
                R.styleable.MainKeyboardView_gestureFloatingPreviewRoundRadius, 0.0f
            )
            mDisplayWidth = mainKeyboardViewAttr.getResources().getDisplayMetrics().widthPixels

            val textPaint: Paint = getTextPaint()
            val textRect: Rect = Rect()
            textPaint.getTextBounds(TEXT_HEIGHT_REFERENCE_CHAR, 0, 1, textRect)
            mGesturePreviewTextHeight = textRect.height()
        }

        fun getTextPaint(): Paint {
            mPaint.setAntiAlias(true)
            mPaint.setTextAlign(Align.CENTER)
            mPaint.setTextSize(mGesturePreviewTextSize.toFloat())
            mPaint.setColor(mGesturePreviewTextColor)
            return mPaint
        }

        fun getBackgroundPaint(): Paint {
            mPaint.setColor(mGesturePreviewColor)
            return mPaint
        }

        companion object {
            private val TEXT_HEIGHT_REFERENCE_CHAR: CharArray = charArrayOf('M')
        }
    }

    private val mParams: GesturePreviewTextParams
    private val mGesturePreviewRectangle: RectF = RectF()
    private var mPreviewTextX: Int = 0
    private var mPreviewTextY: Int = 0
    private var mSuggestedWords: SuggestedWords = SuggestedWords.emptyInstance
    private val mLastPointerCoords: IntArray = CoordinateUtils.newInstance()

    init {
        mParams = GesturePreviewTextParams(mainKeyboardViewAttr)
    }

    override fun onDeallocateMemory() {
        // Nothing to do here.
    }

    fun dismissGestureFloatingPreviewText() {
        setSuggetedWords(SuggestedWords.emptyInstance)
    }

    fun setSuggetedWords(suggestedWords: SuggestedWords) {
        if (!isPreviewEnabled()) {
            return
        }
        mSuggestedWords = suggestedWords
        updatePreviewPosition()
    }

    override fun setPreviewPosition(tracker: PointerTracker) {
        if (!isPreviewEnabled()) {
            return
        }
        tracker.getLastCoordinates(mLastPointerCoords)
        updatePreviewPosition()
    }

    /**
     * Draws gesture preview text
     * @param canvas The canvas where preview text is drawn.
     */
    override fun drawPreview(canvas: Canvas) {
        if (!isPreviewEnabled() || mSuggestedWords.isEmpty
            || TextUtils.isEmpty(mSuggestedWords.getWord(0))
        ) {
            return
        }
        val round: Float = mParams.mGesturePreviewRoundRadius
        canvas.drawRoundRect(
            mGesturePreviewRectangle, round, round, mParams.getBackgroundPaint()
        )
        val text: String? = mSuggestedWords.getWord(0)
        canvas.drawText(
            text!!,
            mPreviewTextX.toFloat(),
            mPreviewTextY.toFloat(),
            mParams.getTextPaint()
        )
    }

    /**
     * Updates gesture preview text position based on mLastPointerCoords.
     */
    protected fun updatePreviewPosition() {
        if (mSuggestedWords.isEmpty || TextUtils.isEmpty(mSuggestedWords.getWord(0))) {
            invalidateDrawingView()
            return
        }
        val text: String? = mSuggestedWords.getWord(0)

        val rectangle: RectF = mGesturePreviewRectangle

        val textHeight: Int = mParams.mGesturePreviewTextHeight
        val textWidth: Float = mParams.getTextPaint().measureText(text)
        val hPad: Float = mParams.mGesturePreviewHorizontalPadding
        val vPad: Float = mParams.mGesturePreviewVerticalPadding
        val rectWidth: Float = textWidth + hPad * 2.0f
        val rectHeight: Float = textHeight + vPad * 2.0f

        val rectX: Float = min(
            max(
                (CoordinateUtils.x(mLastPointerCoords) - rectWidth / 2.0f).toDouble(),
                0.0
            ),
            (mParams.mDisplayWidth - rectWidth).toDouble()
        ).toFloat()
        val rectY: Float = (CoordinateUtils.y(mLastPointerCoords)
                - mParams.mGesturePreviewTextOffset - rectHeight)
        rectangle.set(rectX, rectY, rectX + rectWidth, rectY + rectHeight)

        mPreviewTextX = (rectX + hPad + textWidth / 2.0f).toInt()
        mPreviewTextY = (rectY + vPad).toInt() + textHeight
        // TODO: Should narrow the invalidate region.
        invalidateDrawingView()
    }
}
