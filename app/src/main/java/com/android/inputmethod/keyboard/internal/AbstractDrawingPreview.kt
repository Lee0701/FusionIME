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

import android.graphics.Canvas
import android.view.View
import com.android.inputmethod.keyboard.MainKeyboardView
import com.android.inputmethod.keyboard.PointerTracker

/**
 * Abstract base class for previews that are drawn on DrawingPreviewPlacerView, e.g.,
 * GestureFloatingTextDrawingPreview, GestureTrailsDrawingPreview, and
 * SlidingKeyInputDrawingPreview.
 */
abstract class AbstractDrawingPreview {
    private var mDrawingView: View? = null
    private var mPreviewEnabled: Boolean = false
    private var mHasValidGeometry: Boolean = false

    fun setDrawingView(drawingView: DrawingPreviewPlacerView) {
        mDrawingView = drawingView
        drawingView.addPreview(this)
    }

    protected fun invalidateDrawingView() {
        if (mDrawingView != null) {
            mDrawingView!!.invalidate()
        }
    }

    protected fun isPreviewEnabled(): Boolean {
        return mPreviewEnabled && mHasValidGeometry
    }

    fun setPreviewEnabled(enabled: Boolean) {
        mPreviewEnabled = enabled
    }

    /**
     * Set [MainKeyboardView] geometry and position in the window of input method.
     * The class that is overriding this method must call this super implementation.
     *
     * @param originCoords the top-left coordinates of the [MainKeyboardView] in
     * the input method window coordinate-system. This is unused but has a point in an
     * extended class, such as [GestureTrailsDrawingPreview].
     * @param width the width of [MainKeyboardView].
     * @param height the height of [MainKeyboardView].
     */
    open fun setKeyboardViewGeometry(
        originCoords: IntArray, width: Int,
        height: Int
    ) {
        mHasValidGeometry = (width > 0 && height > 0)
    }

    abstract fun onDeallocateMemory()

    /**
     * Draws the preview
     * @param canvas The canvas where the preview is drawn.
     */
    abstract fun drawPreview(canvas: Canvas)

    /**
     * Set the position of the preview.
     * @param tracker The new location of the preview is based on the points in PointerTracker.
     */
    abstract fun setPreviewPosition(tracker: PointerTracker)
}
