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
package com.android.inputmethod.dictionarypack

import android.app.DownloadManager

/**
 * Struct class to encapsulate the result of a completed download.
 */
class CompletedDownloadInfo(uri: String?, downloadId: Long, status: Int) {
    val mUri: String?
    val mDownloadId: Long
    val mStatus: Int

    init {
        mUri = uri
        mDownloadId = downloadId
        mStatus = status
    }

    fun wasSuccessful(): Boolean {
        return DownloadManager.STATUS_SUCCESSFUL == mStatus
    }
}
