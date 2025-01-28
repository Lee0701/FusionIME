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

import android.content.Context
import android.content.res.Resources
import android.text.TextUtils
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.utils.RunInLocale
import com.android.inputmethod.latin.utils.SubtypeLocaleUtils
import java.util.Locale
import kotlin.math.min

// TODO: Make this an immutable class.
class KeyboardTextsSet {
    private var mResources: Resources? = null
    private var mResourceLocale: Locale? = null
    private var mResourcePackageName: String? = null
    private var mTextsTable: Array<String?>? = null

    fun setLocale(locale: Locale, context: Context) {
        val res: Resources = context.getResources()
        // Null means the current system locale.
        val resourcePackageName: String = res.getResourcePackageName(
            context.getApplicationInfo().labelRes
        )
        setLocale(locale, res, resourcePackageName)
    }

    @UsedForTesting
    fun setLocale(
        locale: Locale, res: Resources?,
        resourcePackageName: String?
    ) {
        mResources = res
        // Null means the current system locale.
        mResourceLocale = if (SubtypeLocaleUtils.NO_LANGUAGE == locale.toString()) null else locale
        mResourcePackageName = resourcePackageName
        mTextsTable = KeyboardTextsTable.getTextsTable(locale)
    }

    fun getText(name: String): String? {
        return KeyboardTextsTable.getText(name, mTextsTable!!)
    }

    // TODO: Resolve text reference when creating {@link KeyboardTextsTable} class.
    fun resolveTextReference(rawText: String): String? {
        if (TextUtils.isEmpty(rawText)) {
            return null
        }
        var level: Int = 0
        var text: String = rawText
        var sb: StringBuilder?
        do {
            level++
            if (level >= MAX_REFERENCE_INDIRECTION) {
                throw RuntimeException(
                    "Too many " + PREFIX_TEXT + " or " + PREFIX_RESOURCE +
                            " reference indirection: " + text
                )
            }

            val prefixLength: Int = PREFIX_TEXT.length
            val size: Int = text.length
            if (size < prefixLength) {
                break
            }

            sb = null
            var pos: Int = 0
            while (pos < size) {
                val c: Char = text.get(pos)
                if (text.startsWith(PREFIX_TEXT, pos)) {
                    if (sb == null) {
                        sb = StringBuilder(text.substring(0, pos))
                    }
                    pos = expandReference(text, pos, PREFIX_TEXT, sb)
                } else if (text.startsWith(PREFIX_RESOURCE, pos)) {
                    if (sb == null) {
                        sb = StringBuilder(text.substring(0, pos))
                    }
                    pos = expandReference(text, pos, PREFIX_RESOURCE, sb)
                } else if (c == BACKSLASH) {
                    if (sb != null) {
                        // Append both escape character and escaped character.
                        sb.append(
                            text.substring(
                                pos,
                                min((pos + 2).toDouble(), size.toDouble()).toInt()
                            )
                        )
                    }
                    pos++
                } else if (sb != null) {
                    sb.append(c)
                }
                pos++
            }

            if (sb != null) {
                text = sb.toString()
            }
        } while (sb != null)
        return if (TextUtils.isEmpty(text)) null else text
    }

    private fun expandReference(
        text: String, pos: Int, prefix: String,
        sb: StringBuilder
    ): Int {
        val prefixLength: Int = prefix.length
        val end: Int = searchTextNameEnd(text, pos + prefixLength)
        val name: String = text.substring(pos + prefixLength, end)
        if (prefix == PREFIX_TEXT) {
            sb.append(getText(name))
        } else { // PREFIX_RESOURCE
            val resourcePackageName: String? = mResourcePackageName
            val getTextJob: RunInLocale<String> = object : RunInLocale<String>() {
                override fun job(res: Resources): String {
                    val resId: Int = res.getIdentifier(name, "string", resourcePackageName)
                    return res.getString(resId)
                }
            }
            sb.append(getTextJob.runInLocale(mResources!!, mResourceLocale))
        }
        return end - 1
    }

    companion object {
        const val PREFIX_TEXT: String = "!text/"
        private const val PREFIX_RESOURCE: String = "!string/"
        const val SWITCH_TO_ALPHA_KEY_LABEL: String = "keylabel_to_alpha"

        private val BACKSLASH: Char = Constants.CODE_BACKSLASH.toChar()
        private const val MAX_REFERENCE_INDIRECTION: Int = 10

        private fun searchTextNameEnd(text: String, start: Int): Int {
            val size: Int = text.length
            for (pos in start until size) {
                val c: Char = text.get(pos)
                // Label name should be consisted of [a-zA-Z_0-9].
                if ((c >= 'a' && c <= 'z') || c == '_' || (c >= '0' && c <= '9')) {
                    continue
                }
                return pos
            }
            return size
        }
    }
}
