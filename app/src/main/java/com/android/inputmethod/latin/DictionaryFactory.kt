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
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.content.res.Resources
import android.util.Log
import com.android.inputmethod.latin.utils.DictionaryInfoUtils
import java.io.File
import java.io.IOException
import java.util.LinkedList
import java.util.Locale

/**
 * Factory for dictionary instances.
 */
object DictionaryFactory {
    private val TAG: String = DictionaryFactory::class.java.getSimpleName()

    /**
     * Initializes a main dictionary collection from a dictionary pack, with explicit flags.
     *
     * This searches for a content provider providing a dictionary pack for the specified
     * locale. If none is found, it falls back to the built-in dictionary - if any.
     * @param context application context for reading resources
     * @param locale the locale for which to create the dictionary
     * @return an initialized instance of DictionaryCollection
     */
    fun createMainDictionaryFromManager(
        context: Context,
        locale: Locale?
    ): DictionaryCollection {
        if (null == locale) {
            Log.e(TAG, "No locale defined for dictionary")
            return DictionaryCollection(
                Dictionary.Companion.TYPE_MAIN, locale,
                createReadOnlyBinaryDictionary(context, locale!!)!!
            )
        }

        val dictList: LinkedList<Dictionary> = LinkedList()
        val assetFileList: ArrayList<AssetFileAddress?> =
            BinaryDictionaryGetter.getDictionaryFiles(locale, context, true)
        if (null != assetFileList) {
            for (f: AssetFileAddress in assetFileList) {
                val readOnlyBinaryDictionary: ReadOnlyBinaryDictionary =
                    ReadOnlyBinaryDictionary(
                        f.mFilename, f.mOffset, f.mLength,
                        false,  /* useFullEditDistance */locale, Dictionary.Companion.TYPE_MAIN
                    )
                if (readOnlyBinaryDictionary.isValidDictionary()) {
                    dictList.add(readOnlyBinaryDictionary)
                } else {
                    readOnlyBinaryDictionary.close()
                    // Prevent this dictionary to do any further harm.
                    killDictionary(context, f)
                }
            }
        }

        // If the list is empty, that means we should not use any dictionary (for example, the user
        // explicitly disabled the main dictionary), so the following is okay. dictList is never
        // null, but if for some reason it is, DictionaryCollection handles it gracefully.
        return DictionaryCollection(Dictionary.Companion.TYPE_MAIN, locale, dictList)
    }

    /**
     * Kills a dictionary so that it is never used again, if possible.
     * @param context The context to contact the dictionary provider, if possible.
     * @param f A file address to the dictionary to kill.
     */
    fun killDictionary(context: Context, f: AssetFileAddress) {
        if (f.pointsToPhysicalFile()) {
            f.deleteUnderlyingFile()
            // Warn the dictionary provider if the dictionary came from there.
            val providerClient: ContentProviderClient?
            try {
                providerClient = context.getContentResolver().acquireContentProviderClient(
                    BinaryDictionaryFileDumper.getProviderUriBuilder("").build()
                )
            } catch (e: SecurityException) {
                Log.e(TAG, "No permission to communicate with the dictionary provider", e)
                return
            }
            if (null == providerClient) {
                Log.e(TAG, "Can't establish communication with the dictionary provider")
                return
            }
            val wordlistId: String =
                DictionaryInfoUtils.getWordListIdFromFileName(File(f.mFilename).getName())
            // TODO: this is a reasonable last resort, but it is suboptimal.
            // The following will remove the entry for this dictionary with the dictionary
            // provider. When the metadata is downloaded again, we will try downloading it
            // again.
            // However, in the practice that will mean the user will find themselves without
            // the new dictionary. That's fine for languages where it's included in the APK,
            // but for other languages it will leave the user without a dictionary at all until
            // the next update, which may be a few days away.
            // Ideally, we would trigger a new download right away, and use increasing retry
            // delays for this particular id/version combination.
            // Then again, this is expected to only ever happen in case of human mistake. If
            // the wrong file is on the server, the following is still doing the right thing.
            // If it's a file left over from the last version however, it's not great.
            BinaryDictionaryFileDumper.reportBrokenFileToDictionaryProvider(
                providerClient,
                context.getString(R.string.dictionary_pack_client_id),
                wordlistId
            )
        }
    }

    /**
     * Initializes a read-only binary dictionary from a raw resource file
     * @param context application context for reading resources
     * @param locale the locale to use for the resource
     * @return an initialized instance of ReadOnlyBinaryDictionary
     */
    private fun createReadOnlyBinaryDictionary(
        context: Context,
        locale: Locale
    ): ReadOnlyBinaryDictionary? {
        var afd: AssetFileDescriptor? = null
        try {
            val resId: Int = DictionaryInfoUtils.getMainDictionaryResourceIdIfAvailableForLocale(
                context.getResources(), locale
            )
            if (0 == resId) return null
            afd = context.getResources().openRawResourceFd(resId)
            if (afd == null) {
                Log.e(TAG, "Found the resource but it is compressed. resId=" + resId)
                return null
            }
            val sourceDir: String = context.getApplicationInfo().sourceDir
            val packagePath: File = File(sourceDir)
            // TODO: Come up with a way to handle a directory.
            if (!packagePath.isFile()) {
                Log.e(TAG, "sourceDir is not a file: " + sourceDir)
                return null
            }
            return ReadOnlyBinaryDictionary(
                sourceDir, afd.getStartOffset(), afd.getLength(),
                false,  /* useFullEditDistance */locale, Dictionary.Companion.TYPE_MAIN
            )
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Could not find the resource")
            return null
        } finally {
            if (null != afd) {
                try {
                    afd.close()
                } catch (e: IOException) {
                    /* IOException on close ? What am I supposed to do ? */
                }
            }
        }
    }
}
