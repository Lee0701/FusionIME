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
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Build.VERSION_CODES
import android.view.inputmethod.CursorAnchorInfo
import javax.annotation.Nonnull

/**
 * A wrapper for [CursorAnchorInfo], which has been introduced in API Level 21. You can use
 * this wrapper to avoid direct dependency on newly introduced types.
 */
open class CursorAnchorInfoCompatWrapper internal constructor() {
    open fun getSelectionStart(): Int {
        throw UnsupportedOperationException("not supported.")
    }

    open fun getSelectionEnd(): Int {
        throw UnsupportedOperationException("not supported.")
    }

    open fun getComposingText(): CharSequence? {
        throw UnsupportedOperationException("not supported.")
    }

    open fun getComposingTextStart(): Int {
        throw UnsupportedOperationException("not supported.")
    }

    open fun getMatrix(): Matrix? {
        throw UnsupportedOperationException("not supported.")
    }

    @Suppress("unused")
    open fun getCharacterBounds(index: Int): RectF? {
        throw UnsupportedOperationException("not supported.")
    }

    @Suppress("unused")
    open fun getCharacterBoundsFlags(index: Int): Int {
        throw UnsupportedOperationException("not supported.")
    }

    open fun getInsertionMarkerBaseline(): Float {
        throw UnsupportedOperationException("not supported.")
    }

    open fun getInsertionMarkerBottom(): Float {
        throw UnsupportedOperationException("not supported.")
    }

    open fun getInsertionMarkerHorizontal(): Float {
        throw UnsupportedOperationException("not supported.")
    }

    open fun getInsertionMarkerTop(): Float {
        throw UnsupportedOperationException("not supported.")
    }

    open fun getInsertionMarkerFlags(): Int {
        throw UnsupportedOperationException("not supported.")
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    private class RealWrapper(@Nonnull info: CursorAnchorInfo) :
        CursorAnchorInfoCompatWrapper() {
        @Nonnull
        private val mInstance: CursorAnchorInfo

        init {
            mInstance = info
        }

        override fun getSelectionStart(): Int {
            return mInstance.getSelectionStart()
        }

        override fun getSelectionEnd(): Int {
            return mInstance.getSelectionEnd()
        }

        override fun getComposingText(): CharSequence? {
            return mInstance.getComposingText()
        }

        override fun getComposingTextStart(): Int {
            return mInstance.getComposingTextStart()
        }

        override fun getMatrix(): Matrix? {
            return mInstance.getMatrix()
        }

        override fun getCharacterBounds(index: Int): RectF? {
            return mInstance.getCharacterBounds(index)
        }

        override fun getCharacterBoundsFlags(index: Int): Int {
            return mInstance.getCharacterBoundsFlags(index)
        }

        override fun getInsertionMarkerBaseline(): Float {
            return mInstance.getInsertionMarkerBaseline()
        }

        override fun getInsertionMarkerBottom(): Float {
            return mInstance.getInsertionMarkerBottom()
        }

        override fun getInsertionMarkerHorizontal(): Float {
            return mInstance.getInsertionMarkerHorizontal()
        }

        override fun getInsertionMarkerTop(): Float {
            return mInstance.getInsertionMarkerTop()
        }

        override fun getInsertionMarkerFlags(): Int {
            return mInstance.getInsertionMarkerFlags()
        }
    }

    companion object {
        /**
         * The insertion marker or character bounds have at least one visible region.
         */
        const val FLAG_HAS_VISIBLE_REGION: Int = 0x01

        /**
         * The insertion marker or character bounds have at least one invisible (clipped) region.
         */
        const val FLAG_HAS_INVISIBLE_REGION: Int = 0x02

        /**
         * The insertion marker or character bounds is placed at right-to-left (RTL) character.
         */
        const val FLAG_IS_RTL: Int = 0x04

        @TargetApi(VERSION_CODES.LOLLIPOP)
        fun wrap(instance: CursorAnchorInfo?): CursorAnchorInfoCompatWrapper? {
            if (BuildCompatUtils.EFFECTIVE_SDK_INT < VERSION_CODES.LOLLIPOP) {
                return null
            }
            if (instance == null) {
                return null
            }
            return RealWrapper(instance)
        }
    }
}
