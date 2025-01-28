/*
 * Copyright (C) 2012 The Android Open Source Project
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
import android.util.Log
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.latin.BinaryDictionary.GetNextWordPropertyResult
import com.android.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import com.android.inputmethod.latin.common.ComposedData
import com.android.inputmethod.latin.common.FileUtils
import com.android.inputmethod.latin.define.DecoderSpecificConstants
import com.android.inputmethod.latin.makedict.DictionaryHeader
import com.android.inputmethod.latin.makedict.FormatSpec
import com.android.inputmethod.latin.makedict.UnsupportedFormatException
import com.android.inputmethod.latin.makedict.WordProperty
import com.android.inputmethod.latin.settings.SettingsValuesForSuggestion
import com.android.inputmethod.latin.utils.AsyncResultHolder
import com.android.inputmethod.latin.utils.CombinedFormatUtils
import com.android.inputmethod.latin.utils.ExecutorUtils
import com.android.inputmethod.latin.utils.WordInputEventForPersonalization
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Abstract base class for an expandable dictionary that can be created and updated dynamically
 * during runtime. When updated it automatically generates a new binary dictionary to handle future
 * queries in native code. This binary dictionary is written to internal storage.
 *
 * A class that extends this abstract class must have a static factory method named
 * getDictionary(Context context, Locale locale, File dictFile, String dictNamePrefix)
 */
abstract class ExpandableBinaryDictionary(
    context: Context, dictName: String,
    locale: Locale?, dictType: String, dictFile: File?
) :
    Dictionary(dictType, locale) {
    /** The application context.  */
    protected val mContext: Context

    /**
     * The binary dictionary generated dynamically from the fusion dictionary. This is used to
     * answer unigram and bigram queries.
     */
    var binaryDictionary: BinaryDictionary?
        private set

    /**
     * The name of this dictionary, used as a part of the filename for storing the binary
     * dictionary.
     */
    private val mDictName: String

    /** Dictionary file  */
    private val mDictFile: File

    /** Indicates whether a task for reloading the dictionary has been scheduled.  */
    private val mIsReloading: AtomicBoolean

    /** Indicates whether the current dictionary needs to be recreated.  */
    var isNeededToRecreate: Boolean
        private set

    private val mLock: ReentrantReadWriteLock

    private var mAdditionalAttributeMap: Map<String?, String?>? = null

    /**
     * Abstract method for loading initial contents of a given dictionary.
     */
    protected abstract fun loadInitialContentsLocked()

    val isValidDictionaryLocked: Boolean
        get() {
            return binaryDictionary!!.isValidDictionary
        }

    /**
     * Creates a new expandable binary dictionary.
     *
     * @param context The application context of the parent.
     * @param dictName The name of the dictionary. Multiple instances with the same
     * name is supported.
     * @param locale the dictionary locale.
     * @param dictType the dictionary type, as a human-readable string
     * @param dictFile dictionary file path. if null, use default dictionary path based on
     * dictionary type.
     */
    init {
        mDictName = dictName
        mContext = context
        mDictFile = getDictFile(context, dictName, dictFile)
        binaryDictionary = null
        mIsReloading = AtomicBoolean()
        isNeededToRecreate = false
        mLock = ReentrantReadWriteLock()
    }

    private fun asyncExecuteTaskWithWriteLock(task: Runnable) {
        asyncExecuteTaskWithLock(mLock.writeLock(), task)
    }

    fun closeBinaryDictionary() {
        if (binaryDictionary != null) {
            binaryDictionary!!.close()
            binaryDictionary = null
        }
    }

    /**
     * Closes and cleans up the binary dictionary.
     */
    override fun close() {
        asyncExecuteTaskWithWriteLock(object : Runnable {
            override fun run() {
                closeBinaryDictionary()
            }
        })
    }

    protected open val headerAttributeMap: MutableMap<String?, String?>
        get() {
            val attributeMap: HashMap<String?, String?> =
                HashMap()
            if (mAdditionalAttributeMap != null) {
                attributeMap.putAll(mAdditionalAttributeMap!!)
            }
            attributeMap.put(DictionaryHeader.DICTIONARY_ID_KEY, mDictName)
            attributeMap.put(DictionaryHeader.DICTIONARY_LOCALE_KEY, mLocale.toString())
            attributeMap.put(
                DictionaryHeader.DICTIONARY_VERSION_KEY,
                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
                    .toString()
            )
            return attributeMap
        }

    private fun removeBinaryDictionary() {
        asyncExecuteTaskWithWriteLock(object : Runnable {
            override fun run() {
                removeBinaryDictionaryLocked()
            }
        })
    }

    fun removeBinaryDictionaryLocked() {
        closeBinaryDictionary()
        if (mDictFile.exists() && !FileUtils.deleteRecursively(mDictFile)) {
            Log.e(TAG, "Can't remove a file: " + mDictFile.getName())
        }
    }

    private fun openBinaryDictionaryLocked() {
        binaryDictionary = BinaryDictionary(
            mDictFile.getAbsolutePath(), 0,  /* offset */mDictFile.length(),
            true,  /* useFullEditDistance */mLocale, mDictType, true /* isUpdatable */
        )
    }

    fun createOnMemoryBinaryDictionaryLocked() {
        binaryDictionary = BinaryDictionary(
            mDictFile.getAbsolutePath(), true,  /* useFullEditDistance */mLocale!!, mDictType,
            DICTIONARY_FORMAT_VERSION.toLong(),
            headerAttributeMap
        )
    }

    fun clear() {
        asyncExecuteTaskWithWriteLock(object : Runnable {
            override fun run() {
                removeBinaryDictionaryLocked()
                createOnMemoryBinaryDictionaryLocked()
            }
        })
    }

    /**
     * Check whether GC is needed and run GC if required.
     */
    fun runGCIfRequired(mindsBlockByGC: Boolean) {
        asyncExecuteTaskWithWriteLock(object : Runnable {
            override fun run() {
                if (this@ExpandableBinaryDictionary.binaryDictionary == null) {
                    return
                }
                runGCIfRequiredLocked(mindsBlockByGC)
            }
        })
    }

    protected fun runGCIfRequiredLocked(mindsBlockByGC: Boolean) {
        if (binaryDictionary!!.needsToRunGC(mindsBlockByGC)) {
            binaryDictionary!!.flushWithGC()
        }
    }

    private fun updateDictionaryWithWriteLock(updateTask: Runnable) {
        reloadDictionaryIfRequired()
        val task: Runnable = object : Runnable {
            override fun run() {
                if (binaryDictionary == null) {
                    return
                }
                runGCIfRequiredLocked(true /* mindsBlockByGC */)
                updateTask.run()
            }
        }
        asyncExecuteTaskWithWriteLock(task)
    }

    /**
     * Adds unigram information of a word to the dictionary. May overwrite an existing entry.
     */
    fun addUnigramEntry(
        word: String, frequency: Int,
        isNotAWord: Boolean, isPossiblyOffensive: Boolean, timestamp: Int
    ) {
        updateDictionaryWithWriteLock(object : Runnable {
            override fun run() {
                addUnigramLocked(word, frequency, isNotAWord, isPossiblyOffensive, timestamp)
            }
        })
    }

    protected fun addUnigramLocked(
        word: String, frequency: Int,
        isNotAWord: Boolean, isPossiblyOffensive: Boolean, timestamp: Int
    ) {
        if (!binaryDictionary!!.addUnigramEntry(
                word, frequency,
                false,  /* isBeginningOfSentence */isNotAWord, isPossiblyOffensive, timestamp
            )
        ) {
            Log.e(TAG, "Cannot add unigram entry. word: " + word)
        }
    }

    /**
     * Dynamically remove the unigram entry from the dictionary.
     */
    fun removeUnigramEntryDynamically(word: String) {
        reloadDictionaryIfRequired()
        asyncExecuteTaskWithWriteLock(object : Runnable {
            override fun run() {
                val binaryDictionary: BinaryDictionary = binaryDictionary ?: return
                runGCIfRequiredLocked(true /* mindsBlockByGC */)
                if (!binaryDictionary.removeUnigramEntry(word)) {
                    if (DEBUG) {
                        Log.i(TAG, "Cannot remove unigram entry: " + word)
                    }
                }
            }
        })
    }

    /**
     * Adds n-gram information of a word to the dictionary. May overwrite an existing entry.
     */
    fun addNgramEntry(
        ngramContext: NgramContext, word: String,
        frequency: Int, timestamp: Int
    ) {
        reloadDictionaryIfRequired()
        asyncExecuteTaskWithWriteLock(object : Runnable {
            override fun run() {
                if (binaryDictionary == null) {
                    return
                }
                runGCIfRequiredLocked(true /* mindsBlockByGC */)
                addNgramEntryLocked(ngramContext, word, frequency, timestamp)
            }
        })
    }

    protected fun addNgramEntryLocked(
        ngramContext: NgramContext, word: String,
        frequency: Int, timestamp: Int
    ) {
        if (!binaryDictionary!!.addNgramEntry(ngramContext, word, frequency, timestamp)) {
            if (DEBUG) {
                Log.i(TAG, "Cannot add n-gram entry.")
                Log.i(TAG, "  NgramContext: " + ngramContext + ", word: " + word)
            }
        }
    }

    /**
     * Update dictionary for the word with the ngramContext.
     */
    fun updateEntriesForWord(
        ngramContext: NgramContext,
        word: String, isValidWord: Boolean, count: Int, timestamp: Int
    ) {
        updateDictionaryWithWriteLock(object : Runnable {
            override fun run() {
                val binaryDictionary: BinaryDictionary = binaryDictionary ?: return
                if (!binaryDictionary.updateEntriesForWordWithNgramContext(
                        ngramContext, word,
                        isValidWord, count, timestamp
                    )
                ) {
                    if (DEBUG) {
                        Log.e(
                            TAG, ("Cannot update counter. word: " + word
                                    + " context: " + ngramContext.toString())
                        )
                    }
                }
            }
        })
    }

    /**
     * Used by Sketch.
     * {@see https://cs.corp.google.com/#android/vendor/unbundled_google/packages/LatinIMEGoogle/tools/sketch/ime-simulator/src/com/android/inputmethod/sketch/imesimulator/ImeSimulator.java&q=updateEntriesForInputEventsCallback&l=286}
     */
    @UsedForTesting
    interface UpdateEntriesForInputEventsCallback {
        fun onFinished()
    }

    /**
     * Dynamically update entries according to input events.
     *
     * Used by Sketch.
     * {@see https://cs.corp.google.com/#android/vendor/unbundled_google/packages/LatinIMEGoogle/tools/sketch/ime-simulator/src/com/android/inputmethod/sketch/imesimulator/ImeSimulator.java&q=updateEntriesForInputEventsCallback&l=286}
     */
    @UsedForTesting
    fun updateEntriesForInputEvents(
        inputEvents: ArrayList<WordInputEventForPersonalization>,
        callback: UpdateEntriesForInputEventsCallback?
    ) {
        reloadDictionaryIfRequired()
        asyncExecuteTaskWithWriteLock(object : Runnable {
            override fun run() {
                try {
                    val binaryDictionary: BinaryDictionary = binaryDictionary ?: return
                    binaryDictionary.updateEntriesForInputEvents(
                        inputEvents.toTypedArray<WordInputEventForPersonalization>()
                    )
                } finally {
                    if (callback != null) {
                        callback.onFinished()
                    }
                }
            }
        })
    }

    override fun getSuggestions(
        composedData: ComposedData,
        ngramContext: NgramContext, proximityInfoHandle: Long,
        settingsValuesForSuggestion: SettingsValuesForSuggestion, sessionId: Int,
        weightForLocale: Float, inOutWeightOfLangModelVsSpatialModel: FloatArray?
    ): ArrayList<SuggestedWordInfo>? {
        reloadDictionaryIfRequired()
        var lockAcquired: Boolean = false
        try {
            lockAcquired = mLock.readLock().tryLock(
                TIMEOUT_FOR_READ_OPS_IN_MILLISECONDS.toLong(), TimeUnit.MILLISECONDS
            )
            if (lockAcquired) {
                if (binaryDictionary == null) {
                    return null
                }
                val suggestions: ArrayList<SuggestedWordInfo>? =
                    binaryDictionary!!.getSuggestions(
                        composedData, ngramContext,
                        proximityInfoHandle, settingsValuesForSuggestion, sessionId,
                        weightForLocale, inOutWeightOfLangModelVsSpatialModel
                    )
                if (binaryDictionary!!.isCorrupted) {
                    Log.i(
                        TAG, ("Dictionary (" + mDictName + ") is corrupted. "
                                + "Remove and regenerate it.")
                    )
                    removeBinaryDictionary()
                }
                return suggestions
            }
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted tryLock() in getSuggestionsWithSessionId().", e)
        } finally {
            if (lockAcquired) {
                mLock.readLock().unlock()
            }
        }
        return null
    }

    override fun isInDictionary(word: String): Boolean {
        reloadDictionaryIfRequired()
        var lockAcquired: Boolean = false
        try {
            lockAcquired = mLock.readLock().tryLock(
                TIMEOUT_FOR_READ_OPS_IN_MILLISECONDS.toLong(), TimeUnit.MILLISECONDS
            )
            if (lockAcquired) {
                if (binaryDictionary == null) {
                    return false
                }
                return isInDictionaryLocked(word)
            }
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted tryLock() in isInDictionary().", e)
        } finally {
            if (lockAcquired) {
                mLock.readLock().unlock()
            }
        }
        return false
    }

    protected fun isInDictionaryLocked(word: String): Boolean {
        if (binaryDictionary == null) return false
        return binaryDictionary!!.isInDictionary(word)
    }

    override fun getMaxFrequencyOfExactMatches(word: String): Int {
        reloadDictionaryIfRequired()
        var lockAcquired: Boolean = false
        try {
            lockAcquired = mLock.readLock().tryLock(
                TIMEOUT_FOR_READ_OPS_IN_MILLISECONDS.toLong(), TimeUnit.MILLISECONDS
            )
            if (lockAcquired) {
                if (binaryDictionary == null) {
                    return Dictionary.NOT_A_PROBABILITY
                }
                return binaryDictionary!!.getMaxFrequencyOfExactMatches(word)
            }
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted tryLock() in getMaxFrequencyOfExactMatches().", e)
        } finally {
            if (lockAcquired) {
                mLock.readLock().unlock()
            }
        }
        return Dictionary.NOT_A_PROBABILITY
    }


    /**
     * Loads the current binary dictionary from internal storage. Assumes the dictionary file
     * exists.
     */
    fun loadBinaryDictionaryLocked() {
        if (DBG_STRESS_TEST) {
            // Test if this class does not cause problems when it takes long time to load binary
            // dictionary.
            try {
                Log.w(TAG, "Start stress in loading: $mDictName")
                Thread.sleep(15000)
                Log.w(TAG, "End stress in loading")
            } catch (e: InterruptedException) {
                Log.w(TAG, "Interrupted while loading: $mDictName", e)
            }
        }
        openBinaryDictionaryLocked()
        binaryDictionary?.close()
        if (binaryDictionary!!.isValidDictionary
            && needsToMigrateDictionary(binaryDictionary!!.formatVersion)
        ) {
            if (!binaryDictionary!!.migrateTo(DICTIONARY_FORMAT_VERSION)) {
                Log.e(TAG, "Dictionary migration failed: " + mDictName)
                removeBinaryDictionaryLocked()
            }
        }
    }

    /**
     * Create a new binary dictionary and load initial contents.
     */
    fun createNewDictionaryLocked() {
        removeBinaryDictionaryLocked()
        createOnMemoryBinaryDictionaryLocked()
        loadInitialContentsLocked()
        // Run GC and flush to file when initial contents have been loaded.
        binaryDictionary!!.flushWithGCIfHasUpdated()
    }

    /**
     * Marks that the dictionary needs to be recreated.
     *
     */
    protected fun setNeedsToRecreate() {
        isNeededToRecreate = true
    }

    fun clearNeedsToRecreate() {
        isNeededToRecreate = false
    }

    /**
     * Load the current binary dictionary from internal storage. If the dictionary file doesn't
     * exists or needs to be regenerated, the new dictionary file will be asynchronously generated.
     * However, the dictionary itself is accessible even before the new dictionary file is actually
     * generated. It may return a null result for getSuggestions() in that case by design.
     */
    fun reloadDictionaryIfRequired() {
        if (!isReloadRequired) return
        asyncReloadDictionary()
    }

    private val isReloadRequired: Boolean
        /**
         * Returns whether a dictionary reload is required.
         */
        get() {
            return binaryDictionary == null || isNeededToRecreate
        }

    /**
     * Reloads the dictionary. Access is controlled on a per dictionary file basis.
     */
    private fun asyncReloadDictionary() {
        val isReloading: AtomicBoolean = mIsReloading
        if (!isReloading.compareAndSet(false, true)) {
            return
        }
        val dictFile: File = mDictFile
        asyncExecuteTaskWithWriteLock {
            try {
                if (!dictFile.exists() || isNeededToRecreate) {
                    // If the dictionary file does not exist or contents have been updated,
                    // generate a new one.
                    createNewDictionaryLocked()
                } else if (binaryDictionary == null) {
                    // Otherwise, load the existing dictionary.
                    loadBinaryDictionaryLocked()
                    val binaryDictionary: BinaryDictionary? = binaryDictionary
                    if (binaryDictionary != null && !(isValidDictionaryLocked // TODO: remove the check below
                                && matchesExpectedBinaryDictFormatVersionForThisType(
                            binaryDictionary.formatVersion
                        ))
                    ) {
                        // Binary dictionary or its format version is not valid. Regenerate
                        // the dictionary file. createNewDictionaryLocked will remove the
                        // existing files if appropriate.
                        createNewDictionaryLocked()
                    }
                }
                clearNeedsToRecreate()
            } finally {
                isReloading.set(false)
            }
        }
    }

    /**
     * Flush binary dictionary to dictionary file.
     */
    fun asyncFlushBinaryDictionary() {
        asyncExecuteTaskWithWriteLock(object : Runnable {
            override fun run() {
                val binaryDictionary: BinaryDictionary = binaryDictionary ?: return
                if (binaryDictionary.needsToRunGC(false /* mindsBlockByGC */)) {
                    binaryDictionary.flushWithGC()
                } else {
                    binaryDictionary.flush()
                }
            }
        })
    }

    val dictionaryStats: DictionaryStats?
        get() {
            reloadDictionaryIfRequired()
            val dictName: String = mDictName
            val dictFile: File = mDictFile
            val result: AsyncResultHolder<DictionaryStats> =
                AsyncResultHolder("DictionaryStats")
            asyncExecuteTaskWithLock(
                mLock.readLock(),
                object : Runnable {
                    override fun run() {
                        result.set(DictionaryStats(mLocale!!, dictName, dictName, dictFile, 0))
                    }
                })
            return result.get(
                null,  /* defaultValue */
                TIMEOUT_FOR_READ_OPS_IN_MILLISECONDS.toLong()
            )
        }

    @UsedForTesting
    fun waitAllTasksForTests() {
        val countDownLatch: CountDownLatch = CountDownLatch(1)
        asyncExecuteTaskWithWriteLock(object : Runnable {
            override fun run() {
                countDownLatch.countDown()
            }
        })
        try {
            countDownLatch.await()
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted while waiting for finishing dictionary operations.", e)
        }
    }

    @UsedForTesting
    fun clearAndFlushDictionaryWithAdditionalAttributes(
        attributeMap: Map<String?, String?>?
    ) {
        mAdditionalAttributeMap = attributeMap
        clear()
    }

    fun dumpAllWordsForDebug() {
        reloadDictionaryIfRequired()
        val tag: String = TAG
        val dictName: String = mDictName
        asyncExecuteTaskWithLock(mLock.readLock(), object : Runnable {
            override fun run() {
                Log.d(tag, "Dump dictionary: $dictName for $mLocale")
                val binaryDictionary: BinaryDictionary = binaryDictionary
                    ?: return
                try {
                    val header: DictionaryHeader? = binaryDictionary.header
                    Log.d(tag, "Format version: " + binaryDictionary.formatVersion)
                    Log.d(
                        tag, CombinedFormatUtils.formatAttributeMap(
                            header!!.mDictionaryOptions.mAttributes
                        )
                    )
                } catch (e: UnsupportedFormatException) {
                    Log.d(tag, "Cannot fetch header information.", e)
                }
                var token: Int = 0
                do {
                    val result: GetNextWordPropertyResult =
                        binaryDictionary.getNextWordProperty(token)
                    val wordProperty: WordProperty? = result.mWordProperty
                    if (wordProperty == null) {
                        Log.d(tag, " dictionary is empty.")
                        break
                    }
                    Log.d(tag, wordProperty.toString())
                    token = result.mNextToken
                } while (token != 0)
            }
        })
    }

    val wordPropertiesForSyncing: Array<WordProperty?>
        /**
         * Returns dictionary content required for syncing.
         */
        get() {
            reloadDictionaryIfRequired()
            val result: AsyncResultHolder<Array<WordProperty?>> =
                AsyncResultHolder("WordPropertiesForSync")
            asyncExecuteTaskWithLock(
                mLock.readLock(),
                object : Runnable {
                    override fun run() {
                        val wordPropertyList: ArrayList<WordProperty?> =
                            ArrayList()
                        val binaryDictionary: BinaryDictionary = binaryDictionary
                            ?: return
                        var token = 0
                        do {
                            // TODO: We need a new API that returns *new* un-synced data.
                            val nextWordPropertyResult: GetNextWordPropertyResult =
                                binaryDictionary.getNextWordProperty(token)
                            val wordProperty: WordProperty? = nextWordPropertyResult.mWordProperty
                            if (wordProperty == null) {
                                break
                            }
                            wordPropertyList.add(wordProperty)
                            token = nextWordPropertyResult.mNextToken
                        } while (token != 0)
                        result.set(wordPropertyList.toTypedArray<WordProperty?>())
                    }
                })
            // TODO: Figure out the best timeout duration for this API.
            return result.get(
                DEFAULT_WORD_PROPERTIES_FOR_SYNC,
                TIMEOUT_FOR_READ_OPS_IN_MILLISECONDS.toLong()
            )!!
        }

    companion object {
        private const val DEBUG: Boolean = false

        /** Used for Log actions from this class  */
        private val TAG: String = ExpandableBinaryDictionary::class.java.getSimpleName()

        /** Whether to print debug output to log  */
        private const val DBG_STRESS_TEST: Boolean = false

        private const val TIMEOUT_FOR_READ_OPS_IN_MILLISECONDS: Int = 100

        /**
         * The maximum length of a word in this dictionary.
         */
        @JvmStatic
        protected val MAX_WORD_LENGTH: Int = DecoderSpecificConstants.DICTIONARY_MAX_WORD_LENGTH

        private const val DICTIONARY_FORMAT_VERSION: Int = FormatSpec.VERSION4

        private val DEFAULT_WORD_PROPERTIES_FOR_SYNC: Array<WordProperty?> =
            arrayOfNulls(0) /* default */

        /* A extension for a binary dictionary file. */
        protected const val DICT_FILE_EXTENSION: String = ".dict"

        fun matchesExpectedBinaryDictFormatVersionForThisType(formatVersion: Int): Boolean {
            return formatVersion == FormatSpec.VERSION4
        }

        private fun needsToMigrateDictionary(formatVersion: Int): Boolean {
            // When we bump up the dictionary format version, the old version should be added to here
            // for supporting migration. Note that native code has to support reading such formats.
            return formatVersion == FormatSpec.VERSION402
        }

        fun getDictFile(
            context: Context, dictName: String,
            dictFile: File?
        ): File {
            return if ((dictFile != null))
                dictFile
            else
                File(context.getFilesDir(), dictName + DICT_FILE_EXTENSION)
        }

        fun getDictName(
            name: String, locale: Locale,
            dictFile: File?
        ): String {
            return if (dictFile != null) dictFile.getName() else name + "." + locale.toString()
        }

        private fun asyncExecuteTaskWithLock(lock: Lock, task: Runnable) {
            ExecutorUtils.getBackgroundExecutor(ExecutorUtils.KEYBOARD)!!
                .execute(object : Runnable {
                    override fun run() {
                        lock.lock()
                        try {
                            task.run()
                        } finally {
                            lock.unlock()
                        }
                    }
                })
        }
    }
}
