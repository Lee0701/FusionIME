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
package com.android.inputmethod.keyboard.emoji

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.widget.LinearLayout
import androidx.viewpager.widget.ViewPager
import ee.oyatl.ime.fusion.R
import com.android.inputmethod.latin.utils.ResourceUtils

internal class EmojiLayoutParams(context: Context) {
    val mEmojiPagerHeight: Int
    private val mEmojiPagerBottomMargin: Int
    val mEmojiKeyboardHeight: Int
    private val mEmojiCategoryPageIdViewHeight: Int
    val mEmojiActionBarHeight: Int
    val mKeyVerticalGap: Int
    private val mKeyHorizontalGap: Int
    private val mBottomPadding: Int
    private val mTopPadding: Int

    init {
        val res: Resources = context.getResources()
        val defaultKeyboardHeight: Int = ResourceUtils.getDefaultKeyboardHeight(res)
        val defaultKeyboardWidth: Int = ResourceUtils.getDefaultKeyboardWidth(context)
        mKeyVerticalGap = res.getFraction(
            R.fraction.config_key_vertical_gap_holo,
            defaultKeyboardHeight, defaultKeyboardHeight
        ).toInt()
        mBottomPadding = res.getFraction(
            R.fraction.config_keyboard_bottom_padding_holo,
            defaultKeyboardHeight, defaultKeyboardHeight
        ).toInt()
        mTopPadding = res.getFraction(
            R.fraction.config_keyboard_top_padding_holo,
            defaultKeyboardHeight, defaultKeyboardHeight
        ).toInt()
        mKeyHorizontalGap = (res.getFraction(
            R.fraction.config_key_horizontal_gap_holo,
            defaultKeyboardWidth, defaultKeyboardWidth
        )).toInt()
        mEmojiCategoryPageIdViewHeight =
            (res.getDimension(R.dimen.config_emoji_category_page_id_height)).toInt()
        val baseheight: Int = (defaultKeyboardHeight - mBottomPadding - mTopPadding
                + mKeyVerticalGap)
        mEmojiActionBarHeight = (baseheight / DEFAULT_KEYBOARD_ROWS
                - (mKeyVerticalGap - mBottomPadding) / 2)
        mEmojiPagerHeight = (defaultKeyboardHeight - mEmojiActionBarHeight
                - mEmojiCategoryPageIdViewHeight)
        mEmojiPagerBottomMargin = 0
        mEmojiKeyboardHeight = mEmojiPagerHeight - mEmojiPagerBottomMargin - 1
    }

    fun setPagerProperties(vp: ViewPager) {
        val lp: LinearLayout.LayoutParams = vp.getLayoutParams() as LinearLayout.LayoutParams
        lp.height = mEmojiKeyboardHeight
        lp.bottomMargin = mEmojiPagerBottomMargin
        vp.setLayoutParams(lp)
    }

    fun setCategoryPageIdViewProperties(v: View) {
        val lp: LinearLayout.LayoutParams = v.getLayoutParams() as LinearLayout.LayoutParams
        lp.height = mEmojiCategoryPageIdViewHeight
        v.setLayoutParams(lp)
    }

    fun getActionBarHeight(): Int {
        return mEmojiActionBarHeight - mBottomPadding
    }

    fun setActionBarProperties(ll: LinearLayout) {
        val lp: LinearLayout.LayoutParams = ll.getLayoutParams() as LinearLayout.LayoutParams
        lp.height = getActionBarHeight()
        ll.setLayoutParams(lp)
    }

    fun setKeyProperties(v: View) {
        val lp: LinearLayout.LayoutParams = v.getLayoutParams() as LinearLayout.LayoutParams
        lp.leftMargin = mKeyHorizontalGap / 2
        lp.rightMargin = mKeyHorizontalGap / 2
        v.setLayoutParams(lp)
    }

    companion object {
        private const val DEFAULT_KEYBOARD_ROWS: Int = 4
    }
}
