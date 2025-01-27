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

import com.android.inputmethod.latin.common.Constants

object KeyboardCodesSet {
    const val PREFIX_CODE: String = "!code/"

    private val sNameToIdMap: HashMap<String, Int> = HashMap()

    fun getCode(name: String): Int {
        val id: Int? = sNameToIdMap.get(name)
        if (id == null) throw RuntimeException("Unknown key code: " + name)
        return DEFAULT.get(id)
    }

    private val ID_TO_NAME: Array<String> = arrayOf(
        "key_tab",
        "key_enter",
        "key_space",
        "key_shift",
        "key_capslock",
        "key_switch_alpha_symbol",
        "key_output_text",
        "key_delete",
        "key_settings",
        "key_shortcut",
        "key_action_next",
        "key_action_previous",
        "key_shift_enter",
        "key_language_switch",
        "key_emoji",
        "key_alpha_from_emoji",
        "key_unspecified",
    )

    private val DEFAULT: IntArray = intArrayOf(
        Constants.CODE_TAB,
        Constants.CODE_ENTER,
        Constants.CODE_SPACE,
        Constants.CODE_SHIFT,
        Constants.CODE_CAPSLOCK,
        Constants.CODE_SWITCH_ALPHA_SYMBOL,
        Constants.CODE_OUTPUT_TEXT,
        Constants.CODE_DELETE,
        Constants.CODE_SETTINGS,
        Constants.CODE_SHORTCUT,
        Constants.CODE_ACTION_NEXT,
        Constants.CODE_ACTION_PREVIOUS,
        Constants.CODE_SHIFT_ENTER,
        Constants.CODE_LANGUAGE_SWITCH,
        Constants.CODE_EMOJI,
        Constants.CODE_ALPHA_FROM_EMOJI,
        Constants.CODE_UNSPECIFIED,
    )

    init {
        for (i in ID_TO_NAME.indices) {
            sNameToIdMap.put(ID_TO_NAME.get(i), i)
        }
    }
}
