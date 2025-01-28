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
package com.android.inputmethod.latin

import com.android.inputmethod.latin.common.FileUtils
import java.io.File

/**
 * Immutable class to hold the address of an asset.
 * As opposed to a normal file, an asset is usually represented as a contiguous byte array in
 * the package file. Open it correctly thus requires the name of the package it is in, but
 * also the offset in the file and the length of this data. This class encapsulates these three.
 */
class AssetFileAddress(filename: String, offset: Long, length: Long) {
    val mFilename: String = filename
    val mOffset: Long = offset
    val mLength: Long = length

    fun pointsToPhysicalFile(): Boolean {
        return 0L == mOffset
    }

    fun deleteUnderlyingFile() {
        FileUtils.deleteRecursively(File(mFilename))
    }

    override fun toString(): String {
        return String.format("%s (offset=%d, length=%d)", mFilename, mOffset, mLength)
    }

    companion object {
        fun makeFromFile(file: File): AssetFileAddress? {
            if (!file.isFile()) return null
            return AssetFileAddress(file.absolutePath, 0L, file.length())
        }

        fun makeFromFileName(filename: String?): AssetFileAddress? {
            if (null == filename) return null
            return makeFromFile(File(filename))
        }

        fun makeFromFileNameAndOffset(
            filename: String?,
            offset: Long, length: Long
        ): AssetFileAddress? {
            if (null == filename) return null
            val f = File(filename)
            if (!f.isFile()) return null
            return AssetFileAddress(filename, offset, length)
        }
    }
}
