/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.inputmethod.latin.utils

import android.content.ContentValues
import android.content.Context
import android.content.res.Resources
import android.text.TextUtils
import android.util.Log
import android.view.inputmethod.InputMethodSubtype
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.dictionarypack.UpdateHandler
import com.android.inputmethod.latin.AssetFileAddress
import com.android.inputmethod.latin.BinaryDictionaryGetter
import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.RichInputMethodManager
import com.android.inputmethod.latin.common.FileUtils
import com.android.inputmethod.latin.common.LocaleUtils
import com.android.inputmethod.latin.define.DecoderSpecificConstants
import com.android.inputmethod.latin.makedict.DictionaryHeader
import com.android.inputmethod.latin.makedict.UnsupportedFormatException
import com.android.inputmethod.latin.settings.SpacingAndPunctuations
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * This class encapsulates the logic for the Latin-IME side of dictionary information management.
 */
object DictionaryInfoUtils {
    private val TAG: String = DictionaryInfoUtils::class.java.simpleName
    val RESOURCE_PACKAGE_NAME: String = R::class.java.getPackage().name
    private const val DEFAULT_MAIN_DICT = "main"
    private const val MAIN_DICT_PREFIX = "main_"
    private const val DECODER_DICT_SUFFIX = DecoderSpecificConstants.DECODER_DICT_SUFFIX

    // 6 digits - unicode is limited to 21 bits
    private const val MAX_HEX_DIGITS_FOR_CODEPOINT = 6

    private const val TEMP_DICT_FILE_SUB = UpdateHandler.TEMP_DICT_FILE_SUB

    /**
     * Returns whether we may want to use this character as part of a file name.
     *
     * This basically only accepts ascii letters and numbers, and rejects everything else.
     */
    private fun isFileNameCharacter(codePoint: Int): Boolean {
        if (codePoint >= 0x30 && codePoint <= 0x39) return true // Digit

        if (codePoint >= 0x41 && codePoint <= 0x5A) return true // Uppercase

        if (codePoint >= 0x61 && codePoint <= 0x7A) return true // Lowercase

        return codePoint == '_'.code // Underscore
    }

    /**
     * Escapes a string for any characters that may be suspicious for a file or directory name.
     *
     * Concretely this does a sort of URL-encoding except it will encode everything that's not
     * alphanumeric or underscore. (true URL-encoding leaves alone characters like '*', which
     * we cannot allow here)
     */
    // TODO: create a unit test for this method
    fun replaceFileNameDangerousCharacters(name: String): String {
        // This assumes '%' is fully available as a non-separator, normal
        // character in a file name. This is probably true for all file systems.
        val sb = StringBuilder()
        val nameLength = name.length
        var i = 0
        while (i < nameLength) {
            val codePoint = name.codePointAt(i)
            if (isFileNameCharacter(codePoint)) {
                sb.appendCodePoint(codePoint)
            } else {
                sb.append(
                    String.format(
                        null as Locale?, "%%%1$0" + MAX_HEX_DIGITS_FOR_CODEPOINT + "x",
                        codePoint
                    )
                )
            }
            i = name.offsetByCodePoints(i, 1)
        }
        return sb.toString()
    }

    /**
     * Helper method to get the top level cache directory.
     */
    private fun getWordListCacheDirectory(context: Context): String {
        return context.filesDir.toString() + File.separator + "dicts"
    }

    /**
     * Helper method to get the top level cache directory.
     */
    fun getWordListStagingDirectory(context: Context): String {
        return context.filesDir.toString() + File.separator + "staging"
    }

    /**
     * Helper method to get the top level temp directory.
     */
    fun getWordListTempDirectory(context: Context): String {
        return context.filesDir.toString() + File.separator + "tmp"
    }

    /**
     * Reverse escaping done by [.replaceFileNameDangerousCharacters].
     */
    fun getWordListIdFromFileName(fname: String): String {
        val sb = StringBuilder()
        val fnameLength = fname.length
        var i = 0
        while (i < fnameLength) {
            val codePoint = fname.codePointAt(i)
            if ('%'.code != codePoint) {
                sb.appendCodePoint(codePoint)
            } else {
                // + 1 to pass the % sign
                val encodedCodePoint =
                    fname.substring(i + 1, i + 1 + MAX_HEX_DIGITS_FOR_CODEPOINT).toInt(16)
                i += MAX_HEX_DIGITS_FOR_CODEPOINT
                sb.appendCodePoint(encodedCodePoint)
            }
            i = fname.offsetByCodePoints(i, 1)
        }
        return sb.toString()
    }

    /**
     * Helper method to the list of cache directories, one for each distinct locale.
     */
    fun getCachedDirectoryList(context: Context): Array<File>? {
        return File(getWordListCacheDirectory(context)).listFiles()
    }

    fun getStagingDirectoryList(context: Context): Array<File>? {
        return File(getWordListStagingDirectory(context)).listFiles()
    }

    fun getUnusedDictionaryList(context: Context): Array<File>? {
        return context.filesDir.listFiles { dir, filename ->
            (!TextUtils.isEmpty(filename) && filename.endsWith(".dict")
                    && filename.contains(TEMP_DICT_FILE_SUB))
        }
    }

    /**
     * Returns the category for a given file name.
     *
     * This parses the file name, extracts the category, and returns it. See
     * [.getMainDictId] and [.isMainWordListId].
     * @return The category as a string or null if it can't be found in the file name.
     */
    fun getCategoryFromFileName(fileName: String): String? {
        val id = getWordListIdFromFileName(fileName)
        val idArray = id.split(BinaryDictionaryGetter.ID_CATEGORY_SEPARATOR.toRegex())
            .dropLastWhile { it.isEmpty() }.toTypedArray()
        // An id is supposed to be in format category:locale, so splitting on the separator
        // should yield a 2-elements array
        if (2 != idArray.size) {
            return null
        }
        return idArray[0]
    }

    /**
     * Find out the cache directory associated with a specific locale.
     */
    fun getCacheDirectoryForLocale(locale: String, context: Context): String {
        val relativeDirectoryName = replaceFileNameDangerousCharacters(locale)
        val absoluteDirectoryName = (getWordListCacheDirectory(context) + File.separator
                + relativeDirectoryName)
        val directory = File(absoluteDirectoryName)
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                Log.e(
                    TAG,
                    "Could not create the directory for locale$locale"
                )
            }
        }
        return absoluteDirectoryName
    }

    /**
     * Generates a file name for the id and locale passed as an argument.
     *
     * In the current implementation the file name returned will always be unique for
     * any id/locale pair, but please do not expect that the id can be the same for
     * different dictionaries with different locales. An id should be unique for any
     * dictionary.
     * The file name is pretty much an URL-encoded version of the id inside a directory
     * named like the locale, except it will also escape characters that look dangerous
     * to some file systems.
     * @param id the id of the dictionary for which to get a file name
     * @param locale the locale for which to get the file name as a string
     * @param context the context to use for getting the directory
     * @return the name of the file to be created
     */
    fun getCacheFileName(id: String, locale: String, context: Context): String {
        val fileName = replaceFileNameDangerousCharacters(id)
        return getCacheDirectoryForLocale(locale, context) + File.separator + fileName
    }

    fun getStagingFileName(id: String?, locale: String?, context: Context): String {
        val stagingDirectory = getWordListStagingDirectory(context)
        // create the directory if it does not exist.
        val directory = File(stagingDirectory)
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                Log.e(TAG, "Could not create the staging directory.")
            }
        }
        // e.g. id="main:en_in", locale ="en_IN"
        val fileName = replaceFileNameDangerousCharacters(
            locale + TEMP_DICT_FILE_SUB + id
        )
        return stagingDirectory + File.separator + fileName
    }

    fun moveStagingFilesIfExists(context: Context) {
        val stagingFiles = getStagingDirectoryList(context)
        if (stagingFiles != null && stagingFiles.size > 0) {
            for (stagingFile in stagingFiles) {
                val fileName = stagingFile.name
                val index = fileName.indexOf(TEMP_DICT_FILE_SUB)
                if (index == -1) {
                    // This should never happen.
                    Log.e(TAG, "Staging file does not have ___ substring.")
                    continue
                }
                val localeAndFileId =
                    fileName.split(TEMP_DICT_FILE_SUB.toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                if (localeAndFileId.size != 2) {
                    Log.e(
                        TAG, String.format(
                            "malformed staging file %s. Deleting.",
                            stagingFile.absoluteFile
                        )
                    )
                    stagingFile.delete()
                    continue
                }

                val locale = localeAndFileId[0]
                // already escaped while moving to staging.
                val fileId = localeAndFileId[1]
                val cacheDirectoryForLocale = getCacheDirectoryForLocale(locale, context)
                val cacheFilename = cacheDirectoryForLocale + File.separator + fileId
                val cacheFile = File(cacheFilename)
                // move the staging file to cache file.
                if (!FileUtils.renameTo(stagingFile, cacheFile)) {
                    Log.e(
                        TAG, String.format(
                            "Failed to rename from %s to %s.",
                            stagingFile.absoluteFile, cacheFile.absoluteFile
                        )
                    )
                }
            }
        }
    }

    fun isMainWordListId(id: String): Boolean {
        val idArray = id.split(BinaryDictionaryGetter.ID_CATEGORY_SEPARATOR.toRegex())
            .dropLastWhile { it.isEmpty() }.toTypedArray()
        // An id is supposed to be in format category:locale, so splitting on the separator
        // should yield a 2-elements array
        if (2 != idArray.size) {
            return false
        }
        return BinaryDictionaryGetter.MAIN_DICTIONARY_CATEGORY == idArray[0]
    }

    /**
     * Find out whether a dictionary is available for this locale.
     * @param context the context on which to check resources.
     * @param locale the locale to check for.
     * @return whether a (non-placeholder) dictionary is available or not.
     */
    fun isDictionaryAvailable(context: Context, locale: Locale): Boolean {
        val res = context.resources
        return 0 != getMainDictionaryResourceIdIfAvailableForLocale(res, locale)
    }

    /**
     * Helper method to return a dictionary res id for a locale, or 0 if none.
     * @param res resources for the app
     * @param locale dictionary locale
     * @return main dictionary resource id
     */
    fun getMainDictionaryResourceIdIfAvailableForLocale(
        res: Resources,
        locale: Locale
    ): Int {
        var resId: Int
        // Try to find main_language_country dictionary.
        if (!locale.country.isEmpty()) {
            val dictLanguageCountry = (MAIN_DICT_PREFIX
                    + locale.toString().lowercase() + DECODER_DICT_SUFFIX)
            if ((res.getIdentifier(
                    dictLanguageCountry, "raw", RESOURCE_PACKAGE_NAME
                ).also { resId = it }) != 0
            ) {
                return resId
            }
        }

        // Try to find main_language dictionary.
        val dictLanguage = MAIN_DICT_PREFIX + locale.language + DECODER_DICT_SUFFIX
        if ((res.getIdentifier(dictLanguage, "raw", RESOURCE_PACKAGE_NAME)
                .also { resId = it }) != 0
        ) {
            return resId
        }

        // Not found, return 0
        return 0
    }

    /**
     * Returns a main dictionary resource id
     * @param res resources for the app
     * @param locale dictionary locale
     * @return main dictionary resource id
     */
    fun getMainDictionaryResourceId(res: Resources, locale: Locale): Int {
        val resourceId = getMainDictionaryResourceIdIfAvailableForLocale(res, locale)
        if (0 != resourceId) {
            return resourceId
        }
        return res.getIdentifier(
            DEFAULT_MAIN_DICT + DecoderSpecificConstants.DECODER_DICT_SUFFIX,
            "raw", RESOURCE_PACKAGE_NAME
        )
    }

    /**
     * Returns the id associated with the main word list for a specified locale.
     *
     * Word lists stored in Android Keyboard's resources are referred to as the "main"
     * word lists. Since they can be updated like any other list, we need to assign a
     * unique ID to them. This ID is just the name of the language (locale-wise) they
     * are for, and this method returns this ID.
     */
    fun getMainDictId(locale: Locale): String {
        // This works because we don't include by default different dictionaries for
        // different countries. This actually needs to return the id that we would
        // like to use for word lists included in resources, and the following is okay.
        return BinaryDictionaryGetter.MAIN_DICTIONARY_CATEGORY +
                BinaryDictionaryGetter.ID_CATEGORY_SEPARATOR + locale.toString()
            .lowercase(Locale.getDefault())
    }

    fun getDictionaryFileHeaderOrNull(
        file: File,
        offset: Long, length: Long
    ): DictionaryHeader? {
        try {
            val header =
                BinaryDictionaryUtils.getHeaderWithOffsetAndLength(file, offset, length)
            return header
        } catch (e: UnsupportedFormatException) {
            return null
        } catch (e: IOException) {
            return null
        }
    }

    /**
     * Returns information of the dictionary.
     *
     * @param fileAddress the asset dictionary file address.
     * @param locale Locale for this file.
     * @return information of the specified dictionary.
     */
    private fun createDictionaryInfoFromFileAddress(
        fileAddress: AssetFileAddress?, locale: Locale
    ): DictionaryInfo {
        val id = getMainDictId(locale)
        val version = DictionaryHeaderUtils.getContentVersion(fileAddress!!)
        val description = SubtypeLocaleUtils.getSubtypeLocaleDisplayName(locale.toString())!!
        // Do not store the filename on db as it will try to move the filename from db to the
        // cached directory. If the filename is already in cached directory, this is not
        // necessary.
        val filenameToStoreOnDb: String? = null
        return DictionaryInfo(
            id, locale, description, filenameToStoreOnDb,
            fileAddress.mLength, File(fileAddress.mFilename).lastModified(), version
        )
    }

    /**
     * Returns the information of the dictionary for the given [AssetFileAddress].
     * If the file is corrupted or a pre-fava file, then the file gets deleted and the null
     * value is returned.
     */
    private fun createDictionaryInfoForUnCachedFile(
        fileAddress: AssetFileAddress?, locale: Locale
    ): DictionaryInfo? {
        val id = getMainDictId(locale)
        val version = DictionaryHeaderUtils.getContentVersion(fileAddress!!)

        if (version == -1) {
            // Purge the pre-fava/corrupted unused dictionaires.
            fileAddress.deleteUnderlyingFile()
            return null
        }

        val description = SubtypeLocaleUtils.getSubtypeLocaleDisplayName(locale.toString())!!

        val unCachedFile = File(fileAddress.mFilename)
        // Store just the filename and not the full path.
        val filenameToStoreOnDb = unCachedFile.name
        return DictionaryInfo(
            id, locale, description, filenameToStoreOnDb, fileAddress.mLength,
            unCachedFile.lastModified(), version
        )
    }

    /**
     * Returns dictionary information for the given locale.
     */
    private fun createDictionaryInfoFromLocale(locale: Locale): DictionaryInfo {
        val id = getMainDictId(locale)
        val version = -1
        val description = SubtypeLocaleUtils.getSubtypeLocaleDisplayName(locale.toString())!!
        return DictionaryInfo(id, locale, description, null, 0L, 0L, version)
    }

    private fun addOrUpdateDictInfo(
        dictList: ArrayList<DictionaryInfo>,
        newElement: DictionaryInfo
    ) {
        val iter = dictList.iterator()
        while (iter.hasNext()) {
            val thisDictInfo = iter.next()
            if (thisDictInfo.mLocale == newElement.mLocale) {
                if (newElement.mVersion <= thisDictInfo.mVersion) {
                    return
                }
                iter.remove()
            }
        }
        dictList.add(newElement)
    }

    fun getCurrentDictionaryFileNameAndVersionInfo(
        context: Context
    ): ArrayList<DictionaryInfo> {
        val dictList = ArrayList<DictionaryInfo>()

        // Retrieve downloaded dictionaries from cached directories
        val directoryList = getCachedDirectoryList(context)
        if (null != directoryList) {
            for (directory in directoryList) {
                val localeString = getWordListIdFromFileName(directory.name)
                val dicts = BinaryDictionaryGetter.getCachedWordLists(
                    localeString, context
                )
                for (dict in dicts) {
                    val wordListId = getWordListIdFromFileName(dict.name)
                    if (!isMainWordListId(wordListId)) {
                        continue
                    }
                    val locale = LocaleUtils.constructLocaleFromString(localeString)
                    val fileAddress: AssetFileAddress? =
                        AssetFileAddress.makeFromFile(dict)
                    val dictionaryInfo =
                        createDictionaryInfoFromFileAddress(fileAddress, locale)
                    // Protect against cases of a less-specific dictionary being found, like an
                    // en dictionary being used for an en_US locale. In this case, the en dictionary
                    // should be used for en_US but discounted for listing purposes.
                    if (dictionaryInfo.mLocale != locale) {
                        continue
                    }
                    addOrUpdateDictInfo(dictList, dictionaryInfo)
                }
            }
        }

        // Retrieve downloaded dictionaries from the unused dictionaries.
        val unusedDictionaryList = getUnusedDictionaryList(context)
        if (unusedDictionaryList != null) {
            for (dictionaryFile in unusedDictionaryList) {
                val fileName = dictionaryFile.name
                val index = fileName.indexOf(TEMP_DICT_FILE_SUB)
                if (index == -1) {
                    continue
                }
                val locale = fileName.substring(0, index)
                val dictionaryInfo = createDictionaryInfoForUnCachedFile(
                    AssetFileAddress.makeFromFile(dictionaryFile),
                    LocaleUtils.constructLocaleFromString(locale)
                )
                if (dictionaryInfo != null) {
                    addOrUpdateDictInfo(dictList, dictionaryInfo)
                }
            }
        }

        // Retrieve files from assets
        val resources = context.resources
        val assets = resources.assets
        for (localeString in assets.locales) {
            val locale = LocaleUtils.constructLocaleFromString(localeString)
            val resourceId =
                getMainDictionaryResourceIdIfAvailableForLocale(
                    context.resources, locale
                )
            if (0 == resourceId) {
                continue
            }
            val fileAddress =
                BinaryDictionaryGetter.loadFallbackResource(context, resourceId)
            val dictionaryInfo = createDictionaryInfoFromFileAddress(
                fileAddress,
                locale
            )
            // Protect against cases of a less-specific dictionary being found, like an
            // en dictionary being used for an en_US locale. In this case, the en dictionary
            // should be used for en_US but discounted for listing purposes.
            // TODO: Remove dictionaryInfo == null when the static LMs have the headers.
            if (dictionaryInfo.mLocale != locale) {
                continue
            }
            addOrUpdateDictInfo(dictList, dictionaryInfo)
        }

        // Generate the dictionary information from  the enabled subtypes. This will not
        // overwrite the real records.
        RichInputMethodManager.init(context)
        val enabledSubtypes: List<InputMethodSubtype> =
            RichInputMethodManager.instance.getMyEnabledInputMethodSubtypeList(true)
        for (subtype in enabledSubtypes) {
            val locale = LocaleUtils.constructLocaleFromString(subtype.locale)
            val dictionaryInfo = createDictionaryInfoFromLocale(locale)
            addOrUpdateDictInfo(dictList, dictionaryInfo)
        }

        return dictList
    }

    @UsedForTesting
    fun looksValidForDictionaryInsertion(
        text: CharSequence,
        spacingAndPunctuations: SpacingAndPunctuations
    ): Boolean {
        if (TextUtils.isEmpty(text)) {
            return false
        }
        val length = text.length
        if (length > DecoderSpecificConstants.DICTIONARY_MAX_WORD_LENGTH) {
            return false
        }
        var i = 0
        var digitCount = 0
        while (i < length) {
            val codePoint = Character.codePointAt(text, i)
            val charCount = Character.charCount(codePoint)
            i += charCount
            if (Character.isDigit(codePoint)) {
                // Count digits: see below
                digitCount += charCount
                continue
            }
            if (!spacingAndPunctuations.isWordCodePoint(codePoint)) {
                return false
            }
        }
        // We reject strings entirely comprised of digits to avoid using PIN codes or credit
        // card numbers. It would come in handy for word prediction though; a good example is
        // when writing one's address where the street number is usually quite discriminative,
        // as well as the postal code.
        return digitCount < length
    }

    class DictionaryInfo(
        id: String, locale: Locale,
        description: String?, filename: String?,
        filesize: Long, modifiedTimeMillis: Long, version: Int
    ) {
        val mId: String = id

        val mLocale: Locale = locale
        val mDescription: String? = description
        val mFilename: String? = filename
        val mFilesize: Long = filesize
        val mModifiedTimeMillis: Long = modifiedTimeMillis
        val mVersion: Int = version

        fun toContentValues(): ContentValues {
            val values = ContentValues()
            values.put(WORDLISTID_COLUMN, mId)
            values.put(LOCALE_COLUMN, mLocale.toString())
            values.put(DESCRIPTION_COLUMN, mDescription)
            values.put(LOCAL_FILENAME_COLUMN, mFilename ?: "")
            values.put(DATE_COLUMN, TimeUnit.MILLISECONDS.toSeconds(mModifiedTimeMillis))
            values.put(FILESIZE_COLUMN, mFilesize)
            values.put(VERSION_COLUMN, mVersion)
            return values
        }

        override fun toString(): String {
            return ("DictionaryInfo : Id = '" + mId
                    + "' : Locale=" + mLocale
                    + " : Version=" + mVersion)
        }

        companion object {
            private const val LOCALE_COLUMN = "locale"
            private const val WORDLISTID_COLUMN = "id"
            private const val LOCAL_FILENAME_COLUMN = "filename"
            private const val DESCRIPTION_COLUMN = "description"
            private const val DATE_COLUMN = "date"
            private const val FILESIZE_COLUMN = "filesize"
            private const val VERSION_COLUMN = "version"
        }
    }
}
