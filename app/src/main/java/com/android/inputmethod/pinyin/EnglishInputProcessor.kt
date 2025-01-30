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
import android.view.inputmethod.InputConnection

/**
 * Class to handle English input.
 */
class EnglishInputProcessor {
    private var mLastKeyCode: Int = KeyEvent.KEYCODE_UNKNOWN

    fun processKey(
        inputContext: InputConnection?, event: KeyEvent?,
        upperCase: Boolean, realAction: Boolean
    ): Boolean {
        if (null == inputContext || null == event) return false

        val keyCode: Int = event.getKeyCode()

        var prefix: CharSequence? = null
        prefix = inputContext.getTextBeforeCursor(2, 0)

        var keyChar: Int
        keyChar = 0
        if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
            keyChar = keyCode - KeyEvent.KEYCODE_A + 'a'.code
            if (upperCase) {
                keyChar = keyChar + 'A'.code - 'a'.code
            }
        } else if (keyCode >= KeyEvent.KEYCODE_0
            && keyCode <= KeyEvent.KEYCODE_9
        ) keyChar = keyCode - KeyEvent.KEYCODE_0 + '0'.code
        else if (keyCode == KeyEvent.KEYCODE_COMMA) keyChar = ','.code
        else if (keyCode == KeyEvent.KEYCODE_PERIOD) keyChar = '.'.code
        else if (keyCode == KeyEvent.KEYCODE_APOSTROPHE) keyChar = '\''.code
        else if (keyCode == KeyEvent.KEYCODE_AT) keyChar = '@'.code
        else if (keyCode == KeyEvent.KEYCODE_SLASH) keyChar = '/'.code

        if (0 == keyChar) {
            mLastKeyCode = keyCode

            var insert: String? = null
            if (KeyEvent.KEYCODE_DEL == keyCode) {
                if (realAction) {
                    inputContext.deleteSurroundingText(1, 0)
                }
            } else if (KeyEvent.KEYCODE_ENTER == keyCode) {
                insert = "\n"
            } else if (KeyEvent.KEYCODE_SPACE == keyCode) {
                insert = " "
            } else {
                return false
            }

            if (null != insert && realAction) inputContext.commitText(insert, insert.length)

            return true
        }

        if (!realAction) return true

        if (KeyEvent.KEYCODE_SHIFT_LEFT == mLastKeyCode
            || KeyEvent.KEYCODE_SHIFT_LEFT == mLastKeyCode
        ) {
            if (keyChar >= 'a'.code && keyChar <= 'z'.code) keyChar = keyChar - 'a'.code + 'A'.code
        } else if (KeyEvent.KEYCODE_ALT_LEFT == mLastKeyCode) {
        }

        val result: String = keyChar.toChar().toString()
        inputContext.commitText(result, result.length)
        mLastKeyCode = keyCode
        return true
    }
}
