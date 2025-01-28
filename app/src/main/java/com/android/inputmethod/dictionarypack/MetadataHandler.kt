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
import android.util.Log
import java.io.IOException
import java.io.InputStreamReader
import java.util.Collections

/**
 * Helper class to easy up manipulation of dictionary pack metadata.
 */
object MetadataHandler {
    val TAG: String = MetadataHandler::class.java.getSimpleName()

    // The canonical file name for metadata. This is not the name of a real file on the
    // device, but a symbolic name used in the database and in metadata handling. It is never
    // tested against, only used for human-readability as the file name for the metadata.
    const val METADATA_FILENAME: String = "metadata.json"

    /**
     * Reads the data from the cursor and store it in metadata objects.
     * @param results the cursor to read data from.
     * @return the constructed list of wordlist metadata.
     */
    private fun makeMetadataObject(results: Cursor?): List<WordListMetadata> {
        val buildingMetadata: ArrayList<WordListMetadata> = ArrayList()
        if (null != results && results.moveToFirst()) {
            val localeColumn: Int = results.getColumnIndex(MetadataDbHelper.LOCALE_COLUMN)
            val typeColumn: Int = results.getColumnIndex(MetadataDbHelper.TYPE_COLUMN)
            val descriptionColumn: Int =
                results.getColumnIndex(MetadataDbHelper.DESCRIPTION_COLUMN)
            val idIndex: Int = results.getColumnIndex(MetadataDbHelper.WORDLISTID_COLUMN)
            val updateIndex: Int = results.getColumnIndex(MetadataDbHelper.DATE_COLUMN)
            val fileSizeIndex: Int =
                results.getColumnIndex(MetadataDbHelper.FILESIZE_COLUMN)
            val rawChecksumIndex: Int =
                results.getColumnIndex(MetadataDbHelper.RAW_CHECKSUM_COLUMN)
            val checksumIndex: Int =
                results.getColumnIndex(MetadataDbHelper.CHECKSUM_COLUMN)
            val retryCountIndex: Int =
                results.getColumnIndex(MetadataDbHelper.RETRY_COUNT_COLUMN)
            val localFilenameIndex: Int =
                results.getColumnIndex(MetadataDbHelper.LOCAL_FILENAME_COLUMN)
            val remoteFilenameIndex: Int =
                results.getColumnIndex(MetadataDbHelper.REMOTE_FILENAME_COLUMN)
            val versionIndex: Int =
                results.getColumnIndex(MetadataDbHelper.VERSION_COLUMN)
            val formatVersionIndex: Int =
                results.getColumnIndex(MetadataDbHelper.FORMATVERSION_COLUMN)
            do {
                buildingMetadata.add(
                    WordListMetadata(
                        results.getString(idIndex),
                        results.getInt(typeColumn),
                        results.getString(descriptionColumn),
                        results.getLong(updateIndex),
                        results.getLong(fileSizeIndex),
                        results.getString(rawChecksumIndex),
                        results.getString(checksumIndex),
                        results.getInt(retryCountIndex),
                        results.getString(localFilenameIndex),
                        results.getString(remoteFilenameIndex),
                        results.getInt(versionIndex),
                        results.getInt(formatVersionIndex),
                        0, results.getString(localeColumn)
                    )
                )
            } while (results.moveToNext())
        }
        return Collections.unmodifiableList(buildingMetadata)
    }

    /**
     * Gets the whole metadata, for installed and not installed dictionaries.
     * @param context The context to open files over.
     * @param clientId the client id for retrieving the database. null for default (deprecated)
     * @return The current metadata.
     */
    fun getCurrentMetadata(
        context: Context?,
        clientId: String?
    ): List<WordListMetadata> {
        // If clientId is null, we get a cursor on the default database (see
        // MetadataDbHelper#getInstance() for more on this)
        val results: Cursor = MetadataDbHelper.queryCurrentMetadata(context, clientId)
        // If null, we should return makeMetadataObject(null), so we go through.
        try {
            return makeMetadataObject(results)
        } finally {
            if (null != results) {
                results.close()
            }
        }
    }

    /**
     * Gets the metadata, for a specific dictionary.
     *
     * @param context The context to open files over.
     * @param clientId the client id for retrieving the database. null for default (deprecated).
     * @param wordListId the word list ID.
     * @param version the word list version.
     * @return the current metaData
     */
    fun getCurrentMetadataForWordList(
        context: Context?,
        clientId: String, wordListId: String?, version: Int
    ): WordListMetadata? {
        val contentValues: ContentValues? = MetadataDbHelper.getContentValuesByWordListId(
            MetadataDbHelper.getDb(context, clientId), wordListId, version
        )
        if (contentValues == null) {
            // TODO: Figure out why this would happen.
            // Check if this happens when the metadata gets updated in the background.
            Log.e(
                TAG, String.format(
                    "Unable to find the current metadata for wordlist "
                            + "(clientId=%s, wordListId=%s, version=%d) on the database",
                    clientId, wordListId, version
                )
            )
            return null
        }
        return WordListMetadata.createFromContentValues(contentValues)
    }

    /**
     * Read metadata from a stream.
     * @param input The stream to read from.
     * @return The read metadata.
     * @throws IOException if the input stream cannot be read
     * @throws BadFormatException if the stream is not in a known format
     */
    @Throws(IOException::class, BadFormatException::class)
    fun readMetadata(input: InputStreamReader?): List<WordListMetadata> {
        return MetadataParser.parseMetadata(input)
    }

    /**
     * Finds a single WordListMetadata inside a whole metadata chunk.
     *
     * Searches through the whole passed metadata for the first WordListMetadata associated
     * with the passed ID. If several metadata chunks with the same id are found, it will
     * always return the one with the bigger FormatVersion that is less or equal than the
     * maximum supported format version (as listed in UpdateHandler).
     * This will NEVER return the metadata with a FormatVersion bigger than what is supported,
     * even if it is the only word list with this ID.
     *
     * @param metadata the metadata to search into.
     * @param id the word list ID of the metadata to find.
     * @return the associated metadata, or null if not found.
     */
    fun findWordListById(
        metadata: List<WordListMetadata>,
        id: String
    ): WordListMetadata? {
        var bestWordList: WordListMetadata? = null
        var bestFormatVersion: Int = Int.MIN_VALUE // To be sure we can't be inadvertently smaller
        for (wordList: WordListMetadata in metadata) {
            if (id == wordList.mId
                && wordList.mFormatVersion <= UpdateHandler.MAXIMUM_SUPPORTED_FORMAT_VERSION && wordList.mFormatVersion > bestFormatVersion
            ) {
                bestWordList = wordList
                bestFormatVersion = wordList.mFormatVersion
            }
        }
        // If we didn't find any match we'll return null.
        return bestWordList
    }
}
