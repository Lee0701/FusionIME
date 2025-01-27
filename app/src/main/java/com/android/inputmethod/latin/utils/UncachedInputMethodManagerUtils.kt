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
package com.android.inputmethod.latin.utils

import android.content.Context
import android.provider.Settings.Secure
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager

/*
 * A utility class for {@link InputMethodManager}. Unlike {@link RichInputMethodManager}, this
 * class provides synchronous, non-cached access to {@link InputMethodManager}. The setup activity
 * is a good example to use this class because {@link InputMethodManagerService} may not be aware of
 * this IME immediately after this IME is installed.
 */
object UncachedInputMethodManagerUtils {
    /**
     * Check if the IME specified by the context is enabled.
     * CAVEAT: This may cause a round trip IPC.
     *
     * @param context package context of the IME to be checked.
     * @param imm the [InputMethodManager].
     * @return true if this IME is enabled.
     */
    fun isThisImeEnabled(
        context: Context,
        imm: InputMethodManager
    ): Boolean {
        val packageName = context.packageName
        for (imi in imm.enabledInputMethodList) {
            if (packageName == imi.packageName) {
                return true
            }
        }
        return false
    }

    /**
     * Check if the IME specified by the context is the current IME.
     * CAVEAT: This may cause a round trip IPC.
     *
     * @param context package context of the IME to be checked.
     * @param imm the [InputMethodManager].
     * @return true if this IME is the current IME.
     */
    fun isThisImeCurrent(
        context: Context,
        imm: InputMethodManager
    ): Boolean {
        val imi = getInputMethodInfoOf(context.packageName, imm)
        val currentImeId = Secure.getString(
            context.contentResolver, Secure.DEFAULT_INPUT_METHOD
        )
        return imi != null && imi.id == currentImeId
    }

    /**
     * Get [InputMethodInfo] of the IME specified by the package name.
     * CAVEAT: This may cause a round trip IPC.
     *
     * @param packageName package name of the IME.
     * @param imm the [InputMethodManager].
     * @return the [InputMethodInfo] of the IME specified by the `packageName`,
     * or null if not found.
     */
    fun getInputMethodInfoOf(
        packageName: String,
        imm: InputMethodManager
    ): InputMethodInfo? {
        for (imi in imm.inputMethodList) {
            if (packageName == imi.packageName) {
                return imi
            }
        }
        return null
    }
}
