/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.inputmethod.keyboard.emoji

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class EmojiCategoryPageIndicatorView @JvmOverloads constructor(
    context: Context?, attrs: AttributeSet?,
    defStyle: Int = 0
) :
    View(context, attrs, defStyle) {
    private val mPaint: Paint = Paint()
    private var mCategoryPageSize: Int = 0
    private var mCurrentCategoryPageId: Int = 0
    private var mOffset: Float = 0.0f

    fun setColors(foregroundColor: Int, backgroundColor: Int) {
        mPaint.setColor(foregroundColor)
        setBackgroundColor(backgroundColor)
    }

    fun setCategoryPageId(size: Int, id: Int, offset: Float) {
        mCategoryPageSize = size
        mCurrentCategoryPageId = id
        mOffset = offset
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (mCategoryPageSize <= 1) {
            // If the category is not set yet or contains only one category,
            // just clear and return.
            canvas.drawColor(0)
            return
        }
        val height: Float = getHeight().toFloat()
        val width: Float = getWidth().toFloat()
        val unitWidth: Float = width / mCategoryPageSize
        val left: Float = unitWidth * mCurrentCategoryPageId + mOffset * unitWidth
        val top: Float = 0.0f
        val right: Float = left + unitWidth
        val bottom: Float = height * BOTTOM_MARGIN_RATIO
        canvas.drawRect(left, top, right, bottom, mPaint)
    }

    companion object {
        private const val BOTTOM_MARGIN_RATIO: Float = 1.0f
    }
}
