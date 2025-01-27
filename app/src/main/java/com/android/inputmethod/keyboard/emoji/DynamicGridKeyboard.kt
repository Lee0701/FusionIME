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

import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Log
import com.android.inputmethod.keyboard.Key
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.latin.settings.Settings
import com.android.inputmethod.latin.utils.JsonUtils
import java.util.ArrayDeque
import java.util.Collections
import kotlin.math.abs

/**
 * This is a Keyboard class where you can add keys dynamically shown in a grid layout
 */
internal class DynamicGridKeyboard(
    prefs: SharedPreferences, templateKeyboard: Keyboard,
    maxKeyCount: Int, categoryId: Int
) : Keyboard(templateKeyboard) {
    private val mLock: Any = Any()

    private val mPrefs: SharedPreferences
    private val mHorizontalStep: Int
    private val mVerticalStep: Int
    private val mColumnsNum: Int
    private val mMaxKeyCount: Int
    private val mIsRecents: Boolean
    private val mGridKeys: ArrayDeque<GridKey> = ArrayDeque()
    private val mPendingKeys: ArrayDeque<Key> = ArrayDeque()

    private var mCachedGridKeys: List<Key?>? = null

    init {
        val key0: Key = getTemplateKey(TEMPLATE_KEY_CODE_0)
        val key1: Key = getTemplateKey(TEMPLATE_KEY_CODE_1)
        mHorizontalStep = abs((key1.getX() - key0.getX()).toDouble())
        mVerticalStep = key0.getHeight() + mVerticalGap
        mColumnsNum = mBaseWidth / mHorizontalStep
        mMaxKeyCount = maxKeyCount
        mIsRecents = categoryId == EmojiCategory.Companion.ID_RECENTS
        mPrefs = prefs
    }

    private fun getTemplateKey(code: Int): Key {
        for (key: Key in super.getSortedKeys()) {
            if (key.getCode() == code) {
                return key
            }
        }
        throw RuntimeException("Can't find template key: code=" + code)
    }

    fun addPendingKey(usedKey: Key) {
        synchronized(mLock) {
            mPendingKeys.addLast(usedKey)
        }
    }

    fun flushPendingRecentKeys() {
        synchronized(mLock) {
            while (!mPendingKeys.isEmpty()) {
                addKey(mPendingKeys.pollFirst(), true)
            }
            saveRecentKeys()
        }
    }

    fun addKeyFirst(usedKey: Key?) {
        addKey(usedKey, true)
        if (mIsRecents) {
            saveRecentKeys()
        }
    }

    fun addKeyLast(usedKey: Key?) {
        addKey(usedKey, false)
    }

    private fun addKey(usedKey: Key?, addFirst: Boolean) {
        if (usedKey == null) {
            return
        }
        synchronized(mLock) {
            mCachedGridKeys = null
            val key: GridKey = GridKey(usedKey)
            while (mGridKeys.remove(key)) {
                // Remove duplicate keys.
            }
            if (addFirst) {
                mGridKeys.addFirst(key)
            } else {
                mGridKeys.addLast(key)
            }
            while (mGridKeys.size > mMaxKeyCount) {
                mGridKeys.removeLast()
            }
            var index: Int = 0
            for (gridKey: GridKey in mGridKeys) {
                val keyX0: Int = getKeyX0(index)
                val keyY0: Int = getKeyY0(index)
                val keyX1: Int = getKeyX1(index)
                val keyY1: Int = getKeyY1(index)
                gridKey.updateCoordinates(keyX0, keyY0, keyX1, keyY1)
                index++
            }
        }
    }

    private fun saveRecentKeys() {
        val keys: ArrayList<Any?> = ArrayList()
        for (key: Key in mGridKeys) {
            if (key.getOutputText() != null) {
                keys.add(key.getOutputText())
            } else {
                keys.add(key.getCode())
            }
        }
        val jsonStr: String = JsonUtils.listToJsonStr(keys)
        Settings.Companion.writeEmojiRecentKeys(mPrefs, jsonStr)
    }

    fun loadRecentKeys(keyboards: Collection<DynamicGridKeyboard>) {
        val str: String = Settings.Companion.readEmojiRecentKeys(mPrefs)
        val keys: List<Any?> = JsonUtils.jsonStrToList(str)
        for (o: Any? in keys) {
            val key: Key?
            if (o is Int) {
                key = getKeyByCode(keyboards, o)
            } else if (o is String) {
                key = getKeyByOutputText(keyboards, o)
            } else {
                Log.w(TAG, "Invalid object: " + o)
                continue
            }
            addKeyLast(key)
        }
    }

    private fun getKeyX0(index: Int): Int {
        val column: Int = index % mColumnsNum
        return column * mHorizontalStep
    }

    private fun getKeyX1(index: Int): Int {
        val column: Int = index % mColumnsNum + 1
        return column * mHorizontalStep
    }

    private fun getKeyY0(index: Int): Int {
        val row: Int = index / mColumnsNum
        return row * mVerticalStep + mVerticalGap / 2
    }

    private fun getKeyY1(index: Int): Int {
        val row: Int = index / mColumnsNum + 1
        return row * mVerticalStep + mVerticalGap / 2
    }

    override fun getSortedKeys(): List<Key?> {
        synchronized(mLock) {
            if (mCachedGridKeys != null) {
                return mCachedGridKeys
            }
            val cachedKeys: ArrayList<Key> = ArrayList(mGridKeys)
            mCachedGridKeys = Collections.unmodifiableList(cachedKeys)
            return mCachedGridKeys
        }
    }

    override fun getNearestKeys(x: Int, y: Int): List<Key?> {
        // TODO: Calculate the nearest key index in mGridKeys from x and y.
        return getSortedKeys()
    }

    internal class GridKey(originalKey: Key) : Key(originalKey) {
        private var mCurrentX: Int = 0
        private var mCurrentY: Int = 0

        fun updateCoordinates(x0: Int, y0: Int, x1: Int, y1: Int) {
            mCurrentX = x0
            mCurrentY = y0
            getHitBox().set(x0, y0, x1, y1)
        }

        override fun getX(): Int {
            return mCurrentX
        }

        override fun getY(): Int {
            return mCurrentY
        }

        override fun equals(o: Any?): Boolean {
            if (o !is Key) return false
            val key: Key = o
            if (getCode() != key.getCode()) return false
            if (!TextUtils.equals(getLabel(), key.getLabel())) return false
            return TextUtils.equals(getOutputText(), key.getOutputText())
        }

        override fun toString(): String {
            return "GridKey: " + super.toString()
        }
    }

    companion object {
        private val TAG: String = DynamicGridKeyboard::class.java.getSimpleName()
        private const val TEMPLATE_KEY_CODE_0: Int = 0x30
        private const val TEMPLATE_KEY_CODE_1: Int = 0x31
        private fun getKeyByCode(
            keyboards: Collection<DynamicGridKeyboard>,
            code: Int
        ): Key? {
            for (keyboard: DynamicGridKeyboard in keyboards) {
                for (key: Key in keyboard.getSortedKeys()) {
                    if (key.getCode() == code) {
                        return key
                    }
                }
            }
            return null
        }

        private fun getKeyByOutputText(
            keyboards: Collection<DynamicGridKeyboard>,
            outputText: String
        ): Key? {
            for (keyboard: DynamicGridKeyboard in keyboards) {
                for (key: Key in keyboard.getSortedKeys()) {
                    if (outputText == key.getOutputText()) {
                        return key
                    }
                }
            }
            return null
        }
    }
}
