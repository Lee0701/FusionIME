/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.text.TextUtils
import android.util.Log
import android.util.SparseArray
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.latin.BinaryDictionary
import com.android.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import com.android.inputmethod.latin.common.ComposedData
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.FileUtils
import com.android.inputmethod.latin.common.InputPointers
import com.android.inputmethod.latin.common.StringUtils
import com.android.inputmethod.latin.makedict.DictionaryHeader
import com.android.inputmethod.latin.makedict.FormatSpec.DictionaryOptions
import com.android.inputmethod.latin.makedict.FormatSpec.FormatOptions
import com.android.inputmethod.latin.makedict.UnsupportedFormatException
import com.android.inputmethod.latin.makedict.WordProperty
import com.android.inputmethod.latin.settings.SettingsValuesForSuggestion
import com.android.inputmethod.latin.utils.BinaryDictionaryUtils
import com.android.inputmethod.latin.utils.JniUtils
import com.android.inputmethod.latin.utils.WordInputEventForPersonalization
import java.io.File
import java.util.Arrays
import java.util.Locale
import javax.annotation.Nonnull

/**
 * Implements a static, compacted, binary dictionary of standard words.
 */
// TODO: All methods which should be locked need to have a suffix "Locked".
class BinaryDictionary : Dictionary {
    private var mNativeDict: Long = 0
    private val mDictSize: Long
    private val mDictFilePath: String?
    private val mUseFullEditDistance: Boolean
    private val mIsUpdatable: Boolean
    private var mHasUpdated: Boolean

    private val mDicTraverseSessions: SparseArray<DicTraverseSession> = SparseArray()

    // TODO: There should be a way to remove used DicTraverseSession objects from
    // {@code mDicTraverseSessions}.
    private fun getTraverseSession(traverseSessionId: Int): DicTraverseSession {
        synchronized(mDicTraverseSessions) {
            var traverseSession: DicTraverseSession = mDicTraverseSessions.get(traverseSessionId)
            if (traverseSession == null) {
                traverseSession = DicTraverseSession(mLocale, mNativeDict, mDictSize)
                mDicTraverseSessions.put(traverseSessionId, traverseSession)
            }
            return traverseSession
        }
    }

    /**
     * Constructs binary dictionary using existing dictionary file.
     * @param filename the name of the file to read through native code.
     * @param offset the offset of the dictionary data within the file.
     * @param length the length of the binary data.
     * @param useFullEditDistance whether to use the full edit distance in suggestions
     * @param dictType the dictionary type, as a human-readable string
     * @param isUpdatable whether to open the dictionary file in writable mode.
     */
    constructor(
        filename: String?, offset: Long, length: Long,
        useFullEditDistance: Boolean, locale: Locale?, dictType: String,
        isUpdatable: Boolean
    ) : super(dictType, locale) {
        mDictSize = length
        mDictFilePath = filename
        mIsUpdatable = isUpdatable
        mHasUpdated = false
        mUseFullEditDistance = useFullEditDistance
        loadDictionary(filename, offset, length, isUpdatable)
    }

    /**
     * Constructs binary dictionary on memory.
     * @param filename the name of the file used to flush.
     * @param useFullEditDistance whether to use the full edit distance in suggestions
     * @param dictType the dictionary type, as a human-readable string
     * @param formatVersion the format version of the dictionary
     * @param attributeMap the attributes of the dictionary
     */
    constructor(
        filename: String?, useFullEditDistance: Boolean,
        locale: Locale, dictType: String, formatVersion: Long,
        attributeMap: Map<String?, String?>
    ) : super(dictType, locale) {
        mDictSize = 0
        mDictFilePath = filename
        // On memory dictionary is always updatable.
        mIsUpdatable = true
        mHasUpdated = false
        mUseFullEditDistance = useFullEditDistance
        val keyArray: Array<String?> = arrayOfNulls(attributeMap.size)
        val valueArray: Array<String?> = arrayOfNulls(attributeMap.size)
        var index: Int = 0
        for (key: String? in attributeMap.keys) {
            keyArray.get(index) = key
            valueArray.get(index) = attributeMap.get(key)
            index++
        }
        mNativeDict = createOnMemoryNative(formatVersion, locale.toString(), keyArray, valueArray)
    }


    // TODO: Move native dict into session
    private fun loadDictionary(
        path: String?, startOffset: Long,
        length: Long, isUpdatable: Boolean
    ) {
        mHasUpdated = false
        mNativeDict = openNative(path, startOffset, length, isUpdatable)
    }

    val isCorrupted: Boolean
        // TODO: Check isCorrupted() for main dictionaries.
        get() {
            if (!isValidDictionary) {
                return false
            }
            if (!isCorruptedNative(mNativeDict)) {
                return false
            }
            // TODO: Record the corruption.
            Log.e(
                TAG,
                "BinaryDictionary (" + mDictFilePath + ") is corrupted."
            )
            Log.e(TAG, "locale: " + mLocale)
            Log.e(TAG, "dict size: " + mDictSize)
            Log.e(TAG, "updatable: " + mIsUpdatable)
            return true
        }

    @get:Throws(UnsupportedFormatException::class)
    val header: DictionaryHeader?
        get() {
            if (mNativeDict == 0L) {
                return null
            }
            val outHeaderSize: IntArray = IntArray(1)
            val outFormatVersion: IntArray = IntArray(1)
            val outAttributeKeys: ArrayList<IntArray> =
                ArrayList()
            val outAttributeValues: ArrayList<IntArray> =
                ArrayList()
            getHeaderInfoNative(
                mNativeDict, outHeaderSize, outFormatVersion, outAttributeKeys,
                outAttributeValues
            )
            val attributes: HashMap<String?, String?> =
                HashMap()
            for (i in outAttributeKeys.indices) {
                val attributeKey: String =
                    StringUtils.getStringFromNullTerminatedCodePointArray(
                        outAttributeKeys.get(i)
                    )
                val attributeValue: String =
                    StringUtils.getStringFromNullTerminatedCodePointArray(
                        outAttributeValues.get(i)
                    )
                attributes.put(attributeKey, attributeValue)
            }
            val hasHistoricalInfo: Boolean =
                DictionaryHeader.Companion.ATTRIBUTE_VALUE_TRUE == attributes.get(DictionaryHeader.Companion.HAS_HISTORICAL_INFO_KEY)
            return DictionaryHeader(
                outHeaderSize.get(0), DictionaryOptions(attributes),
                FormatOptions(outFormatVersion.get(0), hasHistoricalInfo)
            )
        }

    override fun getSuggestions(
        composedData: ComposedData,
        ngramContext: NgramContext, proximityInfoHandle: Long,
        settingsValuesForSuggestion: SettingsValuesForSuggestion,
        sessionId: Int, weightForLocale: Float,
        inOutWeightOfLangModelVsSpatialModel: FloatArray?
    ): ArrayList<SuggestedWordInfo?>? {
        if (!isValidDictionary) {
            return null
        }
        val session: DicTraverseSession = getTraverseSession(sessionId)
        Arrays.fill(session.mInputCodePoints, Constants.NOT_A_CODE)
        ngramContext.outputToArray(
            session.mPrevWordCodePointArrays,
            session.mIsBeginningOfSentenceArray
        )
        val inputPointers: InputPointers = composedData.mInputPointers
        val isGesture: Boolean = composedData.mIsBatchMode
        val inputSize: Int
        if (!isGesture) {
            inputSize =
                composedData.copyCodePointsExceptTrailingSingleQuotesAndReturnCodePointCount(
                    session.mInputCodePoints
                )
            if (inputSize < 0) {
                return null
            }
        } else {
            inputSize = inputPointers.getPointerSize()
        }
        session.mNativeSuggestOptions.setUseFullEditDistance(mUseFullEditDistance)
        session.mNativeSuggestOptions.setIsGesture(isGesture)
        session.mNativeSuggestOptions.setBlockOffensiveWords(
            settingsValuesForSuggestion.mBlockPotentiallyOffensive
        )
        session.mNativeSuggestOptions.setWeightForLocale(weightForLocale)
        if (inOutWeightOfLangModelVsSpatialModel != null) {
            session.mInputOutputWeightOfLangModelVsSpatialModel.get(0) =
                inOutWeightOfLangModelVsSpatialModel.get(0)
        } else {
            session.mInputOutputWeightOfLangModelVsSpatialModel.get(0) =
                Dictionary.Companion.NOT_A_WEIGHT_OF_LANG_MODEL_VS_SPATIAL_MODEL
        }
        // TOOD: Pass multiple previous words information for n-gram.
        getSuggestionsNative(
            mNativeDict, proximityInfoHandle,
            getTraverseSession(sessionId).getSession(), inputPointers.getXCoordinates(),
            inputPointers.getYCoordinates(), inputPointers.getTimes(),
            inputPointers.getPointerIds(), session.mInputCodePoints, inputSize,
            session.mNativeSuggestOptions.getOptions(), session.mPrevWordCodePointArrays,
            session.mIsBeginningOfSentenceArray, ngramContext.getPrevWordCount(),
            session.mOutputSuggestionCount, session.mOutputCodePoints, session.mOutputScores,
            session.mSpaceIndices, session.mOutputTypes,
            session.mOutputAutoCommitFirstWordConfidence,
            session.mInputOutputWeightOfLangModelVsSpatialModel
        )
        if (inOutWeightOfLangModelVsSpatialModel != null) {
            inOutWeightOfLangModelVsSpatialModel.get(0) =
                session.mInputOutputWeightOfLangModelVsSpatialModel.get(0)
        }
        val count: Int = session.mOutputSuggestionCount.get(0)
        val suggestions: ArrayList<SuggestedWordInfo?> = ArrayList()
        for (j in 0 until count) {
            val start: Int = j * DICTIONARY_MAX_WORD_LENGTH
            var len: Int = 0
            while (len < DICTIONARY_MAX_WORD_LENGTH
                && session.mOutputCodePoints.get(start + len) != 0
            ) {
                ++len
            }
            if (len > 0) {
                suggestions.add(
                    SuggestedWordInfo(
                        String(session.mOutputCodePoints, start, len),
                        "",  /* prevWordsContext */
                        (session.mOutputScores.get(j) * weightForLocale).toInt(),
                        session.mOutputTypes.get(j),
                        this,  /* sourceDict */
                        session.mSpaceIndices.get(j),  /* indexOfTouchPointOfSecondWord */
                        session.mOutputAutoCommitFirstWordConfidence.get(0)
                    )
                )
            }
        }
        return suggestions
    }

    val isValidDictionary: Boolean
        get() {
            return mNativeDict != 0L
        }

    val formatVersion: Int
        get() {
            return getFormatVersionNative(mNativeDict)
        }

    override fun isInDictionary(word: String?): Boolean {
        return getFrequency(word) != Dictionary.Companion.NOT_A_PROBABILITY
    }

    override fun getFrequency(word: String?): Int {
        if (TextUtils.isEmpty(word)) {
            return Dictionary.Companion.NOT_A_PROBABILITY
        }
        val codePoints: IntArray = StringUtils.toCodePointArray(word)
        return getProbabilityNative(mNativeDict, codePoints)
    }

    override fun getMaxFrequencyOfExactMatches(word: String?): Int {
        if (TextUtils.isEmpty(word)) {
            return Dictionary.Companion.NOT_A_PROBABILITY
        }
        val codePoints: IntArray = StringUtils.toCodePointArray(word)
        return getMaxProbabilityOfExactMatchesNative(mNativeDict, codePoints)
    }

    @UsedForTesting
    fun isValidNgram(ngramContext: NgramContext, word: String?): Boolean {
        return getNgramProbability(ngramContext, word) != Dictionary.Companion.NOT_A_PROBABILITY
    }

    fun getNgramProbability(ngramContext: NgramContext, word: String?): Int {
        if (!ngramContext.isValid() || TextUtils.isEmpty(word)) {
            return Dictionary.Companion.NOT_A_PROBABILITY
        }
        val prevWordCodePointArrays: Array<IntArray?> =
            arrayOfNulls(ngramContext.getPrevWordCount())
        val isBeginningOfSentenceArray: BooleanArray = BooleanArray(ngramContext.getPrevWordCount())
        ngramContext.outputToArray(prevWordCodePointArrays, isBeginningOfSentenceArray)
        val wordCodePoints: IntArray = StringUtils.toCodePointArray(word)
        return getNgramProbabilityNative(
            mNativeDict, prevWordCodePointArrays,
            isBeginningOfSentenceArray, wordCodePoints
        )
    }

    fun getWordProperty(word: String?, isBeginningOfSentence: Boolean): WordProperty? {
        if (word == null) {
            return null
        }
        val codePoints: IntArray = StringUtils.toCodePointArray(word)
        val outCodePoints: IntArray = IntArray(DICTIONARY_MAX_WORD_LENGTH)
        val outFlags: BooleanArray = BooleanArray(FORMAT_WORD_PROPERTY_OUTPUT_FLAG_COUNT)
        val outProbabilityInfo: IntArray =
            IntArray(FORMAT_WORD_PROPERTY_OUTPUT_PROBABILITY_INFO_COUNT)
        val outNgramPrevWordsArray: ArrayList<Array<IntArray>> = ArrayList()
        val outNgramPrevWordIsBeginningOfSentenceArray: ArrayList<BooleanArray> =
            ArrayList()
        val outNgramTargets: ArrayList<IntArray> = ArrayList()
        val outNgramProbabilityInfo: ArrayList<IntArray> = ArrayList()
        val outShortcutTargets: ArrayList<IntArray> = ArrayList()
        val outShortcutProbabilities: ArrayList<Int> = ArrayList()
        getWordPropertyNative(
            mNativeDict, codePoints, isBeginningOfSentence, outCodePoints,
            outFlags, outProbabilityInfo, outNgramPrevWordsArray,
            outNgramPrevWordIsBeginningOfSentenceArray, outNgramTargets,
            outNgramProbabilityInfo, outShortcutTargets, outShortcutProbabilities
        )
        return WordProperty(
            codePoints,
            outFlags.get(FORMAT_WORD_PROPERTY_IS_NOT_A_WORD_INDEX),
            outFlags.get(FORMAT_WORD_PROPERTY_IS_POSSIBLY_OFFENSIVE_INDEX),
            outFlags.get(FORMAT_WORD_PROPERTY_HAS_NGRAMS_INDEX),
            outFlags.get(FORMAT_WORD_PROPERTY_IS_BEGINNING_OF_SENTENCE_INDEX), outProbabilityInfo,
            outNgramPrevWordsArray, outNgramPrevWordIsBeginningOfSentenceArray,
            outNgramTargets, outNgramProbabilityInfo
        )
    }

    class GetNextWordPropertyResult(wordProperty: WordProperty?, nextToken: Int) {
        var mWordProperty: WordProperty?
        var mNextToken: Int

        init {
            mWordProperty = wordProperty
            mNextToken = nextToken
        }
    }

    /**
     * Method to iterate all words in the dictionary for makedict.
     * If token is 0, this method newly starts iterating the dictionary.
     */
    fun getNextWordProperty(token: Int): GetNextWordPropertyResult {
        val codePoints: IntArray = IntArray(DICTIONARY_MAX_WORD_LENGTH)
        val isBeginningOfSentence: BooleanArray = BooleanArray(1)
        val nextToken: Int = getNextWordNative(
            mNativeDict, token, codePoints,
            isBeginningOfSentence
        )
        val word: String = StringUtils.getStringFromNullTerminatedCodePointArray(codePoints)
        return GetNextWordPropertyResult(
            getWordProperty(word, isBeginningOfSentence.get(0)), nextToken
        )
    }

    // Add a unigram entry to binary dictionary with unigram attributes in native code.
    fun addUnigramEntry(
        word: String?, probability: Int, isBeginningOfSentence: Boolean,
        isNotAWord: Boolean, isPossiblyOffensive: Boolean, timestamp: Int
    ): Boolean {
        if (word == null || (word.isEmpty() && !isBeginningOfSentence)) {
            return false
        }
        val codePoints: IntArray = StringUtils.toCodePointArray(word)
        if (!addUnigramEntryNative(
                mNativeDict, codePoints, probability,
                null,  /* shortcutTargetCodePoints */0,  /* shortcutProbability */
                isBeginningOfSentence, isNotAWord, isPossiblyOffensive, timestamp
            )
        ) {
            return false
        }
        mHasUpdated = true
        return true
    }

    // Remove a unigram entry from the binary dictionary in native code.
    fun removeUnigramEntry(word: String?): Boolean {
        if (TextUtils.isEmpty(word)) {
            return false
        }
        val codePoints: IntArray = StringUtils.toCodePointArray(word)
        if (!removeUnigramEntryNative(mNativeDict, codePoints)) {
            return false
        }
        mHasUpdated = true
        return true
    }

    // Add an n-gram entry to the binary dictionary with timestamp in native code.
    fun addNgramEntry(
        ngramContext: NgramContext, word: String?,
        probability: Int, timestamp: Int
    ): Boolean {
        if (!ngramContext.isValid() || TextUtils.isEmpty(word)) {
            return false
        }
        val prevWordCodePointArrays: Array<IntArray?> =
            arrayOfNulls(ngramContext.getPrevWordCount())
        val isBeginningOfSentenceArray: BooleanArray = BooleanArray(ngramContext.getPrevWordCount())
        ngramContext.outputToArray(prevWordCodePointArrays, isBeginningOfSentenceArray)
        val wordCodePoints: IntArray = StringUtils.toCodePointArray(word)
        if (!addNgramEntryNative(
                mNativeDict, prevWordCodePointArrays,
                isBeginningOfSentenceArray, wordCodePoints, probability, timestamp
            )
        ) {
            return false
        }
        mHasUpdated = true
        return true
    }

    // Update entries for the word occurrence with the ngramContext.
    fun updateEntriesForWordWithNgramContext(
        @Nonnull ngramContext: NgramContext,
        word: String?, isValidWord: Boolean, count: Int, timestamp: Int
    ): Boolean {
        if (TextUtils.isEmpty(word)) {
            return false
        }
        val prevWordCodePointArrays: Array<IntArray?> =
            arrayOfNulls(ngramContext.getPrevWordCount())
        val isBeginningOfSentenceArray: BooleanArray = BooleanArray(ngramContext.getPrevWordCount())
        ngramContext.outputToArray(prevWordCodePointArrays, isBeginningOfSentenceArray)
        val wordCodePoints: IntArray = StringUtils.toCodePointArray(word)
        if (!updateEntriesForWordWithNgramContextNative(
                mNativeDict, prevWordCodePointArrays,
                isBeginningOfSentenceArray, wordCodePoints, isValidWord, count, timestamp
            )
        ) {
            return false
        }
        mHasUpdated = true
        return true
    }

    @UsedForTesting
    fun updateEntriesForInputEvents(inputEvents: Array<WordInputEventForPersonalization>) {
        if (!isValidDictionary) {
            return
        }
        var processedEventCount: Int = 0
        while (processedEventCount < inputEvents.size) {
            if (needsToRunGC(true /* mindsBlockByGC */)) {
                flushWithGC()
            }
            processedEventCount = updateEntriesForInputEventsNative(
                mNativeDict, inputEvents,
                processedEventCount
            )
            mHasUpdated = true
            if (processedEventCount <= 0) {
                return
            }
        }
    }

    private fun reopen() {
        close()
        val dictFile: File = File(mDictFilePath)
        // WARNING: Because we pass 0 as the offset and file.length() as the length, this can
        // only be called for actual files. Right now it's only called by the flush() family of
        // functions, which require an updatable dictionary, so it's okay. But beware.
        loadDictionary(
            dictFile.getAbsolutePath(), 0,  /* startOffset */
            dictFile.length(), mIsUpdatable
        )
    }

    // Flush to dict file if the dictionary has been updated.
    fun flush(): Boolean {
        if (!isValidDictionary) {
            return false
        }
        if (mHasUpdated) {
            if (!flushNative(mNativeDict, mDictFilePath)) {
                return false
            }
            reopen()
        }
        return true
    }

    // Run GC and flush to dict file if the dictionary has been updated.
    fun flushWithGCIfHasUpdated(): Boolean {
        if (mHasUpdated) {
            return flushWithGC()
        }
        return true
    }

    // Run GC and flush to dict file.
    fun flushWithGC(): Boolean {
        if (!isValidDictionary) {
            return false
        }
        if (!flushWithGCNative(mNativeDict, mDictFilePath)) {
            return false
        }
        reopen()
        return true
    }

    /**
     * Checks whether GC is needed to run or not.
     * @param mindsBlockByGC Whether to mind operations blocked by GC. We don't need to care about
     * the blocking in some situations such as in idle time or just before closing.
     * @return whether GC is needed to run or not.
     */
    fun needsToRunGC(mindsBlockByGC: Boolean): Boolean {
        if (!isValidDictionary) {
            return false
        }
        return needsToRunGCNative(mNativeDict, mindsBlockByGC)
    }

    fun migrateTo(newFormatVersion: Int): Boolean {
        if (!isValidDictionary) {
            return false
        }
        val isMigratingDir: File =
            File(mDictFilePath + DIR_NAME_SUFFIX_FOR_RECORD_MIGRATION)
        if (isMigratingDir.exists()) {
            isMigratingDir.delete()
            Log.e(
                TAG, ("Previous migration attempt failed probably due to a crash. "
                        + "Giving up using the old dictionary (" + mDictFilePath + ").")
            )
            return false
        }
        if (!isMigratingDir.mkdir()) {
            Log.e(
                TAG, ("Cannot create a dir (" + isMigratingDir.getAbsolutePath()
                        + ") to record migration.")
            )
            return false
        }
        try {
            val tmpDictFilePath: String = mDictFilePath + DICT_FILE_NAME_SUFFIX_FOR_MIGRATION
            if (!migrateNative(mNativeDict, tmpDictFilePath, newFormatVersion.toLong())) {
                return false
            }
            close()
            val dictFile: File = File(mDictFilePath)
            val tmpDictFile: File = File(tmpDictFilePath)
            if (!FileUtils.deleteRecursively(dictFile)) {
                return false
            }
            if (!BinaryDictionaryUtils.renameDict(tmpDictFile, dictFile)) {
                return false
            }
            loadDictionary(
                dictFile.getAbsolutePath(), 0,  /* startOffset */
                dictFile.length(), mIsUpdatable
            )
            return true
        } finally {
            isMigratingDir.delete()
        }
    }

    @UsedForTesting
    fun getPropertyForGettingStats(query: String): String {
        if (!isValidDictionary) {
            return ""
        }
        return getPropertyNative(mNativeDict, query)
    }

    override fun shouldAutoCommit(candidate: SuggestedWordInfo): Boolean {
        return candidate.mAutoCommitFirstWordConfidence > CONFIDENCE_TO_AUTO_COMMIT
    }

    override fun close() {
        synchronized(mDicTraverseSessions) {
            val sessionsSize: Int = mDicTraverseSessions.size()
            for (index in 0 until sessionsSize) {
                val traverseSession: DicTraverseSession? = mDicTraverseSessions.valueAt(index)
                if (traverseSession != null) {
                    traverseSession.close()
                }
            }
            mDicTraverseSessions.clear()
        }
        closeInternalLocked()
    }

    @Synchronized
    private fun closeInternalLocked() {
        if (mNativeDict != 0L) {
            closeNative(mNativeDict)
            mNativeDict = 0
        }
    }

    // TODO: Manage BinaryDictionary instances without using WeakReference or something.
    @Throws(Throwable::class)
    protected fun finalize() {
        try {
            closeInternalLocked()
        } finally {
            super.finalize()
        }
    }

    companion object {
        private val TAG: String = BinaryDictionary::class.java.getSimpleName()

        // The cutoff returned by native for auto-commit confidence.
        // Must be equal to CONFIDENCE_TO_AUTO_COMMIT in native/jni/src/defines.h
        private const val CONFIDENCE_TO_AUTO_COMMIT: Int = 1000000

        const val DICTIONARY_MAX_WORD_LENGTH: Int = 48
        const val MAX_PREV_WORD_COUNT_FOR_N_GRAM: Int = 3

        @UsedForTesting
        const val UNIGRAM_COUNT_QUERY: String = "UNIGRAM_COUNT"

        @UsedForTesting
        const val BIGRAM_COUNT_QUERY: String = "BIGRAM_COUNT"

        @UsedForTesting
        const val MAX_UNIGRAM_COUNT_QUERY: String = "MAX_UNIGRAM_COUNT"

        @UsedForTesting
        const val MAX_BIGRAM_COUNT_QUERY: String = "MAX_BIGRAM_COUNT"

        val NOT_A_VALID_TIMESTAMP: Int = -1

        // Format to get unigram flags from native side via getWordPropertyNative().
        private const val FORMAT_WORD_PROPERTY_OUTPUT_FLAG_COUNT: Int = 5
        private const val FORMAT_WORD_PROPERTY_IS_NOT_A_WORD_INDEX: Int = 0
        private const val FORMAT_WORD_PROPERTY_IS_POSSIBLY_OFFENSIVE_INDEX: Int = 1
        private const val FORMAT_WORD_PROPERTY_HAS_NGRAMS_INDEX: Int = 2
        private const val FORMAT_WORD_PROPERTY_HAS_SHORTCUTS_INDEX: Int = 3 // DEPRECATED
        private const val FORMAT_WORD_PROPERTY_IS_BEGINNING_OF_SENTENCE_INDEX: Int = 4

        // Format to get probability and historical info from native side via getWordPropertyNative().
        const val FORMAT_WORD_PROPERTY_OUTPUT_PROBABILITY_INFO_COUNT: Int = 4
        const val FORMAT_WORD_PROPERTY_PROBABILITY_INDEX: Int = 0
        const val FORMAT_WORD_PROPERTY_TIMESTAMP_INDEX: Int = 1
        const val FORMAT_WORD_PROPERTY_LEVEL_INDEX: Int = 2
        const val FORMAT_WORD_PROPERTY_COUNT_INDEX: Int = 3

        const val DICT_FILE_NAME_SUFFIX_FOR_MIGRATION: String = ".migrate"
        const val DIR_NAME_SUFFIX_FOR_RECORD_MIGRATION: String = ".migrating"

        init {
            JniUtils.loadNativeLibrary()
        }

        private external fun openNative(
            sourceDir: String?, dictOffset: Long, dictSize: Long,
            isUpdatable: Boolean
        ): Long

        private external fun createOnMemoryNative(
            formatVersion: Long,
            locale: String,
            attributeKeyStringArray: Array<String?>,
            attributeValueStringArray: Array<String?>
        ): Long

        private external fun getHeaderInfoNative(
            dict: Long, outHeaderSize: IntArray,
            outFormatVersion: IntArray, outAttributeKeys: ArrayList<IntArray>,
            outAttributeValues: ArrayList<IntArray>
        )

        private external fun flushNative(dict: Long, filePath: String?): Boolean
        private external fun needsToRunGCNative(dict: Long, mindsBlockByGC: Boolean): Boolean
        private external fun flushWithGCNative(dict: Long, filePath: String?): Boolean
        private external fun closeNative(dict: Long)
        private external fun getFormatVersionNative(dict: Long): Int
        private external fun getProbabilityNative(dict: Long, word: IntArray): Int
        private external fun getMaxProbabilityOfExactMatchesNative(dict: Long, word: IntArray): Int
        private external fun getNgramProbabilityNative(
            dict: Long, prevWordCodePointArrays: Array<IntArray?>,
            isBeginningOfSentenceArray: BooleanArray, word: IntArray
        ): Int

        private external fun getWordPropertyNative(
            dict: Long, word: IntArray,
            isBeginningOfSentence: Boolean, outCodePoints: IntArray, outFlags: BooleanArray,
            outProbabilityInfo: IntArray, outNgramPrevWordsArray: ArrayList<Array<IntArray>>,
            outNgramPrevWordIsBeginningOfSentenceArray: ArrayList<BooleanArray>,
            outNgramTargets: ArrayList<IntArray>, outNgramProbabilityInfo: ArrayList<IntArray>,
            outShortcutTargets: ArrayList<IntArray>, outShortcutProbabilities: ArrayList<Int>
        )

        private external fun getNextWordNative(
            dict: Long, token: Int, outCodePoints: IntArray,
            outIsBeginningOfSentence: BooleanArray
        ): Int

        private external fun getSuggestionsNative(
            dict: Long,
            proximityInfo: Long,
            traverseSession: Long,
            xCoordinates: IntArray?,
            yCoordinates: IntArray?,
            times: IntArray?,
            pointerIds: IntArray?,
            inputCodePoints: IntArray,
            inputSize: Int,
            suggestOptions: IntArray?,
            prevWordCodePointArrays: Array<IntArray?>,
            isBeginningOfSentenceArray: BooleanArray,
            prevWordCount: Int,
            outputSuggestionCount: IntArray,
            outputCodePoints: IntArray,
            outputScores: IntArray,
            outputIndices: IntArray,
            outputTypes: IntArray,
            outputAutoCommitFirstWordConfidence: IntArray,
            inOutWeightOfLangModelVsSpatialModel: FloatArray
        )

        private external fun addUnigramEntryNative(
            dict: Long, word: IntArray, probability: Int,
            shortcutTarget: IntArray?, shortcutProbability: Int, isBeginningOfSentence: Boolean,
            isNotAWord: Boolean, isPossiblyOffensive: Boolean, timestamp: Int
        ): Boolean

        private external fun removeUnigramEntryNative(dict: Long, word: IntArray): Boolean
        private external fun addNgramEntryNative(
            dict: Long,
            prevWordCodePointArrays: Array<IntArray?>, isBeginningOfSentenceArray: BooleanArray,
            word: IntArray, probability: Int, timestamp: Int
        ): Boolean

        private external fun removeNgramEntryNative(
            dict: Long,
            prevWordCodePointArrays: Array<IntArray>,
            isBeginningOfSentenceArray: BooleanArray,
            word: IntArray
        ): Boolean

        private external fun updateEntriesForWordWithNgramContextNative(
            dict: Long,
            prevWordCodePointArrays: Array<IntArray?>, isBeginningOfSentenceArray: BooleanArray,
            word: IntArray, isValidWord: Boolean, count: Int, timestamp: Int
        ): Boolean

        private external fun updateEntriesForInputEventsNative(
            dict: Long,
            inputEvents: Array<WordInputEventForPersonalization>, startIndex: Int
        ): Int

        private external fun getPropertyNative(dict: Long, query: String): String
        private external fun isCorruptedNative(dict: Long): Boolean
        private external fun migrateNative(
            dict: Long, dictFilePath: String,
            newFormatVersion: Long
        ): Boolean
    }
}
