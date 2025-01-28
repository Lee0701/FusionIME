/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import com.android.inputmethod.dictionarypack.ActionBatch.DisableAction
import com.android.inputmethod.dictionarypack.ActionBatch.EnableAction
import com.android.inputmethod.dictionarypack.ActionBatch.FinishDeleteAction
import com.android.inputmethod.dictionarypack.ActionBatch.ForgetAction
import com.android.inputmethod.dictionarypack.ActionBatch.InstallAfterDownloadAction
import com.android.inputmethod.dictionarypack.ActionBatch.MakeAvailableAction
import com.android.inputmethod.dictionarypack.ActionBatch.StartDeleteAction
import com.android.inputmethod.dictionarypack.ActionBatch.StartDownloadAction
import com.android.inputmethod.dictionarypack.ActionBatch.UpdateDataAction
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.makedict.FormatSpec
import com.android.inputmethod.latin.utils.ApplicationUtils
import com.android.inputmethod.latin.utils.DebugLogUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.channels.FileChannel
import java.util.Collections
import java.util.LinkedList
import java.util.TreeSet

/**
 * Handler for the update process.
 *
 * This class is in charge of coordinating the update process for the various dictionaries
 * stored in the dictionary pack.
 */
object UpdateHandler {
    val TAG: String = "DictionaryProvider:" + UpdateHandler::class.java.getSimpleName()
    private val DEBUG: Boolean = DictionaryProvider.DEBUG

    // Used to prevent trying to read the id of the downloaded file before it is written
    val sSharedIdProtector: Any = Any()

    // Value used to mean this is not a real DownloadManager downloaded file id
    // DownloadManager uses as an ID numbers returned out of an AUTOINCREMENT column
    // in SQLite, so it should never return anything < 0.
    val NOT_AN_ID: Int = -1
    val MAXIMUM_SUPPORTED_FORMAT_VERSION: Int = FormatSpec.MAXIMUM_SUPPORTED_STATIC_VERSION

    // Arbitrary. Probably good if it's a power of 2, and a couple thousand bytes long.
    private const val FILE_COPY_BUFFER_SIZE: Int = 8192

    // Table fixed values for metadata / downloads
    const val METADATA_NAME: String = "metadata"
    const val METADATA_TYPE: Int = 0
    const val WORDLIST_TYPE: Int = 1

    // Suffix for generated dictionary files
    private const val DICT_FILE_SUFFIX: String = ".dict"

    // Name of the category for the main dictionary
    const val MAIN_DICTIONARY_CATEGORY: String = "main"

    const val TEMP_DICT_FILE_SUB: String = "___"

    // The id for the "dictionary available" notification.
    const val DICT_AVAILABLE_NOTIFICATION_ID: Int = 1

    /**
     * The list of currently registered listeners.
     */
    private val sUpdateEventListeners
            : MutableList<UpdateEventListener> = Collections.synchronizedList(LinkedList())

    /**
     * Register a new listener to be notified of updates.
     *
     * Don't forget to call unregisterUpdateEventListener when done with it, or
     * it will leak the register.
     */
    fun registerUpdateEventListener(listener: UpdateEventListener) {
        sUpdateEventListeners.add(listener)
    }

    /**
     * Unregister a previously registered listener.
     */
    fun unregisterUpdateEventListener(listener: UpdateEventListener) {
        sUpdateEventListeners.remove(listener)
    }

    private const val DOWNLOAD_OVER_METERED_SETTING_PREFS_KEY: String = "downloadOverMetered"

    /**
     * Write the DownloadManager ID of the currently downloading metadata to permanent storage.
     *
     * @param context to open shared prefs
     * @param uri the uri of the metadata
     * @param downloadId the id returned by DownloadManager
     */
    private fun writeMetadataDownloadId(
        context: Context, uri: String?,
        downloadId: Long
    ) {
        MetadataDbHelper.registerMetadataDownloadId(context, uri, downloadId)
    }

    const val DOWNLOAD_OVER_METERED_SETTING_UNKNOWN: Int = 0
    const val DOWNLOAD_OVER_METERED_ALLOWED: Int = 1
    const val DOWNLOAD_OVER_METERED_DISALLOWED: Int = 2

    /**
     * Sets the setting that tells us whether we may download over a metered connection.
     */
    fun setDownloadOverMeteredSetting(
        context: Context,
        shouldDownloadOverMetered: Boolean
    ) {
        val prefs: SharedPreferences? = CommonPreferences.getCommonPreferences(context)
        val editor: SharedPreferences.Editor = prefs!!.edit()
        editor.putInt(
            DOWNLOAD_OVER_METERED_SETTING_PREFS_KEY, if (shouldDownloadOverMetered)
                DOWNLOAD_OVER_METERED_ALLOWED
            else
                DOWNLOAD_OVER_METERED_DISALLOWED
        )
        editor.apply()
    }

    /**
     * Gets the setting that tells us whether we may download over a metered connection.
     *
     * This returns one of the constants above.
     */
    fun getDownloadOverMeteredSetting(context: Context): Int {
        val prefs: SharedPreferences? = CommonPreferences.getCommonPreferences(context)
        val setting: Int = prefs!!.getInt(
            DOWNLOAD_OVER_METERED_SETTING_PREFS_KEY,
            DOWNLOAD_OVER_METERED_SETTING_UNKNOWN
        )
        return setting
    }

    /**
     * Download latest metadata from the server through DownloadManager for all known clients
     * @param context The context for retrieving resources
     * @return true if an update successfully started, false otherwise.
     */
    fun tryUpdate(context: Context): Boolean {
        // TODO: loop through all clients instead of only doing the default one.
        val uris: TreeSet<String?> = TreeSet()
        val cursor: Cursor = MetadataDbHelper.queryClientIds(context)
        if (null == cursor) return false
        try {
            if (!cursor.moveToFirst()) return false
            do {
                val clientId: String = cursor.getString(0)
                val metadataUri: String? =
                    MetadataDbHelper.getMetadataUriAsString(context, clientId)
                PrivateLog.log("Update for clientId " + DebugLogUtils.s(clientId))
                DebugLogUtils.l("Update for clientId", clientId, " which uses URI ", metadataUri)
                uris.add(metadataUri)
            } while (cursor.moveToNext())
        } finally {
            cursor.close()
        }
        var started: Boolean = false
        for (metadataUri: String? in uris) {
            if (!TextUtils.isEmpty(metadataUri)) {
                // If the metadata URI is empty, that means we should never update it at all.
                // It should not be possible to come here with a null metadata URI, because
                // it should have been rejected at the time of client registration; if there
                // is a bug and it happens anyway, doing nothing is the right thing to do.
                // For more information, {@see DictionaryProvider#insert(Uri, ContentValues)}.
                updateClientsWithMetadataUri(context, metadataUri)
                started = true
            }
        }
        return started
    }

    /**
     * Download latest metadata from the server through DownloadManager for all relevant clients
     *
     * @param context The context for retrieving resources
     * @param metadataUri The client to update
     */
    private fun updateClientsWithMetadataUri(
        context: Context, metadataUri: String?
    ) {
        Log.i(TAG, "updateClientsWithMetadataUri() : MetadataUri = " + metadataUri)
        // Adding a disambiguator to circumvent a bug in older versions of DownloadManager.
        // DownloadManager also stupidly cuts the extension to replace with its own that it
        // gets from the content-type. We need to circumvent this.
        val disambiguator: String = ("#" + System.currentTimeMillis()
                + ApplicationUtils.getVersionName(context) + ".json")
        val metadataRequest: DownloadManager.Request =
            DownloadManager.Request(Uri.parse(metadataUri + disambiguator))
        DebugLogUtils.l("Request =", metadataRequest)

        val res: Resources = context.getResources()
        metadataRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        metadataRequest.setTitle(res.getString(R.string.download_description))
        // Do not show the notification when downloading the metadata.
        metadataRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
        metadataRequest.setVisibleInDownloadsUi(
            res.getBoolean(R.bool.metadata_downloads_visible_in_download_UI)
        )

        val manager: DownloadManagerWrapper = DownloadManagerWrapper(context)
        if (maybeCancelUpdateAndReturnIfStillRunning(
                context, metadataUri, manager,
                DictionaryService.NO_CANCEL_DOWNLOAD_PERIOD_MILLIS
            )
        ) {
            // We already have a recent download in progress. Don't register a new download.
            return
        }
        val downloadId: Long
        synchronized(sSharedIdProtector) {
            downloadId = manager.enqueue(metadataRequest)
            DebugLogUtils.l("Metadata download requested with id", downloadId)
            // If there is still a download in progress, it's been there for a while and
            // there is probably something wrong with download manager. It's best to just
            // overwrite the id and request it again. If the old one happens to finish
            // anyway, we don't know about its ID any more, so the downloadFinished
            // method will ignore it.
            writeMetadataDownloadId(context, metadataUri, downloadId)
        }
        Log.i(TAG, "updateClientsWithMetadataUri() : DownloadId = " + downloadId)
    }

    /**
     * Cancels downloading a file if there is one for this URI and it's too long.
     *
     * If we are not currently downloading the file at this URI, this is a no-op.
     *
     * @param context the context to open the database on
     * @param metadataUri the URI to cancel
     * @param manager an wrapped instance of DownloadManager
     * @param graceTime if there was a download started less than this many milliseconds, don't
     * cancel and return true
     * @return whether the download is still active
     */
    private fun maybeCancelUpdateAndReturnIfStillRunning(
        context: Context,
        metadataUri: String?, manager: DownloadManagerWrapper, graceTime: Long
    ): Boolean {
        synchronized(sSharedIdProtector) {
            val metadataDownloadIdAndStartDate: DownloadIdAndStartDate? =
                MetadataDbHelper.getMetadataDownloadIdAndStartDateForURI(
                    context,
                    metadataUri
                )
            if (null == metadataDownloadIdAndStartDate) return false
            if (NOT_AN_ID.toLong() == metadataDownloadIdAndStartDate.mId) return false
            if (metadataDownloadIdAndStartDate.mStartDate + graceTime
                > System.currentTimeMillis()
            ) {
                return true
            }
            manager.remove(metadataDownloadIdAndStartDate.mId)
            writeMetadataDownloadId(context, metadataUri, NOT_AN_ID.toLong())
        }
        // Consider a cancellation as a failure. As such, inform listeners that the download
        // has failed.
        for (listener: UpdateEventListener in linkedCopyOfList(sUpdateEventListeners)) {
            listener.downloadedMetadata(false)
        }
        return false
    }

    /**
     * Cancels a pending update for this client, if there is one.
     *
     * If we are not currently updating metadata for this client, this is a no-op. This is a helper
     * method that gets the download manager service and the metadata URI for this client.
     *
     * @param context the context, to get an instance of DownloadManager
     * @param clientId the ID of the client we want to cancel the update of
     */
    fun cancelUpdate(context: Context, clientId: String?) {
        val manager: DownloadManagerWrapper = DownloadManagerWrapper(context)
        val metadataUri: String? =
            MetadataDbHelper.getMetadataUriAsString(context, clientId)
        maybeCancelUpdateAndReturnIfStillRunning(context, metadataUri, manager, 0 /* graceTime */)
    }

    /**
     * Registers a download request and flags it as downloading in the metadata table.
     *
     * This is a helper method that exists to avoid race conditions where DownloadManager might
     * finish downloading the file before the data is committed to the database.
     * It registers the request with the DownloadManager service and also updates the metadata
     * database directly within a synchronized section.
     * This method has no intelligence about the data it commits to the database aside from the
     * download request id, which is not known before submitting the request to the download
     * manager. Hence, it only updates the relevant line.
     *
     * @param manager a wrapped download manager service to register the request with.
     * @param request the request to register.
     * @param db the metadata database.
     * @param id the id of the word list.
     * @param version the version of the word list.
     * @return the download id returned by the download manager.
     */
    fun registerDownloadRequest(
        manager: DownloadManagerWrapper,
        request: DownloadManager.Request?, db: SQLiteDatabase, id: String?, version: Int
    ): Long {
        Log.i(TAG, "registerDownloadRequest() : Id = " + id + " : Version = " + version)
        val downloadId: Long
        synchronized(sSharedIdProtector) {
            downloadId = manager.enqueue(request)
            Log.i(TAG, "registerDownloadRequest() : DownloadId = " + downloadId)
            MetadataDbHelper.markEntryAsDownloading(db, id, version, downloadId)
        }
        return downloadId
    }

    /**
     * Retrieve information about a specific download from DownloadManager.
     */
    private fun getCompletedDownloadInfo(
        manager: DownloadManagerWrapper, downloadId: Long
    ): CompletedDownloadInfo {
        val query: DownloadManager.Query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = manager.query(query)

        if (null == cursor) {
            return CompletedDownloadInfo(null, downloadId, DownloadManager.STATUS_FAILED)
        }
        try {
            val uri: String?
            val status: Int
            if (cursor.moveToNext()) {
                val columnStatus: Int = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val columnError: Int = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                val columnUri: Int = cursor.getColumnIndex(DownloadManager.COLUMN_URI)
                val error: Int = cursor.getInt(columnError)
                status = cursor.getInt(columnStatus)
                val uriWithAnchor: String = cursor.getString(columnUri)
                val anchorIndex: Int = uriWithAnchor.indexOf('#')
                if (anchorIndex != -1) {
                    uri = uriWithAnchor.substring(0, anchorIndex)
                } else {
                    uri = uriWithAnchor
                }
                if (DownloadManager.STATUS_SUCCESSFUL != status) {
                    Log.e(
                        TAG, ("Permanent failure of download " + downloadId
                                + " with error code: " + error)
                    )
                }
            } else {
                uri = null
                status = DownloadManager.STATUS_FAILED
            }
            return CompletedDownloadInfo(uri, downloadId, status)
        } finally {
            cursor.close()
        }
    }

    private fun getDownloadRecordsForCompletedDownloadInfo(
        context: Context, downloadInfo: CompletedDownloadInfo
    ): ArrayList<DownloadRecord> {
        // Get and check the ID of the file we are waiting for, compare them to downloaded ones
        synchronized(sSharedIdProtector) {
            val downloadRecords: ArrayList<DownloadRecord> =
                MetadataDbHelper.getDownloadRecordsForDownloadId(
                    context,
                    downloadInfo.mDownloadId
                )
            // If any of these is metadata, we should update the DB
            var hasMetadata: Boolean = false
            for (record: DownloadRecord in downloadRecords) {
                if (record.isMetadata()) {
                    hasMetadata = true
                    break
                }
            }
            if (hasMetadata) {
                writeMetadataDownloadId(context, downloadInfo.mUri, NOT_AN_ID.toLong())
                MetadataDbHelper.saveLastUpdateTimeOfUri(context, downloadInfo.mUri)
            }
            return downloadRecords
        }
    }

    /**
     * Take appropriate action after a download finished, in success or in error.
     *
     * This is called by the system upon broadcast from the DownloadManager that a file
     * has been downloaded successfully.
     * After a simple check that this is actually the file we are waiting for, this
     * method basically coordinates the parsing and comparison of metadata, and fires
     * the computation of the list of actions that should be taken then executes them.
     *
     * @param context The context for this action.
     * @param intent The intent from the DownloadManager containing details about the download.
     */
    /* package */
    fun downloadFinished(context: Context, intent: Intent) {
        // Get and check the ID of the file that was downloaded
        val fileId: Long =
            intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, NOT_AN_ID.toLong())
        Log.i(TAG, "downloadFinished() : DownloadId = " + fileId)
        if (NOT_AN_ID.toLong() == fileId) return  // Spurious wake-up: ignore


        val manager: DownloadManagerWrapper = DownloadManagerWrapper(context)
        val downloadInfo: CompletedDownloadInfo = getCompletedDownloadInfo(manager, fileId)

        val recordList: ArrayList<DownloadRecord> =
            getDownloadRecordsForCompletedDownloadInfo(context, downloadInfo)
        if (null == recordList) return  // It was someone else's download.

        DebugLogUtils.l("Received result for download ", fileId)

        // TODO: handle gracefully a null pointer here. This is practically impossible because
        // we come here only when DownloadManager explicitly called us when it ended a
        // download, so we are pretty sure it's alive. It's theoretically possible that it's
        // disabled right inbetween the firing of the intent and the control reaching here.
        for (record: DownloadRecord in recordList) {
            // downloadSuccessful is not final because we may still have exceptions from now on
            var downloadSuccessful: Boolean = false
            try {
                if (downloadInfo.wasSuccessful()) {
                    downloadSuccessful = handleDownloadedFile(context, record, manager, fileId)
                    Log.i(TAG, "downloadFinished() : Success = " + downloadSuccessful)
                }
            } finally {
                val resultMessage: String = if (downloadSuccessful) "Success" else "Failure"
                if (record.isMetadata()) {
                    Log.i(TAG, "downloadFinished() : Metadata " + resultMessage)
                    publishUpdateMetadataCompleted(context, downloadSuccessful)
                } else {
                    Log.i(TAG, "downloadFinished() : WordList " + resultMessage)
                    val db: SQLiteDatabase =
                        MetadataDbHelper.getDb(context, record.mClientId)
                    publishUpdateWordListCompleted(
                        context, downloadSuccessful, fileId,
                        db,
                        record.mAttributes!!, record.mClientId
                    )
                }
            }
        }
        // Now that we're done using it, we can remove this download from DLManager
        manager.remove(fileId)
    }

    /**
     * Sends a broadcast informing listeners that the dictionaries were updated.
     *
     * This will call all local listeners through the UpdateEventListener#downloadedMetadata
     * callback (for example, the dictionary provider interface uses this to stop the Loading
     * animation) and send a broadcast about the metadata having been updated. For a client of
     * the dictionary pack like Latin IME, this means it should re-query the dictionary pack
     * for any relevant new data.
     *
     * @param context the context, to send the broadcast.
     * @param downloadSuccessful whether the download of the metadata was successful or not.
     */
    fun publishUpdateMetadataCompleted(
        context: Context,
        downloadSuccessful: Boolean
    ) {
        // We need to warn all listeners of what happened. But some listeners may want to
        // remove themselves or re-register something in response. Hence we should take a
        // snapshot of the listener list and warn them all. This also prevents any
        // concurrent modification problem of the static list.
        for (listener: UpdateEventListener in linkedCopyOfList(sUpdateEventListeners)) {
            listener.downloadedMetadata(downloadSuccessful)
        }
        publishUpdateCycleCompletedEvent(context)
    }

    private fun publishUpdateWordListCompleted(
        context: Context,
        downloadSuccessful: Boolean, fileId: Long,
        db: SQLiteDatabase, downloadedFileRecord: ContentValues,
        clientId: String?
    ) {
        synchronized(sSharedIdProtector) {
            if (downloadSuccessful) {
                val actions: ActionBatch = ActionBatch()
                actions.add(
                    InstallAfterDownloadAction(
                        clientId,
                        downloadedFileRecord
                    )
                )
                actions.execute(context, LogProblemReporter(TAG))
            } else {
                MetadataDbHelper.deleteDownloadingEntry(db, fileId)
            }
        }
        // See comment above about #linkedCopyOfLists
        for (listener: UpdateEventListener in linkedCopyOfList<UpdateEventListener>(
            sUpdateEventListeners
        )) {
            listener.wordListDownloadFinished(
                downloadedFileRecord.getAsString(
                    MetadataDbHelper.WORDLISTID_COLUMN
                ), downloadSuccessful
            )
        }
        publishUpdateCycleCompletedEvent(context)
    }

    private fun publishUpdateCycleCompletedEvent(context: Context) {
        // Even if this is not successful, we have to publish the new state.
        PrivateLog.log("Publishing update cycle completed event")
        DebugLogUtils.l("Publishing update cycle completed event")
        for (listener: UpdateEventListener in linkedCopyOfList(sUpdateEventListeners)) {
            listener.updateCycleCompleted()
        }
        signalNewDictionaryState(context)
    }

    private fun handleDownloadedFile(
        context: Context,
        downloadRecord: DownloadRecord, manager: DownloadManagerWrapper,
        fileId: Long
    ): Boolean {
        try {
            // {@link handleWordList(Context,InputStream,ContentValues)}.
            // Handle the downloaded file according to its type
            if (downloadRecord.isMetadata()) {
                DebugLogUtils.l("Data D/L'd is metadata for", downloadRecord.mClientId)
                // #handleMetadata() closes its InputStream argument
                handleMetadata(
                    context, ParcelFileDescriptor.AutoCloseInputStream(
                        manager.openDownloadedFile(fileId)
                    ), downloadRecord.mClientId
                )
            } else {
                DebugLogUtils.l("Data D/L'd is a word list")
                val wordListStatus: Int = downloadRecord.mAttributes!!.getAsInteger(
                    MetadataDbHelper.STATUS_COLUMN
                )
                if (MetadataDbHelper.STATUS_DOWNLOADING == wordListStatus) {
                    // #handleWordList() closes its InputStream argument
                    handleWordList(
                        context, ParcelFileDescriptor.AutoCloseInputStream(
                            manager.openDownloadedFile(fileId)
                        ), downloadRecord
                    )
                } else {
                    Log.e(TAG, "Spurious download ended. Maybe a cancelled download?")
                }
            }
            return true
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "A file was downloaded but it can't be opened", e)
        } catch (e: IOException) {
            // Can't read the file... disk damage?
            Log.e(TAG, "Can't read a file", e)
            // TODO: Check with UX how we should warn the user.
        } catch (e: IllegalStateException) {
            // The format of the downloaded file is incorrect. We should maybe report upstream?
            Log.e(TAG, "Incorrect data received", e)
        } catch (e: BadFormatException) {
            // The format of the downloaded file is incorrect. We should maybe report upstream?
            Log.e(TAG, "Incorrect data received", e)
        }
        return false
    }

    /**
     * Returns a copy of the specified list, with all elements copied.
     *
     * This returns a linked list.
     */
    private fun <T> linkedCopyOfList(src: List<T>): List<T> {
        // Instantiation of a parameterized type is not possible in Java, so it's not possible to
        // return the same type of list that was passed - probably the same reason why Collections
        // does not do it. So we need to decide statically which concrete type to return.
        return LinkedList(src)
    }

    /**
     * Warn Android Keyboard that the state of dictionaries changed and it should refresh its data.
     */
    private fun signalNewDictionaryState(context: Context) {
        // TODO: Also provide the locale of the updated dictionary so that the LatinIme
        // does not have to reset if it is a different locale.
        val newDictBroadcast: Intent =
            Intent(DictionaryPackConstants.NEW_DICTIONARY_INTENT_ACTION)
        context.sendBroadcast(newDictBroadcast)
    }

    /**
     * Parse metadata and take appropriate action (that is, upgrade dictionaries).
     * @param context the context to read settings.
     * @param stream an input stream pointing to the downloaded data. May not be null.
     * Will be closed upon finishing.
     * @param clientId the ID of the client to update
     * @throws BadFormatException if the metadata is not in a known format.
     * @throws IOException if the downloaded file can't be read from the disk
     */
    @Throws(IOException::class, BadFormatException::class)
    fun handleMetadata(
        context: Context, stream: InputStream?,
        clientId: String?
    ) {
        DebugLogUtils.l("Entering handleMetadata")
        val newMetadata: List<WordListMetadata>
        val reader: InputStreamReader = InputStreamReader(stream)
        try {
            // According to the doc InputStreamReader buffers, so no need to add a buffering layer
            newMetadata = MetadataHandler.readMetadata(reader)
        } finally {
            reader.close()
        }

        DebugLogUtils.l("Downloaded metadata :", newMetadata)
        PrivateLog.log("Downloaded metadata\n" + newMetadata)

        val actions: ActionBatch = computeUpgradeTo(context, clientId, newMetadata)
        // TODO: Check with UX how we should report to the user
        // TODO: add an action to close the database
        actions.execute(context, LogProblemReporter(TAG))
    }

    /**
     * Handle a word list: put it in its right place, and update the passed content values.
     * @param context the context for opening files.
     * @param inputStream an input stream pointing to the downloaded data. May not be null.
     * Will be closed upon finishing.
     * @param downloadRecord the content values to fill the file name in.
     * @throws IOException if files can't be read or written.
     * @throws BadFormatException if the md5 checksum doesn't match the metadata.
     */
    @Throws(IOException::class, BadFormatException::class)
    private fun handleWordList(
        context: Context,
        inputStream: InputStream, downloadRecord: DownloadRecord
    ) {
        // DownloadManager does not have the ability to put the file directly where we want
        // it, so we had it download to a temporary place. Now we move it. It will be deleted
        // automatically by DownloadManager.

        DebugLogUtils.l(
            "Downloaded a new word list :", downloadRecord.mAttributes!!.getAsString(
                MetadataDbHelper.DESCRIPTION_COLUMN
            ), "for", downloadRecord.mClientId
        )
        PrivateLog.log(
            ("Downloaded a new word list with description : "
                    + downloadRecord.mAttributes.getAsString(MetadataDbHelper.DESCRIPTION_COLUMN)
                    + " for " + downloadRecord.mClientId)
        )

        val locale: String =
            downloadRecord.mAttributes.getAsString(MetadataDbHelper.LOCALE_COLUMN)
        val destinationFile: String = getTempFileName(context, locale)
        downloadRecord.mAttributes.put(
            MetadataDbHelper.LOCAL_FILENAME_COLUMN,
            destinationFile
        )

        var outputStream: FileOutputStream? = null
        try {
            outputStream = context.openFileOutput(destinationFile, Context.MODE_PRIVATE)
            copyFile(inputStream, outputStream)
        } finally {
            inputStream.close()
            if (outputStream != null) {
                outputStream.close()
            }
        }

        // TODO: Consolidate this MD5 calculation with file copying above.
        // We need to reopen the file because the inputstream bytes have been consumed, and there
        // is nothing in InputStream to reopen or rewind the stream
        var copiedFile: FileInputStream? = null
        val md5sum: String?
        try {
            copiedFile = context.openFileInput(destinationFile)
            md5sum = MD5Calculator.checksum(copiedFile)
        } finally {
            if (copiedFile != null) {
                copiedFile.close()
            }
        }
        if (TextUtils.isEmpty(md5sum)) {
            return  // We can't compute the checksum anyway, so return and hope for the best
        }
        if (md5sum != downloadRecord.mAttributes.getAsString(
                MetadataDbHelper.CHECKSUM_COLUMN
            )
        ) {
            context.deleteFile(destinationFile)
            throw BadFormatException(
                ("MD5 checksum check failed : \"" + md5sum + "\" <> \""
                        + downloadRecord.mAttributes.getAsString(MetadataDbHelper.CHECKSUM_COLUMN)
                        + "\"")
            )
        }
    }

    /**
     * Copies in to out using FileChannels.
     *
     * This tries to use channels for fast copying. If it doesn't work, fall back to
     * copyFileFallBack below.
     *
     * @param in the stream to copy from.
     * @param out the stream to copy to.
     * @throws IOException if both the normal and fallback methods raise exceptions.
     */
    @Throws(IOException::class)
    private fun copyFile(`in`: InputStream, out: OutputStream) {
        DebugLogUtils.l("Copying files")
        if (`in` !is FileInputStream || out !is FileOutputStream) {
            DebugLogUtils.l("Not the right types")
            copyFileFallback(`in`, out)
        } else {
            try {
                val sourceChannel: FileChannel = `in`.getChannel()
                val destinationChannel: FileChannel = out.getChannel()
                sourceChannel.transferTo(
                    0,
                    Int.MAX_VALUE.toLong(), destinationChannel
                )
            } catch (e: IOException) {
                // Can't work with channels, or something went wrong. Copy by hand.
                DebugLogUtils.l("Won't work")
                copyFileFallback(`in`, out)
            }
        }
    }

    /**
     * Copies in to out with read/write methods, not FileChannels.
     *
     * @param in the stream to copy from.
     * @param out the stream to copy to.
     * @throws IOException if a read or a write fails.
     */
    @Throws(IOException::class)
    private fun copyFileFallback(`in`: InputStream, out: OutputStream) {
        DebugLogUtils.l("Falling back to slow copy")
        val buffer: ByteArray = ByteArray(FILE_COPY_BUFFER_SIZE)
        var readBytes: Int = `in`.read(buffer)
        while (readBytes >= 0) {
            out.write(buffer, 0, readBytes)
            readBytes = `in`.read(buffer)
        }
    }

    /**
     * Creates and returns a new file to store a dictionary
     * @param context the context to use to open the file.
     * @param locale the locale for this dictionary, to make the file name more readable.
     * @return the file name, or throw an exception.
     * @throws IOException if the file cannot be created.
     */
    @Throws(IOException::class)
    private fun getTempFileName(context: Context, locale: String): String {
        DebugLogUtils.l("Entering openTempFileOutput")
        val dir: File = context.getFilesDir()
        val f: File = File.createTempFile(locale + TEMP_DICT_FILE_SUB, DICT_FILE_SUFFIX, dir)
        DebugLogUtils.l("File name is", f.getName())
        return f.getName()
    }

    /**
     * Compare metadata (collections of word lists).
     *
     * This method takes whole metadata sets directly and compares them, matching the wordlists in
     * each of them on the id. It creates an ActionBatch object that can be .execute()'d to perform
     * the actual upgrade from `from' to `to'.
     *
     * @param context the context to open databases on.
     * @param clientId the id of the client.
     * @param from the dictionary descriptor (as a list of wordlists) to upgrade from.
     * @param to the dictionary descriptor (as a list of wordlists) to upgrade to.
     * @return an ordered list of runnables to be called to upgrade.
     */
    private fun compareMetadataForUpgrade(
        context: Context?,
        clientId: String?, from: List<WordListMetadata>?,
        to: List<WordListMetadata>?
    ): ActionBatch {
        val actions: ActionBatch = ActionBatch()
        // Upgrade existing word lists
        DebugLogUtils.l("Comparing dictionaries")
        val wordListIds: MutableSet<String> = TreeSet()
        // TODO: Can these be null?
        val fromList: List<WordListMetadata> = if ((from == null))
            ArrayList()
        else
            from
        val toList: List<WordListMetadata> = if ((to == null))
            ArrayList()
        else
            to
        for (wlData: WordListMetadata in fromList) wordListIds.add(wlData.mId)
        for (wlData: WordListMetadata in toList) wordListIds.add(wlData.mId)
        for (id: String in wordListIds) {
            val currentInfo: WordListMetadata? = MetadataHandler.findWordListById(fromList, id)
            val metadataInfo: WordListMetadata? = MetadataHandler.findWordListById(toList, id)
            // TODO: Remove the following unnecessary check, since we are now doing the filtering
            // inside findWordListById.
            val newInfo: WordListMetadata? = if (null == metadataInfo
                || metadataInfo.mFormatVersion > MAXIMUM_SUPPORTED_FORMAT_VERSION
            )
                null
            else
                metadataInfo
            DebugLogUtils.l("Considering updating ", id, "currentInfo =", currentInfo)

            if (null == currentInfo && null == newInfo) {
                // This may happen if a new word list appeared that we can't handle.
                if (null == metadataInfo) {
                    // What happened? Bug in Set<>?
                    Log.e(TAG, "Got an id for a wordlist that is neither in from nor in to")
                } else {
                    // We may come here if there is a new word list that we can't handle.
                    Log.i(
                        TAG, ("Can't handle word list with id '" + id + "' because it has format"
                                + " version " + metadataInfo.mFormatVersion + " and the maximum version"
                                + " we can handle is " + MAXIMUM_SUPPORTED_FORMAT_VERSION)
                    )
                }
                continue
            } else if (null == currentInfo) {
                // This is the case where a new list that we did not know of popped on the server.
                // Make it available.
                actions.add(MakeAvailableAction(clientId, newInfo))
            } else if (null == newInfo) {
                // This is the case where an old list we had is not in the server data any more.
                // Pass false to ForgetAction: this may be installed and we still want to apply
                // a forget-like action (remove the URL) if it is, so we want to turn off the
                // status == AVAILABLE check. If it's DELETING, this is the right thing to do,
                // as we want to leave the record as long as Android Keyboard has not deleted it ;
                // the record will be removed when the file is actually deleted.
                actions.add(ForgetAction(clientId, currentInfo, false))
            } else {
                val db: SQLiteDatabase = MetadataDbHelper.getDb(context, clientId)
                if (newInfo.mVersion == currentInfo.mVersion) {
                    if (TextUtils.equals(newInfo.mRemoteFilename, currentInfo.mRemoteFilename)) {
                        // If the dictionary url hasn't changed, we should preserve the retryCount.
                        newInfo.mRetryCount = currentInfo.mRetryCount
                    }
                    // If it's the same id/version, we update the DB with the new values.
                    // It doesn't matter too much if they didn't change.
                    actions.add(UpdateDataAction(clientId, newInfo))
                } else if (newInfo.mVersion > currentInfo.mVersion) {
                    // If it's a new version, it's a different entry in the database. Make it
                    // available, and if it's installed, also start the download.
                    val values: ContentValues? =
                        MetadataDbHelper.getContentValuesByWordListId(
                            db,
                            currentInfo.mId, currentInfo.mVersion
                        )
                    val status: Int =
                        values!!.getAsInteger(MetadataDbHelper.STATUS_COLUMN)
                    actions.add(MakeAvailableAction(clientId, newInfo))
                    if (status == MetadataDbHelper.STATUS_INSTALLED
                        || status == MetadataDbHelper.STATUS_DISABLED
                    ) {
                        actions.add(StartDownloadAction(clientId, newInfo))
                    } else {
                        // Pass true to ForgetAction: this is indeed an update to a non-installed
                        // word list, so activate status == AVAILABLE check
                        // In case the status is DELETING, this is the right thing to do. It will
                        // leave the entry as DELETING and remove its URL so that Android Keyboard
                        // can delete it the next time it starts up.
                        actions.add(ForgetAction(clientId, currentInfo, true))
                    }
                } else if (DEBUG) {
                    Log.i(
                        TAG, ("Not updating word list " + id
                                + " : current list timestamp is " + currentInfo.mLastUpdate
                                + " ; new list timestamp is " + newInfo.mLastUpdate)
                    )
                }
            }
        }
        return actions
    }

    /**
     * Computes an upgrade from the current state of the dictionaries to some desired state.
     * @param context the context for reading settings and files.
     * @param clientId the id of the client.
     * @param newMetadata the state we want to upgrade to.
     * @return the upgrade from the current state to the desired state, ready to be executed.
     */
    fun computeUpgradeTo(
        context: Context?, clientId: String?,
        newMetadata: List<WordListMetadata>?
    ): ActionBatch {
        val currentMetadata: List<WordListMetadata> =
            MetadataHandler.getCurrentMetadata(context, clientId)
        return compareMetadataForUpgrade(context, clientId, currentMetadata, newMetadata)
    }

    /**
     * Installs a word list if it has never been requested.
     *
     * This is called when a word list is requested, and is available but not installed. It checks
     * the conditions for auto-installation: if the dictionary is a main dictionary for this
     * language, and it has never been opted out through the dictionary interface, then we start
     * installing it. For the user who enables a language and uses it for the first time, the
     * dictionary should magically start being used a short time after they start typing.
     * The mayPrompt argument indicates whether we should prompt the user for a decision to
     * download or not, in case we decide we are in the case where we should download - this
     * roughly happens when the current connectivity is 3G. See
     * DictionaryProvider#getDictionaryWordListsForContentUri for details.
     */
    // As opposed to many other methods, this method does not need the version of the word
    // list because it may only install the latest version we know about for this specific
    // word list ID / client ID combination.
    fun installIfNeverRequested(
        context: Context, clientId: String?,
        wordlistId: String
    ) {
        Log.i(
            TAG, ("installIfNeverRequested() : ClientId = " + clientId
                    + " : WordListId = " + wordlistId)
        )
        val idArray: Array<String> =
            wordlistId.split(DictionaryProvider.ID_CATEGORY_SEPARATOR.toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()
        // If we have a new-format dictionary id (category:manual_id), then use the
        // specified category. Otherwise, it is a main dictionary, so force the
        // MAIN category upon it.
        val category: String = if (2 == idArray.size) idArray.get(0) else MAIN_DICTIONARY_CATEGORY
        if (MAIN_DICTIONARY_CATEGORY != category) {
            // Not a main dictionary. We only auto-install main dictionaries, so we can return now.
            return
        }
        if (CommonPreferences.getCommonPreferences(context).contains(wordlistId)) {
            // If some kind of settings has been done in the past for this specific id, then
            // this is not a candidate for auto-install. Because it already is either true,
            // in which case it may be installed or downloading or whatever, and we don't
            // need to care about it because it's already handled or being handled, or it's false
            // in which case it means the user explicitely turned it off and don't want to have
            // it installed. So we quit right away.
            return
        }

        val db: SQLiteDatabase = MetadataDbHelper.getDb(context, clientId)
        val installCandidate: ContentValues? =
            MetadataDbHelper.getContentValuesOfLatestAvailableWordlistById(db, wordlistId)
        if (MetadataDbHelper.STATUS_AVAILABLE
            != installCandidate!!.getAsInteger(MetadataDbHelper.STATUS_COLUMN)
        ) {
            // If it's not "AVAILABLE", we want to stop now. Because candidates for auto-install
            // are lists that we know are available, but we also know have never been installed.
            // It does obviously not concern already installed lists, or downloading lists,
            // or those that have been disabled, flagged as deleting... So anything else than
            // AVAILABLE means we don't auto-install.
            return
        }

        // We decided against prompting the user for a decision. This may be because we were
        // explicitly asked not to, or because we are currently on wi-fi anyway, or because we
        // already know the answer to the question. We'll enqueue a request ; StartDownloadAction
        // knows to use the correct type of network according to the current settings.

        // Also note that once it's auto-installed, a word list will be marked as INSTALLED. It will
        // thus receive automatic updates if there are any, which is what we want. If the user does
        // not want this word list, they will have to go to the settings and change them, which will
        // change the shared preferences. So there is no way for a word list that has been
        // auto-installed once to get auto-installed again, and that's what we want.
        val actions: ActionBatch = ActionBatch()
        val metadata: WordListMetadata = WordListMetadata.createFromContentValues(
            installCandidate
        )
        actions.add(StartDownloadAction(clientId, metadata))
        val localeString: String =
            installCandidate.getAsString(MetadataDbHelper.LOCALE_COLUMN)

        // We are in a content provider: we can't do any UI at all. We have to defer the displaying
        // itself to the service. Also, we only display this when the user does not have a
        // dictionary for this language already. During setup wizard, however, this UI is
        // suppressed.
        val deviceProvisioned: Boolean = Settings.Global.getInt(
            context.getContentResolver(),
            Settings.Global.DEVICE_PROVISIONED, 0
        ) != 0
        if (deviceProvisioned) {
            val intent: Intent = Intent()
            intent.setClass(context, DictionaryService::class.java)
            intent.setAction(DictionaryService.SHOW_DOWNLOAD_TOAST_INTENT_ACTION)
            intent.putExtra(DictionaryService.LOCALE_INTENT_ARGUMENT, localeString)
            context.startService(intent)
        } else {
            Log.i(TAG, "installIfNeverRequested() : Don't show download toast")
        }

        Log.i(TAG, "installIfNeverRequested() : StartDownloadAction for " + metadata)
        actions.execute(context, LogProblemReporter(TAG))
    }

    /**
     * Marks the word list with the passed id as used.
     *
     * This will download/install the list as required. The action will see that the destination
     * word list is a valid list, and take appropriate action - in this case, mark it as used.
     * @see ActionBatch.Action.execute
     *
     *
     * @param context the context for using action batches.
     * @param clientId the id of the client.
     * @param wordlistId the id of the word list to mark as installed.
     * @param version the version of the word list to mark as installed.
     * @param status the current status of the word list.
     * @param allowDownloadOnMeteredData whether to download even on metered data connection
     */
    // The version argument is not used yet, because we don't need it to retrieve the information
    // we need. However, the pair (id, version) being the primary key to a word list in the database
    // it feels better for consistency to pass it, and some methods retrieving information about a
    // word list need it so we may need it in the future.
    fun markAsUsed(
        context: Context, clientId: String,
        wordlistId: String?, version: Int,
        status: Int, allowDownloadOnMeteredData: Boolean
    ) {
        val wordListMetaData: WordListMetadata? = MetadataHandler.getCurrentMetadataForWordList(
            context, clientId, wordlistId, version
        )

        if (null == wordListMetaData) return

        val actions: ActionBatch = ActionBatch()
        if (MetadataDbHelper.STATUS_DISABLED == status
            || MetadataDbHelper.STATUS_DELETING == status
        ) {
            actions.add(EnableAction(clientId, wordListMetaData))
        } else if (MetadataDbHelper.STATUS_AVAILABLE == status) {
            actions.add(StartDownloadAction(clientId, wordListMetaData))
        } else {
            Log.e(TAG, "Unexpected state of the word list for markAsUsed : " + status)
        }
        actions.execute(context, LogProblemReporter(TAG))
        signalNewDictionaryState(context)
    }

    /**
     * Marks the word list with the passed id as unused.
     *
     * This leaves the file on the disk for ulterior use. The action will see that the destination
     * word list is null, and take appropriate action - in this case, mark it as unused.
     * @see ActionBatch.Action.execute
     *
     *
     * @param context the context for using action batches.
     * @param clientId the id of the client.
     * @param wordlistId the id of the word list to mark as installed.
     * @param version the version of the word list to mark as installed.
     * @param status the current status of the word list.
     */
    // The version and status arguments are not used yet, but this method matches its interface to
    // markAsUsed for consistency.
    fun markAsUnused(
        context: Context, clientId: String,
        wordlistId: String?, version: Int, status: Int
    ) {
        val wordListMetaData: WordListMetadata? = MetadataHandler.getCurrentMetadataForWordList(
            context, clientId, wordlistId, version
        )

        if (null == wordListMetaData) return
        val actions: ActionBatch = ActionBatch()
        actions.add(DisableAction(clientId, wordListMetaData))
        actions.execute(context, LogProblemReporter(TAG))
        signalNewDictionaryState(context)
    }

    /**
     * Marks the word list with the passed id as deleting.
     *
     * This basically means that on the next chance there is (right away if Android Keyboard
     * happens to be up, or the next time it gets up otherwise) the dictionary pack will
     * supply an empty dictionary to it that will replace whatever dictionary is installed.
     * This allows to release the space taken by a dictionary (except for the few bytes the
     * empty dictionary takes up), and override a built-in default dictionary so that we
     * can fake delete a built-in dictionary.
     *
     * @param context the context to open the database on.
     * @param clientId the id of the client.
     * @param wordlistId the id of the word list to mark as deleted.
     * @param version the version of the word list to mark as deleted.
     * @param status the current status of the word list.
     */
    fun markAsDeleting(
        context: Context, clientId: String,
        wordlistId: String?, version: Int, status: Int
    ) {
        val wordListMetaData: WordListMetadata? = MetadataHandler.getCurrentMetadataForWordList(
            context, clientId, wordlistId, version
        )

        if (null == wordListMetaData) return
        val actions: ActionBatch = ActionBatch()
        actions.add(DisableAction(clientId, wordListMetaData))
        actions.add(StartDeleteAction(clientId, wordListMetaData))
        actions.execute(context, LogProblemReporter(TAG))
        signalNewDictionaryState(context)
    }

    /**
     * Marks the word list with the passed id as actually deleted.
     *
     * This reverts to available status or deletes the row as appropriate.
     *
     * @param context the context to open the database on.
     * @param clientId the id of the client.
     * @param wordlistId the id of the word list to mark as deleted.
     * @param version the version of the word list to mark as deleted.
     * @param status the current status of the word list.
     */
    fun markAsDeleted(
        context: Context, clientId: String?,
        wordlistId: String?, version: Int, status: Int
    ) {
        val wordListMetaData: WordListMetadata? = MetadataHandler.getCurrentMetadataForWordList(
            context, clientId!!, wordlistId, version
        )

        if (null == wordListMetaData) return

        val actions: ActionBatch = ActionBatch()
        actions.add(FinishDeleteAction(clientId, wordListMetaData))
        actions.execute(context, LogProblemReporter(TAG))
        signalNewDictionaryState(context)
    }

    /**
     * Checks whether the word list should be downloaded again; in which case an download &
     * installation attempt is made. Otherwise the word list is marked broken.
     *
     * @param context the context to open the database on.
     * @param clientId the id of the client.
     * @param wordlistId the id of the word list which is broken.
     * @param version the version of the broken word list.
     */
    fun markAsBrokenOrRetrying(
        context: Context?, clientId: String?,
        wordlistId: String?, version: Int
    ) {
        val isRetryPossible: Boolean = MetadataDbHelper.maybeMarkEntryAsRetrying(
            MetadataDbHelper.getDb(context, clientId), wordlistId, version
        )

        if (isRetryPossible) {
            if (DEBUG) {
                Log.d(TAG, "Attempting to download & install the wordlist again.")
            }
            val wordListMetaData: WordListMetadata? = MetadataHandler.getCurrentMetadataForWordList(
                context, clientId!!, wordlistId, version
            )
            if (wordListMetaData == null) {
                return
            }

            val actions: ActionBatch = ActionBatch()
            actions.add(StartDownloadAction(clientId, wordListMetaData))
            actions.execute(context!!, LogProblemReporter(TAG))
        } else {
            if (DEBUG) {
                Log.d(TAG, "Retries for wordlist exhausted, deleting the wordlist from table.")
            }
            MetadataDbHelper.deleteEntry(
                MetadataDbHelper.getDb(context, clientId),
                wordlistId, version
            )
        }
    }

    /**
     * An interface for UIs or services that want to know when something happened.
     *
     * This is chiefly used by the dictionary manager UI.
     */
    interface UpdateEventListener {
        fun downloadedMetadata(succeeded: Boolean)
        fun wordListDownloadFinished(wordListId: String, succeeded: Boolean)
        fun updateCycleCompleted()
    }
}
