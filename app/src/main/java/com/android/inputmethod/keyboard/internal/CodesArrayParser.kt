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
package com.android.inputmethod.keyboard.internal

import android.text.TextUtils
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.StringUtils

/**
 * The string parser of codesArray specification for <GridRows></GridRows>. The attribute codesArray is an
 * array of string.
 * Each element of the array defines a key label by specifying a code point as a hexadecimal string.
 * A key label may consist of multiple code points separated by comma.
 * Each element of the array optionally can have an output text definition after vertical bar
 * marker. An output text may consist of multiple code points separated by comma.
 * The format of the codesArray element should be:
 * <pre>
 * label1[,label2]*(|outputText1[,outputText2]*(|minSupportSdkVersion)?)?
</pre> *
 */
// TODO: Write unit tests for this class.
object CodesArrayParser {
    // Constants for parsing.
    private val COMMA: Char = Constants.CODE_COMMA.toChar()
    private val COMMA_REGEX: String = StringUtils.newSingleCodePointString(
        COMMA.code
    )
    private val VERTICAL_BAR_REGEX: String =
        String(charArrayOf(Constants.CODE_BACKSLASH.toChar(), Constants.CODE_VERTICAL_BAR.toChar()))
    private const val BASE_HEX: Int = 16

    private fun getLabelSpec(codesArraySpec: String): String {
        val strs: Array<String> = codesArraySpec.split(VERTICAL_BAR_REGEX.toRegex()).toTypedArray()
        if (strs.size <= 1) {
            return codesArraySpec
        }
        return strs.get(0)
    }

    fun parseLabel(codesArraySpec: String): String {
        val labelSpec: String = getLabelSpec(codesArraySpec)
        val sb: StringBuilder = StringBuilder()
        for (codeInHex: String in labelSpec.split(COMMA_REGEX.toRegex())
            .dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val codePoint: Int = codeInHex.toInt(BASE_HEX)
            sb.appendCodePoint(codePoint)
        }
        return sb.toString()
    }

    private fun getCodeSpec(codesArraySpec: String): String {
        val strs: Array<String> = codesArraySpec.split(VERTICAL_BAR_REGEX.toRegex()).toTypedArray()
        if (strs.size <= 1) {
            return codesArraySpec
        }
        return if (TextUtils.isEmpty(strs.get(1))) strs.get(0) else strs.get(1)
    }

    fun getMinSupportSdkVersion(codesArraySpec: String): Int {
        val strs: Array<String> = codesArraySpec.split(VERTICAL_BAR_REGEX.toRegex()).toTypedArray()
        if (strs.size <= 2) {
            return 0
        }
        try {
            return strs.get(2).toInt()
        } catch (e: NumberFormatException) {
            return 0
        }
    }

    fun parseCode(codesArraySpec: String): Int {
        val codeSpec: String = getCodeSpec(codesArraySpec)
        if (codeSpec.indexOf(COMMA) < 0) {
            return codeSpec.toInt(BASE_HEX)
        }
        return Constants.CODE_OUTPUT_TEXT
    }

    fun parseOutputText(codesArraySpec: String): String? {
        val codeSpec: String = getCodeSpec(codesArraySpec)
        if (codeSpec.indexOf(COMMA) < 0) {
            return null
        }
        val sb: StringBuilder = StringBuilder()
        for (codeInHex: String in codeSpec.split(COMMA_REGEX.toRegex())
            .dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val codePoint: Int = codeInHex.toInt(BASE_HEX)
            sb.appendCodePoint(codePoint)
        }
        return sb.toString()
    }
}
