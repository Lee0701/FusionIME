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
package com.android.inputmethod.compat

import android.view.inputmethod.EditorInfo
import java.lang.reflect.Field
import java.util.Locale

object EditorInfoCompatUtils {
    // Note that EditorInfo.IME_FLAG_FORCE_ASCII has been introduced
    // in API level 16 (Build.VERSION_CODES.JELLY_BEAN).
    private val FIELD_IME_FLAG_FORCE_ASCII: Field? = CompatUtils.getField(
        EditorInfo::class.java, "IME_FLAG_FORCE_ASCII"
    )
    private val OBJ_IME_FLAG_FORCE_ASCII: Int? = CompatUtils.getFieldValue(
        null,  /* receiver */null,  /* defaultValue */FIELD_IME_FLAG_FORCE_ASCII
    ) as Int?
    private val FIELD_HINT_LOCALES: Field? = CompatUtils.getField(
        EditorInfo::class.java, "hintLocales"
    )

    fun hasFlagForceAscii(imeOptions: Int): Boolean {
        if (OBJ_IME_FLAG_FORCE_ASCII == null) return false
        return (imeOptions and OBJ_IME_FLAG_FORCE_ASCII) != 0
    }

    fun imeActionName(imeOptions: Int): String {
        val actionId: Int = imeOptions and EditorInfo.IME_MASK_ACTION
        when (actionId) {
            EditorInfo.IME_ACTION_UNSPECIFIED -> return "actionUnspecified"
            EditorInfo.IME_ACTION_NONE -> return "actionNone"
            EditorInfo.IME_ACTION_GO -> return "actionGo"
            EditorInfo.IME_ACTION_SEARCH -> return "actionSearch"
            EditorInfo.IME_ACTION_SEND -> return "actionSend"
            EditorInfo.IME_ACTION_NEXT -> return "actionNext"
            EditorInfo.IME_ACTION_DONE -> return "actionDone"
            EditorInfo.IME_ACTION_PREVIOUS -> return "actionPrevious"
            else -> return "actionUnknown(" + actionId + ")"
        }
    }

    fun imeOptionsName(imeOptions: Int): String {
        val action: String = imeActionName(imeOptions)
        val flags: StringBuilder = StringBuilder()
        if ((imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
            flags.append("flagNoEnterAction|")
        }
        if ((imeOptions and EditorInfo.IME_FLAG_NAVIGATE_NEXT) != 0) {
            flags.append("flagNavigateNext|")
        }
        if ((imeOptions and EditorInfo.IME_FLAG_NAVIGATE_PREVIOUS) != 0) {
            flags.append("flagNavigatePrevious|")
        }
        if (hasFlagForceAscii(imeOptions)) {
            flags.append("flagForceAscii|")
        }
        return if ((action != null)) flags.toString() + action else flags.toString()
    }

    fun getPrimaryHintLocale(editorInfo: EditorInfo?): Locale? {
        if (editorInfo == null) {
            return null
        }
        val localeList: Any? = CompatUtils.getFieldValue(editorInfo, null, FIELD_HINT_LOCALES)
        if (localeList == null) {
            return null
        }
        if (LocaleListCompatUtils.isEmpty(localeList)) {
            return null
        }
        return LocaleListCompatUtils.get(localeList, 0)
    }
}
