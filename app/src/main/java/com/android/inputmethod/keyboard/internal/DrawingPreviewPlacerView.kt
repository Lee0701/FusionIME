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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.android.inputmethod.latin.common.CoordinateUtils

class DrawingPreviewPlacerView(context: Context?, attrs: AttributeSet?) :
    RelativeLayout(context, attrs) {
    private val mKeyboardViewOrigin: IntArray = CoordinateUtils.newInstance()

    private val mPreviews: ArrayList<AbstractDrawingPreview> = ArrayList()

    init {
        setWillNotDraw(false)
    }

    fun setHardwareAcceleratedDrawingEnabled(enabled: Boolean) {
        if (!enabled) return
        val layerPaint: Paint = Paint()
        layerPaint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_OVER))
        setLayerType(LAYER_TYPE_HARDWARE, layerPaint)
    }

    fun addPreview(preview: AbstractDrawingPreview) {
        if (mPreviews.indexOf(preview) < 0) {
            mPreviews.add(preview)
        }
    }

    fun setKeyboardViewGeometry(
        originCoords: IntArray, width: Int,
        height: Int
    ) {
        CoordinateUtils.copy(mKeyboardViewOrigin, originCoords)
        val count: Int = mPreviews.size
        for (i in 0 until count) {
            mPreviews.get(i).setKeyboardViewGeometry(originCoords, width, height)
        }
    }

    fun deallocateMemory() {
        val count: Int = mPreviews.size
        for (i in 0 until count) {
            mPreviews.get(i).onDeallocateMemory()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        deallocateMemory()
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val originX: Int = CoordinateUtils.x(mKeyboardViewOrigin)
        val originY: Int = CoordinateUtils.y(mKeyboardViewOrigin)
        canvas.translate(originX.toFloat(), originY.toFloat())
        val count: Int = mPreviews.size
        for (i in 0 until count) {
            mPreviews.get(i).drawPreview(canvas)
        }
        canvas.translate(-originX.toFloat(), -originY.toFloat())
    }
}
