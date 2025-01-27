/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.inputmethod.latin.utils

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.Window
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout

object ViewLayoutUtils {
    fun newLayoutParam(
        placer: ViewGroup, width: Int,
        height: Int
    ): MarginLayoutParams {
        return if (placer is FrameLayout) {
            FrameLayout.LayoutParams(width, height)
        } else if (placer is RelativeLayout) {
            RelativeLayout.LayoutParams(width, height)
        } else if (placer == null) {
            throw NullPointerException("placer is null")
        } else {
            throw IllegalArgumentException(
                "placer is neither FrameLayout nor RelativeLayout: "
                        + placer.javaClass.name
            )
        }
    }

    fun placeViewAt(
        view: View, x: Int, y: Int, w: Int,
        h: Int
    ) {
        val lp = view.layoutParams
        if (lp is MarginLayoutParams) {
            val marginLayoutParams = lp
            marginLayoutParams.width = w
            marginLayoutParams.height = h
            marginLayoutParams.setMargins(x, y, 0, 0)
        }
    }

    fun updateLayoutHeightOf(window: Window, layoutHeight: Int) {
        val params = window.attributes
        if (params != null && params.height != layoutHeight) {
            params.height = layoutHeight
            window.attributes = params
        }
    }

    fun updateLayoutHeightOf(view: View, layoutHeight: Int) {
        val params = view.layoutParams
        if (params != null && params.height != layoutHeight) {
            params.height = layoutHeight
            view.layoutParams = params
        }
    }

    fun updateLayoutGravityOf(view: View, layoutGravity: Int) {
        val lp = view.layoutParams
        if (lp is LinearLayout.LayoutParams) {
            val params = lp
            if (params.gravity != layoutGravity) {
                params.gravity = layoutGravity
                view.layoutParams = params
            }
        } else if (lp is FrameLayout.LayoutParams) {
            val params = lp
            if (params.gravity != layoutGravity) {
                params.gravity = layoutGravity
                view.layoutParams = params
            }
        } else {
            throw IllegalArgumentException(
                "Layout parameter doesn't have gravity: "
                        + lp.javaClass.name
            )
        }
    }
}
