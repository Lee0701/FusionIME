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
package com.android.inputmethod.keyboard.internal

import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.SparseIntArray
import com.android.inputmethod.latin.R
import javax.annotation.Nonnull

class KeyboardIconsSet {
    private val mIcons: Array<Drawable?> = arrayOfNulls(NUM_ICONS)
    private val mIconResourceIds: IntArray = IntArray(NUM_ICONS)

    fun loadIcons(keyboardAttrs: TypedArray) {
        val size: Int = ATTR_ID_TO_ICON_ID.size()
        for (index in 0 until size) {
            val attrId: Int = ATTR_ID_TO_ICON_ID.keyAt(index)
            try {
                val icon: Drawable? = keyboardAttrs.getDrawable(attrId)
                setDefaultBounds(icon)
                val iconId: Int = ATTR_ID_TO_ICON_ID.get(attrId)
                mIcons[iconId] = icon
                mIconResourceIds[iconId] = keyboardAttrs.getResourceId(attrId, 0)
            } catch (e: Resources.NotFoundException) {
                Log.w(
                    TAG, ("Drawable resource for icon #"
                            + keyboardAttrs.getResources().getResourceEntryName(attrId)
                            + " not found")
                )
            }
        }
    }

    fun getIconResourceId(name: String): Int {
        val iconId: Int = getIconId(name)
        if (isValidIconId(iconId)) {
            return mIconResourceIds[iconId]
        }
        throw RuntimeException("unknown icon name: " + name)
    }

    fun getIconDrawable(iconId: Int): Drawable? {
        if (isValidIconId(iconId)) {
            return mIcons[iconId]
        }
        throw RuntimeException("unknown icon id: " + getIconName(iconId))
    }

    companion object {
        private val TAG: String = KeyboardIconsSet::class.java.getSimpleName()

        const val PREFIX_ICON: String = "!icon/"
        const val ICON_UNDEFINED: Int = 0
        private const val ATTR_UNDEFINED: Int = 0

        private const val NAME_UNDEFINED: String = "undefined"
        const val NAME_SHIFT_KEY: String = "shift_key"
        const val NAME_SHIFT_KEY_SHIFTED: String = "shift_key_shifted"
        const val NAME_DELETE_KEY: String = "delete_key"
        const val NAME_SETTINGS_KEY: String = "settings_key"
        const val NAME_SPACE_KEY: String = "space_key"
        const val NAME_SPACE_KEY_FOR_NUMBER_LAYOUT: String = "space_key_for_number_layout"
        const val NAME_ENTER_KEY: String = "enter_key"
        const val NAME_GO_KEY: String = "go_key"
        const val NAME_SEARCH_KEY: String = "search_key"
        const val NAME_SEND_KEY: String = "send_key"
        const val NAME_NEXT_KEY: String = "next_key"
        const val NAME_DONE_KEY: String = "done_key"
        const val NAME_PREVIOUS_KEY: String = "previous_key"
        const val NAME_TAB_KEY: String = "tab_key"
        const val NAME_SHORTCUT_KEY: String = "shortcut_key"
        const val NAME_SHORTCUT_KEY_DISABLED: String = "shortcut_key_disabled"
        const val NAME_LANGUAGE_SWITCH_KEY: String = "language_switch_key"
        const val NAME_ZWNJ_KEY: String = "zwnj_key"
        const val NAME_ZWJ_KEY: String = "zwj_key"
        const val NAME_EMOJI_ACTION_KEY: String = "emoji_action_key"
        const val NAME_EMOJI_NORMAL_KEY: String = "emoji_normal_key"

        private val ATTR_ID_TO_ICON_ID: SparseIntArray = SparseIntArray()

        // Icon name to icon id map.
        private val sNameToIdsMap: HashMap<String?, Int> = HashMap()

        private val NAMES_AND_ATTR_IDS: Array<Any> = arrayOf(
            NAME_UNDEFINED, ATTR_UNDEFINED,
            NAME_SHIFT_KEY, R.styleable.Keyboard_iconShiftKey,
            NAME_DELETE_KEY, R.styleable.Keyboard_iconDeleteKey,
            NAME_SETTINGS_KEY, R.styleable.Keyboard_iconSettingsKey,
            NAME_SPACE_KEY, R.styleable.Keyboard_iconSpaceKey,
            NAME_ENTER_KEY, R.styleable.Keyboard_iconEnterKey,
            NAME_GO_KEY, R.styleable.Keyboard_iconGoKey,
            NAME_SEARCH_KEY, R.styleable.Keyboard_iconSearchKey,
            NAME_SEND_KEY, R.styleable.Keyboard_iconSendKey,
            NAME_NEXT_KEY, R.styleable.Keyboard_iconNextKey,
            NAME_DONE_KEY, R.styleable.Keyboard_iconDoneKey,
            NAME_PREVIOUS_KEY, R.styleable.Keyboard_iconPreviousKey,
            NAME_TAB_KEY, R.styleable.Keyboard_iconTabKey,
            NAME_SHORTCUT_KEY, R.styleable.Keyboard_iconShortcutKey,
            NAME_SPACE_KEY_FOR_NUMBER_LAYOUT, R.styleable.Keyboard_iconSpaceKeyForNumberLayout,
            NAME_SHIFT_KEY_SHIFTED, R.styleable.Keyboard_iconShiftKeyShifted,
            NAME_SHORTCUT_KEY_DISABLED, R.styleable.Keyboard_iconShortcutKeyDisabled,
            NAME_LANGUAGE_SWITCH_KEY, R.styleable.Keyboard_iconLanguageSwitchKey,
            NAME_ZWNJ_KEY, R.styleable.Keyboard_iconZwnjKey,
            NAME_ZWJ_KEY, R.styleable.Keyboard_iconZwjKey,
            NAME_EMOJI_ACTION_KEY, R.styleable.Keyboard_iconEmojiActionKey,
            NAME_EMOJI_NORMAL_KEY, R.styleable.Keyboard_iconEmojiNormalKey,
        )

        private val NUM_ICONS: Int = NAMES_AND_ATTR_IDS.size / 2
        private val ICON_NAMES: Array<String?> = arrayOfNulls(NUM_ICONS)

        init {
            var iconId: Int = ICON_UNDEFINED
            var i: Int = 0
            while (i < NAMES_AND_ATTR_IDS.size) {
                val name: String = NAMES_AND_ATTR_IDS[i] as String
                val attrId: Int = NAMES_AND_ATTR_IDS[i + 1] as Int
                if (attrId != ATTR_UNDEFINED) {
                    ATTR_ID_TO_ICON_ID.put(attrId, iconId)
                }
                sNameToIdsMap[name] = iconId
                ICON_NAMES[iconId] = name
                iconId++
                i += 2
            }
        }

        private fun isValidIconId(iconId: Int): Boolean {
            return iconId >= 0 && iconId < ICON_NAMES.size
        }

        fun getIconName(iconId: Int): String {
            return if (isValidIconId(iconId)) ICON_NAMES[iconId]!! else "unknown<" + iconId + ">"
        }

        fun getIconId(name: String?): Int {
            val iconId: Int? = sNameToIdsMap.get(name)
            if (iconId != null) {
                return iconId
            }
            throw RuntimeException("unknown icon name: " + name)
        }

        private fun setDefaultBounds(icon: Drawable?) {
            if (icon != null) {
                icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight())
            }
        }
    }
}
