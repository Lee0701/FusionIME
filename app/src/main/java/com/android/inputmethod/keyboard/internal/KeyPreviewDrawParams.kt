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

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.TypedArray
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.android.inputmethod.latin.R
import kotlin.math.min

class KeyPreviewDrawParams(mainKeyboardViewAttr: TypedArray) {
    // XML attributes of {@link MainKeyboardView}.
    val mPreviewOffset: Int
    val mPreviewHeight: Int
    val mPreviewBackgroundResId: Int
    private val mShowUpAnimatorResId: Int
    private val mDismissAnimatorResId: Int
    private var mHasCustomAnimationParams: Boolean = false
    private var mShowUpDuration: Int = 0
    private var mDismissDuration: Int = 0
    private var mShowUpStartXScale: Float = 0f
    private var mShowUpStartYScale: Float = 0f
    private var mDismissEndXScale: Float = 0f
    private var mDismissEndYScale: Float = 0f
    private var mLingerTimeout: Int
    private var mShowPopup: Boolean = true

    // The graphical geometry of the key preview.
    // <-width->
    // +-------+   ^
    // |       |   |
    // |preview| height (visible)
    // |       |   |
    // +       + ^ v
    //  \     /  |offset
    // +-\   /-+ v
    // |  +-+  |
    // |parent |
    // |    key|
    // +-------+
    // The background of a {@link TextView} being used for a key preview may have invisible
    // paddings. To align the more keys keyboard panel's visible part with the visible part of
    // the background, we need to record the width and height of key preview that don't include
    // invisible paddings.
    private var mVisibleWidth: Int = 0
    private var mVisibleHeight: Int = 0

    // The key preview may have an arbitrary offset and its background that may have a bottom
    // padding. To align the more keys keyboard and the key preview we also need to record the
    // offset between the top edge of parent key and the bottom of the visible part of key
    // preview background.
    private var mVisibleOffset: Int = 0

    fun setVisibleOffset(previewVisibleOffset: Int) {
        mVisibleOffset = previewVisibleOffset
    }

    fun getVisibleOffset(): Int {
        return mVisibleOffset
    }

    fun setGeometry(previewTextView: View) {
        val previewWidth: Int = previewTextView.getMeasuredWidth()
        val previewHeight: Int = mPreviewHeight
        // The width and height of visible part of the key preview background. The content marker
        // of the background 9-patch have to cover the visible part of the background.
        mVisibleWidth = (previewWidth - previewTextView.getPaddingLeft()
                - previewTextView.getPaddingRight())
        mVisibleHeight = (previewHeight - previewTextView.getPaddingTop()
                - previewTextView.getPaddingBottom())
        // The distance between the top edge of the parent key and the bottom of the visible part
        // of the key preview background.
        setVisibleOffset(mPreviewOffset - previewTextView.getPaddingBottom())
    }

    fun getVisibleWidth(): Int {
        return mVisibleWidth
    }

    fun getVisibleHeight(): Int {
        return mVisibleHeight
    }

    fun setPopupEnabled(enabled: Boolean, lingerTimeout: Int) {
        mShowPopup = enabled
        mLingerTimeout = lingerTimeout
    }

    fun isPopupEnabled(): Boolean {
        return mShowPopup
    }

    fun getLingerTimeout(): Int {
        return mLingerTimeout
    }

    fun setAnimationParams(
        hasCustomAnimationParams: Boolean,
        showUpStartXScale: Float, showUpStartYScale: Float, showUpDuration: Int,
        dismissEndXScale: Float, dismissEndYScale: Float, dismissDuration: Int
    ) {
        mHasCustomAnimationParams = hasCustomAnimationParams
        mShowUpStartXScale = showUpStartXScale
        mShowUpStartYScale = showUpStartYScale
        mShowUpDuration = showUpDuration
        mDismissEndXScale = dismissEndXScale
        mDismissEndYScale = dismissEndYScale
        mDismissDuration = dismissDuration
    }

    init {
        mPreviewOffset = mainKeyboardViewAttr.getDimensionPixelOffset(
            R.styleable.MainKeyboardView_keyPreviewOffset, 0
        )
        mPreviewHeight = mainKeyboardViewAttr.getDimensionPixelSize(
            R.styleable.MainKeyboardView_keyPreviewHeight, 0
        )
        mPreviewBackgroundResId = mainKeyboardViewAttr.getResourceId(
            R.styleable.MainKeyboardView_keyPreviewBackground, 0
        )
        mLingerTimeout = mainKeyboardViewAttr.getInt(
            R.styleable.MainKeyboardView_keyPreviewLingerTimeout, 0
        )
        mShowUpAnimatorResId = mainKeyboardViewAttr.getResourceId(
            R.styleable.MainKeyboardView_keyPreviewShowUpAnimator, 0
        )
        mDismissAnimatorResId = mainKeyboardViewAttr.getResourceId(
            R.styleable.MainKeyboardView_keyPreviewDismissAnimator, 0
        )
    }

    fun createShowUpAnimator(target: View): Animator {
        if (mHasCustomAnimationParams) {
            val scaleXAnimator: ObjectAnimator = ObjectAnimator.ofFloat(
                target, View.SCALE_X, mShowUpStartXScale,
                KEY_PREVIEW_SHOW_UP_END_SCALE
            )
            val scaleYAnimator: ObjectAnimator = ObjectAnimator.ofFloat(
                target, View.SCALE_Y, mShowUpStartYScale,
                KEY_PREVIEW_SHOW_UP_END_SCALE
            )
            val showUpAnimator: AnimatorSet = AnimatorSet()
            showUpAnimator.play(scaleXAnimator).with(scaleYAnimator)
            showUpAnimator.setDuration(mShowUpDuration.toLong())
            showUpAnimator.setInterpolator(DECELERATE_INTERPOLATOR)
            return showUpAnimator
        }
        val animator: Animator = AnimatorInflater.loadAnimator(
            target.getContext(), mShowUpAnimatorResId
        )
        animator.setTarget(target)
        animator.setInterpolator(DECELERATE_INTERPOLATOR)
        return animator
    }

    fun createDismissAnimator(target: View): Animator {
        if (mHasCustomAnimationParams) {
            val scaleXAnimator: ObjectAnimator = ObjectAnimator.ofFloat(
                target, View.SCALE_X, mDismissEndXScale
            )
            val scaleYAnimator: ObjectAnimator = ObjectAnimator.ofFloat(
                target, View.SCALE_Y, mDismissEndYScale
            )
            val dismissAnimator: AnimatorSet = AnimatorSet()
            dismissAnimator.play(scaleXAnimator).with(scaleYAnimator)
            val dismissDuration: Int =
                min(mDismissDuration.toDouble(), mLingerTimeout.toDouble()).toInt()
            dismissAnimator.setDuration(dismissDuration.toLong())
            dismissAnimator.setInterpolator(ACCELERATE_INTERPOLATOR)
            return dismissAnimator
        }
        val animator: Animator = AnimatorInflater.loadAnimator(
            target.getContext(), mDismissAnimatorResId
        )
        animator.setTarget(target)
        animator.setInterpolator(ACCELERATE_INTERPOLATOR)
        return animator
    }

    companion object {
        private const val KEY_PREVIEW_SHOW_UP_END_SCALE: Float = 1.0f
        private val ACCELERATE_INTERPOLATOR: AccelerateInterpolator = AccelerateInterpolator()
        private val DECELERATE_INTERPOLATOR: DecelerateInterpolator = DecelerateInterpolator()
    }
}
