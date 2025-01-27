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
package com.android.inputmethod.keyboard.emoji

import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.android.inputmethod.keyboard.Key
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.keyboard.KeyboardView
import com.android.inputmethod.keyboard.emoji.EmojiPageKeyboardView.OnKeyEventListener
import com.android.inputmethod.latin.R

internal class EmojiPalettesAdapter(
    emojiCategory: EmojiCategory,
    listener: OnKeyEventListener
) : PagerAdapter() {
    private val mListener: OnKeyEventListener
    private val mRecentsKeyboard: DynamicGridKeyboard?
    private val mActiveKeyboardViews: SparseArray<EmojiPageKeyboardView> = SparseArray()
    private val mEmojiCategory: EmojiCategory
    private var mActivePosition: Int = 0

    init {
        mEmojiCategory = emojiCategory
        mListener = listener
        mRecentsKeyboard = mEmojiCategory.getKeyboard(EmojiCategory.Companion.ID_RECENTS, 0)
    }

    fun flushPendingRecentKeys() {
        mRecentsKeyboard!!.flushPendingRecentKeys()
        val recentKeyboardView: KeyboardView? =
            mActiveKeyboardViews.get(mEmojiCategory.getRecentTabId())
        if (recentKeyboardView != null) {
            recentKeyboardView.invalidateAllKeys()
        }
    }

    fun addRecentKey(key: Key) {
        if (mEmojiCategory.isInRecentTab()) {
            mRecentsKeyboard!!.addPendingKey(key)
            return
        }
        mRecentsKeyboard!!.addKeyFirst(key)
        val recentKeyboardView: KeyboardView? =
            mActiveKeyboardViews.get(mEmojiCategory.getRecentTabId())
        if (recentKeyboardView != null) {
            recentKeyboardView.invalidateAllKeys()
        }
    }

    fun onPageScrolled() {
        releaseCurrentKey(false /* withKeyRegistering */)
    }

    fun releaseCurrentKey(withKeyRegistering: Boolean) {
        // Make sure the delayed key-down event (highlight effect and haptic feedback) will be
        // canceled.
        val currentKeyboardView: EmojiPageKeyboardView? =
            mActiveKeyboardViews.get(mActivePosition)
        if (currentKeyboardView == null) {
            return
        }
        currentKeyboardView.releaseCurrentKey(withKeyRegistering)
    }

    override fun getCount(): Int {
        return mEmojiCategory.getTotalPageCountOfAllCategories()
    }

    override fun setPrimaryItem(
        container: ViewGroup, position: Int,
        `object`: Any
    ) {
        if (mActivePosition == position) {
            return
        }
        val oldKeyboardView: EmojiPageKeyboardView? = mActiveKeyboardViews.get(mActivePosition)
        if (oldKeyboardView != null) {
            oldKeyboardView.releaseCurrentKey(false /* withKeyRegistering */)
            oldKeyboardView.deallocateMemory()
        }
        mActivePosition = position
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        if (DEBUG_PAGER) {
            Log.d(TAG, "instantiate item: " + position)
        }
        val oldKeyboardView: EmojiPageKeyboardView? = mActiveKeyboardViews.get(position)
        if (oldKeyboardView != null) {
            oldKeyboardView.deallocateMemory()
            // This may be redundant but wanted to be safer..
            mActiveKeyboardViews.remove(position)
        }
        val keyboard: Keyboard? =
            mEmojiCategory.getKeyboardFromPagePosition(position)
        val inflater: LayoutInflater = LayoutInflater.from(container.getContext())
        val keyboardView: EmojiPageKeyboardView = inflater.inflate(
            R.layout.emoji_keyboard_page, container, false /* attachToRoot */
        ) as EmojiPageKeyboardView
        keyboardView.setKeyboard(keyboard!!)
        keyboardView.setOnKeyEventListener(mListener)
        container.addView(keyboardView)
        mActiveKeyboardViews.put(position, keyboardView)
        return keyboardView
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun destroyItem(
        container: ViewGroup, position: Int,
        `object`: Any
    ) {
        if (DEBUG_PAGER) {
            Log.d(TAG, "destroy item: " + position + ", " + `object`.javaClass.getSimpleName())
        }
        val keyboardView: EmojiPageKeyboardView? = mActiveKeyboardViews.get(position)
        if (keyboardView != null) {
            keyboardView.deallocateMemory()
            mActiveKeyboardViews.remove(position)
        }
        if (`object` is View) {
            container.removeView(`object`)
        } else {
            Log.w(TAG, "Warning!!! Emoji palette may be leaking. " + `object`)
        }
    }

    companion object {
        private val TAG: String = EmojiPalettesAdapter::class.java.getSimpleName()
        private const val DEBUG_PAGER: Boolean = false
    }
}
