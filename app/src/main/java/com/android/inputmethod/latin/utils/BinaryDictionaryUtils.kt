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

import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.latin.BinaryDictionary
import com.android.inputmethod.latin.common.StringUtils
import com.android.inputmethod.latin.makedict.DictionaryHeader
import com.android.inputmethod.latin.makedict.UnsupportedFormatException
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

object BinaryDictionaryUtils {
    private val TAG: String = BinaryDictionaryUtils::class.java.simpleName

    init {
        JniUtils.loadNativeLibrary()
    }

    @UsedForTesting
    private external fun createEmptyDictFileNative(
        filePath: String,
        dictVersion: Long,
        locale: String,
        attributeKeyStringArray: Array<String?>,
        attributeValueStringArray: Array<String?>
    ): Boolean

    private external fun calcNormalizedScoreNative(
        before: IntArray?,
        after: IntArray?,
        score: Int
    ): Float

    private external fun setCurrentTimeForTestNative(currentTime: Int): Int

    @Throws(IOException::class, UnsupportedFormatException::class)
    fun getHeader(dictFile: File): DictionaryHeader {
        return getHeaderWithOffsetAndLength(dictFile, 0,  /* offset */dictFile.length())
    }

    @Throws(IOException::class, UnsupportedFormatException::class)
    fun getHeaderWithOffsetAndLength(
        dictFile: File,
        offset: Long,
        length: Long
    ): DictionaryHeader {
        // dictType is never used for reading the header. Passing an empty string.
        val binaryDictionary = BinaryDictionary(
            dictFile.absolutePath, offset, length,
            true,  /* useFullEditDistance */null,  /* locale */"",  /* dictType */
            false /* isUpdatable */
        )
        val header = binaryDictionary.header
        binaryDictionary.close()
        if (header == null) {
            throw IOException()
        }
        return header
    }

    fun renameDict(dictFile: File, newDictFile: File): Boolean {
        if (dictFile.isFile) {
            return dictFile.renameTo(newDictFile)
        } else if (dictFile.isDirectory) {
            val dictName = dictFile.name
            val newDictName = newDictFile.name
            if (newDictFile.exists()) {
                return false
            }
            for (file in dictFile.listFiles()) {
                if (!file.isFile) {
                    continue
                }
                val fileName = file.name
                val newFileName = fileName.replaceFirst(
                    Pattern.quote(dictName).toRegex(),
                    Matcher.quoteReplacement(newDictName)
                )
                if (!file.renameTo(File(dictFile, newFileName))) {
                    return false
                }
            }
            return dictFile.renameTo(newDictFile)
        }
        return false
    }

    @UsedForTesting
    fun createEmptyDictFile(
        filePath: String, dictVersion: Long,
        locale: Locale, attributeMap: Map<String?, String?>
    ): Boolean {
        val keyArray = arrayOfNulls<String>(attributeMap.size)
        val valueArray = arrayOfNulls<String>(attributeMap.size)
        var index = 0
        for (key in attributeMap.keys) {
            keyArray[index] = key
            valueArray[index] = attributeMap[key]
            index++
        }
        return createEmptyDictFileNative(
            filePath, dictVersion, locale.toString(), keyArray,
            valueArray
        )
    }

    fun calcNormalizedScore(
        before: String, after: String,
        score: Int
    ): Float {
        return calcNormalizedScoreNative(
            StringUtils.toCodePointArray(before),
            StringUtils.toCodePointArray(after), score
        )
    }

    /**
     * Control the current time to be used in the native code. If currentTime >= 0, this method sets
     * the current time and gets into test mode.
     * In test mode, set timestamp is used as the current time in the native code.
     * If currentTime < 0, quit the test mode and returns to using time() to get the current time.
     *
     * @param currentTime seconds since the unix epoch
     * @return current time got in the native code.
     */
    @UsedForTesting
    fun setCurrentTimeForTest(currentTime: Int): Int {
        return setCurrentTimeForTestNative(currentTime)
    }
}
