/*
 * Copyright (C) 2010 The Android Open Source Project
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
import com.android.inputmethod.latin.common.StringUtils
import javax.annotation.Nonnull

/**
 * The string parser of the key specification.
 *
 * Each key specification is one of the following:
 * - Label optionally followed by keyOutputText (keyLabel|keyOutputText).
 * - Label optionally followed by code point (keyLabel|!code/code_name).
 * - Icon followed by keyOutputText (!icon/icon_name|keyOutputText).
 * - Icon followed by code point (!icon/icon_name|!code/code_name).
 * Label and keyOutputText are one of the following:
 * - Literal string.
 * - Label reference represented by (!text/label_name), see [KeyboardTextsSet].
 * - String resource reference represented by (!text/resource_name), see [KeyboardTextsSet].
 * Icon is represented by (!icon/icon_name), see [KeyboardIconsSet].
 * Code is one of the following:
 * - Code point presented by hexadecimal string prefixed with "0x"
 * - Code reference represented by (!code/code_name), see [KeyboardCodesSet].
 * Special character, comma ',' backslash '\', and bar '|' can be escaped by '\' character.
 * Note that the '\' is also parsed by XML parser and [MoreKeySpec.splitKeySpecs]
 * as well.
 */
// TODO: Rename to KeySpec and make this class to the key specification object.
object KeySpecParser {
    // Constants for parsing.
    private val BACKSLASH: Char = Constants.CODE_BACKSLASH.toChar()
    private val VERTICAL_BAR: Char = Constants.CODE_VERTICAL_BAR.toChar()
    private const val PREFIX_HEX: String = "0x"

    private fun hasIcon(@Nonnull keySpec: String): Boolean {
        return keySpec.startsWith(KeyboardIconsSet.PREFIX_ICON)
    }

    private fun hasCode(@Nonnull keySpec: String, labelEnd: Int): Boolean {
        if (labelEnd <= 0 || labelEnd + 1 >= keySpec.length) {
            return false
        }
        if (keySpec.startsWith(KeyboardCodesSet.PREFIX_CODE, labelEnd + 1)) {
            return true
        }
        // This is a workaround to have a key that has a supplementary code point. We can't put a
        // string in resource as a XML entity of a supplementary code point or a surrogate pair.
        if (keySpec.startsWith(PREFIX_HEX, labelEnd + 1)) {
            return true
        }
        return false
    }

    @Nonnull
    private fun parseEscape(@Nonnull text: String): String {
        if (text.indexOf(BACKSLASH) < 0) {
            return text
        }
        val length: Int = text.length
        val sb: StringBuilder = StringBuilder()
        var pos: Int = 0
        while (pos < length) {
            val c: Char = text.get(pos)
            if (c == BACKSLASH && pos + 1 < length) {
                // Skip escape char
                pos++
                sb.append(text.get(pos))
            } else {
                sb.append(c)
            }
            pos++
        }
        return sb.toString()
    }

    private fun indexOfLabelEnd(@Nonnull keySpec: String): Int {
        val length: Int = keySpec.length
        if (keySpec.indexOf(BACKSLASH) < 0) {
            val labelEnd: Int = keySpec.indexOf(VERTICAL_BAR)
            if (labelEnd == 0) {
                if (length == 1) {
                    // Treat a sole vertical bar as a special case of key label.
                    return -1
                }
                throw KeySpecParserError("Empty label")
            }
            return labelEnd
        }
        var pos: Int = 0
        while (pos < length) {
            val c: Char = keySpec.get(pos)
            if (c == BACKSLASH && pos + 1 < length) {
                // Skip escape char
                pos++
            } else if (c == VERTICAL_BAR) {
                return pos
            }
            pos++
        }
        return -1
    }

    @Nonnull
    private fun getBeforeLabelEnd(@Nonnull keySpec: String, labelEnd: Int): String {
        return if ((labelEnd < 0)) keySpec else keySpec.substring(0, labelEnd)
    }

    @Nonnull
    private fun getAfterLabelEnd(@Nonnull keySpec: String, labelEnd: Int): String {
        return keySpec.substring(labelEnd +  /* VERTICAL_BAR */1)
    }

    private fun checkDoubleLabelEnd(@Nonnull keySpec: String, labelEnd: Int) {
        if (indexOfLabelEnd(getAfterLabelEnd(keySpec, labelEnd)) < 0) {
            return
        }
        throw KeySpecParserError("Multiple " + VERTICAL_BAR + ": " + keySpec)
    }

    fun getLabel(keySpec: String?): String? {
        if (keySpec == null) {
            // TODO: Throw {@link KeySpecParserError} once Key.keyLabel attribute becomes mandatory.
            return null
        }
        if (hasIcon(keySpec)) {
            return null
        }
        val labelEnd: Int = indexOfLabelEnd(keySpec)
        val label: String = parseEscape(getBeforeLabelEnd(keySpec, labelEnd))
        if (label.isEmpty()) {
            throw KeySpecParserError("Empty label: " + keySpec)
        }
        return label
    }

    private fun getOutputTextInternal(@Nonnull keySpec: String, labelEnd: Int): String? {
        if (labelEnd <= 0) {
            return null
        }
        checkDoubleLabelEnd(keySpec, labelEnd)
        return parseEscape(getAfterLabelEnd(keySpec, labelEnd))
    }

    fun getOutputText(keySpec: String?): String? {
        if (keySpec == null) {
            // TODO: Throw {@link KeySpecParserError} once Key.keyLabel attribute becomes mandatory.
            return null
        }
        val labelEnd: Int = indexOfLabelEnd(keySpec)
        if (hasCode(keySpec, labelEnd)) {
            return null
        }
        val outputText: String? = getOutputTextInternal(keySpec, labelEnd)
        if (outputText != null) {
            if (StringUtils.codePointCount(outputText) == 1) {
                // If output text is one code point, it should be treated as a code.
                // See {@link #getCode(Resources, String)}.
                return null
            }
            if (outputText.isEmpty()) {
                throw KeySpecParserError("Empty outputText: " + keySpec)
            }
            return outputText
        }
        val label: String? = getLabel(keySpec)
        if (label == null) {
            throw KeySpecParserError("Empty label: " + keySpec)
        }
        // Code is automatically generated for one letter label. See {@link getCode()}.
        return if ((StringUtils.codePointCount(label) == 1)) null else label
    }

    fun getCode(keySpec: String?): Int {
        if (keySpec == null) {
            // TODO: Throw {@link KeySpecParserError} once Key.keyLabel attribute becomes mandatory.
            return Constants.CODE_UNSPECIFIED
        }
        val labelEnd: Int = indexOfLabelEnd(keySpec)
        if (hasCode(keySpec, labelEnd)) {
            checkDoubleLabelEnd(keySpec, labelEnd)
            return parseCode(getAfterLabelEnd(keySpec, labelEnd), Constants.CODE_UNSPECIFIED)
        }
        val outputText: String? = getOutputTextInternal(keySpec, labelEnd)
        if (outputText != null) {
            // If output text is one code point, it should be treated as a code.
            // See {@link #getOutputText(String)}.
            if (StringUtils.codePointCount(outputText) == 1) {
                return outputText.codePointAt(0)
            }
            return Constants.CODE_OUTPUT_TEXT
        }
        val label: String? = getLabel(keySpec)
        if (label == null) {
            throw KeySpecParserError("Empty label: " + keySpec)
        }
        // Code is automatically generated for one letter label.
        return if ((StringUtils.codePointCount(label) == 1)) label.codePointAt(0) else Constants.CODE_OUTPUT_TEXT
    }

    fun parseCode(text: String?, defaultCode: Int): Int {
        if (text == null) {
            return defaultCode
        }
        if (text.startsWith(KeyboardCodesSet.PREFIX_CODE)) {
            return KeyboardCodesSet.getCode(text.substring(KeyboardCodesSet.PREFIX_CODE.length))
        }
        // This is a workaround to have a key that has a supplementary code point. We can't put a
        // string in resource as a XML entity of a supplementary code point or a surrogate pair.
        if (text.startsWith(PREFIX_HEX)) {
            return text.substring(PREFIX_HEX.length).toInt(16)
        }
        return defaultCode
    }

    fun getIconId(keySpec: String?): Int {
        if (keySpec == null) {
            // TODO: Throw {@link KeySpecParserError} once Key.keyLabel attribute becomes mandatory.
            return KeyboardIconsSet.ICON_UNDEFINED
        }
        if (!hasIcon(keySpec)) {
            return KeyboardIconsSet.ICON_UNDEFINED
        }
        val labelEnd: Int = indexOfLabelEnd(keySpec)
        val iconName: String = getBeforeLabelEnd(keySpec, labelEnd)
            .substring(KeyboardIconsSet.PREFIX_ICON.length)
        return KeyboardIconsSet.getIconId(iconName)
    }

    class KeySpecParserError(message: String?) : RuntimeException(message)
}
