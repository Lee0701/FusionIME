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
import android.content.res.Resources
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import com.android.inputmethod.dictionarypack.ActionBatch.DisableAction
import com.android.inputmethod.dictionarypack.ActionBatch.EnableAction
import com.android.inputmethod.dictionarypack.ActionBatch.FinishDeleteAction
import com.android.inputmethod.dictionarypack.ActionBatch.ForgetAction
import com.android.inputmethod.dictionarypack.ActionBatch.InstallAfterDownloadAction
import com.android.inputmethod.dictionarypack.ActionBatch.MakeAvailableAction
import com.android.inputmethod.dictionarypack.ActionBatch.MarkPreInstalledAction
import com.android.inputmethod.dictionarypack.ActionBatch.StartDeleteAction
import com.android.inputmethod.dictionarypack.ActionBatch.StartDownloadAction
import com.android.inputmethod.dictionarypack.ActionBatch.UpdateDataAction
import com.android.inputmethod.latin.BinaryDictionaryFileDumper
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.common.LocaleUtils
import com.android.inputmethod.latin.utils.ApplicationUtils
import com.android.inputmethod.latin.utils.DebugLogUtils
import java.util.LinkedList
import java.util.Queue

/**
 * Object representing an upgrade from one state to another.
 *
 * This implementation basically encapsulates a list of Runnable objects. In the future
 * it may manage dependencies between them. Concretely, it does not use Runnable because the
 * actions need an argument.
 */
/*

The state of a word list follows the following scheme.

       |                                   ^
  MakeAvailable                            |
       |        .------------Forget--------'
       V        |
 STATUS_AVAILABLE  <-------------------------.
       |                                     |
StartDownloadAction                  FinishDeleteAction
       |                                     |
       V                                     |
STATUS_DOWNLOADING      EnableAction-- STATUS_DELETING
       |                     |               ^
InstallAfterDownloadAction   |               |
       |     .---------------'        StartDeleteAction
       |     |                               |
       V     V                               |
 STATUS_INSTALLED  <--EnableAction--   STATUS_DISABLED
                    --DisableAction-->

  It may also be possible that DisableAction or StartDeleteAction or
  DownloadAction run when the file is still downloading.  This cancels
  the download and returns to STATUS_AVAILABLE.
  Also, an UpdateDataAction may apply in any state. It does not affect
  the state in any way (nor type, local filename, id or version) but
  may update other attributes like description or remote filename.

  Forget is an DB maintenance action that removes the entry if it is not installed or disabled.
  This happens when the word list information disappeared from the server, or when a new version
  is available and we should forget about the old one.
*/
class ActionBatch {
    /**
     * A piece of update.
     *
     * Action is basically like a Runnable that takes an argument.
     */
    interface Action {
        /**
         * Execute this action NOW.
         * @param context the context to get system services, resources, databases
         */
        fun execute(context: Context)
    }

    /**
     * An action that starts downloading an available word list.
     */
    class StartDownloadAction(clientId: String?, wordList: WordListMetadata?) : Action {
        private val mClientId: String?

        // The data to download. May not be null.
        val mWordList: WordListMetadata?

        init {
            DebugLogUtils.l("New download action for client ", clientId, " : ", wordList)
            mClientId = clientId
            mWordList = wordList
        }

        override fun execute(context: Context) {
            if (null == mWordList) { // This should never happen
                Log.e(TAG, "UpdateAction with a null parameter!")
                return
            }
            DebugLogUtils.l("Downloading word list")
            val db: SQLiteDatabase = MetadataDbHelper.Companion.getDb(context, mClientId)
            val values: ContentValues? = MetadataDbHelper.Companion.getContentValuesByWordListId(
                db,
                mWordList.mId, mWordList.mVersion
            )
            val status: Int = values!!.getAsInteger(MetadataDbHelper.Companion.STATUS_COLUMN)
            val manager: DownloadManagerWrapper = DownloadManagerWrapper(context)
            if (MetadataDbHelper.Companion.STATUS_DOWNLOADING == status) {
                // The word list is still downloading. Cancel the download and revert the
                // word list status to "available".
                manager.remove(values.getAsLong(MetadataDbHelper.Companion.PENDINGID_COLUMN))
                MetadataDbHelper.Companion.markEntryAsAvailable(
                    db,
                    mWordList.mId,
                    mWordList.mVersion
                )
            } else if (MetadataDbHelper.Companion.STATUS_AVAILABLE != status
                && MetadataDbHelper.Companion.STATUS_RETRYING != status
            ) {
                // Should never happen
                Log.e(
                    TAG, ("Unexpected state of the word list '" + mWordList.mId + "' : " + status
                            + " for an upgrade action. Fall back to download.")
                )
            }
            // Download it.
            DebugLogUtils.l("Upgrade word list, downloading", mWordList.mRemoteFilename)

            // This is an upgraded word list: we should download it.
            // Adding a disambiguator to circumvent a bug in older versions of DownloadManager.
            // DownloadManager also stupidly cuts the extension to replace with its own that it
            // gets from the content-type. We need to circumvent this.
            val disambiguator: String = ("#" + System.currentTimeMillis()
                    + ApplicationUtils.getVersionName(context) + ".dict")
            val uri: Uri = Uri.parse(mWordList.mRemoteFilename + disambiguator)
            val request: DownloadManager.Request = DownloadManager.Request(uri)

            val res: Resources = context.getResources()
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            request.setTitle(mWordList.mDescription)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            request.setVisibleInDownloadsUi(
                res.getBoolean(R.bool.dict_downloads_visible_in_download_UI)
            )

            val downloadId: Long = UpdateHandler.registerDownloadRequest(
                manager, request, db,
                mWordList.mId, mWordList.mVersion
            )
            Log.i(
                TAG, String.format(
                    "Starting the dictionary download with version:"
                            + " %d and Url: %s", mWordList.mVersion, uri
                )
            )
            DebugLogUtils.l("Starting download of", uri, "with id", downloadId)
            PrivateLog.log("Starting download of " + uri + ", id : " + downloadId)
        }

        companion object {
            val TAG: String =
                "DictionaryProvider:" + StartDownloadAction::class.java.getSimpleName()
        }
    }

    /**
     * An action that updates the database to reflect the status of a newly installed word list.
     */
    class InstallAfterDownloadAction(
        clientId: String?,
        wordListValues: ContentValues?
    ) : Action {
        private val mClientId: String?

        // The state to upgrade from. May not be null.
        val mWordListValues: ContentValues?

        init {
            DebugLogUtils.l(
                "New InstallAfterDownloadAction for client ", clientId, " : ",
                wordListValues
            )
            mClientId = clientId
            mWordListValues = wordListValues
        }

        override fun execute(context: Context) {
            if (null == mWordListValues) {
                Log.e(TAG, "InstallAfterDownloadAction with a null parameter!")
                return
            }
            val status: Int = mWordListValues.getAsInteger(MetadataDbHelper.Companion.STATUS_COLUMN)
            if (MetadataDbHelper.Companion.STATUS_DOWNLOADING != status) {
                val id: String =
                    mWordListValues.getAsString(MetadataDbHelper.Companion.WORDLISTID_COLUMN)
                Log.e(
                    TAG, ("Unexpected state of the word list '" + id + "' : " + status
                            + " for an InstallAfterDownload action. Bailing out.")
                )
                return
            }

            DebugLogUtils.l("Setting word list as installed")
            val db: SQLiteDatabase = MetadataDbHelper.Companion.getDb(context, mClientId)
            MetadataDbHelper.Companion.markEntryAsFinishedDownloadingAndInstalled(
                db,
                mWordListValues
            )

            // Install the downloaded file by un-compressing and moving it to the staging
            // directory. Ideally, we should do this before updating the DB, but the
            // installDictToStagingFromContentProvider() relies on the db being updated.
            val localeString: String =
                mWordListValues.getAsString(MetadataDbHelper.Companion.LOCALE_COLUMN)
            BinaryDictionaryFileDumper.installDictToStagingFromContentProvider(
                LocaleUtils.constructLocaleFromString(localeString)!!, context, false
            )
        }

        companion object {
            val TAG: String = ("DictionaryProvider:"
                    + InstallAfterDownloadAction::class.java.getSimpleName())
        }
    }

    /**
     * An action that enables an existing word list.
     */
    class EnableAction(clientId: String, wordList: WordListMetadata?) : Action {
        private val mClientId: String

        // The state to upgrade from. May not be null.
        val mWordList: WordListMetadata?

        init {
            DebugLogUtils.l("New EnableAction for client ", clientId, " : ", wordList)
            mClientId = clientId
            mWordList = wordList
        }

        override fun execute(context: Context) {
            if (null == mWordList) {
                Log.e(TAG, "EnableAction with a null parameter!")
                return
            }
            DebugLogUtils.l("Enabling word list")
            val db: SQLiteDatabase = MetadataDbHelper.Companion.getDb(context, mClientId)
            val values: ContentValues? = MetadataDbHelper.Companion.getContentValuesByWordListId(
                db,
                mWordList.mId, mWordList.mVersion
            )
            val status: Int = values!!.getAsInteger(MetadataDbHelper.Companion.STATUS_COLUMN)
            if (MetadataDbHelper.Companion.STATUS_DISABLED != status
                && MetadataDbHelper.Companion.STATUS_DELETING != status
            ) {
                Log.e(
                    TAG, ("Unexpected state of the word list '" + mWordList.mId + " : " + status
                            + " for an enable action. Cancelling")
                )
                return
            }
            MetadataDbHelper.Companion.markEntryAsEnabled(db, mWordList.mId, mWordList.mVersion)
        }

        companion object {
            val TAG: String = "DictionaryProvider:" + EnableAction::class.java.getSimpleName()
        }
    }

    /**
     * An action that disables a word list.
     */
    class DisableAction(clientId: String, wordlist: WordListMetadata?) : Action {
        private val mClientId: String

        // The word list to disable. May not be null.
        val mWordList: WordListMetadata?

        init {
            DebugLogUtils.l("New Disable action for client ", clientId, " : ", wordlist)
            mClientId = clientId
            mWordList = wordlist
        }

        override fun execute(context: Context) {
            if (null == mWordList) { // This should never happen
                Log.e(TAG, "DisableAction with a null word list!")
                return
            }
            DebugLogUtils.l("Disabling word list : " + mWordList)
            val db: SQLiteDatabase = MetadataDbHelper.Companion.getDb(context, mClientId)
            val values: ContentValues? = MetadataDbHelper.Companion.getContentValuesByWordListId(
                db,
                mWordList.mId, mWordList.mVersion
            )
            val status: Int = values!!.getAsInteger(MetadataDbHelper.Companion.STATUS_COLUMN)
            if (MetadataDbHelper.Companion.STATUS_INSTALLED == status) {
                // Disabling an installed word list
                MetadataDbHelper.Companion.markEntryAsDisabled(
                    db,
                    mWordList.mId,
                    mWordList.mVersion
                )
            } else {
                if (MetadataDbHelper.Companion.STATUS_DOWNLOADING != status) {
                    Log.e(
                        TAG, ("Unexpected state of the word list '" + mWordList.mId + "' : "
                                + status + " for a disable action. Fall back to marking as available.")
                    )
                }
                // The word list is still downloading. Cancel the download and revert the
                // word list status to "available".
                val manager: DownloadManagerWrapper = DownloadManagerWrapper(context)
                manager.remove(values.getAsLong(MetadataDbHelper.Companion.PENDINGID_COLUMN))
                MetadataDbHelper.Companion.markEntryAsAvailable(
                    db,
                    mWordList.mId,
                    mWordList.mVersion
                )
            }
        }

        companion object {
            val TAG: String = "DictionaryProvider:" + DisableAction::class.java.getSimpleName()
        }
    }

    /**
     * An action that makes a word list available.
     */
    class MakeAvailableAction(clientId: String?, wordlist: WordListMetadata?) : Action {
        private val mClientId: String?

        // The word list to make available. May not be null.
        val mWordList: WordListMetadata?

        init {
            DebugLogUtils.l("New MakeAvailable action", clientId, " : ", wordlist)
            mClientId = clientId
            mWordList = wordlist
        }

        override fun execute(context: Context) {
            if (null == mWordList) { // This should never happen
                Log.e(TAG, "MakeAvailableAction with a null word list!")
                return
            }
            val db: SQLiteDatabase = MetadataDbHelper.Companion.getDb(context, mClientId)
            if (null != MetadataDbHelper.Companion.getContentValuesByWordListId(
                    db,
                    mWordList.mId, mWordList.mVersion
                )
            ) {
                Log.e(
                    TAG, ("Unexpected state of the word list '" + mWordList.mId + "' "
                            + " for a makeavailable action. Marking as available anyway.")
                )
            }
            DebugLogUtils.l("Making word list available : " + mWordList)
            // If mLocalFilename is null, then it's a remote file that hasn't been downloaded
            // yet, so we set the local filename to the empty string.
            val values: ContentValues = MetadataDbHelper.Companion.makeContentValues(
                0,
                MetadataDbHelper.Companion.TYPE_BULK, MetadataDbHelper.Companion.STATUS_AVAILABLE,
                mWordList.mId, mWordList.mLocale, mWordList.mDescription,
                if (null == mWordList.mLocalFilename) "" else mWordList.mLocalFilename,
                mWordList.mRemoteFilename, mWordList.mLastUpdate, mWordList.mRawChecksum,
                mWordList.mChecksum, mWordList.mRetryCount, mWordList.mFileSize,
                mWordList.mVersion, mWordList.mFormatVersion
            )
            PrivateLog.log(
                ("Insert 'available' record for " + mWordList.mDescription
                        + " and locale " + mWordList.mLocale)
            )
            db.insert(MetadataDbHelper.Companion.METADATA_TABLE_NAME, null, values)
        }

        companion object {
            val TAG: String =
                "DictionaryProvider:" + MakeAvailableAction::class.java.getSimpleName()
        }
    }

    /**
     * An action that marks a word list as pre-installed.
     *
     * This is almost the same as MakeAvailableAction, as it only inserts a line with parameters
     * received from outside.
     * Unlike MakeAvailableAction, the parameters are not received from a downloaded metadata file
     * but from the client directly; it marks a word list as being "installed" and not "available".
     * It also explicitly sets the filename to the empty string, so that we don't try to open
     * it on our side.
     */
    class MarkPreInstalledAction(clientId: String?, wordlist: WordListMetadata?) : Action {
        private val mClientId: String?

        // The word list to mark pre-installed. May not be null.
        val mWordList: WordListMetadata?

        init {
            DebugLogUtils.l("New MarkPreInstalled action", clientId, " : ", wordlist)
            mClientId = clientId
            mWordList = wordlist
        }

        override fun execute(context: Context) {
            if (null == mWordList) { // This should never happen
                Log.e(TAG, "MarkPreInstalledAction with a null word list!")
                return
            }
            val db: SQLiteDatabase = MetadataDbHelper.Companion.getDb(context, mClientId)
            if (null != MetadataDbHelper.Companion.getContentValuesByWordListId(
                    db,
                    mWordList.mId, mWordList.mVersion
                )
            ) {
                Log.e(
                    TAG, ("Unexpected state of the word list '" + mWordList.mId + "' "
                            + " for a markpreinstalled action. Marking as preinstalled anyway.")
                )
            }
            DebugLogUtils.l("Marking word list preinstalled : " + mWordList)
            // This word list is pre-installed : we don't have its file. We should reset
            // the local file name to the empty string so that we don't try to open it
            // accidentally. The remote filename may be set by the application if it so wishes.
            val values: ContentValues = MetadataDbHelper.Companion.makeContentValues(
                0,
                MetadataDbHelper.Companion.TYPE_BULK, MetadataDbHelper.Companion.STATUS_INSTALLED,
                mWordList.mId, mWordList.mLocale, mWordList.mDescription,
                if (TextUtils.isEmpty(mWordList.mLocalFilename)) "" else mWordList.mLocalFilename,
                mWordList.mRemoteFilename, mWordList.mLastUpdate,
                mWordList.mRawChecksum, mWordList.mChecksum, mWordList.mRetryCount,
                mWordList.mFileSize, mWordList.mVersion, mWordList.mFormatVersion
            )
            PrivateLog.log(
                ("Insert 'preinstalled' record for " + mWordList.mDescription
                        + " and locale " + mWordList.mLocale)
            )
            db.insert(MetadataDbHelper.Companion.METADATA_TABLE_NAME, null, values)
        }

        companion object {
            val TAG: String = ("DictionaryProvider:"
                    + MarkPreInstalledAction::class.java.getSimpleName())
        }
    }

    /**
     * An action that updates information about a word list - description, locale etc
     */
    class UpdateDataAction(clientId: String?, wordlist: WordListMetadata?) : Action {
        private val mClientId: String?
        val mWordList: WordListMetadata?

        init {
            DebugLogUtils.l("New UpdateData action for client ", clientId, " : ", wordlist)
            mClientId = clientId
            mWordList = wordlist
        }

        override fun execute(context: Context) {
            if (null == mWordList) { // This should never happen
                Log.e(TAG, "UpdateDataAction with a null word list!")
                return
            }
            val db: SQLiteDatabase = MetadataDbHelper.Companion.getDb(context, mClientId)
            val oldValues: ContentValues? = MetadataDbHelper.Companion.getContentValuesByWordListId(
                db,
                mWordList.mId, mWordList.mVersion
            )
            if (null == oldValues) {
                Log.e(TAG, "Trying to update data about a non-existing word list. Bailing out.")
                return
            }
            DebugLogUtils.l("Updating data about a word list : " + mWordList)
            val values: ContentValues = MetadataDbHelper.Companion.makeContentValues(
                oldValues.getAsInteger(MetadataDbHelper.Companion.PENDINGID_COLUMN),
                oldValues.getAsInteger(MetadataDbHelper.Companion.TYPE_COLUMN),
                oldValues.getAsInteger(MetadataDbHelper.Companion.STATUS_COLUMN),
                mWordList.mId, mWordList.mLocale, mWordList.mDescription,
                oldValues.getAsString(MetadataDbHelper.Companion.LOCAL_FILENAME_COLUMN),
                mWordList.mRemoteFilename, mWordList.mLastUpdate, mWordList.mRawChecksum,
                mWordList.mChecksum, mWordList.mRetryCount, mWordList.mFileSize,
                mWordList.mVersion, mWordList.mFormatVersion
            )
            PrivateLog.log(
                ("Updating record for " + mWordList.mDescription
                        + " and locale " + mWordList.mLocale)
            )
            db.update(
                MetadataDbHelper.Companion.METADATA_TABLE_NAME, values,
                (MetadataDbHelper.Companion.WORDLISTID_COLUMN + " = ? AND "
                        + MetadataDbHelper.Companion.VERSION_COLUMN + " = ?"),
                arrayOf<String?>(mWordList.mId, mWordList.mVersion.toString())
            )
        }

        companion object {
            val TAG: String = "DictionaryProvider:" + UpdateDataAction::class.java.getSimpleName()
        }
    }

    /**
     * An action that deletes the metadata about a word list if possible.
     *
     * This is triggered when a specific word list disappeared from the server, or when a fresher
     * word list is available and the old one was not installed.
     * If the word list has not been installed, it's possible to delete its associated metadata.
     * Otherwise, the settings are retained so that the user can still administrate it.
     */
    class ForgetAction(
        clientId: String?, wordlist: WordListMetadata?,
        hasNewerVersion: Boolean
    ) : Action {
        private val mClientId: String?

        // The word list to remove. May not be null.
        val mWordList: WordListMetadata?
        val mHasNewerVersion: Boolean

        init {
            DebugLogUtils.l("New TryRemove action for client ", clientId, " : ", wordlist)
            mClientId = clientId
            mWordList = wordlist
            mHasNewerVersion = hasNewerVersion
        }

        override fun execute(context: Context) {
            if (null == mWordList) { // This should never happen
                Log.e(TAG, "TryRemoveAction with a null word list!")
                return
            }
            DebugLogUtils.l("Trying to remove word list : " + mWordList)
            val db: SQLiteDatabase = MetadataDbHelper.Companion.getDb(context, mClientId)
            val values: ContentValues? = MetadataDbHelper.Companion.getContentValuesByWordListId(
                db,
                mWordList.mId, mWordList.mVersion
            )
            if (null == values) {
                Log.e(TAG, "Trying to update the metadata of a non-existing wordlist. Cancelling.")
                return
            }
            val status: Int = values.getAsInteger(MetadataDbHelper.Companion.STATUS_COLUMN)
            if (mHasNewerVersion && MetadataDbHelper.Companion.STATUS_AVAILABLE != status) {
                // If we have a newer version of this word list, we should be here ONLY if it was
                // not installed - else we should be upgrading it.
                Log.e(
                    TAG, ("Unexpected status for forgetting a word list info : " + status
                            + ", removing URL to prevent re-download")
                )
            }
            if (MetadataDbHelper.Companion.STATUS_INSTALLED == status || MetadataDbHelper.Companion.STATUS_DISABLED == status || MetadataDbHelper.Companion.STATUS_DELETING == status) {
                // If it is installed or disabled, we need to mark it as deleted so that LatinIME
                // will remove it next time it enquires for dictionaries.
                // If it is deleting and we don't have a new version, then we have to wait until
                // LatinIME actually has deleted it before we can remove its metadata.
                // In both cases, remove the URI from the database since it is not supposed to
                // be accessible any more.
                values.put(MetadataDbHelper.Companion.REMOTE_FILENAME_COLUMN, "")
                values.put(
                    MetadataDbHelper.Companion.STATUS_COLUMN,
                    MetadataDbHelper.Companion.STATUS_DELETING
                )
                db.update(
                    MetadataDbHelper.Companion.METADATA_TABLE_NAME, values,
                    (MetadataDbHelper.Companion.WORDLISTID_COLUMN + " = ? AND "
                            + MetadataDbHelper.Companion.VERSION_COLUMN + " = ?"),
                    arrayOf<String?>(mWordList.mId, mWordList.mVersion.toString())
                )
            } else {
                // If it's AVAILABLE or DOWNLOADING or even UNKNOWN, delete the entry.
                db.delete(
                    MetadataDbHelper.Companion.METADATA_TABLE_NAME,
                    (MetadataDbHelper.Companion.WORDLISTID_COLUMN + " = ? AND "
                            + MetadataDbHelper.Companion.VERSION_COLUMN + " = ?"),
                    arrayOf<String?>(mWordList.mId, mWordList.mVersion.toString())
                )
            }
        }

        companion object {
            val TAG: String = "DictionaryProvider:" + ForgetAction::class.java.getSimpleName()
        }
    }

    /**
     * An action that sets the word list for deletion as soon as possible.
     *
     * This is triggered when the user requests deletion of a word list. This will mark it as
     * deleted in the database, and fire an intent for Android Keyboard to take notice and
     * reload its dictionaries right away if it is up. If it is not up now, then it will
     * delete the actual file the next time it gets up.
     * A file marked as deleted causes the content provider to supply a zero-sized file to
     * Android Keyboard, which will overwrite any existing file and provide no words for this
     * word list. This is not exactly a "deletion", since there is an actual file which takes up
     * a few bytes on the disk, but this allows to override a default dictionary with an empty
     * dictionary. This way, there is no need for the user to make a distinction between
     * dictionaries installed by default and add-on dictionaries.
     */
    class StartDeleteAction(clientId: String, wordlist: WordListMetadata?) : Action {
        private val mClientId: String

        // The word list to delete. May not be null.
        val mWordList: WordListMetadata?

        init {
            DebugLogUtils.l("New StartDelete action for client ", clientId, " : ", wordlist)
            mClientId = clientId
            mWordList = wordlist
        }

        override fun execute(context: Context) {
            if (null == mWordList) { // This should never happen
                Log.e(TAG, "StartDeleteAction with a null word list!")
                return
            }
            DebugLogUtils.l("Trying to delete word list : " + mWordList)
            val db: SQLiteDatabase = MetadataDbHelper.Companion.getDb(context, mClientId)
            val values: ContentValues? = MetadataDbHelper.Companion.getContentValuesByWordListId(
                db,
                mWordList.mId, mWordList.mVersion
            )
            if (null == values) {
                Log.e(TAG, "Trying to set a non-existing wordlist for removal. Cancelling.")
                return
            }
            val status: Int = values.getAsInteger(MetadataDbHelper.Companion.STATUS_COLUMN)
            if (MetadataDbHelper.Companion.STATUS_DISABLED != status) {
                Log.e(TAG, "Unexpected status for deleting a word list info : " + status)
            }
            MetadataDbHelper.Companion.markEntryAsDeleting(db, mWordList.mId, mWordList.mVersion)
        }

        companion object {
            val TAG: String = "DictionaryProvider:" + StartDeleteAction::class.java.getSimpleName()
        }
    }

    /**
     * An action that validates a word list as deleted.
     *
     * This will restore the word list as available if it still is, or remove the entry if
     * it is not any more.
     */
    class FinishDeleteAction(clientId: String?, wordlist: WordListMetadata?) : Action {
        private val mClientId: String?

        // The word list to delete. May not be null.
        val mWordList: WordListMetadata?

        init {
            DebugLogUtils.l("New FinishDelete action for client", clientId, " : ", wordlist)
            mClientId = clientId
            mWordList = wordlist
        }

        override fun execute(context: Context) {
            if (null == mWordList) { // This should never happen
                Log.e(TAG, "FinishDeleteAction with a null word list!")
                return
            }
            DebugLogUtils.l("Trying to delete word list : " + mWordList)
            val db: SQLiteDatabase = MetadataDbHelper.Companion.getDb(context, mClientId)
            val values: ContentValues? = MetadataDbHelper.Companion.getContentValuesByWordListId(
                db,
                mWordList.mId, mWordList.mVersion
            )
            if (null == values) {
                Log.e(TAG, "Trying to set a non-existing wordlist for removal. Cancelling.")
                return
            }
            val status: Int = values.getAsInteger(MetadataDbHelper.Companion.STATUS_COLUMN)
            if (MetadataDbHelper.Companion.STATUS_DELETING != status) {
                Log.e(TAG, "Unexpected status for finish-deleting a word list info : " + status)
            }
            val remoteFilename: String =
                values.getAsString(MetadataDbHelper.Companion.REMOTE_FILENAME_COLUMN)
            // If there isn't a remote filename any more, then we don't know where to get the file
            // from any more, so we remove the entry entirely. As a matter of fact, if the file was
            // marked DELETING but disappeared from the metadata on the server, it ended up
            // this way.
            if (TextUtils.isEmpty(remoteFilename)) {
                db.delete(
                    MetadataDbHelper.Companion.METADATA_TABLE_NAME,
                    (MetadataDbHelper.Companion.WORDLISTID_COLUMN + " = ? AND "
                            + MetadataDbHelper.Companion.VERSION_COLUMN + " = ?"),
                    arrayOf<String?>(mWordList.mId, mWordList.mVersion.toString())
                )
            } else {
                MetadataDbHelper.Companion.markEntryAsAvailable(
                    db,
                    mWordList.mId,
                    mWordList.mVersion
                )
            }
        }

        companion object {
            val TAG: String = "DictionaryProvider:" + FinishDeleteAction::class.java.getSimpleName()
        }
    }

    // An action batch consists of an ordered queue of Actions that can execute.
    private val mActions: Queue<Action>

    init {
        mActions = LinkedList()
    }

    fun add(a: Action) {
        mActions.add(a)
    }

    /**
     * Append all the actions of another action batch.
     * @param that the upgrade to merge into this one.
     */
    fun append(that: ActionBatch) {
        for (a: Action in that.mActions) {
            add(a)
        }
    }

    /**
     * Execute this batch.
     *
     * @param context the context for getting resources, databases, system services.
     * @param reporter a Reporter to send errors to.
     */
    fun execute(context: Context, reporter: ProblemReporter?) {
        DebugLogUtils.l("Executing a batch of actions")
        val remainingActions: Queue<Action> = mActions
        while (!remainingActions.isEmpty()) {
            val a: Action? = remainingActions.poll()
            try {
                a!!.execute(context)
            } catch (e: Exception) {
                if (null != reporter) reporter.report(e)
            }
        }
    }
}
