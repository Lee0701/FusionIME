/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.inputmethod.latin

import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import com.android.inputmethod.dictionarypack.DictionaryPackConstants
import com.android.inputmethod.dictionarypack.MD5Calculator
import com.android.inputmethod.dictionarypack.UpdateHandler
import com.android.inputmethod.latin.common.FileUtils
import com.android.inputmethod.latin.define.DecoderSpecificConstants
import com.android.inputmethod.latin.utils.DictionaryInfoUtils
import com.android.inputmethod.latin.utils.DictionaryInfoUtils.DictionaryInfo
import com.android.inputmethod.latin.utils.FileTransforms
import com.android.inputmethod.latin.utils.MetadataFileUriGetter
import ee.oyatl.ime.fusion.R
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale

/**
 * Group class for static methods to help with creation and getting of the binary dictionary
 * file from the dictionary provider
 */
object BinaryDictionaryFileDumper {
    private val TAG: String = BinaryDictionaryFileDumper::class.java.getSimpleName()
    private const val DEBUG: Boolean = false

    /**
     * The size of the temporary buffer to copy files.
     */
    private const val FILE_READ_BUFFER_SIZE: Int = 8192

    // TODO: make the following data common with the native code
    private val MAGIC_NUMBER_VERSION_1: ByteArray =
        byteArrayOf(0x78.toByte(), 0xB1.toByte(), 0x00.toByte(), 0x00.toByte())
    private val MAGIC_NUMBER_VERSION_2: ByteArray =
        byteArrayOf(0x9B.toByte(), 0xC1.toByte(), 0x3A.toByte(), 0xFE.toByte())

    private val SHOULD_VERIFY_MAGIC_NUMBER: Boolean =
        DecoderSpecificConstants.SHOULD_VERIFY_MAGIC_NUMBER
    private val SHOULD_VERIFY_CHECKSUM: Boolean = DecoderSpecificConstants.SHOULD_VERIFY_CHECKSUM

    private val DICTIONARY_PROJECTION: Array<String> = arrayOf("id")

    private const val QUERY_PARAMETER_MAY_PROMPT_USER: String = "mayPrompt"
    private const val QUERY_PARAMETER_TRUE: String = "true"
    private const val QUERY_PARAMETER_DELETE_RESULT: String = "result"
    private const val QUERY_PARAMETER_SUCCESS: String = "success"
    private const val QUERY_PARAMETER_FAILURE: String = "failure"

    // Using protocol version 2 to communicate with the dictionary pack
    private const val QUERY_PARAMETER_PROTOCOL: String = "protocol"
    private const val QUERY_PARAMETER_PROTOCOL_VALUE: String = "2"

    // The path fragment to append after the client ID for dictionary info requests.
    private const val QUERY_PATH_DICT_INFO: String = "dict"

    // The path fragment to append after the client ID for dictionary datafile requests.
    private const val QUERY_PATH_DATAFILE: String = "datafile"

    // The path fragment to append after the client ID for updating the metadata URI.
    private const val QUERY_PATH_METADATA: String = "metadata"
    private const val INSERT_METADATA_CLIENT_ID_COLUMN: String = "clientid"
    private const val INSERT_METADATA_METADATA_URI_COLUMN: String = "uri"
    private const val INSERT_METADATA_METADATA_ADDITIONAL_ID_COLUMN: String = "additionalid"

    /**
     * Returns a URI builder pointing to the dictionary pack.
     *
     * This creates a URI builder able to build a URI pointing to the dictionary
     * pack content provider for a specific dictionary id.
     */
    fun getProviderUriBuilder(path: String?): Uri.Builder {
        return Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
            .authority(DictionaryPackConstants.AUTHORITY).appendPath(path)
    }

    /**
     * Gets the content URI builder for a specified type.
     *
     * Supported types include QUERY_PATH_DICT_INFO, which takes the locale as
     * the extraPath argument, and QUERY_PATH_DATAFILE, which needs a wordlist ID
     * as the extraPath argument.
     *
     * @param clientId the clientId to use
     * @param contentProviderClient the instance of content provider client
     * @param queryPathType the path element encoding the type
     * @param extraPath optional extra argument for this type (typically word list id)
     * @return a builder that can build the URI for the best supported protocol version
     * @throws RemoteException if the client can't be contacted
     */
    @Throws(RemoteException::class)
    private fun getContentUriBuilderForType(
        clientId: String,
        contentProviderClient: ContentProviderClient, queryPathType: String,
        extraPath: String?
    ): Uri.Builder {
        // Check whether protocol v2 is supported by building a v2 URI and calling getType()
        // on it. If this returns null, v2 is not supported.
        val uriV2Builder: Uri.Builder = getProviderUriBuilder(clientId)
        uriV2Builder.appendPath(queryPathType)
        uriV2Builder.appendPath(extraPath)
        uriV2Builder.appendQueryParameter(
            QUERY_PARAMETER_PROTOCOL,
            QUERY_PARAMETER_PROTOCOL_VALUE
        )
        if (null != contentProviderClient.getType(uriV2Builder.build())) return uriV2Builder
        // Protocol v2 is not supported, so create and return the protocol v1 uri.
        return getProviderUriBuilder(extraPath)
    }

    /**
     * Queries a content provider for the list of word lists for a specific locale
     * available to copy into Latin IME.
     */
    private fun getWordListWordListInfos(
        locale: Locale,
        context: Context, hasDefaultWordList: Boolean
    ): List<WordListInfo> {
        val clientId: String = context.getString(R.string.dictionary_pack_client_id)
        val client: ContentProviderClient =
            context.contentResolver.acquireContentProviderClient(getProviderUriBuilder("").build())
                ?: return emptyList()
        var cursor: Cursor? = null
        try {
            val builder: Uri.Builder = getContentUriBuilderForType(
                clientId, client,
                QUERY_PATH_DICT_INFO, locale.toString()
            )
            if (!hasDefaultWordList) {
                builder.appendQueryParameter(
                    QUERY_PARAMETER_MAY_PROMPT_USER,
                    QUERY_PARAMETER_TRUE
                )
            }
            val queryUri: Uri = builder.build()
            val isProtocolV2: Boolean =
                (QUERY_PARAMETER_PROTOCOL_VALUE == queryUri.getQueryParameter(
                    QUERY_PARAMETER_PROTOCOL
                ))

            cursor = client.query(queryUri, DICTIONARY_PROJECTION, null, null, null)
            if (isProtocolV2 && null == cursor) {
                reinitializeClientRecordInDictionaryContentProvider(context, client, clientId)
                cursor = client.query(queryUri, DICTIONARY_PROJECTION, null, null, null)
            }
            if (null == cursor) return emptyList()
            if (cursor.getCount() <= 0 || !cursor.moveToFirst()) {
                return emptyList()
            }
            val list: ArrayList<WordListInfo> = ArrayList()
            do {
                val wordListId: String = cursor.getString(0)
                val wordListLocale: String = cursor.getString(1)
                val wordListRawChecksum: String = cursor.getString(2)
                if (TextUtils.isEmpty(wordListId)) continue
                list.add(WordListInfo(wordListId, wordListLocale, wordListRawChecksum))
            } while (cursor.moveToNext())
            return list
        } catch (e: RemoteException) {
            // The documentation is unclear as to in which cases this may happen, but it probably
            // happens when the content provider got suddenly killed because it crashed or because
            // the user disabled it through Settings.
            Log.e(TAG, "RemoteException: communication with the dictionary pack cut", e)
            return emptyList()
        } catch (e: Exception) {
            // A crash here is dangerous because crashing here would brick any encrypted device -
            // we need the keyboard to be up and working to enter the password, so we don't want
            // to die no matter what. So let's be as safe as possible.
            Log.e(TAG, "Unexpected exception communicating with the dictionary pack", e)
            return emptyList()
        } finally {
            cursor?.close()
            client.release()
        }
    }


    /**
     * Helper method to encapsulate exception handling.
     */
    private fun openAssetFileDescriptor(
        providerClient: ContentProviderClient, uri: Uri
    ): AssetFileDescriptor? {
        try {
            return providerClient.openAssetFile(uri, "r")
        } catch (e: FileNotFoundException) {
            // I don't want to log the word list URI here for security concerns. The exception
            // contains the name of the file, so let's not pass it to Log.e here.
            Log.e(
                TAG,
                "Could not find a word list from the dictionary provider." /* intentionally don't pass the exception (see comment above) */
            )
            return null
        } catch (e: RemoteException) {
            Log.e(TAG, "Can't communicate with the dictionary pack", e)
            return null
        }
    }

    /**
     * Stages a word list the id of which is passed as an argument. This will write the file
     * to the cache file name designated by its id and locale, overwriting it if already present
     * and creating it (and its containing directory) if necessary.
     */
    private fun installWordListToStaging(
        wordlistId: String, locale: String,
        rawChecksum: String, providerClient: ContentProviderClient,
        context: Context
    ) {
        val COMPRESSED_CRYPTED_COMPRESSED: Int = 0
        val CRYPTED_COMPRESSED: Int = 1
        val COMPRESSED_CRYPTED: Int = 2
        val COMPRESSED_ONLY: Int = 3
        val CRYPTED_ONLY: Int = 4
        val NONE: Int = 5
        val MODE_MIN: Int = COMPRESSED_CRYPTED_COMPRESSED
        val MODE_MAX: Int = NONE

        val clientId: String = context.getString(R.string.dictionary_pack_client_id)
        val wordListUriBuilder: Uri.Builder
        try {
            wordListUriBuilder = getContentUriBuilderForType(
                clientId,
                providerClient, QUERY_PATH_DATAFILE, wordlistId /* extraPath */
            )
        } catch (e: RemoteException) {
            Log.e(TAG, "Can't communicate with the dictionary pack", e)
            return
        }
        val finalFileName: String =
            DictionaryInfoUtils.getStagingFileName(wordlistId, locale, context)
        val tempFileName: String
        try {
            tempFileName = BinaryDictionaryGetter.getTempFileName(wordlistId, context)
        } catch (e: IOException) {
            Log.e(TAG, "Can't open the temporary file", e)
            return
        }

        for (mode in MODE_MIN..MODE_MAX) {
            val originalSourceStream: InputStream
            var inputStream: InputStream? = null
            var uncompressedStream: InputStream? = null
            var decryptedStream: InputStream? = null
            var bufferedInputStream: BufferedInputStream? = null
            var outputFile: File? = null
            var bufferedOutputStream: BufferedOutputStream? = null
            var afd: AssetFileDescriptor? = null
            val wordListUri: Uri = wordListUriBuilder.build()
            try {
                // Open input.
                afd = openAssetFileDescriptor(providerClient, wordListUri)
                // If we can't open it at all, don't even try a number of times.
                if (null == afd) return
                originalSourceStream = afd.createInputStream()
                // Open output.
                outputFile = File(tempFileName)
                // Just to be sure, delete the file. This may fail silently, and return false: this
                // is the right thing to do, as we just want to continue anyway.
                outputFile.delete()
                // Get the appropriate decryption method for this try
                when (mode) {
                    COMPRESSED_CRYPTED_COMPRESSED -> {
                        uncompressedStream =
                            FileTransforms.getUncompressedStream(originalSourceStream)
                        decryptedStream = FileTransforms.getDecryptedStream(uncompressedStream)
                        inputStream = FileTransforms.getUncompressedStream(decryptedStream)
                    }

                    CRYPTED_COMPRESSED -> {
                        decryptedStream = FileTransforms.getDecryptedStream(originalSourceStream)
                        inputStream = FileTransforms.getUncompressedStream(decryptedStream)
                    }

                    COMPRESSED_CRYPTED -> {
                        uncompressedStream =
                            FileTransforms.getUncompressedStream(originalSourceStream)
                        inputStream = FileTransforms.getDecryptedStream(uncompressedStream)
                    }

                    COMPRESSED_ONLY -> inputStream =
                        FileTransforms.getUncompressedStream(originalSourceStream)

                    CRYPTED_ONLY -> inputStream =
                        FileTransforms.getDecryptedStream(originalSourceStream)

                    NONE -> inputStream = originalSourceStream
                }
                bufferedInputStream = BufferedInputStream(inputStream)
                bufferedOutputStream = BufferedOutputStream(FileOutputStream(outputFile))
                checkMagicAndCopyFileTo(bufferedInputStream, bufferedOutputStream)
                bufferedOutputStream.flush()
                bufferedOutputStream.close()

                if (SHOULD_VERIFY_CHECKSUM) {
                    val actualRawChecksum: String? = MD5Calculator.checksum(
                        BufferedInputStream(FileInputStream(outputFile))
                    )
                    Log.i(
                        TAG, ("Computed checksum for downloaded dictionary. Expected = "
                                + rawChecksum + " ; actual = " + actualRawChecksum)
                    )
                    if (!TextUtils.isEmpty(rawChecksum) && rawChecksum != actualRawChecksum) {
                        throw IOException(
                            "Could not decode the file correctly : checksum differs"
                        )
                    }
                }

                // move the output file to the final staging file.
                val finalFile = File(finalFileName)
                if (!FileUtils.renameTo(outputFile, finalFile)) {
                    Log.e(
                        TAG, String.format(
                            "Failed to rename from %s to %s.",
                            outputFile.getAbsoluteFile(), finalFile.getAbsoluteFile()
                        )
                    )
                }

                wordListUriBuilder.appendQueryParameter(
                    QUERY_PARAMETER_DELETE_RESULT,
                    QUERY_PARAMETER_SUCCESS
                )
                if (0 >= providerClient.delete(wordListUriBuilder.build(), null, null)) {
                    Log.e(TAG, "Could not have the dictionary pack delete a word list")
                }
                Log.d(TAG, "Successfully copied file for wordlist ID $wordlistId")
                // Success! Close files (through the finally{} clause) and return.
                return
            } catch (e: Exception) {
                if (DEBUG) {
                    Log.e(TAG, "Can't open word list in mode $mode", e)
                }
                if (null != outputFile) {
                    // This may or may not fail. The file may not have been created if the
                    // exception was thrown before it could be. Hence, both failure and
                    // success are expected outcomes, so we don't check the return value.
                    outputFile.delete()
                }
                // Try the next method.
            } finally {
                // Ignore exceptions while closing files.
                closeAssetFileDescriptorAndReportAnyException(afd)
                closeCloseableAndReportAnyException(inputStream)
                closeCloseableAndReportAnyException(uncompressedStream)
                closeCloseableAndReportAnyException(decryptedStream)
                closeCloseableAndReportAnyException(bufferedInputStream)
                closeCloseableAndReportAnyException(bufferedOutputStream)
            }
        }

        // We could not copy the file at all. This is very unexpected.
        // I'd rather not print the word list ID to the log out of security concerns
        Log.e(TAG, "Could not copy a word list. Will not be able to use it.")
        // If we can't copy it we should warn the dictionary provider so that it can mark it
        // as invalid.
        reportBrokenFileToDictionaryProvider(providerClient, clientId, wordlistId)
    }

    fun reportBrokenFileToDictionaryProvider(
        providerClient: ContentProviderClient, clientId: String,
        wordlistId: String?
    ): Boolean {
        try {
            val wordListUriBuilder: Uri.Builder = getContentUriBuilderForType(
                clientId,
                providerClient, QUERY_PATH_DATAFILE, wordlistId /* extraPath */
            )
            wordListUriBuilder.appendQueryParameter(
                QUERY_PARAMETER_DELETE_RESULT,
                QUERY_PARAMETER_FAILURE
            )
            if (0 >= providerClient.delete(wordListUriBuilder.build(), null, null)) {
                Log.e(TAG, "Unable to delete a word list.")
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "Communication with the dictionary provider was cut", e)
            return false
        }
        return true
    }

    // Ideally the two following methods should be merged, but AssetFileDescriptor does not
    // implement Closeable although it does implement #close(), and Java does not have
    // structural typing.
    private fun closeAssetFileDescriptorAndReportAnyException(
        file: AssetFileDescriptor?
    ) {
        try {
            if (null != file) file.close()
        } catch (e: Exception) {
            Log.e(TAG, "Exception while closing a file", e)
        }
    }

    private fun closeCloseableAndReportAnyException(file: Closeable?) {
        try {
            if (null != file) file.close()
        } catch (e: Exception) {
            Log.e(TAG, "Exception while closing a file", e)
        }
    }

    /**
     * Queries a content provider for word list data for some locale and stage the returned files
     *
     * This will query a content provider for word list data for a given locale, and copy the
     * files locally so that they can be mmap'ed. This may overwrite previously cached word lists
     * with newer versions if a newer version is made available by the content provider.
     * @throw FileNotFoundException if the provider returns non-existent data.
     * @throw IOException if the provider-returned data could not be read.
     */
    fun installDictToStagingFromContentProvider(
        locale: Locale,
        context: Context, hasDefaultWordList: Boolean
    ) {
        val providerClient: ContentProviderClient?
        try {
            providerClient = context.contentResolver.acquireContentProviderClient(getProviderUriBuilder("").build())
        } catch (e: SecurityException) {
            Log.e(TAG, "No permission to communicate with the dictionary provider", e)
            return
        }
        if (null == providerClient) {
            Log.e(TAG, "Can't establish communication with the dictionary provider")
            return
        }
        try {
            val idList: List<WordListInfo> = getWordListWordListInfos(
                locale, context,
                hasDefaultWordList
            )
            for (id: WordListInfo in idList) {
                installWordListToStaging(
                    id.mId, id.mLocale, id.mRawChecksum, providerClient,
                    context
                )
            }
        } finally {
            providerClient.release()
        }
    }

    /**
     * Downloads the dictionary if it was never requested/used.
     *
     * @param locale locale to download
     * @param context the context for resources and providers.
     * @param hasDefaultWordList whether the default wordlist exists in the resources.
     */
    fun downloadDictIfNeverRequested(
        locale: Locale,
        context: Context, hasDefaultWordList: Boolean
    ) {
        getWordListWordListInfos(locale, context, hasDefaultWordList)
    }

    /**
     * Copies the data in an input stream to a target file if the magic number matches.
     *
     * If the magic number does not match the expected value, this method throws an
     * IOException. Other usual conditions for IOException or FileNotFoundException
     * also apply.
     *
     * @param input the stream to be copied.
     * @param output an output stream to copy the data to.
     */
    @Throws(FileNotFoundException::class, IOException::class)
    fun checkMagicAndCopyFileTo(
        input: BufferedInputStream,
        output: BufferedOutputStream
    ) {
        // Check the magic number
        val length: Int = MAGIC_NUMBER_VERSION_2.size
        val magicNumberBuffer: ByteArray = ByteArray(length)
        val readMagicNumberSize: Int = input.read(magicNumberBuffer, 0, length)
        if (readMagicNumberSize < length) {
            throw IOException("Less bytes to read than the magic number length")
        }
        if (SHOULD_VERIFY_MAGIC_NUMBER) {
            if (!MAGIC_NUMBER_VERSION_2.contentEquals(magicNumberBuffer)) {
                if (!MAGIC_NUMBER_VERSION_1.contentEquals(magicNumberBuffer)) {
                    throw IOException("Wrong magic number for downloaded file")
                }
            }
        }
        output.write(magicNumberBuffer)

        // Actually copy the file
        val buffer: ByteArray = ByteArray(FILE_READ_BUFFER_SIZE)
        var readBytes: Int = input.read(buffer)
        while (readBytes >= 0) {
            output.write(buffer, 0, readBytes)
            readBytes = input.read(buffer)
        }
        input.close()
    }

    @Throws(RemoteException::class)
    private fun reinitializeClientRecordInDictionaryContentProvider(
        context: Context,
        client: ContentProviderClient,
        clientId: String
    ) {
        val metadataFileUri: String = MetadataFileUriGetter.getMetadataUri(context)
        Log.i(
            TAG, "reinitializeClientRecordInDictionaryContentProvider() : MetadataFileUri = "
                    + metadataFileUri
        )
        val metadataAdditionalId: String = MetadataFileUriGetter.getMetadataAdditionalId(context)
        // Tell the content provider to reset all information about this client id
        val metadataContentUri: Uri = getProviderUriBuilder(clientId)
            .appendPath(QUERY_PATH_METADATA)
            .appendQueryParameter(QUERY_PARAMETER_PROTOCOL, QUERY_PARAMETER_PROTOCOL_VALUE)
            .build()
        client.delete(metadataContentUri, null, null)
        // Update the metadata URI
        val metadataValues: ContentValues = ContentValues()
        metadataValues.put(INSERT_METADATA_CLIENT_ID_COLUMN, clientId)
        metadataValues.put(INSERT_METADATA_METADATA_URI_COLUMN, metadataFileUri)
        metadataValues.put(INSERT_METADATA_METADATA_ADDITIONAL_ID_COLUMN, metadataAdditionalId)
        client.insert(metadataContentUri, metadataValues)

        // Update the dictionary list.
        val dictionaryContentUriBase: Uri = getProviderUriBuilder(clientId)
            .appendPath(QUERY_PATH_DICT_INFO)
            .appendQueryParameter(QUERY_PARAMETER_PROTOCOL, QUERY_PARAMETER_PROTOCOL_VALUE)
            .build()
        val dictionaryList: ArrayList<DictionaryInfo> =
            DictionaryInfoUtils.getCurrentDictionaryFileNameAndVersionInfo(context)
        val length: Int = dictionaryList.size
        for (i in 0 until length) {
            val info: DictionaryInfo = dictionaryList.get(i)
            Log.i(TAG, "reinitializeClientRecordInDictionaryContentProvider() : Insert $info")
            client.insert(
                Uri.withAppendedPath(dictionaryContentUriBase, info.mId),
                info.toContentValues()
            )
        }

        // Read from metadata file in resources to get the baseline dictionary info.
        // This ensures we start with a valid list of available dictionaries.
        val metadataResourceId: Int = context.resources.getIdentifier(
            "metadata",
            "raw", DictionaryInfoUtils.RESOURCE_PACKAGE_NAME
        )
        if (metadataResourceId == 0) {
            Log.w(TAG, "Missing metadata.json resource")
            return
        }
        var inputStream: InputStream? = null
        try {
            inputStream = context.resources.openRawResource(metadataResourceId)
            UpdateHandler.handleMetadata(context, inputStream, clientId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read metadata.json from resources", e)
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    Log.w(TAG, "Failed to close metadata.json", e)
                }
            }
        }
    }

    /**
     * Initialize a client record with the dictionary content provider.
     *
     * This merely acquires the content provider and calls
     * #reinitializeClientRecordInDictionaryContentProvider.
     *
     * @param context the context for resources and providers.
     * @param clientId the client ID to use.
     */
    fun initializeClientRecordHelper(context: Context, clientId: String) {
        try {
            val client: ContentProviderClient =
                context.contentResolver.acquireContentProviderClient(getProviderUriBuilder("").build())
                    ?: return
            reinitializeClientRecordInDictionaryContentProvider(context, client, clientId)
        } catch (e: RemoteException) {
            Log.e(TAG, "Cannot contact the dictionary content provider", e)
        }
    }
}
