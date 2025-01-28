/*
7 * Copyright (C) 2013 The Android Open Source Project
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

import android.Manifest.permission
import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.util.LruCache
import com.android.inputmethod.annotations.UsedForTesting
import com.android.inputmethod.keyboard.Keyboard
import com.android.inputmethod.latin.DictionaryFacilitator.DictionaryInitializationListener
import com.android.inputmethod.latin.NgramContext.WordInfo
import com.android.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import com.android.inputmethod.latin.common.ComposedData
import com.android.inputmethod.latin.common.Constants
import com.android.inputmethod.latin.common.StringUtils
import com.android.inputmethod.latin.permissions.PermissionsUtil
import com.android.inputmethod.latin.personalization.UserHistoryDictionary
import com.android.inputmethod.latin.settings.SettingsValuesForSuggestion
import com.android.inputmethod.latin.utils.ExecutorUtils
import com.android.inputmethod.latin.utils.SuggestionResults
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.annotation.Nonnull
import kotlin.concurrent.Volatile

/**
 * Facilitates interaction with different kinds of dictionaries. Provides APIs
 * to instantiate and select the correct dictionaries (based on language or account),
 * update entries and fetch suggestions.
 *
 * Currently AndroidSpellCheckerService and LatinIME both use DictionaryFacilitator as
 * a client for interacting with dictionaries.
 */
class DictionaryFacilitatorImpl : DictionaryFacilitator {
    private var mDictionaryGroup: DictionaryGroup = DictionaryGroup()

    @Volatile
    private var mLatchForWaitingLoadingMainDictionaries: CountDownLatch = CountDownLatch(0)

    // To synchronize assigning mDictionaryGroup to ensure closing dictionaries.
    private val mLock: Any = Any()

    private var mValidSpellingWordReadCache: LruCache<String?, Boolean>? = null
    private var mValidSpellingWordWriteCache: LruCache<String, Boolean>? = null

    override fun setValidSpellingWordReadCache(cache: LruCache<String?, Boolean>?) {
        mValidSpellingWordReadCache = cache
    }

    override fun setValidSpellingWordWriteCache(cache: LruCache<String, Boolean>?) {
        mValidSpellingWordWriteCache = cache
    }

    override fun isForLocale(locale: Locale?): Boolean {
        return locale != null && locale == mDictionaryGroup.mLocale
    }

    /**
     * Returns whether this facilitator is exactly for this account.
     *
     * @param account the account to test against.
     */
    override fun isForAccount(account: String?): Boolean {
        return TextUtils.equals(mDictionaryGroup.mAccount, account)
    }

    /**
     * A group of dictionaries that work together for a single language.
     */
    class DictionaryGroup @JvmOverloads constructor(
        locale: Locale? = null,
        mainDict: Dictionary? = null,
        account: String? = null,
        subDicts: Map<String, ExpandableBinaryDictionary?> = emptyMap<String, ExpandableBinaryDictionary>()
    ) {
        /**
         * The locale associated with the dictionary group.
         */
        val mLocale: Locale?

        /**
         * The user account associated with the dictionary group.
         */
        val mAccount: String?

        private var mMainDict: Dictionary? = null

        // Confidence that the most probable language is actually the language the user is
        // typing in. For now, this is simply the number of times a word from this language
        // has been committed in a row.
        private val mConfidence: Int = 0

        var mWeightForTypingInLocale: Float = WEIGHT_FOR_MOST_PROBABLE_LANGUAGE
        var mWeightForGesturingInLocale: Float = WEIGHT_FOR_MOST_PROBABLE_LANGUAGE
        val mSubDictMap: ConcurrentHashMap<String, ExpandableBinaryDictionary> = ConcurrentHashMap()

        init {
            mLocale = locale
            mAccount = account
            // The main dictionary can be asynchronously loaded.
            setMainDict(mainDict)
            for (entry: Map.Entry<String, ExpandableBinaryDictionary?> in subDicts.entries) {
                setSubDict(entry.key, entry.value)
            }
        }

        fun setSubDict(dictType: String, dict: ExpandableBinaryDictionary?) {
            if (dict != null) {
                mSubDictMap.put(dictType, dict)
            }
        }

        fun setMainDict(mainDict: Dictionary?) {
            // Close old dictionary if exists. Main dictionary can be assigned multiple times.
            val oldDict: Dictionary? = mMainDict
            mMainDict = mainDict
            if (oldDict != null && mainDict !== oldDict) {
                oldDict.close()
            }
        }

        fun getDict(dictType: String): Dictionary? {
            if (Dictionary.TYPE_MAIN == dictType) {
                return mMainDict
            }
            return getSubDict(dictType)
        }

        fun getSubDict(dictType: String): ExpandableBinaryDictionary? {
            return mSubDictMap.get(dictType)
        }

        fun hasDict(dictType: String, account: String?): Boolean {
            if (Dictionary.TYPE_MAIN == dictType) {
                return mMainDict != null
            }
            if (Dictionary.TYPE_USER_HISTORY == dictType &&
                !TextUtils.equals(account, mAccount)
            ) {
                // If the dictionary type is user history, & if the account doesn't match,
                // return immediately. If the account matches, continue looking it up in the
                // sub dictionary map.
                return false
            }
            return mSubDictMap.containsKey(dictType)
        }

        fun closeDict(dictType: String) {
            val dict: Dictionary?
            if (Dictionary.TYPE_MAIN == dictType) {
                dict = mMainDict
            } else {
                dict = mSubDictMap.remove(dictType)
            }
            if (dict != null) {
                dict.close()
            }
        }

        companion object {
            // TODO: Add null analysis annotations.
            // TODO: Run evaluation to determine a reasonable value for these constants. The current
            // values are ad-hoc and chosen without any particular care or methodology.
            const val WEIGHT_FOR_MOST_PROBABLE_LANGUAGE: Float = 1.0f
            const val WEIGHT_FOR_GESTURING_IN_NOT_MOST_PROBABLE_LANGUAGE: Float = 0.95f
            const val WEIGHT_FOR_TYPING_IN_NOT_MOST_PROBABLE_LANGUAGE: Float = 0.6f
        }
    }

    override fun onStartInput() {
    }

    override fun onFinishInput(context: Context?) {
    }

    override val isActive: Boolean
        get() {
            return mDictionaryGroup.mLocale != null
        }

    override val locale: Locale
        get() {
            return mDictionaryGroup.mLocale!!
        }

    override fun usesContacts(): Boolean {
        return mDictionaryGroup.getSubDict(Dictionary.TYPE_CONTACTS) != null
    }

    override val account: String?
        get() {
            return null
        }

    override fun resetDictionaries(
        context: Context,
        newLocale: Locale,
        useContactsDict: Boolean,
        usePersonalizedDicts: Boolean,
        forceReloadMainDictionary: Boolean,
        account: String?,
        dictNamePrefix: String,
        listener: DictionaryInitializationListener?
    ) {
        val existingDictionariesToCleanup: HashMap<Locale, ArrayList<String>> = HashMap()
        // TODO: Make subDictTypesToUse configurable by resource or a static final list.
        val subDictTypesToUse: HashSet<String> = HashSet()
        subDictTypesToUse.add(Dictionary.TYPE_USER)

        // Do not use contacts dictionary if we do not have permissions to read contacts.
        val contactsPermissionGranted: Boolean = PermissionsUtil.checkAllPermissionsGranted(
            context, permission.READ_CONTACTS
        )
        if (useContactsDict && contactsPermissionGranted) {
            subDictTypesToUse.add(Dictionary.TYPE_CONTACTS)
        }
        if (usePersonalizedDicts) {
            subDictTypesToUse.add(Dictionary.TYPE_USER_HISTORY)
        }

        // Gather all dictionaries. We'll remove them from the list to clean up later.
        val dictTypeForLocale: ArrayList<String> = ArrayList()
        existingDictionariesToCleanup.put(newLocale, dictTypeForLocale)
        val currentDictionaryGroupForLocale: DictionaryGroup? =
            findDictionaryGroupWithLocale(mDictionaryGroup, newLocale)
        if (currentDictionaryGroupForLocale != null) {
            for (dictType: String in DictionaryFacilitator.DYNAMIC_DICTIONARY_TYPES) {
                if (currentDictionaryGroupForLocale.hasDict(dictType, account)) {
                    dictTypeForLocale.add(dictType)
                }
            }
            if (currentDictionaryGroupForLocale.hasDict(Dictionary.TYPE_MAIN, account)) {
                dictTypeForLocale.add(Dictionary.TYPE_MAIN)
            }
        }

        val dictionaryGroupForLocale: DictionaryGroup? =
            findDictionaryGroupWithLocale(mDictionaryGroup, newLocale)
        val dictTypesToCleanupForLocale: ArrayList<String>? =
            existingDictionariesToCleanup.get(newLocale)
        val noExistingDictsForThisLocale: Boolean = (null == dictionaryGroupForLocale)

        val mainDict: Dictionary?
        if (forceReloadMainDictionary || noExistingDictsForThisLocale
            || !dictionaryGroupForLocale!!.hasDict(Dictionary.TYPE_MAIN, account)
        ) {
            mainDict = null
        } else {
            mainDict = dictionaryGroupForLocale.getDict(Dictionary.TYPE_MAIN)
            dictTypesToCleanupForLocale!!.remove(Dictionary.TYPE_MAIN)
        }

        val subDicts: MutableMap<String, ExpandableBinaryDictionary?> = HashMap()
        for (subDictType: String in subDictTypesToUse) {
            val subDict: ExpandableBinaryDictionary?
            if (noExistingDictsForThisLocale
                || !dictionaryGroupForLocale!!.hasDict(subDictType, account)
            ) {
                // Create a new dictionary.
                subDict = getSubDict(
                    subDictType, context, newLocale, null,  /* dictFile */
                    dictNamePrefix, account
                )
            } else {
                // Reuse the existing dictionary, and don't close it at the end
                subDict = dictionaryGroupForLocale.getSubDict(subDictType)
                dictTypesToCleanupForLocale!!.remove(subDictType)
            }
            subDicts.put(subDictType, subDict)
        }
        val newDictionaryGroup: DictionaryGroup =
            DictionaryGroup(newLocale, mainDict, account, subDicts)

        // Replace Dictionaries.
        val oldDictionaryGroup: DictionaryGroup
        synchronized(mLock) {
            oldDictionaryGroup = mDictionaryGroup
            mDictionaryGroup = newDictionaryGroup
            if (hasAtLeastOneUninitializedMainDictionary()) {
                asyncReloadUninitializedMainDictionaries(context, newLocale, listener)
            }
        }
        if (listener != null) {
            listener.onUpdateMainDictionaryAvailability(hasAtLeastOneInitializedMainDictionary())
        }

        // Clean up old dictionaries.
        for (localeToCleanUp: Locale in existingDictionariesToCleanup.keys) {
            val dictTypesToCleanUp: ArrayList<String>? =
                existingDictionariesToCleanup.get(localeToCleanUp)
            val dictionarySetToCleanup: DictionaryGroup? =
                findDictionaryGroupWithLocale(oldDictionaryGroup, localeToCleanUp)
            for (dictType: String in dictTypesToCleanUp!!) {
                dictionarySetToCleanup!!.closeDict(dictType)
            }
        }

        if (mValidSpellingWordWriteCache != null) {
            mValidSpellingWordWriteCache!!.evictAll()
        }
    }

    private fun asyncReloadUninitializedMainDictionaries(
        context: Context,
        locale: Locale, listener: DictionaryInitializationListener?
    ) {
        val latchForWaitingLoadingMainDictionary: CountDownLatch = CountDownLatch(1)
        mLatchForWaitingLoadingMainDictionaries = latchForWaitingLoadingMainDictionary
        ExecutorUtils.getBackgroundExecutor(ExecutorUtils.KEYBOARD)!!.execute(object : Runnable {
            override fun run() {
                doReloadUninitializedMainDictionaries(
                    context, locale, listener, latchForWaitingLoadingMainDictionary
                )
            }
        })
    }

    fun doReloadUninitializedMainDictionaries(
        context: Context, locale: Locale,
        listener: DictionaryInitializationListener?,
        latchForWaitingLoadingMainDictionary: CountDownLatch
    ) {
        val dictionaryGroup: DictionaryGroup? =
            findDictionaryGroupWithLocale(mDictionaryGroup, locale)
        if (null == dictionaryGroup) {
            // This should never happen, but better safe than crashy
            Log.w(TAG, "Expected a dictionary group for " + locale + " but none found")
            return
        }
        val mainDict: Dictionary =
            DictionaryFactory.createMainDictionaryFromManager(context, locale)
        synchronized(mLock) {
            if (locale == dictionaryGroup.mLocale) {
                dictionaryGroup.setMainDict(mainDict)
            } else {
                // Dictionary facilitator has been reset for another locale.
                mainDict.close()
            }
        }
        if (listener != null) {
            listener.onUpdateMainDictionaryAvailability(hasAtLeastOneInitializedMainDictionary())
        }
        latchForWaitingLoadingMainDictionary.countDown()
    }

    @UsedForTesting
    override fun resetDictionariesForTesting(
        context: Context, locale: Locale,
        dictionaryTypes: ArrayList<String>, dictionaryFiles: HashMap<String?, File?>,
        additionalDictAttributes: Map<String?, Map<String?, String?>?>,
        account: String?
    ) {
        var mainDictionary: Dictionary? = null
        val subDicts: MutableMap<String, ExpandableBinaryDictionary?> = HashMap()

        for (dictType: String in dictionaryTypes) {
            if (dictType == Dictionary.TYPE_MAIN) {
                mainDictionary = DictionaryFactory.createMainDictionaryFromManager(
                    context,
                    locale
                )
            } else {
                val dictFile: File? = dictionaryFiles.get(dictType)
                val dict: ExpandableBinaryDictionary? = getSubDict(
                    dictType, context, locale, dictFile, "",  /* dictNamePrefix */account
                )
                if (additionalDictAttributes.containsKey(dictType)) {
                    dict!!.clearAndFlushDictionaryWithAdditionalAttributes(
                        additionalDictAttributes.get(dictType)
                    )
                }
                if (dict == null) {
                    throw RuntimeException("Unknown dictionary type: " + dictType)
                }
                dict.reloadDictionaryIfRequired()
                dict.waitAllTasksForTests()
                subDicts.put(dictType, dict)
            }
        }
        mDictionaryGroup = DictionaryGroup(locale, mainDictionary, account, subDicts)
    }

    override fun closeDictionaries() {
        val dictionaryGroupToClose: DictionaryGroup
        synchronized(mLock) {
            dictionaryGroupToClose = mDictionaryGroup
            mDictionaryGroup = DictionaryGroup()
        }
        for (dictType: String in DictionaryFacilitator.ALL_DICTIONARY_TYPES) {
            dictionaryGroupToClose.closeDict(dictType)
        }
    }

    @UsedForTesting
    override fun getSubDictForTesting(dictName: String): ExpandableBinaryDictionary? {
        return mDictionaryGroup.getSubDict(dictName)
    }

    // The main dictionaries are loaded asynchronously.  Don't cache the return value
    // of these methods.
    override fun hasAtLeastOneInitializedMainDictionary(): Boolean {
        val mainDict: Dictionary? = mDictionaryGroup.getDict(Dictionary.TYPE_MAIN)
        if (mainDict != null && mainDict.isInitialized) {
            return true
        }
        return false
    }

    override fun hasAtLeastOneUninitializedMainDictionary(): Boolean {
        val mainDict: Dictionary? = mDictionaryGroup.getDict(Dictionary.TYPE_MAIN)
        if (mainDict == null || !mainDict.isInitialized) {
            return true
        }
        return false
    }

    @Throws(InterruptedException::class)
    override fun waitForLoadingMainDictionaries(timeout: Long, unit: TimeUnit?) {
        mLatchForWaitingLoadingMainDictionaries.await(timeout, unit)
    }

    @UsedForTesting
    @Throws(InterruptedException::class)
    override fun waitForLoadingDictionariesForTesting(timeout: Long, unit: TimeUnit?) {
        waitForLoadingMainDictionaries(timeout, unit)
        for (dict: ExpandableBinaryDictionary in mDictionaryGroup.mSubDictMap.values) {
            dict.waitAllTasksForTests()
        }
    }

    override fun addToUserHistory(
        suggestion: String, wasAutoCapitalized: Boolean,
        @Nonnull ngramContext: NgramContext, timeStampInSeconds: Long,
        blockPotentiallyOffensive: Boolean
    ) {
        // Update the spelling cache before learning. Words that are not yet added to user history
        // and appear in no other language model are not considered valid.
        putWordIntoValidSpellingWordCache("addToUserHistory", suggestion)

        val words: Array<String> =
            suggestion.split(Constants.WORD_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
        var ngramContextForCurrentWord: NgramContext = ngramContext
        for (i in words.indices) {
            val currentWord: String = words.get(i)
            val wasCurrentWordAutoCapitalized: Boolean = if ((i == 0)) wasAutoCapitalized else false
            addWordToUserHistory(
                mDictionaryGroup, ngramContextForCurrentWord, currentWord,
                wasCurrentWordAutoCapitalized, timeStampInSeconds.toInt(),
                blockPotentiallyOffensive
            )
            ngramContextForCurrentWord =
                ngramContextForCurrentWord.getNextNgramContext(WordInfo(currentWord))
        }
    }

    private fun putWordIntoValidSpellingWordCache(
        @Nonnull caller: String,
        @Nonnull originalWord: String
    ) {
        if (mValidSpellingWordWriteCache == null) {
            return
        }

        val lowerCaseWord: String = originalWord.lowercase(locale!!)
        val lowerCaseValid: Boolean = isValidSpellingWord(lowerCaseWord)
        mValidSpellingWordWriteCache!!.put(lowerCaseWord, lowerCaseValid)

        val capitalWord: String =
            StringUtils.capitalizeFirstAndDowncaseRest(
                originalWord,
                locale
            )
        val capitalValid: Boolean
        if (lowerCaseValid) {
            // The lower case form of the word is valid, so the upper case must be valid.
            capitalValid = true
        } else {
            capitalValid = isValidSpellingWord(capitalWord)
        }
        mValidSpellingWordWriteCache!!.put(capitalWord, capitalValid)
    }

    private fun addWordToUserHistory(
        dictionaryGroup: DictionaryGroup,
        ngramContext: NgramContext, word: String, wasAutoCapitalized: Boolean,
        timeStampInSeconds: Int, blockPotentiallyOffensive: Boolean
    ) {
        val userHistoryDictionary: ExpandableBinaryDictionary? =
            dictionaryGroup.getSubDict(Dictionary.TYPE_USER_HISTORY)
        if (userHistoryDictionary == null || !isForLocale(userHistoryDictionary.mLocale)) {
            return
        }
        val maxFreq: Int = getFrequency(word)
        if (maxFreq == 0 && blockPotentiallyOffensive) {
            return
        }
        val lowerCasedWord: String = word.lowercase(dictionaryGroup.mLocale!!)
        val secondWord: String
        if (wasAutoCapitalized) {
            if (isValidSuggestionWord(word) && !isValidSuggestionWord(lowerCasedWord)) {
                // If the word was auto-capitalized and exists only as a capitalized word in the
                // dictionary, then we must not downcase it before registering it. For example,
                // the name of the contacts in start-of-sentence position would come here with the
                // wasAutoCapitalized flag: if we downcase it, we'd register a lower-case version
                // of that contact's name which would end up popping in suggestions.
                secondWord = word
            } else {
                // If however the word is not in the dictionary, or exists as a lower-case word
                // only, then we consider that was a lower-case word that had been auto-capitalized.
                secondWord = lowerCasedWord
            }
        } else {
            // HACK: We'd like to avoid adding the capitalized form of common words to the User
            // History dictionary in order to avoid suggesting them until the dictionary
            // consolidation is done.
            // TODO: Remove this hack when ready.
            val lowerCaseFreqInMainDict: Int = if (dictionaryGroup.hasDict(
                    Dictionary.TYPE_MAIN,
                    null /* account */
                )
            ) dictionaryGroup.getDict(Dictionary.TYPE_MAIN)!!
                .getFrequency(lowerCasedWord) else Dictionary.NOT_A_PROBABILITY
            if (maxFreq < lowerCaseFreqInMainDict
                && lowerCaseFreqInMainDict >= CAPITALIZED_FORM_MAX_PROBABILITY_FOR_INSERT
            ) {
                // Use lower cased word as the word can be a distracter of the popular word.
                secondWord = lowerCasedWord
            } else {
                secondWord = word
            }
        }
        // We demote unrecognized words (frequency < 0, below) by specifying them as "invalid".
        // We don't add words with 0-frequency (assuming they would be profanity etc.).
        val isValid: Boolean = maxFreq > 0
        UserHistoryDictionary.addToDictionary(
            userHistoryDictionary, ngramContext, secondWord,
            isValid, timeStampInSeconds
        )
    }

    private fun removeWord(dictName: String, word: String) {
        val dictionary: ExpandableBinaryDictionary? = mDictionaryGroup.getSubDict(dictName)
        if (dictionary != null) {
            dictionary.removeUnigramEntryDynamically(word)
        }
    }

    override fun unlearnFromUserHistory(
        word: String,
        ngramContext: NgramContext, timeStampInSeconds: Long,
        eventType: Int
    ) {
        // TODO: Decide whether or not to remove the word on EVENT_BACKSPACE.
        if (eventType != Constants.EVENT_BACKSPACE) {
            removeWord(Dictionary.TYPE_USER_HISTORY, word)
        }

        // Update the spelling cache after unlearning. Words that are removed from user history
        // and appear in no other language model are not considered valid.
        putWordIntoValidSpellingWordCache(
            "unlearnFromUserHistory",
            word.lowercase(Locale.getDefault())
        )
    }

    // TODO: Revise the way to fusion suggestion results.
    @Nonnull
    override fun getSuggestionResults(
        composedData: ComposedData,
        ngramContext: NgramContext, @Nonnull keyboard: Keyboard,
        settingsValuesForSuggestion: SettingsValuesForSuggestion, sessionId: Int,
        inputStyle: Int
    ): SuggestionResults {
        val proximityInfoHandle: Long = keyboard.proximityInfo.getNativeProximityInfo()
        val suggestionResults = SuggestionResults(
            SuggestedWords.MAX_SUGGESTIONS, ngramContext.isBeginningOfSentenceContext,
            false /* firstSuggestionExceedsConfidenceThreshold */
        )
        val weightOfLangModelVsSpatialModel: FloatArray =
            floatArrayOf(Dictionary.NOT_A_WEIGHT_OF_LANG_MODEL_VS_SPATIAL_MODEL)
        for (dictType: String in DictionaryFacilitator.ALL_DICTIONARY_TYPES) {
            val dictionary: Dictionary? = mDictionaryGroup.getDict(dictType)
            if (null == dictionary) continue
            val weightForLocale: Float = if (composedData.mIsBatchMode)
                mDictionaryGroup.mWeightForGesturingInLocale
            else
                mDictionaryGroup.mWeightForTypingInLocale
            val dictionarySuggestions: ArrayList<SuggestedWordInfo>? =
                dictionary.getSuggestions(
                    composedData, ngramContext,
                    proximityInfoHandle, settingsValuesForSuggestion, sessionId,
                    weightForLocale, weightOfLangModelVsSpatialModel
                )
            if (null == dictionarySuggestions) continue
            suggestionResults.addAll(dictionarySuggestions)
            suggestionResults.mRawSuggestions?.addAll(dictionarySuggestions)
        }
        return suggestionResults
    }

    override fun isValidSpellingWord(word: String): Boolean {
        if (mValidSpellingWordReadCache != null) {
            val cachedValue: Boolean = mValidSpellingWordReadCache!!.get(word)
            if (cachedValue != null) {
                return cachedValue
            }
        }

        return isValidWord(word, DictionaryFacilitator.ALL_DICTIONARY_TYPES)
    }

    override fun isValidSuggestionWord(word: String): Boolean {
        return isValidWord(word, DictionaryFacilitator.ALL_DICTIONARY_TYPES)
    }

    private fun isValidWord(word: String, dictionariesToCheck: Array<String>): Boolean {
        if (TextUtils.isEmpty(word)) {
            return false
        }
        if (mDictionaryGroup.mLocale == null) {
            return false
        }
        for (dictType: String in dictionariesToCheck) {
            val dictionary: Dictionary? = mDictionaryGroup.getDict(dictType)
            // Ideally the passed map would come out of a {@link java.util.concurrent.Future} and
            // would be immutable once it's finished initializing, but concretely a null test is
            // probably good enough for the time being.
            if (null == dictionary) continue
            if (dictionary.isValidWord(word)) {
                return true
            }
        }
        return false
    }

    private fun getFrequency(word: String): Int {
        if (TextUtils.isEmpty(word)) {
            return Dictionary.NOT_A_PROBABILITY
        }
        var maxFreq: Int = Dictionary.NOT_A_PROBABILITY
        for (dictType: String in DictionaryFacilitator.ALL_DICTIONARY_TYPES) {
            val dictionary: Dictionary? = mDictionaryGroup.getDict(dictType)
            if (dictionary == null) continue
            val tempFreq: Int = dictionary.getFrequency(word)
            if (tempFreq >= maxFreq) {
                maxFreq = tempFreq
            }
        }
        return maxFreq
    }

    private fun clearSubDictionary(dictName: String): Boolean {
        val dictionary: ExpandableBinaryDictionary? = mDictionaryGroup.getSubDict(dictName)
        if (dictionary == null) {
            return false
        }
        dictionary.clear()
        return true
    }

    override fun clearUserHistoryDictionary(context: Context?): Boolean {
        return clearSubDictionary(Dictionary.TYPE_USER_HISTORY)
    }

    override fun dumpDictionaryForDebug(dictName: String) {
        val dictToDump: ExpandableBinaryDictionary? = mDictionaryGroup.getSubDict(dictName)
        if (dictToDump == null) {
            Log.e(
                TAG, ("Cannot dump " + dictName + ". "
                        + "The dictionary is not being used for suggestion or cannot be dumped.")
            )
            return
        }
        dictToDump.dumpAllWordsForDebug()
    }

    @Nonnull
    override fun getDictionaryStats(context: Context?): List<DictionaryStats?> {
        val statsOfEnabledSubDicts: ArrayList<DictionaryStats?> = ArrayList()
        for (dictType: String in DictionaryFacilitator.DYNAMIC_DICTIONARY_TYPES) {
            val dictionary: ExpandableBinaryDictionary? = mDictionaryGroup.getSubDict(dictType)
            if (dictionary == null) continue
            statsOfEnabledSubDicts.add(dictionary.dictionaryStats)
        }
        return statsOfEnabledSubDicts
    }

    override fun dump(context: Context?): String {
        return ""
    }

    companion object {
        // TODO: Consolidate dictionaries in native code.
        val TAG: String = DictionaryFacilitatorImpl::class.java.getSimpleName()

        // HACK: This threshold is being used when adding a capitalized entry in the User History
        // dictionary.
        private const val CAPITALIZED_FORM_MAX_PROBABILITY_FOR_INSERT: Int = 140

        val DICT_TYPE_TO_CLASS: MutableMap<String, Class<out ExpandableBinaryDictionary>> =
            HashMap()

        init {
            DICT_TYPE_TO_CLASS.put(
                Dictionary.TYPE_USER_HISTORY,
                UserHistoryDictionary::class.java
            )
            DICT_TYPE_TO_CLASS.put(
                Dictionary.TYPE_USER,
                UserBinaryDictionary::class.java
            )
            DICT_TYPE_TO_CLASS.put(
                Dictionary.TYPE_CONTACTS,
                ContactsBinaryDictionary::class.java
            )
        }

        private const val DICT_FACTORY_METHOD_NAME: String = "getDictionary"
        private val DICT_FACTORY_METHOD_ARG_TYPES: Array<Class<*>> = arrayOf(
            Context::class.java,
            Locale::class.java,
            File::class.java,
            String::class.java,
            String::class.java
        )

        private fun getSubDict(
            dictType: String,
            context: Context, locale: Locale, dictFile: File?,
            dictNamePrefix: String, account: String?
        ): ExpandableBinaryDictionary? {
            val dictClass: Class<out ExpandableBinaryDictionary>? =
                DICT_TYPE_TO_CLASS.get(dictType)
            if (dictClass == null) {
                return null
            }
            try {
                val factoryMethod: Method = dictClass.getMethod(
                    DICT_FACTORY_METHOD_NAME,
                    *DICT_FACTORY_METHOD_ARG_TYPES
                )
                val dict: Any? = factoryMethod.invoke(
                    null,  /* obj */
                    *arrayOf(context, locale, dictFile, dictNamePrefix, account)
                )
                return dict as ExpandableBinaryDictionary?
            } catch (e: NoSuchMethodException) {
                Log.e(TAG, "Cannot create dictionary: " + dictType, e)
                return null
            } catch (e: SecurityException) {
                Log.e(TAG, "Cannot create dictionary: " + dictType, e)
                return null
            } catch (e: IllegalAccessException) {
                Log.e(TAG, "Cannot create dictionary: " + dictType, e)
                return null
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Cannot create dictionary: " + dictType, e)
                return null
            } catch (e: InvocationTargetException) {
                Log.e(TAG, "Cannot create dictionary: " + dictType, e)
                return null
            }
        }

        fun findDictionaryGroupWithLocale(
            dictionaryGroup: DictionaryGroup,
            locale: Locale
        ): DictionaryGroup? {
            return if (locale == dictionaryGroup.mLocale) dictionaryGroup else null
        }
    }
}
