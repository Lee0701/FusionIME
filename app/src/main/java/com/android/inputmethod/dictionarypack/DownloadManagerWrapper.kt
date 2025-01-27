/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.inputmethod.dictionarypack

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.os.ParcelFileDescriptor
import android.util.Log
import com.android.inputmethod.dictionarypack.DownloadManagerWrapper
import java.io.FileNotFoundException

/**
 * A class to help with calling DownloadManager methods.
 *
 * Mostly, the problem here is that most methods from DownloadManager may throw SQL exceptions if
 * they can't open the database on disk. We want to avoid crashing in these cases but can't do
 * much more, so this class insulates the callers from these. SQLiteException also inherit from
 * RuntimeException so they are unchecked :(
 * While we're at it, we also insulate callers from the cases where DownloadManager is disabled,
 * and getSystemService returns null.
 */
class DownloadManagerWrapper private constructor(downloadManager: DownloadManager) {
    private val mDownloadManager: DownloadManager?

    constructor(context: Context) : this(context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager)

    init {
        mDownloadManager = downloadManager
    }

    fun remove(vararg ids: Long) {
        try {
            if (null != mDownloadManager) {
                mDownloadManager.remove(*ids)
            }
        } catch (e: IllegalArgumentException) {
            // This is expected to happen on boot when the device is encrypted.
        } catch (e: SQLiteException) {
            // We couldn't remove the file from DownloadManager. Apparently, the database can't
            // be opened. It may be a problem with file system corruption. In any case, there is
            // not much we can do apart from avoiding crashing.
            Log.e(
                TAG, "Can't remove files with ID " + ids.contentToString() +
                        " from download manager", e
            )
        }
    }

    @Throws(FileNotFoundException::class)
    fun openDownloadedFile(fileId: Long): ParcelFileDescriptor {
        try {
            if (null != mDownloadManager) {
                return mDownloadManager.openDownloadedFile(fileId)
            }
        } catch (e: IllegalArgumentException) {
            // This is expected to happen on boot when the device is encrypted.
        } catch (e: SQLiteException) {
            Log.e(TAG, "Can't open downloaded file with ID " + fileId, e)
        }
        // We come here if mDownloadManager is null or if an exception was thrown.
        throw FileNotFoundException()
    }

    fun query(query: DownloadManager.Query?): Cursor? {
        try {
            if (null != mDownloadManager) {
                return mDownloadManager.query(query)
            }
        } catch (e: IllegalArgumentException) {
            // This is expected to happen on boot when the device is encrypted.
        } catch (e: SQLiteException) {
            Log.e(TAG, "Can't query the download manager", e)
        }
        // We come here if mDownloadManager is null or if an exception was thrown.
        return null
    }

    fun enqueue(request: DownloadManager.Request?): Long {
        try {
            if (null != mDownloadManager) {
                return mDownloadManager.enqueue(request)
            }
        } catch (e: IllegalArgumentException) {
            // This is expected to happen on boot when the device is encrypted.
        } catch (e: SQLiteException) {
            Log.e(TAG, "Can't enqueue a request with the download manager", e)
        }
        return 0
    }

    companion object {
        private val TAG: String = DownloadManagerWrapper::class.java.getSimpleName()
    }
}
