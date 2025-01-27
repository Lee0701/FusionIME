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
package com.android.inputmethod.latin

import java.io.File
import java.math.BigDecimal
import java.util.Locale
import javax.annotation.Nonnull

class DictionaryStats {
    val mLocale: Locale
    val mDictType: String
    val mDictFileName: String?
    val mDictFileSize: Long
    val mContentVersion: Int
    val mWordCount: Int

    constructor(
        @Nonnull locale: Locale,
        @Nonnull dictType: String,
        dictFileName: String?,
        dictFile: File?,
        contentVersion: Int
    ) {
        mLocale = locale
        mDictType = dictType
        mDictFileSize = if ((dictFile == null || !dictFile.exists())) 0 else dictFile.length()
        mDictFileName = dictFileName
        mContentVersion = contentVersion
        mWordCount = -1
    }

    constructor(
        @Nonnull locale: Locale,
        @Nonnull dictType: String,
        wordCount: Int
    ) {
        mLocale = locale
        mDictType = dictType
        mDictFileSize = wordCount.toLong()
        mDictFileName = null
        mContentVersion = 0
        mWordCount = wordCount
    }

    val fileSizeString: String
        get() {
            val bytes: BigDecimal = BigDecimal(mDictFileSize)
            val kb: BigDecimal =
                bytes.divide(BigDecimal(1024), 2, BigDecimal.ROUND_HALF_UP)
            if (kb.toLong() == 0L) {
                return bytes.toString() + " bytes"
            }
            val mb: BigDecimal =
                kb.divide(BigDecimal(1024), 2, BigDecimal.ROUND_HALF_UP)
            if (mb.toLong() == 0L) {
                return kb.toString() + " kb"
            }
            return mb.toString() + " Mb"
        }

    override fun toString(): String {
        val builder: StringBuilder = StringBuilder(mDictType)
        if (mDictType == Dictionary.Companion.TYPE_MAIN) {
            builder.append(" (")
            builder.append(mContentVersion)
            builder.append(")")
        }
        builder.append(": ")
        if (mWordCount > -1) {
            builder.append(mWordCount)
            builder.append(" words")
        } else {
            builder.append(mDictFileName)
            builder.append(" / ")
            builder.append(fileSizeString)
        }
        return builder.toString()
    }

    companion object {
        val NOT_AN_ENTRY_COUNT: Int = -1

        fun toString(stats: Iterable<DictionaryStats>): String {
            val builder: StringBuilder = StringBuilder("LM Stats")
            for (stat: DictionaryStats in stats) {
                builder.append("\n    ")
                builder.append(stat.toString())
            }
            return builder.toString()
        }
    }
}
