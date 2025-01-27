/**
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.android.inputmethod.dictionarypack

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewPropertyAnimator
import android.widget.Button
import android.widget.FrameLayout
import com.android.inputmethod.latin.R

/**
 * A view that handles buttons inside it according to a status.
 */
class ButtonSwitcher : FrameLayout {
    // One of the above
    private var mStatus: Int = NOT_INITIALIZED
    private var mAnimateToStatus: Int = NOT_INITIALIZED

    private var mInstallButton: Button? = null
    private var mCancelButton: Button? = null
    private var mDeleteButton: Button? = null
    private var mInterfaceState: DictionaryListInterfaceState? = null
    private var mOnClickListener: OnClickListener? = null

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    fun reset(interfaceState: DictionaryListInterfaceState?) {
        mStatus = NOT_INITIALIZED
        mAnimateToStatus = NOT_INITIALIZED
        mInterfaceState = interfaceState
    }

    override fun onLayout(
        changed: Boolean, left: Int, top: Int, right: Int,
        bottom: Int
    ) {
        super.onLayout(changed, left, top, right, bottom)
        mInstallButton = findViewById<View>(R.id.dict_install_button) as Button?
        mCancelButton = findViewById<View>(R.id.dict_cancel_button) as Button?
        mDeleteButton = findViewById<View>(R.id.dict_delete_button) as Button?
        setInternalOnClickListener(mOnClickListener)
        setButtonPositionWithoutAnimation(mStatus)
        if (mAnimateToStatus != NOT_INITIALIZED) {
            // We have been asked to animate before we were ready, so we took a note of it.
            // We are now ready: launch the animation.
            animateButtonPosition(mStatus, mAnimateToStatus)
            mStatus = mAnimateToStatus
            mAnimateToStatus = NOT_INITIALIZED
        }
    }

    private fun getButton(status: Int): Button? {
        when (status) {
            STATUS_INSTALL -> return mInstallButton
            STATUS_CANCEL -> return mCancelButton
            STATUS_DELETE -> return mDeleteButton
            else -> return null
        }
    }

    fun setStatusAndUpdateVisuals(status: Int) {
        if (mStatus == NOT_INITIALIZED) {
            setButtonPositionWithoutAnimation(status)
            mStatus = status
        } else {
            if (null == mInstallButton) {
                // We may come here before we have been layout. In this case we don't know our
                // size yet so we can't start animations so we need to remember what animation to
                // start once layout has gone through.
                mAnimateToStatus = status
            } else {
                animateButtonPosition(mStatus, status)
                mStatus = status
            }
        }
    }

    private fun setButtonPositionWithoutAnimation(status: Int) {
        // This may be called by setStatus() before the layout has come yet.
        if (null == mInstallButton) return
        val width: Int = getWidth()
        // Set to out of the screen if that's not the currently displayed status
        mInstallButton!!.setTranslationX((if (STATUS_INSTALL == status) 0 else width).toFloat())
        mCancelButton!!.setTranslationX((if (STATUS_CANCEL == status) 0 else width).toFloat())
        mDeleteButton!!.setTranslationX((if (STATUS_DELETE == status) 0 else width).toFloat())
    }

    // The helper method for {@link AnimatorListenerAdapter}.
    fun animateButtonIfStatusIsEqual(newButton: View, newStatus: Int) {
        if (newStatus != mStatus) return
        animateButton(newButton, ANIMATION_IN)
    }

    private fun animateButtonPosition(oldStatus: Int, newStatus: Int) {
        val oldButton: View? = getButton(oldStatus)
        val newButton: View? = getButton(newStatus)
        if (null != oldButton && null != newButton) {
            // Transition between two buttons : animate out, then in
            animateButton(oldButton, ANIMATION_OUT).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animateButtonIfStatusIsEqual(newButton, newStatus)
                }
            })
        } else if (null != oldButton) {
            animateButton(oldButton, ANIMATION_OUT)
        } else if (null != newButton) {
            animateButton(newButton, ANIMATION_IN)
        }
    }

    fun setInternalOnClickListener(listener: OnClickListener?) {
        mOnClickListener = listener
        if (null != mInstallButton) {
            // Already laid out : do it now
            mInstallButton!!.setOnClickListener(mOnClickListener)
            mCancelButton!!.setOnClickListener(mOnClickListener)
            mDeleteButton!!.setOnClickListener(mOnClickListener)
        }
    }

    private fun animateButton(button: View, direction: Int): ViewPropertyAnimator {
        val outerX: Float = getWidth().toFloat()
        val innerX: Float = button.getX() - button.getTranslationX()
        mInterfaceState!!.removeFromCache(getParent() as View)
        if (ANIMATION_IN == direction) {
            button.setClickable(true)
            return button.animate().translationX(0f)
        }
        button.setClickable(false)
        return button.animate().translationX(outerX - innerX)
    }

    companion object {
        val NOT_INITIALIZED: Int = -1
        const val STATUS_NO_BUTTON: Int = 0
        const val STATUS_INSTALL: Int = 1
        const val STATUS_CANCEL: Int = 2
        const val STATUS_DELETE: Int = 3

        // Animation directions
        const val ANIMATION_IN: Int = 1
        const val ANIMATION_OUT: Int = 2
    }
}
