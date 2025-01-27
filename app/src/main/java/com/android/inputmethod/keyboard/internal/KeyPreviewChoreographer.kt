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
package com.android.inputmethod.keyboard.internal

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.android.inputmethod.keyboard.Key
import com.android.inputmethod.latin.common.CoordinateUtils
import com.android.inputmethod.latin.utils.ViewLayoutUtils
import java.util.ArrayDeque

/**
 * This class controls pop up key previews. This class decides:
 * - what kind of key previews should be shown.
 * - where key previews should be placed.
 * - how key previews should be shown and dismissed.
 */
class KeyPreviewChoreographer(params: KeyPreviewDrawParams) {
    // Free {@link KeyPreviewView} pool that can be used for key preview.
    private val mFreeKeyPreviewViews: ArrayDeque<KeyPreviewView> = ArrayDeque()

    // Map from {@link Key} to {@link KeyPreviewView} that is currently being displayed as key
    // preview.
    private val mShowingKeyPreviewViews: HashMap<Key, KeyPreviewView> = HashMap()

    private val mParams: KeyPreviewDrawParams

    init {
        mParams = params
    }

    fun getKeyPreviewView(key: Key, placerView: ViewGroup): KeyPreviewView {
        var keyPreviewView: KeyPreviewView? = mShowingKeyPreviewViews.remove(key)
        if (keyPreviewView != null) {
            return keyPreviewView
        }
        keyPreviewView = mFreeKeyPreviewViews.poll()
        if (keyPreviewView != null) {
            return keyPreviewView
        }
        val context: Context = placerView.getContext()
        keyPreviewView = KeyPreviewView(context, null /* attrs */)
        keyPreviewView.setBackgroundResource(mParams.mPreviewBackgroundResId)
        placerView.addView(keyPreviewView, ViewLayoutUtils.newLayoutParam(placerView, 0, 0))
        return keyPreviewView
    }

    fun isShowingKeyPreview(key: Key): Boolean {
        return mShowingKeyPreviewViews.containsKey(key)
    }

    fun dismissKeyPreview(key: Key?, withAnimation: Boolean) {
        if (key == null) {
            return
        }
        val keyPreviewView: KeyPreviewView? = mShowingKeyPreviewViews.get(key)
        if (keyPreviewView == null) {
            return
        }
        val tag: Any = keyPreviewView.getTag()
        if (withAnimation) {
            if (tag is KeyPreviewAnimators) {
                tag.startDismiss()
                return
            }
        }
        // Dismiss preview without animation.
        mShowingKeyPreviewViews.remove(key)
        if (tag is Animator) {
            tag.cancel()
        }
        keyPreviewView.setTag(null)
        keyPreviewView.setVisibility(View.INVISIBLE)
        mFreeKeyPreviewViews.add(keyPreviewView)
    }

    fun placeAndShowKeyPreview(
        key: Key, iconsSet: KeyboardIconsSet,
        drawParams: KeyDrawParams?, keyboardViewWidth: Int, keyboardOrigin: IntArray,
        placerView: ViewGroup, withAnimation: Boolean
    ) {
        val keyPreviewView: KeyPreviewView = getKeyPreviewView(key, placerView)
        placeKeyPreview(
            key, keyPreviewView, iconsSet, drawParams!!, keyboardViewWidth, keyboardOrigin
        )
        showKeyPreview(key, keyPreviewView, withAnimation)
    }

    private fun placeKeyPreview(
        key: Key, keyPreviewView: KeyPreviewView,
        iconsSet: KeyboardIconsSet, drawParams: KeyDrawParams,
        keyboardViewWidth: Int, originCoords: IntArray
    ) {
        keyPreviewView.setPreviewVisual(key, iconsSet, drawParams)
        keyPreviewView.measure(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        mParams.setGeometry(keyPreviewView)
        val previewWidth: Int = keyPreviewView.getMeasuredWidth()
        val previewHeight: Int = mParams.mPreviewHeight
        val keyDrawWidth: Int = key.getDrawWidth()
        // The key preview is horizontally aligned with the center of the visible part of the
        // parent key. If it doesn't fit in this {@link KeyboardView}, it is moved inward to fit and
        // the left/right background is used if such background is specified.
        val keyPreviewPosition: Int
        var previewX: Int = (key.getDrawX() - (previewWidth - keyDrawWidth) / 2
                + CoordinateUtils.x(originCoords))
        if (previewX < 0) {
            previewX = 0
            keyPreviewPosition = KeyPreviewView.Companion.POSITION_LEFT
        } else if (previewX > keyboardViewWidth - previewWidth) {
            previewX = keyboardViewWidth - previewWidth
            keyPreviewPosition = KeyPreviewView.Companion.POSITION_RIGHT
        } else {
            keyPreviewPosition = KeyPreviewView.Companion.POSITION_MIDDLE
        }
        val hasMoreKeys: Boolean = (key.getMoreKeys() != null)
        keyPreviewView.setPreviewBackground(hasMoreKeys, keyPreviewPosition)
        // The key preview is placed vertically above the top edge of the parent key with an
        // arbitrary offset.
        val previewY: Int = (key.getY() - previewHeight + mParams.mPreviewOffset
                + CoordinateUtils.y(originCoords))

        ViewLayoutUtils.placeViewAt(
            keyPreviewView, previewX, previewY, previewWidth, previewHeight
        )
        keyPreviewView.setPivotX(previewWidth / 2.0f)
        keyPreviewView.setPivotY(previewHeight.toFloat())
    }

    fun showKeyPreview(
        key: Key, keyPreviewView: KeyPreviewView,
        withAnimation: Boolean
    ) {
        if (!withAnimation) {
            keyPreviewView.setVisibility(View.VISIBLE)
            mShowingKeyPreviewViews.put(key, keyPreviewView)
            return
        }

        // Show preview with animation.
        val showUpAnimator: Animator = createShowUpAnimator(key, keyPreviewView)
        val dismissAnimator: Animator = createDismissAnimator(key, keyPreviewView)
        val animators: KeyPreviewAnimators = KeyPreviewAnimators(
            showUpAnimator, dismissAnimator
        )
        keyPreviewView.setTag(animators)
        animators.startShowUp()
    }

    fun createShowUpAnimator(key: Key, keyPreviewView: KeyPreviewView): Animator {
        val showUpAnimator: Animator = mParams.createShowUpAnimator(keyPreviewView)
        showUpAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animator: Animator) {
                showKeyPreview(key, keyPreviewView, false /* withAnimation */)
            }
        })
        return showUpAnimator
    }

    private fun createDismissAnimator(key: Key, keyPreviewView: KeyPreviewView): Animator {
        val dismissAnimator: Animator = mParams.createDismissAnimator(keyPreviewView)
        dismissAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: Animator) {
                dismissKeyPreview(key, false /* withAnimation */)
            }
        })
        return dismissAnimator
    }

    private class KeyPreviewAnimators(showUpAnimator: Animator, dismissAnimator: Animator) :
        AnimatorListenerAdapter() {
        private val mShowUpAnimator: Animator
        private val mDismissAnimator: Animator

        init {
            mShowUpAnimator = showUpAnimator
            mDismissAnimator = dismissAnimator
        }

        fun startShowUp() {
            mShowUpAnimator.start()
        }

        fun startDismiss() {
            if (mShowUpAnimator.isRunning()) {
                mShowUpAnimator.addListener(this)
                return
            }
            mDismissAnimator.start()
        }

        override fun onAnimationEnd(animator: Animator) {
            mDismissAnimator.start()
        }
    }
}
