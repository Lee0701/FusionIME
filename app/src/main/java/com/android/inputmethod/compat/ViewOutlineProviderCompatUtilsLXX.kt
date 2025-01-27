/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.inputmethod.compat

import android.annotation.TargetApi
import android.graphics.Outline
import android.inputmethodservice.InputMethodService
import android.os.Build.VERSION_CODES
import android.view.View
import android.view.ViewOutlineProvider
import com.android.inputmethod.compat.ViewOutlineProviderCompatUtils.InsetsUpdater

@TargetApi(VERSION_CODES.LOLLIPOP)
internal object ViewOutlineProviderCompatUtilsLXX {
    fun setInsetsOutlineProvider(view: View): InsetsUpdater {
        val provider: InsetsOutlineProvider = InsetsOutlineProvider(view)
        view.setOutlineProvider(provider)
        return provider
    }

    private class InsetsOutlineProvider(view: View) : ViewOutlineProvider(),
        InsetsUpdater {
        private val mView: View
        private var mLastVisibleTopInsets: Int = NO_DATA

        init {
            mView = view
            view.setOutlineProvider(this)
        }

        override fun setInsets(insets: InputMethodService.Insets) {
            val visibleTopInsets: Int = insets.visibleTopInsets
            if (mLastVisibleTopInsets != visibleTopInsets) {
                mLastVisibleTopInsets = visibleTopInsets
                mView.invalidateOutline()
            }
        }

        override fun getOutline(view: View, outline: Outline) {
            if (mLastVisibleTopInsets == NO_DATA) {
                // Call default implementation.
                BACKGROUND.getOutline(view, outline)
                return
            }
            // TODO: Revisit this when floating/resize keyboard is supported.
            outline.setRect(
                view.getLeft(), mLastVisibleTopInsets, view.getRight(), view.getBottom()
            )
        }

        companion object {
            private val NO_DATA: Int = -1
        }
    }
}
