/*
 * Copyright (C) 2014, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.inputmethod.latin

import android.annotation.SuppressLint
import android.content.res.Resources
import android.util.Log
import android.util.Pair
import android.view.KeyEvent
import com.android.inputmethod.keyboard.KeyboardSwitcher
import com.android.inputmethod.keyboard.KeyboardSwitcher.KeyboardSwitchState
import com.android.inputmethod.latin.settings.Settings

/**
 * A class for detecting Emoji-Alt physical key.
 */
@SuppressLint("LongLogTag")
internal class EmojiAltPhysicalKeyDetector(resources: Resources) {
    private val mHotKeysList: MutableList<EmojiHotKeys>

    private class HotKeySet : HashSet<Pair<Int, Int>>()

    private abstract inner class EmojiHotKeys(name: String, keySet: HotKeySet) {
        private val mName: String
        private val mKeySet: HotKeySet

        var mCanFire: Boolean
        var mMetaState: Int = 0

        init {
            mName = name
            mKeySet = keySet
            mCanFire = false
        }

        fun onKeyDown(keyEvent: KeyEvent) {
            if (DEBUG) {
                Log.d(TAG, "EmojiHotKeys.onKeyDown() - " + mName + " - considering " + keyEvent)
            }

            val key: Pair<Int, Int> =
                Pair.create(keyEvent.getKeyCode(), keyEvent.getMetaState())
            if (mKeySet.contains(key)) {
                if (DEBUG) {
                    Log.d(TAG, "EmojiHotKeys.onKeyDown() - " + mName + " - enabling action")
                }
                mCanFire = true
                mMetaState = keyEvent.getMetaState()
            } else if (mCanFire) {
                if (DEBUG) {
                    Log.d(TAG, "EmojiHotKeys.onKeyDown() - " + mName + " - disabling action")
                }
                mCanFire = false
            }
        }

        fun onKeyUp(keyEvent: KeyEvent) {
            if (DEBUG) {
                Log.d(TAG, "EmojiHotKeys.onKeyUp() - " + mName + " - considering " + keyEvent)
            }

            val keyCode: Int = keyEvent.getKeyCode()
            var metaState: Int = keyEvent.getMetaState()
            if (KeyEvent.isModifierKey(keyCode)) {
                // Try restoring meta stat in case the released key was a modifier.
                // I am sure one can come up with scenarios to break this, but it
                // seems to work well in practice.
                metaState = metaState or mMetaState
            }

            val key: Pair<Int, Int> = Pair.create(keyCode, metaState)
            if (mKeySet.contains(key)) {
                if (mCanFire) {
                    if (!keyEvent.isCanceled()) {
                        if (DEBUG) {
                            Log.d(TAG, "EmojiHotKeys.onKeyUp() - " + mName + " - firing action")
                        }
                        action()
                    } else {
                        // This key up event was a part of key combinations and
                        // should be ignored.
                        if (DEBUG) {
                            Log.d(
                                TAG,
                                "EmojiHotKeys.onKeyUp() - " + mName + " - canceled, ignoring action"
                            )
                        }
                    }
                    mCanFire = false
                }
            }

            if (mCanFire) {
                if (DEBUG) {
                    Log.d(TAG, "EmojiHotKeys.onKeyUp() - " + mName + " - disabling action")
                }
                mCanFire = false
            }
        }

        protected abstract fun action()
    }

    init {
        mHotKeysList = ArrayList()

        val emojiSwitchSet: HotKeySet = parseHotKeys(
            resources, R.array.keyboard_switcher_emoji
        )
        val emojiHotKeys: EmojiHotKeys = object : EmojiHotKeys("emoji", emojiSwitchSet) {
            override fun action() {
                val switcher: KeyboardSwitcher = KeyboardSwitcher.getInstance()
                switcher.onToggleKeyboard(KeyboardSwitchState.EMOJI)
            }
        }
        mHotKeysList.add(emojiHotKeys)

        val symbolsSwitchSet: HotKeySet = parseHotKeys(
            resources, R.array.keyboard_switcher_symbols_shifted
        )
        val symbolsHotKeys: EmojiHotKeys = object : EmojiHotKeys("symbols", symbolsSwitchSet) {
            override fun action() {
                val switcher: KeyboardSwitcher = KeyboardSwitcher.getInstance()
                switcher.onToggleKeyboard(KeyboardSwitchState.SYMBOLS_SHIFTED)
            }
        }
        mHotKeysList.add(symbolsHotKeys)
    }

    fun onKeyDown(keyEvent: KeyEvent) {
        if (DEBUG) {
            Log.d(TAG, "onKeyDown(): " + keyEvent)
        }

        if (shouldProcessEvent(keyEvent)) {
            for (hotKeys: EmojiHotKeys in mHotKeysList) {
                hotKeys.onKeyDown(keyEvent)
            }
        }
    }

    fun onKeyUp(keyEvent: KeyEvent) {
        if (DEBUG) {
            Log.d(TAG, "onKeyUp(): " + keyEvent)
        }

        if (shouldProcessEvent(keyEvent)) {
            for (hotKeys: EmojiHotKeys in mHotKeysList) {
                hotKeys.onKeyUp(keyEvent)
            }
        }
    }

    companion object {
        private const val TAG: String = "EmojiAltPhysicalKeyDetector"
        private const val DEBUG: Boolean = false

        private fun shouldProcessEvent(keyEvent: KeyEvent): Boolean {
            if (Settings.instance.current?.mEnableEmojiAltPhysicalKey != true) {
                // The feature is disabled.
                if (DEBUG) {
                    Log.d(TAG, "shouldProcessEvent(): Disabled")
                }
                return false
            }

            return true
        }

        private fun parseHotKeys(
            resources: Resources, resourceId: Int
        ): HotKeySet {
            val keySet = HotKeySet()
            val name: String = resources.getResourceEntryName(resourceId)
            val values: Array<String> = resources.getStringArray(resourceId)
            var i = 0
            while (values != null && i < values.size) {
                val valuePair: Array<String> =
                    values.get(i).split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (valuePair.size != 2) {
                    Log.w(TAG, "Expected 2 integers in " + name + "[" + i + "] : " + values.get(i))
                }
                try {
                    val keyCode: Int = valuePair.get(0).toInt()
                    val metaState: Int = valuePair.get(1).toInt()
                    val key: Pair<Int, Int> = Pair.create(
                        keyCode, KeyEvent.normalizeMetaState(metaState)
                    )
                    keySet.add(key)
                } catch (e: NumberFormatException) {
                    Log.w(TAG, "Failed to parse " + name + "[" + i + "] : " + values.get(i), e)
                }
                i++
            }
            return keySet
        }
    }
}
