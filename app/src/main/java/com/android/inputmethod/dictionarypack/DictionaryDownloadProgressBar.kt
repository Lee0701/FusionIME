/**
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.android.inputmethod.dictionarypack

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.widget.ProgressBar
import com.android.inputmethod.dictionarypack.DictionaryDownloadProgressBar

class DictionaryDownloadProgressBar : ProgressBar {
    private var mClientId: String? = null
    private var mWordlistId: String? = null
    private var mIsCurrentlyAttachedToWindow: Boolean = false
    private var mReporterThread: Thread? = null

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    fun setIds(clientId: String?, wordlistId: String?) {
        mClientId = clientId
        mWordlistId = wordlistId
    }

    /*
     * This method will stop any running updater thread for this progress bar and create and run
     * a new one only if the progress bar is visible.
     * Hence, as a result of calling this method, the progress bar will have an updater thread
     * running if and only if the progress bar is visible.
     */
    private fun updateReporterThreadRunningStatusAccordingToVisibility() {
        if (null != mReporterThread) mReporterThread!!.interrupt()
        if (mIsCurrentlyAttachedToWindow && VISIBLE == getVisibility()) {
            val downloadManagerPendingId: Int =
                getDownloadManagerPendingIdFromWordlistId(getContext(), mClientId, mWordlistId)
            if (NOT_A_DOWNLOADMANAGER_PENDING_ID == downloadManagerPendingId) {
                // Can't get the ID. This is never supposed to happen, but still clear the updater
                // thread and return to avoid a crash.
                mReporterThread = null
                return
            }
            val updaterThread: UpdaterThread =
                UpdaterThread(getContext(), downloadManagerPendingId)
            updaterThread.start()
            mReporterThread = updaterThread
        } else {
            // We're not going to restart the thread anyway, so we may as well garbage collect it.
            mReporterThread = null
        }
    }

    override fun onAttachedToWindow() {
        mIsCurrentlyAttachedToWindow = true
        updateReporterThreadRunningStatusAccordingToVisibility()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mIsCurrentlyAttachedToWindow = false
        updateReporterThreadRunningStatusAccordingToVisibility()
    }

    private inner class UpdaterThread(context: Context, id: Int) : Thread() {
        val mDownloadManagerWrapper: DownloadManagerWrapper
        val mId: Int

        init {
            mDownloadManagerWrapper = DownloadManagerWrapper(context)
            mId = id
        }

        override fun run() {
            try {
                val updateHelper: UpdateHelper = UpdateHelper()
                val query: DownloadManager.Query =
                    DownloadManager.Query().setFilterById(mId.toLong())
                setIndeterminate(true)
                while (!isInterrupted()) {
                    val cursor: Cursor? = mDownloadManagerWrapper.query(query)
                    if (null == cursor) {
                        // Can't contact DownloadManager: this should never happen.
                        return
                    }
                    try {
                        if (cursor.moveToNext()) {
                            val columnBytesDownloadedSoFar: Int = cursor.getColumnIndex(
                                DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                            )
                            val bytesDownloadedSoFar: Int =
                                cursor.getInt(columnBytesDownloadedSoFar)
                            updateHelper.setProgressFromAnotherThread(bytesDownloadedSoFar)
                        } else {
                            // Download has finished and DownloadManager has already been asked to
                            // clean up the db entry.
                            updateHelper.setProgressFromAnotherThread(getMax())
                            return
                        }
                    } finally {
                        cursor.close()
                    }
                    sleep(Companion.REPORT_PERIOD.toLong())
                }
            } catch (e: InterruptedException) {
                // Do nothing and terminate normally.
            }
        }

        inner class UpdateHelper : Runnable {
            private var mProgress: Int = 0
            override fun run() {
                setIndeterminate(false)
                setProgress(mProgress)
            }

            fun setProgressFromAnotherThread(progress: Int) {
                if (mProgress != progress) {
                    mProgress = progress
                    // For some unknown reason, setProgress just does not work from a separate
                    // thread, although the code in ProgressBar looks like it should. Thus, we
                    // resort to a runnable posted to the handler of the view.
                    val handler: Handler? = getHandler()
                    // It's possible to come here before this view has been laid out. If so,
                    // just ignore the call - it will be updated again later.
                    if (null == handler) return
                    handler.post(this)
                }
            }
        }

        companion object {
            private const val REPORT_PERIOD: Int = 150 // how often to report progress, in ms
        }
    }

    companion object {
        private val TAG: String = DictionaryDownloadProgressBar::class.java.getSimpleName()
        private const val NOT_A_DOWNLOADMANAGER_PENDING_ID: Int = 0

        private fun getDownloadManagerPendingIdFromWordlistId(
            context: Context,
            clientId: String?, wordlistId: String?
        ): Int {
            val db: SQLiteDatabase = MetadataDbHelper.Companion.getDb(context, clientId)
            val wordlistValues: ContentValues? =
                MetadataDbHelper.Companion.getContentValuesOfLatestAvailableWordlistById(
                    db,
                    wordlistId
                )
            if (null == wordlistValues) {
                // We don't know anything about a word list with this id. Bug? This should never
                // happen, but still return to prevent a crash.
                Log.e(TAG, "Unexpected word list ID: " + wordlistId)
                return NOT_A_DOWNLOADMANAGER_PENDING_ID
            }
            return wordlistValues.getAsInteger(MetadataDbHelper.Companion.PENDINGID_COLUMN)
        }
    }
}
