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
import javax.annotation.Nonnull

/**
 * The metadata for a single word list.
 *
 * Instances of this class are always immutable.
 */
class WordListMetadata(
    id: String, type: Int,
    description: String?, lastUpdate: Long, fileSize: Long,
    rawChecksum: String?, checksum: String?, retryCount: Int,
    localFilename: String?, remoteFilename: String?,
    version: Int, formatVersion: Int,
    flags: Int, locale: String?
) {
    val mId: String
    val mType: Int // Type, as of MetadataDbHelper#TYPE_*
    val mDescription: String?
    val mLastUpdate: Long
    val mFileSize: Long
    val mRawChecksum: String?
    val mChecksum: String?
    val mLocalFilename: String?
    val mRemoteFilename: String?
    val mVersion: Int // version of this word list
    val mFlags: Int // Always 0 in this version, reserved for future use
    var mRetryCount: Int

    // The locale is matched against the locale requested by the client. The matching algorithm
    // is a standard locale matching with fallback; it is implemented in
    // DictionaryProvider#getDictionaryFileForContentUri.
    val mLocale: String?


    // Version number of the format.
    // This implementation of the DictionaryDataService knows how to handle format 1 only.
    // This is only for forward compatibility, to be able to upgrade the format without
    // breaking old implementations.
    val mFormatVersion: Int

    init {
        mId = id
        mType = type
        mDescription = description
        mLastUpdate = lastUpdate // In milliseconds
        mFileSize = fileSize
        mRawChecksum = rawChecksum
        mChecksum = checksum
        mRetryCount = retryCount
        mLocalFilename = localFilename
        mRemoteFilename = remoteFilename
        mVersion = version
        mFormatVersion = formatVersion
        mFlags = flags
        mLocale = locale
    }

    override fun toString(): String {
        val sb: StringBuilder = StringBuilder(WordListMetadata::class.java.getSimpleName())
        sb.append(" : ").append(mId)
        sb.append("\nType : ").append(mType)
        sb.append("\nDescription : ").append(mDescription)
        sb.append("\nLastUpdate : ").append(mLastUpdate)
        sb.append("\nFileSize : ").append(mFileSize)
        sb.append("\nRawChecksum : ").append(mRawChecksum)
        sb.append("\nChecksum : ").append(mChecksum)
        sb.append("\nRetryCount: ").append(mRetryCount)
        sb.append("\nLocalFilename : ").append(mLocalFilename)
        sb.append("\nRemoteFilename : ").append(mRemoteFilename)
        sb.append("\nVersion : ").append(mVersion)
        sb.append("\nFormatVersion : ").append(mFormatVersion)
        sb.append("\nFlags : ").append(mFlags)
        sb.append("\nLocale : ").append(mLocale)
        return sb.toString()
    }

    companion object {
        /**
         * Create a WordListMetadata from the contents of a ContentValues.
         *
         * If this lacks any required field, IllegalArgumentException is thrown.
         */
        fun createFromContentValues(@Nonnull values: ContentValues): WordListMetadata {
            val id: String = values.getAsString(MetadataDbHelper.WORDLISTID_COLUMN)
            val type: Int = values.getAsInteger(MetadataDbHelper.TYPE_COLUMN)
            val description: String? =
                values.getAsString(MetadataDbHelper.DESCRIPTION_COLUMN)
            val lastUpdate: Long = values.getAsLong(MetadataDbHelper.DATE_COLUMN)
            val fileSize: Long = values.getAsLong(MetadataDbHelper.FILESIZE_COLUMN)
            val rawChecksum: String =
                values.getAsString(MetadataDbHelper.RAW_CHECKSUM_COLUMN)
            val checksum: String? = values.getAsString(MetadataDbHelper.CHECKSUM_COLUMN)
            val retryCount: Int = values.getAsInteger(MetadataDbHelper.RETRY_COUNT_COLUMN)
            val localFilename: String? =
                values.getAsString(MetadataDbHelper.LOCAL_FILENAME_COLUMN)
            val remoteFilename: String? =
                values.getAsString(MetadataDbHelper.REMOTE_FILENAME_COLUMN)
            val version: Int = values.getAsInteger(MetadataDbHelper.VERSION_COLUMN)
            val formatVersion: Int =
                values.getAsInteger(MetadataDbHelper.FORMATVERSION_COLUMN)
            val flags: Int = values.getAsInteger(MetadataDbHelper.FLAGS_COLUMN)
            val locale: String? = values.getAsString(MetadataDbHelper.LOCALE_COLUMN)
            require(!(null == id || null == type || null == description || null == lastUpdate || null == fileSize || null == checksum || null == localFilename || null == remoteFilename || null == version || null == formatVersion || null == flags || null == locale))
            return WordListMetadata(
                id, type, description, lastUpdate, fileSize, rawChecksum,
                checksum, retryCount, localFilename, remoteFilename, version, formatVersion,
                flags, locale
            )
        }
    }
}
