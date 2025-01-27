/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.inputmethod.latin.common

import java.io.File
import java.io.FilenameFilter

/**
 * A simple class to help with removing directories recursively.
 */
object FileUtils {
    private const val TAG = "FileUtils"

    fun deleteRecursively(path: File): Boolean {
        if (path.isDirectory) {
            val files = path.listFiles()
            if (files != null) {
                for (child in files) {
                    deleteRecursively(child)
                }
            }
        }
        return path.delete()
    }

    fun deleteFilteredFiles(dir: File, fileNameFilter: FilenameFilter?): Boolean {
        if (!dir.isDirectory) {
            return false
        }
        val files = dir.listFiles(fileNameFilter) ?: return false
        var hasDeletedAllFiles = true
        for (file in files) {
            if (!deleteRecursively(file)) {
                hasDeletedAllFiles = false
            }
        }
        return hasDeletedAllFiles
    }

    fun renameTo(fromFile: File, toFile: File): Boolean {
        toFile.delete()
        return fromFile.renameTo(toFile)
    }
}
