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

import android.text.TextUtils
import android.util.SparseIntArray
import com.android.inputmethod.compat.CharacterCompat
import com.android.inputmethod.keyboard.Key
import com.android.inputmethod.keyboard.internal.KeySpecParser.KeySpecParserError
import com.android.inputmethod.latin.common.CollectionUtils
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.StringUtils
import java.util.Locale
import javax.annotation.Nonnull

/**
 * The more key specification object. The more keys are an array of [MoreKeySpec].
 *
 * The more keys specification is comma separated "key specification" each of which represents one
 * "more key".
 * The key specification might have label or string resource reference in it. These references are
 * expanded before parsing comma.
 * Special character, comma ',' backslash '\' can be escaped by '\' character.
 * Note that the '\' is also parsed by XML parser and [MoreKeySpec.splitKeySpecs]
 * as well.
 */
// TODO: Should extend the key specification object.
class MoreKeySpec(
    @Nonnull moreKeySpec: String, needsToUpperCase: Boolean,
    @Nonnull locale: Locale
) {
    var mCode: Int = 0
    val mLabel: String?
    var mOutputText: String? = null
    val mIconId: Int

    @Nonnull
    fun buildKey(
        x: Int, y: Int, labelFlags: Int,
        @Nonnull params: KeyboardParams
    ): Key {
        return Key(
            mLabel,
            mIconId,
            mCode,
            mOutputText,
            null,  /* hintLabel */
            labelFlags,
            Key.Companion.BACKGROUND_TYPE_NORMAL,
            x,
            y,
            params.mDefaultKeyWidth,
            params.mDefaultRowHeight,
            params.mHorizontalGap,
            params.mVerticalGap
        )
    }

    override fun hashCode(): Int {
        var hashCode: Int = 1
        hashCode = 31 + mCode
        hashCode = hashCode * 31 + mIconId
        val label: String? = mLabel
        hashCode = hashCode * 31 + (if (label == null) 0 else label.hashCode())
        val outputText: String? = mOutputText
        hashCode = hashCode * 31 + (if (outputText == null) 0 else outputText.hashCode())
        return hashCode
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o is MoreKeySpec) {
            val other: MoreKeySpec = o
            return mCode == other.mCode && mIconId == other.mIconId && TextUtils.equals(
                mLabel,
                other.mLabel
            )
                    && TextUtils.equals(mOutputText, other.mOutputText)
        }
        return false
    }

    override fun toString(): String {
        val label: String? = (if (mIconId == KeyboardIconsSet.Companion.ICON_UNDEFINED)
            mLabel
        else
            KeyboardIconsSet.Companion.PREFIX_ICON + KeyboardIconsSet.Companion.getIconName(mIconId))
        val output: String? = (if (mCode == Constants.CODE_OUTPUT_TEXT)
            mOutputText
        else
            Constants.printableCode(mCode))
        if (StringUtils.codePointCount(label) == 1 && label!!.codePointAt(0) == mCode) {
            return output!!
        }
        return label + "|" + output
    }

    class LettersOnBaseLayout {
        private val mCodes: SparseIntArray = SparseIntArray()
        private val mTexts: HashSet<String?> = HashSet()

        fun addLetter(@Nonnull key: Key) {
            val code: Int = key.getCode()
            if (CharacterCompat.isAlphabetic(code)) {
                mCodes.put(code, 0)
            } else if (code == Constants.CODE_OUTPUT_TEXT) {
                mTexts.add(key.getOutputText())
            }
        }

        fun contains(@Nonnull moreKey: MoreKeySpec): Boolean {
            val code: Int = moreKey.mCode
            if (CharacterCompat.isAlphabetic(code) && mCodes.indexOfKey(code) >= 0) {
                return true
            } else if (code == Constants.CODE_OUTPUT_TEXT && mTexts.contains(moreKey.mOutputText)) {
                return true
            }
            return false
        }
    }

    init {
        if (moreKeySpec.isEmpty()) {
            throw KeySpecParserError("Empty more key spec")
        }
        val label: String? = KeySpecParser.getLabel(moreKeySpec)
        mLabel = if (needsToUpperCase) StringUtils.toTitleCaseOfKeyLabel(label, locale) else label
        val codeInSpec: Int = KeySpecParser.getCode(moreKeySpec)
        val code: Int = if (needsToUpperCase)
            StringUtils.toTitleCaseOfKeyCode(codeInSpec, locale)
        else
            codeInSpec
        if (code == Constants.CODE_UNSPECIFIED) {
            // Some letter, for example German Eszett (U+00DF: "ß"), has multiple characters
            // upper case representation ("SS").
            mCode = Constants.CODE_OUTPUT_TEXT
            mOutputText = mLabel
        } else {
            mCode = code
            val outputText: String? = KeySpecParser.getOutputText(moreKeySpec)
            mOutputText = if (needsToUpperCase)
                StringUtils.toTitleCaseOfKeyLabel(outputText, locale)
            else
                outputText
        }
        mIconId = KeySpecParser.getIconId(moreKeySpec)
    }

    companion object {
        fun removeRedundantMoreKeys(
            moreKeys: Array<MoreKeySpec>?,
            @Nonnull lettersOnBaseLayout: LettersOnBaseLayout
        ): Array<MoreKeySpec>? {
            if (moreKeys == null) {
                return null
            }
            val filteredMoreKeys: ArrayList<MoreKeySpec> = ArrayList()
            for (moreKey: MoreKeySpec in moreKeys) {
                if (!lettersOnBaseLayout.contains(moreKey)) {
                    filteredMoreKeys.add(moreKey)
                }
            }
            val size: Int = filteredMoreKeys.size
            if (size == moreKeys.size) {
                return moreKeys
            }
            if (size == 0) {
                return null
            }
            return filteredMoreKeys.toTypedArray<MoreKeySpec>()
        }

        // Constants for parsing.
        private val COMMA: Char = Constants.CODE_COMMA.toChar()
        private val BACKSLASH: Char = Constants.CODE_BACKSLASH.toChar()
        private val ADDITIONAL_MORE_KEY_MARKER: String = StringUtils.newSingleCodePointString(
            Constants.CODE_PERCENT
        )

        /**
         * Split the text containing multiple key specifications separated by commas into an array of
         * key specifications.
         * A key specification can contain a character escaped by the backslash character, including a
         * comma character.
         * Note that an empty key specification will be eliminated from the result array.
         *
         * @param text the text containing multiple key specifications.
         * @return an array of key specification text. Null if the specified `text` is empty
         * or has no key specifications.
         */
        fun splitKeySpecs(text: String?): Array<String>? {
            if (TextUtils.isEmpty(text)) {
                return null
            }
            val size: Int = text!!.length
            // Optimization for one-letter key specification.
            if (size == 1) {
                return if (text.get(0) == COMMA) null else arrayOf<String?>(text)
            }

            var list: ArrayList<String>? = null
            var start: Int = 0
            // The characters in question in this loop are COMMA and BACKSLASH. These characters never
            // match any high or low surrogate character. So it is OK to iterate through with char
            // index.
            var pos: Int = 0
            while (pos < size) {
                val c: Char = text.get(pos)
                if (c == COMMA) {
                    // Skip empty entry.
                    if (pos - start > 0) {
                        if (list == null) {
                            list = ArrayList()
                        }
                        list.add(text.substring(start, pos))
                    }
                    // Skip comma
                    start = pos + 1
                } else if (c == BACKSLASH) {
                    // Skip escape character and escaped character.
                    pos++
                }
                pos++
            }
            val remain: String? = if ((size - start > 0)) text.substring(start) else null
            if (list == null) {
                return if (remain != null) arrayOf(remain) else null
            }
            if (remain != null) {
                list.add(remain)
            }
            return list.toTypedArray<String>()
        }

        @Nonnull
        private val EMPTY_STRING_ARRAY: Array<String?> = arrayOfNulls(0)

        @Nonnull
        private fun filterOutEmptyString(array: Array<String?>?): Array<String?> {
            if (array == null) {
                return EMPTY_STRING_ARRAY
            }
            var out: ArrayList<String?>? = null
            for (i in array.indices) {
                val entry: String? = array.get(i)
                if (TextUtils.isEmpty(entry)) {
                    if (out == null) {
                        out = CollectionUtils.arrayAsList(array, 0, i)
                    }
                } else if (out != null) {
                    out.add(entry)
                }
            }
            if (out == null) {
                return array
            }
            return out.toTypedArray<String?>()
        }

        fun insertAdditionalMoreKeys(
            moreKeySpecs: Array<String?>?,
            additionalMoreKeySpecs: Array<String?>?
        ): Array<String?>? {
            val moreKeys: Array<String?> = filterOutEmptyString(moreKeySpecs)
            val additionalMoreKeys: Array<String?> = filterOutEmptyString(additionalMoreKeySpecs)
            val moreKeysCount: Int = moreKeys.size
            val additionalCount: Int = additionalMoreKeys.size
            var out: ArrayList<String?>? = null
            var additionalIndex: Int = 0
            for (moreKeyIndex in 0 until moreKeysCount) {
                val moreKeySpec: String? = moreKeys.get(moreKeyIndex)
                if (moreKeySpec == ADDITIONAL_MORE_KEY_MARKER) {
                    if (additionalIndex < additionalCount) {
                        // Replace '%' marker with additional more key specification.
                        val additionalMoreKey: String? = additionalMoreKeys.get(additionalIndex)
                        if (out != null) {
                            out.add(additionalMoreKey)
                        } else {
                            moreKeys.get(moreKeyIndex) = additionalMoreKey
                        }
                        additionalIndex++
                    } else {
                        // Filter out excessive '%' marker.
                        if (out == null) {
                            out = CollectionUtils.arrayAsList(moreKeys, 0, moreKeyIndex)
                        }
                    }
                } else {
                    if (out != null) {
                        out.add(moreKeySpec)
                    }
                }
            }
            if (additionalCount > 0 && additionalIndex == 0) {
                // No '%' marker is found in more keys.
                // Insert all additional more keys to the head of more keys.
                out = CollectionUtils.arrayAsList(
                    additionalMoreKeys,
                    additionalIndex,
                    additionalCount
                )
                for (i in 0 until moreKeysCount) {
                    out.add(moreKeys.get(i))
                }
            } else if (additionalIndex < additionalCount) {
                // The number of '%' markers are less than additional more keys.
                // Append remained additional more keys to the tail of more keys.
                out = CollectionUtils.arrayAsList(moreKeys, 0, moreKeysCount)
                for (i in additionalIndex until additionalCount) {
                    out.add(additionalMoreKeys.get(additionalIndex))
                }
            }
            if (out == null && moreKeysCount > 0) {
                return moreKeys
            } else if (out != null && out.size > 0) {
                return out.toTypedArray<String?>()
            } else {
                return null
            }
        }

        fun getIntValue(
            moreKeys: Array<String?>?, key: String,
            defaultValue: Int
        ): Int {
            if (moreKeys == null) {
                return defaultValue
            }
            val keyLen: Int = key.length
            var foundValue: Boolean = false
            var value: Int = defaultValue
            for (i in moreKeys.indices) {
                val moreKeySpec: String? = moreKeys.get(i)
                if (moreKeySpec == null || !moreKeySpec.startsWith(key)) {
                    continue
                }
                moreKeys.get(i) = null
                try {
                    if (!foundValue) {
                        value = moreKeySpec.substring(keyLen).toInt()
                        foundValue = true
                    }
                } catch (e: NumberFormatException) {
                    throw RuntimeException(
                        "integer should follow after " + key + ": " + moreKeySpec
                    )
                }
            }
            return value
        }

        fun getBooleanValue(moreKeys: Array<String?>?, key: String): Boolean {
            if (moreKeys == null) {
                return false
            }
            var value: Boolean = false
            for (i in moreKeys.indices) {
                val moreKeySpec: String? = moreKeys.get(i)
                if (moreKeySpec == null || moreKeySpec != key) {
                    continue
                }
                moreKeys.get(i) = null
                value = true
            }
            return value
        }
    }
}
