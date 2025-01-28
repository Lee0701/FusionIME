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
package com.android.inputmethod.latin.suggestions

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import com.android.inputmethod.keyboard.Key
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.keyboard.KeyboardActionListener
import com.android.inputmethod.keyboard.MoreKeysKeyboardView
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.SuggestedWords
import com.android.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import com.android.inputmethod.latin.suggestions.MoreSuggestions.MoreSuggestionKey
import com.android.inputmethod.latin.suggestions.MoreSuggestionsView

/**
 * A view that renders a virtual [MoreSuggestions]. It handles rendering of keys and detecting
 * key presses and touch movements.
 */
class MoreSuggestionsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet?,
    defStyle: Int = R.attr.moreKeysKeyboardViewStyle
) :
    MoreKeysKeyboardView(context, attrs, defStyle) {
    abstract class MoreSuggestionsListener : KeyboardActionListener.Adapter() {
        abstract fun onSuggestionSelected(info: SuggestedWordInfo)
    }

    var isInModalMode: Boolean = false
        private set

    override var keyboard: Keyboard?
        get() = super.keyboard
        // TODO: Remove redundant override method.
        set(keyboard) {
            super.keyboard = keyboard
            isInModalMode = false
            // With accessibility mode off, {@link #mAccessibilityDelegate} is set to null at the
            // above {@link MoreKeysKeyboardView#setKeyboard(Keyboard)} call.
            // With accessibility mode on, {@link #mAccessibilityDelegate} is set to a
            // {@link MoreKeysKeyboardAccessibilityDelegate} object at the above
            // {@link MoreKeysKeyboardView#setKeyboard(Keyboard)} call.
            if (mAccessibilityDelegate != null) {
                mAccessibilityDelegate!!.setOpenAnnounce(R.string.spoken_open_more_suggestions)
                mAccessibilityDelegate!!.setCloseAnnounce(R.string.spoken_close_more_suggestions)
            }
        }

    override val defaultCoordX: Int
        get() {
            val pane: MoreSuggestions? = keyboard as MoreSuggestions?
            return pane!!.mOccupiedWidth / 2
        }

    fun updateKeyboardGeometry(keyHeight: Int) {
        updateKeyDrawParams(keyHeight)
    }

    fun setModalMode() {
        isInModalMode = true
        // Set vertical correction to zero (Reset more keys keyboard sliding allowance
        // {@link R#dimen.config_more_keys_keyboard_slide_allowance}).
        mKeyDetector.setKeyboard(
            keyboard!!,
            -getPaddingLeft().toFloat(),
            -getPaddingTop().toFloat()
        )
    }

    override fun onKeyInput(key: Key, x: Int, y: Int) {
        if (key !is MoreSuggestionKey) {
            Log.e(
                TAG, "Expected key is MoreSuggestionKey, but found "
                        + key.javaClass.getName()
            )
            return
        }
        val keyboard: Keyboard? = this.keyboard
        if (keyboard !is MoreSuggestions) {
            Log.e(
                TAG, "Expected keyboard is MoreSuggestions, but found "
                        + keyboard!!.javaClass.getName()
            )
            return
        }
        val suggestedWords: SuggestedWords? = keyboard.mSuggestedWords
        val index: Int = key.mSuggestedWordIndex
        if (index < 0 || index >= suggestedWords!!.size()) {
            Log.e(TAG, "Selected suggestion has an illegal index: " + index)
            return
        }
        if (mListener !is MoreSuggestionsListener) {
            Log.e(
                TAG, "Expected mListener is MoreSuggestionsListener, but found "
                        + mListener!!.javaClass.getName()
            )
            return
        }
        (mListener as MoreSuggestionsListener).onSuggestionSelected(suggestedWords!!.getInfo(index))
    }

    companion object {
        private val TAG: String = MoreSuggestionsView::class.java.getSimpleName()
    }
}
