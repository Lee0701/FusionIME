/*
 * Copyright (C) 2009 The Android Open Source Project
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
package com.android.inputmethod.pinyin

import android.view.KeyEvent

/**
 * Class used to map the symbols on Dream's hardware keyboard to corresponding
 * Chinese full-width symbols.
 */
object KeyMapDream {
    // Number of shift bits to store full-width symbols
    private const val SHIFT_FWCH = 8
    private val mKeyMap = intArrayOf(
        KeyEvent.KEYCODE_UNKNOWN,
        KeyEvent.KEYCODE_SOFT_LEFT,
        KeyEvent.KEYCODE_SOFT_RIGHT,
        KeyEvent.KEYCODE_HOME,
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_CALL,
        KeyEvent.KEYCODE_ENDCALL,
        KeyEvent.KEYCODE_0 or ('\uff09'.code shl SHIFT_FWCH),  // )
        KeyEvent.KEYCODE_1 or ('\uff01'.code shl SHIFT_FWCH),  // !
        KeyEvent.KEYCODE_2 or ('\uff20'.code shl SHIFT_FWCH),  // @
        KeyEvent.KEYCODE_3 or ('\uff03'.code shl SHIFT_FWCH),  // #
        KeyEvent.KEYCODE_4 or ('\uffe5'.code shl SHIFT_FWCH),  // $ - fullwidth Yuan
        KeyEvent.KEYCODE_5 or ('\uff05'.code shl SHIFT_FWCH),  // %
        KeyEvent.KEYCODE_6 or ('\u2026'.code shl SHIFT_FWCH),  // ^ - Apostrophe
        KeyEvent.KEYCODE_7 or ('\uff06'.code shl SHIFT_FWCH),  // &
        KeyEvent.KEYCODE_8 or ('\uff0a'.code shl SHIFT_FWCH),  // *
        KeyEvent.KEYCODE_9 or ('\uff08'.code shl SHIFT_FWCH),  // (
        KeyEvent.KEYCODE_STAR,
        KeyEvent.KEYCODE_POUND,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_POWER,
        KeyEvent.KEYCODE_CAMERA,
        KeyEvent.KEYCODE_CLEAR,
        KeyEvent.KEYCODE_A,
        KeyEvent.KEYCODE_B or ('\uff3d'.code shl SHIFT_FWCH),  // ]
        KeyEvent.KEYCODE_C or ('\u00a9'.code shl SHIFT_FWCH),  // copyright
        KeyEvent.KEYCODE_D or ('\u3001'.code shl SHIFT_FWCH),  // \\
        KeyEvent.KEYCODE_E or ('_'.code shl SHIFT_FWCH),  // _
        KeyEvent.KEYCODE_F or ('\uff5b'.code shl SHIFT_FWCH),  // {
        KeyEvent.KEYCODE_G or ('\uff5d'.code shl SHIFT_FWCH),  // }
        KeyEvent.KEYCODE_H or ('\uff1a'.code shl SHIFT_FWCH),  // :
        KeyEvent.KEYCODE_I or ('\uff0d'.code shl SHIFT_FWCH),  // -
        KeyEvent.KEYCODE_J or ('\uff1b'.code shl SHIFT_FWCH),  // ;
        KeyEvent.KEYCODE_K or ('\u201c'.code shl SHIFT_FWCH),  // "
        KeyEvent.KEYCODE_L or ('\u2019'.code shl SHIFT_FWCH),  // '
        KeyEvent.KEYCODE_M or ('\u300b'.code shl SHIFT_FWCH),  // > - French quotes
        KeyEvent.KEYCODE_N or ('\u300a'.code shl SHIFT_FWCH),  // < - French quotes
        KeyEvent.KEYCODE_O or ('\uff0b'.code shl SHIFT_FWCH),  // +
        KeyEvent.KEYCODE_P or ('\uff1d'.code shl SHIFT_FWCH),  // =
        KeyEvent.KEYCODE_Q or ('\t'.code shl SHIFT_FWCH),  // \t
        KeyEvent.KEYCODE_R or ('\u00ae'.code shl SHIFT_FWCH),  // trademark
        KeyEvent.KEYCODE_S or ('\uff5c'.code shl SHIFT_FWCH),  // |
        KeyEvent.KEYCODE_T or ('\u20ac'.code shl SHIFT_FWCH),  //
        KeyEvent.KEYCODE_U or ('\u00d7'.code shl SHIFT_FWCH),  // multiplier
        KeyEvent.KEYCODE_V or ('\uff3b'.code shl SHIFT_FWCH),  // [
        KeyEvent.KEYCODE_W or ('\uff40'.code shl SHIFT_FWCH),  // `
        KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_Y or ('\u00f7'.code shl SHIFT_FWCH),
        KeyEvent.KEYCODE_Z,
        KeyEvent.KEYCODE_COMMA or ('\uff1f'.code shl SHIFT_FWCH),
        KeyEvent.KEYCODE_PERIOD or ('\uff0f'.code shl SHIFT_FWCH),
        KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT,
        KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT,
        KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_SYM,
        KeyEvent.KEYCODE_EXPLORER, KeyEvent.KEYCODE_ENVELOPE,
        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DEL,
        KeyEvent.KEYCODE_GRAVE, KeyEvent.KEYCODE_MINUS,
        KeyEvent.KEYCODE_EQUALS, KeyEvent.KEYCODE_LEFT_BRACKET,
        KeyEvent.KEYCODE_RIGHT_BRACKET, KeyEvent.KEYCODE_BACKSLASH,
        KeyEvent.KEYCODE_SEMICOLON, KeyEvent.KEYCODE_APOSTROPHE,
        KeyEvent.KEYCODE_SLASH,
        KeyEvent.KEYCODE_AT or ('\uff5e'.code shl SHIFT_FWCH),
        KeyEvent.KEYCODE_NUM, KeyEvent.KEYCODE_HEADSETHOOK,
        KeyEvent.KEYCODE_FOCUS, KeyEvent.KEYCODE_PLUS,
        KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_NOTIFICATION,
        KeyEvent.KEYCODE_SEARCH,
    )

    fun getChineseLabel(keyCode: Int): Char {
        if (keyCode <= 0 || keyCode >= KeyEvent.getMaxKeyCode()) return 0.toChar()
        assert((mKeyMap[keyCode] and 0x000000ff) == keyCode)
        return (mKeyMap[keyCode] shr SHIFT_FWCH).toChar()
    }
}
