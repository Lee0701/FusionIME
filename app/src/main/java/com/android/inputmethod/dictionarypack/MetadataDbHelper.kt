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

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.text.TextUtils
import android.util.Log
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.utils.DebugLogUtils
import java.io.File
import java.util.LinkedList
import java.util.TreeMap
import kotlin.math.min

/**
 * Various helper functions for the state database
 */
class MetadataDbHelper private constructor(context: Context?, clientId: String) :
    SQLiteOpenHelper(
        context,
        METADATA_DATABASE_NAME_STEM + (if (TextUtils.isEmpty(clientId)) "" else "." + clientId),
        null, CURRENT_METADATA_DATABASE_VERSION
    ) {
    private val mContext: Context?
    private val mClientId: String?

    private fun createClientTable(db: SQLiteDatabase) {
        // The clients table only exists in the primary db, the one that has an empty client id
        if (!TextUtils.isEmpty(mClientId)) return
        db.execSQL(METADATA_CREATE_CLIENT_TABLE)
        val defaultMetadataUri: String = mContext!!.getString(R.string.default_metadata_uri)
        if (!TextUtils.isEmpty(defaultMetadataUri)) {
            val defaultMetadataValues: ContentValues = ContentValues()
            defaultMetadataValues.put(CLIENT_CLIENT_ID_COLUMN, "")
            defaultMetadataValues.put(CLIENT_METADATA_URI_COLUMN, defaultMetadataUri)
            defaultMetadataValues.put(CLIENT_PENDINGID_COLUMN, UpdateHandler.NOT_AN_ID)
            db.insert(CLIENT_TABLE_NAME, null, defaultMetadataValues)
        }
    }

    /**
     * Create the table and populate it with the resources found inside the apk.
     *
     * @see SQLiteOpenHelper.onCreate
     * @param db the database to create and populate.
     */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(METADATA_TABLE_CREATE)
        createClientTable(db)
    }

    /**
     * Upgrade the database. Upgrade from version 3 is supported.
     * Version 3 has a DB named METADATA_DATABASE_NAME_STEM containing a table METADATA_TABLE_NAME.
     * Version 6 and above has a DB named METADATA_DATABASE_NAME_STEM containing a
     * table CLIENT_TABLE_NAME, and for each client a table called METADATA_TABLE_STEM + "." + the
     * name of the client and contains a table METADATA_TABLE_NAME.
     * For schemas, see the above create statements. The schemas have never changed so far.
     *
     * This method is called by the framework. See [SQLiteOpenHelper.onUpgrade]
     * @param db The database we are upgrading
     * @param oldVersion The old database version (the one on the disk)
     * @param newVersion The new database version as supplied to the constructor of SQLiteOpenHelper
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (METADATA_DATABASE_INITIAL_VERSION == oldVersion && METADATA_DATABASE_VERSION_WITH_CLIENTID <= newVersion && CURRENT_METADATA_DATABASE_VERSION >= newVersion) {
            // Upgrade from version METADATA_DATABASE_INITIAL_VERSION to version
            // METADATA_DATABASE_VERSION_WITH_CLIENT_ID
            // Only the default database should contain the client table, so we test for mClientId.
            if (TextUtils.isEmpty(mClientId)) {
                // Anyway in version 3 only the default table existed so the emptiness
                // test should always be true, but better check to be sure.
                createClientTable(db)
            }
        } else if (METADATA_DATABASE_VERSION_WITH_CLIENTID < newVersion
            && CURRENT_METADATA_DATABASE_VERSION >= newVersion
        ) {
            // Here we drop the client table, so that all clients send us their information again.
            // The client table contains the URL to hit to update the available dictionaries list,
            // but the info about the dictionaries themselves is stored in the table called
            // METADATA_TABLE_NAME and we want to keep it, so we only drop the client table.
            db.execSQL("DROP TABLE IF EXISTS " + CLIENT_TABLE_NAME)
            // Only the default database should contain the client table, so we test for mClientId.
            if (TextUtils.isEmpty(mClientId)) {
                createClientTable(db)
            }
        } else {
            // If we're not in the above case, either we are upgrading from an earlier versionCode
            // and we should wipe the database, or we are handling a version we never heard about
            // (can only be a bug) so it's safer to wipe the database.
            db.execSQL("DROP TABLE IF EXISTS " + METADATA_TABLE_NAME)
            db.execSQL("DROP TABLE IF EXISTS " + CLIENT_TABLE_NAME)
            onCreate(db)
        }
        // A rawChecksum column that did not exist in the previous versions was added that
        // corresponds to the md5 checksum of the file after decompression/decryption. This is to
        // strengthen the system against corrupted dictionary files.
        // The most secure way to upgrade a database is to just test for the column presence, and
        // add it if it's not there.
        addRawChecksumColumnUnlessPresent(db)

        // A retry count column that did not exist in the previous versions was added that
        // corresponds to the number of download & installation attempts that have been made
        // in order to strengthen the system recovery from corrupted dictionary files.
        // The most secure way to upgrade a database is to just test for the column presence, and
        // add it if it's not there.
        addRetryCountColumnUnlessPresent(db)
    }

    /**
     * Downgrade the database. This drops and recreates the table in all cases.
     */
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No matter what the numerical values of oldVersion and newVersion are, we know this
        // is a downgrade (newVersion < oldVersion). There is no way to know what the future
        // databases will look like, but we know it's extremely likely that it's okay to just
        // drop the tables and start from scratch. Hence, we ignore the versions and just wipe
        // everything we want to use.
        if (oldVersion <= newVersion) {
            Log.e(
                TAG, ("onDowngrade database but new version is higher? " + oldVersion + " <= "
                        + newVersion)
            )
        }
        db.execSQL("DROP TABLE IF EXISTS " + METADATA_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + CLIENT_TABLE_NAME)
        onCreate(db)
    }

    init {
        mContext = context
        mClientId = clientId
    }

    companion object {
        private val TAG: String = MetadataDbHelper::class.java.getSimpleName()

        // This was the initial release version of the database. It should never be
        // changed going forward.
        private const val METADATA_DATABASE_INITIAL_VERSION: Int = 3

        // This is the first released version of the database that implements CLIENTID. It is
        // used to identify the versions for upgrades. This should never change going forward.
        private const val METADATA_DATABASE_VERSION_WITH_CLIENTID: Int = 6

        // The current database version.
        // This MUST be increased every time the dictionary pack metadata URL changes.
        private const val CURRENT_METADATA_DATABASE_VERSION: Int = 16

        private val NOT_A_DOWNLOAD_ID: Long = -1

        // The number of retries allowed when attempting to download a broken dictionary.
        const val DICTIONARY_RETRY_THRESHOLD: Int = 2

        const val METADATA_TABLE_NAME: String = "pendingUpdates"
        const val CLIENT_TABLE_NAME: String = "clients"
        const val PENDINGID_COLUMN: String = "pendingid" // Download Manager ID
        const val TYPE_COLUMN: String = "type"
        const val STATUS_COLUMN: String = "status"
        const val LOCALE_COLUMN: String = "locale"
        const val WORDLISTID_COLUMN: String = "id"
        const val DESCRIPTION_COLUMN: String = "description"
        const val LOCAL_FILENAME_COLUMN: String = "filename"
        const val REMOTE_FILENAME_COLUMN: String = "url"
        const val DATE_COLUMN: String = "date"
        const val CHECKSUM_COLUMN: String = "checksum"
        const val FILESIZE_COLUMN: String = "filesize"
        const val VERSION_COLUMN: String = "version"
        const val FORMATVERSION_COLUMN: String = "formatversion"
        const val FLAGS_COLUMN: String = "flags"
        const val RAW_CHECKSUM_COLUMN: String = "rawChecksum"
        const val RETRY_COUNT_COLUMN: String = "remainingRetries"
        const val COLUMN_COUNT: Int = 15

        private const val CLIENT_CLIENT_ID_COLUMN: String = "clientid"
        private const val CLIENT_METADATA_URI_COLUMN: String = "uri"
        private const val CLIENT_METADATA_ADDITIONAL_ID_COLUMN: String = "additionalid"
        private const val CLIENT_LAST_UPDATE_DATE_COLUMN: String = "lastupdate"
        private const val CLIENT_PENDINGID_COLUMN: String = "pendingid" // Download Manager ID

        const val METADATA_DATABASE_NAME_STEM: String = "pendingUpdates"
        const val METADATA_UPDATE_DESCRIPTION: String = "metadata"

        const val DICTIONARIES_ASSETS_PATH: String = "dictionaries"

        // Statuses, for storing in the STATUS_COLUMN
        // IMPORTANT: The following are used as index arrays in ../WordListPreference
        // Do not change their values without updating the matched code.
        // Unknown status: this should never happen.
        const val STATUS_UNKNOWN: Int = 0

        // Available: this word list is available, but it is not downloaded (not downloading), because
        // it is set not to be used.
        const val STATUS_AVAILABLE: Int = 1

        // Downloading: this word list is being downloaded.
        const val STATUS_DOWNLOADING: Int = 2

        // Installed: this word list is installed and usable.
        const val STATUS_INSTALLED: Int = 3

        // Disabled: this word list is installed, but has been disabled by the user.
        const val STATUS_DISABLED: Int = 4

        // Deleting: the user marked this word list to be deleted, but it has not been yet because
        // Latin IME is not up yet.
        const val STATUS_DELETING: Int = 5

        // Retry: dictionary got corrupted, so an attempt must be done to download & install it again.
        const val STATUS_RETRYING: Int = 6

        // Types, for storing in the TYPE_COLUMN
        // This is metadata about what is available.
        const val TYPE_METADATA: Int = 1

        // This is a bulk file. It should replace older files.
        const val TYPE_BULK: Int = 2

        // This is an incremental update, expected to be small, and meaningless on its own.
        const val TYPE_UPDATE: Int = 3

        private val METADATA_TABLE_CREATE: String = ("CREATE TABLE " + METADATA_TABLE_NAME + " ("
                + PENDINGID_COLUMN + " INTEGER, "
                + TYPE_COLUMN + " INTEGER, "
                + STATUS_COLUMN + " INTEGER, "
                + WORDLISTID_COLUMN + " TEXT, "
                + LOCALE_COLUMN + " TEXT, "
                + DESCRIPTION_COLUMN + " TEXT, "
                + LOCAL_FILENAME_COLUMN + " TEXT, "
                + REMOTE_FILENAME_COLUMN + " TEXT, "
                + DATE_COLUMN + " INTEGER, "
                + CHECKSUM_COLUMN + " TEXT, "
                + FILESIZE_COLUMN + " INTEGER, "
                + VERSION_COLUMN + " INTEGER,"
                + FORMATVERSION_COLUMN + " INTEGER, "
                + FLAGS_COLUMN + " INTEGER, "
                + RAW_CHECKSUM_COLUMN + " TEXT,"
                + RETRY_COUNT_COLUMN + " INTEGER, "
                + "PRIMARY KEY (" + WORDLISTID_COLUMN + "," + VERSION_COLUMN + "));")
        private val METADATA_CREATE_CLIENT_TABLE: String =
            ("CREATE TABLE IF NOT EXISTS " + CLIENT_TABLE_NAME + " ("
                    + CLIENT_CLIENT_ID_COLUMN + " TEXT, "
                    + CLIENT_METADATA_URI_COLUMN + " TEXT, "
                    + CLIENT_METADATA_ADDITIONAL_ID_COLUMN + " TEXT, "
                    + CLIENT_LAST_UPDATE_DATE_COLUMN + " INTEGER NOT NULL DEFAULT 0, "
                    + CLIENT_PENDINGID_COLUMN + " INTEGER, "
                    + FLAGS_COLUMN + " INTEGER, "
                    + "PRIMARY KEY (" + CLIENT_CLIENT_ID_COLUMN + "));")

        // List of all metadata table columns.
        val METADATA_TABLE_COLUMNS: Array<String> = arrayOf(
            PENDINGID_COLUMN, TYPE_COLUMN,
            STATUS_COLUMN, WORDLISTID_COLUMN, LOCALE_COLUMN, DESCRIPTION_COLUMN,
            LOCAL_FILENAME_COLUMN, REMOTE_FILENAME_COLUMN, DATE_COLUMN, CHECKSUM_COLUMN,
            FILESIZE_COLUMN, VERSION_COLUMN, FORMATVERSION_COLUMN, FLAGS_COLUMN,
            RAW_CHECKSUM_COLUMN, RETRY_COUNT_COLUMN
        )

        // List of all client table columns.
        val CLIENT_TABLE_COLUMNS: Array<String> = arrayOf(
            CLIENT_CLIENT_ID_COLUMN,
            CLIENT_METADATA_URI_COLUMN, CLIENT_PENDINGID_COLUMN, FLAGS_COLUMN
        )

        // List of public columns returned to clients. Everything that is not in this list is
        // private and implementation-dependent.
        val DICTIONARIES_LIST_PUBLIC_COLUMNS: Array<String> = arrayOf(
            STATUS_COLUMN, WORDLISTID_COLUMN,
            LOCALE_COLUMN, DESCRIPTION_COLUMN, DATE_COLUMN, FILESIZE_COLUMN, VERSION_COLUMN
        )

        // This class exhibits a singleton-like behavior by client ID, so it is getInstance'd
        // and has a private c'tor.
        private var sInstanceMap: TreeMap<String, MetadataDbHelper>? = null

        @Synchronized
        fun getInstance(
            context: Context?,
            clientIdOrNull: String?
        ): MetadataDbHelper {
            // As a backward compatibility feature, null can be passed here to retrieve the "default"
            // database. Before multi-client support, the dictionary packed used only one database
            // and would not be able to handle several dictionary sets. Passing null here retrieves
            // this legacy database. New clients should make sure to always pass a client ID so as
            // to avoid conflicts.
            val clientId: String = if (null != clientIdOrNull) clientIdOrNull else ""
            if (null == sInstanceMap) sInstanceMap = TreeMap()
            var helper: MetadataDbHelper? = sInstanceMap!!.get(clientId)
            if (null == helper) {
                helper = MetadataDbHelper(context, clientId)
                sInstanceMap!!.put(clientId, helper)
            }
            return helper
        }

        /**
         * Get the database itself. This always returns the same object for any client ID. If the
         * client ID is null, a default database is returned for backward compatibility. Don't
         * pass null for new calls.
         *
         * @param context the context to create the database from. This is ignored after the first call.
         * @param clientId the client id to retrieve the database of. null for default (deprecated)
         * @return the database.
         */
        fun getDb(context: Context?, clientId: String?): SQLiteDatabase {
            return getInstance(context, clientId).getWritableDatabase()
        }

        private fun addRawChecksumColumnUnlessPresent(db: SQLiteDatabase) {
            try {
                db.execSQL(
                    ("SELECT " + RAW_CHECKSUM_COLUMN + " FROM "
                            + METADATA_TABLE_NAME + " LIMIT 0;")
                )
            } catch (e: SQLiteException) {
                Log.i(TAG, "No " + RAW_CHECKSUM_COLUMN + " column : creating it")
                db.execSQL(
                    ("ALTER TABLE " + METADATA_TABLE_NAME + " ADD COLUMN "
                            + RAW_CHECKSUM_COLUMN + " TEXT;")
                )
            }
        }

        private fun addRetryCountColumnUnlessPresent(db: SQLiteDatabase) {
            try {
                db.execSQL(
                    ("SELECT " + RETRY_COUNT_COLUMN + " FROM "
                            + METADATA_TABLE_NAME + " LIMIT 0;")
                )
            } catch (e: SQLiteException) {
                Log.i(TAG, "No " + RETRY_COUNT_COLUMN + " column : creating it")
                db.execSQL(
                    ("ALTER TABLE " + METADATA_TABLE_NAME + " ADD COLUMN "
                            + RETRY_COUNT_COLUMN + " INTEGER DEFAULT " + DICTIONARY_RETRY_THRESHOLD + ";")
                )
            }
        }

        /**
         * Given a client ID, returns whether this client exists.
         *
         * @param context a context to open the database
         * @param clientId the client ID to check
         * @return true if the client is known, false otherwise
         */
        fun isClientKnown(context: Context?, clientId: String?): Boolean {
            // If the client is known, they'll have a non-null metadata URI. An empty string is
            // allowed as a metadata URI, if the client doesn't want any updates to happen.
            return null != getMetadataUriAsString(context, clientId)
        }

        private val sMetadataUriGetter: MetadataUriGetter = MetadataUriGetter()

        /**
         * Returns the metadata URI as a string.
         *
         * If the client is not known, this will return null. If it is known, it will return
         * the URI as a string. Note that the empty string is a valid value.
         *
         * @param context a context instance to open the database on
         * @param clientId the ID of the client we want the metadata URI of
         * @return the string representation of the URI
         */
        fun getMetadataUriAsString(context: Context?, clientId: String?): String? {
            val defaultDb: SQLiteDatabase = getDb(context, null)
            val cursor: Cursor = defaultDb.query(
                CLIENT_TABLE_NAME,
                arrayOf(CLIENT_METADATA_URI_COLUMN),
                CLIENT_CLIENT_ID_COLUMN + " = ?", arrayOf(clientId),
                null, null, null, null
            )
            try {
                if (!cursor.moveToFirst()) return null
                return MetadataUriGetter.getUri(context, cursor.getString(0))
            } finally {
                cursor.close()
            }
        }

        /**
         * Update the last metadata update time for all clients using a particular URI.
         *
         * This method searches for all clients using a particular URI and updates the last
         * update time for this client.
         * The current time is used as the latest update time. This saved date will be what
         * is returned henceforth by [.getLastUpdateDateForClient],
         * until this method is called again.
         *
         * @param context a context instance to open the database on
         * @param uri the metadata URI we just downloaded
         */
        fun saveLastUpdateTimeOfUri(context: Context?, uri: String?) {
            PrivateLog.log("Save last update time of URI : " + uri + " " + System.currentTimeMillis())
            val values: ContentValues = ContentValues()
            values.put(CLIENT_LAST_UPDATE_DATE_COLUMN, System.currentTimeMillis())
            val defaultDb: SQLiteDatabase = getDb(context, null)
            val cursor: Cursor = queryClientIds(context)
            if (null == cursor) return
            try {
                if (!cursor.moveToFirst()) return
                do {
                    val clientId: String = cursor.getString(0)
                    val metadataUri: String? =
                        getMetadataUriAsString(context, clientId)
                    if (metadataUri == uri) {
                        defaultDb.update(
                            CLIENT_TABLE_NAME, values,
                            CLIENT_CLIENT_ID_COLUMN + " = ?", arrayOf(clientId)
                        )
                    }
                } while (cursor.moveToNext())
            } finally {
                cursor.close()
            }
        }

        /**
         * Retrieves the last date at which we updated the metadata for this client.
         *
         * The returned date is in milliseconds from the EPOCH; this is the same unit as
         * returned by [System.currentTimeMillis].
         *
         * @param context a context instance to open the database on
         * @param clientId the client ID to get the latest update date of
         * @return the last date at which this client was updated, as a long.
         */
        fun getLastUpdateDateForClient(context: Context?, clientId: String?): Long {
            val defaultDb: SQLiteDatabase = getDb(context, null)
            val cursor: Cursor = defaultDb.query(
                CLIENT_TABLE_NAME,
                arrayOf(CLIENT_LAST_UPDATE_DATE_COLUMN),
                CLIENT_CLIENT_ID_COLUMN + " = ?",
                arrayOf(if (null == clientId) "" else clientId),
                null, null, null, null
            )
            try {
                if (!cursor.moveToFirst()) return 0
                return cursor.getLong(0) // Only one column, return it
            } finally {
                cursor.close()
            }
        }

        /**
         * Get the metadata download ID for a metadata URI.
         *
         * This will retrieve the download ID for the metadata file that has the passed URI.
         * If this URI is not being downloaded right now, it will return NOT_AN_ID.
         *
         * @param context a context instance to open the database on
         * @param uri the URI to retrieve the metadata download ID of
         * @return the download id and start date, or null if the URL is not known
         */
        fun getMetadataDownloadIdAndStartDateForURI(
            context: Context?, uri: String?
        ): DownloadIdAndStartDate? {
            val defaultDb: SQLiteDatabase = getDb(context, null)
            val cursor: Cursor = defaultDb.query(
                CLIENT_TABLE_NAME,
                arrayOf(CLIENT_PENDINGID_COLUMN, CLIENT_LAST_UPDATE_DATE_COLUMN),
                CLIENT_METADATA_URI_COLUMN + " = ?", arrayOf(uri),
                null, null, null, null
            )
            try {
                if (!cursor.moveToFirst()) return null
                return DownloadIdAndStartDate(cursor.getInt(0).toLong(), cursor.getLong(1))
            } finally {
                cursor.close()
            }
        }

        fun getOldestUpdateTime(context: Context?): Long {
            val defaultDb: SQLiteDatabase = getDb(context, null)
            val cursor: Cursor = defaultDb.query(
                CLIENT_TABLE_NAME,
                arrayOf(CLIENT_LAST_UPDATE_DATE_COLUMN),
                null, null, null, null, null
            )
            try {
                if (!cursor.moveToFirst()) return 0
                val columnIndex: Int = 0 // Only one column queried
                // Initialize the earliestTime to the largest possible value.
                var earliestTime: Long = Long.MAX_VALUE // Almost 300 million years in the future
                do {
                    val thisTime: Long = cursor.getLong(columnIndex)
                    earliestTime = min(thisTime.toDouble(), earliestTime.toDouble()).toLong()
                } while (cursor.moveToNext())
                return earliestTime
            } finally {
                cursor.close()
            }
        }

        /**
         * Helper method to make content values to write into the database.
         * @return content values with all the arguments put with the right column names.
         */
        fun makeContentValues(
            pendingId: Int, type: Int,
            status: Int, wordlistId: String?, locale: String?,
            description: String?, filename: String?, url: String?, date: Long,
            rawChecksum: String?, checksum: String?, retryCount: Int,
            filesize: Long, version: Int, formatVersion: Int
        ): ContentValues {
            val result: ContentValues = ContentValues(COLUMN_COUNT)
            result.put(PENDINGID_COLUMN, pendingId)
            result.put(TYPE_COLUMN, type)
            result.put(WORDLISTID_COLUMN, wordlistId)
            result.put(STATUS_COLUMN, status)
            result.put(LOCALE_COLUMN, locale)
            result.put(DESCRIPTION_COLUMN, description)
            result.put(LOCAL_FILENAME_COLUMN, filename)
            result.put(REMOTE_FILENAME_COLUMN, url)
            result.put(DATE_COLUMN, date)
            result.put(RAW_CHECKSUM_COLUMN, rawChecksum)
            result.put(RETRY_COUNT_COLUMN, retryCount)
            result.put(CHECKSUM_COLUMN, checksum)
            result.put(FILESIZE_COLUMN, filesize)
            result.put(VERSION_COLUMN, version)
            result.put(FORMATVERSION_COLUMN, formatVersion)
            result.put(FLAGS_COLUMN, 0)
            return result
        }

        /**
         * Helper method to fill in an incomplete ContentValues with default values.
         * A wordlist ID and a locale are required, otherwise BadFormatException is thrown.
         * @return the same object that was passed in, completed with default values.
         */
        @Throws(BadFormatException::class)
        fun completeWithDefaultValues(result: ContentValues): ContentValues {
            if (null == result.get(WORDLISTID_COLUMN) || null == result.get(LOCALE_COLUMN)) {
                throw BadFormatException()
            }
            // 0 for the pending id, because there is none
            if (null == result.get(PENDINGID_COLUMN)) result.put(PENDINGID_COLUMN, 0)
            // This is a binary blob of a dictionary
            if (null == result.get(TYPE_COLUMN)) result.put(TYPE_COLUMN, TYPE_BULK)
            // This word list is unknown, but it's present, else we wouldn't be here, so INSTALLED
            if (null == result.get(STATUS_COLUMN)) result.put(STATUS_COLUMN, STATUS_INSTALLED)
            // No description unless specified, because we can't guess it
            if (null == result.get(DESCRIPTION_COLUMN)) result.put(DESCRIPTION_COLUMN, "")
            // File name - this is an asset, so it works as an already deleted file.
            //     hence, we need to supply a non-existent file name. Anything will
            //     do as long as it returns false when tested with File#exist(), and
            //     the empty string does not, so it's set to "_".
            if (null == result.get(LOCAL_FILENAME_COLUMN)) result.put(LOCAL_FILENAME_COLUMN, "_")
            // No remote file name : this can't be downloaded. Unless specified.
            if (null == result.get(REMOTE_FILENAME_COLUMN)) result.put(REMOTE_FILENAME_COLUMN, "")
            // 0 for the update date : 1970/1/1. Unless specified.
            if (null == result.get(DATE_COLUMN)) result.put(DATE_COLUMN, 0)
            // Raw checksum unknown unless specified
            if (null == result.get(RAW_CHECKSUM_COLUMN)) result.put(RAW_CHECKSUM_COLUMN, "")
            // Retry column 0 unless specified
            if (null == result.get(RETRY_COUNT_COLUMN)) result.put(
                RETRY_COUNT_COLUMN,
                DICTIONARY_RETRY_THRESHOLD
            )
            // Checksum unknown unless specified
            if (null == result.get(CHECKSUM_COLUMN)) result.put(CHECKSUM_COLUMN, "")
            // No filesize unless specified
            if (null == result.get(FILESIZE_COLUMN)) result.put(FILESIZE_COLUMN, 0)
            // Smallest possible version unless specified
            if (null == result.get(VERSION_COLUMN)) result.put(VERSION_COLUMN, 1)
            // Assume current format unless specified
            if (null == result.get(FORMATVERSION_COLUMN)) result.put(
                FORMATVERSION_COLUMN,
                UpdateHandler.MAXIMUM_SUPPORTED_FORMAT_VERSION
            )
            // No flags unless specified
            if (null == result.get(FLAGS_COLUMN)) result.put(FLAGS_COLUMN, 0)
            return result
        }

        /**
         * Reads a column in a Cursor as a String and stores it in a ContentValues object.
         * @param result the ContentValues object to store the result in.
         * @param cursor the Cursor to read the column from.
         * @param columnId the column ID to read.
         */
        private fun putStringResult(result: ContentValues, cursor: Cursor, columnId: String) {
            result.put(columnId, cursor.getString(cursor.getColumnIndex(columnId)))
        }

        /**
         * Reads a column in a Cursor as an int and stores it in a ContentValues object.
         * @param result the ContentValues object to store the result in.
         * @param cursor the Cursor to read the column from.
         * @param columnId the column ID to read.
         */
        private fun putIntResult(result: ContentValues, cursor: Cursor, columnId: String) {
            result.put(columnId, cursor.getInt(cursor.getColumnIndex(columnId)))
        }

        private fun getFirstLineAsContentValues(cursor: Cursor): ContentValues? {
            val result: ContentValues?
            if (cursor.moveToFirst()) {
                result = ContentValues(COLUMN_COUNT)
                putIntResult(result, cursor, PENDINGID_COLUMN)
                putIntResult(result, cursor, TYPE_COLUMN)
                putIntResult(result, cursor, STATUS_COLUMN)
                putStringResult(result, cursor, WORDLISTID_COLUMN)
                putStringResult(result, cursor, LOCALE_COLUMN)
                putStringResult(result, cursor, DESCRIPTION_COLUMN)
                putStringResult(result, cursor, LOCAL_FILENAME_COLUMN)
                putStringResult(result, cursor, REMOTE_FILENAME_COLUMN)
                putIntResult(result, cursor, DATE_COLUMN)
                putStringResult(result, cursor, RAW_CHECKSUM_COLUMN)
                putStringResult(result, cursor, CHECKSUM_COLUMN)
                putIntResult(result, cursor, RETRY_COUNT_COLUMN)
                putIntResult(result, cursor, FILESIZE_COLUMN)
                putIntResult(result, cursor, VERSION_COLUMN)
                putIntResult(result, cursor, FORMATVERSION_COLUMN)
                putIntResult(result, cursor, FLAGS_COLUMN)
                if (cursor.moveToNext()) {
                    // TODO: print the second level of the stack to the log so that we know
                    // in which code path the error happened
                    Log.e(TAG, "Several SQL results when we expected only one!")
                }
            } else {
                result = null
            }
            return result
        }

        /**
         * Gets the info about as specific download, indexed by its DownloadManager ID.
         * @param db the database to get the information from.
         * @param id the DownloadManager id.
         * @return metadata about this download. This returns all columns in the database.
         */
        fun getContentValuesByPendingId(
            db: SQLiteDatabase,
            id: Long
        ): ContentValues? {
            val cursor: Cursor = db.query(
                METADATA_TABLE_NAME,
                METADATA_TABLE_COLUMNS,
                PENDINGID_COLUMN + "= ?",
                arrayOf(id.toString()),
                null, null, null
            )
            if (null == cursor) {
                return null
            }
            try {
                // There should never be more than one result. If because of some bug there are,
                // returning only one result is the right thing to do, because we couldn't handle
                // several anyway and we should still handle one.
                return getFirstLineAsContentValues(cursor)
            } finally {
                cursor.close()
            }
        }

        /**
         * Gets the info about an installed OR deleting word list with a specified id.
         *
         * Basically, this is the word list that we want to return to Android Keyboard when
         * it asks for a specific id.
         *
         * @param db the database to get the information from.
         * @param id the word list ID.
         * @return the metadata about this word list.
         */
        fun getInstalledOrDeletingWordListContentValuesByWordListId(
            db: SQLiteDatabase, id: String?
        ): ContentValues? {
            val cursor: Cursor = db.query(
                METADATA_TABLE_NAME,
                METADATA_TABLE_COLUMNS,
                WORDLISTID_COLUMN + "=? AND (" + STATUS_COLUMN + "=? OR " + STATUS_COLUMN + "=?)",
                arrayOf(
                    id, STATUS_INSTALLED.toString(),
                    STATUS_DELETING.toString()
                ),
                null, null, null
            )
            if (null == cursor) {
                return null
            }
            try {
                // There should only be one result, but if there are several, we can't tell which
                // is the best, so we just return the first one.
                return getFirstLineAsContentValues(cursor)
            } finally {
                cursor.close()
            }
        }

        /**
         * Given a specific download ID, return records for all pending downloads across all clients.
         *
         * If several clients use the same metadata URL, we know to only download it once, and
         * dispatch the update process across all relevant clients when the download ends. This means
         * several clients may share a single download ID if they share a metadata URI.
         * The dispatching is done in
         * [UpdateHandler.downloadFinished], which
         * finds out about the list of relevant clients by calling this method.
         *
         * @param context a context instance to open the databases
         * @param downloadId the download ID to query about
         * @return the list of records. Never null, but may be empty.
         */
        fun getDownloadRecordsForDownloadId(
            context: Context?,
            downloadId: Long
        ): ArrayList<DownloadRecord> {
            val defaultDb: SQLiteDatabase = getDb(context, "")
            val results: ArrayList<DownloadRecord> = ArrayList()
            val cursor: Cursor = defaultDb.query(
                CLIENT_TABLE_NAME, CLIENT_TABLE_COLUMNS,
                null, null, null, null, null
            )
            try {
                if (!cursor.moveToFirst()) return results
                val clientIdIndex: Int = cursor.getColumnIndex(CLIENT_CLIENT_ID_COLUMN)
                val pendingIdColumn: Int = cursor.getColumnIndex(CLIENT_PENDINGID_COLUMN)
                do {
                    val pendingId: Long = cursor.getInt(pendingIdColumn).toLong()
                    val clientId: String = cursor.getString(clientIdIndex)
                    if (pendingId == downloadId) {
                        results.add(DownloadRecord(clientId, null))
                    }
                    val valuesForThisClient: ContentValues? =
                        getContentValuesByPendingId(getDb(context, clientId), downloadId)
                    if (null != valuesForThisClient) {
                        results.add(DownloadRecord(clientId, valuesForThisClient))
                    }
                } while (cursor.moveToNext())
            } finally {
                cursor.close()
            }
            return results
        }

        /**
         * Gets the info about a specific word list.
         *
         * @param db the database to get the information from.
         * @param id the word list ID.
         * @param version the word list version.
         * @return the metadata about this word list.
         */
        fun getContentValuesByWordListId(
            db: SQLiteDatabase,
            id: String?, version: Int
        ): ContentValues? {
            val cursor: Cursor = db.query(
                METADATA_TABLE_NAME,
                METADATA_TABLE_COLUMNS,
                (WORDLISTID_COLUMN + "= ? AND " + VERSION_COLUMN + "= ? AND "
                        + FORMATVERSION_COLUMN + "<= ?"),
                arrayOf(
                    id,
                    version.toString(),
                    UpdateHandler.MAXIMUM_SUPPORTED_FORMAT_VERSION.toString()
                ),
                null,  /* groupBy */
                null,  /* having */
                FORMATVERSION_COLUMN + " DESC" /* orderBy */
            )
            if (null == cursor) {
                return null
            }
            try {
                // This is a lookup by primary key, so there can't be more than one result.
                return getFirstLineAsContentValues(cursor)
            } finally {
                cursor.close()
            }
        }

        /**
         * Gets the info about the latest word list with an id.
         *
         * @param db the database to get the information from.
         * @param id the word list ID.
         * @return the metadata about the word list with this id and the latest version number.
         */
        fun getContentValuesOfLatestAvailableWordlistById(
            db: SQLiteDatabase, id: String?
        ): ContentValues? {
            val cursor: Cursor = db.query(
                METADATA_TABLE_NAME,
                METADATA_TABLE_COLUMNS,
                WORDLISTID_COLUMN + "= ?",
                arrayOf(id), null, null, VERSION_COLUMN + " DESC", "1"
            )
            if (null == cursor) {
                return null
            }
            try {
                // Return the first result from the list of results.
                return getFirstLineAsContentValues(cursor)
            } finally {
                cursor.close()
            }
        }

        /**
         * Gets the current metadata about INSTALLED, AVAILABLE or DELETING dictionaries.
         *
         * This odd method is tailored to the needs of
         * DictionaryProvider#getDictionaryWordListsForContentUri, which needs the word list if
         * it is:
         * - INSTALLED: this should be returned to LatinIME if the file is still inside the dictionary
         * pack, so that it can be copied. If the file is not there, it's been copied already and should
         * not be returned, so getDictionaryWordListsForContentUri takes care of this.
         * - DELETING: this should be returned to LatinIME so that it can actually delete the file.
         * - AVAILABLE: this should not be returned, but should be checked for auto-installation.
         *
         * @param context the context for getting the database.
         * @param clientId the client id for retrieving the database. null for default (deprecated)
         * @return a cursor with metadata about usable dictionaries.
         */
        fun queryInstalledOrDeletingOrAvailableDictionaryMetadata(
            context: Context?, clientId: String?
        ): Cursor {
            // If clientId is null, we get the defaut DB (see #getInstance() for more about this)
            val results: Cursor = getDb(context, clientId).query(
                METADATA_TABLE_NAME,
                METADATA_TABLE_COLUMNS,
                STATUS_COLUMN + " = ? OR " + STATUS_COLUMN + " = ? OR " + STATUS_COLUMN + " = ?",
                arrayOf(
                    STATUS_INSTALLED.toString(),
                    STATUS_DELETING.toString(),
                    STATUS_AVAILABLE.toString()
                ),
                null, null, LOCALE_COLUMN
            )
            return results
        }

        /**
         * Gets the current metadata about all dictionaries.
         *
         * This will retrieve the metadata about all dictionaries, including
         * older files, or files not yet downloaded.
         *
         * @param context the context for getting the database.
         * @param clientId the client id for retrieving the database. null for default (deprecated)
         * @return a cursor with metadata about usable dictionaries.
         */
        fun queryCurrentMetadata(context: Context?, clientId: String?): Cursor {
            // If clientId is null, we get the defaut DB (see #getInstance() for more about this)
            val results: Cursor = getDb(context, clientId).query(
                METADATA_TABLE_NAME,
                METADATA_TABLE_COLUMNS, null, null, null, null, LOCALE_COLUMN
            )
            return results
        }

        /**
         * Gets the list of all dictionaries known to the dictionary provider, with only public columns.
         *
         * This will retrieve information about all known dictionaries, and their status. As such,
         * it will also return information about dictionaries on the server that have not been
         * downloaded yet, but may be requested.
         * This only returns public columns. It does not populate internal columns in the returned
         * cursor.
         * The value returned by this method is intended to be good to be returned directly for a
         * request of the list of dictionaries by a client.
         *
         * @param context the context to read the database from.
         * @param clientId the client id for retrieving the database. null for default (deprecated)
         * @return a cursor that lists all available dictionaries and their metadata.
         */
        fun queryDictionaries(context: Context?, clientId: String?): Cursor {
            // If clientId is null, we get the defaut DB (see #getInstance() for more about this)
            val results: Cursor = getDb(context, clientId).query(
                METADATA_TABLE_NAME,
                DICTIONARIES_LIST_PUBLIC_COLUMNS,  // Filter out empty locales so as not to return auxiliary data, like a
                // data line for downloading metadata:
                LOCALE_COLUMN + " != ?",
                arrayOf(""),  // TODO: Reinstate the following code for bulk, then implement partial updates
                /*                MetadataDbHelper.TYPE_COLUMN + " = ?",
                new String[] { Integer.toString(MetadataDbHelper.TYPE_BULK) }, */
                null,
                null,
                LOCALE_COLUMN
            )
            return results
        }

        /**
         * Deletes all data associated with a client.
         *
         * @param context the context for opening the database
         * @param clientId the ID of the client to delete.
         * @return true if the client was successfully deleted, false otherwise.
         */
        fun deleteClient(context: Context?, clientId: String?): Boolean {
            // Remove all metadata associated with this client
            val db: SQLiteDatabase = getDb(context, clientId)
            db.execSQL("DROP TABLE IF EXISTS " + METADATA_TABLE_NAME)
            db.execSQL(METADATA_TABLE_CREATE)
            // Remove this client's entry in the clients table
            val defaultDb: SQLiteDatabase = getDb(context, "")
            if (0 == defaultDb.delete(
                    CLIENT_TABLE_NAME,
                    CLIENT_CLIENT_ID_COLUMN + " = ?", arrayOf(clientId)
                )
            ) {
                return false
            }
            return true
        }

        /**
         * Updates information relative to a specific client.
         *
         * Updatable information includes the metadata URI and the additional ID column. It may be
         * expanded in the future.
         * The passed values must include a client ID in the key CLIENT_CLIENT_ID_COLUMN, and it must
         * be equal to the string passed as an argument for clientId. It may not be empty.
         * The passed values must also include a non-null metadata URI in the
         * CLIENT_METADATA_URI_COLUMN column, as well as a non-null additional ID in the
         * CLIENT_METADATA_ADDITIONAL_ID_COLUMN. Both these strings may be empty.
         * If any of the above is not complied with, this function returns without updating data.
         *
         * @param context the context, to open the database
         * @param clientId the ID of the client to update
         * @param values the values to update. Must conform to the protocol (see above)
         */
        fun updateClientInfo(
            context: Context?, clientId: String,
            values: ContentValues
        ) {
            // Validity check the content values
            val valuesClientId: String = values.getAsString(CLIENT_CLIENT_ID_COLUMN)
            val valuesMetadataUri: String? = values.getAsString(CLIENT_METADATA_URI_COLUMN)
            val valuesMetadataAdditionalId: String? =
                values.getAsString(CLIENT_METADATA_ADDITIONAL_ID_COLUMN)
            // Empty string is a valid client ID, but external apps may not configure it, so disallow
            // both null and empty string.
            // Empty string is a valid metadata URI if the client does not want updates, so allow
            // empty string but disallow null.
            // Empty string is a valid additional ID so allow empty string but disallow null.
            if (TextUtils.isEmpty(valuesClientId) || null == valuesMetadataUri || null == valuesMetadataAdditionalId) {
                // We need all these columns to be filled in
                DebugLogUtils.l("Missing parameter for updateClientInfo")
                return
            }
            if (clientId != valuesClientId) {
                // Mismatch! The client violates the protocol.
                DebugLogUtils.l(
                    "Received an updateClientInfo request for ", clientId,
                    " but the values " + "contain a different ID : ", valuesClientId
                )
                return
            }
            // Default value for a pending ID is NOT_AN_ID
            values.put(CLIENT_PENDINGID_COLUMN, UpdateHandler.NOT_AN_ID)
            val defaultDb: SQLiteDatabase = getDb(context, "")
            if (-1L == defaultDb.insert(CLIENT_TABLE_NAME, null, values)) {
                defaultDb.update(
                    CLIENT_TABLE_NAME, values,
                    CLIENT_CLIENT_ID_COLUMN + " = ?", arrayOf(clientId)
                )
            }
        }

        /**
         * Retrieves the list of existing client IDs.
         * @param context the context to open the database
         * @return a cursor containing only one column, and one client ID per line.
         */
        fun queryClientIds(context: Context?): Cursor {
            return getDb(context, null).query(
                CLIENT_TABLE_NAME,
                arrayOf(CLIENT_CLIENT_ID_COLUMN), null, null, null, null, null
            )
        }

        /**
         * Register a download ID for a specific metadata URI.
         *
         * This method should be called when a download for a metadata URI is starting. It will
         * search for all clients using this metadata URI and will register for each of them
         * the download ID into the database for later retrieval by
         * [.getDownloadRecordsForDownloadId].
         *
         * @param context a context for opening databases
         * @param uri the metadata URI
         * @param downloadId the download ID
         */
        fun registerMetadataDownloadId(
            context: Context?, uri: String?,
            downloadId: Long
        ) {
            val values: ContentValues = ContentValues()
            values.put(CLIENT_PENDINGID_COLUMN, downloadId)
            values.put(CLIENT_LAST_UPDATE_DATE_COLUMN, System.currentTimeMillis())
            val defaultDb: SQLiteDatabase = getDb(context, "")
            val cursor: Cursor = queryClientIds(context)
            if (null == cursor) return
            try {
                if (!cursor.moveToFirst()) return
                do {
                    val clientId: String = cursor.getString(0)
                    val metadataUri: String? =
                        getMetadataUriAsString(context, clientId)
                    if (metadataUri == uri) {
                        defaultDb.update(
                            CLIENT_TABLE_NAME, values,
                            CLIENT_CLIENT_ID_COLUMN + " = ?", arrayOf(clientId)
                        )
                    }
                } while (cursor.moveToNext())
            } finally {
                cursor.close()
            }
        }

        /**
         * Marks a downloading entry as having successfully downloaded and being installed.
         *
         * The metadata database contains information about ongoing processes, typically ongoing
         * downloads. This marks such an entry as having finished and having installed successfully,
         * so it becomes INSTALLED.
         *
         * @param db the metadata database.
         * @param r content values about the entry to mark as processed.
         */
        fun markEntryAsFinishedDownloadingAndInstalled(
            db: SQLiteDatabase,
            r: ContentValues
        ) {
            when (r.getAsInteger(TYPE_COLUMN)) {
                TYPE_BULK -> {
                    DebugLogUtils.l("Ended processing a wordlist")
                    // Updating a bulk word list is a three-step operation:
                    // - Add the new entry to the table
                    // - Remove the old entry from the table
                    // - Erase the old file
                    // We start by gathering the names of the files we should delete.
                    val filenames: MutableList<String> = LinkedList()
                    val c: Cursor = db.query(
                        METADATA_TABLE_NAME,
                        arrayOf(LOCAL_FILENAME_COLUMN),
                        LOCALE_COLUMN + " = ? AND " +
                                WORDLISTID_COLUMN + " = ? AND " + STATUS_COLUMN + " = ?",
                        arrayOf(
                            r.getAsString(LOCALE_COLUMN),
                            r.getAsString(WORDLISTID_COLUMN),
                            STATUS_INSTALLED.toString()
                        ),
                        null, null, null
                    )
                    try {
                        if (c.moveToFirst()) {
                            // There should never be more than one file, but if there are, it's a bug
                            // and we should remove them all. I think it might happen if the power of
                            // the phone is suddenly cut during an update.
                            val filenameIndex: Int = c.getColumnIndex(LOCAL_FILENAME_COLUMN)
                            do {
                                DebugLogUtils.l("Setting for removal", c.getString(filenameIndex))
                                filenames.add(c.getString(filenameIndex))
                            } while (c.moveToNext())
                        }
                    } finally {
                        c.close()
                    }
                    r.put(STATUS_COLUMN, STATUS_INSTALLED)
                    db.beginTransactionNonExclusive()
                    // Delete all old entries. There should never be any stalled entries, but if
                    // there are, this deletes them.
                    db.delete(
                        METADATA_TABLE_NAME,
                        WORDLISTID_COLUMN + " = ?",
                        arrayOf(r.getAsString(WORDLISTID_COLUMN))
                    )
                    db.insert(METADATA_TABLE_NAME, null, r)
                    db.setTransactionSuccessful()
                    db.endTransaction()
                    for (filename: String in filenames) {
                        try {
                            val f: File = File(filename)
                            f.delete()
                        } catch (e: SecurityException) {
                            // No permissions to delete. Um. Can't do anything.
                        } // I don't think anything else can be thrown
                    }
                }

                else -> {}
            }
        }

        /**
         * Removes a downloading entry from the database.
         *
         * This is invoked when a download fails. Either we tried to download, but
         * we received a permanent failure and we should remove it, or we got manually
         * cancelled and we should leave it at that.
         *
         * @param db the metadata database.
         * @param id the DownloadManager id of the file.
         */
        fun deleteDownloadingEntry(db: SQLiteDatabase, id: Long) {
            db.delete(
                METADATA_TABLE_NAME, PENDINGID_COLUMN + " = ? AND " + STATUS_COLUMN + " = ?",
                arrayOf(id.toString(), STATUS_DOWNLOADING.toString())
            )
        }

        /**
         * Forcefully removes an entry from the database.
         *
         * This is invoked when a file is broken. The file has been downloaded, but Android
         * Keyboard is telling us it could not open it.
         *
         * @param db the metadata database.
         * @param id the id of the word list.
         * @param version the version of the word list.
         */
        fun deleteEntry(db: SQLiteDatabase, id: String?, version: Int) {
            db.delete(
                METADATA_TABLE_NAME, WORDLISTID_COLUMN + " = ? AND " + VERSION_COLUMN + " = ?",
                arrayOf(id, version.toString())
            )
        }

        /**
         * Internal method that sets the current status of an entry of the database.
         *
         * @param db the metadata database.
         * @param id the id of the word list.
         * @param version the version of the word list.
         * @param status the status to set the word list to.
         * @param downloadId an optional download id to write, or NOT_A_DOWNLOAD_ID
         */
        private fun markEntryAs(
            db: SQLiteDatabase, id: String?,
            version: Int, status: Int, downloadId: Long
        ) {
            val values: ContentValues? = getContentValuesByWordListId(db, id, version)
            values!!.put(STATUS_COLUMN, status)
            if (NOT_A_DOWNLOAD_ID != downloadId) {
                values.put(PENDINGID_COLUMN, downloadId)
            }
            db.update(
                METADATA_TABLE_NAME, values,
                WORDLISTID_COLUMN + " = ? AND " + VERSION_COLUMN + " = ?",
                arrayOf(id, version.toString())
            )
        }

        /**
         * Writes the status column for the wordlist with this id as enabled. Typically this
         * means the word list is currently disabled and we want to set its status to INSTALLED.
         *
         * @param db the metadata database.
         * @param id the id of the word list.
         * @param version the version of the word list.
         */
        fun markEntryAsEnabled(
            db: SQLiteDatabase, id: String?,
            version: Int
        ) {
            markEntryAs(db, id, version, STATUS_INSTALLED, NOT_A_DOWNLOAD_ID)
        }

        /**
         * Writes the status column for the wordlist with this id as disabled. Typically this
         * means the word list is currently installed and we want to set its status to DISABLED.
         *
         * @param db the metadata database.
         * @param id the id of the word list.
         * @param version the version of the word list.
         */
        fun markEntryAsDisabled(
            db: SQLiteDatabase, id: String?,
            version: Int
        ) {
            markEntryAs(db, id, version, STATUS_DISABLED, NOT_A_DOWNLOAD_ID)
        }

        /**
         * Writes the status column for the wordlist with this id as available. This happens for
         * example when a word list has been deleted but can be downloaded again.
         *
         * @param db the metadata database.
         * @param id the id of the word list.
         * @param version the version of the word list.
         */
        fun markEntryAsAvailable(
            db: SQLiteDatabase, id: String?,
            version: Int
        ) {
            markEntryAs(db, id, version, STATUS_AVAILABLE, NOT_A_DOWNLOAD_ID)
        }

        /**
         * Writes the designated word list as downloadable, alongside with its download id.
         *
         * @param db the metadata database.
         * @param id the id of the word list.
         * @param version the version of the word list.
         * @param downloadId the download id.
         */
        fun markEntryAsDownloading(
            db: SQLiteDatabase, id: String?,
            version: Int, downloadId: Long
        ) {
            markEntryAs(db, id, version, STATUS_DOWNLOADING, downloadId)
        }

        /**
         * Writes the designated word list as deleting.
         *
         * @param db the metadata database.
         * @param id the id of the word list.
         * @param version the version of the word list.
         */
        fun markEntryAsDeleting(
            db: SQLiteDatabase, id: String?,
            version: Int
        ) {
            markEntryAs(db, id, version, STATUS_DELETING, NOT_A_DOWNLOAD_ID)
        }

        /**
         * Checks retry counts and marks the word list as retrying if retry is possible.
         *
         * @param db the metadata database.
         * @param id the id of the word list.
         * @param version the version of the word list.
         * @return `true` if the retry is possible.
         */
        fun maybeMarkEntryAsRetrying(
            db: SQLiteDatabase, id: String?,
            version: Int
        ): Boolean {
            val values: ContentValues? = getContentValuesByWordListId(db, id, version)
            val retryCount: Int = values!!.getAsInteger(RETRY_COUNT_COLUMN)
            if (retryCount > 1) {
                values.put(STATUS_COLUMN, STATUS_RETRYING)
                values.put(RETRY_COUNT_COLUMN, retryCount - 1)
                db.update(
                    METADATA_TABLE_NAME, values,
                    WORDLISTID_COLUMN + " = ? AND " + VERSION_COLUMN + " = ?",
                    arrayOf(id, version.toString())
                )
                return true
            }
            return false
        }
    }
}
