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

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetFileDescriptor
import android.util.Log
import com.android.inputmethod.latin.common.LocaleUtils
import com.android.inputmethod.latin.define.DecoderSpecificConstants
import com.android.inputmethod.latin.makedict.DictionaryHeader
import com.android.inputmethod.latin.makedict.UnsupportedFormatException
import com.android.inputmethod.latin.utils.BinaryDictionaryUtils
import com.android.inputmethod.latin.utils.DictionaryInfoUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.BufferUnderflowException
import java.util.Locale

/**
 * Helper class to get the address of a mmap'able dictionary file.
 */
object BinaryDictionaryGetter {
    /**
     * Used for Log actions from this class
     */
    private val TAG: String = BinaryDictionaryGetter::class.java.getSimpleName()

    /**
     * Used to return empty lists
     */
    private val EMPTY_FILE_ARRAY: Array<File?> = arrayOfNulls(0)

    /**
     * Name of the common preferences name to know which word list are on and which are off.
     */
    private const val COMMON_PREFERENCES_NAME: String = "LatinImeDictPrefs"

    private val SHOULD_USE_DICT_VERSION: Boolean = DecoderSpecificConstants.SHOULD_USE_DICT_VERSION

    // Name of the category for the main dictionary
    const val MAIN_DICTIONARY_CATEGORY: String = "main"
    const val ID_CATEGORY_SEPARATOR: String = ":"

    // The key considered to read the version attribute in a dictionary file.
    private val VERSION_KEY: String = "version"

    /**
     * Generates a unique temporary file name in the app cache directory.
     */
    @Throws(IOException::class)
    fun getTempFileName(id: String, context: Context): String {
        val safeId: String = DictionaryInfoUtils.replaceFileNameDangerousCharacters(id)
        val directory: File = File(DictionaryInfoUtils.getWordListTempDirectory(context))
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                Log.e(TAG, "Could not create the temporary directory")
            }
        }
        // If the first argument is less than three chars, createTempFile throws a
        // RuntimeException. We don't really care about what name we get, so just
        // put a three-chars prefix makes us safe.
        return File.createTempFile("xxx" + safeId, null, directory).getAbsolutePath()
    }

    /**
     * Returns a file address from a resource, or null if it cannot be opened.
     */
    fun loadFallbackResource(
        context: Context,
        fallbackResId: Int
    ): AssetFileAddress? {
        var afd: AssetFileDescriptor? = null
        try {
            afd = context.getResources().openRawResourceFd(fallbackResId)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Resource not found: " + fallbackResId)
            return null
        }
        if (afd == null) {
            Log.e(TAG, "Resource cannot be opened: " + fallbackResId)
            return null
        }
        try {
            return AssetFileAddress.Companion.makeFromFileNameAndOffset(
                context.getApplicationInfo().sourceDir, afd.getStartOffset(), afd.getLength()
            )
        } finally {
            try {
                afd.close()
            } catch (ignored: IOException) {
            }
        }
    }

    /**
     * Returns the list of cached files for a specific locale, one for each category.
     *
     * This will return exactly one file for each word list category that matches
     * the passed locale. If several files match the locale for any given category,
     * this returns the file with the closest match to the locale. For example, if
     * the passed word list is en_US, and for a category we have an en and an en_US
     * word list available, we'll return only the en_US one.
     * Thus, the list will contain as many files as there are categories.
     *
     * @param locale the locale to find the dictionary files for, as a string.
     * @param context the context on which to open the files upon.
     * @return an array of binary dictionary files, which may be empty but may not be null.
     */
    fun getCachedWordLists(locale: String?, context: Context): Array<File?> {
        val directoryList: Array<File?>? = DictionaryInfoUtils.getCachedDirectoryList(context)
        if (null == directoryList) return EMPTY_FILE_ARRAY
        val cacheFiles: HashMap<String?, FileAndMatchLevel> = HashMap()
        for (directory: File in directoryList) {
            if (!directory.isDirectory()) continue
            val dirLocale: String =
                DictionaryInfoUtils.getWordListIdFromFileName(directory.getName())
            val matchLevel: Int = LocaleUtils.getMatchLevel(dirLocale, locale)
            if (LocaleUtils.isMatch(matchLevel)) {
                val wordLists: Array<File>? = directory.listFiles()
                if (null != wordLists) {
                    for (wordList: File in wordLists) {
                        val category: String? =
                            DictionaryInfoUtils.getCategoryFromFileName(wordList.getName())
                        val currentBestMatch: FileAndMatchLevel? = cacheFiles.get(category)
                        if (null == currentBestMatch || currentBestMatch.mMatchLevel < matchLevel) {
                            cacheFiles.put(category, FileAndMatchLevel(wordList, matchLevel))
                        }
                    }
                }
            }
        }
        if (cacheFiles.isEmpty()) return EMPTY_FILE_ARRAY
        val result: Array<File?> = arrayOfNulls(cacheFiles.size)
        var index: Int = 0
        for (entry: FileAndMatchLevel in cacheFiles.values) {
            result.get(index++) = entry.mFile
        }
        return result
    }

    // ## HACK ## we prevent usage of a dictionary before version 18. The reason for this is, since
    // those do not include allowlist entries, the new code with an old version of the dictionary
    // would lose allowlist functionality.
    private fun hackCanUseDictionaryFile(file: File): Boolean {
        if (!SHOULD_USE_DICT_VERSION) {
            return true
        }

        try {
            // Read the version of the file
            val header: DictionaryHeader = BinaryDictionaryUtils.getHeader(file)
            val version: String? = header.mDictionaryOptions.mAttributes.get(VERSION_KEY)
            if (null == version) {
                // No version in the options : the format is unexpected
                return false
            }
            // Version 18 is the first one to include the allowlist. 
            // Obviously this is a big ## HACK ##
            return version.toInt() >= 18
        } catch (e: FileNotFoundException) {
            return false
        } catch (e: IOException) {
            return false
        } catch (e: NumberFormatException) {
            return false
        } catch (e: BufferUnderflowException) {
            return false
        } catch (e: UnsupportedFormatException) {
            return false
        }
    }

    /**
     * Returns a list of file addresses for a given locale, trying relevant methods in order.
     *
     * Tries to get binary dictionaries from various sources, in order:
     * - Uses a content provider to get a public dictionary set, as per the protocol described
     * in BinaryDictionaryFileDumper.
     * If that fails:
     * - Gets a file name from the built-in dictionary for this locale, if any.
     * If that fails:
     * - Returns null.
     * @return The list of addresses of valid dictionary files, or null.
     */
    fun getDictionaryFiles(
        locale: Locale,
        context: Context, notifyDictionaryPackForUpdates: Boolean
    ): ArrayList<AssetFileAddress> {
        if (notifyDictionaryPackForUpdates) {
            val hasDefaultWordList: Boolean = DictionaryInfoUtils.isDictionaryAvailable(
                context, locale
            )
            // It makes sure that the first time keyboard comes up and the dictionaries are reset,
            // the DB is populated with the appropriate values for each locale. Helps in downloading
            // the dictionaries when the user enables and switches new languages before the
            // DictionaryService runs.
            BinaryDictionaryFileDumper.downloadDictIfNeverRequested(
                locale, context, hasDefaultWordList
            )

            // Move a staging files to the cache ddirectories if any.
            DictionaryInfoUtils.moveStagingFilesIfExists(context)
        }
        val cachedWordLists: Array<File?> = getCachedWordLists(locale.toString(), context)
        val mainDictId: String = DictionaryInfoUtils.getMainDictId(locale)
        val dictPackSettings: DictPackSettings = DictPackSettings(context)

        var foundMainDict: Boolean = false
        val fileList: ArrayList<AssetFileAddress> = ArrayList()
        // cachedWordLists may not be null, see doc for getCachedDictionaryList
        for (f: File in cachedWordLists) {
            val wordListId: String = DictionaryInfoUtils.getWordListIdFromFileName(f.getName())
            val canUse: Boolean = f.canRead() && hackCanUseDictionaryFile(f)
            if (canUse && DictionaryInfoUtils.isMainWordListId(wordListId)) {
                foundMainDict = true
            }
            if (!dictPackSettings.isWordListActive(wordListId)) continue
            if (canUse) {
                val afa: AssetFileAddress? =
                    AssetFileAddress.Companion.makeFromFileName(f.getPath())
                if (null != afa) fileList.add(afa)
            } else {
                Log.e(
                    TAG, ("Found a cached dictionary file for " + locale.toString()
                            + " but cannot read or use it")
                )
            }
        }

        if (!foundMainDict && dictPackSettings.isWordListActive(mainDictId)) {
            val fallbackResId: Int =
                DictionaryInfoUtils.getMainDictionaryResourceId(context.getResources(), locale)
            val fallbackAsset: AssetFileAddress? = loadFallbackResource(context, fallbackResId)
            if (null != fallbackAsset) {
                fileList.add(fallbackAsset)
            }
        }

        return fileList
    }

    private class DictPackSettings(context: Context?) {
        val mDictPreferences: SharedPreferences?

        init {
            mDictPreferences = if (null == context)
                null
            else
                context.getSharedPreferences(
                    COMMON_PREFERENCES_NAME,
                    Context.MODE_MULTI_PROCESS
                )
        }

        fun isWordListActive(dictId: String?): Boolean {
            if (null == mDictPreferences) {
                // If we don't have preferences it basically means we can't find the dictionary
                // pack - either it's not installed, or it's disabled, or there is some strange
                // bug. Either way, a word list with no settings should be on by default: default
                // dictionaries in LatinIME are on if there is no settings at all, and if for some
                // reason some dictionaries have been installed BUT the dictionary pack can't be
                // found anymore it's safer to actually supply installed dictionaries.
                return true
            }
            // The default is true here for the same reasons as above. We got the dictionary
            // pack but if we don't have any settings for it it means the user has never been
            // to the settings yet. So by default, the main dictionaries should be on.
            return mDictPreferences.getBoolean(dictId, true)
        }
    }

    /**
     * Utility class for the [.getCachedWordLists] method
     */
    private class FileAndMatchLevel(file: File, matchLevel: Int) {
        val mFile: File
        val mMatchLevel: Int

        init {
            mFile = file
            mMatchLevel = matchLevel
        }
    }
}
