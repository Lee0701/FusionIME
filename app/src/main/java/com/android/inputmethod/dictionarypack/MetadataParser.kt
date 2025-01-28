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

import android.text.TextUtils
import android.util.JsonReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Collections
import java.util.TreeMap

/**
 * Helper class containing functions to parse the dictionary metadata.
 */
object MetadataParser {
    // Name of the fields in the JSON-formatted file.
    private val ID_FIELD_NAME: String = MetadataDbHelper.WORDLISTID_COLUMN
    private const val LOCALE_FIELD_NAME: String = "locale"
    private val DESCRIPTION_FIELD_NAME: String = MetadataDbHelper.DESCRIPTION_COLUMN
    private const val UPDATE_FIELD_NAME: String = "update"
    private val FILESIZE_FIELD_NAME: String = MetadataDbHelper.FILESIZE_COLUMN
    private val RAW_CHECKSUM_FIELD_NAME: String = MetadataDbHelper.RAW_CHECKSUM_COLUMN
    private val CHECKSUM_FIELD_NAME: String = MetadataDbHelper.CHECKSUM_COLUMN
    private val REMOTE_FILENAME_FIELD_NAME: String =
        MetadataDbHelper.REMOTE_FILENAME_COLUMN
    private val VERSION_FIELD_NAME: String = MetadataDbHelper.VERSION_COLUMN
    private val FORMATVERSION_FIELD_NAME: String = MetadataDbHelper.FORMATVERSION_COLUMN

    /**
     * Parse one JSON-formatted word list metadata.
     * @param reader the reader containing the data.
     * @return a WordListMetadata object from the parsed data.
     * @throws IOException if the underlying reader throws IOException during reading.
     */
    @Throws(IOException::class, BadFormatException::class)
    private fun parseOneWordList(reader: JsonReader): WordListMetadata {
        val arguments: TreeMap<String, String> = TreeMap()
        reader.beginObject()
        while (reader.hasNext()) {
            val name: String = reader.nextName()
            if (!TextUtils.isEmpty(name)) {
                arguments.put(name, reader.nextString())
            }
        }
        reader.endObject()
        if (TextUtils.isEmpty(arguments.get(ID_FIELD_NAME))
            || TextUtils.isEmpty(arguments.get(LOCALE_FIELD_NAME))
            || TextUtils.isEmpty(arguments.get(DESCRIPTION_FIELD_NAME))
            || TextUtils.isEmpty(arguments.get(UPDATE_FIELD_NAME))
            || TextUtils.isEmpty(arguments.get(FILESIZE_FIELD_NAME))
            || TextUtils.isEmpty(arguments.get(CHECKSUM_FIELD_NAME))
            || TextUtils.isEmpty(arguments.get(REMOTE_FILENAME_FIELD_NAME))
            || TextUtils.isEmpty(arguments.get(VERSION_FIELD_NAME))
            || TextUtils.isEmpty(arguments.get(FORMATVERSION_FIELD_NAME))
        ) {
            throw BadFormatException(arguments.toString())
        }
        // TODO: need to find out whether it's bulk or update
        // The null argument is the local file name, which is not known at this time and will
        // be decided later.
        return WordListMetadata(
            arguments[ID_FIELD_NAME]!!,
            MetadataDbHelper.TYPE_BULK,
            arguments[DESCRIPTION_FIELD_NAME],
            arguments[UPDATE_FIELD_NAME]!!.toLong(),
            arguments[FILESIZE_FIELD_NAME]!!.toLong(),
            arguments[RAW_CHECKSUM_FIELD_NAME],
            arguments[CHECKSUM_FIELD_NAME],
            MetadataDbHelper.DICTIONARY_RETRY_THRESHOLD,  /* retryCount */
            null,
            arguments[REMOTE_FILENAME_FIELD_NAME],
            arguments[VERSION_FIELD_NAME]!!.toInt(),
            arguments[FORMATVERSION_FIELD_NAME]!!
                .toInt(),
            0, arguments[LOCALE_FIELD_NAME]
        )
    }

    /**
     * Parses metadata in the JSON format.
     * @param input a stream reader expected to contain JSON formatted metadata.
     * @return dictionary metadata, as an array of WordListMetadata objects.
     * @throws IOException if the underlying reader throws IOException during reading.
     * @throws BadFormatException if the data was not in the expected format.
     */
    @Throws(IOException::class, BadFormatException::class)
    fun parseMetadata(input: InputStreamReader?): List<WordListMetadata> {
        val reader: JsonReader = JsonReader(input)
        val readInfo: ArrayList<WordListMetadata> = ArrayList()
        reader.beginArray()
        while (reader.hasNext()) {
            val thisMetadata: WordListMetadata = parseOneWordList(reader)
            if (!TextUtils.isEmpty(thisMetadata.mLocale)) readInfo.add(thisMetadata)
        }
        return Collections.unmodifiableList(readInfo)
    }
}
