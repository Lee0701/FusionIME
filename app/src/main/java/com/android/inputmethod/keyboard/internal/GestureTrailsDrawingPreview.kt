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
package com.android.inputmethod.keyboard.internal

import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.os.Handler
import android.util.SparseArray
import com.android.inputmethod.keyboard.PointerTracker

/**
 * Draw preview graphics of multiple gesture trails during gesture input.
 */
class GestureTrailsDrawingPreview(mainKeyboardViewAttr: TypedArray) : AbstractDrawingPreview(),
    Runnable {
    private val mGestureTrails: SparseArray<GestureTrailDrawingPoints> = SparseArray()
    private val mDrawingParams: GestureTrailDrawingParams
    private val mGesturePaint: Paint
    private var mOffscreenWidth: Int = 0
    private var mOffscreenHeight: Int = 0
    private var mOffscreenOffsetY: Int = 0
    private var mOffscreenBuffer: Bitmap? = null
    private val mOffscreenCanvas: Canvas = Canvas()
    private val mOffscreenSrcRect: Rect = Rect()
    private val mDirtyRect: Rect = Rect()
    private val mGestureTrailBoundsRect: Rect = Rect() // per trail

    private val mDrawingHandler: Handler = Handler()

    init {
        mDrawingParams = GestureTrailDrawingParams(mainKeyboardViewAttr)
        val gesturePaint: Paint = Paint()
        gesturePaint.setAntiAlias(true)
        gesturePaint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC))
        mGesturePaint = gesturePaint
    }

    override fun setKeyboardViewGeometry(
        originCoords: IntArray, width: Int,
        height: Int
    ) {
        super.setKeyboardViewGeometry(originCoords, width, height)
        mOffscreenOffsetY = (height
                * GestureStrokeRecognitionPoints.Companion.EXTRA_GESTURE_TRAIL_AREA_ABOVE_KEYBOARD_RATIO).toInt()
        mOffscreenWidth = width
        mOffscreenHeight = mOffscreenOffsetY + height
    }

    override fun onDeallocateMemory() {
        freeOffscreenBuffer()
    }

    private fun freeOffscreenBuffer() {
        mOffscreenCanvas.setBitmap(null)
        mOffscreenCanvas.setMatrix(null)
        if (mOffscreenBuffer != null) {
            mOffscreenBuffer!!.recycle()
            mOffscreenBuffer = null
        }
    }

    private fun mayAllocateOffscreenBuffer() {
        if (mOffscreenBuffer != null && mOffscreenBuffer!!.getWidth() == mOffscreenWidth && mOffscreenBuffer!!.getHeight() == mOffscreenHeight) {
            return
        }
        freeOffscreenBuffer()
        mOffscreenBuffer = Bitmap.createBitmap(
            mOffscreenWidth, mOffscreenHeight, Bitmap.Config.ARGB_8888
        )
        mOffscreenCanvas.setBitmap(mOffscreenBuffer)
        mOffscreenCanvas.translate(0f, mOffscreenOffsetY.toFloat())
    }

    private fun drawGestureTrails(
        offscreenCanvas: Canvas, paint: Paint,
        dirtyRect: Rect
    ): Boolean {
        // Clear previous dirty rectangle.
        if (!dirtyRect.isEmpty()) {
            paint.setColor(Color.TRANSPARENT)
            paint.setStyle(Paint.Style.FILL)
            offscreenCanvas.drawRect(dirtyRect, paint)
        }
        dirtyRect.setEmpty()
        var needsUpdatingGestureTrail: Boolean = false
        // Draw gesture trails to offscreen buffer.
        synchronized(mGestureTrails) {
            // Trails count == fingers count that have ever been active.
            val trailsCount: Int = mGestureTrails.size()
            for (index in 0 until trailsCount) {
                val trail: GestureTrailDrawingPoints = mGestureTrails.valueAt(index)
                needsUpdatingGestureTrail = needsUpdatingGestureTrail or trail.drawGestureTrail(
                    offscreenCanvas, paint,
                    mGestureTrailBoundsRect, mDrawingParams
                )
                // {@link #mGestureTrailBoundsRect} has bounding box of the trail.
                dirtyRect.union(mGestureTrailBoundsRect)
            }
        }
        return needsUpdatingGestureTrail
    }

    override fun run() {
        // Update preview.
        invalidateDrawingView()
    }

    /**
     * Draws the preview
     * @param canvas The canvas where the preview is drawn.
     */
    override fun drawPreview(canvas: Canvas) {
        if (!isPreviewEnabled()) {
            return
        }
        mayAllocateOffscreenBuffer()
        // Draw gesture trails to offscreen buffer.
        val needsUpdatingGestureTrail: Boolean = drawGestureTrails(
            mOffscreenCanvas, mGesturePaint, mDirtyRect
        )
        if (needsUpdatingGestureTrail) {
            mDrawingHandler.removeCallbacks(this)
            mDrawingHandler.postDelayed(this, mDrawingParams.mUpdateInterval.toLong())
        }
        // Transfer offscreen buffer to screen.
        if (!mDirtyRect.isEmpty()) {
            mOffscreenSrcRect.set(mDirtyRect)
            mOffscreenSrcRect.offset(0, mOffscreenOffsetY)
            canvas.drawBitmap(mOffscreenBuffer!!, mOffscreenSrcRect, mDirtyRect, null)
            // Note: Defer clearing the dirty rectangle here because we will get cleared
            // rectangle on the canvas.
        }
    }

    /**
     * Set the position of the preview.
     * @param tracker The new location of the preview is based on the points in PointerTracker.
     */
    override fun setPreviewPosition(tracker: PointerTracker) {
        if (!isPreviewEnabled()) {
            return
        }
        var trail: GestureTrailDrawingPoints?
        synchronized(mGestureTrails) {
            trail = mGestureTrails.get(tracker.mPointerId)
            if (trail == null) {
                trail = GestureTrailDrawingPoints()
                mGestureTrails.put(tracker.mPointerId, trail)
            }
        }
        trail!!.addStroke(tracker.getGestureStrokeDrawingPoints(), tracker.getDownTime())

        // TODO: Should narrow the invalidate region.
        invalidateDrawingView()
    }
}
